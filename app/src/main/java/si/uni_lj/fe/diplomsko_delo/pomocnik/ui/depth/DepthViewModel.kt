package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.depth

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
import androidx.annotation.StringRes
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import si.uni_lj.fe.diplomsko_delo.pomocnik.R
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.DepthEstimator
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.tts.AppTextToSpeech
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

/**
 * ViewModel for the depth estimation screen.
 * Processes camera images to estimate depth and provides visual feedback.
 */
class DepthViewModel(
    context: Context,
    val tts: AppTextToSpeech
) : ViewModel() {

    private val estimator: DepthEstimator = DepthEstimator(context)

    private val _centerDepthText = mutableStateOf<String?>(null)
    val centerDepthText: State<String?> = _centerDepthText

    private val _depthBitmap = mutableStateOf<Bitmap?>(null)
    val depthBitmap: State<Bitmap?> = _depthBitmap

    private var lastTtsCallTimestamp = 0L
    private val ttsMutex = Mutex()

    companion object {
        private const val TAG = "DepthViewModel"
        private val TTS_INTERVAL_MS = TimeUnit.SECONDS.toMillis(2)

        // Higher MiDaS Value = Closer = Lower Scale Number (Scale 1 is closest)
        private val SCALE_BOUNDARIES = listOf(
            1000f, 900f, 800f, 700f, 600f, 500f, 400f, 300f, 200f, 100f
        )
        private const val MAX_SCALE = 11
    }


    /**
     * Processes a camera image to estimate depth and update the UI.
     */
    fun processImage(
        imageProxy: ImageProxy,
        context: Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap: Bitmap?
            val depthMap: Array<Array<Array<FloatArray>>>?
            try {
                bitmap = imageProxyToBitmap(imageProxy)
                depthMap = estimator.estimateDepth(bitmap)

                depthMap?.let { depthOutput ->
                    val centerDepthValue = depthOutput[0][128][128][0]
                    Log.d(TAG, "Center Depth Raw Value: $centerDepthValue")

                    val scaleNumber = mapValueToScale(centerDepthValue)
                    val descriptionResId = getQualitativeDescriptionResId(scaleNumber)

                    val uiText = context.getString(
                        R.string.depth_ui_feedback,
                        centerDepthValue,
                        scaleNumber
                    )

                    withContext(Dispatchers.Main) {
                        _centerDepthText.value = uiText
                    }

                    val now = SystemClock.elapsedRealtime()
                    if (now - lastTtsCallTimestamp > TTS_INTERVAL_MS) {
                        ttsMutex.withLock {
                            if (now - lastTtsCallTimestamp > TTS_INTERVAL_MS) {
                                lastTtsCallTimestamp = now
                                val descriptionString = context.getString(descriptionResId)
                                val ttsMessage = context.getString(
                                    R.string.depth_tts_feedback,
                                    descriptionString,
                                    scaleNumber
                                )
                                Log.d(TAG, "TTS: $ttsMessage")
                                tts.readText(ttsMessage)
                            }
                        }
                    }

                    val grayscaleBitmap = createGrayscaleBitmap(depthOutput)
                    withContext(Dispatchers.Main) {
                        _depthBitmap.value = grayscaleBitmap
                    }

                } ?: run {
                    Log.e(TAG, "Depth estimation failed.")
                    withContext(Dispatchers.Main) { _centerDepthText.value = "Estimation Failed" }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error processing image or depth", e)
                withContext(Dispatchers.Main) { _centerDepthText.value = "Processing Error" }
            } finally {
                imageProxy.close()
            }
        }
    }

    /**
     * Maps a raw depth value to a scale from 1-11.
     */
    private fun mapValueToScale(value: Float): Int {
        if (value > SCALE_BOUNDARIES[0]) return 1
        for (i in 0 until SCALE_BOUNDARIES.size - 1) {
            if (value > SCALE_BOUNDARIES[i + 1] && value <= SCALE_BOUNDARIES[i]) {
                return i + 2
            }
        }
        return MAX_SCALE
    }

    @StringRes
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
     * Creates a grayscale visualization of the depth map.
     */
    private fun createGrayscaleBitmap(depthMap: Array<Array<Array<FloatArray>>>): Bitmap {
        val height = depthMap[0].size
        val width = depthMap[0][0].size
        val grayscale = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        var min = Float.MAX_VALUE
        var max = Float.MIN_VALUE

        for (y in 0 until height) {
            for (x in 0 until width) {
                val v = depthMap[0][y][x][0]
                if (v < min) min = v
                if (v > max) max = v
            }
        }

        val range = max - min

        if (range <= 0f) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    grayscale.setPixel(x, y, 0xFF808080.toInt()) // Mid-gray
                }
            }
            return grayscale
        }

        for (y in 0 until height) {
            for (x in 0 until width) {
                val norm = ((depthMap[0][y][x][0] - min) / range * 255).toInt().coerceIn(0, 255)
                val color = (0xFF shl 24) or (norm shl 16) or (norm shl 8) or norm
                grayscale.setPixel(x, y, color)
            }
        }
        
        return grayscale
    }

    /**
     * Converts an ImageProxy to a Bitmap.
     * Note: This YUV->NV21->JPEG->Bitmap conversion is inefficient.
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        require(image.format == ImageFormat.YUV_420_888) { "Unsupported image format: ${image.format}" }

        val yBuffer = image.planes[0].buffer
        val uBuffer = image.planes[1].buffer
        val vBuffer = image.planes[2].buffer

        val ySize = yBuffer.remaining()
        val uSize = uBuffer.remaining()
        val vSize = vBuffer.remaining()

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
            bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        }
        return bitmap
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "Closing DepthEstimator.")
        try {
            estimator.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing DepthEstimator", e)
        }
    }
}