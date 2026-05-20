package com.example.mangaapp.ui.read

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangaapp.R

class PageAdapter(
    private val imageUrls: List<String>
) : RecyclerView.Adapter<PageAdapter.PageViewHolder>() {

    inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivPage: ImageView = view.findViewById(R.id.iv_page)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_page, parent, false)
        return PageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
        Glide.with(holder.itemView.context)
            .load(imageUrls[position])
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.ivPage)
    }

    override fun getItemCount() = imageUrls.size
}
