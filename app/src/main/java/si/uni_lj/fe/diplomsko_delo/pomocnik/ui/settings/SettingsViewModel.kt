package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager

class SettingsViewModel(private val preferencesManager: PreferencesManager) : ViewModel() {

    val language: Flow<String> = preferencesManager.language
    val readingSpeed: Flow<Float> = preferencesManager.readingSpeed
    val isDarkMode: Flow<Boolean> = preferencesManager.isDarkMode

    fun setLanguage(language: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(language)
        }
    }

    fun setReadingSpeed(speed: Float) {
        viewModelScope.launch {
            preferencesManager.setReadingSpeed(speed)
        }
    }

    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(isDark)
        }
    }
}