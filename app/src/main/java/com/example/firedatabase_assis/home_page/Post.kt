package com.example.firedatabase_assis.home_page

import androidx.room.Entity
import androidx.room.PrimaryKey


data class Post(
    val id: Int,
    val title: String,
    val releaseDate: String,
    val overview: String,
    val posterPath: String,
    val voteAverage: Double,
    val voteCount: Int,
    val backdropPath: String,
    val genreIds: String,
    val originalLanguage: String,
    val originalTitle: String,
    val popularity: Double,
)

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val postId: Int,
    val username: String,
    val content: String
)