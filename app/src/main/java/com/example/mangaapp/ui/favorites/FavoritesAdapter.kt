package com.example.mangaapp.ui.favorites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangaapp.R
import com.example.mangaapp.models.Manga
import com.example.mangaapp.models.MangaStatus
import java.text.NumberFormat
import java.util.Locale

class FavoritesAdapter(
    private var items: List<Manga>,
    private val onClick: (Manga) -> Unit,
    private val onRemove: (Manga) -> Unit
) : RecyclerView.Adapter<FavoritesAdapter.FavViewHolder>() {

    inner class FavViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView     = view.findViewById(R.id.iv_cover)
        val tvName: TextView       = view.findViewById(R.id.tv_name)
        val tvGenre: TextView      = view.findViewById(R.id.tv_genre)
        val tvViews: TextView      = view.findViewById(R.id.tv_views)
        val tvStatus: TextView     = view.findViewById(R.id.tv_status)
        val tvChapters: TextView   = view.findViewById(R.id.tv_chapters)
        val tvPaid: TextView       = view.findViewById(R.id.tv_paid_badge)
        val btnRemove: ImageButton = view.findViewById(R.id.btn_remove_favorite)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FavViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_favorite_manga, parent, false)
        return FavViewHolder(view)
    }

    override fun onBindViewHolder(holder: FavViewHolder, position: Int) {
        val manga  = items[position]
        val format = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        holder.tvName.text     = manga.name
        holder.tvGenre.text    = manga.genres.joinToString(" • ")
        holder.tvViews.text    = "👁 ${format.format(manga.totalViews)}"
        holder.tvChapters.text = "${manga.totalChapters} chương"

        if (manga.status == MangaStatus.ONGOING) {
            holder.tvStatus.text = "Đang ra"
            holder.tvStatus.setBackgroundColor(
                holder.itemView.context.getColor(R.color.badge_new))
        } else {
            holder.tvStatus.text = "Hoàn thành"
            holder.tvStatus.setBackgroundColor(
                holder.itemView.context.getColor(R.color.badge_full))
        }

        holder.tvPaid.visibility = if (manga.isPaid) View.VISIBLE else View.GONE

        Glide.with(holder.itemView.context)
            .load(manga.coverUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(holder.ivCover)

        holder.itemView.setOnClickListener { onClick(manga) }
        holder.btnRemove.setOnClickListener { onRemove(manga) }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<Manga>) {
        items = newList
        notifyDataSetChanged()
    }
}
