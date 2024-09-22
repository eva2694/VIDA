package si.uni_lj.fe.diplomsko_delo.pomocnik.util

import android.content.Context
import android.util.Log
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale


@Suppress("DEPRECATION")
class TextToSpeech(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var isSpeaking = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("sl", "SI"))
            isInitialized = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!isInitialized) {
                Log.e("TextToSpeechUtil", "Language not supported")
            } else {

                tts?.setSpeechRate(0.75f)

                tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                    override fun onStart(utteranceId: String?) {
                        isSpeaking = true
                    }

                    override fun onDone(utteranceId: String?) {
                        isSpeaking = false
                    }

                    @Deprecated("Deprecated in Java")
                    override fun onError(utteranceId: String?) {
                        isSpeaking = false
                        Log.e("TextToSpeechUtil", "Error in TTS utterance")
                    }
                })
            }
        } else {
            Log.e("TextToSpeechUtil", "Initialization failed")
        }
    }

    fun speak(text: String) {
        if (isInitialized && !isSpeaking) {
            val params = HashMap<String, String>()
            params[TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID] = "utteranceId"
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, params)
        } else {
            Log.e("TextToSpeechUtil", "TTS not initialized or is already speaking")
        }
    }

    fun shutdown() {
        tts?.shutdown()
    }
}