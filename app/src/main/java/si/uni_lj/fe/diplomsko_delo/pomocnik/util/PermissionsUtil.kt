package si.uni_lj.fe.diplomsko_delo.pomocnik.util

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import si.uni_lj.fe.diplomsko_delo.pomocnik.R

/**
 * Composable that handles camera permission requests.
 * Shows appropriate dialogs for permission states and guides users to settings if needed.
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
    
    var showRationale by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasCameraPermission = isGranted
        if (!isGranted) {
            // Check if we should show rationale
            if (context is ComponentActivity && context.shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                showRationale = true
            } else {
                // Permission permanently denied, show settings dialog
                showSettings = true
            }
        }
    }

    // Show rationale dialog
    if (showRationale) {
        AlertDialog(
            onDismissRequest = { showRationale = false },
            title = { Text(context.getString(R.string.permission_title)) },
            text = { Text(context.getString(R.string.permission_rationale)) },
            confirmButton = {
                Button(onClick = {
                    showRationale = false
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }) {
                    Text(context.getString(R.string.permission_grant))
                }
            },
            dismissButton = {
                Button(onClick = { showRationale = false }) {
                    Text(context.getString(R.string.permission_not_now))
                }
            }
        )
    }

    // Show settings dialog
    if (showSettings) {
        AlertDialog(
            onDismissRequest = { showSettings = false },
            title = { Text(context.getString(R.string.permission_settings_title)) },
            text = { Text(context.getString(R.string.permission_settings_message)) },
            confirmButton = {
                Button(onClick = {
                    showSettings = false
                    context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                    })
                }) {
                    Text(context.getString(R.string.permission_open_settings))
                }
            },
            dismissButton = {
                Button(onClick = { showSettings = false }) {
                    Text(context.getString(R.string.permission_cancel))
                }
            }
        )
    }

    // Request permission if not granted
    LaunchedEffect(key1 = true) {
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    // Show content if permission is granted
    if (hasCameraPermission) {
        content()
    }
}
