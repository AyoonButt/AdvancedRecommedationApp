package com.example.firedatabase_assis.home_page

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.database.CommentDao
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CommentsAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private var comments: List<Comment>,
    private val commentDao: CommentDao,
    private val postId: Int
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.username.text = comment.username
        holder.content.text = comment.content



        holder.addCommentButton.setOnClickListener {
            val commentText = holder.commentInput.text.toString().trim()
            if (commentText.isNotEmpty()) {
                val newComment = Comment(
                    postId = postId,
                    username = "User", // Replace "User" with actual username
                    content = commentText
                )
                lifecycleOwner.lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            commentDao.insertComment(newComment)
                        }
                        // Update the UI on the main thread
                        holder.commentInput.text.clear()
                        Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show()
                    } catch (e: Exception) {
                        // Handle exceptions
                        Toast.makeText(context, "Failed to add comment", Toast.LENGTH_SHORT).show()
                        e.printStackTrace()
                    }
                }
            } else {
                Toast.makeText(context, "Comment cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun updateComments(newComments: List<Comment>) {
        comments = newComments
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.comment_username)
        val content: TextView = itemView.findViewById(R.id.comment_content)
        var commentInput: EditText = itemView.findViewById(R.id.comment_input)
        var addCommentButton: Button = itemView.findViewById(R.id.add_comment_button)
    }
}
