package com.shardmail.app.data.local

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SecurePreferences(context: Context) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs: SharedPreferences = try {
        EncryptedSharedPreferences.create(
            context,
            "shardmail_secure_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (e: Exception) {
        // Fallback if Keystore state was invalidated
        context.getSharedPreferences("shardmail_secure_prefs_fallback", Context.MODE_PRIVATE)
    }

    var customDomain: String
        get() = prefs.getString(KEY_CUSTOM_DOMAIN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CUSTOM_DOMAIN, value.trim().lowercase()).apply()

    var workerUrl: String
        get() = prefs.getString(KEY_WORKER_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_WORKER_URL, value.trim().removeSuffix("/")).apply()

    var adminPassword: String
        get() = prefs.getString(KEY_ADMIN_PASSWORD, "") ?: ""
        set(value) = prefs.edit().putString(KEY_ADMIN_PASSWORD, value.trim()).apply()

    var resendApiKey: String
        get() = prefs.getString(KEY_RESEND_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_RESEND_API_KEY, value.trim()).apply()

    var cloudflareApiToken: String
        get() = prefs.getString(KEY_CLOUDFLARE_API_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_CLOUDFLARE_API_TOKEN, value.trim()).apply()

    var onboardingDone: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_DONE, value).apply()

    fun isWorkerUrlValid(): Boolean {
        val url = workerUrl
        if (url.isBlank() || url.contains("<") || url.contains(">")) {
            return false
        }
        return url.startsWith("http://") || url.startsWith("https://")
    }

    fun isConfigured(): Boolean {
        return isWorkerUrlValid() && adminPassword.isNotBlank() && customDomain.isNotBlank()
    }

    companion object {
        private const val KEY_CUSTOM_DOMAIN = "custom_domain"
        private const val KEY_WORKER_URL = "worker_url"
        private const val KEY_ADMIN_PASSWORD = "admin_password"
        private const val KEY_RESEND_API_KEY = "resend_api_key"
        private const val KEY_CLOUDFLARE_API_TOKEN = "cloudflare_api_token"
        private const val KEY_ONBOARDING_DONE = "onboarding_done"
    }
}
