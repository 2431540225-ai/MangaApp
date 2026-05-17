package com.example.mangaapp.models

data class Manga(
    val id: Int,
    val name: String,
    val slug: String,
    val author: String,
    val description: String,
    val coverUrl: String,
    val genres: List<String>,
    val totalChapters: Int,
    val totalViews: Int,
    val status: MangaStatus = MangaStatus.ONGOING,
    val isPaid: Boolean = false,
    val createdAt: String = ""
)

enum class MangaStatus {
    ONGOING,    // Đang ra
    COMPLETED   // Hoàn thành
}
