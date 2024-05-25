package com.example.firedatabase_assis.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.firedatabase_assis.database.ApiWorkerParams
import com.example.firedatabase_assis.database.GenericDao
import com.example.firedatabase_assis.database.Movie
import com.example.firedatabase_assis.database.MovieResponse
import com.google.gson.GsonBuilder
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
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

class ApiWorker<T : Any>(
    context: Context,
    params: WorkerParameters,
    private val apiWorkerParams: ApiWorkerParams,
    private val dao: GenericDao<T>,
    private val mapToTable: (Movie) -> T
) : CoroutineWorker(context, params) {

    private val api: ApiService

    init {
        AndroidThreeTen.init(context)

        val loggingInterceptor =
            HttpLoggingInterceptor { message -> Log.d("Retrofit", message) }.apply {
                level = HttpLoggingInterceptor.Level.BODY
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
            dao.insertAll(movies)

            Result.success()
        } catch (e: Exception) {
            Log.e("ApiWorker", "Error: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun getAllMovies(): List<T> {
        val movies = mutableListOf<T>()
        var page = apiWorkerParams.page
        var releaseDateGte = apiWorkerParams.releaseDateGte
        val currentYear = LocalDate.now().year


        var hasNextPage = true
        while (hasNextPage) {
            Log.d("ApiWorker", "Requesting movies with release date >= $releaseDateGte")
            val response = getMovies(page, releaseDateGte)
            //delay(30000)

            if (response.isSuccessful) {
                val body = response.body()
                body?.let {
                    Log.d("ApiWorker", "Total pages: ${it.total_pages}")

                    val movieTables = it.results.map { movie ->
                        mapToTable(movie)
                    }
                    movies.addAll(movieTables)
                    dao.insertAll(movieTables)  // Insert retrieved movies into the database
                    movies.clear()  // Clear movies variable after insertion

                    if (it.page < it.total_pages && page < 500) {
                        page++
                    } else {
                        if (page >= 500) {
                            // Increment the release date
                            releaseDateGte = incrementReleaseDate(
                                releaseDateGte,
                                currentYear,
                                it.total_pages
                            )

                            page = 1
                        } else {
                            hasNextPage = false
                        }
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


    private fun incrementReleaseDate(
        currentDate: String,
        currentYear: Int,
        totalPages: Int
    ): String {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        var newDate = LocalDate.parse(currentDate, dateFormatter)
        var bestDate: LocalDate? = null

        // Check if the total pages meet the criteria
        if (totalPages >= 500) {
            val increment12 = newDate.plusMonths(12).withDayOfMonth(1)
            val increment6 = newDate.plusMonths(6).withDayOfMonth(1)
            val increment3 = newDate.plusMonths(3).withDayOfMonth(1)
            val increment1 = newDate.plusMonths(1).withDayOfMonth(1)

            if (increment12.year <= currentYear && !increment12.isAfter(LocalDate.now())) {
                val response = runBlocking { getMovies(1, increment12.format(dateFormatter)) }
                if (response.isSuccessful && (response.body()?.total_pages ?: 0) < 500) {
                    println("Using date: ${increment12.format(dateFormatter)}")
                    return increment12.format(dateFormatter)
                } else {
                    bestDate = increment12
                }
            } else if (increment6.year <= currentYear && !increment6.isAfter(LocalDate.now())) {
                val response = runBlocking { getMovies(1, increment6.format(dateFormatter)) }
                if (response.isSuccessful && (response.body()?.total_pages ?: 0) < 500) {
                    println("Using date: ${increment6.format(dateFormatter)}")
                    return increment6.format(dateFormatter)
                } else {
                    bestDate = increment6
                }
            } else if (increment3.year <= currentYear && !increment3.isAfter(LocalDate.now())) {
                val response = runBlocking { getMovies(1, increment3.format(dateFormatter)) }
                if (response.isSuccessful && (response.body()?.total_pages ?: 0) < 500) {
                    println("Using date: ${increment3.format(dateFormatter)}")
                    return increment3.format(dateFormatter)
                } else {
                    bestDate = increment3
                }
            } else if (increment1.year <= currentYear && !increment1.isAfter(LocalDate.now())) {
                val response = runBlocking { getMovies(1, increment1.format(dateFormatter)) }
                if (response.isSuccessful && (response.body()?.total_pages ?: 0) < 500) {
                    println("Using date: ${increment1.format(dateFormatter)}")
                    return increment1.format(dateFormatter)
                } else {
                    bestDate = increment1
                }
            }

        }

        // If no suitable date is found, return the best date found so far
        return bestDate?.format(dateFormatter) ?: LocalDate.now().format(dateFormatter)
    }

    private suspend fun getMovies(page: Int, releaseDateGte: String): Response<MovieResponse> {
        val apiKey = apiWorkerParams.apiKey
        val includeAdult = apiWorkerParams.includeAdult
        val includeVideo = apiWorkerParams.includeVideo
        val language = apiWorkerParams.language
        val watchProviders = apiWorkerParams.watchProviders
        val sortBy = apiWorkerParams.sortBy
        val watchRegion = apiWorkerParams.watchRegion

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
