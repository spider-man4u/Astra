package com.spider.astra.voice

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import androidx.core.content.ContextCompat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

data class VoiceUiState(
    val recognitionAvailable: Boolean = false,
    val ttsReady: Boolean = false,
    val isListening: Boolean = false,
    val isSpeaking: Boolean = false,
    val partialTranscript: String = "",
    val transcript: String? = null,
    val errorMessage: String? = null
)

object VoiceLanguages {
    const val ENGLISH_INDIA = "en-IN"
    const val HINDI_INDIA = "hi-IN"

    fun normalize(languageTag: String?): String =
        if (languageTag == HINDI_INDIA) HINDI_INDIA else ENGLISH_INDIA
}

/**
 * Foreground-only speech input and output.
 * Recognition begins only after a UI tap and RECORD_AUDIO permission.
 */
class AstraVoiceController(context: Context) {
    private val appContext = context.applicationContext
    private val _state = MutableStateFlow(
        VoiceUiState(recognitionAvailable = SpeechRecognizer.isRecognitionAvailable(appContext))
    )
    val state: StateFlow<VoiceUiState> = _state.asStateFlow()

    private var recognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsInitialized = false
    private var released = false

    init {
        textToSpeech = TextToSpeech(appContext) { status ->
            if (released) return@TextToSpeech
            val engine = textToSpeech
            if (status == TextToSpeech.SUCCESS && engine != null) {
                engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        _state.value = _state.value.copy(isSpeaking = true)
                    }

                    override fun onDone(utteranceId: String?) {
                        _state.value = _state.value.copy(isSpeaking = false)
                    }

                    override fun onError(utteranceId: String?) {
                        _state.value = _state.value.copy(
                            isSpeaking = false,
                            errorMessage = "ASTRA could not speak that reply."
                        )
                    }

                    override fun onError(utteranceId: String?, errorCode: Int) {
                        onError(utteranceId)
                    }
                })

                val result = engine.setLanguage(Locale.forLanguageTag(VoiceLanguages.ENGLISH_INDIA))
                ttsInitialized = result >= TextToSpeech.LANG_AVAILABLE
                _state.value = _state.value.copy(
                    ttsReady = ttsInitialized,
                    errorMessage = if (ttsInitialized) null
                    else "English (India) speech data is unavailable. Check Android Text-to-speech settings."
                )
            } else {
                _state.value = _state.value.copy(
                    ttsReady = false,
                    errorMessage = "No text-to-speech engine is ready on this device."
                )
            }
        }
    }

    fun startListening(languageTag: String) {
        if (released) return
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            reportError("Microphone permission is required. Tap the microphone and allow access.")
            return
        }
        if (!SpeechRecognizer.isRecognitionAvailable(appContext)) {
            _state.value = _state.value.copy(
                recognitionAvailable = false,
                errorMessage = "No speech recognition service is installed or enabled."
            )
            return
        }

        val speechRecognizer = try {
            recognizer ?: createRecognizer().also { recognizer = it }
        } catch (_: Exception) {
            reportError("ASTRA could not start the speech recognition service.")
            return
        }

        _state.value = _state.value.copy(
            recognitionAvailable = true,
            isListening = true,
            partialTranscript = "",
            transcript = null,
            errorMessage = null
        )

        val language = VoiceLanguages.normalize(languageTag)
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, language)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, appContext.packageName)
        }

        try {
            speechRecognizer.cancel()
            speechRecognizer.startListening(intent)
        } catch (_: Exception) {
            _state.value = _state.value.copy(
                isListening = false,
                errorMessage = "Speech recognition could not start. Please try again."
            )
        }
    }

    fun stopListening() {
        if (_state.value.isListening) runCatching { recognizer?.stopListening() }
    }

    fun speak(text: String, languageTag: String) {
        if (released || text.isBlank()) return
        val engine = textToSpeech
        if (!ttsInitialized || engine == null) {
            reportError("The speech engine is still starting or unavailable. Try again in a moment.")
            return
        }

        val locale = Locale.forLanguageTag(VoiceLanguages.normalize(languageTag))
        val languageResult = engine.setLanguage(locale)
        if (languageResult == TextToSpeech.LANG_MISSING_DATA ||
            languageResult == TextToSpeech.LANG_NOT_SUPPORTED
        ) {
            reportError("The selected speech voice is unavailable. Install its voice data in Android Text-to-speech settings.")
            return
        }

        val utteranceId = "astra-" + System.currentTimeMillis()
        val options = Bundle().apply {
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        val result = engine.speak(
            text.take(MAX_SPOKEN_CHARACTERS),
            TextToSpeech.QUEUE_FLUSH,
            options,
            utteranceId
        )
        if (result == TextToSpeech.ERROR) {
            reportError("Android's text-to-speech engine rejected the reply.")
        } else {
            _state.value = _state.value.copy(isSpeaking = true, errorMessage = null)
        }
    }

    fun stopSpeaking() {
        runCatching { textToSpeech?.stop() }
        _state.value = _state.value.copy(isSpeaking = false)
    }

    fun clearTranscript() {
        _state.value = _state.value.copy(transcript = null)
    }

    fun reportError(message: String) {
        _state.value = _state.value.copy(
            isListening = false,
            isSpeaking = false,
            errorMessage = message
        )
    }

    fun clearError() {
        _state.value = _state.value.copy(errorMessage = null)
    }

    fun release() {
        if (released) return
        released = true
        runCatching { recognizer?.cancel() }
        runCatching { recognizer?.destroy() }
        recognizer = null
        runCatching { textToSpeech?.stop() }
        runCatching { textToSpeech?.shutdown() }
        textToSpeech = null
        ttsInitialized = false
        _state.value = VoiceUiState()
    }

    private fun createRecognizer(): SpeechRecognizer {
        val instance = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            SpeechRecognizer.isOnDeviceRecognitionAvailable(appContext)
        ) {
            SpeechRecognizer.createOnDeviceSpeechRecognizer(appContext)
        } else {
            SpeechRecognizer.createSpeechRecognizer(appContext)
        }

        instance.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                _state.value = _state.value.copy(isListening = true, partialTranscript = "", errorMessage = null)
            }
            override fun onBeginningOfSpeech() {
                _state.value = _state.value.copy(isListening = true)
            }
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() {
                _state.value = _state.value.copy(isListening = false)
            }
            override fun onError(error: Int) {
                _state.value = _state.value.copy(
                    isListening = false,
                    errorMessage = errorMessageFor(error)
                )
            }
            override fun onResults(results: Bundle?) {
                val spokenText = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    ?.trim()
                _state.value = _state.value.copy(
                    isListening = false,
                    partialTranscript = "",
                    transcript = spokenText?.takeIf { it.isNotBlank() },
                    errorMessage = if (spokenText.isNullOrBlank()) "I didn't catch that. Please try again." else null
                )
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val partial = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    ?.firstOrNull()
                    .orEmpty()
                _state.value = _state.value.copy(partialTranscript = partial)
            }
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
        })
        return instance
    }

    private fun errorMessageFor(error: Int): String = when (error) {
        SpeechRecognizer.ERROR_AUDIO -> "Microphone audio error. Check your mic and try again."
        SpeechRecognizer.ERROR_CLIENT -> "Speech recognition was interrupted. Try again."
        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Microphone permission was denied."
        SpeechRecognizer.ERROR_NETWORK -> "Speech recognition network error."
        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Speech recognition timed out while connecting."
        SpeechRecognizer.ERROR_NO_MATCH -> "I didn't catch those words. Please try again."
        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Speech service is busy. Wait a moment and retry."
        SpeechRecognizer.ERROR_SERVER -> "The speech recognition service returned an error."
        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech was detected. Tap the mic and speak."
        else -> "Speech recognition failed (code " + error + ")."
    }

    companion object {
        private const val MAX_SPOKEN_CHARACTERS = 1800
    }
}
