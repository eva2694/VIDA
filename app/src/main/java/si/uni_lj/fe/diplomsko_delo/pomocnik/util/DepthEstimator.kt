package si.uni_lj.fe.diplomsko_delo.pomocnik.util

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import org.tensorflow.lite.Interpreter
import org.tensorflow.lite.support.common.FileUtil
import si.uni_lj.fe.diplomsko_delo.pomocnik.Constants
import java.io.IOException


class DepthEstimator(context: Context) {
    private var interpreter: Interpreter? = null
    private var isClosed = false

    companion object {
        private const val TAG = "DepthEstimator"

        // IMPORTANT: Verify these match YOUR specific model from Constants.MIDAS_PATH
        private const val MODEL_INPUT_WIDTH = 256
        private const val MODEL_INPUT_HEIGHT = 256
        private const val MODEL_INPUT_CHANNELS = 3
        private const val MODEL_OUTPUT_WIDTH = 256 // Verify
        private const val MODEL_OUTPUT_HEIGHT = 256 // Verify
        private const val MODEL_OUTPUT_CHANNELS = 1 // Verify
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
     * Estimates depth from a bitmap. Returns null if estimation fails or estimator wasn't initialized.
     */
    fun estimateDepth(bitmap: Bitmap): Array<Array<Array<FloatArray>>>? {
        if (interpreter == null || isClosed) {
            Log.w(TAG, "Interpreter not initialized or already closed.")
            return null
        }

        // --- Verification Point ---
        // Verify that YOUR model actually expects the preprocessing done below (ImageNet normalization).
        // Some models might expect simple 0-1 scaling or different normalization values.
        val input = preprocess(bitmap)

        // --- Verification Point ---
        // Verify that YOUR model output shape matches this declaration. Use tools like Netron to inspect the model.
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

    /** Prepares the bitmap for the TFLite model. */
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
        // recycle the resized bitmap if no longer needed immediately after loop (optional)
        // resizedBitmap.recycle() // Be careful if bitmap is needed elsewhere
        return input
    }

    /** Closes the interpreter to release resources. */
    fun close() {
        if (!isClosed && interpreter != null) {
            interpreter?.close()
            interpreter = null // Nullify after closing
            isClosed = true
            Log.d(TAG, "Interpreter closed.")
        }
    }
}

// Make sure Constants.MIDAS_PATH is defined elsewhere, e.g.:
// object Constants { const val MIDAS_PATH = "midas_v2_1_small_256.tflite" } // Or your actual model name