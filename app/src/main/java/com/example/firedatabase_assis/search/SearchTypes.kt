package com.example.firedatabase_assis.search

data class SearchResult(
    val results: List<Result>
)

sealed class Result {
    data class Person(
        val id: Int,
        val name: String,
        val profile_path: String?
    ) : Result()

    data class Movie(
        val id: Int,
        val title: String,
        val poster_path: String?
    ) : Result()

    data class TvShow(
        val id: Int,
        val name: String,
        val poster_path: String?
    ) : Result()
}
