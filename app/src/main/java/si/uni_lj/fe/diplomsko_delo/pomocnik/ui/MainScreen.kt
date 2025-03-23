package si.uni_lj.fe.diplomsko_delo.pomocnik.ui

import android.util.Log
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import si.uni_lj.fe.diplomsko_delo.pomocnik.R
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.depth.DepthScreen
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore.ExploreScreen
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.read.ReadScreen
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.settings.SettingsScreen
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.ImageProcessor
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TTSManager
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.YoloModelLoader
import java.util.concurrent.ExecutorService


@Composable
fun MainScreen(
    cameraExecutor: ExecutorService,
    yoloModelLoader: YoloModelLoader,
    imageProcessor: ImageProcessor,
    preferencesManager: PreferencesManager
) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    DisposableEffect(navController) {
        val listener =
            NavController.OnDestinationChangedListener { _: NavController, destination: NavDestination, _ ->
                TTSManager.stop()
                Log.d("MainScreen", "Switched to: ${destination.route} — TTS stopped.")
            }
        navController.addOnDestinationChangedListener(listener)
        onDispose {
            navController.removeOnDestinationChangedListener(listener)
        }
    }

    Scaffold(
        bottomBar = {
            NavigationBar  {
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = "Explore screen"
                        )
                    },
                    label = { Text(stringResource(R.string.tab_explore)) },
                    selected = currentRoute == "explore",
                    onClick = {
                        coroutineScope.launch {
                            navController.navigate("explore"){
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }

                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.TextFields, contentDescription = "Read screen") },
                    label = { Text(stringResource(R.string.tab_read)) },
                    selected = currentRoute == "read",
                    onClick = {
                        coroutineScope.launch {
                            navController.navigate("read"){
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings screen") },
                    label = { Text(stringResource(R.string.tab_settings)) },
                    selected = currentRoute == "settings",
                    onClick = {
                        coroutineScope.launch {
                            navController.navigate("settings"){
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )

                NavigationBarItem(
                    icon = { Icon(Icons.Default.ViewInAr, contentDescription = "Depth screen") },
                    label = { Text(stringResource(R.string.tab_depth)) },
                    selected = currentRoute == "depth",
                    onClick = {
                        coroutineScope.launch {
                            navController.navigate("depth") {
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "explore",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable("explore") {
                ExploreScreen(cameraExecutor, yoloModelLoader, imageProcessor)
            }
            composable("read") {
                ReadScreen(cameraExecutor)
            }
            composable("settings") {
                SettingsScreen(preferencesManager)
            }
            composable("depth") {
                DepthScreen(cameraExecutor, preferencesManager)
            }
        }
    }
}

