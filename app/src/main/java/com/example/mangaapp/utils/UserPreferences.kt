package com.example.mangaapp.utils

import android.content.Context
import android.content.SharedPreferences
import com.example.mangaapp.models.User

/**
 * Helper lưu/đọc thông tin User vào SharedPreferences.
 *
 * Mục đích: Giữ session qua app restart và process kill.
 * Firebase Auth tự persist token ✅ — nhưng currentUser (coins, role, unlockedChapters)
 * cần được cache thủ công ở đây.
 *
 * Serialize thủ công bằng SharedPreferences thuần (không cần Gson).
 */
object UserPreferences {

    private const val PREF_NAME = "user_session"

    // Keys
    private const val KEY_FIRESTORE_ID       = "firestore_id"
    private const val KEY_USERNAME           = "username"
    private const val KEY_EMAIL              = "email"
    private const val KEY_AVATAR_URL         = "avatar_url"
    private const val KEY_ROLE_ID            = "role_id"
    private const val KEY_COINS              = "coins"
    private const val KEY_UNLOCKED_CHAPTERS  = "unlocked_chapters"  // StringSet

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ── Save ──────────────────────────────────────────────────────────────────

    /**
     * Lưu toàn bộ thông tin user vào SharedPreferences.
     * Gọi mỗi khi currentUser thay đổi (load từ Firestore, cập nhật coin, unlock chapter).
     */
    fun saveUser(context: Context, user: User) {
        prefs(context).edit().apply {
            putString(KEY_FIRESTORE_ID,      user.firestoreId)
            putString(KEY_USERNAME,          user.username)
            putString(KEY_EMAIL,             user.email)
            putString(KEY_AVATAR_URL,        user.avatarUrl)
            putInt(KEY_ROLE_ID,              user.roleId)
            putInt(KEY_COINS,               user.coins)
            // StringSet không preserve order nhưng ổn với unlocked chapters
            putStringSet(KEY_UNLOCKED_CHAPTERS, user.unlockedChapters.toSet())
            apply()
        }
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    /**
     * Đọc thông tin user từ SharedPreferences.
     * @return User nếu đã từng lưu, null nếu chưa có (chưa login bao giờ / đã logout)
     */
    fun loadUser(context: Context): User? {
        val p = prefs(context)
        val firestoreId = p.getString(KEY_FIRESTORE_ID, null)
            ?: return null  // Chưa có cache → chưa login

        return User(
            firestoreId      = firestoreId,
            username         = p.getString(KEY_USERNAME,  "") ?: "",
            email            = p.getString(KEY_EMAIL,     "") ?: "",
            avatarUrl        = p.getString(KEY_AVATAR_URL,"") ?: "",
            roleId           = p.getInt(KEY_ROLE_ID, 2),
            coins            = p.getInt(KEY_COINS,    0),
            unlockedChapters = (p.getStringSet(KEY_UNLOCKED_CHAPTERS, emptySet()) ?: emptySet()).toList()
        )
    }

    // ── Clear ─────────────────────────────────────────────────────────────────

    /**
     * Xóa toàn bộ cache user. Gọi khi logout.
     */
    fun clearUser(context: Context) {
        prefs(context).edit().clear().apply()
    }

    // ── Partial updates (hiệu quả hơn, tránh ghi toàn bộ) ───────────────────

    /** Chỉ cập nhật coins (ví dụ: sau khi trừ/cộng coin) */
    fun updateCoins(context: Context, newCoins: Int) {
        prefs(context).edit().putInt(KEY_COINS, newCoins).apply()
    }

    /** Thêm một key mở khóa vào StringSet */
    fun addUnlockedChapter(context: Context, key: String) {
        val p = prefs(context)
        val current = p.getStringSet(KEY_UNLOCKED_CHAPTERS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(key)
        p.edit().putStringSet(KEY_UNLOCKED_CHAPTERS, current).apply()
    }

    // ── Reading History ────────────────────────────────────────────────────────

    private const val PREF_HISTORY_NAME = "reading_history"

    private fun historyPrefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREF_HISTORY_NAME, Context.MODE_PRIVATE)

    /** Lưu chương đang đọc gần nhất của bộ truyện */
    fun saveLastReadChapter(context: Context, storyId: String, chapterNumber: Int) {
        if (storyId.isEmpty()) return
        historyPrefs(context).edit().putInt(storyId, chapterNumber).apply()
    }

    /** Lấy chương đang đọc gần nhất của bộ truyện. Trả về -1 nếu chưa đọc */
    fun getLastReadChapter(context: Context, storyId: String): Int {
        if (storyId.isEmpty()) return -1
        return historyPrefs(context).getInt(storyId, -1)
    }
}
