package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.depth

import DepthViewModelFactory
import android.content.Context
import android.util.Log
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager
import java.util.concurrent.ExecutorService

/**
 * Screen that displays depth information from the camera feed.
 * Shows a grayscale depth map overlay and text with depth information.
 */
@Composable
fun DepthScreen(cameraExecutor: ExecutorService, preferencesManager: PreferencesManager) {
    val context = LocalContext.current
    val viewModelFactory = remember {
        DepthViewModelFactory(context)
    }
    val viewModel: DepthViewModel = viewModel(factory = viewModelFactory)

    val displayRotation = remember {
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.rotation
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val depthBitmap by viewModel.depthBitmap
    val centerDepthText by viewModel.centerDepthText

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
                viewModel.processImage(imageProxy, ctx)
            }

            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("DepthScreen", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }, modifier = Modifier.fillMaxSize())

        depthBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Depth Map Visualization",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                alpha = 0.8f
            )
        }

        centerDepthText?.let { depthText ->
            Text(
                text = depthText,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.Center)
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(8.dp)
            )
        }
    }
}