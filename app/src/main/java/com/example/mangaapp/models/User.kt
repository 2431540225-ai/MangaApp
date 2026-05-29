package com.example.mangaapp.models

data class User(
    val id: Int = 0,
    val username: String = "",
    val email: String = "",
    val avatarUrl: String = "",

    // ── Role ──────────────────────────────────────────────────────────────────
    /** 1 = admin, 2 = reader (mặc định), 3 = author */
    val roleId: Int = 2,

    // ── Coin system ───────────────────────────────────────────────────────────
    /** Số coin hiện có trong ví */
    val coins: Int = 0,
    /** Document ID trong Firestore collection "users" */
    val firestoreId: String = "",
    /** Danh sách key "storyId_chapterNumber" đã được mở khóa */
    val unlockedChapters: List<String> = emptyList()
) {
    /** Kiểm tra xem user đã mở khóa chapter chưa */
    fun hasUnlocked(storyFirestoreId: String, chapterNumber: Int): Boolean {
        return unlockedChapters.contains("${storyFirestoreId}_$chapterNumber")
    }

    val isAdmin: Boolean get() = roleId == 1
    val isAuthor: Boolean get() = roleId == 3 || roleId == 1
    val isReader: Boolean get() = roleId == 2
}