package com.example.mangaapp.utils

import android.content.Context
import com.example.mangaapp.models.Comment
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object CommentStorage {
    private const val PREF_NAME = "comment_prefs"
    private const val KEY_COMMENTS = "comments"
    private val gson = Gson()

    fun saveComments(context: Context, comments: List<Comment>) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_COMMENTS, gson.toJson(comments)).apply()
    }

    fun loadComments(context: Context): MutableList<Comment> {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val json = prefs.getString(KEY_COMMENTS, null) ?: return mutableListOf()
        val type = object : TypeToken<MutableList<Comment>>() {}.type
        return gson.fromJson(json, type)
    }
}