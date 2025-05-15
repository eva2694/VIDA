package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager

/**
 * ViewModel for the settings screen.
 * Manages user preferences and provides methods to update them.
 */
class SettingsViewModel(
    private val preferencesManager: PreferencesManager
) : ViewModel() {

      val language: Flow<String> = preferencesManager.language
      val readingSpeed: Flow<Float> = preferencesManager.readingSpeed
      val isDarkMode: Flow<Boolean> = preferencesManager.isDarkMode
      val isHighContrast: Flow<Boolean> = preferencesManager.isHighContrast

      /**
       * Updates the app language.
       */
      fun setLanguage(language: String) {
            viewModelScope.launch {
                  preferencesManager.setLanguage(language)
                }
          }

      /**
       * Updates the text-to-speech reading speed.
       */
      fun setReadingSpeed(speed: Float) {
            viewModelScope.launch {
                  preferencesManager.setReadingSpeed(speed)
                }
          }

      /**
       * Updates the dark mode setting.
       */
      fun setDarkMode(isDark: Boolean) {
            viewModelScope.launch {
                  preferencesManager.setDarkMode(isDark)
                }
          }

      /**
       * Updates the high contrast setting.
       */
      fun setHighContrast(isHighContrast: Boolean) {
            viewModelScope.launch {
                  preferencesManager.setHighContrast(isHighContrast)
                }
          }
}