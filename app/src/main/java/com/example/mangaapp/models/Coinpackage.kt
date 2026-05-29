package com.example.mangaapp.models

/**
 * Gói coin mà người dùng có thể mua.
 * Dữ liệu lưu trong Firestore collection "coinPackages".
 */
data class CoinPackage(
    val id: String = "",
    /** Số coin nhận được */
    val coins: Int = 0,
    /** Số coin bonus (khuyến mãi) */
    val bonusCoins: Int = 0,
    /** Giá hiển thị, ví dụ: "10.000đ" */
    val priceLabel: String = "",
    /** Giá tính bằng VND (dùng để xử lý payment) */
    val priceVnd: Long = 0L,
    /** Nhãn đặc biệt, ví dụ: "Phổ biến", "Tiết kiệm nhất" */
    val badge: String = ""
) {
    /** Tổng coin thực nhận = coins + bonusCoins */
    val totalCoins: Int get() = coins + bonusCoins
}