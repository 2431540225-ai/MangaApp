package com.example.mangaapp.ui.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangaapp.R
import com.example.mangaapp.models.Manga
import java.text.NumberFormat
import java.util.Locale

class RankingAdapter(
    private val items: List<Manga>,
    private val onClick: (Manga) -> Unit
) : RecyclerView.Adapter<RankingAdapter.RankViewHolder>() {

    inner class RankViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView    = view.findViewById(R.id.tv_rank)
        val ivCover: ImageView  = view.findViewById(R.id.iv_cover)
        val tvName: TextView    = view.findViewById(R.id.tv_manga_name)
        val tvAuthor: TextView  = view.findViewById(R.id.tv_author)
        val tvViews: TextView   = view.findViewById(R.id.tv_views)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ranking, parent, false)
        return RankViewHolder(view)
    }

    override fun onBindViewHolder(holder: RankViewHolder, position: Int) {
        val manga  = items[position]
        val rank   = position + 1
        val format = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        holder.tvRank.text   = rank.toString()
        holder.tvName.text   = manga.name
        holder.tvAuthor.text = manga.author
        holder.tvViews.text  = "👁 ${format.format(manga.totalViews)} lượt xem"

        // Màu top 3
        val rankColor = when (rank) {
            1 -> "#FFB300"  // Vàng
            2 -> "#9E9E9E"  // Bạc
            3 -> "#795548"  // Đồng
            else -> null
        }
        rankColor?.let {
            holder.tvRank.setBackgroundColor(
                android.graphics.Color.parseColor(it)
            )
        }

        Glide.with(holder.itemView.context)
            .load(manga.coverUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(holder.ivCover)

        holder.itemView.setOnClickListener { onClick(manga) }
    }

    override fun getItemCount() = items.size.coerceAtMost(10)
}
