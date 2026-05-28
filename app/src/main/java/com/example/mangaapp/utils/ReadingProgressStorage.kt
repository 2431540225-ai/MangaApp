package com.example.mangaapp.utils

import android.content.Context

object ReadingProgressStorage {
    private const val PREF_NAME = "reading_progress"

    // Đổi mangaId: Int → firestoreId: String cho chính xác
    fun saveProgress(context: Context, firestoreId: String, chapterNumber: Int) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt("progress_$firestoreId", chapterNumber).apply()
    }

    fun getProgress(context: Context, firestoreId: String): Int {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getInt("progress_$firestoreId", -1)
    }

    fun clearProgress(context: Context, firestoreId: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove("progress_$firestoreId").apply()
    }
}