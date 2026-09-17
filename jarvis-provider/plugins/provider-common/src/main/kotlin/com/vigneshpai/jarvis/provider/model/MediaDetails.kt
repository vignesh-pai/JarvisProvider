package com.vigneshpai.jarvis.provider.model

import kotlinx.serialization.Serializable

/** Full details for a piece of media resolved from a provider. */
@Serializable
data class MediaDetails(
    /** Provider-specific identifier. */
    val id: String,
    val title: String,
    val description: String? = null,
    val posterUrl: String? = null,
    val backdropUrl: String? = null,
    val year: Int? = null,
    val type: ContentType = ContentType.UNKNOWN,
    /** Episodes, ordered by season → episode. Present for SERIES and ANIME. */
    val episodes: List<Episode> = emptyList(),
    val genres: List<String> = emptyList(),
    val rating: Float? = null,
    val duration: Int? = null,
)

@Serializable
data class Episode(
    val id: String,
    val title: String? = null,
    val season: Int = 1,
    val episode: Int,
    val thumbUrl: String? = null,
)
