package com.example.mangaapp.models

/**
 * Tổng quan ví doanh thu tác giả.
 * pendingCoins: số coin có thể rút (70% từ mỗi lần mở khóa chapter).
 */
data class AuthorRevenueSummary(
    val pendingCoins: Long = 0,
    val totalEarned: Long = 0,
    val totalWithdrawn: Long = 0,
    val authorSharePercent: Int = 70
) {
    val platformSharePercent: Int get() = 100 - authorSharePercent

    /** Quy đổi coin → VND (theo gói nạp: 50 coin ≈ 5.000đ) */
    fun coinsToVnd(coins: Long): Long = coins * 100

    val pendingVnd: Long get() = coinsToVnd(pendingCoins)
    val totalEarnedVnd: Long get() = coinsToVnd(totalEarned)
    val totalWithdrawnVnd: Long get() = coinsToVnd(totalWithdrawn)
}
