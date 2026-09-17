package com.vigneshpai.jarvis.provider.vaplayer

import com.vigneshpai.jarvis.provider.api.Provider
import com.vigneshpai.jarvis.provider.api.ProviderPlugin
import com.vigneshpai.jarvis.provider.model.ContentType
import com.vigneshpai.jarvis.provider.model.Episode
import com.vigneshpai.jarvis.provider.model.MediaDetails
import com.vigneshpai.jarvis.provider.model.PluginMetadata
import com.vigneshpai.jarvis.provider.model.SearchResult
import com.vigneshpai.jarvis.provider.model.StreamLink
import com.vigneshpai.jarvis.provider.model.SubtitleTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration

// ── Constants ────────────────────────────────────────────────────────────────

private const val VA_PLAYER_API     = "https://streamdata.vaplayer.ru/api.php"
private const val VA_PLAYER_REFERER = "https://nextgencloudfabric.com/"
private const val TMDB_SEARCH_BASE  = "https://www.themoviedb.org"
private const val TMDB_IMAGE_BASE   = "https://image.tmdb.org/t/p/w500"
private const val USER_AGENT =
    "Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/121.0"

// ── Plugin entry-point ───────────────────────────────────────────────────────

/**
 * Plugin entry-point for the VaPlayer provider.
 *
 * This class is referenced by the JAR manifest attribute `Plugin-Entry-Point`
 * so the Jarvis runtime can instantiate it via reflection.
 */
class VaPlayerProviderPlugin : ProviderPlugin {

    override val metadata = PluginMetadata(
        id = "provider.vaplayer",
        name = "VaPlayer",
        version = "1.0.0",
        description = "Streams movies and series via VaPlayer (streamdata.vaplayer.ru)",
        author = "Jarvis Team",
        minApiVersion = 1,
        maxApiVersion = 1,
        minAppVersion = "2.0.0",
        supportedTypes = listOf("movie", "series"),
    )

    override fun createProvider(): Provider = VaPlayerProvider()
}

// ── Provider implementation ──────────────────────────────────────────────────

/**
 * [Provider] implementation backed by the VaPlayer streaming API.
 *
 * Media IDs use the format `{type}/{tmdb_id}`, e.g. `movie/550` or `tv/1396`.
 * Episode IDs use the format `{season}/{episode}`, e.g. `1/1`.
 *
 * Stream links are HLS (m3u8) URLs served by VaPlayer; a `Referer` header of
 * [VA_PLAYER_REFERER] must be sent with every playback request.
 */
class VaPlayerProvider : Provider {

    private val http: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(15))
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val json = Json { ignoreUnknownKeys = true }

    // ── search ───────────────────────────────────────────────────────────────

    /**
     * Searches TMDB's public trending/search endpoint (no API key required).
     *
     * Results are filtered to movies and TV shows only.
     */
    override suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query, StandardCharsets.UTF_8)
        val url = "$TMDB_SEARCH_BASE/search/trending?query=$encoded"
        val body = fetchText(url, referer = "$TMDB_SEARCH_BASE/") ?: return@withContext emptyList()

        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
            ?: return@withContext emptyList()

        val results = root["results"]?.jsonArray ?: return@withContext emptyList()

        buildList {
            for (element in results) {
                // The TMDB trending endpoint mixes JSON objects with autocomplete strings;
                // skip any element that is not an object.
                val obj = (element as? JsonObject) ?: continue

                val mediaType = obj["media_type"]?.jsonPrimitive?.contentOrNull ?: continue
                if (mediaType != "movie" && mediaType != "tv") continue

                val id = obj["id"]?.jsonPrimitive?.intOrNull ?: continue
                val title = obj["title"]?.jsonPrimitive?.contentOrNull
                    ?: obj["name"]?.jsonPrimitive?.contentOrNull
                    ?: continue
                val posterPath = obj["poster_path"]?.jsonPrimitive?.contentOrNull
                val releaseDate = obj["release_date"]?.jsonPrimitive?.contentOrNull
                    ?: obj["first_air_date"]?.jsonPrimitive?.contentOrNull
                val year = releaseDate?.take(4)?.toIntOrNull()
                val contentType = if (mediaType == "tv") ContentType.SERIES else ContentType.MOVIE

                add(
                    SearchResult(
                        id = "$mediaType/$id",
                        title = title,
                        posterUrl = posterPath?.let { "$TMDB_IMAGE_BASE$it" },
                        year = year,
                        type = contentType,
                    )
                )
            }
        }
    }

    // ── loadDetails ──────────────────────────────────────────────────────────

    /**
     * Returns [MediaDetails] for the given [id] (`{type}/{tmdb_id}`).
     *
     * For TV shows the episode list is discovered by probing the VaPlayer API
     * season by season until no further content is found (up to 25 seasons /
     * 50 episodes per season).
     */
    override suspend fun loadDetails(id: String): MediaDetails? = withContext(Dispatchers.IO) {
        val (mediaType, tmdbId) = parseMediaId(id) ?: return@withContext null

        if (mediaType == "movie") {
            val apiUrl = "$VA_PLAYER_API?tmdb=$tmdbId&type=movie"
            val data = fetchVaPlayerData(apiUrl)
            val title = data?.get("title")?.jsonPrimitive?.contentOrNull
                ?: data?.get("file_name")?.jsonPrimitive?.contentOrNull
                ?: id
            MediaDetails(
                id = id,
                title = title,
                type = ContentType.MOVIE,
            )
        } else {
            // Fetch title from season 1 episode 1 probe, which also starts discovery
            val probeUrl = "$VA_PLAYER_API?tmdb=$tmdbId&type=tv&season=1&episode=1"
            val probeData = fetchVaPlayerData(probeUrl)
            val title = probeData?.get("title")?.jsonPrimitive?.contentOrNull
                ?: probeData?.get("file_name")?.jsonPrimitive?.contentOrNull
                ?: id
            val episodes = discoverEpisodes(tmdbId)
            MediaDetails(
                id = id,
                title = title,
                type = ContentType.SERIES,
                episodes = episodes,
            )
        }
    }

    // ── loadLinks ────────────────────────────────────────────────────────────

    /**
     * Resolves HLS stream links from VaPlayer.
     *
     * @param mediaId   `{type}/{tmdb_id}`, e.g. `movie/550` or `tv/1396`.
     * @param episodeId `{season}/{episode}` for series, `null` for movies.
     */
    override suspend fun loadLinks(mediaId: String, episodeId: String?): List<StreamLink> =
        withContext(Dispatchers.IO) {
            val (mediaType, tmdbId) = parseMediaId(mediaId) ?: return@withContext emptyList()

            val apiUrl = buildApiUrl(mediaType, tmdbId, episodeId)
                ?: return@withContext emptyList()

            val responseData = fetchVaPlayerData(apiUrl) ?: return@withContext emptyList()
            val streamUrls = responseData["stream_urls"]?.jsonArray ?: return@withContext emptyList()

            buildList {
                streamUrls.forEachIndexed { index, element ->
                    val url = element.jsonPrimitive.contentOrNull ?: return@forEachIndexed
                    add(
                        StreamLink(
                            url = url,
                            quality = if (index == 0) "Auto" else "Mirror ${index + 1}",
                            mimeType = "application/x-mpegurl",
                            headers = mapOf("Referer" to VA_PLAYER_REFERER),
                        )
                    )
                }
            }
        }

    // ── loadSubtitles ────────────────────────────────────────────────────────

    /**
     * Returns subtitle tracks bundled in the VaPlayer API response.
     */
    override suspend fun loadSubtitles(mediaId: String, episodeId: String?): List<SubtitleTrack> =
        withContext(Dispatchers.IO) {
            val (mediaType, tmdbId) = parseMediaId(mediaId) ?: return@withContext emptyList()

            val apiUrl = buildApiUrl(mediaType, tmdbId, episodeId)
                ?: return@withContext emptyList()

            val responseData = fetchVaPlayerData(apiUrl) ?: return@withContext emptyList()
            val subtitles = responseData["subtitles"]?.jsonArray ?: return@withContext emptyList()

            buildList {
                for (element in subtitles) {
                    val sub = element as? JsonObject ?: continue
                    val url = sub["url"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                        ?: continue
                    val language = sub["language"]?.jsonPrimitive?.contentOrNull ?: "und"
                    val label = sub["label"]?.jsonPrimitive?.contentOrNull
                        ?: language
                    add(SubtitleTrack(url = url, language = language, label = label))
                }
            }
        }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Splits `{type}/{tmdbId}` into a pair, or returns `null` on invalid input.
     */
    private fun parseMediaId(id: String): Pair<String, String>? {
        val slash = id.lastIndexOf('/')
        if (slash < 0) return null
        val type = id.substring(0, slash)
        val tmdbId = id.substring(slash + 1)
        if (tmdbId.isBlank() || tmdbId.toLongOrNull() == null) return null
        return type to tmdbId
    }

    /**
     * Builds the VaPlayer API URL for a movie or a specific TV episode.
     *
     * @param episodeId `{season}/{episode}` or `null` for movies.
     */
    private fun buildApiUrl(mediaType: String, tmdbId: String, episodeId: String?): String? {
        return if (mediaType == "movie" || episodeId == null) {
            "$VA_PLAYER_API?tmdb=$tmdbId&type=movie"
        } else {
            val parts = episodeId.split('/')
            if (parts.size != 2) return null
            val season = parts[0].toIntOrNull()?.takeIf { it > 0 } ?: return null
            val episode = parts[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
            "$VA_PLAYER_API?tmdb=$tmdbId&type=tv&season=$season&episode=$episode"
        }
    }

    /**
     * Fetches the VaPlayer API and returns the `data` object from the JSON response,
     * or `null` if the request fails or the API indicates an error.
     */
    private fun fetchVaPlayerData(apiUrl: String): JsonObject? {
        val body = fetchText(apiUrl, referer = VA_PLAYER_REFERER) ?: return null
        val root = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull() ?: return null
        if (root["status_code"]?.jsonPrimitive?.contentOrNull != "200") return null
        return root["data"]?.jsonObject
    }

    /**
     * Probes the VaPlayer API to discover all available seasons and episodes for
     * a TV show identified by its TMDB ID.
     *
     * Scanning stops when VaPlayer returns no stream URLs for the next season or
     * episode, up to 25 seasons × 50 episodes.
     */
    private fun discoverEpisodes(tmdbId: String): List<Episode> {
        val episodes = mutableListOf<Episode>()
        for (season in 1..25) {
            var foundAny = false
            for (ep in 1..50) {
                val url = "$VA_PLAYER_API?tmdb=$tmdbId&type=tv&season=$season&episode=$ep"
                val data = fetchVaPlayerData(url) ?: break
                val streamUrls = data["stream_urls"]?.jsonArray ?: break
                if (streamUrls.isEmpty()) break
                val title = data["file_name"]?.jsonPrimitive?.contentOrNull
                    ?: "Episode $ep"
                episodes.add(
                    Episode(
                        id = "$season/$ep",
                        title = title,
                        season = season,
                        episode = ep,
                    )
                )
                foundAny = true
            }
            if (!foundAny) break
        }
        return episodes
    }

    /**
     * Performs a blocking HTTP GET and returns the response body as a string,
     * or `null` on any error.
     */
    private fun fetchText(url: String, referer: String): String? {
        return runCatching {
            val request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(20))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "application/json, */*")
                .header("Referer", referer)
                .GET()
                .build()
            val response = http.send(request, HttpResponse.BodyHandlers.ofString())
            if (response.statusCode() in 200..299) response.body() else null
        }.getOrNull()
    }
}
