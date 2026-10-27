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

/** Small user settings stored with DataStore (name, strictness, light/dark mode, mascot, break reminders). */
class SettingsRepository(context: Context) {

    private val store = context.applicationContext.dataStore

    private val nameKey = stringPreferencesKey("user_name")
    private val strictnessKey = stringPreferencesKey("strictness")
    private val darkModeKey = booleanPreferencesKey("dark_mode")
    private val celebratedKey = intPreferencesKey("celebrated_streak")
    private val onboardedKey = booleanPreferencesKey("onboarded")
    private val mascotKey = stringPreferencesKey("mascot_theme")
    private val potFlowersKey = intPreferencesKey("pot_flowers")
    private val potDaysKey = intPreferencesKey("pot_days")
    private val potLastDateKey = stringPreferencesKey("pot_last_date")
    private val bloomEventKey = stringPreferencesKey("bloom_event")
    private val breakEnabledKey = booleanPreferencesKey("break_enabled")
    private val breakMinutesKey = intPreferencesKey("break_minutes")
    private val breakStyleKey = stringPreferencesKey("break_style")
    private val breakRepeatKey = booleanPreferencesKey("break_repeat")

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

    /** Highest streak milestone the mascot already celebrated (reset to 0 when a streak breaks). */
    val celebratedStreak: Flow<Int> = store.data.map { it[celebratedKey] ?: 0 }

    suspend fun setCelebratedStreak(days: Int) {
        store.edit { it[celebratedKey] = days }
    }

    /** true after the first-run walkthrough; null on a fresh install (never set). */
    val onboarded: Flow<Boolean?> = store.data.map { it[onboardedKey] }

    suspend fun setOnboarded(done: Boolean) {
        store.edit { it[onboardedKey] = done }
    }

    /** Blobby (default) or Bloomy. */
    val mascotTheme: Flow<MascotTheme> = store.data.map { prefs ->
        MascotTheme.entries.find { it.name == prefs[mascotKey] } ?: MascotTheme.BLOBBY
    }

    suspend fun setMascotTheme(theme: MascotTheme) {
        store.edit { it[mascotKey] = theme.name }
    }

    /** Bloomy's current pot (see Growth). Finished pots are in the garden table instead. */
    val potProgress: Flow<PotProgress> = store.data.map { prefs ->
        PotProgress(prefs[potFlowersKey] ?: 0, prefs[potDaysKey] ?: 0, prefs[potLastDateKey])
    }

    suspend fun setPotProgress(progress: PotProgress) {
        store.edit { prefs ->
            prefs[potFlowersKey] = progress.flowers
            prefs[potDaysKey] = progress.daysTowardNext
            if (progress.lastDate == null) prefs.remove(potLastDateKey) else prefs[potLastDateKey] = progress.lastDate
        }
    }

    /** A new flower or full pot Bloomy hasn't celebrated yet (null = nothing to show). */
    val bloomEvent: Flow<BloomEvent?> = store.data.map { prefs ->
        BloomEvent.entries.find { it.name == prefs[bloomEventKey] }
    }

    /** A full pot beats a flower: never replace a pending POT with FLOWER. null clears it. */
    suspend fun setBloomEvent(event: BloomEvent?) {
        store.edit { prefs ->
            when {
                event == null -> prefs.remove(bloomEventKey)
                event == BloomEvent.FLOWER && prefs[bloomEventKey] == BloomEvent.POT.name -> Unit
                else -> prefs[bloomEventKey] = event.name
            }
        }
    }

    /** Break reminder choices (defaults: on, 20 min, notification + bubble, keep reminding). */
    val breakSettings: Flow<BreakSettings> = store.data.map { prefs ->
        BreakSettings(
            enabled = prefs[breakEnabledKey] ?: true,
            minutes = prefs[breakMinutesKey] ?: BreakSettings.DEFAULT_MINUTES,
            style = ReminderStyle.entries.find { it.name == prefs[breakStyleKey] } ?: ReminderStyle.BOTH,
            repeat = prefs[breakRepeatKey] ?: true
        )
    }

    suspend fun setBreakSettings(settings: BreakSettings) {
        store.edit { prefs ->
            prefs[breakEnabledKey] = settings.enabled
            prefs[breakMinutesKey] = settings.minutes
            prefs[breakStyleKey] = settings.style.name
            prefs[breakRepeatKey] = settings.repeat
        }
    }

    suspend fun setUserName(name: String) {
        store.edit { it[nameKey] = name.trim() }
    }

    suspend fun setStrictness(strictness: Strictness) {
        store.edit { it[strictnessKey] = strictness.name }
    }
}
