package com.example.firedatabase_assis.adapters

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
import com.example.firedatabase_assis.database.Comments
import com.example.firedatabase_assis.workers.Comment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.transactions.transaction

class CommentsAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private var comments: List<Comment>,
    private val postId: Int
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    private fun getUserIdFromPreferences(): Int {
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        return sharedPreferences.getInt("userId", 0) // Default to 0 if userId is not found
    }

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
                val userId = getUserIdFromPreferences()
                val newComment = Comment(
                    commentId = 0, // ID will be auto-generated
                    postId = postId,
                    userId = userId, // Use the retrieved userId
                    username = "User", // Replace "User" with actual username
                    content = commentText,
                    sentiment = "Neutral" // Default value; update if needed
                )
                lifecycleOwner.lifecycleScope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            // Insert new comment into the database
                            transaction {
                                Comments.insert {
                                    it[postId] = newComment.postId
                                    it[Comments.userId] = newComment.userId
                                    it[username] = newComment.username
                                    it[content] = newComment.content
                                    it[sentiment] = newComment.sentiment
                                }
                            }
                        }
                        // Update the UI on the main thread
                        withContext(Dispatchers.Main) {
                            holder.commentInput.text.clear()
                            Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show()
                            // Optionally refresh the comments list here
                        }
                    } catch (e: Exception) {
                        // Handle exceptions
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to add comment", Toast.LENGTH_SHORT)
                                .show()
                        }
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
        val commentInput: EditText = itemView.findViewById(R.id.comment_input)
        val addCommentButton: Button = itemView.findViewById(R.id.add_comment_button)
    }
}
