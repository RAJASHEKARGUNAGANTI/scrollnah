package com.unscroll.app.data

import android.content.Context
import android.hardware.biometrics.BiometricManager
import android.hardware.biometrics.BiometricManager.Authenticators.BIOMETRIC_STRONG
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyPermanentlyInvalidatedException
import android.security.keystore.KeyProperties
import androidx.annotation.RequiresApi
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey

/**
 * Parent lock's optional fingerprint unlock (Android 11+, the phone's own fingerprint prompt, no library).
 *
 * Android never tells an app WHOSE finger matched, only that a fingerprint saved on the phone did.
 * So the parent turns this on only if all saved fingerprints are theirs, and a key in Android's
 * secure key storage guards it: the key only works after a strong fingerprint check, and Android
 * destroys it when a fingerprint is added or removed. Then fingerprint unlock turns off by itself,
 * the PIN is needed again and the report says why. ScrollNah never sees or stores a fingerprint.
 */
object ParentFingerprint {

    private const val KEY_NAME = "parent_lock_fingerprint"

    enum class Ready { YES, NO_FINGERPRINT, NOT_SUPPORTED }

    /** Can this phone do it: Android 11+, a strong sensor, and at least one fingerprint saved? */
    fun ready(context: Context): Ready {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return Ready.NOT_SUPPORTED
        val manager = context.getSystemService(BiometricManager::class.java) ?: return Ready.NOT_SUPPORTED
        return when (manager.canAuthenticate(BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> Ready.YES
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> Ready.NO_FINGERPRINT
            else -> Ready.NOT_SUPPORTED
        }
    }

    /** A fresh key for the fingerprints saved right now (turning fingerprint unlock on). */
    @RequiresApi(Build.VERSION_CODES.R)
    fun createKey() {
        forget()
        val spec = KeyGenParameterSpec.Builder(KEY_NAME, KeyProperties.PURPOSE_ENCRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setUserAuthenticationRequired(true)
            .setUserAuthenticationParameters(0, KeyProperties.AUTH_BIOMETRIC_STRONG) // a fingerprint every time
            .setInvalidatedByBiometricEnrollment(true) // a fingerprint added or removed destroys it
            .build()
        KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply { init(spec) }.generateKey()
    }

    /** The key ready for a fingerprint check ([cipher]), or [gone] = destroyed (fingerprints changed). */
    private class Opened(val cipher: Cipher?, val gone: Boolean)

    private fun openKey(): Opened = try {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val key = store.getKey(KEY_NAME, null) as? SecretKey
        if (key == null) Opened(null, gone = true)
        else Opened(Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, key) }, gone = false)
    } catch (e: KeyPermanentlyInvalidatedException) {
        Opened(null, gone = true)
    } catch (e: Exception) {
        Opened(null, gone = false) // a hiccup, not a change: don't turn anything off
    }

    /** A fingerprint was added or removed: fingerprint unlock turns off (once) and the report says why. */
    fun stopBecauseChanged() {
        if (ParentLock.fingerprintOn) {
            ParentLock.setFingerprint(false)
            TamperLog.add(TamperLog.Kind.FINGERPRINT_CHANGED)
        }
        forget()
    }

    /** Called now and then (TamperWatch), so a new fingerprint is noted when it happens. No prompt. */
    fun checkStillValid() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && ParentLock.fingerprintOn && openKey().gone) stopBecauseChanged()
    }

    fun forget() {
        try {
            KeyStore.getInstance("AndroidKeyStore").apply { load(null) }.deleteEntry(KEY_NAME)
        } catch (e: Exception) {
            // Nothing to delete.
        }
    }

    /** What happened when the fingerprint prompt closed. */
    sealed class Result {
        object Right : Result()
        object Cancelled : Result()          // "Use PIN", Back, or the prompt was closed
        object KeyGone : Result()            // a fingerprint was added or removed since it was turned on
        object TooManyTries : Result()       // Android's own lockout after wrong fingers
        data class Problem(val message: String) : Result()
    }

    /**
     * Shows Android's fingerprint prompt, using the key, so only a fingerprint saved when it was
     * turned on can pass. [onResult] runs on the main thread. Returns a way to close the prompt.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun ask(context: Context, title: String, subtitle: String, cancelText: String, onResult: (Result) -> Unit): CancellationSignal? {
        val opened = openKey()
        val cipher = opened.cipher
        if (cipher == null) {
            onResult(if (opened.gone) Result.KeyGone else Result.Problem(NOT_NOW))
            return null
        }
        val executor = context.mainExecutor
        val cancel = CancellationSignal()
        try {
            val prompt = BiometricPrompt.Builder(context)
                .setTitle(title)
                .setSubtitle(subtitle)
                .setAllowedAuthenticators(BIOMETRIC_STRONG)
                .setConfirmationRequired(false)
                .setNegativeButton(cancelText, executor) { _, _ -> onResult(Result.Cancelled) }
                .build()
            prompt.authenticate(BiometricPrompt.CryptoObject(cipher), cancel, executor, callback(onResult))
        } catch (e: Exception) {
            onResult(Result.Problem(NOT_NOW))
            return null
        }
        return cancel
    }

    private const val NOT_NOW = "Fingerprint isn't working right now. Use the PIN or pattern."

    @RequiresApi(Build.VERSION_CODES.R)
    private fun callback(onResult: (Result) -> Unit) = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            // Use the key once: it only works after a real fingerprint match.
            val ok = try {
                result.cryptoObject?.cipher?.doFinal(ByteArray(1)) != null
            } catch (e: Exception) {
                false
            }
            onResult(if (ok) Result.Right else Result.Problem("That didn't work. Use the PIN or pattern."))
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            onResult(
                when (errorCode) {
                    BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT, BiometricPrompt.BIOMETRIC_ERROR_LOCKOUT_PERMANENT -> Result.TooManyTries
                    // ("Use the PIN" has its own listener, set on the prompt.)
                    BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED, BiometricPrompt.BIOMETRIC_ERROR_CANCELED -> Result.Cancelled
                    else -> Result.Problem(errString.toString())
                }
            )
        }
        // A finger that doesn't match: Android's prompt says so itself and lets them try again.
    }
}
