package si.uni_lj.fe.diplomsko_delo.pomocnik.util

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

/**
 * Helper class for managing UI language settings.
 * Handles language changes for both modern (Android 13+) and legacy Android versions.
 */
class UILangHelper {
    /**
     * Changes the application's UI language.
     * @param context Application context
     * @param lang Language code (e.g., "sl" for Slovenian, "en" for English)
     */
    fun changeUILanguage(context: Context, lang: String) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(lang)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        }
    }

    /**
     * Retrieves the current UI language setting.
     * @param context Application context
     * @return Current language code (e.g., "sl" or "en")
     */
    fun getUILanguage(context: Context): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales[0].toLanguageTag()
                .split("-").first().toString()
        } else {
            AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()?.split("-")?.first()
                .toString()
        }
    }
}