package si.uni_lj.fe.diplomsko_delo.pomocnik.util

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private const val PREFERENCES_NAME = "user_preferences"

private val Context.dataStore by preferencesDataStore(name = PREFERENCES_NAME)

/**
 * Manages user preferences using DataStore.
 * Handles language, reading speed, and theme settings.
 */
class PreferencesManager(private val context: Context) {

    companion object {
        val LANGUAGE = stringPreferencesKey("language")
        val READING_SPEED = floatPreferencesKey("reading_speed")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
        val HAS_SELECTED_LANGUAGE = booleanPreferencesKey("has_selected_language")
    }

    /** Current language setting (default: "sl") */
    val language: Flow<String> = context.dataStore.data
        .map { it[LANGUAGE] ?: "sl" }

    /** Text-to-speech reading speed (default: 1.0) */
    val readingSpeed: Flow<Float> = context.dataStore.data
        .map { it[READING_SPEED] ?: 1.0f }

    /** Dark mode setting (default: false) */
    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { it[DARK_MODE] ?: false }

    /** Whether the user has made their initial language selection (default: false) */
    val hasSelectedLanguage: Flow<Boolean> = context.dataStore.data
        .map { it[HAS_SELECTED_LANGUAGE] ?: false }

    /**
     * Updates the language setting.
     * @param value Language code ("sl" for Slovenian, "en" for English)
     */
    suspend fun setLanguage(value: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE] = value
            preferences[HAS_SELECTED_LANGUAGE] = true
        }
    }

    /**
     * Updates the text-to-speech reading speed.
     * @param value Speed multiplier (0.5 to 2.0)
     */
    suspend fun setReadingSpeed(value: Float) {
        context.dataStore.edit { it[READING_SPEED] = value }
    }

    /**
     * Updates the dark mode setting.
     * @param value true for dark mode, false for light mode
     */
    suspend fun setDarkMode(value: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = value }
    }

    /**
     * Retrieves the current dark mode setting.
     * @return true if dark mode is enabled, false if light mode is enabled, null if not set
     */
    suspend fun getDarkMode(): Boolean? {
        return context.dataStore.data.map { it[DARK_MODE] }.first()
    }
}