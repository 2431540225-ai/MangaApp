package com.example.mangaapp.repository

import com.example.mangaapp.model.ReadingHistory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

class ReadingHistoryRepository {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    // Collection: users/{userId}/readingHistory/{storyId}
    private fun getHistoryCollection(userId: String) =
        db.collection("users").document(userId).collection("readingHistory")

    /**
     * Lưu hoặc cập nhật lịch sử đọc khi user đọc 1 chương
     * Gọi hàm này từ màn hình đọc chương
     */
    fun saveOrUpdateHistory(
        storyId: String,
        storyTitle: String,
        storyCoverUrl: String,
        authorName: String,
        chapterId: String,
        chapterTitle: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val userId = auth.currentUser?.uid ?: return

        val history = ReadingHistory(
            historyId = storyId,         // dùng storyId làm document ID => mỗi truyện chỉ 1 bản ghi
            userId = userId,
            storyId = storyId,
            storyTitle = storyTitle,
            storyCoverUrl = storyCoverUrl,
            authorName = authorName,
            lastChapterId = chapterId,
            lastChapterTitle = chapterTitle,
            lastReadTime = System.currentTimeMillis()
        )

        getHistoryCollection(userId)
            .document(storyId)           // document ID = storyId, tự động ghi đè nếu đã tồn tại
            .set(history)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Lấy danh sách lịch sử đọc, sắp xếp theo thời gian đọc gần nhất
     */
    fun getReadingHistory(
        onSuccess: (List<ReadingHistory>) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val userId = auth.currentUser?.uid ?: run {
            onSuccess(emptyList())
            return
        }

        getHistoryCollection(userId)
            .orderBy("lastReadTime", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { snapshot ->
                val list = snapshot.documents.mapNotNull { doc ->
                    doc.toObject(ReadingHistory::class.java)
                }
                onSuccess(list)
            }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Xoá 1 mục lịch sử đọc
     */
    fun deleteHistory(
        storyId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val userId = auth.currentUser?.uid ?: return

        getHistoryCollection(userId)
            .document(storyId)
            .delete()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

    /**
     * Xoá toàn bộ lịch sử đọc
     */
    fun clearAllHistory(
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        val userId = auth.currentUser?.uid ?: return

        getHistoryCollection(userId)
            .get()
            .addOnSuccessListener { snapshot ->
                val batch = db.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch
                    .commit()
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener { onFailure(it) }
            }
            .addOnFailureListener { onFailure(it) }
    }
}