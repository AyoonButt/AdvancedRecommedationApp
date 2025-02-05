package com.example.firedatabase_assis.home_page

import com.example.firedatabase_assis.postgres.CommentDto

// ServerMessage.kt
sealed class ServerMessage {
    data class NewRootComment(val comment: CommentDto) : ServerMessage()
    data class NewReply(val comment: CommentDto, val parentId: Int) : ServerMessage()
    data class ReplyCountUpdate(val commentId: Int, val newCount: Int) : ServerMessage()
}

// ClientMessage.kt
sealed class ClientMessage {
    data class UpdateSubscription(val expandedSections: Set<Int>) : ClientMessage()
}