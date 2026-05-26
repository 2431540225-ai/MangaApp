package com.example.mangaapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    private const val PREF_NAME = "theme_prefs"
    private const val KEY_IS_DARK = "is_dark_mode"

    private lateinit var prefs: SharedPreferences

    /** Gọi trong MainActivity.onCreate() TRƯỚC super.onCreate() */
    fun init(context: Context) {
        prefs = context.applicationContext
            .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /** Áp dụng theme đã lưu (gọi sau init, trước setContentView) */
    fun applyTheme() {
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode()) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )
    }

    /** Đổi theme ngược lại rồi lưu */
    fun toggle() {
        prefs.edit().putBoolean(KEY_IS_DARK, !isDarkMode()).apply()
        applyTheme()
    }

    /** true = đang ở Dark mode, false = Light mode */
    fun isDarkMode(): Boolean =
        prefs.getBoolean(KEY_IS_DARK, true) // mặc định dark
}