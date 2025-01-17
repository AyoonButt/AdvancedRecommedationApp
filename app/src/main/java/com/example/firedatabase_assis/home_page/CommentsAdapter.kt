package com.example.firedatabase_assis.home_page

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.R
import com.example.firedatabase_assis.postgres.ApiResponse
import com.example.firedatabase_assis.postgres.CommentDto
import com.example.firedatabase_assis.postgres.Comments
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
    private val onReplyClicked: ((parentCommentId: Int) -> Unit),
    private val isNestedAdapter: Boolean = false,
    private val parentId: Int? = null,
    private val viewModel: CommentsViewModel
) : RecyclerView.Adapter<CommentsAdapter.CommentViewHolder>() {

    private var commentsList: MutableList<CommentDto> = mutableListOf()

    init {
        commentsList = if (comments.isEmpty()) {
            mutableListOf()
        } else {
            comments.filter { it.parentCommentId == null }.toMutableList()
        }
        // Initialize empty reply counts map
        viewModel._replyCountsMap.value = emptyMap()
        // Only fetch counts if we have comments
        if (commentsList.isNotEmpty()) {
            fetchReplyCountsForAllComments()
        }
    }

    private fun fetchReplyCountsForAllComments() {
        val commentIds = commentsList.mapNotNull { it.commentId }
        if (commentIds.isEmpty()) {
            viewModel._replyCountsMap.value = emptyMap()
            return
        }

        coroutineScope.launch {
            try {
                // First fetch counts
                val counts = withContext(Dispatchers.IO) {
                    val retrofit = getRetrofitInstance()
                    val api = retrofit.create(Comments::class.java)
                    val response = api.getReplyCountsForComments(commentIds)
                    response.body()?.associate { it.parentId to it.replyCount } ?: emptyMap()
                }

                Log.d("CommentsAdapter", "Fetched reply counts: $counts")

                // For comments with replies, fetch and cache their replies
                counts.forEach { (commentId, replyCount) ->
                    if (replyCount > 0) {
                        withContext(Dispatchers.IO) {
                            val replies = fetchRepliesForComment(commentId)
                            withContext(Dispatchers.Main) {
                                viewModel.initializeReplies(commentId, replies)
                            }
                        }
                    }
                }

                // Update counts in ViewModel
                viewModel._replyCountsMap.value = counts

                withContext(Dispatchers.Main) {
                    notifyDataSetChanged()
                }
            } catch (e: Exception) {
                Log.e("CommentsAdapter", "Error fetching reply counts", e)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.comment_item, parent, false)
        return CommentViewHolder(view)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentsList[position]
        holder.username.text = comment.username
        holder.content.text = comment.content

        // Apply indentation for replies using marginStart
        (holder.itemView.layoutParams as? ViewGroup.MarginLayoutParams)?.apply {
            if (comment.parentCommentId != null) {
                // Apply 50dp margin for replies (matching your original layout)
                marginStart = (50 * context.resources.displayMetrics.density).toInt()
            } else {
                // Reset margin for root comments
                marginStart = 0
            }
        }

        // Handle reply counts and visibility for parent comments
        if (comment.parentCommentId == null) {
            comment.commentId?.let { commentId ->
                val replyCount = viewModel.replyCountsMap.value[commentId] ?: 0

                if (replyCount > 0) {
                    holder.viewRepliesButton.apply {
                        visibility = View.VISIBLE
                        text = if (viewModel.visibleReplySections.value.contains(commentId)) {
                            "Hide Replies ($replyCount)"
                        } else {
                            "View Replies ($replyCount)"
                        }
                        setOnClickListener {
                            toggleRepliesVisibility(holder, comment)
                        }
                    }
                } else {
                    holder.viewRepliesButton.visibility = View.GONE
                }
            }
        } else {
            holder.viewRepliesButton.visibility = View.GONE
        }

        // Handle reply button visibility
        holder.replyButton.apply {
            visibility = View.VISIBLE
            Log.d("CommentsAdapter", "Setting up reply button for comment ID: ${comment.commentId}")
            setOnClickListener {
                Log.d(
                    "CommentsAdapter",
                    "Reply button clicked for comment ID: ${comment.commentId}"
                )
                comment.commentId?.let { parentId ->
                    Log.d("CommentsAdapter", "Calling onReplyClicked with parentId: $parentId")
                    onReplyClicked(parentId)
                } ?: Log.e("CommentsAdapter", "Comment ID is null when reply clicked")
            }
        }

        // Handle "replied to" username display
        if (comment.parentCommentId != null) {
            coroutineScope.launch {
                try {
                    val response = withContext(Dispatchers.IO) {
                        getRepliedTo(comment.parentCommentId)
                    }
                    if (response.isSuccessful && response.body()?.success == true) {
                        withContext(Dispatchers.Main) {
                            holder.repliedToUser.apply {
                                visibility = View.VISIBLE
                                text = "Replying to @${response.body()?.message}"
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
                    }
                }
            }
        } else {
            holder.repliedToUser.visibility = View.GONE
        }
    }

    private fun toggleRepliesVisibility(holder: CommentViewHolder, comment: CommentDto) {
        val commentId = comment.commentId ?: return
        val isVisible = viewModel.visibleReplySections.value.contains(commentId)

        if (isVisible) {
            // Hide replies
            hideReplies(commentId)
            viewModel.toggleReplySection(commentId, false)
            val replyCount = viewModel.replyCountsMap.value[commentId] ?: 0
            holder.viewRepliesButton.text = "View Replies ($replyCount)"
        } else {
            // Show replies
            val cachedReplies = viewModel.getCachedReplies(commentId)
            if (cachedReplies != null && cachedReplies.isNotEmpty()) {
                // Use cached replies
                showRepliesInline(holder, commentId, cachedReplies)
                viewModel.toggleReplySection(commentId, true)
                holder.viewRepliesButton.text = "Hide Replies (${cachedReplies.size})"
            } else {
                // Fetch replies if not cached
                coroutineScope.launch {
                    try {
                        val replies = withContext(Dispatchers.IO) {
                            fetchRepliesForComment(commentId)
                        }

                        withContext(Dispatchers.Main) {
                            if (replies.isNotEmpty()) {
                                // Cache the fetched replies
                                viewModel.initializeReplies(commentId, replies)
                                showRepliesInline(holder, commentId, replies)
                                viewModel.toggleReplySection(commentId, true)

                                // Update reply count
                                val currentCounts = viewModel._replyCountsMap.value.toMutableMap()
                                currentCounts[commentId] = replies.size
                                viewModel._replyCountsMap.value = currentCounts

                                holder.viewRepliesButton.text = "Hide Replies (${replies.size})"
                            } else {
                                holder.viewRepliesButton.visibility = View.GONE
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error loading replies", Toast.LENGTH_SHORT)
                                .show()
                        }
                    }
                }
            }
        }
    }

    private fun hideReplies(parentId: Int) {
        val position = getPositionForComment(parentId)
        if (position == -1) return

        // Find all replies to remove
        val toRemove = mutableListOf<Int>()
        var currentPosition = position + 1

        // Keep tracking replies until we hit a root comment or end of list
        while (currentPosition < commentsList.size) {
            val comment = commentsList[currentPosition]
            if (comment.parentCommentId == null) {
                // Stop at next root comment
                break
            }
            // Add any comment in the reply chain
            toRemove.add(currentPosition)
            currentPosition++
        }

        // Remove replies from the end to avoid index shifting
        for (i in toRemove.reversed()) {
            commentsList.removeAt(i)
        }

        // Ensure visibility state is properly updated
        viewModel.toggleReplySection(parentId, false)

        if (toRemove.isNotEmpty()) {
            notifyItemRangeRemoved(toRemove.first(), toRemove.size)
        }
    }

    private fun showRepliesInline(
        holder: CommentViewHolder,
        commentId: Int,
        replies: List<CommentDto>
    ) {
        val parentPosition = getPositionForComment(commentId)
        if (parentPosition == -1) return

        // Get all replies in the chain and show them
        val allReplies = mutableListOf<CommentDto>()
        val repliesQueue = ArrayDeque(replies)

        while (repliesQueue.isNotEmpty()) {
            val reply = repliesQueue.removeFirst()
            allReplies.add(reply)

            reply.commentId?.let { replyId ->
                viewModel.getCachedReplies(replyId)?.let { nestedReplies ->
                    repliesQueue.addAll(nestedReplies)
                }
            }
        }

        // Update the reply counts
        viewModel.updateReplyCountsRecursively(commentId)

        // Insert all replies immediately after the parent comment
        val sortedReplies = allReplies.sortedByDescending { it.timestamp }
        commentsList.addAll(parentPosition + 1, sortedReplies)
        notifyItemRangeInserted(parentPosition + 1, sortedReplies.size)
        holder.viewRepliesButton.text = "Hide Replies (${allReplies.size})"

        // Update UI for comments with replies
        notifyItemChanged(parentPosition)
    }


    fun updateComments(newComments: List<CommentDto>) {
        Log.d(
            "CommentsAdapter",
            "Updating comments. New size: ${newComments.size}, isNested: $isNestedAdapter"
        )
        commentsList.clear()
        if (isNestedAdapter) {
            commentsList.addAll(newComments.filter { it.parentCommentId == parentId })
        } else {
            commentsList.addAll(newComments.filter { it.parentCommentId == null })
            // Fetch reply counts for all comments after updating
            fetchReplyCountsForAllComments()
        }
        notifyDataSetChanged()
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

    private suspend fun getRepliedTo(commentId: Int): Response<ApiResponse> {
        val retrofit = getRetrofitInstance()
        val api = retrofit.create(Comments::class.java)
        return api.getParentCommentUsername(commentId)
    }

    private fun getRetrofitInstance(): Retrofit {
        return Retrofit.Builder()
            .baseUrl(BuildConfig.POSTRGRES_API_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    override fun getItemCount(): Int = commentsList.size

    fun getPositionForComment(commentId: Int): Int {
        return commentsList.indexOfFirst { it.commentId == commentId }
    }

    fun addNewComment(comment: CommentDto) {
        when {
            comment.parentCommentId == null -> {
                // Add root comment at the top
                commentsList.add(0, comment)
                notifyItemInserted(0)

                // Initialize empty reply count for new comment
                val currentCounts = viewModel._replyCountsMap.value.toMutableMap()
                comment.commentId?.let { commentId ->
                    currentCounts[commentId] = 0
                    viewModel._replyCountsMap.value = currentCounts
                }
            }

            else -> {
                val parentId = comment.parentCommentId
                val parentPosition = commentsList.indexOfFirst { it.commentId == parentId }

                if (parentPosition != -1) {
                    // Only update UI if replies are visible
                    if (viewModel.visibleReplySections.value.contains(parentId)) {
                        val insertPosition = parentPosition + 1
                        commentsList.add(insertPosition, comment)
                        notifyItemInserted(insertPosition)
                    }

                    // Update counts and UI immediately
                    val totalCount = viewModel.updateReplyCountsRecursively(parentId)

                    // Update parent comment UI to ensure count is refreshed
                    notifyItemChanged(parentPosition)

                    // Find and update root parent if this is a nested reply
                    val rootParentId = findRootParentId(parentId)
                    if (rootParentId != parentId) {
                        viewModel.updateReplyCountsRecursively(rootParentId)
                        val rootPosition =
                            commentsList.indexOfFirst { it.commentId == rootParentId }
                        if (rootPosition != -1) {
                            notifyItemChanged(rootPosition)
                        }
                    }
                }
            }
        }
    }

    private fun findRootParentId(commentId: Int): Int {
        var currentId = commentId
        var currentComment = commentsList.find { it.commentId == currentId }

        while (currentComment?.parentCommentId != null) {
            currentId = currentComment.parentCommentId!!
            currentComment = commentsList.find { it.commentId == currentId }
        }

        return currentId
    }

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.comment_author)
        val content: TextView = itemView.findViewById(R.id.comment_content)
        val repliedToUser: TextView = itemView.findViewById(R.id.replied_to_user)
        val viewRepliesButton: TextView = itemView.findViewById(R.id.view_replies_button)
        val replyButton: TextView = itemView.findViewById(R.id.reply_button)
    }
}