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

val Context.dataStore by preferencesDataStore(name = "app_preferences")

class PreferencesManager(private val context: Context) {

    companion object {
        val LANGUAGE_KEY = stringPreferencesKey("language")
        val SPEED_KEY = floatPreferencesKey("reading_speed")
        val DARK_MODE_KEY = booleanPreferencesKey("dark_mode")
    }

    val language: Flow<String> = context.dataStore.data
        .map { preferences ->
            preferences[LANGUAGE_KEY] ?: "SI"
        }

    val readingSpeed: Flow<Float> = context.dataStore.data
        .map { preferences ->
            preferences[SPEED_KEY] ?: 0.75f
        }

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[DARK_MODE_KEY] ?: false
        }

    suspend fun setLanguage(language: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language
        }
    }

    suspend fun setReadingSpeed(speed: Float) {
        context.dataStore.edit { preferences ->
            preferences[SPEED_KEY] = speed
        }
    }

    suspend fun setDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[DARK_MODE_KEY] = isDarkMode
        }
    }

    suspend fun getLanguage(): String {
        val preferences = context.dataStore.data.first()
        return preferences[LANGUAGE_KEY] ?: "SI"
    }

    suspend fun getReadingSpeed(): Float {
        val preferences = context.dataStore.data.first()
        return preferences[SPEED_KEY] ?: 0.75f
    }

    suspend fun getDarkMode(): Boolean {
        val preferences = context.dataStore.data.first()
        return preferences[DARK_MODE_KEY] ?: false
    }
}