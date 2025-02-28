package com.example.firedatabase_assis.home_page

import android.content.Context
import android.util.Log
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


    private lateinit var webSocketManager: CommentWebSocketManager

    var onCommentsListChanged: ((List<CommentDto>) -> Unit)? = null

    var commentsList: MutableList<CommentDto> = mutableListOf()
        set(value) {
            field = value
            onCommentsListChanged?.invoke(value)
        }

    init {
        commentsList = if (comments.isEmpty()) {
            mutableListOf()
        } else {
            comments.filter { it.parentCommentId == null }.toMutableList()
        }
        // Initialize with empty map to ensure UI elements are visible
        viewModel._replyCountsMap.value = commentsList.mapNotNull { comment ->
            comment.commentId?.let { id ->
                id to (viewModel._replyCountsMap.value[id] ?: 0)
            }
        }.toMap()
        if (commentsList.isNotEmpty()) {
            fetchReplyCountsForAllComments()
        }
        setupWebSocket()
    }


    private fun setupWebSocket() {
        webSocketManager = CommentWebSocketManager(
            BuildConfig.WEBSOCKET_URL,
            viewModel,
            coroutineScope
        )

        // Listen for new comments
        coroutineScope.launch {
            viewModel.lastReceivedComment.collect { update ->
                when (update) {
                    is CommentsViewModel.CommentUpdate.NewRoot -> {
                        addNewComment(update.comment)
                    }

                    is CommentsViewModel.CommentUpdate.NewReply -> {
                        if (viewModel.visibleReplySections.value.contains(update.parentId)) {
                            // If the parent's replies are visible, add the new reply
                            addNewComment(update.comment)
                        } else {
                            // Just update the reply count UI
                            val parentPosition = getPositionForComment(update.parentId)
                            if (parentPosition != -1) {
                                notifyItemChanged(parentPosition)
                            }
                        }
                    }

                    null -> {} // Initial state or reset
                }
            }
        }

        webSocketManager.connect()
    }


    private fun fetchReplyCountsForAllComments() {
        val commentIds = commentsList.mapNotNull { it.commentId }
        if (commentIds.isEmpty()) return

        coroutineScope.launch {
            try {
                // Fetch only the reply counts
                val counts = withContext(Dispatchers.IO) {
                    val retrofit = getRetrofitInstance()
                    val api = retrofit.create(Comments::class.java)
                    val response = api.getReplyCountsForComments(commentIds)
                    response.body()?.associate { it.parentId to it.replyCount } ?: emptyMap()
                }

                // Update the counts in the ViewModel
                val updatedCounts = viewModel._replyCountsMap.value.toMutableMap()
                counts.forEach { (id, count) ->
                    updatedCounts[id] = count
                }
                viewModel._replyCountsMap.value = updatedCounts

            } catch (e: Exception) {
                Log.e("CommentsAdapter", "Error fetching reply counts", e)
                // Use cached/default values on error
                val defaultCounts = commentIds.associateWith {
                    viewModel._replyCountsMap.value[it] ?: 0
                }
                viewModel._replyCountsMap.value = defaultCounts
            } finally {
                withContext(Dispatchers.Main) {
                    notifyDataSetChanged()
                }
            }
        }
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CommentViewHolder {
        val commentItemView = CommentItemView(parent.context).apply {
            layoutParams = RecyclerView.LayoutParams(
                RecyclerView.LayoutParams.MATCH_PARENT,
                RecyclerView.LayoutParams.WRAP_CONTENT
            ).apply {
                // Set default margins
                marginStart = 0
                marginEnd = 0
                topMargin = 4
                bottomMargin = 4
            }
        }
        return CommentViewHolder(commentItemView)
    }

    override fun onBindViewHolder(holder: CommentViewHolder, position: Int) {
        val comment = commentsList[position]
        val commentItemView = holder.itemView as CommentItemView

        // Set tag and data
        val isSelected = viewModel.selectedComments.value.contains(comment.commentId)
        commentItemView.setComment(
            comment = comment,
            isSelected = isSelected,
            onSelection = { commentId, selected ->
                viewModel.setCommentSelected(commentId, selected)
            }
        )

        // Handle selection state
        handleSelectionState(holder, comment)

        // Apply indentation for replies using new LayoutParams each time
        commentItemView.layoutParams = RecyclerView.LayoutParams(
            RecyclerView.LayoutParams.MATCH_PARENT,
            RecyclerView.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = if (comment.parentCommentId != null) {
                (40 * commentItemView.context.resources.displayMetrics.density).toInt()
            } else {
                0
            }
            marginEnd = 0
            topMargin = 4
            bottomMargin = 4
        }

        // Handle reply counts and visibility using binding
        if (comment.parentCommentId == null) {
            comment.commentId?.let { commentId ->
                val replyCount = viewModel.replyCountsMap.value[commentId] ?: 0
                val isVisible = viewModel.visibleReplySections.value.contains(commentId)

                commentItemView.binding.viewRepliesButton.apply {
                    visibility = if (replyCount > 0) View.VISIBLE else View.GONE
                    text = if (isVisible) {
                        "Hide Replies ($replyCount)"
                    } else {
                        "View Replies ($replyCount)"
                    }

                    if (isVisible && !isCommentAvailable(commentId)) {
                        val cachedReplies = viewModel.getCachedReplies(commentId)
                        if (cachedReplies != null) {
                            showRepliesInline(holder, commentId, cachedReplies)
                        }
                    }

                    setOnClickListener {
                        toggleRepliesVisibility(holder, comment)
                    }
                }
            }
        } else {
            commentItemView.binding.viewRepliesButton.visibility = View.GONE
        }

        // Handle reply button using binding
        commentItemView.binding.replyButton.apply {
            visibility = View.VISIBLE
            setOnClickListener {
                comment.commentId?.let { parentId ->
                    onReplyClicked(parentId)
                }
            }
        }
    }


    fun toggleRepliesVisibility(holder: CommentViewHolder, comment: CommentDto) {
        val commentId = comment.commentId ?: return
        val isVisible = viewModel.visibleReplySections.value.contains(commentId)

        if (isVisible) {
            hideReplies(commentId)
            viewModel.toggleReplySection(commentId, false)
            val replyCount = viewModel.replyCountsMap.value[commentId] ?: 0
            holder.viewRepliesButton.text = "View Replies ($replyCount)"
            // Update websocket subscriptions
            webSocketManager.updateSubscriptions(viewModel.visibleReplySections.value)
        } else {
            val cachedReplies = viewModel.getCachedReplies(commentId)
            if (cachedReplies != null && cachedReplies.isNotEmpty()) {
                showRepliesInline(holder, commentId, cachedReplies)
                viewModel.toggleReplySection(commentId, true)
                holder.viewRepliesButton.text = "Hide Replies (${cachedReplies.size})"
                // Update websocket subscriptions
                webSocketManager.updateSubscriptions(viewModel.visibleReplySections.value)
            } else {
                coroutineScope.launch {
                    try {
                        val replies = withContext(Dispatchers.IO) {
                            fetchRepliesForComment(commentId)
                        }

                        withContext(Dispatchers.Main) {
                            if (replies.isNotEmpty()) {
                                viewModel.initializeReplies(commentId, replies)
                                showRepliesInline(holder, commentId, replies)
                                viewModel.toggleReplySection(commentId, true)
                                // Update websocket subscriptions
                                webSocketManager.updateSubscriptions(viewModel.visibleReplySections.value)

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
        commentsList.addAll(parentPosition + 1, allReplies)
        notifyItemRangeInserted(parentPosition + 1, allReplies.size)
        holder.viewRepliesButton.text = "Hide Replies (${allReplies.size})"

        // Update UI for comments with replies
        notifyItemChanged(parentPosition)
    }


    fun updateComments(newComments: List<CommentDto>) {
        if (isNestedAdapter) {
            commentsList = newComments.filter { it.parentCommentId == parentId }.toMutableList()
        } else {
            commentsList = newComments.filter { it.parentCommentId == null }.toMutableList()
            fetchReplyCountsForAllComments()
        }
        notifyDataSetChanged()
    }

    private fun isCommentAvailable(commentId: Int): Boolean {
        // Check if comment exists in current list
        if (getPositionForComment(commentId) != -1) return true

        // Check if comment exists in any visible reply sections
        for (parentId in viewModel.visibleReplySections.value) {
            viewModel.getCachedReplies(parentId)?.let { replies ->
                if (replies.any { it.commentId == commentId }) return true
            }
        }
        return false
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

    private fun handleSelectionState(holder: CommentViewHolder, comment: CommentDto) {
        comment.commentId?.let { commentId ->
            val isSelected = viewModel.selectedComments.value.contains(commentId)
            (holder.itemView as CommentItemView).setSelected(isSelected)
        }
    }

    fun selectComment(commentId: Int) {
        viewModel.setCommentSelected(commentId, true)
        val position = getPositionForComment(commentId)
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    fun deselectComment(commentId: Int) {
        viewModel.setCommentSelected(commentId, false)
        val position = getPositionForComment(commentId)
        if (position != -1) {
            notifyItemChanged(position)
        }
    }

    override fun getItemCount(): Int = commentsList.size

    fun getPositionForComment(commentId: Int): Int {
        return commentsList.indexOfFirst { it.commentId == commentId }
    }

    fun addNewComment(comment: CommentDto) {
        when {
            comment.parentCommentId == null -> {
                // Add root comment at the top
                val newList = commentsList.toMutableList()
                newList.add(0, comment)
                commentsList = newList
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
                        val newList = commentsList.toMutableList()
                        val insertPosition = parentPosition + 1
                        newList.add(insertPosition, comment)
                        commentsList = newList
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
