package com.example.firedatabase_assis.postgres

import android.os.Parcelable
import com.google.gson.annotations.SerializedName
import kotlinx.parcelize.Parcelize

data class UserEntity(
    @SerializedName("userId") val userId: Int,
    @SerializedName("name") var name: String,
    @SerializedName("username") var username: String,
    @SerializedName("email") var email: String,
    @SerializedName("password") var password: String,
    @SerializedName("language") var language: String,
    @SerializedName("region") var region: String,
    @SerializedName("minMovie") var minMovie: Int,
    @SerializedName("maxMovie") var maxMovie: Int,
    @SerializedName("minTV") var minTv: Int,
    @SerializedName("maxTV") var maxTv: Int,
    @SerializedName("oldestDate") var oldestDate: String,
    @SerializedName("recentDate") var recentDate: String,
    @SerializedName("recentLogin") var recentLogin: String,
    @SerializedName("createdAt") val createdAt: String
)

data class GenreEntity(
    @SerializedName("genreId") val genreId: Int = 0,
    @SerializedName("genreName") val genreName: String = ""
)

data class SubscriptionProvider(
    @SerializedName("providerId") val providerId: Int? = null,
    @SerializedName("providerName") val providerName: String
)


data class UserRequest(
    @SerializedName("user_dto") val userDto: UserDto,
    @SerializedName("subscriptions") val subscriptions: List<Int>,
    @SerializedName("genres") val genres: List<Int>,
    @SerializedName("avoid_genres") val avoidGenres: List<Int>
)

@Parcelize
data class CommentDto(
    @SerializedName("comment_id") val commentId: Int?,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("username") val username: String,
    @SerializedName("post_id") val postId: Int,
    @SerializedName("content") val content: String,
    @SerializedName("sentiment") val sentiment: String?,
    @SerializedName("timestamp") val timestamp: String?,
    @SerializedName("parent_comment_id") val parentCommentId: Int? = null,
    @SerializedName("comment_type") val commentType: String
) : Parcelable

data class ReplyDto(
    @SerializedName("post_id") val postId: Int,
    @SerializedName("content") val content: String,
    @SerializedName("sentiment") val sentiment: String? = null,
    @SerializedName("timestamp") val timestamp: String? = null
)

data class TrailerInteractionDto(
    @SerializedName("interaction_id") val interactionId: Int?,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("post_id") val postId: Int,
    @SerializedName("start_timestamp") val startTimestamp: String,
    @SerializedName("end_timestamp") val endTimestamp: String,
    @SerializedName("replay_count") val replayCount: Int,
    @SerializedName("is_muted") val isMuted: Boolean,
    @SerializedName("like_state") val likeState: Boolean,
    @SerializedName("save_state") val saveState: Boolean,
    @SerializedName("comment_button_pressed") val commentButtonPressed: Boolean,
)

data class UserPostInteractionDto(
    @SerializedName("interaction_id") val interactionId: Int?,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("post_id") val postId: Int,
    @SerializedName("start_timestamp") val startTimestamp: String,
    @SerializedName("end_timestamp") val endTimestamp: String,
    @SerializedName("like_state") val likeState: Boolean = false,
    @SerializedName("save_state") val saveState: Boolean = false,
    @SerializedName("comment_button_pressed") val commentButtonPressed: Boolean = false,
)

@Parcelize
data class PostDto(
    @SerializedName("post_id") val postId: Int?,
    @SerializedName("tmdb_id") val tmdbId: Int,
    @SerializedName("type") val type: String,
    @SerializedName("title") val title: String,
    @SerializedName("subscription") val subscription: String,
    @SerializedName("release_date") val releaseDate: String?,
    @SerializedName("overview") val overview: String?,
    @SerializedName("poster_path") val posterPath: String?,
    @SerializedName("vote_average") val voteAverage: Double,
    @SerializedName("vote_count") val voteCount: Int,
    @SerializedName("original_language") val originalLanguage: String?,
    @SerializedName("original_title") val originalTitle: String?,
    @SerializedName("popularity") val popularity: Double,
    @SerializedName("genre_ids") val genreIds: String,
    @SerializedName("post_like_count") val postLikeCount: Int = 0,
    @SerializedName("trailer_like_count") val trailerLikeCount: Int = 0,
    @SerializedName("video_key") val videoKey: String
) : Parcelable

data class UserDto(
    @SerializedName("user_id") val userId: Int?,
    @SerializedName("name") val name: String,
    @SerializedName("username") val username: String,
    @SerializedName("password") val password: String,
    @SerializedName("email") val email: String,
    @SerializedName("language") val language: String,
    @SerializedName("region") val region: String,
    @SerializedName("min_movie") val minMovie: Int?,
    @SerializedName("max_movie") val maxMovie: Int?,
    @SerializedName("min_tv") val minTv: Int?,
    @SerializedName("max_tv") val maxTv: Int?,
    @SerializedName("oldest_date") val oldestDate: String,
    @SerializedName("recent_date") val recentDate: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("recent_login") val recentLogin: String?
)

data class UserPreferencesDto(
    @SerializedName("user_id") val userId: Int,
    @SerializedName("language") val language: String,
    @SerializedName("region") val region: String,
    @SerializedName("min_movie") val minMovie: Int?,
    @SerializedName("max_movie") val maxMovie: Int?,
    @SerializedName("min_tv") val minTv: Int?,
    @SerializedName("max_tv") val maxTv: Int?,
    @SerializedName("oldest_date") val oldestDate: String,
    @SerializedName("recent_date") val recentDate: String,
    @SerializedName("subscriptions") val subscriptions: List<Int>,
    @SerializedName("genre_ids") val genreIds: List<Int>,
    @SerializedName("avoid_genre_ids") val avoidGenreIds: List<Int>
)

data class ReplyCountDto(
    @SerializedName("parent_id") val parentId: Int,
    @SerializedName("reply_count") val replyCount: Int
)

data class VideoPair(
    @SerializedName("video_key") val videoKey: String,
    @SerializedName("post_id") val postId: Int
)

data class VideoDto(
    @SerializedName("video_key") val videoKey: String,
    @SerializedName("post_id") val postId: Int,
    @SerializedName("tmdb_id") val tmdbId: Int,
    @SerializedName("type") val type: String
)

data class InfoDto(
    @SerializedName("tmdb_id")
    val tmdbId: Int,
    @SerializedName("type")
    val type: String,
    @SerializedName("start_timestamp")
    val startTimestamp: String,
    @SerializedName("end_timestamp")
    val endTimestamp: String,
    @SerializedName("user_id")
    val userId: Int
)

data class UserSubscriptionDto(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("provider_id")
    val providerId: Int,
    @SerializedName("provider_name")
    val providerName: String,
    @SerializedName("priority")
    val priority: Int
)

data class UserGenreDto(
    @SerializedName("user_id")
    val userId: Int,
    @SerializedName("genre_id")
    val genreId: Int,
    @SerializedName("genre_name")
    val genreName: String,
    @SerializedName("priority")
    val priority: Int
)

data class ApiResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String
)

data class CommentResponse(
    @SerializedName("success") val success: Boolean,
    @SerializedName("message") val message: String,
    @SerializedName("comment_id") val commentId: Int
)

data class UserUpdate(
    @SerializedName("language") val language: String? = null,
    @SerializedName("region") val region: String? = null,
    @SerializedName("minMovie") val minMovie: Int? = null,
    @SerializedName("maxMovie") val maxMovie: Int? = null,
    @SerializedName("minTV") val minTV: Int? = null,
    @SerializedName("maxTV") val maxTV: Int? = null,
    @SerializedName("name") val name: String? = null,
    @SerializedName("username") val username: String? = null,
    @SerializedName("email") val email: String? = null,
    @SerializedName("oldestDate") val oldestDate: String? = null,
    @SerializedName("recentDate") val recentDate: String? = null
)

data class InteractionStates(
    @SerializedName("isLiked") val isLiked: Boolean = false,
    @SerializedName("isSaved") val isSaved: Boolean = false
)