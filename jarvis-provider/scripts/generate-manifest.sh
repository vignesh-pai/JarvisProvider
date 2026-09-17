#!/usr/bin/env bash
# generate-manifest.sh
#
# Updates public/plugins.json with metadata for a newly built plugin JAR.
#
# Usage:
#   ./scripts/generate-manifest.sh <provider-name> <version> <jar-path>
#
# Example:
#   ./scripts/generate-manifest.sh netflix 1.2.0 plugins/netflix-provider/build/libs/netflix-provider-1.2.0.jar
#
# Environment variables (optional overrides):
#   GITHUB_REPO  – GitHub repository in "owner/repo" form (default: vignesh-pai/JarvisProvider)
#   RELEASE_TAG  – Git tag of the release (default: <provider-name>-v<version>)

set -euo pipefail

# ── Arguments ────────────────────────────────────────────────────────────────
PROVIDER_NAME="${1:?Usage: $0 <provider-name> <version> <jar-path>}"
VERSION="${2:?Usage: $0 <provider-name> <version> <jar-path>}"
JAR_PATH="${3:?Usage: $0 <provider-name> <version> <jar-path>}"

GITHUB_REPO="${GITHUB_REPO:-vignesh-pai/JarvisProvider}"
RELEASE_TAG="${RELEASE_TAG:-${PROVIDER_NAME}-v${VERSION}}"

MANIFEST="public/plugins.json"
PLUGIN_ID="provider.${PROVIDER_NAME}"
TIMESTAMP="$(date -u +"%Y-%m-%dT%H:%M:%SZ")"

# ── Validate JAR exists ───────────────────────────────────────────────────────
if [[ ! -f "$JAR_PATH" ]]; then
  echo "❌ JAR not found: $JAR_PATH" >&2
  exit 1
fi

# ── Compute checksum & size ───────────────────────────────────────────────────
CHECKSUM="$(sha256sum "$JAR_PATH" | awk '{print $1}')"
SIZE_BYTES="$(wc -c < "$JAR_PATH" | tr -d ' ')"

DOWNLOAD_URL="https://github.com/${GITHUB_REPO}/releases/download/${RELEASE_TAG}/${PROVIDER_NAME}-provider-${VERSION}.jar"

echo "📦 Provider  : $PLUGIN_ID"
echo "🏷  Version   : $VERSION"
echo "🔗 URL        : $DOWNLOAD_URL"
echo "🔐 SHA-256    : $CHECKSUM"
echo "📏 Size       : $SIZE_BYTES bytes"

# ── Ensure jq is available ────────────────────────────────────────────────────
if ! command -v jq &>/dev/null; then
  echo "❌ jq is required but not installed." >&2
  exit 1
fi

# ── Update or insert plugin entry in plugins.json ────────────────────────────
DISPLAY_NAME="$(echo "$PROVIDER_NAME" | awk '{print toupper(substr($0,1,1)) substr($0,2)}') Provider"

PLUGIN_JSON=$(jq -n \
  --arg id          "$PLUGIN_ID" \
  --arg name        "$DISPLAY_NAME" \
  --arg version     "$VERSION" \
  --arg downloadUrl "$DOWNLOAD_URL" \
  --arg checksum    "$CHECKSUM" \
  --argjson size    "$SIZE_BYTES" \
  --arg timestamp   "$TIMESTAMP" \
  --arg providerName "$(echo "$PROVIDER_NAME" | awk '{print toupper(substr($0,1,1)) substr($0,2)}')" \
  '{
    id: $id,
    name: $name,
    version: $version,
    description: ($providerName + " streaming provider"),
    author: "Jarvis Team",
    minApiVersion: 1,
    maxApiVersion: 1,
    minAppVersion: "2.0.0",
    supportedTypes: ["movie", "series"],
    downloadUrl: $downloadUrl,
    checksumSha256: $checksum,
    changelog: ("Release " + $version),
    size_bytes: $size,
    last_updated: $timestamp,
    enabled: true
  }')

# Check whether this plugin id already exists in the manifest.
EXISTS=$(jq --arg id "$PLUGIN_ID" 'any(.plugins[]; .id == $id)' "$MANIFEST")

if [[ "$EXISTS" == "true" ]]; then
  # Update existing entry.
  jq --arg id "$PLUGIN_ID" \
     --argjson plugin "$PLUGIN_JSON" \
     --arg ts "$TIMESTAMP" \
     '(.plugins[] | select(.id == $id)) = $plugin | .last_updated = $ts' \
     "$MANIFEST" > /tmp/plugins_tmp.json
else
  # Append new entry.
  jq --argjson plugin "$PLUGIN_JSON" \
     --arg ts "$TIMESTAMP" \
     '.plugins += [$plugin] | .last_updated = $ts' \
     "$MANIFEST" > /tmp/plugins_tmp.json
fi

mv /tmp/plugins_tmp.json "$MANIFEST"

echo "✅ $MANIFEST updated successfully."
