package com.vigneshpai.jarvis.provider.model

import kotlinx.serialization.Serializable

/**
 * Metadata declared by a plugin JAR.
 *
 * The Jarvis app reads this to verify API compatibility before loading the plugin.
 */
@Serializable
data class PluginMetadata(
    /** Stable reverse-domain identifier, e.g. "provider.netflix". */
    val id: String,
    val name: String,
    val version: String,
    val description: String = "",
    val author: String = "",
    /**
     * Lowest provider-api version this plugin supports.
     * Must be ≤ the API version the running Jarvis build exposes.
     */
    val minApiVersion: Int,
    /**
     * Highest provider-api version this plugin supports.
     * Jarvis will refuse to load plugins whose maxApiVersion is below
     * the app's current API version.
     */
    val maxApiVersion: Int,
    /** Minimum Jarvis app version string required (semver). */
    val minAppVersion: String = "1.0.0",
    /** Content types the plugin can serve. */
    val supportedTypes: List<String> = emptyList(),
)
