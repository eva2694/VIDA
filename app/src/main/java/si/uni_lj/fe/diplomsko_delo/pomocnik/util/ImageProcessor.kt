package si.uni_lj.fe.diplomsko_delo.pomocnik.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageProxy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import si.uni_lj.fe.diplomsko_delo.pomocnik.models.BoundingBox

class ImageProcessor {

    suspend fun processImage(
        imageProxy: ImageProxy,
        yoloModelLoader: YoloModelLoader
    ): List<BoundingBox> {
        val bitmap = imageProxy.toBitmapFromRGBA8888()

        val rotatedBitmap = rotateBitmap(bitmap, imageProxy.imageInfo.rotationDegrees)

        return withContext(Dispatchers.IO) {
            val results = yoloModelLoader.detect(rotatedBitmap)
            imageProxy.close()
            results
        }
    }

    private fun ImageProxy.toBitmapFromRGBA8888(): Bitmap {
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

}