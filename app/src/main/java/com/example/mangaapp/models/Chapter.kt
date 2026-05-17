package com.example.mangaapp.models

data class Chapter(
    val id: Int,
    val mangaId: Int,
    val chapterNumber: Int,
    val title: String,
    val imageUrls: List<String> = emptyList(),
    val content: String = "",
    val publishedAt: String = ""
)
