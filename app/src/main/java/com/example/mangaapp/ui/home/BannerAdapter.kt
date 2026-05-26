package com.example.mangaapp.ui.home

import android.graphics.LinearGradient
import android.graphics.Shader
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangaapp.R
import com.example.mangaapp.models.Manga

class BannerAdapter(
    private val items: List<Manga>,
    private val onClick: (Manga) -> Unit
) : RecyclerView.Adapter<BannerAdapter.BannerViewHolder>() {

    inner class BannerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivBanner: ImageView = view.findViewById(R.id.iv_banner)
        val tvName: TextView    = view.findViewById(R.id.tv_banner_name)
        val tvGenre: TextView   = view.findViewById(R.id.tv_banner_genre)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BannerViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_banner, parent, false)
        return BannerViewHolder(view)
    }

    override fun onBindViewHolder(holder: BannerViewHolder, position: Int) {
        val manga = items[position]

        holder.tvName.text  = manga.name
        holder.tvGenre.text = manga.genres.joinToString(" • ")

        // Apply gradient shader to title text
        holder.tvName.post {
            val width = holder.tvName.width.toFloat()
            if (width > 0f) {
                val colorStart = holder.itemView.context.getColor(R.color.primary)      // #A78BFA
                val colorEnd   = holder.itemView.context.getColor(R.color.gradient_end) // #60A5FA
                holder.tvName.paint.shader = LinearGradient(
                    0f, 0f, width, 0f,
                    colorStart, colorEnd,
                    Shader.TileMode.CLAMP
                )
                holder.tvName.invalidate()
            }
        }

        Glide.with(holder.itemView.context)
            .load(manga.coverUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(holder.ivBanner)

        holder.itemView.setOnClickListener { onClick(manga) }
    }

    override fun getItemCount() = items.size
}