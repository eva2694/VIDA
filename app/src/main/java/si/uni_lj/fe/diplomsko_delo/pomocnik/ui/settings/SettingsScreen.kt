@file:Suppress("DEPRECATION")

package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.settings


import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager


@Composable
fun SettingsScreen(preferencesManager: PreferencesManager) {
    val context = LocalContext.current
    val viewModelFactory = remember {
        SettingsViewModelFactory(preferencesManager, context);
    }
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)

    val language by viewModel.language.collectAsState(initial = "sl")
    val readingSpeed by viewModel.readingSpeed.collectAsState(initial = 1.0f)
    val isDarkMode by viewModel.isDarkMode.collectAsState(initial = false)

    Scaffold { paddingValues ->
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
                LanguageOption("Slovenian", "sl", language, viewModel)
                LanguageOption("English", "en", language, viewModel)
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
            onClick = { viewModel.setLanguage(languageCode) }
        )
        Text(label, modifier = Modifier.padding(4.dp))
    }
}