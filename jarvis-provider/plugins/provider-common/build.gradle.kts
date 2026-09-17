/**
 * Build script for the provider-common module.
 *
 * This module defines the stable plugin contract that all provider plugins must implement.
 * It is a compile-time dependency for plugin authors and for the Jarvis app runtime.
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

version = "1.0.0"

dependencies {
    api(libs.kotlinx.serialization.json)
    api(libs.kotlinx.coroutines.core)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.jar {
    manifest {
        attributes(
            "Provider-Common-Version" to project.version,
        )
    }
}
