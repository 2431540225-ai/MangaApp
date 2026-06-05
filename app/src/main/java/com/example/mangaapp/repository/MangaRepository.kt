package com.example.mangaapp.repository

import android.content.Context
import com.example.mangaapp.models.Chapter
import com.example.mangaapp.models.Comment
import com.example.mangaapp.models.Manga
import com.example.mangaapp.models.MangaCategory
import com.example.mangaapp.models.MangaStatus
import com.example.mangaapp.utils.CommentStorage
import com.example.mangaapp.utils.UserSession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

object MangaRepository {

    val genres = listOf("Tất cả", "Hành Động", "Tình Cảm", "Hài Hước", "Phiêu Lưu", "Kinh Dị")

    private val db = FirebaseFirestore.getInstance()

    // MANGA

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
                        title         = data["title"]     as? String ?: "",
                        publishedAt   = data["createdAt"] as? String ?: "",
                        isFree        = data["isFree"]    as? Boolean ?: true,
                        coinPrice     = (data["coinPrice"] as? Long)?.toInt() ?: 0,
                        authorId      = data["authorId"]  as? String ?: "",
                        firestoreDocId = doc.id
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
                val doc  = result.documents.firstOrNull()
                val data = doc?.data
                val chapter = data?.let {
                    Chapter(
                        id            = chapterNumber,
                        mangaId       = 0,
                        chapterNumber = chapterNumber,
                        title         = it["title"]     as? String  ?: "",
                        publishedAt   = it["createdAt"] as? String  ?: "",
                        isFree        = it["isFree"]    as? Boolean ?: true,
                        coinPrice     = (it["coinPrice"] as? Long)?.toInt() ?: 0,
                        authorId      = it["authorId"]  as? String  ?: "",
                        firestoreDocId = doc.id
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

    // ─── UPLOAD MANGA (dành cho tác giả) ─────────────────────────────────────

    /**
     * Tác giả đăng truyện mới lên Firestore.
     * @return firestoreId của truyện vừa tạo qua onSuccess callback
     */
    fun uploadManga(
        title: String,
        author: String,
        description: String,
        coverUrl: String,
        genres: List<String>,
        category: MangaCategory,
        onSuccess: (firestoreId: String) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val authorId = UserSession.firebaseUid ?: run {
            onError(Exception("Bạn cần đăng nhập để đăng truyện"))
            return
        }

        val slug = generateSlug(title)

        val data = hashMapOf(
            "title"         to title,
            "author"        to author,
            "description"   to description,
            "coverUrl"      to coverUrl,
            "genres"        to genres,
            "category"      to if (category == MangaCategory.TRUYEN_TRANH) "truyen_tranh" else "tieu_thuyet",
            "status"        to "ongoing",
            "totalChapters" to 0,
            "totalViews"    to 0,
            "authorId"      to authorId,
            "createdAt"     to System.currentTimeMillis().toString()
        )

        val docRef = db.collection("stories").document(slug)
        docRef.get()
            .addOnSuccessListener { snapshot ->
                val finalSlug = if (snapshot.exists()) "$slug-${System.currentTimeMillis() % 10000}" else slug
                db.collection("stories").document(finalSlug)
                    .set(data)
                    .addOnSuccessListener { onSuccess(finalSlug) }
                    .addOnFailureListener { onError(it) }
            }
            .addOnFailureListener { onError(it) }
    }

    /**
     * Tạo slug URL-friendly từ tên truyện.
     */
    private fun generateSlug(title: String): String {
        val vietnameseMap = mapOf(
            'à' to 'a', 'á' to 'a', 'ả' to 'a', 'ã' to 'a', 'ạ' to 'a',
            'ă' to 'a', 'ắ' to 'a', 'ặ' to 'a', 'ằ' to 'a', 'ẳ' to 'a', 'ẵ' to 'a',
            'â' to 'a', 'ấ' to 'a', 'ầ' to 'a', 'ẩ' to 'a', 'ẫ' to 'a', 'ậ' to 'a',
            'è' to 'e', 'é' to 'e', 'ẻ' to 'e', 'ẽ' to 'e', 'ẹ' to 'e',
            'ê' to 'e', 'ế' to 'e', 'ề' to 'e', 'ể' to 'e', 'ễ' to 'e', 'ệ' to 'e',
            'ì' to 'i', 'í' to 'i', 'ỉ' to 'i', 'ĩ' to 'i', 'ị' to 'i',
            'ò' to 'o', 'ó' to 'o', 'ỏ' to 'o', 'õ' to 'o', 'ọ' to 'o',
            'ô' to 'o', 'ố' to 'o', 'ồ' to 'o', 'ổ' to 'o', 'ỗ' to 'o', 'ộ' to 'o',
            'ơ' to 'o', 'ớ' to 'o', 'ờ' to 'o', 'ở' to 'o', 'ỡ' to 'o', 'ợ' to 'o',
            'ù' to 'u', 'ú' to 'u', 'ủ' to 'u', 'ũ' to 'u', 'ụ' to 'u',
            'ư' to 'u', 'ứ' to 'u', 'ừ' to 'u', 'ử' to 'u', 'ữ' to 'u', 'ự' to 'u',
            'ỳ' to 'y', 'ý' to 'y', 'ỷ' to 'y', 'ỹ' to 'y', 'ỵ' to 'y',
            'đ' to 'd'
        )
        return title.lowercase()
            .map { vietnameseMap[it] ?: it }
            .joinToString("")
            .replace(Regex("[^a-z0-9\\s-]"), "")
            .replace(Regex("\\s+"), "-")
            .replace(Regex("-+"), "-")
            .trim('-')
            .take(80)
    }

    /**
     * Tác giả đăng chapter mới cho một truyện đã có.
     *
     * FIX: Chuỗi async được nối đúng thứ tự:
     *   1. set(chapterData)
     *   2a. Nếu truyện tranh → batch.commit() pages → update totalChapters → onSuccess()
     *   2b. Nếu tiểu thuyết  → update(content) → update totalChapters → onSuccess()
     *   2c. Nếu không có pages/content → update totalChapters → onSuccess()
     */
    fun uploadChapter(
        storyFirestoreId: String,
        chapterNumber: Int,
        title: String,
        contentText: String,
        pageUrls: List<String>,
        isFree: Boolean,
        coinPrice: Int,
        onSuccess: () -> Unit,
        onError: (Exception) -> Unit
    ) {
        val authorId = UserSession.firebaseUid ?: run {
            onError(Exception("Bạn cần đăng nhập để đăng chapter"))
            return
        }

        val chapterData = hashMapOf(
            "chapterNumber" to chapterNumber,
            "title"         to title,
            "isFree"        to isFree,
            "coinPrice"     to if (isFree) 0 else coinPrice,
            "authorId"      to authorId,
            "createdAt"     to System.currentTimeMillis().toString()
        )

        val storyRef   = db.collection("stories").document(storyFirestoreId)
        val chapterId  = "chapter_$chapterNumber"
        val chapterRef = storyRef.collection("chapters").document(chapterId)

        // Bước 1: Tạo document chapter
        chapterRef.set(chapterData)
            .addOnFailureListener { onError(it) }
            .addOnSuccessListener {

                // Helper: tăng totalChapters rồi gọi onSuccess()
                fun incrementAndFinish() {
                    storyRef.update("totalChapters", FieldValue.increment(1))
                        .addOnSuccessListener { onSuccess() }
                        .addOnFailureListener { onError(it) }
                }

                when {
                    // Bước 2a: Truyện tranh — lưu pages vào sub-collection
                    pageUrls.isNotEmpty() -> {
                        val batch = db.batch()
                        pageUrls.forEachIndexed { index, url ->
                            val pageRef = chapterRef
                                .collection("pages")
                                .document("page_${index + 1}")
                            batch.set(pageRef, mapOf("pageNumber" to index + 1, "url" to url))
                        }
                        batch.commit()
                            .addOnFailureListener { onError(it) }
                            .addOnSuccessListener { incrementAndFinish() }
                    }

                    // Bước 2b: Tiểu thuyết — lưu content vào field
                    contentText.isNotEmpty() -> {
                        chapterRef.update("content", contentText)
                            .addOnFailureListener { onError(it) }
                            .addOnSuccessListener { incrementAndFinish() }
                    }

                    // Bước 2c: Không có gì thêm
                    else -> incrementAndFinish()
                }
            }
    }

    // ─── COMMENTS ────────────────────────────────────────────────────────────

    private var comments: MutableList<Comment> = mutableListOf()
    private var isCommentsLoaded = false

    private fun defaultComments() = mutableListOf(
        Comment(id = "1", firestoreId = "sample", userId = "user1", userName = "Naruto_Fan", content = "Truyện hay quá!"),
        Comment(id = "2", firestoreId = "sample", userId = "user2", userName = "MangaLover", content = "Tác giả vẽ đẹp vãi!")
    )

    fun initComments(context: Context) {
        if (isCommentsLoaded) return
        val saved = CommentStorage.loadComments(context)
        comments = if (saved.isEmpty()) defaultComments() else saved
        isCommentsLoaded = true
    }

    fun getCommentsByFirestoreId(firestoreId: String): List<Comment> =
        comments.filter { it.firestoreId == firestoreId && it.chapterId == null }

    fun getCommentsByChapter(firestoreId: String, chapterId: Int): List<Comment> =
        comments.filter { it.firestoreId == firestoreId && it.chapterId == chapterId }

    fun addComment(context: Context, comment: Comment) {
        comments.add(comment)
        CommentStorage.saveComments(context, comments)
    }

    // ─── FAVORITES & FOLLOWING ───────────────────────────────────────────────

    // Cache local để tránh gọi Firestore liên tục trong cùng session
    private val favoriteCache = mutableMapOf<String, MutableList<String>>()   // uid -> [storyId]
    private val followingCache = mutableMapOf<String, MutableList<String>>()  // uid -> [storyId]

    /** Lấy danh sách storyId đã yêu thích (từ cache hoặc Firestore) */
    fun getFavoriteIds(uid: String): List<String> = favoriteCache[uid] ?: emptyList()

    /** Lấy danh sách storyId đang theo dõi (từ cache hoặc Firestore) */
    fun getFollowingIds(uid: String): List<String> = followingCache[uid] ?: emptyList()

    /** Tải favorites + following từ Firestore về cache */
    fun loadUserLists(uid: String, onComplete: () -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                @Suppress("UNCHECKED_CAST")
                val favs = (doc.get("favorites") as? List<String>) ?: emptyList()
                val follows = (doc.get("following") as? List<String>) ?: emptyList()
                favoriteCache[uid] = favs.toMutableList()
                followingCache[uid] = follows.toMutableList()
                onComplete()
            }
            .addOnFailureListener { onComplete() }
    }

    /** Kiểm tra truyện có trong yêu thích không */
    fun isFavorite(uid: String, storyId: String): Boolean =
        favoriteCache[uid]?.contains(storyId) == true

    /** Kiểm tra truyện có đang theo dõi không */
    fun isFollowing(uid: String, storyId: String): Boolean =
        followingCache[uid]?.contains(storyId) == true

    /** Thêm vào yêu thích */
    fun addFavorite(uid: String, storyId: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        db.collection("users").document(uid)
            .update("favorites", com.google.firebase.firestore.FieldValue.arrayUnion(storyId))
            .addOnSuccessListener {
                favoriteCache.getOrPut(uid) { mutableListOf() }.add(storyId)
                onSuccess()
            }
            .addOnFailureListener { e ->
                // Nếu field chưa tồn tại, set luôn
                db.collection("users").document(uid)
                    .update(mapOf("favorites" to listOf(storyId)))
                    .addOnSuccessListener {
                        favoriteCache.getOrPut(uid) { mutableListOf() }.add(storyId)
                        onSuccess()
                    }
                    .addOnFailureListener { onError(it) }
            }
    }

    /** Xóa khỏi yêu thích */
    fun removeFavorite(uid: String, storyId: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        db.collection("users").document(uid)
            .update("favorites", com.google.firebase.firestore.FieldValue.arrayRemove(storyId))
            .addOnSuccessListener {
                favoriteCache[uid]?.remove(storyId)
                onSuccess()
            }
            .addOnFailureListener { onError(it) }
    }

    /** Thêm vào theo dõi */
    fun addFollowing(uid: String, storyId: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        db.collection("users").document(uid)
            .update("following", com.google.firebase.firestore.FieldValue.arrayUnion(storyId))
            .addOnSuccessListener {
                followingCache.getOrPut(uid) { mutableListOf() }.add(storyId)
                onSuccess()
            }
            .addOnFailureListener {
                db.collection("users").document(uid)
                    .update(mapOf("following" to listOf(storyId)))
                    .addOnSuccessListener {
                        followingCache.getOrPut(uid) { mutableListOf() }.add(storyId)
                        onSuccess()
                    }
                    .addOnFailureListener { e -> onError(e) }
            }
    }

    /** Bỏ theo dõi */
    fun removeFollowing(uid: String, storyId: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
        db.collection("users").document(uid)
            .update("following", com.google.firebase.firestore.FieldValue.arrayRemove(storyId))
            .addOnSuccessListener {
                followingCache[uid]?.remove(storyId)
                onSuccess()
            }
            .addOnFailureListener { onError(it) }
    }

    /** Toggle yêu thích: nếu đã thích thì bỏ, chưa thích thì thêm */
    fun toggleFavorite(uid: String, storyId: String, onSuccess: (Boolean) -> Unit, onError: (Exception) -> Unit) {
        if (isFavorite(uid, storyId)) {
            removeFavorite(uid, storyId, { onSuccess(false) }, onError)
        } else {
            addFavorite(uid, storyId, { onSuccess(true) }, onError)
        }
    }

    /** Toggle theo dõi */
    fun toggleFollowing(uid: String, storyId: String, onSuccess: (Boolean) -> Unit, onError: (Exception) -> Unit) {
        if (isFollowing(uid, storyId)) {
            removeFollowing(uid, storyId, { onSuccess(false) }, onError)
        } else {
            addFollowing(uid, storyId, { onSuccess(true) }, onError)
        }
    }

    // ─── HELPER ──────────────────────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    private fun docToManga(id: String, data: Map<String, Any>, index: Int): Manga {
        val statusStr   = data["status"]   as? String ?: "completed"
        val categoryStr = data["category"] as? String ?: "truyen_tranh"
        return Manga(
            id            = index + 1,
            name          = data["title"]       as? String ?: "",
            slug          = id,
            author        = data["author"]      as? String ?: "",
            description   = data["description"] as? String ?: "",
            coverUrl      = data["coverUrl"]    as? String ?: "",
            genres        = (data["genres"]     as? List<String>) ?: emptyList(),
            totalChapters = (data["totalChapters"] as? Long)?.toInt() ?: 0,
            totalViews    = (data["totalViews"]    as? Long)?.toInt() ?: 0,
            status        = if (statusStr == "ongoing") MangaStatus.ONGOING else MangaStatus.COMPLETED,
            category      = if (categoryStr == "tieu_thuyet") MangaCategory.TIEU_THUYET else MangaCategory.TRUYEN_TRANH,
            createdAt     = data["createdAt"] as? String ?: "",
            firestoreId   = id,
            averageRating = (data["averageRating"] as? Double)?.toFloat() ?: 0f,
            ratingCount   = (data["ratingCount"]   as? Long)?.toInt() ?: 0
        )
    }

    /**
     * Người dùng đánh giá truyện (1–5 sao).
     * Dùng Firestore Transaction để tính lại averageRating an toàn khi nhiều user cùng rate.
     *
     * Cấu trúc Firestore:
     *   stories/{storyId}/ratings/{uid}  →  { star: Int, createdAt: String }
     *   stories/{storyId}                →  { averageRating: Float, ratingCount: Int }
     */
    fun submitRating(
        storyId: String,
        star: Int,
        onSuccess: (newAverage: Float, newCount: Int) -> Unit,
        onError: (Exception) -> Unit
    ) {
        val uid = UserSession.firebaseUid ?: run {
            onError(Exception("Bạn cần đăng nhập để đánh giá"))
            return
        }

        val storyRef  = db.collection("stories").document(storyId)
        val ratingRef = storyRef.collection("ratings").document(uid)

        db.runTransaction { tx ->
            val oldRatingSnap = tx.get(ratingRef)
            val storySnap     = tx.get(storyRef)

            val oldStar      = if (oldRatingSnap.exists()) (oldRatingSnap.getLong("star") ?: 0L).toInt() else 0
            val oldAvg       = (storySnap.getDouble("averageRating") ?: 0.0).toFloat()
            val oldCount     = (storySnap.getLong("ratingCount") ?: 0L).toInt()
            val isFirstRating = !oldRatingSnap.exists()

            val (newAvg, newCount) = if (isFirstRating) {
                val nc  = oldCount + 1
                val na  = ((oldAvg * oldCount) + star) / nc
                Pair(na, nc)
            } else {
                // Thay thế star cũ bằng star mới
                val na = ((oldAvg * oldCount) - oldStar + star) / oldCount
                Pair(na, oldCount)
            }

            tx.set(ratingRef, mapOf("star" to star, "createdAt" to System.currentTimeMillis().toString()))
            tx.update(storyRef, mapOf("averageRating" to newAvg, "ratingCount" to newCount))

            Pair(newAvg, newCount)
        }.addOnSuccessListener { (avg, count) ->
            onSuccess(avg, count)
        }.addOnFailureListener { onError(it) }
    }

    /**
     * Lấy số sao người dùng hiện tại đã rate cho truyện này (0 = chưa rate).
     */
    fun getUserRating(
        storyId: String,
        onResult: (Int) -> Unit
    ) {
        val uid = UserSession.firebaseUid ?: run { onResult(0); return }
        db.collection("stories").document(storyId)
            .collection("ratings").document(uid)
            .get()
            .addOnSuccessListener { doc ->
                onResult(if (doc.exists()) (doc.getLong("star") ?: 0L).toInt() else 0)
            }
            .addOnFailureListener { onResult(0) }
    }
}