package com.example.firedatabase_assis.postgres

import retrofit2.Response

data class UserEntity(
    val userId: Int? = null,
    var name: String,
    var username: String,
    var email: String,
    var password: String,
    var language: String,
    var region: String,
    var minMovie: Int,
    var maxMovie: Int,
    var minTV: Int,
    var maxTV: Int,
    var oldestDate: String,
    var recentDate: String,
    var recentLogin: String,
    val createdAt: String
)


data class PostEntity(
    val postId: Int? = null,
    val tmdbId: Int = 0,
    var postLikeCount: Int = 0,
    val trailerLikeCount: Int = 0,
    val type: String = "",
    val title: String = "",
    val subscription: String = "",
    val releaseDate: String = "",
    val overview: String = "",
    val posterPath: String = "",
    val voteAverage: Double = 0.0,
    val voteCount: Int = 0,
    val originalLanguage: String = "",
    val originalTitle: String = "",
    val popularity: Double = 0.0,
    val genreIds: String = "",
    val videoKey: String = ""
)


data class UserPostInteraction(
    val interactionId: Int? = null,
    val user: UserEntity,
    val post: PostEntity,
    val timeSpentOnPost: Long,
    val likeState: Boolean = false,
    val saveState: Boolean = false,
    val commentButtonPressed: Boolean = false,
    val commentMade: Boolean = false,
    var timestamp: String
)

data class UserPostInteractionDTO(
    val interactionId: Int = 0,
    val userId: Int,  // Instead of full user object
    val postId: Int,  // Instead of full post object
    val likeState: Boolean,
    val saveState: Boolean,
    val commentButtonPressed: Boolean,
    val commentMade: Boolean,
    val timestamp: String,
    val timeSpentOnPost: Long
)

data class UserTrailerInteraction(
    val interactionId: Int? = null,
    val user: UserEntity,
    val post: Response<PostEntity?>,
    val timeSpent: Long,
    val replayCount: Int? = null,
    val likeState: Boolean,
    val saveState: Boolean,
    val isMuted: Boolean,
    val commentButtonPressed: Boolean,
    val commentMade: Boolean,
    var timestamp: String
)


data class CommentEntity(
    val commentId: Int? = null,
    val user: UserEntity,
    val post: PostEntity,
    val content: String,
    val sentiment: String? = null,
    val timestamp: String? = null,
    val parentComment: CommentEntity? = null,
    var replies: List<CommentEntity> = listOf()
)


data class GenreEntity(
    val genreId: Int? = null,
    val genreName: String = ""
)

data class SubscriptionProvider(
    val providerId: Int? = null,
    val providerName: String
)

data class UserInfo(
    val userId: Int,
    val name: String,
    val username: String,
    val email: String,
    val language: String,
    val region: String,
    val minMovie: Int,
    val maxMovie: Int,
    val minTV: Int,
    val maxTV: Int,
    val oldestDate: String?,
    val recentDate: String?,
    val createdAt: String?,
    var subscriptions: List<Int> = listOf(),
    var genres: List<Int> = listOf(),
    var avoidGenres: List<Int> = listOf()
)

data class ReplyRequest(
    val postId: Int,
    val content: String,
    val sentiment: String? = null
)

data class UserParams(
    val language: String,
    val region: String,
    val minMovie: Int,
    val maxMovie: Int,
    val minTV: Int,
    val maxTV: Int,
    val oldestDate: String,
    val recentDate: String
)

data class UserRequest(
    val user: UserEntity,
    val subscriptions: List<Int>,
    val genres: List<Int>,
    var avoidGenres: List<Int>
)


data class UserUpdateRequest(
    val userData: UserEntity,  // The user entity with updated information
    val subscriptions: List<Int>,  // List of subscription provider IDs
    val genres: List<Int>,  // List of genre IDs
    val avoidGenres: List<Int>  // List of avoided genre IDs
)

