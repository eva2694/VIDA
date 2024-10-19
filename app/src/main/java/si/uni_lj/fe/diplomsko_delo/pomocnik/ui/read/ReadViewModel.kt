package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.read

import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TextToSpeech


class ReadViewModel(private val tts: TextToSpeech) : ViewModel() {

    private val textRecognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    @OptIn(ExperimentalGetImage::class)
    fun processImage(image: ImageProxy) {
        val inputImage = InputImage.fromMediaImage(image.image!!, image.imageInfo.rotationDegrees)

        textRecognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                val recognizedText = visionText.text
                if (recognizedText.isNotEmpty()) {
                    tts.readText(recognizedText)
                }
            }
            .addOnFailureListener { e ->
                Log.e("ReadVieModel", "There was an error with text recognition! : $e")
                e.printStackTrace()
            }
            .addOnCompleteListener {
                image.close()
            }
    }
}
