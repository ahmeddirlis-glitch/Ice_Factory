package com.example.data.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import com.example.data.util.PakistaniFormatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class SpeechUiState(
    val isListening: Boolean = false,
    val partialText: String = "",
    val finalText: String = "",
    val resultVersion: Long = 0L,
    val errorMessage: String? = null,
    val selectedLocaleTag: String = "ur-PK",
    val speechAvailable: Boolean = true
)

class VoiceSpeechManager(private val context: Context) {
    private val _state = MutableStateFlow(
        SpeechUiState(speechAvailable = SpeechRecognizer.isRecognitionAvailable(context))
    )
    val state: StateFlow<SpeechUiState> = _state.asStateFlow()

    private val mainHandler = Handler(Looper.getMainLooper())
    private var speechRecognizer: SpeechRecognizer? = null
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var currentSessionId: Long = 0L
    private var resultDeliveredForSession: Boolean = false
    private var pendingFallbackRunnable: Runnable? = null

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            if (ttsReady) {
                val res = tts?.setLanguage(Locale("en", "IN"))
                if (res == TextToSpeech.LANG_MISSING_DATA || res == TextToSpeech.LANG_NOT_SUPPORTED) {
                    tts?.language = Locale.US
                }
            }
        }
    }

    fun setLocaleTag(tag: String) {
        _state.value = _state.value.copy(selectedLocaleTag = tag)
    }

    fun startListening() {
        val available = SpeechRecognizer.isRecognitionAvailable(context)
        if (!available) {
            _state.value = _state.value.copy(
                speechAvailable = false,
                isListening = false,
                errorMessage = "Device speech recognizer is unavailable. Type your command below instead."
            )
            return
        }

        cancelRecognizerInternal()
        currentSessionId++
        val sessionId = currentSessionId
        resultDeliveredForSession = false

        _state.value = _state.value.copy(
            isListening = true,
            partialText = "",
            finalText = "",
            errorMessage = null
        )

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        speechRecognizer = recognizer

        recognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                if (sessionId != currentSessionId) return
                _state.value = _state.value.copy(
                    isListening = true,
                    errorMessage = null
                )
            }

            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                if (sessionId != currentSessionId) return
                _state.value = _state.value.copy(isListening = false)
                schedulePartialPromotionIfNeeded(sessionId, delayMs = 900L)
            }

            override fun onError(error: Int) {
                if (sessionId != currentSessionId) return
                pendingFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
                val capturedPartial = _state.value.partialText
                if (!resultDeliveredForSession && capturedPartial.isNotBlank()) {
                    promoteTranscript(sessionId, capturedPartial)
                    return
                }
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech heard. Please speak clearly or type below."
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Listening timed out. Tap mic to try again."
                    SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error."
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission is required."
                    else -> "Voice capture interrupted. You can also type your command."
                }
                _state.value = _state.value.copy(isListening = false, errorMessage = msg)
            }

            override fun onResults(results: Bundle?) {
                if (sessionId != currentSessionId) return
                pendingFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val rawTop = matches?.firstOrNull().orEmpty().ifBlank { _state.value.partialText }
                promoteTranscript(sessionId, rawTop)
            }

            override fun onPartialResults(partialResults: Bundle?) {
                if (sessionId != currentSessionId) return
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val rawTop = matches?.firstOrNull().orEmpty()
                if (rawTop.isNotBlank()) {
                    val normalized = PakistaniFormatters.normalizeVoiceTranscript(rawTop)
                    _state.value = _state.value.copy(partialText = normalized)
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, _state.value.selectedLocaleTag)
            putExtra(
                RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES,
                arrayListOf("ur-PK", "en-IN", "hi-IN", "en-US")
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isListening = false,
                errorMessage = "Could not start microphone: ${e.localizedMessage ?: "Try typing below"}"
            )
        }
    }

    private fun promoteTranscript(sessionId: Long, rawText: String) {
        if (sessionId != currentSessionId || resultDeliveredForSession) return
        val normalized = PakistaniFormatters.normalizeVoiceTranscript(rawText)
        if (normalized.isBlank()) {
            _state.value = _state.value.copy(isListening = false)
            return
        }
        resultDeliveredForSession = true
        _state.value = _state.value.copy(
            isListening = false,
            finalText = normalized,
            partialText = normalized,
            errorMessage = null,
            resultVersion = _state.value.resultVersion + 1L
        )
    }

    private fun schedulePartialPromotionIfNeeded(sessionId: Long, delayMs: Long) {
        pendingFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
        val runnable = Runnable {
            if (sessionId == currentSessionId && !resultDeliveredForSession) {
                val partial = _state.value.partialText
                if (partial.isNotBlank()) {
                    promoteTranscript(sessionId, partial)
                }
            }
        }
        pendingFallbackRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

    /**
     * Stops active microphone capture WITHOUT destroying the recognizer,
     * allowing onResults (or partial-result promotion fallback) to complete cleanly.
     */
    fun stopListening() {
        val sessionId = currentSessionId
        try {
            speechRecognizer?.stopListening()
        } catch (_: Exception) {
        }
        _state.value = _state.value.copy(isListening = false)
        schedulePartialPromotionIfNeeded(sessionId, delayMs = 700L)
    }

    private fun cancelRecognizerInternal() {
        pendingFallbackRunnable?.let { mainHandler.removeCallbacks(it) }
        pendingFallbackRunnable = null
        try {
            speechRecognizer?.cancel()
            speechRecognizer?.destroy()
        } catch (_: Exception) {
        }
        speechRecognizer = null
    }

    fun clearResult() {
        cancelRecognizerInternal()
        currentSessionId++
        resultDeliveredForSession = false
        _state.value = _state.value.copy(
            isListening = false,
            partialText = "",
            finalText = "",
            resultVersion = 0L,
            errorMessage = null
        )
    }

    fun speak(text: String) {
        if (ttsReady && text.isNotBlank()) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "brohi_voice_${System.currentTimeMillis()}")
        }
    }

    fun release() {
        cancelRecognizerInternal()
        _state.value = _state.value.copy(isListening = false)
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
    }
}
