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
import com.example.mangaapp.models.MangaStatus

class MangaCardAdapter(
    private val items: List<Manga>,
    private val onClick: (Manga) -> Unit
) : RecyclerView.Adapter<MangaCardAdapter.CardViewHolder>() {

    inner class CardViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView     = view.findViewById(R.id.iv_cover)
        val tvName: TextView       = view.findViewById(R.id.tv_manga_name)
        val tvChapters: TextView   = view.findViewById(R.id.tv_chapter_count)
        val tvBadge: TextView      = view.findViewById(R.id.tv_status_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CardViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manga_card, parent, false)
        return CardViewHolder(view)
    }

    override fun onBindViewHolder(holder: CardViewHolder, position: Int) {
        val manga = items[position]

        holder.tvName.text     = manga.name
        holder.tvChapters.text = "${manga.totalChapters} chương"

        // Badge trạng thái
        if (manga.status == MangaStatus.ONGOING) {
            holder.tvBadge.text = "Đang ra"
            holder.tvBadge.setBackgroundColor(
                holder.itemView.context.getColor(R.color.badge_new)
            )
        } else {
            holder.tvBadge.text = "Hoàn thành"
            holder.tvBadge.setBackgroundColor(
                holder.itemView.context.getColor(R.color.badge_full)
            )
        }

        Glide.with(holder.itemView.context)
            .load(manga.coverUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(holder.ivCover)

        holder.itemView.setOnClickListener { onClick(manga) }
    }

    override fun getItemCount() = items.size
}
