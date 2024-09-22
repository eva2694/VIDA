package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore

import androidx.camera.core.ImageProxy
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import si.uni_lj.fe.diplomsko_delo.pomocnik.models.BoundingBox
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.ImageProcessor
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.ModelLoader
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TextToSpeech

class ExploreViewModel(
    private val modelLoader: ModelLoader,
    private val imageProcessor: ImageProcessor,
    private val tts: TextToSpeech
) : ViewModel() {

    var detectionResults by mutableStateOf<List<BoundingBox>>(emptyList())
        private set

    fun processImage(imageProxy: ImageProxy) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = imageProcessor.processImage(imageProxy, modelLoader)
            updateDetectionResults(results)
            imageProxy.close()
        }
    }

    private fun updateDetectionResults(results: List<BoundingBox>) {
        detectionResults = results
    }


    fun speak(text: String) {
        viewModelScope.launch {
            tts.speak(text)
        }
    }

    override fun onCleared() {
        super.onCleared()
        tts.shutdown()
    }
}
