package com.example.mangaapp.ui.list

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
import java.text.NumberFormat
import java.util.Locale

class MangaGridAdapter(
    private var items: List<Manga>,
    private val onClick: (Manga) -> Unit
) : RecyclerView.Adapter<MangaGridAdapter.GridViewHolder>() {

    inner class GridViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val ivCover: ImageView  = view.findViewById(R.id.iv_cover)
        val tvName: TextView    = view.findViewById(R.id.tv_name)
        val tvGenre: TextView   = view.findViewById(R.id.tv_genre)
        val tvViews: TextView   = view.findViewById(R.id.tv_views)
        val tvStatus: TextView  = view.findViewById(R.id.tv_status)
        val tvChapters: TextView = view.findViewById(R.id.tv_chapters)
        val tvPaid: TextView    = view.findViewById(R.id.tv_paid_badge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GridViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_manga_grid, parent, false)
        return GridViewHolder(view)
    }

    override fun onBindViewHolder(holder: GridViewHolder, position: Int) {
        val manga  = items[position]
        val format = NumberFormat.getNumberInstance(Locale("vi", "VN"))

        holder.tvName.text     = manga.name
        holder.tvGenre.text    = manga.genres.joinToString(" • ")
        holder.tvViews.text    = "👁 ${format.format(manga.totalViews)}"
        holder.tvChapters.text = "${manga.totalChapters} ch"

        // Trạng thái
        if (manga.status == MangaStatus.ONGOING) {
            holder.tvStatus.text = "Đang ra"
            holder.tvStatus.setBackgroundColor(
                holder.itemView.context.getColor(R.color.badge_new))
        } else {
            holder.tvStatus.text = "Hoàn thành"
            holder.tvStatus.setBackgroundColor(
                holder.itemView.context.getColor(R.color.badge_full))
        }

        // Badge VIP
        holder.tvPaid.visibility = if (manga.isPaid) View.VISIBLE else View.GONE

        Glide.with(holder.itemView.context)
            .load(manga.coverUrl)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .centerCrop()
            .into(holder.ivCover)

        holder.itemView.setOnClickListener { onClick(manga) }
    }

    override fun getItemCount() = items.size

    /** Cập nhật danh sách khi tìm kiếm hoặc lọc */
    fun updateList(newList: List<Manga>) {
        items = newList
        notifyDataSetChanged()
    }
}
