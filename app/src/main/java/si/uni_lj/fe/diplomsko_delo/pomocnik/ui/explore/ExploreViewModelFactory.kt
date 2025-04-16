package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.AppImageProcessor
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TTSManager
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.YoloModelLoader

/**
 * Factory for creating ExploreViewModel instances with required dependencies.
 */
class ExploreViewModelFactory(
    private val context: Context,
    private val yoloModelLoader: YoloModelLoader,
    private val appImageProcessor: AppImageProcessor,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExploreViewModel::class.java)) {
            val tts = TTSManager.getInstance(context)
            return ExploreViewModel(yoloModelLoader, appImageProcessor, tts) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

