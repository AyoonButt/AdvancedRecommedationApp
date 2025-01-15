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
            // Initialize empty state
            viewModel._replyCountsMap.value = emptyMap()
            return
        }

        coroutineScope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    val retrofit = getRetrofitInstance()
                    val api = retrofit.create(Comments::class.java)
                    api.getReplyCountsForComments(commentIds)
                }

                if (response.isSuccessful) {
                    val counts =
                        response.body()?.associate { it.parentId to it.replyCount } ?: emptyMap()
                    Log.d("CommentsAdapter", "Fetched reply counts: $counts")

                    // Reset all counts in ViewModel
                    viewModel._replyCountsMap.value = counts

                    // Fetch and cache replies for comments that have them
                    withContext(Dispatchers.IO) {
                        counts.forEach { (commentId, count) ->
                            if (count > 0) {
                                val replies = fetchRepliesForComment(commentId)
                                // Sort replies by newest first before caching
                                val sortedReplies = replies.sortedByDescending { it.timestamp }
                                viewModel.initializeReplies(commentId, sortedReplies)
                            }
                        }
                    }

                    withContext(Dispatchers.Main) {
                        notifyDataSetChanged()
                    }
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
            setOnClickListener {
                comment.commentId?.let { parentId ->
                    onReplyClicked(parentId)
                }
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
            holder.viewRepliesButton.text =
                "View Replies (${viewModel.replyCountsMap.value[commentId] ?: 0})"
        } else {
            // Show replies
            val cachedReplies = viewModel.getCachedReplies(commentId)
            if (cachedReplies != null && cachedReplies.isNotEmpty()) {
                // Use cached replies
                showRepliesInline(holder, commentId, cachedReplies)
                viewModel.toggleReplySection(commentId, true)
            } else {
                // Fetch replies if not cached
                coroutineScope.launch {
                    try {
                        val replies = withContext(Dispatchers.IO) {
                            fetchRepliesForComment(commentId)
                        }

                        // Cache the fetched replies
                        viewModel.initializeReplies(commentId, replies)

                        withContext(Dispatchers.Main) {
                            if (replies.isNotEmpty()) {
                                showRepliesInline(holder, commentId, replies)
                                viewModel.toggleReplySection(commentId, true)
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

        // Insert all replies immediately after the parent comment
        val sortedReplies = replies.sortedByDescending { it.timestamp } // Sort by newest first
        commentsList.addAll(parentPosition + 1, sortedReplies)
        notifyItemRangeInserted(parentPosition + 1, replies.size)
        holder.viewRepliesButton.text = "Hide Replies (${replies.size})"
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
                // Find the root parent of this reply chain
                var rootParentId: Int = comment.parentCommentId
                var currentComment = commentsList.find { it.commentId == rootParentId }

                // Track the original parent ID before traversing up the chain
                val immediateParentId = rootParentId

                while (currentComment?.parentCommentId != null) {
                    rootParentId = currentComment.parentCommentId!!
                    currentComment = commentsList.find { it.commentId == rootParentId }
                }

                // Find immediate parent's position
                val parentPosition = commentsList.indexOfFirst { it.commentId == immediateParentId }
                if (parentPosition != -1) {
                    // Only add to UI if the parent's replies are visible
                    if (viewModel.visibleReplySections.value.contains(rootParentId)) {
                        val insertPosition = parentPosition + 1
                        commentsList.add(insertPosition, comment)
                        notifyItemInserted(insertPosition)
                    }

                    // Always update caches regardless of visibility
                    if (rootParentId != immediateParentId) {
                        val rootReplies = viewModel.getCachedReplies(rootParentId) ?: listOf()
                        viewModel.initializeReplies(rootParentId, listOf(comment) + rootReplies)
                    }

                    val parentReplies = viewModel.getCachedReplies(immediateParentId) ?: listOf()
                    viewModel.initializeReplies(immediateParentId, listOf(comment) + parentReplies)

                    // Update count in ViewModel
                    val currentCounts = viewModel._replyCountsMap.value.toMutableMap()
                    currentCounts[rootParentId] =
                        (viewModel.getCachedReplies(rootParentId)?.size ?: 0)
                    viewModel._replyCountsMap.value = currentCounts

                    // Update UI
                    notifyItemChanged(parentPosition)
                    if (rootParentId != immediateParentId) {
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

    inner class CommentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val username: TextView = itemView.findViewById(R.id.comment_author)
        val content: TextView = itemView.findViewById(R.id.comment_content)
        val repliedToUser: TextView = itemView.findViewById(R.id.replied_to_user)
        val viewRepliesButton: TextView = itemView.findViewById(R.id.view_replies_button)
        val replyButton: TextView = itemView.findViewById(R.id.reply_button)
    }
}