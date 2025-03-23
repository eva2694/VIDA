package si.uni_lj.fe.diplomsko_delo.pomocnik.util

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
// Language of UI components / which strings.xml will be used
class UILangHelper {
    fun changeUILanguage(context: Context, lang: String) {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales = LocaleList.forLanguageTags(lang)
        } else {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang))
        }
    }

    // just in case I need this
    fun getUILanguage(context: Context): String {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return context.getSystemService(LocaleManager::class.java).applicationLocales[0].toLanguageTag().split("-").first().toString()
        } else {
            return AppCompatDelegate.getApplicationLocales()[0]?.toLanguageTag()?.split("-")?.first().toString()
        }
    }
}