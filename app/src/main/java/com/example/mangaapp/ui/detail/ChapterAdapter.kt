package com.example.mangaapp.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.R
import com.example.mangaapp.models.Chapter

class ChapterAdapter(
    private var items: List<Chapter>,
    private val onClick: (Chapter) -> Unit
) : RecyclerView.Adapter<ChapterAdapter.ChapterViewHolder>() {

    inner class ChapterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvNumber: TextView = view.findViewById(R.id.tv_chapter_number)
        val tvTitle: TextView  = view.findViewById(R.id.tv_chapter_title)
        val tvDate: TextView   = view.findViewById(R.id.tv_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChapterViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_chapter, parent, false)
        return ChapterViewHolder(view)
    }

    override fun onBindViewHolder(holder: ChapterViewHolder, position: Int) {
        val chapter = items[position]
        holder.tvNumber.text = "Chương ${chapter.chapterNumber}"
        holder.tvTitle.text  = chapter.title
        holder.tvDate.text   = chapter.publishedAt.ifEmpty { "Mới cập nhật" }
        holder.itemView.setOnClickListener { onClick(chapter) }
    }

    override fun getItemCount() = items.size

    fun updateList(newList: List<Chapter>) {
        items = newList
        notifyDataSetChanged()
    }
}
