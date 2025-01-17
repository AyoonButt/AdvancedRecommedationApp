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