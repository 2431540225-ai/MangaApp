package com.example.mangaapp.ui.coin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Space
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.R
import com.example.mangaapp.models.CoinPackage

/**
 * Adapter hiển thị danh sách gói coin dạng grid 2 cột.
 * Highlight gói có badge (Phổ biến / Tiết kiệm nhất) bằng viền màu.
 */
class CoinPackageAdapter(
    private val packages: List<CoinPackage>,
    private val onBuyClick: (CoinPackage) -> Unit
) : RecyclerView.Adapter<CoinPackageAdapter.VH>() {

    inner class VH(view: View) : RecyclerView.ViewHolder(view) {
        val card: CardView          = view.findViewById(R.id.card_package)
        val tvBadge: TextView       = view.findViewById(R.id.tv_pkg_badge)
        val badgePlaceholder: View  = view.findViewById(R.id.view_badge_placeholder)
        val tvCoins: TextView       = view.findViewById(R.id.tv_pkg_coins)
        val tvBonus: TextView       = view.findViewById(R.id.tv_pkg_bonus)
        val spaceNoBonus: Space     = view.findViewById(R.id.space_no_bonus)
        val tvPrice: TextView       = view.findViewById(R.id.tv_pkg_price)
        val btnBuy: Button          = view.findViewById(R.id.btn_pkg_buy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = VH(
        LayoutInflater.from(parent.context).inflate(R.layout.item_coin_package, parent, false)
    )

    override fun getItemCount() = packages.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val pkg = packages[position]

        // Số coin chính
        holder.tvCoins.text = "${pkg.coins}"

        // Bonus
        if (pkg.bonusCoins > 0) {
            holder.tvBonus.visibility = View.VISIBLE
            holder.tvBonus.text = "+${pkg.bonusCoins} bonus 🎁"
            holder.spaceNoBonus.visibility = View.GONE
        } else {
            holder.tvBonus.visibility = View.GONE
            holder.spaceNoBonus.visibility = View.VISIBLE
        }

        // Giá
        holder.tvPrice.text = pkg.priceLabel

        // Badge dải ngang trên cùng
        if (pkg.badge.isNotEmpty()) {
            holder.tvBadge.visibility = View.VISIBLE
            holder.badgePlaceholder.visibility = View.GONE
            holder.tvBadge.text = pkg.badge
            val badgeColor = if (pkg.badge.contains("Tiết kiệm")) "#FF6F00" else "#E53935"
            holder.tvBadge.setBackgroundColor(android.graphics.Color.parseColor(badgeColor))
            holder.card.cardElevation = 6f
        } else {
            holder.tvBadge.visibility = View.GONE
            holder.badgePlaceholder.visibility = View.VISIBLE
            holder.card.cardElevation = 3f
        }

        // Nút mua: hiển thị tổng coin nếu có bonus
        if (pkg.bonusCoins > 0) {
            holder.btnBuy.text = "Mua · ${pkg.totalCoins} coin"
        } else {
            holder.btnBuy.text = "Mua ngay"
        }

        holder.btnBuy.setOnClickListener { onBuyClick(pkg) }
        holder.card.setOnClickListener { onBuyClick(pkg) }
    }
}
