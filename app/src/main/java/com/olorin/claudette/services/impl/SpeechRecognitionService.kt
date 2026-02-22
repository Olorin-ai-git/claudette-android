package com.olorin.claudette.services.impl

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import java.util.Locale

class SpeechRecognitionService(
    private val context: Context,
    private val onTranscriptFinalized: (String) -> Unit
) {

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _currentTranscript = MutableStateFlow("")
    val currentTranscript: StateFlow<String> = _currentTranscript.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null

    fun toggleListening() {
        if (_isListening.value) {
            stopListening()
        } else {
            startListening()
        }
    }

    fun startListening() {
        if (_isListening.value) return

        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Timber.e("Speech recognition is not available on this device")
            return
        }

        val recognizer = SpeechRecognizer.createSpeechRecognizer(context)
        recognizer.setRecognitionListener(createRecognitionListener())

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.US.toLanguageTag())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer = recognizer
        recognizer.startListening(intent)
        _isListening.value = true
        _currentTranscript.value = ""
        Timber.i("Speech recognition started")
    }

    fun stopListening() {
        if (!_isListening.value) return

        speechRecognizer?.stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null

        val transcript = _currentTranscript.value.trim()
        if (transcript.isNotEmpty()) {
            Timber.i("Finalizing transcript: %s...", transcript.take(50))
            onTranscriptFinalized(transcript)
        }

        _currentTranscript.value = ""
        _isListening.value = false
        Timber.i("Speech recognition stopped")
    }

    private fun createRecognitionListener(): RecognitionListener {
        return object : RecognitionListener {

            override fun onReadyForSpeech(params: Bundle?) {
                Timber.d("Speech recognizer ready")
            }

            override fun onBeginningOfSpeech() {
                Timber.d("Speech detected")
            }

            override fun onRmsChanged(rmsdB: Float) {
                // Audio level change, no action needed
            }

            override fun onBufferReceived(buffer: ByteArray?) {
                // Raw audio buffer, no action needed
            }

            override fun onEndOfSpeech() {
                Timber.d("End of speech detected")
            }

            override fun onError(error: Int) {
                val errorMessage = mapSpeechError(error)
                Timber.e("Speech recognition error: %s (code %d)", errorMessage, error)

                if (_isListening.value) {
                    _isListening.value = false
                    speechRecognizer?.destroy()
                    speechRecognizer = null
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                val bestResult = matches?.firstOrNull()
                if (!bestResult.isNullOrBlank()) {
                    _currentTranscript.value = bestResult

                    val transcript = bestResult.trim()
                    if (transcript.isNotEmpty()) {
                        Timber.i("Final transcript: %s...", transcript.take(50))
                        onTranscriptFinalized(transcript)
                    }
                }

                _currentTranscript.value = ""
                _isListening.value = false
                speechRecognizer?.destroy()
                speechRecognizer = null
            }

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults
                    ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)

                val bestPartial = matches?.firstOrNull()
                if (!bestPartial.isNullOrBlank()) {
                    _currentTranscript.value = bestPartial
                }
            }

            override fun onEvent(eventType: Int, params: Bundle?) {
                // Reserved for future use
            }
        }
    }

    companion object {
        private fun mapSpeechError(errorCode: Int): String {
            return when (errorCode) {
                SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
                SpeechRecognizer.ERROR_CLIENT -> "Client-side error"
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> "Network error"
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
                SpeechRecognizer.ERROR_NO_MATCH -> "No speech match found"
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
                SpeechRecognizer.ERROR_SERVER -> "Server error"
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech input timeout"
                else -> "Unknown error"
            }
        }
    }
}
