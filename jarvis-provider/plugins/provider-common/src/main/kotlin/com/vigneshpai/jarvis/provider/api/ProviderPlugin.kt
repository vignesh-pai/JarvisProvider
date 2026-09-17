package com.vigneshpai.jarvis.provider.api

import com.vigneshpai.jarvis.provider.model.PluginMetadata

/**
 * Entry-point interface for every provider plugin JAR.
 *
 * The Jarvis runtime discovers plugins by loading the class named in the
 * JAR manifest attribute `Plugin-Entry-Point` and casting it to this interface.
 *
 * ### JAR manifest convention
 * ```
 * Plugin-Entry-Point: com.example.myprovider.MyProviderPlugin
 * ```
 *
 * ### Minimal implementation
 * ```kotlin
 * class MyProviderPlugin : ProviderPlugin {
 *     override val metadata = PluginMetadata(
 *         id = "provider.example",
 *         name = "Example Provider",
 *         version = "1.0.0",
 *         minApiVersion = 1,
 *         maxApiVersion = 1,
 *     )
 *     override fun createProvider(): Provider = MyProvider()
 * }
 * ```
 */
interface ProviderPlugin {

    /** Metadata used by the Jarvis app for compatibility checks and UI display. */
    val metadata: PluginMetadata

    /** Factory method — called once per plugin load to obtain a [Provider] instance. */
    fun createProvider(): Provider
}
