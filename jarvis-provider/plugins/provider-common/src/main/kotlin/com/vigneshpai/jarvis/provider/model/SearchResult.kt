package com.vigneshpai.jarvis.provider.model

import kotlinx.serialization.Serializable

/** A single result returned from a provider search. */
@Serializable
data class SearchResult(
    /** Stable identifier used by this provider to reference the item. */
    val id: String,
    /** Display title. */
    val title: String,
    /** Optional poster image URL. */
    val posterUrl: String? = null,
    /** Release year, if available. */
    val year: Int? = null,
    /** Content type (movie, series, anime, …). */
    val type: ContentType = ContentType.UNKNOWN,
)

@Serializable
enum class ContentType {
    MOVIE,
    SERIES,
    ANIME,
    UNKNOWN,
}
