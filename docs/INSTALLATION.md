# LENTERA 2.0 — INSTALLATION & BUILD GUIDE

## Prerequisites
* Android Studio Ladybug (2024.2.1+) or newer
* JDK 17
* Android SDK 37 (Build Tools 35.0.0 / 37.0.0)
* Target Device: iQOO Android smartphone (or Android 8.0+ / API 26+ device)

## Build Instructions
1. Open project in Android Studio or terminal.
2. Build debug APK:
   ```bash
   ./gradlew assembleDebug
   ```
3. Run tests:
   ```bash
   ./gradlew test
   ```
4. Deploy to connected iQOO device:
   ```bash
   ./gradlew installDebug
   ```
