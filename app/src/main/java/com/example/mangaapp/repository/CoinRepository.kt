package com.example.mangaapp.repository

import com.example.mangaapp.models.CoinPackage
import com.example.mangaapp.utils.UserSession
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Transaction

/**
 * Xử lý toàn bộ logic liên quan đến Coin:
 *  - Tải danh sách gói coin
 *  - Mở khóa chapter (trừ coin + ghi unlockedChapters + chia hoa hồng)
 *  - Kiểm tra chapter đã mở chưa
 *  - Nạp coin (sau khi thanh toán thành công)
 */
object CoinRepository {

    private val db = FirebaseFirestore.getInstance()

    /** Tỉ lệ chia hoa hồng cho tác giả (70%) */
    private const val AUTHOR_REVENUE_RATIO = 0.70

    // ── Coin Packages ─────────────────────────────────────────────────────────

    /**
     * Lấy danh sách gói coin từ Firestore.
     * Nếu chưa có data trên Firestore, trả về danh sách mặc định.
     */
    fun getCoinPackages(
        onSuccess: (List<CoinPackage>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        db.collection("coinPackages")
            .orderBy("coins")
            .get()
            .addOnSuccessListener { result ->
                if (result.isEmpty) {
                    onSuccess(defaultPackages())
                    return@addOnSuccessListener
                }
                val list = result.documents.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    CoinPackage(
                        id          = doc.id,
                        coins       = (data["coins"]      as? Long)?.toInt() ?: 0,
                        bonusCoins  = (data["bonusCoins"] as? Long)?.toInt() ?: 0,
                        priceLabel  = data["priceLabel"]  as? String ?: "",
                        priceVnd    = data["priceVnd"]    as? Long   ?: 0L,
                        badge       = data["badge"]       as? String ?: ""
                    )
                }
                onSuccess(if (list.isEmpty()) defaultPackages() else list)
            }
            .addOnFailureListener { onError(it) }
    }

    /** Danh sách gói coin mặc định (dùng khi Firestore chưa có data) */
    private fun defaultPackages() = listOf(
        CoinPackage("pkg_50",   50,   0,  "5.000đ",  5_000,  ""),
        CoinPackage("pkg_100",  100,  10, "10.000đ", 10_000, "Phổ biến"),
        CoinPackage("pkg_300",  300,  50, "25.000đ", 25_000, ""),
        CoinPackage("pkg_500",  500, 100, "40.000đ", 40_000, "Tiết kiệm nhất"),
        CoinPackage("pkg_1000", 1000, 300,"80.000đ", 80_000, "")
    )

    // ── Unlock Chapter ────────────────────────────────────────────────────────

    /**
     * Mở khóa một chapter bằng coin.
     *
     * Thực hiện trong một Firestore Transaction để đảm bảo atomic:
     *  1. Kiểm tra user còn đủ coin
     *  2. Trừ coin của user
     *  3. Thêm key vào unlockedChapters của user
     *  4. Cộng hoa hồng vào tài khoản tác giả (70%)
     *  5. Ghi log vào coinTransactions
     *
     * @param storyFirestoreId  Firestore document ID của truyện
     * @param chapterNumber     Số thứ tự chapter
     * @param coinPrice         Số coin cần trả
     * @param authorId          Firestore UID của tác giả
     */
    fun unlockChapter(
        storyFirestoreId: String,
        chapterNumber: Int,
        coinPrice: Int,
        authorId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = UserSession.firebaseUid ?: run { onError("Bạn cần đăng nhập để mở khóa chương này"); return }
        val currentCoins = UserSession.currentUser?.coins ?: 0

        if (currentCoins < coinPrice) {
            onError("Không đủ coin! Bạn cần $coinPrice coin nhưng chỉ có $currentCoins coin.")
            return
        }

        val unlockKey    = "${storyFirestoreId}_$chapterNumber"
        val userRef      = db.collection("users").document(uid)
        val txLogRef     = db.collection("coinTransactions").document()
        val authorRef    = db.collection("authorRevenue").document(authorId)

        db.runTransaction { tx: Transaction ->
            val userSnap = tx.get(userRef)
            val latestCoins = (userSnap.getLong("coins") ?: 0).toInt()

            if (latestCoins < coinPrice) throw Exception("Không đủ coin!")

            val existingUnlocked = (userSnap.get("unlockedChapters") as? List<*>) ?: emptyList<String>()
            if (existingUnlocked.contains(unlockKey)) {
                // Đã mở rồi → không trừ coin lần nữa
                return@runTransaction
            }

            // 1. Trừ coin user
            tx.update(userRef, "coins", FieldValue.increment(-coinPrice.toLong()))
            // 2. Thêm key vào danh sách đã mở
            tx.update(userRef, "unlockedChapters", FieldValue.arrayUnion(unlockKey))
            // 3. Cộng hoa hồng tác giả 70%
            val authorCoins = (coinPrice * AUTHOR_REVENUE_RATIO).toLong()
            tx.set(
                authorRef,
                mapOf(
                    "authorId"       to authorId,
                    "pendingCoins"   to FieldValue.increment(authorCoins),
                    "totalEarned"    to FieldValue.increment(authorCoins)
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            // 4. Ghi log transaction
            tx.set(txLogRef, hashMapOf(
                "userId"        to uid,
                "type"          to "unlock_chapter",
                "amount"        to -coinPrice,
                "storyId"       to storyFirestoreId,
                "chapterNumber" to chapterNumber,
                "authorId"      to authorId,
                "authorRevenue" to authorCoins,
                "timestamp"     to System.currentTimeMillis()
            ))
        }.addOnSuccessListener {
            // Cập nhật cache local
            val newCoins = (UserSession.currentUser?.coins ?: 0) - coinPrice
            UserSession.updateCoins(newCoins)
            UserSession.addUnlockedChapter(storyFirestoreId, chapterNumber)
            onSuccess()
        }.addOnFailureListener {
            onError(it.message ?: "Mở khóa thất bại, vui lòng thử lại")
        }
    }

    // ── Check Unlock ──────────────────────────────────────────────────────────

    /**
     * Kiểm tra chapter đã được mở khóa chưa.
     * Ưu tiên check cache local (UserSession) trước, nếu không có mới hỏi Firestore.
     */
    fun isChapterUnlocked(
        storyFirestoreId: String,
        chapterNumber: Int,
        onResult: (Boolean) -> Unit
    ) {
        val user = UserSession.currentUser
        if (user != null) {
            // Check cache trước
            onResult(user.hasUnlocked(storyFirestoreId, chapterNumber))
            return
        }
        // Chưa đăng nhập → chưa unlock
        onResult(false)
    }

    // ── Top Up Coins ──────────────────────────────────────────────────────────

    /**
     * Nạp coin vào tài khoản sau khi thanh toán thành công.
     * Gọi hàm này từ payment callback.
     *
     * @param amount Số coin cần cộng vào
     * @param packageId ID gói coin đã mua (để ghi log)
     */
    fun topUpCoins(
        amount: Int,
        packageId: String,
        onSuccess: (newBalance: Int) -> Unit,
        onError: (String) -> Unit
    ) {
        val uid = UserSession.firebaseUid ?: run { onError("Chưa đăng nhập"); return }
        val userRef  = db.collection("users").document(uid)
        val txLogRef = db.collection("coinTransactions").document()

        db.runTransaction { tx: Transaction ->
            val snap        = tx.get(userRef)
            val currentBal  = (snap.getLong("coins") ?: 0).toInt()
            val newBalance  = currentBal + amount

            tx.update(userRef, "coins", newBalance)
            tx.set(txLogRef, hashMapOf(
                "userId"    to uid,
                "type"      to "top_up",
                "amount"    to amount,
                "packageId" to packageId,
                "timestamp" to System.currentTimeMillis()
            ))
            newBalance
        }.addOnSuccessListener { newBalance ->
            UserSession.updateCoins(newBalance as Int)
            onSuccess(newBalance)
        }.addOnFailureListener {
            onError(it.message ?: "Nạp coin thất bại")
        }
    }

    // ── Lịch sử coin độc giả ──────────────────────────────────────────────────

    fun getUserCoinHistory(
        userId: String,
        limit: Int = 30,
        onSuccess: (List<com.example.mangaapp.models.WalletTransaction>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("coinTransactions")
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { result ->
                onSuccess(result.documents.map { doc -> mapUserTransaction(doc.id, doc.data ?: emptyMap()) })
            }
            .addOnFailureListener {
                db.collection("coinTransactions")
                    .whereEqualTo("userId", userId)
                    .get()
                    .addOnSuccessListener { result ->
                        val list = result.documents
                            .sortedByDescending { it.getLong("timestamp") ?: 0L }
                            .take(limit)
                            .map { doc -> mapUserTransaction(doc.id, doc.data ?: emptyMap()) }
                        onSuccess(list)
                    }
                    .addOnFailureListener { e -> onError(e.message ?: "Không tải lịch sử") }
            }
    }

    private fun mapUserTransaction(
        id: String,
        data: Map<String, Any?>
    ): com.example.mangaapp.models.WalletTransaction {
        val type = data["type"] as? String ?: ""
        val amount = (data["amount"] as? Long) ?: 0L
        return com.example.mangaapp.models.WalletTransaction(
            id            = id,
            type          = type,
            amount        = amount,
            timestamp     = (data["timestamp"] as? Long) ?: 0L,
            storyTitle    = data["storyId"] as? String ?: "",
            chapterNumber = ((data["chapterNumber"] as? Long) ?: 0L).toInt()
        )
    }
}