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
            historyId = storyId,
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
            .document(storyId)
            .set(history)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onFailure(it) }
    }

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

    fun getHistoryItem(
        storyId: String,
        onSuccess: (ReadingHistory?) -> Unit,
        onFailure: (Exception) -> Unit = {}
    ) {
        val userId = auth.currentUser?.uid ?: run {
            onSuccess(null)
            return
        }

        getHistoryCollection(userId)
            .document(storyId)
            .get()
            .addOnSuccessListener { doc ->
                onSuccess(doc.toObject(ReadingHistory::class.java))
            }
            .addOnFailureListener { onFailure(it) }
    }

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