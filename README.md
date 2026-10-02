# PocketPathshala

PocketPathshala is an offline-first Android tutor and local classroom host. It
keeps textbook content, retrieval, quizzes, and answer generation on the
device. It does not require an account, cloud sync, or an internet connection.

## Setup

Prerequisites:

- Android Studio Ladybug (2024.2.1+) or newer
- JDK 17
- Android SDK 35+ (the project target is defined in `app/build.gradle.kts`)
- Android 8.0 / API 26 or newer

Build and test from the project directory:

```text
gradlew.bat :app:testDebugUnitTest
gradlew.bat :app:assembleDebug
```

Install the debug APK with Android Studio or `gradlew.bat :app:installDebug`.

## Airplane-mode hotspot demo

1. Install and open the app on the teacher phone.
2. Enable **Airplane mode**, then enable **Wi-Fi hotspot** (or a local Wi-Fi
   network). Keep mobile data and internet access disabled.
3. Open **Host**, choose **Import/setup**, and select a supported local file.
4. Wait for indexing and cached quizzes to finish, then tap **Start host**.
5. The teacher screen shows a local join URL and QR code. On each student
   device, join the same hotspot, scan the QR code (or enter the URL), and open
   the bundled browser client.

The host URL is local to the hotspot/LAN. No external service is contacted.
The QR code is generated on-device.

## Import formats

The Host setup flow accepts:

- PDF textbooks (`application/pdf`), with page-preserving extraction.
- UTF-8 plain text files (`text/plain` and compatible `text/*` providers).

Unsupported MIME types are rejected. Importing a file does not upload or
transmit it; the indexed pack remains in the local Room database.

## Answer-provider labels

The default provider is **Fallback reasoning**. It is deterministic,
evidence-only output grounded in retrieved pack excerpts and citations; it is
not Gemma. The app's provider boundary can support an on-device Gemma model
when a compatible model pack is installed, but this build does not claim that
Gemma is present. UI and browser responses keep these labels distinct.

## Benchmarks

Benchmark records use real elapsed-time measurements when a run is explicitly
started on a device. Until hardware runs are performed, the status is
**UNMEASURED**. This repository intentionally contains no invented startup,
OCR, retrieval, generation, memory, or hotspot-latency numbers.

## Inspiration and license

The project was inspired by
[`fengkiej/lentera`](https://github.com/fengkiej/lentera), released under the
MIT License. This project does not copy that repository's code. See the
repository and included source notices for their respective license terms.
