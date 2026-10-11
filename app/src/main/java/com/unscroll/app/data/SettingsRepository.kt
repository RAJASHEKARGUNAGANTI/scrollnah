package com.unscroll.app.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

/** Small user settings stored with DataStore (name, strictness, light/dark mode). */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private val nameKey = stringPreferencesKey("user_name")
    private val strictnessKey = stringPreferencesKey("strictness")
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val celebratedKey = intPreferencesKey("celebrated_streak")
    private val onboardedKey = booleanPreferencesKey("onboarded")

    val userName: Flow<String> = store.data.map { it[nameKey].orEmpty() }

    val strictness: Flow<Strictness> = store.data.map { prefs ->
        Strictness.entries.find { it.name == prefs[strictnessKey] } ?: Strictness.FIRM
    }

    /** true = dark, false = light, null = not chosen yet (follow the phone's setting). */
    val darkMode: Flow<Boolean?> = store.data.map { it[darkModeKey] }

    suspend fun setDarkMode(dark: Boolean) {
        store.edit { it[darkModeKey] = dark }
    }

    /** Back to following the phone's light/dark setting. */
    suspend fun followSystemTheme() {
        store.edit { it.remove(darkModeKey) }
    }

    /** Highest streak milestone Blobby already celebrated (reset to 0 when a streak breaks). */
    val celebratedStreak: Flow<Int> = store.data.map { it[celebratedKey] ?: 0 }

    suspend fun setCelebratedStreak(days: Int) {
        store.edit { it[celebratedKey] = days }
    }

    /** true after the first-run walkthrough; null on a fresh install (never set). */
    val onboarded: Flow<Boolean?> = store.data.map { it[onboardedKey] }

    suspend fun setOnboarded(done: Boolean) {
        store.edit { it[onboardedKey] = done }
    }

    suspend fun setUserName(name: String) {
        store.edit { it[nameKey] = name.trim() }
    }

    suspend fun setStrictness(strictness: Strictness) {
        store.edit { it[strictnessKey] = strictness.name }
    }
}
