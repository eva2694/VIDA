@file:Suppress("DEPRECATION")

package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore


import android.content.Context
import android.graphics.Paint
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import si.uni_lj.fe.diplomsko_delo.pomocnik.R
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.AppImageProcessor
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.YoloModelLoader
import java.util.concurrent.ExecutorService


/**
 * Screen that displays object detection results from the camera feed.
 * Shows bounding boxes around detected objects and provides audio feedback.
 */
@Composable
fun ExploreScreen(
    cameraExecutor: ExecutorService,
    yoloModelLoader: YoloModelLoader,
    appImageProcessor: AppImageProcessor
) {
    val context = LocalContext.current
    val viewModelFactory = remember {
        ExploreViewModelFactory(context, yoloModelLoader, appImageProcessor)
    }
    val viewModel: ExploreViewModel = viewModel(factory = viewModelFactory)

    val displayRotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        .defaultDisplay.rotation

    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val detectionResults = viewModel.detectionResults
  
    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(factory = { ctx ->
            val previewView = PreviewView(ctx)
            val preview = Preview.Builder().build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(displayRotation)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
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

        Box(modifier = Modifier.fillMaxSize()) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                detectionResults.forEach { result ->
                    val x1 = result.x1 * canvasWidth
                    val y1 = result.y1 * canvasHeight
                    val x2 = result.x2 * canvasWidth
                    val y2 = result.y2 * canvasHeight

                    val boxColor = Color(0xFFFFFF00)
                    val strokeWidth = 6f

                    drawRect(
                        color = boxColor,
                        topLeft = Offset(x1, y1),
                        size = androidx.compose.ui.geometry.Size(
                            width = (x2 - x1) * 1.333F,
                            height = y2 - y1
                        ),
                        style = Stroke(width = strokeWidth)
                    )

                    val text = "${result.clsName}: ${"%.0f".format(result.cnf * 100)}%"

                    drawContext.canvas.nativeCanvas.apply {
                        drawText(
                            text,
                            x1,
                            y1 - 10,
                            Paint().apply {
                                color = android.graphics.Color.YELLOW
                                textSize = 45f
                                style = android.graphics.Paint.Style.FILL
                            }
                        )
                    }
                }
            }

            LaunchedEffect(detectionResults) {
                if (detectionResults.isNotEmpty()) {
                    detectionResults.forEach { result ->
                        val text = result.clsName

                        if (result.cnf > 0.3) {
                            viewModel.speak(text)
                        }
                    }
                }
            }

            Button(
                onClick = { viewModel.stopReading() },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Text(text = stringResource(R.string.stop_reading))
            }
        }
    }
}





