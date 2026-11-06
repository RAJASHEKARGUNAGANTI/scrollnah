package com.unscroll.app.data

import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import kotlinx.coroutines.flow.first
import org.json.JSONObject
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
    private val timeLeftBubbleKey = booleanPreferencesKey("time_left_bubble")
    private val reelBadgeKey = booleanPreferencesKey("reel_badge")
    private val widgetCardDoneKey = booleanPreferencesKey("widget_card_done")
    private val reelCardDoneKey = booleanPreferencesKey("reel_card_done")
    private val soundsKey = booleanPreferencesKey("sounds")
    private val slowPlanOnKey = booleanPreferencesKey("slow_plan_on")
    private val slowPlanStepKey = intPreferencesKey("slow_plan_step")
    private val slowPlanGoalKey = intPreferencesKey("slow_plan_goal")
    private val slowPlanLastKey = stringPreferencesKey("slow_plan_last")
    private val popupsConfirmedKey = booleanPreferencesKey("popups_confirmed")
    private val weekReadyNoteKey = booleanPreferencesKey("week_ready_note")
    private val insteadIdeasKey = stringPreferencesKey("instead_ideas")
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

    // ---- Rewards (hats and pot colours earned with streaks) ----

    private val bestStreakEverKey = intPreferencesKey("best_streak_ever")
    private fun rewardKey(theme: MascotTheme) = stringPreferencesKey("reward_${theme.name.lowercase()}")

    /** The longest streak ever seen. Only goes up, so "Clear history" doesn't take rewards away. */
    val bestStreakEver: Flow<Int> = store.data.map { it[bestStreakEverKey] ?: 0 }

    suspend fun noteStreak(best: Int) {
        store.edit { if (best > (it[bestStreakEverKey] ?: 0)) it[bestStreakEverKey] = best }
    }

    /** What the current mascot wears (null = nothing), if it's still unlocked. */
    val activeReward: Flow<MascotReward?> = store.data.map { prefs ->
        val theme = MascotTheme.entries.find { it.name == prefs[mascotKey] } ?: MascotTheme.BLOBBY
        MascotReward.entries.find { it.name == prefs[rewardKey(theme)] && it.theme == theme }
    }

    suspend fun setReward(theme: MascotTheme, reward: MascotReward?) {
        store.edit { if (reward == null) it.remove(rewardKey(theme)) else it[rewardKey(theme)] = reward.name }
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

    /** Show "12 min of Instagram left today" when a limited app is opened (default on). */
    val timeLeftBubble: Flow<Boolean> = store.data.map { it[timeLeftBubbleKey] ?: true }

    suspend fun setTimeLeftBubble(on: Boolean) {
        store.edit { it[timeLeftBubbleKey] = on }
    }

    /** Show today's Reels/Shorts count with the mascot while Reels or Shorts are open (default on; needs the tracker). */
    val reelBadge: Flow<Boolean> = store.data.map { it[reelBadgeKey] ?: true }

    suspend fun setReelBadge(on: Boolean) {
        store.edit { it[reelBadgeKey] = on }
    }

    /** The mascot's sounds in the app and on notifications (default on; see SoundFx). */
    val sounds: Flow<Boolean> = store.data.map { it[soundsKey] ?: true }

    suspend fun setSounds(on: Boolean) {
        store.edit { it[soundsKey] = on }
    }

    /** The slow-down plan (limits a little lower every week, see [SlowDownPlan]). */
    val slowPlan: Flow<SlowPlan> = store.data.map {
        SlowPlan(
            on = it[slowPlanOnKey] ?: false,
            stepMinutes = it[slowPlanStepKey] ?: SlowDownPlan.DEFAULT_STEP,
            goalMinutes = it[slowPlanGoalKey] ?: SlowDownPlan.DEFAULT_GOAL,
            lastStep = it[slowPlanLastKey]
        )
    }

    /** Switching the plan on starts the week from [today]; off forgets the date. */
    suspend fun setSlowPlanOn(on: Boolean, today: java.time.LocalDate = java.time.LocalDate.now()) {
        store.edit {
            it[slowPlanOnKey] = on
            if (on) it[slowPlanLastKey] = today.toString() else it.remove(slowPlanLastKey)
        }
    }

    suspend fun setSlowPlanStep(minutes: Int) {
        store.edit { it[slowPlanStepKey] = minutes }
    }

    suspend fun setSlowPlanGoal(minutes: Int) {
        store.edit { it[slowPlanGoalKey] = minutes }
    }

    suspend fun setSlowPlanLastStep(day: java.time.LocalDate) {
        store.edit { it[slowPlanLastKey] = day.toString() }
    }

    /** Home's "Put me on your home screen" card was used or closed, so it doesn't come back. */
    val widgetCardDone: Flow<Boolean> = store.data.map { it[widgetCardDoneKey] ?: false }

    suspend fun setWidgetCardDone() {
        store.edit { it[widgetCardDoneKey] = true }
    }

    /** Home's "Count your Reels & Shorts" card was closed, or the tracker was turned on: it doesn't come back. */
    val reelCardDone: Flow<Boolean> = store.data.map { it[reelCardDoneKey] ?: false }

    suspend fun setReelCardDone() {
        store.edit { it[reelCardDoneKey] = true }
    }

    /** vivo/iQOO: the user told us they allowed pop-ups in the background (the phone can't be asked). */
    val popupsConfirmed: Flow<Boolean> = store.data.map { it[popupsConfirmedKey] ?: false }

    suspend fun setPopupsConfirmed() {
        store.edit { it[popupsConfirmedKey] = true }
    }

    /** Sunday evening note that the week in review is ready. On by default. */
    val weekReadyNote: Flow<Boolean> = store.data.map { it[weekReadyNoteKey] ?: true }

    suspend fun setWeekReadyNote(on: Boolean) {
        store.edit { it[weekReadyNoteKey] = on }
    }

    /** "Do this instead" ideas for the Pause screen (saved one per line). Never edited = a starter list. */
    val insteadIdeas: Flow<List<String>> = store.data.map { prefs ->
        prefs[insteadIdeasKey]?.split('\n')?.filter { it.isNotBlank() } ?: STARTER_IDEAS
    }

    suspend fun setInsteadIdeas(ideas: List<String>) {
        store.edit { it[insteadIdeasKey] = ideas.joinToString("\n") }
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

    /**
     * Every setting as JSON, for Backup: {"key": {"t": "s|i|l|b|f|d", "v": value}}.
     * Generic, so settings added later are backed up too.
     */
    suspend fun exportJson(): JSONObject {
        val json = JSONObject()
        store.data.first().asMap().forEach { (key, value) ->
            if (key == popupsConfirmedKey) return@forEach // about this phone, not something to restore on a new one
            val (type, v) = when (value) {
                is String -> "s" to value
                is Int -> "i" to value
                is Long -> "l" to value
                is Boolean -> "b" to value
                is Float -> "f" to value.toDouble()
                is Double -> "d" to value
                else -> return@forEach // (string sets aren't used)
            }
            json.put(key.name, JSONObject().put("t", type).put("v", v))
        }
        return json
    }

    /** Replaces every setting with the ones from [json] (see [exportJson]). */
    suspend fun importJson(json: JSONObject) {
        store.edit { prefs ->
            val popupsConfirmed = prefs[popupsConfirmedKey] // stays with this phone
            prefs.clear()
            popupsConfirmed?.let { prefs[popupsConfirmedKey] = it }
            json.keys().forEach { name ->
                val entry = json.getJSONObject(name)
                when (entry.getString("t")) {
                    "s" -> prefs[stringPreferencesKey(name)] = entry.getString("v")
                    "i" -> prefs[intPreferencesKey(name)] = entry.getInt("v")
                    "l" -> prefs[longPreferencesKey(name)] = entry.getLong("v")
                    "b" -> prefs[booleanPreferencesKey(name)] = entry.getBoolean("v")
                    "f" -> prefs[floatPreferencesKey(name)] = entry.getDouble("v").toFloat()
                    "d" -> prefs[doublePreferencesKey(name)] = entry.getDouble("v")
                }
            }
        }
    }

    suspend fun setUserName(name: String) {
        store.edit { it[nameKey] = name.trim() }
    }

    suspend fun setStrictness(strictness: Strictness) {
        store.edit { it[strictnessKey] = strictness.name }
    }

    companion object {
        /** What the Pause screen suggests until the user writes their own ideas. */
        val STARTER_IDEAS = listOf(
            "Drink a glass of water",
            "Stretch for one minute",
            "Step outside for some air",
            "Read a few pages of a book",
            "Call someone you like"
        )
    }
}
