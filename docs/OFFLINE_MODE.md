# PocketPathshala — Offline-First Architecture & Privacy

## Offline Principles

1. **Zero Cloud Requirement**: After initial book download or local file selection, all core operations execute 100% locally on-device.
2. **Textbook Privacy**: Textbooks and user questions remain on the smartphone. No prompt telemetry, student data, or private textbook content is uploaded to any remote server.
3. **No Account Required**: The app operates without login, user accounts, or external subscriptions.

## On-Device Capabilities

* **Curriculum Browsing**: Full syllabus structure for NCERT Classes 6–10 is bundled and immediately searchable.
* **Text Extraction & Indexing**: PDF rendering and text parsing occur locally using Android `PdfRenderer` and on-device ML Kit Text Recognition.
* **Grounded Retrieval**: In-memory token overlap and Room database search run on SQLite without external indexers.
* **Grounded Reasoning**: On-device tutor pipeline delivers conversational guidance, step-by-step math solutions, and textbook citations.
* **Classroom P2P Host**: The teacher phone creates an offline Wi-Fi hotspot and hosts a local HTTP + WebSocket server ([`HostServer`](file:///e:/MyApplication/app/src/main/java/com/dilshad/myapplication/host/HostServer.kt)), enabling nearby student devices to connect and learn via their browser without internet access.
