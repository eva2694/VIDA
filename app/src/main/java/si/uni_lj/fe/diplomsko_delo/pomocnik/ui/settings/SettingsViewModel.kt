package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager

class SettingsViewModel(val context: Context, private val preferencesManager: PreferencesManager) : ViewModel() {

    val language: Flow<String> = preferencesManager.language
    val readingSpeed: Flow<Float> = preferencesManager.readingSpeed
    val isDarkMode: Flow<Boolean> = preferencesManager.isDarkMode

    private val _languageChanged = MutableStateFlow(false)
    private val _readingSpeedChanged = MutableStateFlow(false)
    private val _darkModeChanged = MutableStateFlow(false)

    val languageChanged: Flow<Boolean> get() = _languageChanged
    val readingSpeedChanged: Flow<Boolean> get() = _readingSpeedChanged
    val darkModeChanged: Flow<Boolean> get() = _darkModeChanged

    fun setLanguage(language: String) {
        viewModelScope.launch {
            preferencesManager.setLanguage(language)
            _languageChanged.value = true
        }
    }

    fun setReadingSpeed(speed: Float) {
        viewModelScope.launch {
            preferencesManager.setReadingSpeed(speed)
            _readingSpeedChanged.value = true
        }
    }

    fun setDarkMode(isDark: Boolean) {
        viewModelScope.launch {
            preferencesManager.setDarkMode(isDark)
            _darkModeChanged.value = true
        }
    }

    fun resetChangeFlags() {
        _languageChanged.value = false
        _readingSpeedChanged.value = false
        _darkModeChanged.value = false
    }
}