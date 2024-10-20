@file:Suppress("DEPRECATION")

package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.settings

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.coroutines.launch
import androidx.compose.runtime.livedata.observeAsState


@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {

    val coroutineScope = rememberCoroutineScope()

    val language by viewModel.language.observeAsState()
    val readingSpeed by viewModel.readingSpeed.collectAsState(initial = 1.0f)
    val isDarkMode by viewModel.isDarkMode.collectAsState(initial = false)

    val languageChanged by viewModel.languageChanged.collectAsState(initial = false)
    val readingSpeedChanged by viewModel.readingSpeedChanged.collectAsState(initial = false)
    val darkModeChanged by viewModel.darkModeChanged.collectAsState(initial = false)

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(20.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            Text("Select Language", style = MaterialTheme.typography.titleLarge)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                language?.let { LanguageOption("Slovenian", "sl", language!!, viewModel) }
                language?.let { LanguageOption("English", "en", language!!, viewModel) }
            }

            Divider(modifier = Modifier.padding(vertical = 20.dp))

            Text(
                "Select Reading Speed: ${String.format("%.2f", readingSpeed)}",
                style = MaterialTheme.typography.titleLarge
            )
            Slider(
                value = readingSpeed,
                onValueChange = {
                    viewModel.setReadingSpeed(it)
                },
                valueRange = 0.25f..2f,
                steps = 6,
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .padding(horizontal = 16.dp)
            )

            Divider(modifier = Modifier.padding(vertical = 20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Dark Mode", modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleLarge)
                Switch(
                    checked = isDarkMode,
                    onCheckedChange = { viewModel.setDarkMode(it) },
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
            }

            Divider(modifier = Modifier.padding(vertical = 20.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        val context = viewModel.context

                        if (languageChanged) {
                            language?.let { notifyLanguageSettingsChanged(context, it) }
                        }
                        if (readingSpeedChanged) {
                            notifySpeedSettingsChanged(context)
                        }
                        if (darkModeChanged) {
                            notifyDarkModeSettingsChanged(context)
                        }

                        snackbarHostState.showSnackbar(
                            message = "Settings applied successfully",
                            actionLabel = "OK",
                            duration = SnackbarDuration.Short
                        )

                        viewModel.resetChangeFlags()
                    }
                },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Text("OK")
            }
        }
    }
}

@Composable
fun LanguageOption(label: String, languageCode: String, selectedLanguage: String, viewModel: SettingsViewModel) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = languageCode == selectedLanguage,
            onClick = { viewModel.setLanguageChanged(languageCode) }
        )
        Text(label, modifier = Modifier.padding(4.dp))
    }
}

private fun notifyLanguageSettingsChanged(context: Context, language: String) {
    val intent = Intent("LANGUAGE_SETTINGS_CHANGED").apply {
        putExtra("language", language)
    }
    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
}

private fun notifySpeedSettingsChanged(context: Context) {
    val intent = Intent("SPEED_SETTINGS_CHANGED")
    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
}

private fun notifyDarkModeSettingsChanged(context: Context) {
    val intent = Intent("DARK_MODE_SETTINGS_CHANGED")
    LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
}
