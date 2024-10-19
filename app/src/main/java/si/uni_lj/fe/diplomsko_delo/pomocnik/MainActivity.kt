package si.uni_lj.fe.diplomsko_delo.pomocnik

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.MainScreen
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore.ExploreViewModel
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore.ExploreViewModelFactory
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.theme.PomocnikTheme
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.ImageProcessor
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.ModelLoader
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PermissionsUtil
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TextToSpeech
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private  lateinit var tts: TextToSpeech
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cameraExecutor = Executors.newSingleThreadExecutor()

        tts = TextToSpeech(this)
        val modelLoader = ModelLoader(this)
        val imageProcessor = ImageProcessor()

        val exploreViewModel: ExploreViewModel = ViewModelProvider(
            this,
            ExploreViewModelFactory(modelLoader, imageProcessor, tts)
        )[ExploreViewModel::class.java]

        Log.d("MainActivity", "Camera executor initialized")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                TfLite.initialize(this@MainActivity).await()
                Log.d("MainActivity", "TensorFlow Lite initialized successfully")
                enableEdgeToEdge()
                setContent {
                    PomocnikTheme {
                        PermissionsUtil {
                            MainScreen(cameraExecutor, viewModel = exploreViewModel)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing TensorFlow Lite", e)
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        tts.shutdown()
        cameraExecutor.shutdown()
    }


}
