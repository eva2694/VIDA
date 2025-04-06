@file:Suppress("DEPRECATION")

package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.read

import android.content.Context
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import java.util.concurrent.ExecutorService


/**
 * Screen that provides text recognition from the camera feed.
 * Reads text aloud using text-to-speech.
 */
@Composable
fun ReadScreen(
    cameraExecutor: ExecutorService
) {
    val context = LocalContext.current
    val viewModelFactory = remember {
        ReadViewModelFactory(context)
    }
    val viewModel: ReadViewModel = viewModel(factory = viewModelFactory)
    val displayRotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        .defaultDisplay.rotation

    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            val previewView = PreviewView(ctx)
            val preview = Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(displayRotation)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                viewModel.processImage(imageProxy)
            }

            val cameraProvider = cameraProviderFuture.get()
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                CameraSelector.DEFAULT_BACK_CAMERA,
                preview,
                imageAnalysis
            )

            previewView
        }, modifier = Modifier.fillMaxSize())
    }
}