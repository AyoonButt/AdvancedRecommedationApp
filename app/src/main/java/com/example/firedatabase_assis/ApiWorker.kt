package com.example.firedatabase_assis

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.gson.GsonBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

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
        val loggingInterceptor = HttpLoggingInterceptor(object : HttpLoggingInterceptor.Logger {
            override fun log(message: String) {
                Log.d("Retrofit", message)
            }
        }).apply {
            level = HttpLoggingInterceptor.Level.BODY // Change to desired log level
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create(gson))
            .client(client)
            .build()

        api = retrofit.create(ApiService::class.java)
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val movies = getAllMovies()

            // Insert retrieved movies into the database
            val database = MovieDatabase.getDatabase(applicationContext)
            val movieDao = database.movieDao()
            movieDao.insertAll(movies)

            Result.success()
        } catch (e: Exception) {
            Log.e("ApiWorker", "Error: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun getAllMovies(): List<MovieTable> {
        val movies = mutableListOf<MovieTable>()
        var page = apiWorkerParams.page

        var hasNextPage = true
        while (hasNextPage) {
            val response = getMovies(page)

            if (response.isSuccessful) {
                val body = response.body()
                body?.let {
                    val movieTables = it.results.map { movie ->
                        movie.toMovieTable()
                    }
                    movies.addAll(movieTables)
                    if (it.page < it.totalPages) {
                        page++
                    } else {
                        hasNextPage = false
                    }
                } ?: run {
                    hasNextPage = false
                }
            } else {
                Log.e("ApiWorker", "Error: ${response.code()} - ${response.message()}")
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
