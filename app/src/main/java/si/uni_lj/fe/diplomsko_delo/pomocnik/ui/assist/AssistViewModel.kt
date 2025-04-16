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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import si.uni_lj.fe.diplomsko_delo.pomocnik.Constants
import si.uni_lj.fe.diplomsko_delo.pomocnik.R
import si.uni_lj.fe.diplomsko_delo.pomocnik.models.AssistResult
import si.uni_lj.fe.diplomsko_delo.pomocnik.models.BoundingBox
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.DepthEstimator
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.DepthEstimator.Companion.getQualitativeDescriptionResId
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.YoloModelLoader
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.tts.AppTextToSpeech
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.InputStreamReader
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
    private val context: Context
) : ViewModel() {

    // UI state
    private val _assistResults = mutableStateOf<List<AssistResult>>(emptyList())
    val assistResults: State<List<AssistResult>> = _assistResults

    // OCR setup
    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    // TTS rate limiting
    private var lastTtsCallTimestamp = 0L
    private val ttsMutex = Mutex()

    // Scene detection
    private var sceneInterpreter: Interpreter? = null
    private var sceneLabels = mutableListOf<String>()
    private var hasPerformedSceneDetection = false
    private val preferencesManager = PreferencesManager(context)


    companion object {
        private const val TAG = "AssistViewModel"
        private val TTS_INTERVAL_MS = TimeUnit.SECONDS.toMillis(3)
        private const val YOLO_CONFIDENCE_THRESHOLD = 0.3f

        // Scene detection constants
        private const val INPUT_MEAN = 0.485f
        private const val INPUT_STANDARD_DEVIATION = 0.229f
        private const val INPUT_SIZE = 224
        private const val NUM_CLASSES = 365
    }

    init {
        initializeSceneDetector()
        loadSceneLabels()
        observeLanguageChanges()
    }

    private fun observeLanguageChanges() {
        viewModelScope.launch {
            preferencesManager.language.collect {
                loadSceneLabels()
            }
        }
    }

    private fun initializeSceneDetector() {
        try {
            val options = Interpreter.Options().apply {
                numThreads = 4
            }
            val model = FileUtil.loadMappedFile(context, Constants.SCENE_DETECTOR_PATH)
            sceneInterpreter = Interpreter(model, options)

            val inputShape = sceneInterpreter?.getInputTensor(0)?.shape()
            val outputShape = sceneInterpreter?.getOutputTensor(0)?.shape()
            Log.d(TAG, "Scene model input shape: ${inputShape?.contentToString()}")
            Log.d(TAG, "Scene model output shape: ${outputShape?.contentToString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing scene classifier interpreter", e)
        }
    }

    private fun loadSceneLabels() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val lang = preferencesManager.language.first()
                val labelPath =
                    if (lang == "sl") Constants.SCENE_LABELS_PATH_SI else Constants.SCENE_LABELS_PATH_EN
                context.assets.open(labelPath).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        sceneLabels.clear()
                        reader.lineSequence().forEach { line ->
                            val trimmedLine = line.trim()
                            if (trimmedLine.isNotEmpty()) {
                                sceneLabels.add(trimmedLine.replace("_", " "))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading scene labels", e)
            }
        }
    }

    private fun detectScene(bitmap: Bitmap): String? {
        if (sceneInterpreter == null || sceneLabels.isEmpty()) return null

        val pixels = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(pixels, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)

        val inputBuffer = TensorBuffer.createFixedSize(
            intArrayOf(1, 3, INPUT_SIZE, INPUT_SIZE),
            DataType.FLOAT32
        )

        val inputArray = FloatArray(1 * 3 * INPUT_SIZE * INPUT_SIZE)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            inputArray[i] =
                ((pixel shr 16 and 0xFF) / 255.0f - INPUT_MEAN) / INPUT_STANDARD_DEVIATION
            inputArray[i + INPUT_SIZE * INPUT_SIZE] =
                ((pixel shr 8 and 0xFF) / 255.0f - INPUT_MEAN) / INPUT_STANDARD_DEVIATION
            inputArray[i + 2 * INPUT_SIZE * INPUT_SIZE] =
                ((pixel and 0xFF) / 255.0f - INPUT_MEAN) / INPUT_STANDARD_DEVIATION
        }

        inputBuffer.loadArray(inputArray)

        val outputBuffer = TensorBuffer.createFixedSize(
            intArrayOf(1, NUM_CLASSES),
            DataType.FLOAT32
        )

        sceneInterpreter?.run(inputBuffer.buffer, outputBuffer.buffer)

        val confidences = outputBuffer.floatArray
        val softmaxConfidences = softmax(confidences)

        val maxIndex =
            softmaxConfidences.indices.maxByOrNull { softmaxConfidences[it] } ?: return null
        val maxConfidence = softmaxConfidences[maxIndex]

        return if (maxConfidence > 0.2f) {
            sceneLabels[maxIndex]
        } else {
            null
        }
    }

    private fun softmax(input: FloatArray): FloatArray {
        val max = input.maxOrNull() ?: 0f
        val exp = input.map { kotlin.math.exp(it - max) }
        val sum = exp.sum()
        return exp.map { (it / sum) }.toFloatArray()
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

                // Perform scene detection once when screen is first opened
                if (!hasPerformedSceneDetection) {
                    val scene = detectScene(rotatedBitmap)
                    if (scene != null) {
                        val lang = preferencesManager.language.first()
                        val prefix = if (lang == "sl")
                            context.getString(R.string.scene_prefix)
                        else
                            context.getString(R.string.scene_prefix)
                        tts.readText("$prefix $scene")
                    }
                    hasPerformedSceneDetection = true
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
                    var depth = -1
                    if (fullDepthMap != null) {
                        try {
                            val centerX = (detection.x1 + detection.x2) / 2f
                            val centerY = (detection.y1 + detection.y2) / 2f
                            val depthMapX = (centerX * (fullDepthMap[0][0].size - 1)).toInt().coerceIn(0, fullDepthMap[0][0].size - 1)
                            val depthMapY = (centerY * (fullDepthMap[0].size - 1)).toInt().coerceIn(0, fullDepthMap[0].size - 1)
                            depth = fullDepthMap[0][depthMapY][depthMapX][0].toInt()
                            Log.d(TAG, "Object: ${detection.clsName}, CenterDepth: $depth")
                        } catch (e: Exception) {
                            Log.e(TAG, "Error sampling depth for ${detection.clsName}", e)
                            depth = -1
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
                            depth = depth,
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
                            if (result.depth != -1) {
                                val descriptionResId = getQualitativeDescriptionResId(result.depth)
                                val descriptionString = context.getString(descriptionResId)

                                message = context.getString(
                                    R.string.depth_tts_feedback,
                                    descriptionString,
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

    fun stopReading() {
        tts.stop()
    }

    fun resetSceneDetection() {
        hasPerformedSceneDetection = false
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