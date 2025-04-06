import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.depth.DepthViewModel
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TTSManager

class DepthViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DepthViewModel::class.java)) {
            val tts = TTSManager.getInstance(context)
            return DepthViewModel(context, tts) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
