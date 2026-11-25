#!/usr/bin/env bash
set -Eeuo pipefail

./gradlew
./gradlew --no-daemon --no-scan --console=plain assembleDebug
# adb install -r app/build/outputs/apk/debug/app-debug.apk
