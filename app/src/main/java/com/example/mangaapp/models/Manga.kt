package com.example.mangaapp.models

import java.io.Serializable

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
    val createdAt: String = "",
    val category: MangaCategory = MangaCategory.TRUYEN_TRANH,
    val firestoreId: String = ""
) : Serializable

enum class MangaStatus : Serializable {
    ONGOING,
    COMPLETED
}

enum class MangaCategory : Serializable {
    TRUYEN_TRANH,
    TIEU_THUYET
}
