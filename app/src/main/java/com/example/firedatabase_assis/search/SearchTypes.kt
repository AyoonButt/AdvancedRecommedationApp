package com.example.firedatabase_assis.search


data class Movie(
    val id: Int,
    val title: String,
    val poster_path: String?,
    val overview: String,
    val mediaType: String,
    val originalLanguage: String,
    val originalTitle: String,
    val popularity: Double,
    val voteAverage: Double,
    val firstAirDate: String,
    val voteCount: Int,
    val genreIds: List<Int>
)

class TV(
    val id: Int,
    val name: String,
    val poster_path: String?,
    val overview: String,
    val mediaType: String,
    val originalLanguage: String,
    val originalTitle: String,
    val popularity: Double,
    val voteAverage: Double,
    val firstAirDate: String,
    val voteCount: Int,
    val genreIds: List<Int>
)

data class Person(
    val id: Int,
    val name: String,
    val profile_path: String?,
    val gender: Int,
    val knownForDepartment: String,
    val knownFor: List<Any>, // This can be a list of movies or TV shows
    val mediaType: String = "person"
)


