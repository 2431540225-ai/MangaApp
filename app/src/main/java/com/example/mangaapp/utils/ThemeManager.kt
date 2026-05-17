package com.example.mangaapp.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate

object ThemeManager {

    private const val PREF_NAME = "manga_app_prefs"
    private const val KEY_DARK_MODE = "is_dark_mode"

    private lateinit var prefs: SharedPreferences

    /** Gọi hàm này trong Application.onCreate() hoặc MainActivity.onCreate() */
    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /** Kiểm tra hiện tại có đang ở dark mode không */
    fun isDarkMode(): Boolean = prefs.getBoolean(KEY_DARK_MODE, false)

    /** Bật dark mode */
    fun enableDarkMode() {
        prefs.edit().putBoolean(KEY_DARK_MODE, true).apply()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }

    /** Bật light mode */
    fun enableLightMode() {
        prefs.edit().putBoolean(KEY_DARK_MODE, false).apply()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
    }

    /** Toggle: nếu đang dark → chuyển light, ngược lại */
    fun toggle() {
        if (isDarkMode()) enableLightMode() else enableDarkMode()
    }

    /** Áp dụng theme đã lưu — gọi trong MainActivity.onCreate() trước setContentView() */
    fun applyTheme() {
        if (isDarkMode()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }
    }
}
