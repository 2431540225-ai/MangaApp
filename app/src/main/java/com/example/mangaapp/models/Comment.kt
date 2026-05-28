package com.example.mangaapp.models

data class Comment(
    val id: String = "",
    val firestoreId: String = "",
    val chapterId: Int? = null,
    val userId: String = "",
    val userName: String = "",
    val avatarUrl: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val likes: Int = 0
)