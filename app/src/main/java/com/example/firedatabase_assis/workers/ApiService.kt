package com.example.firedatabase_assis.workers

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("discover/movie")
    suspend fun getMovies(
        @Query("api_key") apiKey: String,
        @Query("include_adult") includeAdult: Boolean,
        @Query("include_video") includeVideo: Boolean,
        @Query("language") language: String,
        @Query("page") page: Int,
        @Query("primary_release_date.gte") releaseDateGte: String,
        @Query("primary_release_date.lte") releaseDateLte: String,
        @Query("sort_by") sortBy: String,
        @Query("watch_region") region: String,
        @Query("with_runtime.gte") runtimeGte: Int,
        @Query("with_runtime.lte") runtimeLte: Int,
        @Query("watch_providers") watchProviders: String
    ): Response<APIResponse>

    @GET("discover/tv")
    suspend fun getSeries(
        @Query("api_key") apiKey: String,
        @Query("include_adult") includeAdult: Boolean,
        @Query("include_video") includeVideo: Boolean,
        @Query("language") language: String,
        @Query("page") page: Int,
        @Query("primary_release_date.gte") releaseDateGte: String,
        @Query("primary_release_date.lte") releaseDateLte: String,
        @Query("sort_by") sortBy: String,
        @Query("watch_region") region: String,
        @Query("with_runtime.gte") runtimeGte: Int,
        @Query("with_runtime.lte") runtimeLte: Int,
        @Query("watch_providers") watchProviders: String
    ): Response<APIResponse>

    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String
    ): Response<VideoResponse>

    @GET("tv/{series_id}/videos")
    suspend fun getTvVideos(
        @Path("series_id") seriesId: Int,
        @Query("language") language: String
    ): Response<VideoResponse>

    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String
    ): Response<CreditsResponse>

    @GET("tv/{series_id}/aggregate_credits")
    suspend fun getTvCredits(
        @Path("series_id") seriesId: Int,
        @Query("language") language: String
    ): Response<CreditsResponse>
}