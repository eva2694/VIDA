package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.assist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.DepthEstimator
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TTSManager
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.YoloModelLoader

/**
 * Factory for creating AssistViewModel instances.
 * Provides dependencies required for object detection, depth estimation, and TTS functionality.
 *
 * @param context Application context for resource access
 * @param yoloModelLoader Loader for the YOLO object detection model
 */
class AssistViewModelFactory(
    private val context: Context,
    private val yoloModelLoader: YoloModelLoader
) : ViewModelProvider.Factory {

    // Lazy initialization of depth estimator
    private val depthEstimator: DepthEstimator by lazy {
        DepthEstimator(context.applicationContext)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AssistViewModel::class.java)) {
            val ttsInstance = TTSManager.getInstance(context.applicationContext)
            return AssistViewModel(
                yoloModelLoader = yoloModelLoader,
                depthEstimator = depthEstimator,
                tts = ttsInstance,
                context = context.applicationContext
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}