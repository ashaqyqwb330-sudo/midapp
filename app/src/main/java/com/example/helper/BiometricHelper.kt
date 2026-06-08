package com.example.helper

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricHelper {
    var onAuthSuccess: (() -> Unit)? = null
    var onAuthError: ((String) -> Unit)? = null

    /**
     * Checks if the device has biometric hardware and enrolled biometrics.
     */
    fun isBiometricAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val status = biometricManager.canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or 
            BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Checks if only strong hardware biometrics (Fingerprint/Face) are available and enrolled.
     */
    fun isStrongBiometricOnlyAvailable(context: Context): Boolean {
        val biometricManager = BiometricManager.from(context)
        val status = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        return status == BiometricManager.BIOMETRIC_SUCCESS
    }

    /**
     * Triggers the biometric prompt using the FragmentActivity executor.
     */
    fun triggerBiometricPrompt(
        activity: FragmentActivity,
        title: String = "التحقق الأمني الحيوي 🛡️",
        subtitle: String = "استخدم البصمة أو ملامح الوجه للوصول لقاعدة البيانات"
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onAuthError?.invoke(errString.toString())
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onAuthSuccess?.invoke()
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onAuthError?.invoke("فشل التحقق الحيوي. يرجى المحاولة مجدداً")
            }
        })

        val builder = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)

        // Android SDK requirement: If we allow status/device credential (PIN/Pattern),
        // we must NOT define negative button text. Otherwise we MUST define it.
        val biometricManager = BiometricManager.from(activity)
        val canCredential = biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
        
        if (canCredential) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or 
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            builder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
            builder.setNegativeButtonText("إلغاء التصفح")
        }

        try {
            biometricPrompt.authenticate(builder.build())
        } catch (e: Exception) {
            onAuthError?.invoke(e.localizedMessage ?: "خطأ في الاتصال بنظام الأمن")
        }
    }
}
