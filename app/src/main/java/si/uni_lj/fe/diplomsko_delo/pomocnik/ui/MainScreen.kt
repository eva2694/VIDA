package si.uni_lj.fe.diplomsko_delo.pomocnik.ui

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Assistant
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.ViewInAr
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import si.uni_lj.fe.diplomsko_delo.pomocnik.R
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.assist.AssistScreen
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

    var isExploreMenuExpanded by remember { mutableStateOf(false) }

    val modesRoutes = listOf("explore", "read", "depth")
    val isModesSelected = currentRoute in modesRoutes

    val navItemColors = NavigationBarItemDefaults.colors()

    val selectedDropdownItemColor = MaterialTheme.colorScheme.primary
    val defaultDropdownItemColor =
        MaterialTheme.colorScheme.secondary


    // Stop TTS when navigating
    DisposableEffect(navController) {
        val listener =
            NavController.OnDestinationChangedListener { _, destination: NavDestination, _ ->
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
            NavigationBar {
                // 1. Assist Item
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Assistant,
                            contentDescription = stringResource(R.string.tab_assist)
                        )
                    },
                    label = {
                        Text(
                            stringResource(R.string.tab_assist),
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 10.sp
                        )
                    },
                    selected = currentRoute == "assist",
                    onClick = {
                        if (currentRoute != "assist") {
                            isExploreMenuExpanded = false
                            coroutineScope.launch {
                                navController.navigate("assist") {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                )

                // 2. Modes Item Trigger
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clickable(
                            onClick = { isExploreMenuExpanded = true },
                            role = Role.Button,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    color = if (isModesSelected) navItemColors.selectedIndicatorColor else Color.Transparent,
                                    shape = RoundedCornerShape(50)
                                )
                                .padding(20.dp, 4.dp)
                        ) {
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = stringResource(R.string.tab_modes),
                                tint = if (isModesSelected) navItemColors.selectedIconColor else navItemColors.unselectedIconColor
                            )
                        }

                        Text(
                            stringResource(R.string.tab_modes),
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 10.sp,
                            textAlign = TextAlign.Center,
                            color = if (isModesSelected) navItemColors.selectedTextColor else navItemColors.unselectedTextColor
                        )
                    }

                    DropdownMenu(
                        expanded = isExploreMenuExpanded,
                        onDismissRequest = { isExploreMenuExpanded = false }
                    ) {
                        // Explore
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(
                                            color = Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .then(
                                            if (currentRoute == "explore") Modifier.border(
                                                width = 2.dp,
                                                color = selectedDropdownItemColor,
                                                shape = RoundedCornerShape(8.dp)
                                            ) else Modifier
                                        )
                                        .padding(8.dp)
                                        .fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.Visibility,
                                        contentDescription = stringResource(R.string.tab_explore),
                                        tint = if (currentRoute == "explore") selectedDropdownItemColor else defaultDropdownItemColor
                                    )
                                    Text(
                                        stringResource(R.string.tab_explore),
                                        color = if (currentRoute == "explore") selectedDropdownItemColor else defaultDropdownItemColor,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            },
                            onClick = {
                                isExploreMenuExpanded = false
                                if (currentRoute != "explore") {
                                    coroutineScope.launch {
                                        navController.navigate("explore") {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            }

                        )
                        // Read
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(
                                            color = Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .then(
                                            if (currentRoute == "read") Modifier.border(
                                                width = 2.dp,
                                                color = selectedDropdownItemColor,
                                                shape = RoundedCornerShape(8.dp)
                                            ) else Modifier
                                        )
                                        .padding(8.dp)
                                        .fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.TextFields,
                                        contentDescription = stringResource(R.string.tab_read),
                                        tint = if (currentRoute == "read") selectedDropdownItemColor else defaultDropdownItemColor
                                    )
                                    Text(
                                        stringResource(R.string.tab_read),
                                        color = if (currentRoute == "read") selectedDropdownItemColor else defaultDropdownItemColor,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            },
                            onClick = {
                                isExploreMenuExpanded = false
                                if (currentRoute != "read") {
                                    coroutineScope.launch {
                                        navController.navigate("read") {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            },

                            )
                        // Depth
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(
                                            color = Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .then(
                                            if (currentRoute == "depth") Modifier.border(
                                                width = 2.dp,
                                                color = selectedDropdownItemColor,
                                                shape = RoundedCornerShape(8.dp)
                                            ) else Modifier
                                        )
                                        .padding(8.dp)
                                        .fillMaxWidth()
                                ) {
                                    Icon(
                                        Icons.Default.ViewInAr,
                                        contentDescription = stringResource(R.string.tab_depth),
                                        tint = if (currentRoute == "depth") selectedDropdownItemColor else defaultDropdownItemColor
                                    )
                                    Text(
                                        stringResource(R.string.tab_depth),
                                        color = if (currentRoute == "depth") selectedDropdownItemColor else defaultDropdownItemColor,
                                        modifier = Modifier.padding(start = 8.dp)
                                    )
                                }
                            },
                            onClick = {
                                isExploreMenuExpanded = false
                                if (currentRoute != "depth") {
                                    coroutineScope.launch {
                                        navController.navigate("depth") {
                                            popUpTo(navController.graph.startDestinationId) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            },

                            )
                    }
                }

                // 3. Settings Item
                NavigationBarItem(
                    icon = {
                        Icon(
                            Icons.Default.Settings,
                            contentDescription = stringResource(R.string.tab_settings)
                        )
                    },
                    label = {
                        Text(
                            stringResource(R.string.tab_settings),
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 10.sp
                        )
                    },
                    selected = currentRoute == "settings",
                    onClick = {
                        if (currentRoute != "settings") {
                            isExploreMenuExpanded = false
                            coroutineScope.launch {
                                navController.navigate("settings") {
                                    popUpTo(navController.graph.startDestinationId) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "assist",
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            composable("assist") {
                AssistScreen(
                    cameraExecutor,
                    yoloModelLoader
                )
            }
            composable("explore") { ExploreScreen(cameraExecutor, yoloModelLoader, imageProcessor) }
            composable("read") { ReadScreen(cameraExecutor) }
            composable("depth") { DepthScreen(cameraExecutor, preferencesManager) }
            composable("settings") { SettingsScreen(preferencesManager) }
        }
    }
}