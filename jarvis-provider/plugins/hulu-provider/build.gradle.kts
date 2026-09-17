/**
 * Build script for the hulu-provider plugin module.
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

version = findProperty("jarvis.pluginVersion") as String? ?: "1.0.0"

dependencies {
    compileOnly(project(":plugins:provider-common"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.shadowJar {
    archiveBaseName.set("hulu-provider")
    archiveClassifier.set("")
    manifest {
        attributes(
            "Plugin-Entry-Point" to "com.vigneshpai.jarvis.provider.hulu.HuluProviderPlugin",
            "Plugin-Id"          to "provider.hulu",
            "Plugin-Version"     to project.version,
            "Plugin-Min-Api"     to 1,
            "Plugin-Max-Api"     to 1,
        )
    }
    exclude("com/vigneshpai/jarvis/provider/api/**")
    exclude("com/vigneshpai/jarvis/provider/model/**")
}

tasks.build { dependsOn(tasks.shadowJar) }
