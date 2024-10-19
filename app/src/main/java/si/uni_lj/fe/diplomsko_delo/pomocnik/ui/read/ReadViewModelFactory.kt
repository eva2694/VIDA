package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.read

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TextToSpeech

class ReadViewModelFactory(
    private val tts: TextToSpeech
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ReadViewModel::class.java)) {
            return ReadViewModel(tts) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}