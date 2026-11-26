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
import android.speech.tts.UtteranceProgressListener
import com.example.data.util.PakistaniFormatters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

object VoiceWakeCoordinator {
    private val _foregroundMicBusy = MutableStateFlow(false)
    val foregroundMicBusy: StateFlow<Boolean> = _foregroundMicBusy.asStateFlow()

    private val _isWakeServiceRunning = MutableStateFlow(false)
    val isWakeServiceRunning: StateFlow<Boolean> = _isWakeServiceRunning.asStateFlow()

    private val _wakeStatusMessage = MutableStateFlow("Off")
    val wakeStatusMessage: StateFlow<String> = _wakeStatusMessage.asStateFlow()

    fun setForegroundBusy(busy: Boolean) {
        _foregroundMicBusy.value = busy
    }

    fun setWakeServiceRunning(running: Boolean, status: String = if (running) "Listening for 'Hey Ice'" else "Off") {
        _isWakeServiceRunning.value = running
        _wakeStatusMessage.value = status
    }
}

data class SpeechUiState(
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val partialText: String = "",
    val finalText: String = "",
    val resultVersion: Long = 0L,
    val errorMessage: String? = null,
    val selectedLocaleTag: String = "ur-PK", // "ur-PK", "hi-IN", "en-IN"
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
    private val speechCallbacks = ConcurrentHashMap<String, () -> Unit>()

    init {
        tts = TextToSpeech(context.applicationContext) { status ->
            ttsReady = (status == TextToSpeech.SUCCESS)
            if (ttsReady) {
                tts?.setSpeechRate(0.98f)
                tts?.setPitch(1.0f)
                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        mainHandler.post {
                            _state.value = _state.value.copy(isSpeaking = true)
                        }
                    }

                    override fun onDone(utteranceId: String?) {
                        completeUtterance(utteranceId)
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        completeUtterance(utteranceId)
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        completeUtterance(utteranceId)
                    }
                })
                applyBestTtsLocale("en")
            }
        }
    }

    private fun completeUtterance(utteranceId: String?) {
        mainHandler.post {
            _state.value = _state.value.copy(isSpeaking = false)
            if (utteranceId != null) {
                val cb = speechCallbacks.remove(utteranceId)
                cb?.invoke()
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

        // Stop TTS if still speaking so mic never picks up assistant's own voice
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        speechCallbacks.clear()

        VoiceWakeCoordinator.setForegroundBusy(true)
        cancelRecognizerInternal()
        currentSessionId++
        val sessionId = currentSessionId
        resultDeliveredForSession = false

        _state.value = _state.value.copy(
            isListening = true,
            isSpeaking = false,
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
                VoiceWakeCoordinator.setForegroundBusy(false)
                val msg = when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH -> "No speech heard. Please speak clearly or tap mic."
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

        val lang = _state.value.selectedLocaleTag.takeIf { it.isNotBlank() && it != "auto" } ?: "ur-PK"
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, lang)
            putExtra(
                RecognizerIntent.EXTRA_SUPPORTED_LANGUAGES,
                arrayListOf("ur-PK", "hi-IN", "en-IN", "en-US")
            )
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        }
        try {
            recognizer.startListening(intent)
        } catch (e: Exception) {
            VoiceWakeCoordinator.setForegroundBusy(false)
            _state.value = _state.value.copy(
                isListening = false,
                errorMessage = "Could not start microphone: ${e.localizedMessage ?: "Try typing below"}"
            )
        }
    }

    private fun promoteTranscript(sessionId: Long, rawText: String) {
        if (sessionId != currentSessionId || resultDeliveredForSession) return
        val normalized = PakistaniFormatters.normalizeVoiceTranscript(rawText)
        VoiceWakeCoordinator.setForegroundBusy(false)
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
                } else {
                    VoiceWakeCoordinator.setForegroundBusy(false)
                }
            }
        }
        pendingFallbackRunnable = runnable
        mainHandler.postDelayed(runnable, delayMs)
    }

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
        speechCallbacks.clear()
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        VoiceWakeCoordinator.setForegroundBusy(false)
        currentSessionId++
        resultDeliveredForSession = false
        _state.value = _state.value.copy(
            isListening = false,
            isSpeaking = false,
            partialText = "",
            finalText = "",
            resultVersion = 0L,
            errorMessage = null
        )
    }

    fun speak(
        text: String,
        languageHint: String = "en",
        onFinished: (() -> Unit)? = null
    ) {
        val clean = text.trim()
        if (clean.isBlank()) {
            onFinished?.invoke()
            return
        }
        if (!ttsReady || tts == null) {
            onFinished?.invoke()
            return
        }

        applyBestTtsLocale(languageHint)
        val utteranceId = "brohi_voice_${System.currentTimeMillis()}"
        if (onFinished != null) {
            speechCallbacks[utteranceId] = onFinished
            // Safety fallback in case TTS engine never fires onDone
            val maxWaitMs = (clean.length * 95L).coerceIn(2_500L, 12_000L)
            mainHandler.postDelayed({
                if (speechCallbacks.containsKey(utteranceId)) {
                    completeUtterance(utteranceId)
                }
            }, maxWaitMs)
        }
        _state.value = _state.value.copy(isSpeaking = true)
        tts?.speak(clean, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    private fun applyBestTtsLocale(languageHint: String) {
        val engine = tts ?: return
        val candidates: List<Locale> = when (languageHint.lowercase(Locale.US)) {
            "ur" -> listOf(Locale("ur", "PK"), Locale("ur", "IN"), Locale("hi", "IN"), Locale("en", "IN"), Locale.US)
            "hi" -> listOf(Locale("hi", "IN"), Locale("ur", "PK"), Locale("en", "IN"), Locale.US)
            else -> listOf(Locale("en", "IN"), Locale.US, Locale.UK)
        }
        for (loc in candidates) {
            val res = engine.setLanguage(loc)
            if (res != TextToSpeech.LANG_MISSING_DATA && res != TextToSpeech.LANG_NOT_SUPPORTED) {
                return
            }
        }
    }

    fun stopSpeaking() {
        speechCallbacks.clear()
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
        _state.value = _state.value.copy(isSpeaking = false)
    }

    fun release() {
        cancelRecognizerInternal()
        speechCallbacks.clear()
        VoiceWakeCoordinator.setForegroundBusy(false)
        _state.value = _state.value.copy(isListening = false, isSpeaking = false)
        try {
            tts?.stop()
            tts?.shutdown()
        } catch (_: Exception) {
        }
        tts = null
    }
}
