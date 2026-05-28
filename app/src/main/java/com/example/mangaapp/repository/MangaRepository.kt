package com.example.mangaapp.repository

import android.content.Context
import com.example.mangaapp.models.Chapter
import com.example.mangaapp.models.Comment
import com.example.mangaapp.models.Manga
import com.example.mangaapp.models.MangaCategory
import com.example.mangaapp.models.MangaStatus
import com.example.mangaapp.utils.CommentStorage
import com.google.firebase.firestore.FirebaseFirestore

object MangaRepository {

    val genres = listOf("Tất cả", "Hành Động", "Tình Cảm", "Hài Hước", "Phiêu Lưu", "Kinh Dị")

    private val db = FirebaseFirestore.getInstance()

    // ─── MANGA ───────────────────────────────────────────────────────────────

    fun getAllManga(onSuccess: (List<Manga>) -> Unit, onError: (Exception) -> Unit) {
        db.collection("stories")
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapIndexedNotNull { index, doc ->
                    docToManga(doc.id, doc.data ?: return@mapIndexedNotNull null, index)
                }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it) }
    }

    fun getMangaByCategory(
        category: MangaCategory,
        onSuccess: (List<Manga>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val categoryStr = if (category == MangaCategory.TRUYEN_TRANH) "truyen_tranh" else "tieu_thuyet"
        db.collection("stories")
            .whereEqualTo("category", categoryStr)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapIndexedNotNull { index, doc ->
                    docToManga(doc.id, doc.data ?: return@mapIndexedNotNull null, index)
                }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it) }
    }

    fun getFeaturedManga(onSuccess: (List<Manga>) -> Unit, onError: (Exception) -> Unit) {
        db.collection("stories")
            .orderBy("totalViews", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(4)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapIndexedNotNull { index, doc ->
                    docToManga(doc.id, doc.data ?: return@mapIndexedNotNull null, index)
                }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it) }
    }

    fun getLatestManga(onSuccess: (List<Manga>) -> Unit, onError: (Exception) -> Unit) {
        db.collection("stories")
            .orderBy("createdAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(6)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapIndexedNotNull { index, doc ->
                    docToManga(doc.id, doc.data ?: return@mapIndexedNotNull null, index)
                }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it) }
    }

    fun getRankingManga(onSuccess: (List<Manga>) -> Unit, onError: (Exception) -> Unit) {
        db.collection("stories")
            .orderBy("totalViews", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapIndexedNotNull { index, doc ->
                    docToManga(doc.id, doc.data ?: return@mapIndexedNotNull null, index)
                }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it) }
    }

    fun getMangaById(
        firestoreId: String,
        onSuccess: (Manga?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("stories")
            .document(firestoreId)
            .get()
            .addOnSuccessListener { doc ->
                val manga = doc.data?.let { docToManga(doc.id, it, 0) }
                onSuccess(manga)
            }
            .addOnFailureListener { onError(it) }
    }

    fun searchManga(
        query: String,
        onSuccess: (List<Manga>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("stories")
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.mapIndexedNotNull { index, doc ->
                    docToManga(doc.id, doc.data ?: return@mapIndexedNotNull null, index)
                }.filter {
                    it.name.contains(query, ignoreCase = true) ||
                            it.author.contains(query, ignoreCase = true)
                }
                onSuccess(list)
            }
            .addOnFailureListener { onError(it) }
    }

    // ─── CHAPTERS ────────────────────────────────────────────────────────────

    fun getChaptersByMangaId(
        firestoreId: String,
        onSuccess: (List<Chapter>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("stories")
            .document(firestoreId)
            .collection("chapters")
            .orderBy("chapterNumber")
            .get()
            .addOnSuccessListener { result ->
                val chapters = result.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    Chapter(
                        id            = (data["chapterNumber"] as? Long)?.toInt() ?: 0,
                        mangaId       = 0,
                        chapterNumber = (data["chapterNumber"] as? Long)?.toInt() ?: 0,
                        title         = data["title"] as? String ?: "",
                        publishedAt   = data["createdAt"] as? String ?: ""
                    )
                }
                onSuccess(chapters)
            }
            .addOnFailureListener { onError(it) }
    }

    fun getChapter(
        firestoreId: String,
        chapterNumber: Int,
        onSuccess: (Chapter?) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("stories")
            .document(firestoreId)
            .collection("chapters")
            .whereEqualTo("chapterNumber", chapterNumber)
            .limit(1)
            .get()
            .addOnSuccessListener { result ->
                val doc = result.documents.firstOrNull()
                val data = doc?.data
                val chapter = data?.let {
                    Chapter(
                        id            = chapterNumber,
                        mangaId       = 0,
                        chapterNumber = chapterNumber,
                        title         = it["title"] as? String ?: "",
                        publishedAt   = it["createdAt"] as? String ?: ""
                    )
                }
                onSuccess(chapter)
            }
            .addOnFailureListener { onError(it) }
    }

    // ─── PAGES ───────────────────────────────────────────────────────────────

    fun getChapterPagesFromFirestore(
        storyId: String,
        chapterId: String,
        onSuccess: (List<String>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("stories")
            .document(storyId)
            .collection("chapters")
            .document(chapterId)
            .collection("pages")
            .orderBy("pageNumber")
            .get()
            .addOnSuccessListener { result ->
                val pages = result.documents.mapNotNull { it.getString("url") }
                onSuccess(pages)
            }
            .addOnFailureListener { onError(it) }
    }

    // ─── COMMENTS ────────────────────────────────────────────────────────────

    private var comments: MutableList<Comment> = mutableListOf()
    private var isCommentsLoaded = false

    private fun defaultComments() = mutableListOf(
        Comment(
            id          = "1",
            firestoreId = "sample",
            userId      = "user1",
            userName    = "Naruto_Fan",
            content     = "Truyện hay quá!"
        ),
        Comment(
            id          = "2",
            firestoreId = "sample",
            userId      = "user2",
            userName    = "MangaLover",
            content     = "Tác giả vẽ đẹp vãi!"
        )
    )

    fun initComments(context: Context) {
        if (isCommentsLoaded) return
        val saved = CommentStorage.loadComments(context)
        comments = if (saved.isEmpty()) defaultComments() else saved
        isCommentsLoaded = true
    }

    fun getCommentsByFirestoreId(firestoreId: String): List<Comment> {
        return comments.filter { it.firestoreId == firestoreId && it.chapterId == null }
    }

    fun getCommentsByChapter(firestoreId: String, chapterId: Int): List<Comment> {
        return comments.filter { it.firestoreId == firestoreId && it.chapterId == chapterId }
    }

    fun addComment(context: Context, comment: Comment) {
        comments.add(comment)
        CommentStorage.saveComments(context, comments)
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun docToManga(id: String, data: Map<String, Any>, index: Int): Manga {
        val statusStr = data["status"] as? String ?: "completed"
        val categoryStr = data["category"] as? String ?: "truyen_tranh"
        return Manga(
            id            = index + 1,
            name          = data["title"] as? String ?: "",
            slug          = id,
            author        = data["author"] as? String ?: "",
            description   = data["description"] as? String ?: "",
            coverUrl      = data["coverUrl"] as? String ?: "",
            genres        = (data["genres"] as? List<String>) ?: emptyList(),
            totalChapters = (data["totalChapters"] as? Long)?.toInt() ?: 0,
            totalViews    = (data["totalViews"] as? Long)?.toInt() ?: 0,
            status        = if (statusStr == "ongoing") MangaStatus.ONGOING else MangaStatus.COMPLETED,
            category      = if (categoryStr == "tieu_thuyet") MangaCategory.TIEU_THUYET else MangaCategory.TRUYEN_TRANH,
            createdAt     = data["createdAt"] as? String ?: "",
            firestoreId   = id
        )
    }
}