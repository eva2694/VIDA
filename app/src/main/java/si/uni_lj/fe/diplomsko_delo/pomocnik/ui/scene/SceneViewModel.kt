package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.scene

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.tensorflow.lite.DataType
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import si.uni_lj.fe.diplomsko_delo.pomocnik.Constants
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.tts.AppTextToSpeech
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

data class SceneResult(
    val className: String,
    val confidence: Float
)

/**
 * ViewModel for the scene classification screen.
 * Processes camera images to classify scenes and provides audio feedback.
 */
class SceneViewModel(
    private val context: Context,
    val tts: AppTextToSpeech
) : ViewModel() {

    private var interpreter: Interpreter? = null
    private var labels = mutableListOf<String>()
    private var lastTtsCallTimestamp = 0L
    private val ttsMutex = Mutex()
    private val preferencesManager = PreferencesManager(context)

    private val _sceneResults = mutableStateOf<List<SceneResult>>(emptyList())
    val sceneResults: State<List<SceneResult>> = _sceneResults

    companion object {
        private const val TAG = "SceneViewModel"
        private val TTS_INTERVAL_MS = TimeUnit.SECONDS.toMillis(2)
        private const val INPUT_MEAN = 0.485f
        private const val INPUT_STANDARD_DEVIATION = 0.229f
        private const val NUM_CLASSES = 365
    }

    init {
        initializeInterpreter()
        loadLabels()
        observeLanguageChanges()
    }

    private fun observeLanguageChanges() {
        viewModelScope.launch {
            preferencesManager.language.collect {
                loadLabels()
            }
        }
    }

    private fun initializeInterpreter() {
        try {
            val options = Interpreter.Options().apply {
                numThreads = 4
            }
            val model = FileUtil.loadMappedFile(context, Constants.SCENE_DETECTOR_PATH)
            interpreter = Interpreter(model, options)
            
            val inputShape = interpreter?.getInputTensor(0)?.shape()
            val outputShape = interpreter?.getOutputTensor(0)?.shape()
            Log.d(TAG, "Model input shape: ${inputShape?.contentToString()}")
            Log.d(TAG, "Model output shape: ${outputShape?.contentToString()}")
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing scene classifier interpreter", e)
        }
    }

    private fun loadLabels() {
        viewModelScope.launch(Dispatchers.IO) {
            val lang = preferencesManager.language.first()
            val labelPath =
                if (lang == "sl") Constants.SCENE_LABELS_PATH_SI else Constants.SCENE_LABELS_PATH_EN

            try {
                context.assets.open(labelPath).use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).use { reader ->
                        labels.clear()
                        reader.lineSequence().forEach { line ->
                            val trimmedLine = line.trim()
                            if (trimmedLine.isNotEmpty()) {
                                labels.add(trimmedLine.replace("_", " "))
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error loading scene labels", e)
            }
        }
    }

    fun processImage(imageProxy: ImageProxy) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val bitmap = imageProxyToBitmap(imageProxy)
                val results = classifyScene(bitmap)
                updateSceneResults(results)
                speakResults(results)
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image", e)
            } finally {
                imageProxy.close()
            }
        }
    }

    @OptIn(ExperimentalGetImage::class)
    private fun imageProxyToBitmap(imageProxy: ImageProxy): Bitmap {
        val image = imageProxy.image ?: throw Exception("Image is null")
        val planes = image.planes
        val buffer = planes[0].buffer
        val pixelStride = planes[0].pixelStride
        val rowStride = planes[0].rowStride
        val rowPadding = rowStride - pixelStride * imageProxy.width

        val bitmap = Bitmap.createBitmap(
            imageProxy.width + rowPadding / pixelStride,
            imageProxy.height,
            Bitmap.Config.ARGB_8888
        )
        bitmap.copyPixelsFromBuffer(buffer)
        
        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
        val x = (256 - 224) / 2
        val y = (256 - 224) / 2
        return Bitmap.createBitmap(resizedBitmap, x, y, 224, 224)
    }

    private fun classifyScene(bitmap: Bitmap): List<SceneResult> {
        val pixels = IntArray(224 * 224)
        bitmap.getPixels(pixels, 0, 224, 0, 0, 224, 224)
        
        val inputBuffer = TensorBuffer.createFixedSize(
            intArrayOf(1, 3, 224, 224),
            DataType.FLOAT32
        )
        
        val inputArray = FloatArray(1 * 3 * 224 * 224)
        for (i in pixels.indices) {
            val pixel = pixels[i]
            inputArray[i] = ((pixel shr 16 and 0xFF) / 255.0f - INPUT_MEAN) / INPUT_STANDARD_DEVIATION
            inputArray[i + 224 * 224] = ((pixel shr 8 and 0xFF) / 255.0f - INPUT_MEAN) / INPUT_STANDARD_DEVIATION
            inputArray[i + 2 * 224 * 224] = ((pixel and 0xFF) / 255.0f - INPUT_MEAN) / INPUT_STANDARD_DEVIATION
        }
        
        inputBuffer.loadArray(inputArray)
        
        val outputBuffer = TensorBuffer.createFixedSize(
            intArrayOf(1, NUM_CLASSES),
            DataType.FLOAT32
        )

        interpreter?.run(inputBuffer.buffer, outputBuffer.buffer)

        val confidences = outputBuffer.floatArray
        val softmaxConfidences = softmax(confidences)

        val results = mutableListOf<SceneResult>()
        for (i in softmaxConfidences.indices) {
            results.add(SceneResult(labels[i], softmaxConfidences[i]))
        }

        return results.sortedByDescending { it.confidence }
    }

    private fun softmax(input: FloatArray): FloatArray {
        val max = input.maxOrNull() ?: 0f
        val exp = input.map { kotlin.math.exp(it - max) }
        val sum = exp.sum()
        return exp.map { (it / sum) }.toFloatArray()
    }

    private fun updateSceneResults(results: List<SceneResult>) {
        _sceneResults.value = results
    }

    private suspend fun speakResults(results: List<SceneResult>) {
        if (results.isEmpty()) return

        val currentTime = SystemClock.elapsedRealtime()
        if (currentTime - lastTtsCallTimestamp < TTS_INTERVAL_MS) return

        ttsMutex.withLock {
            if (currentTime - lastTtsCallTimestamp < TTS_INTERVAL_MS) return

            val topResult = results.first()
            if (topResult.confidence > Constants.SCENE_READ_CONFIDENCE) {
                tts.readText(topResult.className)
                lastTtsCallTimestamp = currentTime
            }
        }
    }

    override fun onCleared() {
        interpreter?.close()
        super.onCleared()
    }

    fun stopReading() {
        tts.stop()
    }
} 