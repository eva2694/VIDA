package si.uni_lj.fe.diplomsko_delo.pomocnik.util.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale


@Suppress("DEPRECATION")
class AppTextToSpeech(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val textQueue = mutableListOf<String>()

    private var pendingLanguage: String? = null
    private var pendingSpeechRate: Float? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isInitialized = true
            setUtteranceProgressListener()

            Log.d("TextToSpeechUtil", "TTS initialized")

            // Apply pending settings
            pendingLanguage?.let {
                applyLanguage(it)
                pendingLanguage = null
            }

            pendingSpeechRate?.let {
                applySpeechRate(it)
                pendingSpeechRate = null
            }
        } else {
            Log.e("TextToSpeechUtil", "Initialization failed")
        }
    }

    private fun setUtteranceProgressListener() {
        tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
            }

            override fun onDone(utteranceId: String?) {
                processNextInQueue()
            }
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                Log.e("TextToSpeechUtil", "Error in TTS utterance")
                processNextInQueue()
            }
        })
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun queueSpeak(text: String) {
        if (isInitialized) {
            if (text !in textQueue) {
                textQueue.add(text)
            }
            GlobalScope.launch {
                processNextInQueue()
            }
        } else {
            Log.e("TextToSpeechUtil", "TTS not initialized or is already speaking")
        }
    }

    private fun processNextInQueue() {
        if (textQueue.isNotEmpty() && !isSpeaking()) {
            val nextText = textQueue.removeAt(0)
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "utteranceId"
            tts?.speak(nextText, TextToSpeech.QUEUE_FLUSH, params)
        }
    }

    private fun isSpeaking(): Boolean {
        return tts?.isSpeaking ?: false
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        isInitialized = false
        textQueue.clear()
    }

    fun stop() {
        textQueue.clear()
        tts?.stop()
    }

    fun readText(text: String) {
        if (isInitialized && !isSpeaking()) {
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "utteranceId"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        } else if (!isInitialized) {
            Log.e("TextToSpeechUtil", "TTS not initialized.")
        } else if (isSpeaking()) {
            Log.d("TextToSpeechUtil", "TTS speaking.")
        } else {
            Log.e("TextToSpeechUtil", "Cannot read, idk why...")
        }
    }

    fun setLanguage(languageCode: String) {
        if (!isInitialized) {
            pendingLanguage = languageCode
            Log.e("TextToSpeechUtil", "TTS not initialized. Will apply language later.")
            return
        }
        applyLanguage(languageCode)
    }

    fun setSpeechRate(rate: Float) {
        if (!isInitialized) {
            pendingSpeechRate = rate
            Log.e("TextToSpeechUtil", "TTS not initialized. Will apply speech rate later.")
            return
        }
        applySpeechRate(rate)
    }

    private fun applyLanguage(languageCode: String) {
        val locale = when (languageCode) {
            "sl" -> Locale("sl", "SI")
            "en" -> Locale.ENGLISH
            else -> Locale.getDefault()
        }

        val result = tts?.setLanguage(locale)
        if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
            Log.e("TextToSpeechUtil", "Language not supported: $languageCode")
        } else {
            Log.d("TextToSpeechUtil", "Language set to $languageCode")
        }
    }

    private fun applySpeechRate(rate: Float) {
        tts?.setSpeechRate(rate)
        Log.d("TextToSpeechUtil", "Speech rate set to $rate")
    }
}