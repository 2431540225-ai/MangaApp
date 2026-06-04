package com.example.mangaapp.repository

import com.example.mangaapp.models.AuthorRevenueSummary
import com.example.mangaapp.models.WalletTransaction
import com.example.mangaapp.utils.UserSession
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Transaction

/**
 * Quản lý ví doanh thu tác giả: tổng thu, rút tiền, lịch sử.
 */
object AuthorRevenueRepository {

    private val db = FirebaseFirestore.getInstance()

    /** Tỉ lệ tác giả nhận (đồng bộ với CoinRepository) */
    const val AUTHOR_SHARE_PERCENT = 70

    /** Số coin tối thiểu để yêu cầu rút */
    const val MIN_WITHDRAW_COINS = 100L

    /** 1 coin ≈ 100 VND khi quy đổi rút tiền */
    const val VND_PER_COIN = 100L

    // ── Summary ─────────────────────────────────────────────────────────────

    fun getRevenueSummary(
        authorId: String,
        onSuccess: (AuthorRevenueSummary) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("authorRevenue").document(authorId).get()
            .addOnSuccessListener { doc ->
                onSuccess(
                    AuthorRevenueSummary(
                        pendingCoins     = doc.getLong("pendingCoins") ?: 0L,
                        totalEarned      = doc.getLong("totalEarned") ?: 0L,
                        totalWithdrawn   = doc.getLong("totalWithdrawn") ?: 0L,
                        authorSharePercent = AUTHOR_SHARE_PERCENT
                    )
                )
            }
            .addOnFailureListener { onError(it.message ?: "Không tải được doanh thu") }
    }

    // ── Lịch sử thu nhập (từ coinTransactions) ──────────────────────────────

    fun getEarningsHistory(
        authorId: String,
        limit: Int = 50,
        onSuccess: (List<WalletTransaction>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("coinTransactions")
            .whereEqualTo("authorId", authorId)
            .whereEqualTo("type", "unlock_chapter")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.map { doc ->
                    WalletTransaction(
                        id            = doc.id,
                        type          = "author_earning",
                        amount        = doc.getLong("authorRevenue") ?: 0L,
                        timestamp     = doc.getLong("timestamp") ?: 0L,
                        storyTitle    = doc.getString("storyTitle") ?: doc.getString("storyId") ?: "",
                        chapterNumber = (doc.getLong("chapterNumber") ?: 0).toInt(),
                        note          = "Hoa hồng ${AUTHOR_SHARE_PERCENT}%"
                    )
                }
                onSuccess(list)
            }
            .addOnFailureListener {
                // Firestore có thể thiếu composite index → fallback không orderBy
                loadEarningsFallback(authorId, limit, onSuccess, onError)
            }
    }

    private fun loadEarningsFallback(
        authorId: String,
        limit: Int,
        onSuccess: (List<WalletTransaction>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("coinTransactions")
            .whereEqualTo("authorId", authorId)
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents
                    .filter { it.getString("type") == "unlock_chapter" }
                    .sortedByDescending { it.getLong("timestamp") ?: 0L }
                    .take(limit)
                    .map { doc ->
                        WalletTransaction(
                            id            = doc.id,
                            type          = "author_earning",
                            amount        = doc.getLong("authorRevenue") ?: 0L,
                            timestamp     = doc.getLong("timestamp") ?: 0L,
                            storyTitle    = doc.getString("storyTitle") ?: doc.getString("storyId") ?: "",
                            chapterNumber = (doc.getLong("chapterNumber") ?: 0).toInt()
                        )
                    }
                onSuccess(list)
            }
            .addOnFailureListener { e -> onError(e.message ?: "Không tải lịch sử") }
    }

    // ── Lịch sử rút tiền ──────────────────────────────────────────────────────

    fun getWithdrawalHistory(
        authorId: String,
        limit: Int = 30,
        onSuccess: (List<WalletTransaction>) -> Unit,
        onError: (String) -> Unit
    ) {
        db.collection("withdrawalRequests")
            .whereEqualTo("authorId", authorId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(limit.toLong())
            .get()
            .addOnSuccessListener { result ->
                val list = result.documents.map { doc ->
                    WalletTransaction(
                        id        = doc.id,
                        type      = "author_withdraw",
                        amount    = -(doc.getLong("coinAmount") ?: 0L),
                        timestamp = doc.getLong("timestamp") ?: 0L,
                        status    = doc.getString("status") ?: "pending",
                        note      = doc.getString("bankName") ?: ""
                    )
                }
                onSuccess(list)
            }
            .addOnFailureListener {
                db.collection("withdrawalRequests")
                    .whereEqualTo("authorId", authorId)
                    .get()
                    .addOnSuccessListener { result ->
                        val list = result.documents
                            .sortedByDescending { it.getLong("timestamp") ?: 0L }
                            .take(limit)
                            .map { doc ->
                                WalletTransaction(
                                    id        = doc.id,
                                    type      = "author_withdraw",
                                    amount    = -(doc.getLong("coinAmount") ?: 0L),
                                    timestamp = doc.getLong("timestamp") ?: 0L,
                                    status    = doc.getString("status") ?: "pending",
                                    note      = doc.getString("bankName") ?: ""
                                )
                            }
                        onSuccess(list)
                    }
                    .addOnFailureListener { e -> onError(e.message ?: "Không tải lịch sử rút") }
            }
    }

    /** Gộp thu + rút, sắp xếp theo thời gian */
    fun getCombinedHistory(
        authorId: String,
        onSuccess: (List<WalletTransaction>) -> Unit,
        onError: (String) -> Unit
    ) {
        var earnings = emptyList<WalletTransaction>()
        var withdrawals = emptyList<WalletTransaction>()
        var earningsDone = false
        var withdrawDone = false
        var errorMsg: String? = null

        fun tryFinish() {
            if (!earningsDone || !withdrawDone) return
            if (errorMsg != null) {
                onError(errorMsg!!)
                return
            }
            onSuccess(
                (earnings + withdrawals).sortedByDescending { it.timestamp }
            )
        }

        getEarningsHistory(authorId, 50,
            onSuccess = { earnings = it; earningsDone = true; tryFinish() },
            onError   = { errorMsg = it; earningsDone = true; tryFinish() }
        )
        getWithdrawalHistory(authorId, 30,
            onSuccess = { withdrawals = it; withdrawDone = true; tryFinish() },
            onError   = { if (errorMsg == null) errorMsg = it; withdrawDone = true; tryFinish() }
        )
    }

    // ── Rút tiền ──────────────────────────────────────────────────────────────

    /**
     * Yêu cầu rút coin → chuyển khoản ngân hàng (xử lý thủ công / admin sau).
     * Trừ pendingCoins ngay; admin duyệt qua collection withdrawalRequests.
     */
    fun requestWithdrawal(
        coinAmount: Long,
        bankName: String,
        accountNumber: String,
        accountHolder: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val authorId = UserSession.firebaseUid ?: run {
            onError("Chưa đăng nhập"); return
        }
        if (coinAmount < MIN_WITHDRAW_COINS) {
            onError("Số coin tối thiểu để rút là $MIN_WITHDRAW_COINS 🪙")
            return
        }
        if (bankName.isBlank() || accountNumber.isBlank() || accountHolder.isBlank()) {
            onError("Vui lòng điền đầy đủ thông tin ngân hàng")
            return
        }

        val authorRef = db.collection("authorRevenue").document(authorId)
        val withdrawRef = db.collection("withdrawalRequests").document()
        val txLogRef = db.collection("coinTransactions").document()

        db.runTransaction { tx: Transaction ->
            val snap = tx.get(authorRef)
            val pending = snap.getLong("pendingCoins") ?: 0L
            if (pending < coinAmount) {
                throw Exception("Số dư khả dụng không đủ (còn $pending 🪙)")
            }

            tx.set(
                authorRef,
                mapOf(
                    "authorId"       to authorId,
                    "pendingCoins"   to FieldValue.increment(-coinAmount),
                    "totalWithdrawn" to FieldValue.increment(coinAmount)
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )

            val vndAmount = coinAmount * VND_PER_COIN
            tx.set(withdrawRef, hashMapOf(
                "authorId"      to authorId,
                "coinAmount"    to coinAmount,
                "vndAmount"     to vndAmount,
                "bankName"      to bankName.trim(),
                "accountNumber" to accountNumber.trim(),
                "accountHolder" to accountHolder.trim(),
                "status"        to "pending",
                "timestamp"     to System.currentTimeMillis()
            ))

            tx.set(txLogRef, hashMapOf(
                "userId"    to authorId,
                "authorId"  to authorId,
                "type"      to "author_withdraw",
                "amount"    to -coinAmount,
                "vndAmount" to vndAmount,
                "status"    to "pending",
                "timestamp" to System.currentTimeMillis()
            ))
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Rút tiền thất bại") }
    }

    /** Lưu thông tin ngân hàng mặc định của tác giả */
    fun saveBankInfo(
        bankName: String,
        accountNumber: String,
        accountHolder: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val authorId = UserSession.firebaseUid ?: run { onError("Chưa đăng nhập"); return }
        db.collection("authorRevenue").document(authorId)
            .set(
                mapOf(
                    "bankName"      to bankName.trim(),
                    "accountNumber" to accountNumber.trim(),
                    "accountHolder" to accountHolder.trim()
                ),
                com.google.firebase.firestore.SetOptions.merge()
            )
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { onError(it.message ?: "Lưu thất bại") }
    }

    fun getSavedBankInfo(
        authorId: String,
        onResult: (bankName: String, accountNumber: String, accountHolder: String) -> Unit
    ) {
        db.collection("authorRevenue").document(authorId).get()
            .addOnSuccessListener { doc ->
                onResult(
                    doc.getString("bankName") ?: "",
                    doc.getString("accountNumber") ?: "",
                    doc.getString("accountHolder") ?: ""
                )
            }
    }
}
