package com.example.mangaapp.models

data class Chapter(
    val id: Int = 0,
    val mangaId: Int = 0,
    val chapterNumber: Int = 0,
    val title: String = "",
    val imageUrls: List<String> = emptyList(),
    val content: String = "",
    val publishedAt: String = "",
    val pages: List<String> = emptyList(),

    // ── Coin / Lock system ────────────────────────────────────────────────────
    /** true = chương miễn phí, false = cần coin để mở */
    val isFree: Boolean = true,
    /** Số coin cần để mở chương này (chỉ có ý nghĩa khi isFree = false) */
    val coinPrice: Int = 0,
    /** firestoreId của tác giả đăng chương này */
    val authorId: String = "",
    /** Document ID trong Firestore (dùng khi cần update) */
    val firestoreDocId: String = ""
)