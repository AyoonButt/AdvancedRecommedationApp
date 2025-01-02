package com.example.firedatabase_assis.postgres

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

data class GenreEntity(
    val genreId: Int? = null,
    val genreName: String = ""
)

data class SubscriptionProvider(
    val providerId: Int? = null,
    val providerName: String
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
    val userDto: UserDto,
    val subscriptions: List<Int>,
    val genres: List<Int>,
    val avoidGenres: List<Int>
)


data class UserUpdateRequest(
    val userDto: UserDto,
    val subscriptions: List<Int>,
    val genres: List<Int>,
    val avoidGenres: List<Int>
)


data class CommentDto(
    val commentId: Int?,
    val userId: Int,
    val username: String,
    val postId: Int,
    val content: String,
    val sentiment: String?,
    val timestamp: String?,
    val parentCommentId: Int? = null
)


data class ReplyDto(
    val postId: Int,
    val content: String,
    val sentiment: String? = null,
    val timestamp: String? = null
)


data class UserPostInteractionDto(
    val interactionId: Int?,
    val userId: Int,
    val postId: Int,
    val timeSpentOnPost: Long,
    val likeState: Boolean = false,
    val saveState: Boolean = false,
    val commentButtonPressed: Boolean = false,
    val commentMade: Boolean = false,
    val timestamp: String
)


data class PostDto(
    val postId: Int?,
    val tmdbId: Int,
    val type: String,
    val title: String,
    val subscription: String,
    val releaseDate: String?,
    val overview: String?,
    val posterPath: String?,
    val voteAverage: Double,
    val voteCount: Int,
    val originalLanguage: String?,
    val originalTitle: String?,
    val popularity: Double,
    val genreIds: String,
    val postLikeCount: Int = 0,
    val trailerLikeCount: Int = 0,
    val videoKey: String
)


data class TrailerInteractionDto(
    val interactionId: Int?,
    val userId: Int,
    val postId: Int,
    val timeSpent: Long,
    val replayCount: Int,
    val isMuted: Boolean,
    val likeState: Boolean,
    val saveState: Boolean,
    val commentButtonPressed: Boolean,
    val commentMade: Boolean,
    val timestamp: String
)


data class UserDto(
    val userId: Int?,
    val name: String,
    val username: String,
    val password: String,
    val email: String,
    val language: String,
    val region: String,
    val minMovie: Int?,
    val maxMovie: Int?,
    val minTV: Int?,
    val maxTV: Int?,
    val oldestDate: String,
    val recentDate: String,
    val createdAt: String,
    val recentLogin: String?
)


data class UserPreferencesDto(
    val userId: Int?,
    val language: String,
    val region: String,
    val minMovie: Int?,
    val maxMovie: Int?,
    val minTV: Int?,
    val maxTV: Int?,
    val oldestDate: String,
    val recentDate: String,
    val subscriptions: List<Int>,
    val genreIds: List<Int>,
    val avoidGenreIds: List<Int>
)


