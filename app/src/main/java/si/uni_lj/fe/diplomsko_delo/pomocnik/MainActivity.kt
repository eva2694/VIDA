@file:Suppress("DEPRECATION")

package si.uni_lj.fe.diplomsko_delo.pomocnik

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.ViewModelProvider
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.MainScreen
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore.ExploreViewModel
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore.ExploreViewModelFactory
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.read.ReadViewModel
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.read.ReadViewModelFactory
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.settings.SettingsViewModel
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.settings.SettingsViewModelFactory
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.theme.PomocnikTheme
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.ImageProcessor
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.ModelLoader
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PermissionsUtil
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TextToSpeech
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : ComponentActivity() {
    private lateinit var cameraExecutor: ExecutorService
    private  lateinit var tts: TextToSpeech
    private lateinit var preferencesManager: PreferencesManager
    private lateinit var modelLoader: ModelLoader
    private lateinit var imageProcessor: ImageProcessor

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            updateSettings()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        cameraExecutor = Executors.newSingleThreadExecutor()
        modelLoader = ModelLoader(this)
        imageProcessor = ImageProcessor()
        preferencesManager = PreferencesManager(this)
        tts = TextToSpeech(this)

        val exploreViewModel: ExploreViewModel = ViewModelProvider(
            this,
            ExploreViewModelFactory(modelLoader, imageProcessor, tts)
        )[ExploreViewModel::class.java]

        val readViewModel: ReadViewModel = ViewModelProvider(
            this,
            ReadViewModelFactory(tts)
        )[ReadViewModel::class.java]

        val settingsViewModel: SettingsViewModel = ViewModelProvider(
            this,
            SettingsViewModelFactory(preferencesManager, this)
        )[SettingsViewModel::class.java]

        Log.d("MainActivity", "Camera executor initialized")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                TfLite.initialize(this@MainActivity).await()
                Log.d("MainActivity", "TensorFlow Lite initialized successfully")
                enableEdgeToEdge()
                setContent {
                    PomocnikTheme {
                        PermissionsUtil {
                            MainScreen(
                                cameraExecutor,
                                exploreViewModel = exploreViewModel,
                                readViewModel = readViewModel,
                                tts = tts,
                                settingsViewModel = settingsViewModel
                            )
                        }
                    }
                }
                updateTTSLanguage()
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing TensorFlow Lite", e)
                e.printStackTrace()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.d("MainActivity", "onDestroy called")
        tts.shutdown()
        cameraExecutor.shutdown()
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "onResume called")
    }

    override fun onStart() {
        super.onStart()
        Log.d("MainActivity", "onStart called")
        val filter = IntentFilter("SETTINGS_CHANGED")
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter)
    }

    override fun onStop() {
        super.onStop()
        Log.d("MainActivity", "onStop called")
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    private fun updateSettings() {
        CoroutineScope(Dispatchers.Main).launch {
            val newLanguage = preferencesManager.getLanguage()
            val newReadingSpeed = preferencesManager.getReadingSpeed()
            // todo : val isDarkMode = preferencesManager.getDarkMode()

            tts.setLanguage(newLanguage)
            tts.setSpeechRate(newReadingSpeed)

            modelLoader.updateLabels(newLanguage)
        }

    }

    private fun updateTTSLanguage() {
        CoroutineScope(Dispatchers.Main).launch {
            val newLanguage = preferencesManager.getLanguage()
            tts.setLanguage(newLanguage)
        }
    }

    private fun refreshUI(language: String, readingSpeed: Float, isDarkMode: Boolean) {
        // todo
    }


}
