package si.uni_lj.fe.diplomsko_delo.pomocnik.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import si.uni_lj.fe.diplomsko_delo.pomocnik.R
import kotlinx.coroutines.delay

/**
 * Splash screen composable that displays the app logo and loading indicator.
 * Handles the initial animation and timing before transitioning to the main app.
 *
 * @param onSplashComplete Callback function to be invoked when splash animation completes
 */
@Composable
fun SplashScreen(
    onSplashComplete: () -> Unit
) {
    // Animation state
    var startAnimation by remember { mutableStateOf(false) }
    val alphaAnim = animateFloatAsState(
        targetValue = if (startAnimation) 1f else 0f,
        animationSpec = tween(durationMillis = 2000)
    )

    // Start animation and schedule completion
    LaunchedEffect(key1 = true) {
        startAnimation = true
        delay(2500)
        onSplashComplete()
    }

    // Main layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App logo with fade-in animation
            Image(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(120.dp)
                    .alpha(alphaAnim.value)
            )
            Spacer(modifier = Modifier.height(16.dp))
            // Loading indicator with fade-in animation
            CircularProgressIndicator(
                modifier = Modifier.alpha(alphaAnim.value),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
} 