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
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.AppImageProcessor
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.YoloModelLoader
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.tts.AppTextToSpeech

/**
 * ViewModel for the object detection screen.
 * Processes camera images to detect objects and provides audio feedback.
 */
class ExploreViewModel(
    private val yoloModelLoader: YoloModelLoader,
    private val appImageProcessor: AppImageProcessor,
    val tts: AppTextToSpeech

) : ViewModel() {

    var detectionResults by mutableStateOf<List<BoundingBox>>(emptyList())
        private set

    /**
     * Processes a camera image to detect objects.
     */
    fun processImage(imageProxy: ImageProxy) {
        viewModelScope.launch(Dispatchers.IO) {
            val results = appImageProcessor.processImage(imageProxy, yoloModelLoader)
            updateDetectionResults(results)
            imageProxy.close()
        }
    }

    private fun updateDetectionResults(results: List<BoundingBox>) {
        detectionResults = results
    }

    /**
     * Provides audio feedback for detected objects.
     */
    fun speak(text: String) {
        viewModelScope.launch {
            tts.readText(text)
        }
    }

    fun stopReading() {
        tts.stop()
    }
}
