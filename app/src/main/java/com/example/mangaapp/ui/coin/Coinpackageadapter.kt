package com.example.mangaapp.ui.coin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.R
import com.example.mangaapp.models.CoinPackage

class CoinPackageAdapter(
    private val items: List<CoinPackage>,
    private val onBuy: (CoinPackage) -> Unit
) : RecyclerView.Adapter<CoinPackageAdapter.PackageViewHolder>() {

    inner class PackageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvCoins: TextView   = view.findViewById(R.id.tv_pkg_coins)
        val tvBonus: TextView   = view.findViewById(R.id.tv_pkg_bonus)
        val tvPrice: TextView   = view.findViewById(R.id.tv_pkg_price)
        val tvBadge: TextView   = view.findViewById(R.id.tv_pkg_badge)
        val btnBuy: Button      = view.findViewById(R.id.btn_pkg_buy)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = PackageViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_coin_package, parent, false)
    )

    override fun onBindViewHolder(holder: PackageViewHolder, position: Int) {
        val pkg = items[position]
        holder.tvCoins.text = "${pkg.coins} 🪙"
        holder.tvPrice.text = pkg.priceLabel
        holder.btnBuy.setOnClickListener { onBuy(pkg) }

        if (pkg.bonusCoins > 0) {
            holder.tvBonus.visibility = View.VISIBLE
            holder.tvBonus.text       = "+${pkg.bonusCoins} bonus"
        } else {
            holder.tvBonus.visibility = View.GONE
        }

        if (pkg.badge.isNotEmpty()) {
            holder.tvBadge.visibility = View.VISIBLE
            holder.tvBadge.text       = pkg.badge
        } else {
            holder.tvBadge.visibility = View.GONE
        }
    }

    override fun getItemCount() = items.size
}