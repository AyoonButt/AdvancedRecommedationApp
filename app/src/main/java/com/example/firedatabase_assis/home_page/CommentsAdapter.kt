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
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.login_setup.UserViewModel
import com.example.firedatabase_assis.postgres.CommentEntity
import com.example.firedatabase_assis.postgres.Comments
import com.example.firedatabase_assis.postgres.PostEntity
import com.example.firedatabase_assis.postgres.Posts
import com.example.firedatabase_assis.postgres.ReplyRequest
import com.example.firedatabase_assis.postgres.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CommentsAdapter(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private var comments: List<CommentEntity>,
    private val postId: Int, // Only postId is passed now, not PostEntity
    private val userViewModel: UserViewModel
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    private fun getUserFromViewModel(): UserEntity? {
        return userViewModel.currentUser.value // Return UserEntity object
    }

    private fun getRetrofitInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.POSTRGRES_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    private suspend fun fetchPostEntityById(postId: Int): PostEntity? {
        val retrofit = getRetrofitInstance()
        val api = retrofit.create(Posts::class.java)

        try {
            val response = api.fetchPostEntityById(postId)
            if (response.isSuccessful) {
                return response.body()
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to fetch post", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            e.printStackTrace()
        }
        return null
    }


    private suspend fun addCommentToApi(comment: CommentEntity) {
        val retrofit = getRetrofitInstance()
        val api = retrofit.create(Comments::class.java)

        try {
            val response = api.addComment(comment)
            if (response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show()
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to add comment", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            e.printStackTrace()
        }
    }

    private suspend fun addReplyToApi(userId: Int, commentId: Int, replyText: String) {
        val retrofit = getRetrofitInstance()
        val api = retrofit.create(Comments::class.java)
        val replyRequest = ReplyRequest(userId, replyText, sentiment = null)

        try {
            val response = api.addReply(userId, commentId, replyRequest)
            if (response.isSuccessful) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Reply added", Toast.LENGTH_SHORT).show()
                }
                // Fetch the updated list of replies for the comment
                val updatedReplies = fetchRepliesForComment(commentId)
                // Update the comment with the new list of replies
                withContext(Dispatchers.Main) {
                    updateRepliesForComment(commentId, updatedReplies)
                }
            } else {
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to add reply", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            e.printStackTrace()
        }
    }


    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]
        holder.username.text = comment.user.name
        holder.content.text = comment.content

        holder.addCommentButton.setOnClickListener {
            val replyText = holder.commentInput.text.toString().trim()
            val user = getUserFromViewModel()

            if (replyText.isNotEmpty() && user != null) {
                lifecycleOwner.lifecycleScope.launch {
                    if (comment.parentComment == null) {
                        // This is a root comment, so call addCommentToApi
                        val postEntity = fetchPostEntityById(postId)
                        if (postEntity != null) {
                            val newComment = CommentEntity(
                                commentId = 0, // ID will be auto-generated
                                user = user,
                                post = postEntity,
                                content = replyText,
                                sentiment = "Neutral", // Default value
                                timestamp = System.currentTimeMillis().toString(),
                                parentComment = null,
                                replies = listOf() // No replies initially
                            )
                            addCommentToApi(newComment)
                            holder.commentInput.text.clear()
                        } else {
                            Toast.makeText(
                                context,
                                "Failed to fetch post details",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        // This is a reply to an existing comment
                        user.userId?.let { userId ->
                            comment.commentId?.let { commentId ->
                                addReplyToApi(userId, commentId, replyText)
                                holder.commentInput.text.clear()
                            }
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Reply cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }

        if (comment.replies.isEmpty()) {
            holder.viewRepliesButton.visibility = View.GONE
        } else {
            holder.viewRepliesButton.visibility = View.VISIBLE
        }

        // Initially hide the repliesRecyclerView
        holder.repliesRecyclerView.visibility = View.GONE

        // Handle viewing replies
        holder.viewRepliesButton.setOnClickListener {
            lifecycleOwner.lifecycleScope.launch {
                // Toggle visibility on button click
                if (holder.repliesRecyclerView.visibility == View.GONE) {
                    comment.commentId?.let { commentId ->
                        // Fetch replies from the API if needed
                        val fetchedReplies = fetchRepliesForComment(commentId)
                        comment.replies = fetchedReplies // Update the replies in the comment object

                        // Display replies if any exist
                        if (comment.replies.isNotEmpty()) {
                            val replyAdapter = CommentsAdapter(
                                context,
                                lifecycleOwner,
                                comment.replies,
                                postId,
                                userViewModel
                            )
                            holder.repliesRecyclerView.adapter = replyAdapter
                            holder.repliesRecyclerView.visibility = View.VISIBLE
                        } else {
                            Toast.makeText(context, "No replies to display", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                } else {
                    // Hide replies if they are currently visible
                    holder.repliesRecyclerView.visibility = View.GONE
                }
            }
        }

    }

    private suspend fun fetchRepliesForComment(commentId: Int): List<CommentEntity> {
        val response = getRepliesForComment(commentId)
        return if (response.isSuccessful) {
            response.body() ?: emptyList()  // Return the replies or an empty list if null
        } else {
            emptyList()  // Return an empty list if the response was not successful
        }
    }


    private suspend fun getRepliesForComment(commentId: Int): Response<List<CommentEntity>> {
        val retrofit = getRetrofitInstance()
        val api = retrofit.create(Comments::class.java)
        return api.getAllReplies(commentId)
    }

    private fun updateRepliesForComment(commentId: Int, replies: List<CommentEntity>) {
        val comment = comments.find { it.commentId == commentId }
        comment?.replies = replies
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun getItemCount(): Int {
        return comments.size
    }

    fun updateComments(newComments: List<CommentEntity>) {
        comments = newComments
        notifyDataSetChanged()
    }

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.comment_author)
        val content: TextView = itemView.findViewById(R.id.comment_content)
        val commentInput: EditText = itemView.findViewById(R.id.comment_input)
        val addCommentButton: Button = itemView.findViewById(R.id.add_comment_button)
        val repliesRecyclerView: RecyclerView = itemView.findViewById(R.id.replies_recycler_view)
        val viewRepliesButton: Button = itemView.findViewById(R.id.view_replies_button)

        init {
            repliesRecyclerView.layoutManager = LinearLayoutManager(context)
        }
    }
}
