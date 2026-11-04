package com.unscroll.app.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Parent lock: a PIN or pattern that a parent sets, so a child can't loosen ScrollNah.
 * While it's on and locked, Limits and Settings need the PIN, and so do extra minutes,
 * ending focus early and turning the lock off. Everything is stored on this phone only;
 * the PIN itself is never saved, only a salted hash of it.
 *
 * Unlocking lasts while a locked screen is in use, then [UNLOCK_MS] more; the screen
 * turning off or the lock button on Home locks it again straight away.
 */
object ParentLock {

    enum class Kind { PIN, PATTERN }

    /** What the lock looks like right now, for screens to follow. */
    data class State(val on: Boolean, val locked: Boolean, val kind: Kind)

    sealed class Check {
        object Right : Check()
        /** Wrong; [triesLeft] before a wait starts (0 = a wait has just started). */
        data class Wrong(val triesLeft: Int) : Check()
        /** Too many wrong tries: wait [msLeft] before trying again. */
        data class Wait(val msLeft: Long) : Check()
    }

    sealed class Reset {
        object None : Reset()
        data class Waiting(val msLeft: Long) : Reset()
        /** The 24 hours are over: a new PIN can be set without the old one. */
        object Ready : Reset()
    }

    const val UNLOCK_MS = 5 * 60_000L
    const val RESET_WAIT_MS = 24 * 60 * 60_000L
    const val FREE_TRIES = 5
    const val MIN_PIN = 4
    const val MAX_PIN = 8
    const val MIN_PATTERN = 4

    private lateinit var prefs: SharedPreferences
    private val _state = MutableStateFlow(State(on = false, locked = false, kind = Kind.PIN))
    val state: StateFlow<State> = _state

    // In memory only: after ScrollNah restarts, it's always locked.
    @Volatile private var unlockedUntil = 0L

    /** Called once from UnscrollApplication.onCreate. */
    fun init(context: Context) {
        prefs = context.getSharedPreferences("parent_lock", Context.MODE_PRIVATE)
        publish()
    }

    val isOn: Boolean get() = prefs.getBoolean("on", false)
    val kind: Kind get() = Kind.entries.find { it.name == prefs.getString("kind", null) } ?: Kind.PIN
    val isLocked: Boolean get() = isOn && System.currentTimeMillis() >= unlockedUntil

    /** Turns the lock on with a new PIN or pattern (also used to change it). Unlocked afterwards. */
    fun set(kind: Kind, secret: String) {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putBoolean("on", true)
            .putString("kind", kind.name)
            .putString("salt", salt.toHex())
            .putString("hash", hash(secret, salt).toHex())
            .putInt("wrong", 0)
            .remove("wait_until")
            .remove("reset_at")
            .commit()
        unlock()
    }

    fun turnOff() {
        prefs.edit().clear().commit()
        unlockedUntil = 0L
        publish()
    }

    /** Checks a PIN or pattern; a right one unlocks and cancels a pending "Forgot PIN". */
    fun check(secret: String, now: Long = System.currentTimeMillis()): Check {
        val waitUntil = prefs.getLong("wait_until", 0L)
        if (now < waitUntil) return Check.Wait(waitUntil - now)
        val salt = prefs.getString("salt", null)?.fromHex() ?: return Check.Wrong(FREE_TRIES)
        val expected = prefs.getString("hash", null)?.fromHex() ?: return Check.Wrong(FREE_TRIES)
        if (MessageDigest.isEqual(hash(secret, salt), expected)) {
            val hadReset = prefs.contains("reset_at")
            prefs.edit().putInt("wrong", 0).remove("wait_until").remove("reset_at").apply()
            if (hadReset) TamperLog.add(TamperLog.Kind.RESET_CANCELLED)
            unlock()
            return Check.Right
        }
        val wrong = prefs.getInt("wrong", 0) + 1
        val wait = waitAfter(wrong)
        prefs.edit().putInt("wrong", wrong).putLong("wait_until", if (wait > 0) now + wait else 0L).apply()
        if (wrong == FREE_TRIES) TamperLog.add(TamperLog.Kind.WRONG_TRIES, FREE_TRIES.toString())
        return Check.Wrong((FREE_TRIES - wrong).coerceAtLeast(0))
    }

    /** How long until another try is allowed (0 = now). */
    fun waitLeft(now: Long = System.currentTimeMillis()): Long =
        (prefs.getLong("wait_until", 0L) - now).coerceAtLeast(0L)

    fun unlock(now: Long = System.currentTimeMillis()) {
        unlockedUntil = now + UNLOCK_MS
        publish()
    }

    /** A locked screen is in use: keep it unlocked a little longer. */
    fun stillUsing(now: Long = System.currentTimeMillis()) {
        if (isOn && now < unlockedUntil) unlockedUntil = now + UNLOCK_MS
    }

    fun lock() {
        unlockedUntil = 0L
        publish()
    }

    /** Re-checks whether the unlock time has run out (screens call this now and then). */
    fun refresh() = publish()

    // ---- "Forgot PIN": a 24-hour wait, so a child can't quietly reset it ----

    fun requestReset(now: Long = System.currentTimeMillis()) {
        if (prefs.contains("reset_at")) return
        prefs.edit().putLong("reset_at", now + RESET_WAIT_MS).commit()
        TamperLog.add(TamperLog.Kind.RESET_ASKED)
    }

    fun reset(now: Long = System.currentTimeMillis()): Reset = resetAt(prefs.getLong("reset_at", 0L), now)

    /** Pure, for tests: where a reset is, given when it may happen ([resetAt], 0 = never asked). */
    fun resetAt(resetAt: Long, now: Long): Reset = when {
        resetAt <= 0L -> Reset.None
        now < resetAt -> Reset.Waiting(resetAt - now)
        else -> Reset.Ready
    }

    /** After the wait: unlock without the old PIN so a new one can be set. */
    fun useReset(now: Long = System.currentTimeMillis()): Boolean {
        if (reset(now) != Reset.Ready) return false
        prefs.edit().remove("reset_at").putInt("wrong", 0).remove("wait_until").apply()
        TamperLog.add(TamperLog.Kind.RESET_USED)
        unlock(now)
        return true
    }

    // ---- Uninstall protection (device admin) wish, so setup can ask for it ----

    var uninstallProtection: Boolean
        get() = prefs.getBoolean("uninstall", false)
        set(value) {
            prefs.edit().putBoolean("uninstall", value).apply()
        }

    // ---- Pure helpers (tested) ----

    /** How long to wait after [wrong] wrong tries in a row: none for the first few, then longer each time. */
    fun waitAfter(wrong: Int): Long = when {
        wrong < FREE_TRIES -> 0L
        wrong == FREE_TRIES -> 30_000L
        wrong == FREE_TRIES + 1 -> 60_000L
        wrong == FREE_TRIES + 2 -> 5 * 60_000L
        else -> 15 * 60_000L
    }

    /** A slow, salted hash, so the PIN can't be read back from the phone's storage. */
    fun hash(secret: String, salt: ByteArray): ByteArray {
        val spec = PBEKeySpec(secret.toCharArray(), salt, 20_000, 256)
        return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
    }

    private fun publish() {
        _state.value = State(on = isOn, locked = isLocked, kind = kind)
    }

    private fun ByteArray.toHex() = joinToString("") { "%02x".format(it) }
    private fun String.fromHex() = chunked(2).map { it.toInt(16).toByte() }.toByteArray()
}
