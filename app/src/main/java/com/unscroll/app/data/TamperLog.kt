package com.unscroll.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Parent lock's report: things that may mean someone tried to get around ScrollNah
 * (it was stopped, a switch it needs was turned off, the clock was moved, wrong PINs...).
 * Android doesn't let an app prevent these, but the parent can see them later.
 * Only kept while Parent lock is on, only on this phone, at most [MAX] entries.
 */
object TamperLog {

    enum class Kind {
        STOPPED, SWITCH_OFF, SWITCH_ON, CLOCK_CHANGED, WRONG_TRIES,
        RESET_ASKED, RESET_CANCELLED, RESET_USED, UNINSTALL_GUARD_OFF_ASKED, UNINSTALL_GUARD_OFF
    }

    data class Event(val at: Long, val kind: Kind, val detail: String = "")

    const val MAX = 100

    private lateinit var prefs: SharedPreferences
    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events
    private val _seenAt = MutableStateFlow(0L)
    /** When the parent last looked at the report; newer events get a dot on Home's lock button. */
    val seenAt: StateFlow<Long> = _seenAt

    /** Called once from UnscrollApplication.onCreate. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences("tamper_log", Context.MODE_PRIVATE)
        _events.value = read()
        _seenAt.value = prefs.getLong("seen_at", 0L)
    }

    /** Adds an event, newest first. Does nothing while Parent lock is off. */
    fun add(kind: Kind, detail: String = "", at: Long = System.currentTimeMillis()) {
        if (!ParentLock.isOn) return
        val list = (listOf(Event(at, kind, detail)) + read()).sortedByDescending { it.at }.take(MAX)
        save(list)
    }

    fun clear() = save(emptyList())

    fun markSeen(now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong("seen_at", now).apply()
        _seenAt.value = now
    }

    /** The sentence shown in the report, e.g. "Display over other apps was turned off". */
    fun describe(e: Event): String = when (e.kind) {
        Kind.STOPPED -> "ScrollNah was stopped" + if (e.detail.isNotBlank()) " (${e.detail})" else ""
        Kind.SWITCH_OFF -> "${e.detail} was turned off"
        Kind.SWITCH_ON -> "${e.detail} was turned back on"
        Kind.CLOCK_CHANGED -> "The phone's clock was moved ${e.detail}"
        Kind.WRONG_TRIES -> "Wrong PIN or pattern ${e.detail} times in a row"
        Kind.RESET_ASKED -> "Someone tapped \"Forgot PIN\" (a new one can be set 24 hours later)"
        Kind.RESET_CANCELLED -> "The \"Forgot PIN\" wait was cancelled with the right PIN"
        Kind.RESET_USED -> "A new PIN was set after the 24-hour wait"
        Kind.UNINSTALL_GUARD_OFF_ASKED -> "Someone started turning off uninstall protection"
        Kind.UNINSTALL_GUARD_OFF -> "Uninstall protection was turned off"
    }

    /** "Today 4:10 PM", "Yesterday 9:05 AM" or "3 Nov, 9:05 AM". */
    fun whenText(at: Long, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()): String {
        val time = Instant.ofEpochMilli(at).atZone(zone)
        val days = java.time.temporal.ChronoUnit.DAYS.between(time.toLocalDate(), Instant.ofEpochMilli(now).atZone(zone).toLocalDate())
        val clock = time.format(DateTimeFormatter.ofPattern("h:mm a"))
        return when (days) {
            0L -> "Today $clock"
            1L -> "Yesterday $clock"
            else -> time.format(DateTimeFormatter.ofPattern("d MMM, h:mm a"))
        }
    }

    /** "forward by 3 h 20 min" / "back by 45 min" for a clock jump of [ms]. */
    fun jumpText(ms: Long): String {
        val minutes = kotlin.math.abs(ms) / 60_000L
        val text = if (minutes >= 60) "${minutes / 60} h" + (if (minutes % 60 > 0) " ${minutes % 60} min" else "") else "$minutes min"
        return (if (ms > 0) "forward by " else "back by ") + text
    }

    private fun read(): List<Event> = try {
        val arr = JSONArray(prefs.getString("events", "[]"))
        (0 until arr.length()).mapNotNull { i ->
            val o = arr.getJSONObject(i)
            val kind = Kind.entries.find { it.name == o.optString("k") } ?: return@mapNotNull null
            Event(o.getLong("t"), kind, o.optString("d"))
        }
    } catch (e: Exception) {
        emptyList()
    }

    private fun save(list: List<Event>) {
        val arr = JSONArray()
        list.forEach { arr.put(JSONObject().put("t", it.at).put("k", it.kind.name).put("d", it.detail)) }
        prefs.edit().putString("events", arr.toString()).commit()
        _events.value = list
    }
}
