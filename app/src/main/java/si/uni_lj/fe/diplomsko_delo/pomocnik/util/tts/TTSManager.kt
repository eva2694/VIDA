package si.uni_lj.fe.diplomsko_delo.pomocnik.util

import android.annotation.SuppressLint
import android.content.Context
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.tts.AppTextToSpeech

object TTSManager {
    @SuppressLint("StaticFieldLeak")
    private var instance: AppTextToSpeech? = null

    fun getInstance(context: Context): AppTextToSpeech {
        if (instance == null) {
            instance = AppTextToSpeech(context.applicationContext)
        }
        return instance!!
    }

    fun shutdown() {
        instance?.shutdown()
        instance = null
    }

    fun stop() {
        instance?.stop()
    }

    fun setLanguage(language: String) {
        instance?.setLanguage(language)
    }

    fun setSpeechRate(rate: Float) {
        instance?.setSpeechRate(rate)
    }

}
