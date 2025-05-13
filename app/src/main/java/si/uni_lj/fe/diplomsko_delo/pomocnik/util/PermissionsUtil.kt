package si.uni_lj.fe.diplomsko_delo.pomocnik.util

import android.Manifest
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Composable that handles camera permission requests.
 * Displays a warning message if permission is not granted.
 * 
 * @param content The content to display when camera permission is granted
 */
@Composable
fun PermissionsUtil(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        Log.d("RequestCameraPermission", "Camera permission granted: $isGranted")
    }

    LaunchedEffect(key1 = true) {
        Log.d("RequestCameraPermission", "Launching camera permission request")
        launcher.launch(Manifest.permission.CAMERA)
    }

    if (hasCameraPermission) {
        content()
    } else {
        Log.d("RequestCameraPermission", "Camera permission not granted, displaying warning message")
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier.size(100.dp)
                ){}
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Locked icon",
                        modifier = Modifier
                            .size(100.dp)
                            .padding(top = 24.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
