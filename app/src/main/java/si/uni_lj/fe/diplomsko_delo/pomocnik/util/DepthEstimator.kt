package si.uni_lj.fe.diplomsko_delo.pomocnik.util

import android.content.Context
import android.graphics.Bitmap
import org.tensorflow.lite.Interpreter
import si.uni_lj.fe.diplomsko_delo.pomocnik.Constants
import java.io.FileInputStream
import java.nio.MappedByteBuffer
import java.nio.channels.FileChannel


class DepthEstimator(context: Context) {
    private val interpreter: Interpreter

    init {
        val assetFileDescriptor = context.assets.openFd(Constants.MIDAS_PATH)
        val inputStream = FileInputStream(assetFileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        val startOffset = assetFileDescriptor.startOffset
        val declaredLength = assetFileDescriptor.declaredLength
        val modelBuffer: MappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength)
        interpreter = Interpreter(modelBuffer)
    }

    fun estimateDepth(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val input = preprocess(bitmap)
        val output = Array(1) { Array(256) { Array(256) { FloatArray(1) } } }
        interpreter.run(input, output)
        return output
    }

    private fun preprocess(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val resized = Bitmap.createScaledBitmap(bitmap, 256, 256, true)
        val input = Array(1) { Array(256) { Array(256) { FloatArray(3) } } }

        for (y in 0 until 256) {
            for (x in 0 until 256) {
                val pixel = resized.getPixel(x, y)
                input[0][y][x][0] = ((pixel shr 16 and 0xFF) / 255.0f - 0.485f) / 0.229f
                input[0][y][x][1] = ((pixel shr 8 and 0xFF) / 255.0f - 0.456f) / 0.224f
                input[0][y][x][2] = ((pixel and 0xFF) / 255.0f - 0.406f) / 0.225f
            }
        }

        return input
    }
}