package com.example.mydigidaw.security

import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_STRONG
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricManager.Authenticators.DEVICE_CREDENTIAL
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

sealed interface AuthResult {
    data class Success(val cryptoObject: BiometricPrompt.CryptoObject?) : AuthResult
    data class Failed(val code: Int, val message: String) : AuthResult
}

object BiometricAuth {
    /** Biometrik atau PIN/pola/sandi layar perangkat. */
    val deviceAuthenticators: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) BIOMETRIC_STRONG or DEVICE_CREDENTIAL
        else BIOMETRIC_WEAK or DEVICE_CREDENTIAL

    fun canUseDeviceAuth(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(deviceAuthenticators) == BiometricManager.BIOMETRIC_SUCCESS

    fun canUseStrongBiometric(context: Context): Boolean =
        BiometricManager.from(context).canAuthenticate(BIOMETRIC_STRONG) == BiometricManager.BIOMETRIC_SUCCESS

    suspend fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String? = null,
        authenticators: Int = deviceAuthenticators,
        negativeText: String = "Batal",
        crypto: BiometricPrompt.CryptoObject? = null,
    ): AuthResult {
        if (!activity.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
            return AuthResult.Failed(-1, "Aplikasi tidak aktif")
        }
        return suspendCancellableCoroutine { cont ->
            val prompt = BiometricPrompt(
                activity,
                ContextCompat.getMainExecutor(activity),
                object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        if (cont.isActive) cont.resume(AuthResult.Success(result.cryptoObject))
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        if (cont.isActive) cont.resume(AuthResult.Failed(errorCode, errString.toString()))
                    }
                },
            )
            val info = BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .apply {
                    if (subtitle != null) setSubtitle(subtitle)
                    setAllowedAuthenticators(authenticators)
                    if (authenticators and DEVICE_CREDENTIAL == 0) setNegativeButtonText(negativeText)
                }
                .setConfirmationRequired(false)
                .build()
            if (crypto != null) prompt.authenticate(info, crypto) else prompt.authenticate(info)
            cont.invokeOnCancellation { prompt.cancelAuthentication() }
        }
    }
}
