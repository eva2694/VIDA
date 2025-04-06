package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.assist

import android.content.Context
import android.graphics.Paint
import android.util.Log
import android.view.WindowManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.YoloModelLoader
import java.util.concurrent.ExecutorService

@Composable
fun AssistScreen(
    cameraExecutor: ExecutorService,
    yoloModelLoader: YoloModelLoader,
) {
    val context = LocalContext.current
    val viewModelFactory = remember {
        AssistViewModelFactory(context, yoloModelLoader)
    }
    val viewModel: AssistViewModel = viewModel(factory = viewModelFactory)

    val displayRotation = remember {
        (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay.rotation
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }

    val assistResults by viewModel.assistResults

    Box(modifier = Modifier.fillMaxSize()) {
        // Camera Preview
        AndroidView(factory = { ctx ->
            val previewView = PreviewView(ctx)
            val preview = Preview.Builder().build()
                .also { it.surfaceProvider = previewView.surfaceProvider }

            val imageAnalysis = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setTargetRotation(displayRotation)
                .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                .build()

            imageAnalysis.setAnalyzer(cameraExecutor) { imageProxy ->
                viewModel.processImage(imageProxy, context)
            }

            // Camera Provider Setup
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
                    Log.e("AssistScreen", "Use case binding failed", exc)
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }, modifier = Modifier.fillMaxSize())

        // Bounding Box and Text Overlay Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height

            assistResults.forEach { result ->
                val bbox = result.boundingBox
                // Denormalize coordinates
                val x1 = bbox.x1 * canvasWidth
                val y1 = bbox.y1 * canvasHeight
                val x2 = bbox.x2 * canvasWidth
                val y2 = bbox.y2 * canvasHeight

                val boxColor = Color.Yellow
                val strokeWidth = 5f
                val textBgAlpha = 0.6f
                val textSizePx = 40f

                // Draw Bounding Box
                if (x2 > x1 && y2 > y1) {
                    drawRect(
                        color = boxColor,
                        topLeft = Offset(x1, y1),
                        size = Size(width = x2 - x1, height = y2 - y1),
                        style = Stroke(width = strokeWidth)
                    )

                    // Prepare text label
                    val confidencePercent = "%.0f".format(bbox.cnf * 100)
                    val scaleText = if (result.depthScale != -1) "(${result.depthScale}/11)" else "(Depth N/A)"
                    val labelText = "${bbox.clsName} ${confidencePercent}% $scaleText"

                    // Draw Text with Background
                    drawContext.canvas.nativeCanvas.apply {
                        val textPaint = Paint().apply {
                            color = android.graphics.Color.BLACK
                            textSize = textSizePx
                            style = Paint.Style.FILL
                        }
                        val bgPaint = Paint().apply {
                            color = boxColor.copy(alpha = textBgAlpha).hashCode()
                            style = Paint.Style.FILL
                        }

                        val textBounds = android.graphics.Rect()
                        textPaint.getTextBounds(labelText, 0, labelText.length, textBounds)
                        val textHeight = textBounds.height()
                        val textWidth = textPaint.measureText(labelText)

                        val bgTop = y1 - textHeight - (strokeWidth * 2)
                        val bgBottom = y1 - strokeWidth
                        val bgLeft = x1
                        val bgRight = x1 + textWidth + (strokeWidth * 2)

                        // Draw background rect first
                        drawRect(bgLeft, bgTop, bgRight, bgBottom, bgPaint)
                        // Draw text on top of background
                        drawText(
                            labelText,
                            x1 + strokeWidth,
                            y1 - strokeWidth - (strokeWidth / 2),
                            textPaint
                        )

                        // Draw OCR Text below the main label
                        if (!result.ocrText.isNullOrBlank()) {
                            val ocrText = "OCR: ${result.ocrText}"
                            val ocrTextPaint = Paint().apply {
                                color = android.graphics.Color.WHITE
                                textSize = textSizePx * 0.8f
                                style = Paint.Style.FILL
                            }
                            val ocrBgPaint = Paint().apply {
                                color = Color.Black.copy(alpha = textBgAlpha).hashCode()
                                style = Paint.Style.FILL
                            }
                            val ocrTextBounds = android.graphics.Rect()
                            ocrTextPaint.getTextBounds(ocrText, 0, ocrText.length, ocrTextBounds)
                            val ocrTextHeight = ocrTextBounds.height()
                            val ocrTextWidth = ocrTextPaint.measureText(ocrText)

                            val ocrBgTop = bgBottom + strokeWidth
                            val ocrBgBottom = ocrBgTop + ocrTextHeight + (strokeWidth * 2)
                            val ocrBgLeft = x1
                            val ocrBgRight = x1 + ocrTextWidth + (strokeWidth * 2)

                            drawRect(ocrBgLeft, ocrBgTop, ocrBgRight, ocrBgBottom, ocrBgPaint)
                            drawText(
                                ocrText,
                                x1 + strokeWidth,
                                ocrBgBottom - strokeWidth - (strokeWidth / 2) ,
                                ocrTextPaint
                            )
                        }
                    }
                } else {
                    Log.w("AssistScreen Draw", "Skipping drawing invalid bbox: $bbox")
                }
            }
        }
    }
}