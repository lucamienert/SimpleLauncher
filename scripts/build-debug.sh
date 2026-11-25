#!/usr/bin/env bash
set -Eeuo pipefail
cd "$(dirname "$0")/.."

echo ">>> Building Debug APK..."
./gradlew --no-daemon --no-scan --console=plain assembleDebug

cp app/build/outputs/apk/debug/app-debug.apk SimpleLauncher-DEBUG.apk
echo ">>> Debug APK ready"
