package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.settings

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.LanguageChangeHelper
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager

class SettingsViewModel(val context: Context, private val preferencesManager: PreferencesManager) : ViewModel() {

    private val languageChangeHelper = LanguageChangeHelper()

    private val _language = MutableLiveData<String>().apply {
        value = languageChangeHelper.getLanguageCode(context)
    }

    val language: LiveData<String> get() = _language

    val readingSpeed: Flow<Float> = preferencesManager.readingSpeed
    val isDarkMode: Flow<Boolean> = preferencesManager.isDarkMode

    private val _languageChanged = MutableStateFlow(false)
    private val _readingSpeedChanged = MutableStateFlow(false)
    private val _darkModeChanged = MutableStateFlow(false)

    val languageChanged: Flow<Boolean> get() = _languageChanged
    val readingSpeedChanged: Flow<Boolean> get() = _readingSpeedChanged
    val darkModeChanged: Flow<Boolean> get() = _darkModeChanged

    fun setLanguageChanged(language: String) {
        viewModelScope.launch {
            _languageChanged.value = true
            _language.value = language
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