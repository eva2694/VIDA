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

class PreferencesManager(private val context: Context) {

    companion object {
        val LANGUAGE = stringPreferencesKey("language")
        val READING_SPEED = floatPreferencesKey("reading_speed")
        val DARK_MODE = booleanPreferencesKey("dark_mode")
    }

    val language: Flow<String> = context.dataStore.data
        .map { it[LANGUAGE] ?: "sl" }

    val readingSpeed: Flow<Float> = context.dataStore.data
        .map { it[READING_SPEED] ?: 0.7f }

    val isDarkMode: Flow<Boolean> = context.dataStore.data
        .map { it[DARK_MODE] ?: false }

    suspend fun setLanguage(value: String) {
        context.dataStore.edit { it[LANGUAGE] = value }
    }

    suspend fun setReadingSpeed(value: Float) {
        context.dataStore.edit { it[READING_SPEED] = value }
    }

    suspend fun setDarkMode(value: Boolean) {
        context.dataStore.edit { it[DARK_MODE] = value }
    }

    suspend fun getDarkMode(): Boolean {
        return context.dataStore.data.map { it[DARK_MODE] ?: false }.first()
    }

}