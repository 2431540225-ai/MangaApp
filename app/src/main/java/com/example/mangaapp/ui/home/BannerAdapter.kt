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

        Glide.with(holder.itemView.context)
            .load(manga.coverUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(holder.ivBanner)

        holder.itemView.setOnClickListener { onClick(manga) }
    }

    override fun getItemCount() = items.size
}
