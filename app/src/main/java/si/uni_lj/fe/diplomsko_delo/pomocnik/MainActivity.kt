
package si.uni_lj.fe.diplomsko_delo.pomocnik


import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.MainScreen
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.theme.PomocnikTheme
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.ImageProcessor
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.ModelLoader
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PermissionsUtil
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TTSManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var modelLoader: ModelLoader
    private lateinit var imageProcessor: ImageProcessor

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferencesManager = PreferencesManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        modelLoader = ModelLoader(this, preferencesManager.language)
        imageProcessor = ImageProcessor()

        Log.d("MainActivity", "Camera executor initialized")

        CoroutineScope(Dispatchers.Main).launch {
            TfLite.initialize(this@MainActivity).await()
            applyInitialTTSSettings()
            observeSettingsChanges()
        }


    }

    private suspend fun applyInitialTTSSettings() {
        val lang = preferencesManager.language.first()
        val speed = preferencesManager.readingSpeed.first()

        val tts = TTSManager.getInstance(applicationContext)
        tts.setLanguage(lang)
        tts.setSpeechRate(speed)
    }

    private fun observeSettingsChanges() {
        CoroutineScope(Dispatchers.Main).launch {
            preferencesManager.isDarkMode.collectLatest { isDarkMode ->
                setContent {
                    PomocnikTheme(darkTheme = isDarkMode) {
                        PermissionsUtil {
                            MainScreen(
                                cameraExecutor,
                                modelLoader,
                                imageProcessor,
                                preferencesManager
                            )
                        }
                    }
                }
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            preferencesManager.language.collectLatest { lang ->
                TTSManager.getInstance(applicationContext).setLanguage(lang)
            }
        }

        CoroutineScope(Dispatchers.Main).launch {
            preferencesManager.readingSpeed.collectLatest { speed ->
                TTSManager.getInstance(applicationContext).setSpeechRate(speed)
            }
        }
    }

    override fun onDestroy() {
        cameraExecutor.shutdown()
        if (!isChangingConfigurations) {
            TTSManager.shutdown()
        }
        super.onDestroy()
    }

}
