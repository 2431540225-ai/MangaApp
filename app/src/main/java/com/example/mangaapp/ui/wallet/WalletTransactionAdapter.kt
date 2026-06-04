package com.example.mangaapp.ui.wallet

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.R
import com.example.mangaapp.models.WalletTransaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Adapter lịch sử giao dịch dùng chung cho ví độc giả và ví tác giả.
 * - Hiển thị icon theo loại giao dịch
 * - Màu xanh/đỏ cho credit/debit
 * - Badge trạng thái cho yêu cầu rút tiền
 */
class WalletTransactionAdapter(
    private var items: List<WalletTransaction> = emptyList(),
    private val forAuthor: Boolean = false
) : RecyclerView.Adapter<WalletTransactionAdapter.VH>() {

    private val dateFmt = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi"))

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val tvIcon: TextView     = view.findViewById(R.id.tv_tx_icon)
        val tvTitle: TextView    = view.findViewById(R.id.tv_tx_title)
        val tvSubtitle: TextView = view.findViewById(R.id.tv_tx_subtitle)
        val tvAmount: TextView   = view.findViewById(R.id.tv_tx_amount)
        val tvDate: TextView     = view.findViewById(R.id.tv_tx_date)
        val tvStatus: TextView   = view.findViewById(R.id.tv_tx_status)
    }

    fun submitList(list: List<WalletTransaction>) {
        items = list
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_wallet_transaction, parent, false)
    )

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val tx = items[position]
        val ctx = holder.itemView.context

        // ── Icon ─────────────────────────────────────────────────
        holder.tvIcon.text = iconFor(tx.type)

        // ── Tiêu đề & mô tả ──────────────────────────────────────
        holder.tvTitle.text = buildTitle(tx)
        val subtitle = buildSubtitle(tx)
        if (subtitle.isNotEmpty()) {
            holder.tvSubtitle.visibility = View.VISIBLE
            holder.tvSubtitle.text = subtitle
        } else {
            holder.tvSubtitle.visibility = View.GONE
        }

        // ── Ngày giờ ─────────────────────────────────────────────
        holder.tvDate.text = if (tx.timestamp > 0) dateFmt.format(Date(tx.timestamp)) else ""

        // ── Số coin (+ hoặc -) ───────────────────────────────────
        val sign = if (tx.amount >= 0) "+" else ""
        holder.tvAmount.text = "$sign${tx.amount} 🪙"
        holder.tvAmount.setTextColor(
            if (tx.amount >= 0) Color.parseColor("#10B981") else Color.parseColor("#EF4444")
        )

        // ── Badge trạng thái (chỉ cho rút tiền) ──────────────────
        if (tx.type == "author_withdraw") {
            holder.tvStatus.visibility = View.VISIBLE
            val (label, color) = statusInfo(tx.status)
            holder.tvStatus.text = label
            holder.tvStatus.setTextColor(Color.parseColor(color))
        } else {
            holder.tvStatus.visibility = View.GONE
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private fun iconFor(type: String) = when (type) {
        "top_up"          -> "💳"
        "unlock_chapter"  -> "🔓"
        "author_earning"  -> "💰"
        "author_withdraw" -> "🏦"
        else              -> "🪙"
    }

    private fun buildTitle(tx: WalletTransaction) = when (tx.type) {
        "top_up"          -> "Nạp coin"
        "unlock_chapter"  -> {
            val ch = if (tx.chapterNumber > 0) "Chương ${tx.chapterNumber}" else "Chapter"
            "Mở khóa $ch"
        }
        "author_earning"  -> {
            val ch = if (tx.chapterNumber > 0) " · Ch.${tx.chapterNumber}" else ""
            "Hoa hồng$ch"
        }
        "author_withdraw" -> "Yêu cầu rút tiền"
        else              -> tx.typeLabel
    }

    private fun buildSubtitle(tx: WalletTransaction): String {
        return when (tx.type) {
            "author_withdraw" -> {
                if (tx.note.isNotEmpty()) tx.note else ""
            }
            "unlock_chapter"  -> {
                if (tx.storyTitle.isNotEmpty()) tx.storyTitle else ""
            }
            "author_earning"  -> {
                val story = if (forAuthor && tx.storyTitle.isNotEmpty()) tx.storyTitle else ""
                val note  = if (tx.note.isNotEmpty()) tx.note else ""
                listOf(story, note).filter { it.isNotEmpty() }.joinToString(" · ")
            }
            "top_up"          -> if (tx.note.isNotEmpty()) tx.note else ""
            else              -> ""
        }
    }

    private fun statusInfo(status: String): Pair<String, String> = when (status) {
        "pending"  -> "⏳ Đang chờ duyệt" to "#F59E0B"
        "approved" -> "✅ Đã chuyển khoản" to "#10B981"
        "rejected" -> "❌ Bị từ chối" to "#EF4444"
        else       -> status to "#888888"
    }
}
