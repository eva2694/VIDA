package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.read

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TTSManager

/**
 * Factory for creating ReadViewModel instances with required dependencies.
 */
class ReadViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReadViewModel::class.java)) {
            val tts = TTSManager.getInstance(context)
            return ReadViewModel(tts) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}