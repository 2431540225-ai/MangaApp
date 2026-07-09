package com.example.mangaapp.utils

import android.content.Context
import com.example.mangaapp.models.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

/**
 * Singleton quản lý trạng thái đăng nhập và coin của user hiện tại.
 *
 * ── Session persistence (2 lớp) ────────────────────────────────────────────
 * Lớp 1 — SharedPreferences (instant, offline):
 *   Snapshot của User được lưu vào SharedPreferences mỗi khi thay đổi.
 *   Khi app mở lại sau khi bị kill → đọc ngay, không cần Firestore.
 *
 * Lớp 2 — Firestore refresh (fresh data, online):
 *   Sau khi đọc cache, tự động sync Firestore ở background để lấy
 *   coins/unlockedChapters mới nhất (tránh data stale).
 *
 * ── Cách dùng ───────────────────────────────────────────────────────────────
 *   UserSession.init(context)     // Gọi một lần trong MainActivity.onCreate()
 *   UserSession.currentUser       // User hiện tại (null nếu chưa login)
 *   UserSession.isLoggedIn        // true nếu đã login và có currentUser
 */
object UserSession {

    private val auth = FirebaseAuth.getInstance()
    private val db   = FirebaseFirestore.getInstance()

    /** Context của Application — dùng để đọc/ghi SharedPreferences */
    private var appContext: Context? = null

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
     * Khởi tạo UserSession khi app mở.
     * Gọi một lần duy nhất trong MainActivity.onCreate().
     *
     * Luồng:
     * 1. Lưu appContext để dùng cho SharedPreferences
     * 2. Đọc cache local (SharedPreferences) → currentUser có ngay lập tức
     * 3. Nếu Firebase Auth còn token → sync Firestore ở background (fresh data)
     */
    fun init(context: Context) {
        appContext = context.applicationContext

        val firebaseUser = auth.currentUser
        if (firebaseUser == null) {
            // Chưa login → clear cache phòng trường hợp stale
            currentUser = null
            UserPreferences.clearUser(context)
            return
        }

        // Bước 1: Đọc cache SharedPreferences ngay lập tức
        val cached = UserPreferences.loadUser(context)
        if (cached != null && cached.firestoreId == firebaseUser.uid) {
            // Cache hợp lệ → set ngay để UI có data tức thì
            currentUser = cached
            startCoinListener(firebaseUser.uid)
        }

        // Bước 2: Sync Firestore ở background (không block UI)
        // Kết quả sẽ ghi đè cache nếu có thay đổi (coin mới, chapter unlock mới)
        refreshFromFirestore(firebaseUser.uid)
    }

    /**
     * Refresh data từ Firestore và cập nhật cache.
     * Được gọi ngầm sau khi đọc cache local.
     */
    private fun refreshFromFirestore(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val data = doc.data ?: return@addOnSuccessListener
                @Suppress("UNCHECKED_CAST")
                val freshUser = User(
                    firestoreId      = uid,
                    username         = data["username"] as? String ?: "",
                    email            = data["email"]    as? String ?: "",
                    avatarUrl        = data["avatarUrl"] as? String ?: "",
                    roleId           = (data["roleId"]  as? Long)?.toInt() ?: 2,
                    coins            = (data["coins"]   as? Long)?.toInt() ?: 0,
                    unlockedChapters = (data["unlockedChapters"] as? List<String>) ?: emptyList()
                )
                val oldCoins = currentUser?.coins
                currentUser = freshUser

                // Lưu lại vào SharedPreferences với data mới nhất
                appContext?.let { UserPreferences.saveUser(it, freshUser) }

                // Nếu coin thay đổi so với cache → notify listeners
                if (oldCoins != null && oldCoins != freshUser.coins) {
                    coinChangeListeners.forEach { it(freshUser.coins) }
                }

                // Đảm bảo realtime listener đang chạy
                startCoinListener(uid)
            }
            // Nếu Firestore lỗi → giữ nguyên cache local, không crash
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
                    val user = User(
                        firestoreId       = uid,
                        username          = data["username"] as? String ?: "",
                        email             = data["email"]    as? String ?: "",
                        avatarUrl         = data["avatarUrl"] as? String ?: "",
                        roleId            = (data["roleId"]  as? Long)?.toInt() ?: 2,
                        coins             = (data["coins"]   as? Long)?.toInt() ?: 0,
                        unlockedChapters  = (data["unlockedChapters"] as? List<String>) ?: emptyList()
                    )
                    currentUser = user
                    // Lưu vào SharedPreferences
                    appContext?.let { UserPreferences.saveUser(it, user) }
                    startCoinListener(uid)
                    onComplete(user)
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
                val user = User(
                    firestoreId = uid,
                    username    = newUser["username"] as String,
                    email       = newUser["email"] as String,
                    roleId      = 2,
                    coins       = 0
                )
                currentUser = user
                // Lưu vào SharedPreferences
                appContext?.let { ctx -> UserPreferences.saveUser(ctx, user) }
                startCoinListener(uid)
                onComplete(user)
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
                    // Persist thay đổi coin vào SharedPreferences
                    appContext?.let { UserPreferences.updateCoins(it, newCoins) }
                    coinChangeListeners.forEach { it(newCoins) }
                }
            }
    }

    /** Cập nhật coin trong cache local (sau khi trừ/cộng coin) */
    fun updateCoins(newCoins: Int) {
        currentUser = currentUser?.copy(coins = newCoins)
        appContext?.let { UserPreferences.updateCoins(it, newCoins) }
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
            // Persist vào SharedPreferences
            appContext?.let { UserPreferences.addUnlockedChapter(it, key) }
        }
    }

    /** Đăng xuất */
    fun signOut() {
        coinListener?.remove()
        coinListener = null
        currentUser  = null
        coinChangeListeners.clear()
        // Xóa cache local khi logout
        appContext?.let { UserPreferences.clearUser(it) }
        auth.signOut()
    }
}