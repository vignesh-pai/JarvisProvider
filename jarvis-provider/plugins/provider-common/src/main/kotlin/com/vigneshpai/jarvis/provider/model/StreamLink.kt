package com.vigneshpai.jarvis.provider.model

import kotlinx.serialization.Serializable

/** A playable stream link resolved by a provider. */
@Serializable
data class StreamLink(
    /** Direct playback URL. */
    val url: String,
    /** Human-readable quality label, e.g. "1080p", "720p". */
    val quality: String? = null,
    /** MIME type, e.g. "video/mp4", "application/x-mpegurl". */
    val mimeType: String? = null,
    /** Additional HTTP headers required for playback. */
    val headers: Map<String, String> = emptyMap(),
    /** True if this link requires DRM. */
    val isDrm: Boolean = false,
)
