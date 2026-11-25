#!/usr/bin/env bash
set -Eeuo pipefail
cd "$(dirname "$0")/.."

echo ">>> Cleaning build artifacts and Gradle caches..."
./gradlew clean --no-daemon --console=plain

rm -f SimpleLauncher-*.apk

echo ">>> Clean finished successfully!"
