package com.example.mangaapp.models

/**
 * Một dòng trong lịch sử ví (độc giả hoặc tác giả).
 */
data class WalletTransaction(
    val id: String = "",
    val type: String = "",
    val amount: Long = 0,
    val timestamp: Long = 0,
    val storyTitle: String = "",
    val chapterNumber: Int = 0,
    val status: String = "",
    val note: String = ""
) {
    val isCredit: Boolean get() = amount > 0

    val typeLabel: String
        get() = when (type) {
            "top_up"           -> "Nạp coin"
            "unlock_chapter"   -> "Mở khóa chương"
            "author_earning"   -> "Hoa hồng chương"
            "author_withdraw"  -> "Rút tiền"
            else               -> type
        }
}
