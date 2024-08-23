package com.example.firedatabase_assis.workers


data class ApiWorkerParams(
    val apiKey: String,
    val includeAdult: Boolean,
    val includeVideo: Boolean,
    val page: Int,
    val sortBy: String,

    )

data class APIResponse(
    val page: Int,
    val results: List<Data>,
    val total_pages: Int,
    val total_results: Int
)

data class Data(
    val id: Int,
    val title: String,
    val release_date: String,
    val overview: String,
    val poster_path: String,
    val vote_average: Double,
    val vote_count: Int,
    val genre_ids: List<String>,
    val original_language: String,
    val original_title: String,
    val popularity: Double
)


data class VideoResponse(val results: List<Video>)
data class Video(
    val key: String,
    val type: String,
    val publishedAt: String,
    val isOfficial: Boolean
)

data class CreditsResponse(val cast: List<Cast>, val crew: List<Crew>)
data class Cast(val name: String)
data class Crew(val name: String)


