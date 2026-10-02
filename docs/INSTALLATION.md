# PocketPathshala — Installation & Verification Guide

## Prerequisites

* **Android Studio**: Ladybug (2024.2.1+) or newer
* **JDK**: 17 or higher
* **Android SDK**: Compile SDK 37, Target SDK 35, Min SDK 26 (Android 8.0+)
* **Hardware**: Any modern Android smartphone or tablet (optimized for devices with 4GB+ RAM)

## Command-Line Build & Test

From the root project directory:

```bash
# 1. Run complete unit test suite
.\gradlew.bat testDebugUnitTest

# 2. Build debug APK
.\gradlew.bat assembleDebug

# 3. Install to connected Android device via ADB
.\gradlew.bat installDebug
```

## Running Acceptance Verification

To run unit tests across curriculum, retrieval, AI orchestration, and host server subsystems:

```bash
.\gradlew.bat :app:testDebugUnitTest --tests "*Curriculum*" --tests "*AIOrchestrator*" --tests "*HostServer*"
```
