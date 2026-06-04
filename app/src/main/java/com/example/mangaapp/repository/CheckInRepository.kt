package com.example.mangaapp.repository

import com.example.mangaapp.utils.UserSession
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Xử lý logic điểm danh hàng ngày (Daily Check-in).
 *
 * Firestore fields trên users/{uid}:
 *   - lastCheckInDate : String  ("yyyy-MM-dd")
 *   - checkInStreak   : Int     (số ngày liên tiếp)
 *
 * Bảng thưởng:
 *   Ngày 1→5 : streak × 5 coin
 *   Ngày 6   : 30 coin
 *   Ngày 7+  : 50 coin (bonus tuần)
 */
object CheckInRepository {

    private val db = FirebaseFirestore.getInstance()

    /** Múi giờ Việt Nam */
    private val VN_TZ = TimeZone.getTimeZone("Asia/Ho_Chi_Minh")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
        timeZone = VN_TZ
    }

    // ── Reward table ─────────────────────────────────────────────────────────

    /** Tính số coin thưởng dựa trên streak */
    fun getRewardForStreak(streak: Int): Int = when {
        streak <= 0 -> 5
        streak <= 5 -> streak * 5
        streak == 6 -> 30
        else        -> 50   // ngày 7+
    }

    // ── Check-in ─────────────────────────────────────────────────────────────

    data class CheckInStatus(
        val alreadyCheckedInToday: Boolean,
        val currentStreak: Int,
        val todayReward: Int
    )

    /**
     * Đọc trạng thái điểm danh hôm nay.
     */
    fun getCheckInStatus(onResult: (CheckInStatus) -> Unit) {
        val uid = UserSession.firebaseUid ?: run {
            onResult(CheckInStatus(false, 0, getRewardForStreak(1)))
            return
        }

        db.collection("users").document(uid).get()
            .addOnSuccessListener { doc ->
                val lastDate = doc.getString("lastCheckInDate") ?: ""
                val streak   = (doc.getLong("checkInStreak") ?: 0L).toInt()
                val today    = todayStr()

                if (lastDate == today) {
                    // Đã điểm danh hôm nay
                    onResult(CheckInStatus(true, streak, getRewardForStreak(streak)))
                } else {
                    // Tính streak mới nếu điểm danh hôm nay
                    val newStreak = if (isYesterday(lastDate)) streak + 1 else 1
                    onResult(CheckInStatus(false, newStreak, getRewardForStreak(newStreak)))
                }
            }
            .addOnFailureListener {
                onResult(CheckInStatus(false, 0, getRewardForStreak(1)))
            }
    }

    /**
     * Thực hiện điểm danh — Firestore Transaction:
     *  1. Check chưa điểm danh hôm nay
     *  2. Tính streak mới
     *  3. Cộng coin
     *  4. Ghi log coinTransactions
     */
    fun checkIn(
        onSuccess: (reward: Int, newStreak: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = UserSession.firebaseUid ?: run {
            onError("Bạn cần đăng nhập để điểm danh")
            return
        }

        val userRef  = db.collection("users").document(uid)
        val txLogRef = db.collection("coinTransactions").document()
        val today    = todayStr()

        db.runTransaction { tx: Transaction ->
            val snap = tx.get(userRef)

            val lastDate = snap.getString("lastCheckInDate") ?: ""
            val streak   = (snap.getLong("checkInStreak") ?: 0L).toInt()

            // Đã điểm danh rồi
            if (lastDate == today) {
                throw Exception("ALREADY_CHECKED_IN")
            }

            // Tính streak mới
            val newStreak = if (isYesterday(lastDate)) streak + 1 else 1
            val reward    = getRewardForStreak(newStreak)

            // Update user
            tx.update(userRef, mapOf(
                "lastCheckInDate" to today,
                "checkInStreak"   to newStreak,
                "coins"           to com.google.firebase.firestore.FieldValue.increment(reward.toLong())
            ))

            // Ghi log
            tx.set(txLogRef, hashMapOf(
                "userId"    to uid,
                "type"      to "daily_check_in",
                "amount"    to reward,
                "streak"    to newStreak,
                "date"      to today,
                "timestamp" to System.currentTimeMillis()
            ))

            Pair(reward, newStreak)
        }.addOnSuccessListener { result ->
            val (reward, newStreak) = result
            // Cập nhật cache local
            val oldCoins = UserSession.currentUser?.coins ?: 0
            UserSession.updateCoins(oldCoins + reward)
            onSuccess(reward, newStreak)
        }.addOnFailureListener { e ->
            if (e.message == "ALREADY_CHECKED_IN") {
                onError("Bạn đã điểm danh hôm nay rồi! 🎉")
            } else {
                onError(e.message ?: "Điểm danh thất bại, thử lại sau")
            }
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    /** Ngày hôm nay dạng "yyyy-MM-dd" (VN timezone) */
    private fun todayStr(): String = dateFormat.format(Calendar.getInstance(VN_TZ).time)

    /** Kiểm tra dateStr có phải là ngày hôm qua không */
    private fun isYesterday(dateStr: String): Boolean {
        if (dateStr.isEmpty()) return false
        return try {
            val lastDate = dateFormat.parse(dateStr) ?: return false
            val yesterday = Calendar.getInstance(VN_TZ).apply {
                add(Calendar.DAY_OF_YEAR, -1)
            }
            dateFormat.format(lastDate) == dateFormat.format(yesterday.time)
        } catch (_: Exception) {
            false
        }
    }
}
