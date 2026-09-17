package com.vigneshpai.jarvis.provider.hulu

import com.vigneshpai.jarvis.provider.api.Provider
import com.vigneshpai.jarvis.provider.api.ProviderPlugin
import com.vigneshpai.jarvis.provider.model.MediaDetails
import com.vigneshpai.jarvis.provider.model.PluginMetadata
import com.vigneshpai.jarvis.provider.model.SearchResult
import com.vigneshpai.jarvis.provider.model.StreamLink
import com.vigneshpai.jarvis.provider.model.SubtitleTrack

/**
 * Plugin entry-point for the Hulu provider.
 */
class HuluProviderPlugin : ProviderPlugin {

    override val metadata = PluginMetadata(
        id = "provider.hulu",
        name = "Hulu Provider",
        version = "1.0.0",
        description = "Streams content from Hulu",
        author = "Jarvis Team",
        minApiVersion = 1,
        maxApiVersion = 1,
        minAppVersion = "2.0.0",
        supportedTypes = listOf("movie", "series"),
    )

    override fun createProvider(): Provider = HuluProvider()
}

/**
 * Concrete Hulu [Provider] implementation.
 *
 * Replace the stub bodies below with real HTTP scraping / API calls.
 */
class HuluProvider : Provider {

    override suspend fun search(query: String): List<SearchResult> {
        // TODO: implement Hulu search
        return emptyList()
    }

    override suspend fun loadDetails(id: String): MediaDetails? {
        // TODO: implement Hulu detail loading
        return null
    }

    override suspend fun loadLinks(mediaId: String, episodeId: String?): List<StreamLink> {
        // TODO: implement Hulu link extraction
        return emptyList()
    }

    override suspend fun loadSubtitles(mediaId: String, episodeId: String?): List<SubtitleTrack> {
        // TODO: implement Hulu subtitle extraction
        return emptyList()
    }
}
