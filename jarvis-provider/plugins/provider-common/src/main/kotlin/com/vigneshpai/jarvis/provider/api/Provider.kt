package com.vigneshpai.jarvis.provider.api

import com.vigneshpai.jarvis.provider.model.MediaDetails
import com.vigneshpai.jarvis.provider.model.SearchResult
import com.vigneshpai.jarvis.provider.model.StreamLink
import com.vigneshpai.jarvis.provider.model.SubtitleTrack

/**
 * Core interface that every provider plugin must implement.
 *
 * All suspend functions run on the dispatcher chosen by the Jarvis runtime;
 * avoid blocking calls inside implementations.
 */
interface Provider {

    /**
     * Search for content matching [query].
     *
     * @return A list of [SearchResult] items; empty list if nothing found.
     */
    suspend fun search(query: String): List<SearchResult>

    /**
     * Resolve full details for the item identified by [id].
     *
     * @param id The provider-specific identifier from [SearchResult.id].
     * @return Full [MediaDetails], or `null` if the item could not be resolved.
     */
    suspend fun loadDetails(id: String): MediaDetails?

    /**
     * Extract playable stream links for the given episode or movie.
     *
     * @param mediaId  Provider-specific media identifier.
     * @param episodeId Episode identifier (pass `null` for movies).
     * @return A list of [StreamLink] items ordered from highest to lowest quality.
     */
    suspend fun loadLinks(mediaId: String, episodeId: String? = null): List<StreamLink>

    /**
     * Extract subtitle tracks for the given episode or movie.
     *
     * @return Empty list if no subtitles are available.
     */
    suspend fun loadSubtitles(mediaId: String, episodeId: String? = null): List<SubtitleTrack>
}
