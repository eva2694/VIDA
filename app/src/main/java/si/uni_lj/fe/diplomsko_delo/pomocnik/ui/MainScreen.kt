package si.uni_lj.fe.diplomsko_delo.pomocnik.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore.ExploreScreen
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.explore.ExploreViewModel
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.read.ReadScreen
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.read.ReadViewModel
import java.util.concurrent.ExecutorService


@Composable
fun MainScreen(cameraExecutor: ExecutorService, exploreViewModel: ExploreViewModel, readViewModel: ReadViewModel) {
    val navController = rememberNavController()
    val coroutineScope = rememberCoroutineScope()



    Scaffold(
        bottomBar = {
            NavigationBar  {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Visibility, contentDescription = "Explore") },
                    label = { Text("Razglej se") },
                    selected = navController.currentDestination?.route == "explore",
                    onClick = {
                        coroutineScope.launch {
                            navController.navigate("explore")
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.TextFields, contentDescription = "Read") },
                    label = { Text("Beri") },
                    selected = navController.currentDestination?.route == "read",
                    onClick = {
                        coroutineScope.launch {
                            navController.navigate("read")
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

