package com.example.firedatabase_assis.home_page

import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.postgres.CommentDto
import com.example.firedatabase_assis.postgres.Comments
import com.example.firedatabase_assis.postgres.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CommentsAdapter(
    private val coroutineScope: CoroutineScope,
    private val context: Context,
    comments: List<CommentDto>,
    private val postId: Int,
    private val currentUser: UserEntity?,
    private val onReplyClicked: ((parentCommentId: Int) -> Unit)  // Callback to handle replies
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    private var commentsList: MutableList<CommentDto> = comments.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentsList[position]
        holder.username.text = comment.username
        holder.content.text = comment.content

        // Set the "replied to" user, if available
        // Fix the coroutine scope usage
        if (comment.parentCommentId != null) {
            coroutineScope.launch {  // Use the provided coroutineScope
                withContext(Dispatchers.IO) {  // Move network call to IO dispatcher
                    try {
                        val response = getRepliedTo(comment.parentCommentId)
                        withContext(Dispatchers.Main) {  // Switch to Main for UI updates
                            if (response.isSuccessful) {
                                val repliedToUsername = response.body()
                                if (repliedToUsername != null) {
                                    holder.repliedToUser.apply {
                                        visibility = View.VISIBLE
                                        text = "Replying to: $repliedToUsername"
                                    }
                                } else {
                                    holder.repliedToUser.visibility = View.GONE
                                }
                            } else {
                                holder.repliedToUser.visibility = View.GONE
                                Log.e(TAG, "Error: ${response.code()} - ${response.message()}")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            holder.repliedToUser.visibility = View.GONE
                            Log.e(TAG, "Failed to get replied to username: ${e.message}")
                        }
                    }
                }
            }
        }
        // Reply button click listener
        holder.replyButton.setOnClickListener {
            comment.commentId?.let { parentId ->
                onReplyClicked(parentId)
            }
        }

        // Handle replies visibility toggle
        holder.viewRepliesButton.setOnClickListener {
            toggleRepliesVisibility(holder, comment)
        }
    }

    private fun getRetrofitInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.POSTRGRES_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private fun toggleRepliesVisibility(holder: CommentViewHolder, comment: CommentDto) {
        val isRepliesVisible = holder.repliesRecyclerView.visibility == View.VISIBLE
        if (isRepliesVisible) {
            holder.repliesRecyclerView.visibility = View.GONE
        } else {
            coroutineScope.launch {  // Use the provided coroutineScope instead of creating new one
                withContext(Dispatchers.IO) {
                    try {
                        val replies = fetchRepliesForComment(comment.commentId!!)
                        withContext(Dispatchers.Main) {
                            if (replies.isEmpty()) {
                                Toast.makeText(context, "No replies found", Toast.LENGTH_SHORT)
                                    .show()
                            } else {
                                holder.repliesRecyclerView.apply {
                                    layoutManager = LinearLayoutManager(context)
                                    adapter = CommentsAdapter(
                                        coroutineScope,  // Pass the same coroutineScope
                                        context,
                                        replies,
                                        postId,
                                        currentUser,
                                        onReplyClicked
                                    )
                                    visibility = View.VISIBLE
                                }
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error loading replies", Toast.LENGTH_SHORT)
                                .show()
                            Log.e("CommentsAdapter", "Error fetching replies", e)
                        }
                    }
                }
            }
        }
    }

    fun updateComments(newComments: List<CommentDto?>) {
        commentsList.clear()
        commentsList.addAll(newComments)
    }

    private suspend fun fetchRepliesForComment(commentId: Int): List<CommentDto> {
        val response = getRepliesForComment(commentId)
        return if (response.isSuccessful) {
            response.body() ?: emptyList()
        } else {
            emptyList()
        }
    }

    private suspend fun getRepliesForComment(commentId: Int): Response<List<CommentDto>> {
        val retrofit = getRetrofitInstance()
        val api = retrofit.create(Comments::class.java)
        return api.getAllReplies(commentId)
    }

    private suspend fun getRepliedTo(commentId: Int): Response<String> {
        val retrofit = getRetrofitInstance()
        val api = retrofit.create(Comments::class.java)
        return api.getParentCommentUsername(commentId)
    }


    override fun getItemCount(): Int {
        return commentsList.size
    }

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.comment_author)
        val content: TextView = itemView.findViewById(R.id.comment_content)
        val repliedToUser: TextView =
            itemView.findViewById(R.id.replied_to_user)  // "Replying to" text
        val repliesRecyclerView: RecyclerView = itemView.findViewById(R.id.replies_recycler_view)
        val viewRepliesButton: Button = itemView.findViewById(R.id.view_replies_button)
        val replyButton: TextView = itemView.findViewById(R.id.reply_button)

        init {
            repliesRecyclerView.layoutManager = LinearLayoutManager(context)
        }
    }
}

private fun <E> MutableList<E>.addAll(elements: List<E?>) {

}
