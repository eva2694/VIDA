package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.assist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.DepthEstimator
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TTSManager
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.YoloModelLoader

class AssistViewModelFactory(
    private val context: Context,
    private val yoloModelLoader: YoloModelLoader
) : ViewModelProvider.Factory {

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
                context = context.applicationContext,
                tts = ttsInstance
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}