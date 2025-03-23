package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.depth

import android.content.Context
import android.graphics.Bitmap
import androidx.camera.core.ImageProxy
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import si.uni_lj.fe.diplomsko_delo.pomocnik.R
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.DepthEstimator
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.tts.AppTextToSpeech


class DepthViewModel(
    val tts: AppTextToSpeech
) : ViewModel() {
    private var estimator: DepthEstimator? = null

    val depthBitmap = mutableStateOf<Bitmap?>(null)

    fun setContext(context: android.content.Context) {
        if (estimator == null) {
            estimator = DepthEstimator(context)
        }
    }

    fun processImage(imageProxy: ImageProxy, context: Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = imageProxyToBitmap(imageProxy)
            val depthMap = estimator?.estimateDepth(bitmap)

            imageProxy.close()

            depthMap?.let {
                val centerDepth = it[0][128][128][0]
                val message = context.getString(R.string.depth_feedback, centerDepth)
                tts.readText(message)

                val grayscale = Bitmap.createBitmap(256, 256, Bitmap.Config.ARGB_8888)
                var min = Float.MAX_VALUE
                var max = Float.MIN_VALUE

                for (y in 0 until 256) {
                    for (x in 0 until 256) {
                        val v = it[0][y][x][0]
                        if (v < min) min = v
                        if (v > max) max = v
                    }
                }

                for (y in 0 until 256) {
                    for (x in 0 until 256) {
                        val norm = ((it[0][y][x][0] - min) / (max - min) * 255).toInt().coerceIn(0, 255)
                        val color = 0xFF shl 24 or (norm shl 16) or (norm shl 8) or norm
                        grayscale.setPixel(x, y, color)
                    }
                }

                depthBitmap.value = grayscale
            }
        }
    }

    private fun imageProxyToBitmap(image: ImageProxy): Bitmap {
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

        val yuvImage = android.graphics.YuvImage(nv21, android.graphics.ImageFormat.NV21, image.width, image.height, null)
        val out = java.io.ByteArrayOutputStream()
        yuvImage.compressToJpeg(android.graphics.Rect(0, 0, image.width, image.height), 100, out)
        val imageBytes = out.toByteArray()
        val bitmap = android.graphics.BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)

        // 🔁 Rotate bitmap to match the ImageProxy orientation
        val rotationDegrees = image.imageInfo.rotationDegrees
        return if (rotationDegrees != 0) {
            val matrix = android.graphics.Matrix().apply { postRotate(rotationDegrees.toFloat()) }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } else {
            bitmap
        }
    }



}
