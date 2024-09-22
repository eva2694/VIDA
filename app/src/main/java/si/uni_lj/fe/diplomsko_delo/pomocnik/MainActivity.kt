package si.uni_lj.fe.diplomsko_delo.pomocnik

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.google.android.gms.tflite.java.TfLite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore.ExploreScreen
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.theme.PomocnikTheme
import si.uni_lj.fe.erk.roadsigns.PermissionsUtil


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d("MainActivity", "Camera executor initialized")

        CoroutineScope(Dispatchers.Main).launch {
            try {
                TfLite.initialize(this@MainActivity).await()
                Log.d("MainActivity", "TensorFlow Lite initialized successfully")
                enableEdgeToEdge()
                setContent {
                    PomocnikTheme {
                        PermissionsUtil {
                            ExploreScreen()
                        }

                    }
                }
            } catch (e: Exception) {
                Log.e("MainActivity", "Error initializing TensorFlow Lite", e)
                e.printStackTrace()
            }
        }
    }


}
