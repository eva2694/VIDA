package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore


import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.Paint
import android.util.Log
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import si.uni_lj.fe.diplomsko_delo.pomocnik.models.BoundingBox
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.ModelLoader
import java.util.concurrent.ExecutorService



@Composable
fun ExploreScreen(cameraExecutor: ExecutorService) {
    val context = LocalContext.current

    val displayRotation = (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
        .defaultDisplay.rotation

    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var detectionResults by remember { mutableStateOf<List<BoundingBox>>(listOf()) }

    val modelLoader = remember { ModelLoader(context) }

    // maybe put it in a box to really fill max size?
    AndroidView(factory = { ctx ->
        val previewView = PreviewView(ctx)
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }

        val imageAnalysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetRotation(displayRotation)
            //.setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888) // ! this has less overhead but needs conversion. TBD!
            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
            .build()

        imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
            processImage(imageProxy, modelLoader) { results ->
                detectionResults = results
            }
        }

        val cameraProvider = cameraProviderFuture.get()
        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageAnalysis)

        previewView
    }, modifier = Modifier.fillMaxSize())

    Box(modifier = Modifier.fillMaxSize()) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            detectionResults.forEach { result ->
                val boxColor = Color(0xFFFFFF00)
                val strokeWidth = 5f

                drawRect(
                    color = boxColor,
                    topLeft = Offset(result.x1, result.y1),
                    size = androidx.compose.ui.geometry.Size(
                        width = result.x2 - result.x1,
                        height = result.y2 - result.y1
                    ),
                    style = Stroke(width = strokeWidth)
                )

                val text = "${result.clsName}: ${"%.2f".format(result.cnf)}"

                drawContext.canvas.nativeCanvas.apply {
                    drawText(
                        text,
                        result.x1,
                        result.y1 - 10,
                        Paint().apply {
                            color = android.graphics.Color.GREEN
                            textSize = 40f
                            style = android.graphics.Paint.Style.FILL
                        }
                    )
                }
            }
        }
    }
}

private fun processImage(imageProxy: ImageProxy, modelLoader: ModelLoader, onResults: (List<BoundingBox>) -> Unit) {
    val bitmap = imageProxy.toBitmapFromRGBA8888() ?: run {
        Log.e("ImageAnalysis", "Bitmap conversion failed")
        imageProxy.close()
        return
    }

    val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)

    CoroutineScope(Dispatchers.IO).launch {
        val results = modelLoader.detect(rotatedBitmap)
        onResults(results)
        imageProxy.close()
    }
}

private fun ImageProxy.toBitmapFromRGBA8888(): Bitmap? {
    val buffer = planes[0].buffer
    val bytes = ByteArray(buffer.remaining())
    buffer.get(bytes)

    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
        copyPixelsFromBuffer(java.nio.ByteBuffer.wrap(bytes))
    }
}

private fun rotateBitmap(bitmap: Bitmap, rotationDegrees: Int): Bitmap {
    if (rotationDegrees == 0) return bitmap

    val matrix = Matrix().apply {
        postRotate(rotationDegrees.toFloat())
    }
    return Bitmap.createBitmap(
        bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
    )
}


