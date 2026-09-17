// Root build file — coordinates all plugin modules.
// Individual plugin modules are under plugins/.

plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

subprojects {
    group = "com.vigneshpai.jarvis"
}

/**
 * Build all plugin JARs at once:
 *   ./gradlew buildAllPlugins
 */
tasks.register("buildAllPlugins") {
    group = "jarvis"
    description = "Build all provider plugin JARs"
    dependsOn(
        ":plugins:hulu-provider:shadowJar",
        ":plugins:vaplayer-provider:shadowJar",
    )
}
