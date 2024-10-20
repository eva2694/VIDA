package si.uni_lj.fe.diplomsko_delo.pomocnik.util

import android.content.Context
import android.util.Log
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.Locale


@Suppress("DEPRECATION")
class TextToSpeech(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private val textQueue = mutableListOf<String>()

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {

            val locale = Locale("sl", "SI")
            val result = tts?.setLanguage(locale)

            isInitialized = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!isInitialized) {
                Log.e("TextToSpeechUtil", "Language not supported")
            } else {
                tts?.setSpeechRate(0.70f)
                setUtteranceProgressListener()
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
        } else {
            Log.e("TextToSpeechUtil", "TTS not initialized or is currently speaking.")
        }
    }

    fun setLanguage(languageCode: String) {
        if (isInitialized) {
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
        } else {
            Log.e("TextToSpeechUtil", "TTS not initialized. Cannot set language.")
        }
    }

    fun setSpeechRate(rate: Float) {
        if (isInitialized) {
            tts?.setSpeechRate(rate)
            Log.d("TextToSpeechUtil", "Speech rate set to $rate")
        } else {
            Log.e("TextToSpeechUtil", "TTS not initialized. Cannot set speech rate.")
        }
    }
}