
package si.uni_lj.fe.diplomsko_delo.pomocnik


import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tflite.java.TfLite
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
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.UILangHelper
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var modelLoader: ModelLoader
    private lateinit var imageProcessor: ImageProcessor

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // For now: Lock orientation. (Landscape is almost handled btw, only BB are missing)
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        preferencesManager = PreferencesManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        modelLoader = ModelLoader(this, preferencesManager.language)
        imageProcessor = ImageProcessor()

        Log.d("MainActivity", "Camera executor initialized")

        lifecycleScope.launch(Dispatchers.IO) {
            TfLite.initialize(this@MainActivity).await()
            applyInitialTTSSettings()
        }

        observeSettingsChanges()
    }

    private suspend fun applyInitialTTSSettings() {
        val lang = preferencesManager.language.first()
        val speed = preferencesManager.readingSpeed.first()

        val tts = TTSManager.getInstance(applicationContext)
        tts.setLanguage(lang)
        tts.setSpeechRate(speed)
    }

    private fun observeSettingsChanges() {
        lifecycleScope.launch {
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

        lifecycleScope.launch {
            preferencesManager.language.collectLatest { lang ->
                TTSManager.getInstance(applicationContext).setLanguage(lang)
                UILangHelper().changeUILanguage(applicationContext, lang)
            }
        }

        lifecycleScope.launch {
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
