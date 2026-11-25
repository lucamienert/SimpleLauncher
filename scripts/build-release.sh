#!/usr/bin/env bash
set -Eeuo pipefail
cd "$(dirname "$0")/.."

VERSION="${1:-v1.0.0}"
echo ">>> Building Release APK ($VERSION)..."
./gradlew --no-daemon --no-scan --console=plain clean assembleRelease

OUTPUT_NAME="SimpleLauncher-${VERSION}.apk"
cp app/build/outputs/apk/release/app-release.apk "$OUTPUT_NAME"
echo ">>> Done! Created: $OUTPUT_NAME"
ls -lh "$OUTPUT_NAME"
