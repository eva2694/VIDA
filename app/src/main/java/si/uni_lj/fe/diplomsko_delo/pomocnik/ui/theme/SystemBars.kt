package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.theme

import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager

@Composable
fun SetSystemBarsColor(preferencesManager: PreferencesManager) {
    val context = LocalContext.current as ComponentActivity
    val navBarColor = MaterialTheme.colorScheme.surfaceColorAtElevation(3.dp).toArgb()

    // Use collectAsState to observe the dark mode preference
    val isDark by preferencesManager.isDarkMode.collectAsState(initial = false)

    DisposableEffect(isDark, navBarColor) {
        context.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                navBarColor,
                navBarColor
            ),
            navigationBarStyle = SystemBarStyle.light(
                navBarColor,
                navBarColor
            )
        )
        onDispose { }
    }
}
