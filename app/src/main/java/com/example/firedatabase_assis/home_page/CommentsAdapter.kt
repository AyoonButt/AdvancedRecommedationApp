package com.example.firedatabase_assis.home_page

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.postgres.CommentEntity
import com.example.firedatabase_assis.postgres.Comments
import com.example.firedatabase_assis.postgres.PostEntity
import com.example.firedatabase_assis.postgres.Posts
import com.example.firedatabase_assis.postgres.ReplyRequest
import com.example.firedatabase_assis.postgres.UserEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class CommentsAdapter(
    private val context: Context,
    comments: List<CommentEntity>,
    private val postId: Int,
    private val currentUser: UserEntity?,
    private val onCommentAdded: (() -> Unit)? = null  // Add this callback
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    private var comments: MutableList<CommentEntity> = comments.toMutableList()


    init {
        Log.d("CommentsAdapter", "Initializing with ${comments.size} comments")
        Log.d("CommentsAdapter", "Current user: $currentUser")
        Log.d("CommentsAdapter", "Post ID: $postId")
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
            Log.d("addCommentToApi", "Sending comment to API: $comment")
            val response = api.addComment(comment)
            Log.d("addCommentToApi", "Response received: $response")

            if (response.isSuccessful) {
                Log.d("addCommentToApi", "Comment added successfully")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Comment added", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e(
                    "addCommentToApi",
                    "Failed to add comment. Response: ${response.errorBody()?.string()}"
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to add comment", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("addCommentToApi", "Network error occurred: ${e.message}", e)
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
            Log.d("addReplyToApi", "Sending reply to API: $replyRequest for commentId: $commentId")
            val response = api.addReply(userId, commentId, replyRequest)
            Log.d("addReplyToApi", "Response received: $response")

            if (response.isSuccessful) {
                Log.d("addReplyToApi", "Reply added successfully")
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Reply added", Toast.LENGTH_SHORT).show()
                }
                // Fetch the updated list of replies for the comment
                val updatedReplies = fetchRepliesForComment(commentId)
                Log.d("addReplyToApi", "Updated replies fetched: $updatedReplies")
                // Update the comment with the new list of replies
                withContext(Dispatchers.Main) {
                    updateRepliesForComment(commentId, updatedReplies)
                }
            } else {
                Log.e(
                    "addReplyToApi",
                    "Failed to add reply. Response: ${response.errorBody()?.string()}"
                )
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Failed to add reply", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            Log.e("addReplyToApi", "Network error occurred: ${e.message}", e)
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Network error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
            e.printStackTrace()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        Log.d("CommentsAdapter", "Creating new ViewHolder")
        val view = LayoutInflater.from(parent.context).inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view).also {
            Log.d("CommentsAdapter", "ViewHolder created successfully")
        }
    }


    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = comments[position]


        // Bind basic comment data
        holder.apply {
            username.text = comment.user.name
            content.text = comment.content

            // Reset view state
            commentInput.text.clear()
            repliesRecyclerView.visibility = View.GONE
            viewRepliesButton.visibility =
                if (comment.replies.isEmpty()) View.GONE else View.VISIBLE
        }

        // Handle comment/reply submission
        holder.addCommentButton.setOnClickListener {
            Log.d("CommentsAdapter", "Add Button Clicked")
            val replyText = holder.commentInput.text.toString().trim()


            if (replyText.isEmpty()) {
                Toast.makeText(context, "Reply cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (currentUser == null) {
                Toast.makeText(context, "Please log in to comment", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Launch coroutine for background task
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    if (comment.parentComment == null) {
                        handleNewComment(holder, currentUser, replyText)
                    } else {
                        handleReply(holder, currentUser, comment, replyText)
                    }
                } catch (e: Exception) {
                    // Handle error on the main thread
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                    Log.e("CommentsAdapter", "Error handling comment/reply", e)
                }
            }
        }

        // Handle replies visibility toggle
        holder.viewRepliesButton.setOnClickListener {
            val isRepliesVisible = holder.repliesRecyclerView.visibility == View.VISIBLE

            if (isRepliesVisible) {
                holder.repliesRecyclerView.visibility = View.GONE
            } else {
                // Launch coroutine for fetching replies
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        comment.commentId?.let { commentId ->
                            val replies = fetchRepliesForComment(commentId)

                            // Run UI update on the main thread
                            withContext(Dispatchers.Main) {
                                if (replies.isEmpty()) {
                                    Toast.makeText(
                                        context,
                                        "No replies to display",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    return@withContext
                                }

                                comment.replies = replies
                                // Set the adapter for repliesRecyclerView
                                holder.repliesRecyclerView.apply {
                                    // Set layout manager
                                    layoutManager = LinearLayoutManager(context)
                                    // Set adapter with replies
                                    adapter = CommentsAdapter(
                                        context,
                                        replies.toMutableList(),
                                        postId,
                                        currentUser
                                    )
                                    visibility = View.VISIBLE
                                }
                            }
                        }
                    } catch (e: Exception) {
                        // Handle error on the main thread
                        withContext(Dispatchers.Main) {
                            Toast.makeText(
                                context,
                                "Error loading replies: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        Log.e("CommentsAdapter", "Error loading replies", e)
                    }
                }
            }
        }
    }


    private suspend fun handleNewComment(
        holder: CommentViewHolder,
        currentUser: UserEntity,
        replyText: String
    ) {
        val postEntity = fetchPostEntityById(postId) ?: run {
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Failed to fetch post details", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val newComment = CommentEntity(
            commentId = 0,
            user = currentUser,
            post = postEntity,
            content = replyText,
            sentiment = "Neutral",
            timestamp = System.currentTimeMillis().toString(),
            parentComment = null,
            replies = listOf()
        )

        addCommentToApi(newComment)
        withContext(Dispatchers.Main) {
            holder.commentInput.text.clear()
            onCommentAdded?.invoke()  // Notify fragment to refresh
        }
    }

    private suspend fun handleReply(
        holder: CommentViewHolder,
        currentUser: UserEntity,
        parentComment: CommentEntity,
        replyText: String
    ) {
        currentUser.userId?.let { userId ->
            parentComment.commentId?.let { commentId ->
                addReplyToApi(userId, commentId, replyText)
                withContext(Dispatchers.Main) {
                    holder.commentInput.text.clear()
                    onCommentAdded?.invoke()  // Notify fragment to refresh
                }
            }
        }
    }


    fun updateComments(newComments: List<CommentEntity>) {
        comments.clear()
        comments.addAll(newComments)
        notifyDataSetChanged()
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


    override fun getItemCount(): Int {
        return comments.size
    }


    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.comment_author)
        val content: TextView = itemView.findViewById(R.id.comment_content)
        val commentInput: EditText = itemView.findViewById(R.id.comment_input)
        val addCommentButton: Button = itemView.findViewById(R.id.add_comment_button)
        val repliesRecyclerView: RecyclerView = itemView.findViewById(R.id.replies_recycler_view)
        val viewRepliesButton: Button = itemView.findViewById(R.id.view_replies_button)

        // Initialize the RecyclerView for replies
        init {
            repliesRecyclerView.layoutManager =
                LinearLayoutManager(context)
        }

    }
}




