package si.uni_lj.fe.diplomsko_delo.pomocnik.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore.ExploreScreen
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore.ExploreViewModel
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.read.ReadScreen
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.read.ReadViewModel
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.TextToSpeech
import java.util.concurrent.ExecutorService


@Composable
fun MainScreen(cameraExecutor: ExecutorService, exploreViewModel: ExploreViewModel, readViewModel: ReadViewModel, tts: TextToSpeech) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            NavigationBar  {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Visibility, contentDescription = "Explore") },
                    label = { Text("Razglej se") },
                    selected = currentRoute == "explore",
                    onClick = {
                        coroutineScope.launch {
                            tts.stop()
                            navController.navigate("explore"){
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }

                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.TextFields, contentDescription = "Read") },
                    label = { Text("Beri") },
                    selected = currentRoute == "read",
                    onClick = {
                        coroutineScope.launch {
                            tts.stop()
                            navController.navigate("read"){
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
                ExploreScreen(cameraExecutor, exploreViewModel)
            }
            composable("read") {
                ReadScreen(cameraExecutor, readViewModel)
            }
        }
    }
}

