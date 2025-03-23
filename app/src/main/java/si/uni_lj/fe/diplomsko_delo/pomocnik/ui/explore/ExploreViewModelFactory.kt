package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.ImageProcessor
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TTSManager
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.YoloModelLoader

class ExploreViewModelFactory(
    private val context: Context,
    private val yoloModelLoader: YoloModelLoader,
    private val imageProcessor: ImageProcessor,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExploreViewModel::class.java)) {
            val tts = TTSManager.getInstance(context)
            return ExploreViewModel(yoloModelLoader, imageProcessor, tts) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

