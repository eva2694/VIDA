@file:Suppress("DEPRECATION")

package si.uni_lj.fe.diplomsko_delo.pomocnik.ui.settings


import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import si.uni_lj.fe.diplomsko_delo.pomocnik.R
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager

/**
 * Screen that allows users to configure app settings.
 * Includes language selection, reading speed adjustment, and dark mode toggle.
 */
@SuppressLint("DefaultLocale")
@Composable
fun SettingsScreen(preferencesManager: PreferencesManager) {
    val viewModelFactory = remember {
        SettingsViewModelFactory(preferencesManager)
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
            Text(
                stringResource(R.string.settings_title_lang),
                style = MaterialTheme.typography.titleLarge
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column {
                    LanguageOption(
                        stringResource(R.string.settings_sl),
                        "sl",
                        language,
                        viewModel
                    )
                    LanguageOption(
                        stringResource(R.string.settings_eng),
                        "en",
                        language,
                        viewModel

                    )
                }

            }

            Divider(modifier = Modifier.padding(vertical = 20.dp))

            Text(
                "${stringResource(R.string.setting_rs_title)}: ${
                    String.format(
                        "%.2f",
                        readingSpeed
                    )
                }",
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
                Text(
                    stringResource(R.string.settings_title_dm),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleLarge
                )
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

/**
 * Composable for a language selection option.
 */
@Composable
fun LanguageOption(
    label: String,
    languageCode: String,
    selectedLanguage: String,
    viewModel: SettingsViewModel
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(
            selected = languageCode == selectedLanguage,
            onClick = {
                viewModel.setLanguage(languageCode)
            }
        )
        Text(label, modifier = Modifier.padding(4.dp))
    }
}