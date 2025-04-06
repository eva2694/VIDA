package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.depth

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import android.util.Log
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import si.uni_lj.fe.diplomsko_delo.pomocnik.R
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.DepthEstimator
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.tts.AppTextToSpeech
import java.io.ByteArrayOutputStream

class DepthViewModel(
    context: Context,
    val tts: AppTextToSpeech
) : ViewModel() {

    private val estimator: DepthEstimator = DepthEstimator(context)

    private val _centerDepthText = mutableStateOf<String?>(null)
    val centerDepthText: State<String?> = _centerDepthText

    private val _depthBitmap = mutableStateOf<Bitmap?>(null)
    val depthBitmap: State<Bitmap?> = _depthBitmap

    fun processImage(
        imageProxy: ImageProxy,
        context: Context
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            var bitmap: Bitmap? = null
            var depthMap: Array<Array<Array<FloatArray>>>? = null
            try {
                bitmap = imageProxyToBitmap(imageProxy)
                depthMap = estimator.estimateDepth(bitmap)

                depthMap?.let { depthOutput ->
                    // --- Depth Value Interpretation ---
                    // MiDaS typically outputs relative inverse depth.
                    // The raw value isn't directly in meters. Higher values mean closer.
                    // The text/TTS feedback should reflect this relative nature unless you calibrate.
                    val centerDepthValue =
                        depthOutput[0][128][128][0]

                    val centerDepthFormatted = String.format("%.2f", centerDepthValue)
                    val feedbackText = centerDepthFormatted

                    withContext(Dispatchers.Main) {
                        _centerDepthText.value = feedbackText
                    }


                    val message = context.getString(R.string.depth_feedback, centerDepthValue)
                    tts.readText(message)

                    val grayscaleBitmap = createGrayscaleBitmap(depthOutput)
                    withContext(Dispatchers.Main) {
                        _depthBitmap.value = grayscaleBitmap
                    }

                } ?: run {
                    Log.e("DepthViewModel", "Depth estimation failed.")
                    // Optionally clear previous state on failure
                    // withContext(Dispatchers.Main) {
                    //     _centerDepthText.value = "Error"
                    //     _depthBitmap.value = null
                    // }
                }
            } catch (e: Exception) {
                Log.e("DepthViewModel", "Error processing image or depth", e)
                withContext(Dispatchers.Main) {
                    _centerDepthText.value = "Processing Error"
                    // _depthBitmap.value = null // Keep last valid bitmap? Or clear?
                }
            } finally {
                imageProxy.close()
            }
        }
    }

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

        // --- Visualization Stability Note ---
        // Normalizing based on the current frame's min/max can cause the visual
        // representation of a static object to flicker if other objects change the overall min/max.
        // For more stable visualization, consider:
        // 1. Clamping: Use fixed min/max values expected from the model.
        // 2. Filtering: Apply temporal smoothing to the min/max values.
        // 3. Color Mapping: Use a color map (like VIRIDIS) instead of grayscale for better depth differentiation.

        val range = max - min
        if (range <= 0f) {
            for (y in 0 until height) {
                for (x in 0 until width) {
                    val color = 0xFF808080.toInt() // Mid-gray
                    grayscale.setPixel(x, y, color)
                }
            }
            return grayscale
        }


        for (y in 0 until height) {
            for (x in 0 until width) {
                // Normalize the depth value to 0-255 range for grayscale
                val norm = ((depthMap[0][y][x][0] - min) / range * 255).toInt().coerceIn(0, 255)
                // Create ARGB color (alpha=255, R=G=B=norm)
                val color = (0xFF shl 24) or (norm shl 16) or (norm shl 8) or norm
                grayscale.setPixel(x, y, color)
            }
        }
        return grayscale
    }

    /**
     * Converts an ImageProxy (YUV_420_888 format) to a Bitmap.
     *
     * NOTE: This YUV->NV21->JPEG->Bitmap conversion is common but INEFFICIENT.
     * For performance-critical applications, consider:
     * 1. Native Code (libyuv): Use JNI to call optimized C++ YUV-to-RGB conversion routines.
     * 2. RenderScript (Deprecated): Was previously an option for accelerated conversion.
     * 3. Exploring other libraries or direct buffer manipulation if possible.
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
        require(image.format == ImageFormat.YUV_420_888) {
            "Unsupported image format: ${image.format}"
        }

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
        Log.d("DepthViewModel", "Closing DepthEstimator.")
        try {
            estimator.close()
        } catch (e: Exception) {
            Log.e("DepthViewModel", "Error closing DepthEstimator", e)
        }
    }
}