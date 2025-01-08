package com.example.firedatabase_assis.home_page

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
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
    private val onReplyClicked: ((parentCommentId: Int) -> Unit),
    private val isNestedAdapter: Boolean = false
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    private var commentsList: MutableList<CommentDto> = mutableListOf()

    init {
        // Only show comments appropriate for this adapter level
        commentsList = if (isNestedAdapter) {
            comments.toMutableList()
        } else {
            // For root level, only show comments without parents
            comments.filter { it.parentCommentId == null }.toMutableList()
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        Log.d("CommentsAdapter", "onCreateViewHolder called")
        val view = LayoutInflater.from(context).inflate(R.layout.comment_item, parent, false)
        Log.d("CommentsAdapter", "View inflated: ${view != null}")


        return CommentViewHolder(view).also {
            Log.d("CommentsAdapter", "ViewHolder created successfully")
        }
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentsList[position]
        holder.username.text = comment.username
        holder.content.text = comment.content

        if (comment.parentCommentId != null) {
            coroutineScope.launch {
                withContext(Dispatchers.IO) {
                    try {
                        val response = getRepliedTo(comment.parentCommentId)
                        if (response.isSuccessful) {
                            val parentUsername = response.body()
                            withContext(Dispatchers.Main) {
                                holder.repliedToUser.apply {
                                    visibility = View.VISIBLE
                                    text = "Replying to @$parentUsername"
                                }
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                holder.repliedToUser.visibility = View.GONE
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            holder.repliedToUser.visibility = View.GONE
                            Log.e("CommentsAdapter", "Error fetching replied to username", e)
                        }
                    }
                }
            }
        } else {
            holder.repliedToUser.visibility = View.GONE
        }

        // Set up reply button
        holder.replyButton.setOnClickListener {
            comment.commentId?.let { parentId ->
                onReplyClicked(parentId)
                (holder.itemView.context as? FragmentActivity)?.supportFragmentManager?.findFragmentById(
                    R.id.fragment_container
                )?.let { fragment ->
                    if (fragment is CommentFragment) {
                        fragment.showKeyboardForReply(parentId)
                    }
                }
            }
        }

        // Only check for replies on root comments
        if (!isNestedAdapter && comment.parentCommentId == null) {
            comment.commentId?.let { commentId ->
                // Check for replies existence
                coroutineScope.launch {
                    withContext(Dispatchers.IO) {
                        try {
                            val replies = fetchRepliesForComment(commentId)
                            withContext(Dispatchers.Main) {
                                if (replies.isNotEmpty()) {
                                    holder.viewRepliesButton.apply {
                                        visibility = View.VISIBLE
                                        text = "View Replies"
                                        setOnClickListener {
                                            toggleRepliesVisibility(holder, comment)
                                        }
                                    }
                                } else {
                                    holder.viewRepliesButton.visibility = View.GONE
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                holder.viewRepliesButton.visibility = View.GONE
                                Log.e("CommentsAdapter", "Error checking replies", e)
                            }
                        }
                    }
                }
            }
        } else {
            // Hide view replies button for nested comments
            holder.viewRepliesButton.visibility = View.GONE
            holder.repliesRecyclerView.visibility = View.GONE
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
            // Hide replies
            holder.repliesRecyclerView.visibility = View.GONE
            holder.viewRepliesButton.text = "View Replies"
        } else {
            // Load replies
            coroutineScope.launch {
                try {
                    // Fetch replies on IO dispatcher
                    val replies = withContext(Dispatchers.IO) {
                        fetchRepliesForComment(comment.commentId!!)
                    }

                    // Update UI on Main dispatcher
                    if (replies.isEmpty()) {
                        holder.viewRepliesButton.visibility = View.GONE
                    } else {
                        holder.repliesRecyclerView.apply {
                            layoutManager = LinearLayoutManager(context)
                            adapter = CommentsAdapter(
                                coroutineScope,
                                context,
                                replies,
                                postId,
                                currentUser,
                                onReplyClicked,
                                isNestedAdapter = true
                            )
                            visibility = View.VISIBLE
                        }

                        // Change text to "Hide Replies" AFTER replies are loaded
                        holder.viewRepliesButton.text = "Hide Replies"
                    }
                } catch (e: Exception) {
                    // Handle errors on Main dispatcher
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Error loading replies", Toast.LENGTH_SHORT).show()
                        Log.e("CommentsAdapter", "Error fetching replies", e)

                        // Optionally, reset the button text if loading fails
                        holder.viewRepliesButton.text = "View Replies"
                    }
                }
            }
        }
    }
    

    fun updateComments(newComments: List<CommentDto>) {
        commentsList.clear()
        if (isNestedAdapter) {
            commentsList.addAll(newComments)
        } else {
            commentsList.addAll(newComments.filter { it.parentCommentId == null })
        }
        notifyDataSetChanged()
    }

    private suspend fun fetchRepliesForComment(commentId: Int): List<CommentDto> {
        val response = getRepliesForComment(commentId)
        return if (response.isSuccessful) {
            response.body() ?: mutableListOf()
        } else {
            mutableListOf()
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


    override fun getItemCount(): Int = commentsList.size

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.comment_author)
        val content: TextView = itemView.findViewById(R.id.comment_content)
        val repliedToUser: TextView = itemView.findViewById(R.id.replied_to_user)
        val repliesRecyclerView: RecyclerView = itemView.findViewById(R.id.replies_recycler_view)
        val viewRepliesButton: TextView =
            itemView.findViewById(R.id.view_replies_button) // Changed to TextView
        val replyButton: TextView = itemView.findViewById(R.id.reply_button)

        init {
            repliesRecyclerView.layoutManager = LinearLayoutManager(context)
        }
    }
}




