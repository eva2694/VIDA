package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.scene

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TTSManager

/**
 * Factory for creating SceneViewModel instances with required dependencies.
 */
class SceneViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SceneViewModel::class.java)) {
            val tts = TTSManager.getInstance(context)
            return SceneViewModel(context, tts) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
} 