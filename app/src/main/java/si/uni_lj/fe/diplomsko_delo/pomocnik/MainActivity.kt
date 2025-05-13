package si.uni_lj.fe.diplomsko_delo.pomocnik

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.MainScreen
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.theme.PomocnikTheme
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.AppImageProcessor
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PermissionsUtil
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TTSManager
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.UILangHelper
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.YoloModelLoader
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Main activity that serves as the entry point of the application.
 * Handles initialization of core components and manages the app's lifecycle.
 */
class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var yoloModelLoader: YoloModelLoader
    private lateinit var appImageProcessor: AppImageProcessor

    @SuppressLint("SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        // Handle splash screen based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val splashScreen = installSplashScreen()
            super.onCreate(savedInstanceState)
        } else {
            super.onCreate(savedInstanceState)
        }

        // Lock orientation to portrait mode
        this.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        preferencesManager = PreferencesManager(this)
        cameraExecutor = Executors.newSingleThreadExecutor()
        yoloModelLoader = YoloModelLoader(this, preferencesManager.language)
        appImageProcessor = AppImageProcessor()

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
                                yoloModelLoader,
                                appImageProcessor,
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
