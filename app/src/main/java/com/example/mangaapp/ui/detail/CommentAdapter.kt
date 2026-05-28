package com.example.mangaapp.ui.detail

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mangaapp.R
import com.example.mangaapp.models.Comment
import java.text.SimpleDateFormat
import java.util.*

class CommentAdapter(
    private val comments: MutableList<Comment>
) : RecyclerView.Adapter<CommentAdapter.ViewHolder>() {

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvAvatar  : TextView = view.findViewById(R.id.tv_avatar)
        val tvUserName: TextView = view.findViewById(R.id.tv_comment_username)
        val tvContent : TextView = view.findViewById(R.id.tv_comment_content)
        val tvTime    : TextView = view.findViewById(R.id.tv_comment_time)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_comment, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val comment = comments[position]
        holder.tvUserName.text = comment.userName
        holder.tvContent.text  = comment.content
        holder.tvTime.text     = formatTime(comment.timestamp)
        holder.tvAvatar.text   = comment.userName.first().uppercaseChar().toString()
    }

    override fun getItemCount() = comments.size

    fun addComment(comment: Comment) {
        comments.add(0, comment)
        notifyItemInserted(0)
    }

    private fun formatTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("vi", "VN"))
        return sdf.format(Date(timestamp))
    }
}
