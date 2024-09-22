package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.ImageProcessor
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.ModelLoader
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TextToSpeech

class ExploreViewModelFactory(
    private val modelLoader: ModelLoader,
    private val imageProcessor: ImageProcessor,
    private val tts: TextToSpeech
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ExploreViewModel::class.java)) {
            return ExploreViewModel(modelLoader, imageProcessor, tts) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

