/**
 * Build script for the vaplayer-provider plugin module.
 *
 * Produces a fat (shadow) JAR with the Plugin-Entry-Point manifest attribute
 * so the Jarvis runtime can discover the entry-point class automatically.
 */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

version = findProperty("jarvis.pluginVersion") as String? ?: "1.0.0"

dependencies {
    // provider-common is a "provided" dependency — the Jarvis runtime supplies it
    // at load time, so we only need it on the compile class-path, not in the fat JAR.
    compileOnly(project(":plugins:provider-common"))

    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.shadowJar {
    archiveBaseName.set("vaplayer-provider")
    archiveClassifier.set("")
    manifest {
        attributes(
            "Plugin-Entry-Point" to "com.vigneshpai.jarvis.provider.vaplayer.VaPlayerProviderPlugin",
            "Plugin-Id"          to "provider.vaplayer",
            "Plugin-Version"     to project.version,
            "Plugin-Min-Api"     to 1,
            "Plugin-Max-Api"     to 1,
        )
    }
    // Exclude provider-common classes — supplied by the host runtime.
    exclude("com/vigneshpai/jarvis/provider/api/**")
    exclude("com/vigneshpai/jarvis/provider/model/**")
}

// Make the default `build` task produce the shadow JAR.
tasks.build { dependsOn(tasks.shadowJar) }
