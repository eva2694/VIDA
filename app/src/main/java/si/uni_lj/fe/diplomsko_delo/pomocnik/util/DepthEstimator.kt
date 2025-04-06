package si.uni_lj.fe.diplomsko_delo.pomocnik.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import si.uni_lj.fe.diplomsko_delo.pomocnik.Constants
import java.io.IOException

/**
 * Handles depth estimation using the MiDaS model.
 * Processes images to generate depth maps for spatial understanding.
 */
class DepthEstimator(context: Context) {
    private var interpreter: Interpreter? = null
    private var isClosed = false

    companion object {
        private const val TAG = "DepthEstimator"
        private const val MODEL_INPUT_WIDTH = 256
        private const val MODEL_INPUT_HEIGHT = 256
        private const val MODEL_INPUT_CHANNELS = 3
        private const val MODEL_OUTPUT_WIDTH = 256
        private const val MODEL_OUTPUT_HEIGHT = 256
        private const val MODEL_OUTPUT_CHANNELS = 1
    }

    init {
        try {
            val modelBuffer = FileUtil.loadMappedFile(context, Constants.MIDAS_PATH)
            Log.d(TAG, "Model loaded successfully from ${Constants.MIDAS_PATH}")
            val options = Interpreter.Options()
            interpreter = Interpreter(modelBuffer, options)
            Log.d(TAG, "Interpreter initialized.")
        } catch (e: IOException) {
            Log.e(TAG, "Error loading model or initializing interpreter", e)
            interpreter = null
        }
    }

    /**
     * Estimates depth from a bitmap image.
     * @param bitmap Input image to process
     * @return 4D array containing depth values, or null if estimation fails
     */
    fun estimateDepth(bitmap: Bitmap): Array<Array<Array<FloatArray>>>? {
        if (interpreter == null || isClosed) {
            Log.w(TAG, "Interpreter not initialized or already closed.")
            return null
        }

        val input = preprocess(bitmap)
        val output = Array(1) {
            Array(MODEL_OUTPUT_HEIGHT) {
                Array(MODEL_OUTPUT_WIDTH) {
                    FloatArray(MODEL_OUTPUT_CHANNELS)
                }
            }
        }

        try {
            interpreter?.run(input, output)
            return output
        } catch (e: Exception) {
            Log.e(TAG, "Error during model inference", e)
            return null
        }
    }

    /**
     * Preprocesses the input bitmap for the model.
     * Applies ImageNet normalization and resizes to model input dimensions.
     */
    private fun preprocess(bitmap: Bitmap): Array<Array<Array<FloatArray>>> {
        val resizedBitmap =
            Bitmap.createScaledBitmap(bitmap, MODEL_INPUT_WIDTH, MODEL_INPUT_HEIGHT, true)

        val input = Array(1) {
            Array(MODEL_INPUT_HEIGHT) {
                Array(MODEL_INPUT_WIDTH) {
                    FloatArray(MODEL_INPUT_CHANNELS)
                }
            }
        }

        for (y in 0 until MODEL_INPUT_HEIGHT) {
            for (x in 0 until MODEL_INPUT_WIDTH) {
                val pixel = resizedBitmap.getPixel(x, y)

                input[0][y][x][0] = ((pixel shr 16 and 0xFF) / 255.0f - 0.485f) / 0.229f // R
                input[0][y][x][1] = ((pixel shr 8 and 0xFF) / 255.0f - 0.456f) / 0.224f  // G
                input[0][y][x][2] = ((pixel and 0xFF) / 255.0f - 0.406f) / 0.225f         // B
            }
        }
        return input
    }

    /**
     * Releases model resources.
     */
    fun close() {
        if (!isClosed && interpreter != null) {
            interpreter?.close()
            interpreter = null
            isClosed = true
            Log.d(TAG, "Interpreter closed.")
        }
    }
}

// Make sure Constants.MIDAS_PATH is defined elsewhere, e.g.:
// object Constants { const val MIDAS_PATH = "midas_v2_1_small_256.tflite" } // Or your actual model name