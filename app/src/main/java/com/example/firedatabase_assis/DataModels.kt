package com.example.firedatabase_assis

import androidx.room.Entity
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
    val watchRegion: String,
)

data class MovieResponse(
    val page: Int,
    val results: List<Movie>,
    val totalPages: Int,
    val totalResults: Int
) {

    companion object {
        fun fromJson(jsonObject: JsonObject): MovieResponse {
            return MovieResponse(
                jsonObject["page"].asInt,
                jsonObject["results"].asJsonArray.map { Movie.fromJson(it.asJsonObject) },
                jsonObject["total_pages"].asInt,
                jsonObject["total_results"].asInt
            )
        }
    }
}


data class Movie(
    val id: Int,
    val title: String,
    val release_date: String,
    val poster_path: String,
    val vote_average: Double,
    val vote_count: Int,
    // Add other fields as needed
) {
    companion object {
        fun fromJson(jsonObject: JsonObject): Movie {
            return Movie(
                jsonObject["id"].asInt,
                jsonObject["title"].asString,
                jsonObject["release_date"].asString,
                jsonObject["poster_path"].asString,
                jsonObject["vote_average"].asDouble,
                jsonObject["vote_count"].asInt,
                // Initialize other fields as needed
            )
        }
    }
}

@Entity(tableName = "movies")
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