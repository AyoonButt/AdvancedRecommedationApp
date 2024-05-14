package com.example.firedatabase_assis

data class Comment(
    val author: String,
    val content: String
)


data class Post(
    val title: String,
    val content: String,
    val author: String,
    val community: String,
    var upvotes: Int,
    var downvotes: Int,
    var upvoted: Boolean = false,
    var downvoted: Boolean = false,
    val comments: MutableList<Comment> = mutableListOf()
)
