package com.example.mangaapp.utils

import com.example.mangaapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Singleton quản lý trạng thái đăng nhập và coin của user hiện tại.
 * Gọi UserSession.currentUser để lấy thông tin user.
 * Gọi UserSession.isLoggedIn để kiểm tra đăng nhập.
 */
object UserSession {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    /** User hiện tại (null nếu chưa đăng nhập) */
    var currentUser: User? = null
        private set

    /** Lắng nghe realtime cập nhật coin */
    private var coinListener: ListenerRegistration? = null

    val isLoggedIn: Boolean get() = auth.currentUser != null && currentUser != null

    val firebaseUid: String? get() = auth.currentUser?.uid

    // ── Callbacks cho các màn hình cần biết khi coin thay đổi ────────────────
    private val coinChangeListeners = mutableListOf<(Int) -> Unit>()

    fun addCoinChangeListener(listener: (Int) -> Unit) {
        coinChangeListeners.add(listener)
    }

    fun removeCoinChangeListener(listener: (Int) -> Unit) {
        coinChangeListeners.remove(listener)
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tải thông tin user từ Firestore sau khi đăng nhập Firebase Auth thành công.
     * Đồng thời bắt đầu lắng nghe thay đổi coin realtime.
     */
    fun loadUser(onComplete: (User?) -> Unit = {}) {
        val uid = auth.currentUser?.uid ?: run { onComplete(null); return }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val data = doc.data
                if (data != null) {
                    @Suppress("UNCHECKED_CAST")
                    currentUser = User(
                        firestoreId       = uid,
                        username          = data["username"] as? String ?: "",
                        email             = data["email"]    as? String ?: "",
                        avatarUrl         = data["avatarUrl"] as? String ?: "",
                        roleId            = (data["roleId"]  as? Long)?.toInt() ?: 2,
                        coins             = (data["coins"]   as? Long)?.toInt() ?: 0,
                        unlockedChapters  = (data["unlockedChapters"] as? List<String>) ?: emptyList()
                    )
                    startCoinListener(uid)
                    onComplete(currentUser)
                } else {
                    // Document chưa tồn tại → tạo mới
                    createUserDocument(uid, onComplete)
                }
            }
            .addOnFailureListener { onComplete(null) }
    }

    /** Tạo document user mới trong Firestore (lần đầu đăng ký) */
    fun createUserDocument(uid: String, onComplete: (User?) -> Unit = {}) {
        val firebaseUser = auth.currentUser ?: return
        val newUser = hashMapOf(
            "username"         to (firebaseUser.displayName ?: firebaseUser.email?.substringBefore("@") ?: "Độc giả"),
            "email"            to (firebaseUser.email ?: ""),
            "avatarUrl"        to "",
            "roleId"           to 2,
            "coins"            to 0,
            "unlockedChapters" to emptyList<String>(),
            "createdAt"        to System.currentTimeMillis()
        )
        db.collection("users").document(uid).set(newUser)
            .addOnSuccessListener {
                currentUser = User(
                    firestoreId = uid,
                    username    = newUser["username"] as String,
                    email       = newUser["email"] as String,
                    roleId      = 2,
                    coins       = 0
                )
                startCoinListener(uid)
                onComplete(currentUser)
            }
            .addOnFailureListener { onComplete(null) }
    }

    /** Lắng nghe thay đổi coin realtime từ Firestore */
    private fun startCoinListener(uid: String) {
        coinListener?.remove()
        coinListener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                val newCoins = (snapshot?.getLong("coins"))?.toInt() ?: return@addSnapshotListener
                val oldCoins = currentUser?.coins ?: return@addSnapshotListener
                if (newCoins != oldCoins) {
                    currentUser = currentUser?.copy(coins = newCoins)
                    coinChangeListeners.forEach { it(newCoins) }
                }
            }
    }

    /** Cập nhật coin trong cache local (sau khi trừ/cộng coin) */
    fun updateCoins(newCoins: Int) {
        currentUser = currentUser?.copy(coins = newCoins)
        coinChangeListeners.forEach { it(newCoins) }
    }

    /** Cập nhật danh sách chapter đã mở khóa trong cache local */
    fun addUnlockedChapter(storyId: String, chapterNumber: Int) {
        val key = "${storyId}_$chapterNumber"
        val current = currentUser ?: return
        if (!current.unlockedChapters.contains(key)) {
            currentUser = current.copy(
                unlockedChapters = current.unlockedChapters + key
            )
        }
    }

    /** Đăng xuất */
    fun signOut() {
        coinListener?.remove()
        coinListener = null
        currentUser  = null
        coinChangeListeners.clear()
        auth.signOut()
    }
}