# JarvisProvider

> **Plugin distribution hub for the [Jarvis](https://github.com/vignesh-pai/Jarvis) app.**  
> Providers (content extractors) are published here as independently-updatable JAR plugins.  
> The plugin manifest is served globally via [Vercel](https://jarvis-provider.vercel.app/plugins.json).

---

## Architecture overview

```
Jarvis App  ──fetch──▶  Vercel (plugins.json manifest)
                              │
                              └──download──▶  GitHub Releases (plugin JARs)
```

| Component | Description |
|---|---|
| `jarvis-provider/plugins/provider-common` | Stable API interfaces & DTOs shared by all plugins |
| `jarvis-provider/plugins/<name>-provider` | Individual provider plugin modules |
| `public/plugins.json` | Master manifest hosted on Vercel |
| `.github/workflows/publish-plugins.yml` | CI/CD: build → release → manifest update |
| `jarvis-provider/scripts/generate-manifest.sh` | Helper script used by the workflow |

---

## Manifest URL

```
https://jarvis-provider.vercel.app/plugins.json
```

Configure the Jarvis app to fetch from this URL:

```kotlin
// In your PluginRepositoryManager
val MANIFEST_URL = "https://jarvis-provider.vercel.app/plugins.json"
```

---

## Repository structure

```
JarvisProvider/
├── .github/
│   └── workflows/
│       └── publish-plugins.yml   # Build & release workflow
├── jarvis-provider/
│   ├── plugins/
│   │   ├── provider-common/          # Shared interfaces/DTOs (stable contract)
│   │   │   └── src/main/kotlin/
│   │   │       └── com/vigneshpai/jarvis/provider/
│   │   │           ├── api/
│   │   │           │   ├── Provider.kt
│   │   │           │   └── ProviderPlugin.kt
│   │   │           └── model/
│   │   │               ├── SearchResult.kt
│   │   │               ├── MediaDetails.kt
│   │   │               ├── StreamLink.kt
│   │   │               ├── SubtitleTrack.kt
│   │   │               └── PluginMetadata.kt
│   │   ├── vaplayer-provider/        # VaPlayer provider plugin
│   │   └── hulu-provider/            # Hulu provider plugin
│   ├── scripts/
│   │   └── generate-manifest.sh      # Update manifest after a release
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   └── gradle.properties
├── public/
│   └── plugins.json              # Manifest served by Vercel
└── README.md
```

---

## Adding a new provider

### 1. Create the module directory

```bash
mkdir -p jarvis-provider/plugins/myprovider-provider/src/main/kotlin/com/vigneshpai/jarvis/provider/myprovider
```

### 2. Add `build.gradle.kts`

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

version = "1.0.0"

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
    archiveBaseName.set("myprovider-provider")
    archiveClassifier.set("")
    manifest {
        attributes(
            "Plugin-Entry-Point" to "com.vigneshpai.jarvis.provider.myprovider.MyProviderPlugin",
            "Plugin-Id"          to "provider.myprovider",
            "Plugin-Version"     to project.version,
            "Plugin-Min-Api"     to 1,
            "Plugin-Max-Api"     to 1,
        )
    }
    exclude("com/vigneshpai/jarvis/provider/api/**")
    exclude("com/vigneshpai/jarvis/provider/model/**")
}

tasks.build { dependsOn(tasks.shadowJar) }
```

### 3. Register the module in `jarvis-provider/settings.gradle.kts`

```kotlin
include(":plugins:myprovider-provider")
```

### 4. Implement the plugin entry-point

```kotlin
class MyProviderPlugin : ProviderPlugin {
    override val metadata = PluginMetadata(
        id = "provider.myprovider",
        name = "My Provider",
        version = "1.0.0",
        minApiVersion = 1,
        maxApiVersion = 1,
    )
    override fun createProvider(): Provider = MyProvider()
}

class MyProvider : Provider {
    override suspend fun search(query: String): List<SearchResult> = TODO()
    override suspend fun loadDetails(id: String): MediaDetails? = TODO()
    override suspend fun loadLinks(mediaId: String, episodeId: String?): List<StreamLink> = TODO()
    override suspend fun loadSubtitles(mediaId: String, episodeId: String?): List<SubtitleTrack> = TODO()
}
```

---

## Versioning & releasing a plugin

### Tag naming convention

```
<provider-name>-v<semver>
```

Examples: `netflix-v1.0.0`, `hulu-v2.1.3`, `anime-v1.0.0-beta1`

### Release steps

```bash
# 1. Bump version in jarvis-provider/plugins/<name>-provider/build.gradle.kts
# 2. Commit the version bump
git add jarvis-provider/plugins/<name>-provider/build.gradle.kts
git commit -m "chore: bump <name>-provider to v1.2.3"

# 3. Tag & push — GitHub Actions takes care of the rest
git tag <name>-v1.2.3
git push origin <name>-v1.2.3
```

The workflow will:
1. Build the plugin fat JAR
2. Compute the SHA-256 checksum
3. Create a GitHub Release and upload the JAR
4. Update `public/plugins.json` with the new version metadata
5. Commit the updated manifest — Vercel auto-redeploys

---

## Local development & testing

### Build a single plugin

```bash
cd jarvis-provider && ./gradlew :plugins:vaplayer-provider:shadowJar
# JAR → jarvis-provider/plugins/vaplayer-provider/build/libs/vaplayer-provider-<version>.jar
```

### Build all plugins at once

```bash
cd jarvis-provider && ./gradlew buildAllPlugins
```

### Test with a local Jarvis build

1. Build the JAR locally.
2. Copy it to your device or emulator:
   ```bash
   adb push jarvis-provider/plugins/vaplayer-provider/build/libs/vaplayer-provider-1.0.0.jar \
       /sdcard/Android/data/com.vigneshpai.jarvis/files/plugins/
   ```
3. In Jarvis, go to **Settings → Plugins → Load from file** and select the JAR.

### Validate the manifest locally

```bash
./jarvis-provider/scripts/generate-manifest.sh vaplayer 1.0.0 jarvis-provider/plugins/vaplayer-provider/build/libs/vaplayer-provider-1.0.0.jar
cat public/plugins.json
```

---

## API versioning & compatibility matrix

| API version | Breaking changes | Min Jarvis |
|---|---|---|
| 1 | Initial release | 2.0.0 |

When you need to make breaking changes to `provider-common`, increment `jarvis.targetApiVersion` in `gradle.properties` and update the compatibility table above.

Jarvis app refuses to load plugins whose `maxApiVersion` is below the app's current API version.

---

## `plugins.json` schema

```jsonc
{
  "repository_version": "1.0",
  "last_updated": "<ISO-8601>",
  "plugins": [
    {
      "id":             "provider.example",     // stable reverse-domain id
      "name":           "Example Provider",
      "version":        "1.0.0",
      "description":    "…",
      "author":         "Jarvis Team",
      "minApiVersion":  1,
      "maxApiVersion":  1,
      "minAppVersion":  "2.0.0",
      "supportedTypes": ["movie", "series"],
      "downloadUrl":    "https://github.com/…/releases/download/example-v1.0.0/example-provider-1.0.0.jar",
      "checksumSha256": "<sha256>",
      "changelog":      "…",
      "size_bytes":     512000,
      "last_updated":   "<ISO-8601>",
      "enabled":        true
    }
  ]
}
```

---

## Security

- All JARs are verified against `checksumSha256` before loading.
- Only HTTPS download URLs are accepted by the Jarvis runtime.
- The manifest is served over Vercel's CDN (HTTPS enforced).
- The `enabled` flag acts as a remote kill-switch for compromised plugin versions.

---

## Contributing

1. Fork this repository.
2. Add your provider under `jarvis-provider/plugins/<name>-provider/`.
3. Open a pull request — CI will validate the build.
4. Once merged, a maintainer tags the release to trigger publishing.
