package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.assist

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import si.uni_lj.fe.diplomsko_delo.pomocnik.R
import si.uni_lj.fe.diplomsko_delo.pomocnik.models.AssistResult
import si.uni_lj.fe.diplomsko_delo.pomocnik.models.BoundingBox
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.DepthEstimator
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.YoloModelLoader
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.tts.AppTextToSpeech
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * ViewModel for the Assist screen that handles real-time object detection, depth estimation, and OCR.
 * Processes camera frames to detect objects, estimate their depth, and extract text using OCR.
 * Provides feedback through TTS (Text-to-Speech) about detected objects.
 */
class AssistViewModel(
    private val yoloModelLoader: YoloModelLoader,
    private val depthEstimator: DepthEstimator,
    val tts: AppTextToSpeech,
) : ViewModel() {

    // UI state
    private val _assistResults = mutableStateOf<List<AssistResult>>(emptyList())
    val assistResults: State<List<AssistResult>> = _assistResults

    // OCR setup
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // TTS rate limiting
    private var lastTtsCallTimestamp = 0L
    private val ttsMutex = Mutex()

    companion object {
        private const val TAG = "AssistViewModel"
        private val TTS_INTERVAL_MS = TimeUnit.SECONDS.toMillis(3)
        private const val YOLO_CONFIDENCE_THRESHOLD = 0.4f

        // Depth scale boundaries for distance estimation
        private val SCALE_BOUNDARIES = listOf(
            1000f, 900f, 800f, 700f, 600f, 500f, 400f, 300f, 200f, 100f
        )
        private const val MAX_SCALE = 11
    }

    /**
     * Processes a camera frame to detect objects, estimate depth, and perform OCR.
     * Updates the UI state with detection results and provides TTS feedback.
     *
     * @param imageProxy The camera frame to process
     * @param context Context for resource access
     */
    @OptIn(ExperimentalGetImage::class)
    fun processImage(imageProxy: ImageProxy, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val rotatedBitmap: Bitmap?
            val fullDepthMap: Array<Array<Array<FloatArray>>>?
            val detections: List<BoundingBox>
            val combinedResults = mutableListOf<AssistResult>()

            try {
                // Convert camera frame to bitmap
                rotatedBitmap = imageProxyToYuvBitmap(imageProxy)
                if (rotatedBitmap == null) {
                    Log.e(TAG, "Failed to convert ImageProxy to Bitmap")
                    return@launch
                }

                // Run object detection
                detections = yoloModelLoader.detect(rotatedBitmap)

                // Estimate depth for the frame
                fullDepthMap = depthEstimator.estimateDepth(rotatedBitmap)
                if (fullDepthMap == null) {
                    Log.w(TAG, "Depth estimation failed for this frame.")
                }

                // Process each detection
                for (detection in detections) {
                    if (detection.cnf < YOLO_CONFIDENCE_THRESHOLD) continue

                    // Calculate depth scale for the detected object
                    var depthScale = -1
                    if (fullDepthMap != null) {
                        try {
                            val centerX = (detection.x1 + detection.x2) / 2f
                            val centerY = (detection.y1 + detection.y2) / 2f
                            val depthMapX = (centerX * (fullDepthMap[0][0].size - 1)).toInt().coerceIn(0, fullDepthMap[0][0].size - 1)
                            val depthMapY = (centerY * (fullDepthMap[0].size - 1)).toInt().coerceIn(0, fullDepthMap[0].size - 1)
                            val rawDepth = fullDepthMap[0][depthMapY][depthMapX][0]
                            depthScale = mapValueToScale(rawDepth)
                            Log.d(TAG, "Object: ${detection.clsName}, CenterDepth: $rawDepth, Scale: $depthScale")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error sampling depth for ${detection.clsName}", e)
                            depthScale = -1
                        }
                    }

                    // Perform OCR on the detected object region
                    var ocrTextResult: String? = null
                    try {
                        val cropRect = Rect(
                            (detection.x1 * rotatedBitmap.width).toInt().coerceAtLeast(0),
                            (detection.y1 * rotatedBitmap.height).toInt().coerceAtLeast(0),
                            (detection.x2 * rotatedBitmap.width).toInt().coerceAtMost(rotatedBitmap.width),
                            (detection.y2 * rotatedBitmap.height).toInt().coerceAtMost(rotatedBitmap.height)
                        )

                        if (cropRect.width() > 0 && cropRect.height() > 0) {
                            val croppedBitmap = Bitmap.createBitmap(
                                rotatedBitmap,
                                cropRect.left,
                                cropRect.top,
                                cropRect.width(),
                                cropRect.height()
                            )
                            val imageForText = InputImage.fromBitmap(croppedBitmap, 0)
                            val visionText = textRecognizer.process(imageForText).await()
                            ocrTextResult = visionText.text.replace("\n", " ").trim()
                            if (ocrTextResult.isNotEmpty()) {
                                Log.d(TAG, "OCR Success for ${detection.clsName}: $ocrTextResult")
                            } else {
                                Log.d(TAG, "OCR for ${detection.clsName} produced no text.")
                                ocrTextResult = null
                            }
                            // TODO: Consider recycling croppedBitmap for memory optimization
                        } else {
                            Log.w(TAG, "Skipping OCR for ${detection.clsName} due to invalid crop rectangle: $cropRect")
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error during OCR for ${detection.clsName}", e)
                        ocrTextResult = null
                    }

                    // Add processed result
                    combinedResults.add(
                        AssistResult(
                            boundingBox = detection,
                            depthScale = depthScale,
                            ocrText = ocrTextResult
                        )
                    )
                }

                // Update UI with results
                withContext(Dispatchers.Main) {
                    _assistResults.value = combinedResults
                }

                // Provide TTS feedback
                generateAndSpeakFeedback(combinedResults, context)

            } catch (e: Exception) {
                Log.e(TAG, "Error processing image in AssistViewModel", e)
                withContext(Dispatchers.Main) {
                    _assistResults.value = emptyList()
                }
            } finally {
                imageProxy.close()
                // TODO: Consider recycling rotatedBitmap for memory optimization
            }
        }
    }

    /**
     * Generates and speaks TTS feedback for detected objects.
     * Includes object name, depth information, and OCR text if available.
     */
    private fun generateAndSpeakFeedback(results: List<AssistResult>, context: Context) {
        if (results.isEmpty()) return

        val now = SystemClock.elapsedRealtime()
        if (now - lastTtsCallTimestamp > TTS_INTERVAL_MS) {
            viewModelScope.launch {
                ttsMutex.withLock {
                    if (now - lastTtsCallTimestamp > TTS_INTERVAL_MS) {
                        lastTtsCallTimestamp = now

                        val messages = results.map { result ->
                            var message: String
                            if (result.depthScale != -1) {
                                val descriptionResId = getQualitativeDescriptionResId(result.depthScale)
                                val descriptionString = context.getString(descriptionResId)

                                message = context.getString(
                                    R.string.depth_tts_feedback,
                                    descriptionString,
                                    result.depthScale
                                )

                                message = "${result.boundingBox.clsName}, $message"

                            } else {
                                message = result.boundingBox.clsName
                            }

                            // Append OCR text if available
                            if (!result.ocrText.isNullOrBlank()) {
                                message += "." + context.getString(R.string.tts_ocr_prefix) + result.ocrText
                            }
                            message
                        }

                        if (messages.isNotEmpty()) {
                            val combinedTtsMessage = messages.take(3).joinToString(separator = ". ")
                            Log.d(TAG, "Combined TTS: $combinedTtsMessage")
                            tts.readText(combinedTtsMessage)
                        }
                    }
                }
            }
        }
    }

    /**
     * Maps a depth value to a scale number (1-11) representing distance.
     * Lower numbers indicate closer objects.
     */
    private fun mapValueToScale(value: Float): Int {
        if (value <= 0) return MAX_SCALE
        if (value > SCALE_BOUNDARIES[0]) return 1
        for (i in 0 until SCALE_BOUNDARIES.size - 1) {
            if (value > SCALE_BOUNDARIES[i + 1] && value <= SCALE_BOUNDARIES[i]) {
                return i + 2
            }
        }
        return MAX_SCALE
    }

    /**
     * Returns the string resource ID for qualitative depth description based on scale.
     */
    @androidx.annotation.StringRes
    private fun getQualitativeDescriptionResId(scaleNumber: Int): Int {
        return when (scaleNumber) {
            1, 2 -> R.string.depth_desc_very_close
            3, 4 -> R.string.depth_desc_close
            5, 6, 7 -> R.string.depth_desc_medium
            8, 9, 10 -> R.string.depth_desc_far
            else -> R.string.depth_desc_very_far
        }
    }

    /**
     * Converts ImageProxy (expecting YUV_420_888) to Bitmap, handling rotation.
     * WARNING: This YUV->NV21->JPEG->Bitmap conversion is INEFFICIENT. TODO: Replace with faster method.
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun imageProxyToYuvBitmap(image: ImageProxy): Bitmap? {
        return try {
            require(image.format == ImageFormat.YUV_420_888) { "Requires YUV_420_888, received ${image.format}" }

            val yBuffer = image.planes[0].buffer
            val uBuffer = image.planes[1].buffer
            val vBuffer = image.planes[2].buffer
            val ySize = yBuffer.remaining(); val uSize = uBuffer.remaining(); val vSize = vBuffer.remaining()
            val nv21 = ByteArray(ySize + uSize + vSize)

            yBuffer.get(nv21, 0, ySize)
            vBuffer.get(nv21, ySize, vSize)
            uBuffer.get(nv21, ySize + vSize, uSize)

            val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
            val out = ByteArrayOutputStream()
            yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
            val imageBytes = out.toByteArray()
            var bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

            val rotationDegrees = image.imageInfo.rotationDegrees
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                if (rotated != bitmap) { // Only recycle original if rotation happened
                    bitmap.recycle()
                }
                bitmap = rotated
            }
            bitmap
        } catch (e: Exception) {
            Log.e(TAG, "Error converting YUV ImageProxy to Bitmap", e)
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "Closing resources in AssistViewModel.")
        try {
            depthEstimator.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing resources", e)
        }
    }
}