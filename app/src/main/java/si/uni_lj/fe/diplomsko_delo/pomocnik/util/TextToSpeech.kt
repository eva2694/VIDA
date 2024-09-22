package si.uni_lj.fe.diplomsko_delo.pomocnik.util

import android.content.Context
import android.util.Log
import android.speech.tts.TextToSpeech
import java.util.Locale


class TextToSpeech(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("sl", "SI"))
            isInitialized = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            if (!isInitialized) {
                Log.e("TextToSpeechUtil", "Language not supported")
            }
        } else {
            Log.e("TextToSpeechUtil", "Initialization failed")
        }
    }

    fun speak(text: String) {
        if (isInitialized) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, null)
        } else {
            Log.e("TextToSpeechUtil", "TTS not initialized")
        }
    }

    fun shutdown() {
        tts?.shutdown()
    }
}