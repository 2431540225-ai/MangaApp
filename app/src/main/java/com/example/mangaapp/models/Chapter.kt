package com.example.mangaapp.models

data class Chapter(
    val id: Int = 0,
    val mangaId: Int = 0,
    val chapterNumber: Int = 0,
    val title: String = "",
    val imageUrls: List<String> = emptyList(),
    val content: String = "",
    val publishedAt: String = "",
    // Field mới — lấy từ Firestore
    val pages: List<String> = emptyList()
)