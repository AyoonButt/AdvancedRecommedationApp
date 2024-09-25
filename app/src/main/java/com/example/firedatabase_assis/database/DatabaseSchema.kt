package com.example.firedatabase_assis.database

import android.util.Log
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.Table
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction


object Users : Table() {
    val userId = integer("user_id").autoIncrement()
    val name = varchar("name", 255)
    val username = varchar("username", 255).uniqueIndex()
    val email = varchar("email", 255)
    val pswd = varchar("pswd", 255)  // Use a secure method for password hashing
    val language = varchar("language", 50)
    val region = varchar("region", 50)
    val minMovie = integer("minMovie")
    val maxMovie = integer("maxMovie")
    val minTV = integer("minTV")
    val maxTV = integer("maxTV")
    val oldestDate = varchar("oldestDate", 50)
    val recentDate = varchar("recentDate", 50)
    val recentLogin = varchar("recentLogin", 75)
    val createdAt = varchar("createdAt", 75)
    override val primaryKey = PrimaryKey(userId)
}

object UserSubscriptions : Table() {
    val userId = integer("user_id").references(Users.userId)
    val providerID = integer("provider_id").references(SubscriptionProviders.providerId)
    val priority = integer("priority")
    override val primaryKey = PrimaryKey(userId, providerID)
}


object UserGenres : Table() {
    val userId = integer("user_id").references(Users.userId)
    val genreID =
        integer("genre_id").references(Genres.genreId)  // Corrected from provider_id to genre_id
    override val primaryKey = PrimaryKey(userId, genreID)  // Corrected from providerID to genreId
}

object Posts : Table() {
    val postId = integer("post_id").autoIncrement()
    val tmdbId = integer("tmdb_id")
    val postLikeCount = integer("postLikeCount")
    val trailerLikeCount = integer("trailerLikeCount")
    val type = varchar("type", 50)
    val title = varchar("title", 255)
    val subscription = varchar("provider", 255)
    val releaseDate = varchar("releaseDate", 100)
    val overview = text("overview")
    val posterPath = text("posterPath")
    val voteAverage = double("voteAverage")
    val voteCount = integer("voteCount")
    val originalLanguage = varchar("originalLanguage", 50)
    val originalTitle = varchar("originalTitle", 255)
    val popularity = double("popularity")
    val genreIds = varchar("genreIds", 255)
    val videoKey = varchar("videoKey", 100)
    override val primaryKey = PrimaryKey(postId)
}

object Cast : Table() {
    val castId = integer("cast_id").autoIncrement()
    val postId = integer("post_id").references(Posts.postId)
    val personId = integer("person_id")
    val name = varchar("name", 255)
    val gender = integer("gender")
    val knownForDepartment = varchar("known_for_department", 255)
    val character = varchar("character", 255)
    val episodeCount = integer("episode_count")
    val orderIndex = integer("order_index")
    val popularity = decimal("popularity", 10, 2)
    val profilePath = varchar("profile_path", 255).nullable()
    override val primaryKey = PrimaryKey(castId)

}

object Crew : Table() {
    val crewId = integer("crew_id").autoIncrement()
    val postId = integer("post_id").references(Posts.postId)
    val personId = integer("person_id")
    val name = varchar("name", 255)
    val gender = integer("gender")
    val knownForDepartment = varchar("known_for_department", 255)
    val job = varchar("job", 255)
    val department = varchar("department", 255)
    val episodeCount = integer("episode_count")
    val popularity = decimal("popularity", 10, 2)
    val profilePath = varchar("profile_path", 255).nullable()
    override val primaryKey = PrimaryKey(crewId)
}


object UserPostInteractions : Table() {
    val interactionId = integer("interaction_id").autoIncrement()
    val userId = integer("user_id").references(Users.userId)
    val postId = integer("post_id").references(Posts.postId)
    val timeSpentOnPost = long("time_spent")
    val likeState = bool("like_state")
    val saveState = bool("save_state")
    val commentButtonPressed = bool("comment_button_pressed")
    val commentMade = bool("comment_made")
    val timestamp = varchar("timestamp", 75)
    override val primaryKey = PrimaryKey(interactionId)
}

object UserTrailerInteractions : Table() {
    val interactionId = integer("interaction_id").autoIncrement()
    val userId = integer("user_id").references(Users.userId)
    val postId = integer("post_id").references(Posts.postId)
    val timeSpent = long("time_spent")
    val isMuted = bool("isMuted")
    val trailerLikeState = bool("trailer_like_state")
    val trailerSaveState = bool("trailer_save_state")
    val replayTimes = integer("replay_times")
    val commentButtonPressed = bool("comment_button_pressed")
    val commentMade = bool("comment_made")
    val timestamp = varchar("timestamp", 75)
    override val primaryKey = PrimaryKey(interactionId)
}

object Comments : Table() {
    val postId = integer("post_id").references(Posts.postId)
    val commentId = integer("comment_id").autoIncrement()
    val userId = integer("user_id").references(Users.userId)
    val username = varchar("username", 50).references(Users.username)
    val content = text("content")
    val sentiment = varchar("sentiment", 50)
    override val primaryKey = PrimaryKey(commentId)
}

object SubscriptionProviders : Table() {
    val providerId = integer("provider_id").autoIncrement()
    val providerName = varchar("provider_name", 255)
    override val primaryKey = PrimaryKey(providerId)
}

object Genres : Table() {
    val genreId = integer("genre_id")
    val genreName = varchar("genre_name", 255)
    override val primaryKey = PrimaryKey(genreId)
}


object PostGenres : Table() {
    val postId = integer("post_id").references(Posts.postId)
    val genreID = integer("genre_id").references(Genres.genreId)
    val avoidGenres = varchar("avoid_genre", 500)
    override val primaryKey = PrimaryKey(postId, genreID)
}


fun connectToDatabase() {
    try {
        Database.connect(
            url = "jdbc:postgresql://recommendation-app.c76yuoa4ivv0.us-east-2.rds.amazonaws.com:5432/postgres",
            driver = "org.postgresql.Driver",
            user = "postgres",  // e.g., "postgres"
            password = "Watermelon2131"  // your local database password
        )
        Log.d("DatabaseConnection", "Connection to local DB successful")
    } catch (e: Exception) {
        Log.e("DatabaseConnection", "Connection to local DB failed: ${e.message}", e)
    }
}


fun createTables() {
    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.create(
            Users,
            Posts,
            UserPostInteractions,
            UserTrailerInteractions,
            Comments,
            SubscriptionProviders,
            Genres,
            UserSubscriptions,
            PostGenres
        )
    }
}


