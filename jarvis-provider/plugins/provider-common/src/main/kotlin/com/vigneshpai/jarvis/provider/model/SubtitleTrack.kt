package com.vigneshpai.jarvis.provider.model

import kotlinx.serialization.Serializable

/** A subtitle/caption track associated with a media item. */
@Serializable
data class SubtitleTrack(
    /** URL pointing to the subtitle file (.srt, .vtt, …). */
    val url: String,
    /** BCP-47 language tag, e.g. "en", "ja". */
    val language: String,
    /** Optional display label shown to the user. */
    val label: String? = null,
    /** True if this track is the default selection. */
    val isDefault: Boolean = false,
)
