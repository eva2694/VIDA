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
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import si.uni_lj.fe.diplomsko_delo.pomocnik.R


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
        Column(modifier = Modifier.padding(16.dp)) {
            BasicText(
                text = "Camera permission is required for this app to work!",
                style = TextStyle(
                    color = Color.Red,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                ),
                modifier = Modifier.padding(16.dp)
            )

            Image(
                painter = painterResource(id = R.drawable.lock),
                contentDescription = "Cute puppy",
                modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(8.dp))
            )

        }

    }
}
