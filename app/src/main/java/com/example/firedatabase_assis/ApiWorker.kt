package com.example.firedatabase_assis

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {
    @GET("discover/movie")
    suspend fun getMovies(
        @Query("api_key") apiKey: String,
        @Query("include_adult") includeAdult: Boolean,
        @Query("include_video") includeVideo: Boolean,
        @Query("language") language: String,
        @Query("page") page: Int,
        @Query("release_date.gte") releaseDateGte: String,
        @Query("sort_by") sortBy: String,
        @Query("watch_region") region: String,
        @Query("watch_providers") watchProviders: String,
    ): Response<MovieResponse>
}

class ApiWorker(
    context: Context,
    params: WorkerParameters,
    private val apiWorkerParams: ApiWorkerParams
) :
    CoroutineWorker(context, params) {

    private val api: ApiService

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        api = retrofit.create(ApiService::class.java)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val movies = getAllMovies()

            val database = MovieDatabase.getDatabase(applicationContext)
            val movieDao = database.movieDao()

            movieDao.insertAll(movies)

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }

    private suspend fun getAllMovies(): List<Movie> {
        val movies = mutableListOf<Movie>()
        var page = apiWorkerParams.page
        var hasNextPage = true

        while (hasNextPage) {
            val response = getMovies(page)

            if (response.isSuccessful) {
                val body = response.body()
                body?.let {
                    movies.addAll(it.results)
                    if (it.page < it.totalPages) {
                        page++
                    } else {
                        hasNextPage = false
                    }
                } ?: run {
                    hasNextPage = false
                }
            } else {
                hasNextPage = false
            }
        }
        return movies
    }

    private suspend fun getMovies(page: Int): Response<MovieResponse> {
        val apiKey = apiWorkerParams.apiKey
        val watchProviders = apiWorkerParams.watchProviders // Example: 8 represents Netflix
        val watchRegion = apiWorkerParams.watchRegion
        val language = apiWorkerParams.language
        val includeAdult = apiWorkerParams.includeAdult
        val includeVideo = apiWorkerParams.includeVideo
        val sortBy = apiWorkerParams.sortBy
        val releaseDateGte = apiWorkerParams.releaseDateGte

        return api.getMovies(
            apiKey,
            includeAdult,
            includeVideo,
            language,
            page,
            releaseDateGte,
            sortBy,
            watchRegion,
            watchProviders
        )
    }
}