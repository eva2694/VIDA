package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(viewModel: SettingsViewModel) {
    val language by viewModel.language.collectAsState(initial = "SI")
    val readingSpeed by viewModel.readingSpeed.collectAsState(initial = 0.75f)
    val isDarkMode by viewModel.isDarkMode.collectAsState(initial = false)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {
        Spacer(modifier = Modifier.height(20.dp))

        Text("Select Language", style = MaterialTheme.typography.titleLarge)

        Row(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            LanguageOption("Slovenian", "SI", language, viewModel)
            LanguageOption("English", "EN", language, viewModel)
        }

        Divider(modifier = Modifier.padding(vertical = 20.dp))

        Text("Select Reading Speed: ${String.format("%.2f", readingSpeed)}", style = MaterialTheme.typography.titleLarge)
        Slider(
            value = readingSpeed,
            onValueChange = { viewModel.setReadingSpeed(it) },
            valueRange = 0.25f..2f,
            steps = 6,
            modifier = Modifier.padding(vertical = 8.dp).padding(horizontal = 16.dp)
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
