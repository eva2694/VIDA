package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager


/**
 * Factory for creating SettingsViewModel instances with required dependencies.
 */
class SettingsViewModelFactory(
    private val preferencesManager: PreferencesManager,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}