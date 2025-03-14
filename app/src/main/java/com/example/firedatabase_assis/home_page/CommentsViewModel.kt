package com.example.firedatabase_assis.home_page

import androidx.lifecycle.ViewModel
import com.example.firedatabase_assis.postgres.CommentDto
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CommentsViewModel : ViewModel() {
    private val _visibleReplySections = MutableStateFlow<Set<Int>>(emptySet())
    val visibleReplySections: StateFlow<Set<Int>> = _visibleReplySections.asStateFlow()

    private val repliesCache = mutableMapOf<Int, MutableList<CommentDto>>()

    val _replyCountsMap = MutableStateFlow<Map<Int, Int>>(emptyMap())
    val replyCountsMap: StateFlow<Map<Int, Int>> = _replyCountsMap.asStateFlow()

    private val _lastReceivedComment = MutableStateFlow<CommentUpdate?>(null)
    val lastReceivedComment: StateFlow<CommentUpdate?> = _lastReceivedComment.asStateFlow()

    private val _selectedComments = MutableStateFlow<Set<Int>>(emptySet())
    val selectedComments: StateFlow<Set<Int>> = _selectedComments.asStateFlow()


    sealed class CommentUpdate {
        data class NewRoot(val comment: CommentDto) : CommentUpdate()
        data class NewReply(val comment: CommentDto, val parentId: Int) : CommentUpdate()
    }


    fun setCommentSelected(commentId: Int, selected: Boolean) {
        val currentSelected = _selectedComments.value.toMutableSet()
        if (selected) {
            currentSelected.add(commentId)
        } else {
            currentSelected.remove(commentId)
        }
        _selectedComments.value = currentSelected
    }

    fun clearSelections() {
        _selectedComments.value = emptySet()
    }

    fun addNewRootComment(comment: CommentDto) {
        comment.commentId?.let { commentId ->
            val counts = _replyCountsMap.value.toMutableMap()
            counts[commentId] = 0
            _replyCountsMap.value = counts

            // Notify about new root comment
            _lastReceivedComment.value = CommentUpdate.NewRoot(comment)
        }
    }

    fun addNewReply(comment: CommentDto) {
        comment.parentCommentId?.let { parentId ->
            // Update reply count
            val counts = _replyCountsMap.value.toMutableMap()
            val currentCount = counts[parentId] ?: 0
            counts[parentId] = currentCount + 1
            _replyCountsMap.value = counts

            // Notify about new reply
            _lastReceivedComment.value = CommentUpdate.NewReply(comment, parentId)
        }
    }

    fun toggleReplySection(commentId: Int, visible: Boolean) {
        val currentSet = _visibleReplySections.value.toMutableSet()
        if (visible) {
            currentSet.add(commentId)
        } else {
            currentSet.remove(commentId)
        }
        _visibleReplySections.value = currentSet
    }

    fun addCommentToCache(parentId: Int, comment: CommentDto) {
        // Add to cache
        val currentReplies = repliesCache.getOrPut(parentId) { mutableListOf() }
        currentReplies.add(comment)

        // Update count
        val currentCounts = _replyCountsMap.value.toMutableMap()
        currentCounts[parentId] = currentReplies.size
        _replyCountsMap.value = currentCounts

        // Make visible
        toggleReplySection(parentId, true)
    }

    // In ViewModel
    fun updateReplyCountsRecursively(commentId: Int): Int {
        val currentCounts = _replyCountsMap.value.toMutableMap()

        // Get total count including nested replies
        fun getTotalReplies(id: Int): Int {
            val directReplies = getCachedReplies(id) ?: return 0
            var total = directReplies.size

            directReplies.forEach { reply ->
                reply.commentId?.let { replyId ->
                    total += getTotalReplies(replyId)
                }
            }
            return total
        }

        val totalCount = getTotalReplies(commentId)
        currentCounts[commentId] = totalCount
        _replyCountsMap.value = currentCounts
        return totalCount
    }

    fun initializeReplies(parentId: Int, replies: List<CommentDto>) {
        repliesCache[parentId] = replies.toMutableList()

        val currentCounts = _replyCountsMap.value.toMutableMap()
        currentCounts[parentId] = replies.size
        _replyCountsMap.value = currentCounts
    }

    fun getCachedReplies(parentId: Int): List<CommentDto>? = repliesCache[parentId]
    fun clearCache() {
        repliesCache.clear()
        _replyCountsMap.value = emptyMap()
        _visibleReplySections.value = emptySet()
    }
}