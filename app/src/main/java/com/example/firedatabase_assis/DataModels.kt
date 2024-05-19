package com.example.firedatabase_assis


import androidx.room.PrimaryKey
import com.google.gson.JsonObject


data class ApiWorkerParams(
    val apiKey: String,
    val includeAdult: Boolean,
    val includeVideo: Boolean,
    val language: String,
    val page: Int,
    val releaseDateGte: String,
    val sortBy: String,
    val watchProviders: String,
    val watchRegion: String
)

data class MovieResponse(
    val page: Int,
    val results: List<Movie>,
    val totalPages: Int,
    val totalResults: Int
)

data class Movie(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val title: String,
    val releaseDate: String,
    val overview: String,
    val posterPath: String,
    val voteAverage: Double,
    val voteCount: Int,
    val backdropPath: String,
    val genreIds: List<Int>,
    val originalLanguage: String,
    val originalTitle: String,
    val popularity: Double
) {
    companion object {
        fun fromJson(jsonObject: JsonObject): Movie {
            return Movie(
                jsonObject["id"].asInt,
                jsonObject["title"].asString,
                jsonObject["release_date"].asString,
                jsonObject["overview"].asString,
                jsonObject["poster_path"].asString,
                jsonObject["vote_average"].asDouble,
                jsonObject["vote_count"].asInt,
                jsonObject["backdrop_path"].asString,
                jsonObject["genre_ids"].asJsonArray.map { it.asInt },
                jsonObject["original_language"].asString,
                jsonObject["original_title"].asString,
                jsonObject["popularity"].asDouble
            )
        }
    }

    fun toMovieTable(): MovieTable {
        return MovieTable(
            id,
            title,
            releaseDate,
            overview,
            posterPath,
            voteAverage,
            voteCount,
            backdropPath,
            genreIds,
            originalLanguage,
            originalTitle,
            popularity
        )
    }
}


data class MovieTable(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val title: String,
    val releaseDate: String,
    val overview: String,
    val posterPath: String,
    val voteAverage: Double,
    val voteCount: Int,
    val backdropPath: String,
    val genreIds: List<Int>,
    val originalLanguage: String,
    val originalTitle: String,
    val popularity: Double
)