package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.depth

import DepthViewModelFactory
import android.content.Context
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager
import java.util.concurrent.ExecutorService

@Composable
fun DepthScreen(cameraExecutor: ExecutorService, preferencesManager: PreferencesManager) {
    val context = LocalContext.current
    val viewModelFactory = remember {
        DepthViewModelFactory(context)
    }
    val viewModel: DepthViewModel = viewModel(factory = viewModelFactory)

    LaunchedEffect(Unit) {
        viewModel.setContext(context)
    }

    val displayRotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        .defaultDisplay.rotation

    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val depthBitmap by viewModel.depthBitmap

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
                viewModel.processImage(imageProxy, context)
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

        depthBitmap?.let { bitmap ->
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Depth Map",
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        }

        viewModel.centerDepthText.value?.let { depthText ->
            Text(
                text = depthText,
                fontSize = 20.sp,
                textAlign = TextAlign.Center,
                color = Color.Red,
                modifier = Modifier
                    .align(Alignment.Center)
            )
        }
    }
}