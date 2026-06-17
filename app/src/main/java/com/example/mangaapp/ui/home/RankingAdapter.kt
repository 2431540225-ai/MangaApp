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

class RankingAdapter(
    private val items: List<Manga>,
    private val onClick: (Manga) -> Unit
) : RecyclerView.Adapter<RankingAdapter.RankViewHolder>() {
    // Số lượng hiển thị tối đa, null = hiện tất cả
    private var limitCount: Int? = 5
    /** Gọi hàm này khi user bấm "Xem Tất Cả" để bỏ giới hạn */
    fun showAll() {
        limitCount = null
        notifyDataSetChanged()
    }
    /** Số item thực sự hiển thị (có thể bị giới hạn) */
    private fun displayCount(): Int {
        val limit = limitCount ?: return items.size
        return minOf(limit, items.size)
    }
    inner class RankViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvRank: TextView     = view.findViewById(R.id.tv_rank)
        val ivCover: ImageView   = view.findViewById(R.id.iv_cover)
        val tvName: TextView     = view.findViewById(R.id.tv_manga_name)
        val tvChapters: TextView = view.findViewById(R.id.tv_chapter_count)
        val tvAuthor: TextView   = view.findViewById(R.id.tv_author)
        val tvViews: TextView    = view.findViewById(R.id.tv_views)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RankViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_ranking, parent, false)
        return RankViewHolder(view)
    }

    override fun onBindViewHolder(holder: RankViewHolder, position: Int) {
        val manga = items[position]
        val rank  = position + 1

        holder.tvRank.text = rank.toString()
        holder.tvRank.setTextColor(
            androidx.core.content.ContextCompat.getColor(
                holder.itemView.context, R.color.color_text_primary
            )
        )

        holder.tvName.text     = manga.name
        holder.tvChapters.text = "${manga.totalChapters}+ chapters"
        holder.tvAuthor.text   = manga.author
        holder.tvViews.text    = ""

        Glide.with(holder.itemView.context)
            .load(manga.coverUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(holder.ivCover)

        holder.itemView.setOnClickListener { onClick(manga) }
    }

    override fun getItemCount() = displayCount()
}