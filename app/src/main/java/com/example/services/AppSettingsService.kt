package com.example.services

import android.content.Context
import android.content.SharedPreferences

class AppSettingsService(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("wallet_app_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_THEME = "ui_theme" // SYSTEM, LIGHT, DARK
        private const val KEY_CURRENCY = "app_currency" // USD, EUR, GBP, JPY, CAD, AUD, INR
        private const val KEY_PIN_CODE = "app_pin_code" // String (4 digits)
        private const val KEY_BIOMETRIC_ENABLED = "biometric_enabled" // Boolean
        private const val KEY_USER_NAME = "user_name" // String
        private const val KEY_USER_AVATAR = "user_avatar_path" // String (file path/URI or predefined index)
    }

    var theme: String
        get() = prefs.getString(KEY_THEME, "DARK") ?: "DARK" // Default dark theme for fintech premium aesthetic!
        set(value) = prefs.edit().putString(KEY_THEME, value).apply()

    var currency: String
        get() = prefs.getString(KEY_CURRENCY, "USD") ?: "USD"
        set(value) = prefs.edit().putString(KEY_CURRENCY, value).apply()

    var userName: String
        get() = prefs.getString(KEY_USER_NAME, "John Doe") ?: "John Doe"
        set(value) = prefs.edit().putString(KEY_USER_NAME, value).apply()

    var userAvatarPath: String?
        get() = prefs.getString(KEY_USER_AVATAR, null)
        set(value) {
            if (value == null) {
                prefs.edit().remove(KEY_USER_AVATAR).apply()
            } else {
                prefs.edit().putString(KEY_USER_AVATAR, value).apply()
            }
        }

    var pinCode: String?
        get() = prefs.getString(KEY_PIN_CODE, null)
        set(value) {
            if (value == null) {
                prefs.edit().remove(KEY_PIN_CODE).apply()
            } else {
                prefs.edit().putString(KEY_PIN_CODE, value).apply()
            }
        }

    var isBiometricEnabled: Boolean
        get() = prefs.getBoolean(KEY_BIOMETRIC_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BIOMETRIC_ENABLED, value).apply()

    fun isAppLockEnabled(): Boolean {
        return pinCode != null
    }

    val currencySymbol: String
        get() = when (currency) {
            "USD" -> "$"
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            "CAD" -> "C$"
            "AUD" -> "A$"
            "INR" -> "₹"
            "BDT" -> "৳"
            else -> {
                if (currency.length <= 3) currency else "$"
            }
        }
}
