package com.dilshad.myapplication.domain.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import java.util.Locale

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    UNAVAILABLE
}

class VoiceEngine(
    private val context: Context,
    private val onTranscriptReceived: (String) -> Unit,
    private val onError: (String) -> Unit
) {
    var state: VoiceState = VoiceState.IDLE
        private set

    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var isTtsReady = false

    init {
        initTTS()
        initSTT()
    }

    private fun initTTS() {
        tts = TextToSpeech(context.applicationContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.US
                isTtsReady = true
            }
        }
    }

    private fun initSTT() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        state = VoiceState.LISTENING
                    }
                    override fun onBeginningOfSpeech() {}
                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}
                    override fun onEndOfSpeech() {
                        state = VoiceState.PROCESSING
                    }
                    override fun onError(error: Int) {
                        state = VoiceState.IDLE
                        onError("Speech recognition error ($error). You can type your question.")
                    }
                    override fun onResults(results: Bundle?) {
                        state = VoiceState.IDLE
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val text = matches?.firstOrNull()
                        if (!text.isNullOrBlank()) {
                            onTranscriptReceived(text)
                        } else {
                            onError("No speech detected. Try again or type.")
                        }
                    }
                    override fun onPartialResults(partialResults: Bundle?) {}
                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        } else {
            state = VoiceState.UNAVAILABLE
        }
    }

    fun startListening(language: String = "en-US") {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            state = VoiceState.UNAVAILABLE
            onError("Offline voice recognition isn't available for this language on this device. You can type instead.")
            return
        }

        try {
            stopSpeaking()
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            }
            speechRecognizer?.startListening(intent)
            state = VoiceState.LISTENING
        } catch (e: Exception) {
            state = VoiceState.IDLE
            onError("Voice input error: ${e.message ?: "Unable to access microphone"}")
        }
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        state = VoiceState.IDLE
    }

    fun speak(text: String, language: String = "English") {
        if (!isTtsReady) return
        val loc = when (language.lowercase()) {
            "hindi" -> Locale.forLanguageTag("hi-IN")
            "odia" -> Locale.forLanguageTag("or-IN")
            else -> Locale.US
        }
        tts?.language = loc
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "POCKETPATHSHALA_TTS")
        state = VoiceState.SPEAKING
    }

    fun stopSpeaking() {
        if (tts?.isSpeaking == true) {
            tts?.stop()
        }
        if (state == VoiceState.SPEAKING) {
            state = VoiceState.IDLE
        }
    }

    fun destroy() {
        speechRecognizer?.destroy()
        tts?.stop()
        tts?.shutdown()
    }
}
