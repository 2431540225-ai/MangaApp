package com.example.mangaapp.model

data class ReadingHistory(
    val historyId: String = "",
    val userId: String = "",
    val storyId: String = "",
    val storyTitle: String = "",
    val storyCoverUrl: String = "",
    val authorName: String = "",
    val lastChapterId: String = "",
    val lastChapterTitle: String = "",
    val lastReadTime: Long = 0L
) {
    // Constructor không tham số cho Firestore
    constructor() : this("", "", "", "", "", "", "", "", 0L)
}