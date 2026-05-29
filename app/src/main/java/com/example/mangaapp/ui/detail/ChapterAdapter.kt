package com.example.mangaapp.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.R
import com.example.mangaapp.models.Chapter
import com.example.mangaapp.utils.UserSession

class ChapterAdapter(
    private var items: List<Chapter>,
    private val onClick: (Chapter) -> Unit
) : RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

    inner class ChapterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumber: TextView  = view.findViewById(R.id.tv_chapter_number)
        val tvTitle: TextView   = view.findViewById(R.id.tv_chapter_title)
        val tvDate: TextView    = view.findViewById(R.id.tv_date)
        val ivLock: ImageView   = view.findViewById(R.id.iv_lock_icon)
        val tvCoinPrice: TextView = view.findViewById(R.id.tv_coin_price)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chapter, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = items[position]
        val context = holder.itemView.context

        holder.tvNumber.text = "Chương ${chapter.chapterNumber}"
        holder.tvTitle.text  = chapter.title.ifEmpty { "" }
        holder.tvDate.text   = chapter.publishedAt.ifEmpty { "Mới cập nhật" }

        val user      = UserSession.currentUser
        val isUnlocked = chapter.isFree ||
                user?.hasUnlocked("", chapter.chapterNumber) == true

        if (chapter.isFree) {
            // Chapter miễn phí
            holder.ivLock.visibility    = View.GONE
            holder.tvCoinPrice.visibility = View.GONE
            holder.tvNumber.setTextColor(ContextCompat.getColor(context, R.color.light_text_primary))
        } else {
            val alreadyUnlocked = user?.hasUnlocked(
                // storyId sẽ được inject qua setStoryId()
                currentStoryId,
                chapter.chapterNumber
            ) == true

            if (alreadyUnlocked) {
                // Đã mở khóa → không hiện lock nữa
                holder.ivLock.visibility    = View.GONE
                holder.tvCoinPrice.visibility = View.GONE
                holder.tvNumber.setTextColor(ContextCompat.getColor(context, R.color.light_text_primary))
            } else {
                // Chưa mở khóa
                holder.ivLock.visibility    = View.VISIBLE
                holder.tvCoinPrice.visibility = View.VISIBLE
                holder.tvCoinPrice.text     = "${chapter.coinPrice} 🪙"
                holder.tvNumber.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
            }
        }

        holder.itemView.setOnClickListener { onClick(chapter) }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<Chapter>) {
        items = newList
        notifyDataSetChanged()
    }

    // Adapter cần biết storyId để check unlock
    private var currentStoryId: String = ""
    fun setStoryId(id: String) { currentStoryId = id }
}