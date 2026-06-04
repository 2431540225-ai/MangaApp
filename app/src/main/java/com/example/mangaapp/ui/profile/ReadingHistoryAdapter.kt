package com.example.mangaapp.ui.profile

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mangaapp.R
import com.example.mangaapp.model.ReadingHistory
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class ReadingHistoryAdapter(
    private val onItemClick: (ReadingHistory) -> Unit,
    private val onDeleteClick: (ReadingHistory) -> Unit
) : RecyclerView.Adapter<ReadingHistoryAdapter.ViewHolder>() {

    private val items = mutableListOf<ReadingHistory>()

    fun submitList(newList: List<ReadingHistory>) {
        items.clear()
        items.addAll(newList)
        notifyDataSetChanged()
    }

    fun removeItem(storyId: String) {
        val index = items.indexOfFirst { it.storyId == storyId }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_reading_history, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imgCover: ImageView = itemView.findViewById(R.id.imgCover)
        private val tvTitle: TextView = itemView.findViewById(R.id.tvStoryTitle)
        private val tvAuthor: TextView = itemView.findViewById(R.id.tvAuthorName)
        private val tvChapter: TextView = itemView.findViewById(R.id.tvLastChapter)
        private val tvTime: TextView = itemView.findViewById(R.id.tvLastReadTime)
        private val btnDelete: ImageView = itemView.findViewById(R.id.btnDelete)

        fun bind(history: ReadingHistory) {
            tvTitle.text = history.storyTitle
            tvAuthor.text = "Tác giả: ${history.authorName}"
            tvChapter.text = "Đang đọc: ${history.lastChapterTitle}"
            tvTime.text = formatTime(history.lastReadTime)

            // Load ảnh bìa bằng Glide
            Glide.with(itemView.context)
                .load(history.storyCoverUrl)
                .placeholder(android.R.drawable.ic_menu_gallery) // Dùng ảnh gallery mặc định của máy
                .error(android.R.drawable.stat_notify_error)
                .centerCrop()
                .into(imgCover)

            // Click vào item => mở truyện, tiếp tục từ chương đang đọc
            itemView.setOnClickListener { onItemClick(history) }

            // Click nút xoá
            btnDelete.setOnClickListener { onDeleteClick(history) }
        }

        private fun formatTime(timestamp: Long): String {
            val now = System.currentTimeMillis()
            val diff = now - timestamp

            return when {
                diff < TimeUnit.MINUTES.toMillis(1) -> "Vừa xong"
                diff < TimeUnit.HOURS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toMinutes(diff)} phút trước"
                diff < TimeUnit.DAYS.toMillis(1) -> "${TimeUnit.MILLISECONDS.toHours(diff)} giờ trước"
                diff < TimeUnit.DAYS.toMillis(7) -> "${TimeUnit.MILLISECONDS.toDays(diff)} ngày trước"
                else -> {
                    val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    sdf.format(Date(timestamp))
                }
            }
        }
    }
}