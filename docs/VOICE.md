# LENTERA 2.0 — VOICE TUTOR SUBSYSTEM

## Voice Subsystem Architecture
LENTERA 2.0 provides an offline voice interface using native Android Speech APIs (`SpeechRecognizer` and `TextToSpeech`):

* **STT (Speech-to-Text)**: Wraps Android native `SpeechRecognizer` configured for offline recognition.
* **TTS (Text-to-Speech)**: Wraps Android `TextToSpeech` engine for offline vocal responses.
* **Fallback Behavior**: If offline speech recognition is unavailable on a specific device, the UI displays a clear notification (*"Offline voice recognition isn't available for this language on this device. You can type instead."*) and seamlessly routes to text input.
* **Privacy Guard**: No raw audio is ever recorded or uploaded to cloud servers.
