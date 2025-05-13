package si.uni_lj.fe.diplomsko_delo.pomocnik.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import si.uni_lj.fe.diplomsko_delo.pomocnik.R
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.theme.iconColor
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.theme.languageSelectionDark
import si.uni_lj.fe.diplomsko_delo.pomocnik.ui.theme.languageSelectionLight
import si.uni_lj.fe.diplomsko_delo.pomocnik.util.PreferencesManager

@Composable
fun LanguageSelectionScreen(
    preferencesManager: PreferencesManager,
    onLanguageSelected: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val isDarkTheme = isSystemInDarkTheme()
    val backgroundColor = if (isDarkTheme) languageSelectionDark else languageSelectionLight

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = iconColor,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 32.dp)
                )

                Image(
                    painter = painterResource(id = R.drawable.vida_no_bg),
                    contentDescription = "VIDA Logo",
                    modifier = Modifier
                        .size(200.dp)
                        .padding(bottom = 64.dp)
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            preferencesManager.setLanguage("sl")
                            onLanguageSelected()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .padding(bottom = 24.dp)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = iconColor
                    )
                ) {
                    Text(
                        text = stringResource(R.string.language_selection_slovenian),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp
                        ),
                        color = backgroundColor
                    )
                }

                Button(
                    onClick = {
                        coroutineScope.launch {
                            preferencesManager.setLanguage("en")
                            onLanguageSelected()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = iconColor
                    )
                ) {
                    Text(
                        text = stringResource(R.string.language_selection_english),
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontSize = 20.sp
                        ),
                        color = backgroundColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
} 