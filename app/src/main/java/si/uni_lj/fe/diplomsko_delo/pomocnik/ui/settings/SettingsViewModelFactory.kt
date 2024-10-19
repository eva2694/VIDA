package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager


class SettingsViewModelFactory(
    private val preferencesManager: PreferencesManager,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
            return SettingsViewModel(context, preferencesManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}