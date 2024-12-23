package com.example.firedatabase_assis.workers


data class ApiWorkerParams(
    val apiKey: String,
    val includeAdult: Boolean,
    val includeVideo: Boolean,
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
    val publishedAt: String?, // Make it nullable
    val isOfficial: Boolean?
)


data class CreditsResponse(val cast: List<Cast>, val crew: List<Crew>)
data class Cast(
    val id: Int,
    val name: String,
    val character: String,
    val gender: Int?,
    val profilePath: String?,
    val order: Int
)

data class Crew(
    val id: Int,
    val name: String,
    val job: String,
    val department: String,
    val gender: Int?,
    val profilePath: String?
)


