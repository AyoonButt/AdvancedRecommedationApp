package com.example.firedatabase_assis.database


import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters


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
    val total_pages: Int,
    val total_results: Int
)


@TypeConverters(Converters::class)
data class Movie(
    @PrimaryKey(autoGenerate = true)
    val id: Int,
    val title: String,
    val release_date: String,
    val overview: String,
    val poster_path: String,
    val vote_average: Double,
    val vote_count: Int,
    val backdrop_path: String,
    val genre_ids: List<String>,
    val original_language: String,
    val original_title: String,
    val popularity: Double
) {


    fun toMovieTable(): MovieTable {
        return MovieTable(
            id,
            title,
            release_date,
            overview,
            poster_path,
            vote_average,
            vote_count,
            backdrop_path,
            genre_ids,
            original_language,
            original_title,
            popularity
        )
    }
}

@Entity
@TypeConverters(Converters::class)
data class MovieTable(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String? = null,
    val releaseDate: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val voteAverage: Double? = null,
    val voteCount: Int? = null,
    val backdropPath: String? = null,
    val genreIds: List<String>? = null,
    val originalLanguage: String? = null,
    val originalTitle: String? = null,
    val popularity: Double? = null
)

@Entity("DisneyMovieTable")
@TypeConverters(Converters::class)
data class DisneyMovieTable(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String? = null,
    val releaseDate: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val voteAverage: Double? = null,
    val voteCount: Int? = null,
    val backdropPath: String? = null,
    val genreIds: List<String>? = null,
    val originalLanguage: String? = null,
    val originalTitle: String? = null,
    val popularity: Double? = null
)

@Entity("PrimeMovieTable")
@TypeConverters(Converters::class)
data class PrimeMovieTable(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String? = null,
    val releaseDate: String? = null,
    val overview: String? = null,
    val posterPath: String? = null,
    val voteAverage: Double? = null,
    val voteCount: Int? = null,
    val backdropPath: String? = null,
    val genreIds: List<String>? = null,
    val originalLanguage: String? = null,
    val originalTitle: String? = null,
    val popularity: Double? = null
)


fun Movie.toDisneyMovieTable(): DisneyMovieTable {
    return DisneyMovieTable(
        id = this.id,
        title = this.title,
        releaseDate = this.release_date,
        overview = this.overview,
        posterPath = this.poster_path,
        voteAverage = this.vote_average,
        voteCount = this.vote_count,
        backdropPath = this.backdrop_path,
        genreIds = this.genre_ids,
        originalLanguage = this.original_language,
        originalTitle = this.original_title,
        popularity = this.popularity
    )
}

fun Movie.toPrimeMovieTable(): PrimeMovieTable {
    return PrimeMovieTable(
        id = this.id,
        title = this.title,
        releaseDate = this.release_date,
        overview = this.overview,
        posterPath = this.poster_path,
        voteAverage = this.vote_average,
        voteCount = this.vote_count,
        backdropPath = this.backdrop_path,
        genreIds = this.genre_ids,
        originalLanguage = this.original_language,
        originalTitle = this.original_title,
        popularity = this.popularity
    )
}