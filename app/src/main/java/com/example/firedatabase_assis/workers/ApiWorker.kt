package com.example.firedatabase_assis.workers

import UserParams
import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.database.Posts
import com.google.gson.GsonBuilder
import com.jakewharton.threetenabp.AndroidThreeTen
import fetchUserParams
import getProvidersByPriority
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.jetbrains.exposed.sql.insert
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import org.jetbrains.exposed.sql.update
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import parseAndInsertCredits
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import selectBestVideoKey
import java.util.concurrent.TimeUnit


class ApiWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val api: ApiService


    private val apiWorkerParams: ApiWorkerParams = ApiWorkerParams(
        apiKey = BuildConfig.TMDB_API_KEY,
        includeAdult = false,
        includeVideo = false,
        sortBy = "popularity.desc"
    )

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
        return@withContext try {
            val userId = 1 // Replace with actual user ID retrieval
            val userParams = fetchUserParams(userId) ?: return@withContext Result.failure()

            processProvidersForUser(userId, userParams)

            Result.success()
        } catch (e: Exception) {
            Log.e("ApiWorker", "Error: ${e.message}", e)
            Result.failure()
        }
    }

    private suspend fun getAllMoviesOrSeries(
        type: String,
        userParams: UserParams,
        providers: List<Int>
    ): List<Data> {
        val mediaList = mutableListOf<Data>()  // Keep it as List<Data>
        var page = 1
        var releaseDateGte = userParams.oldestDate
        val currentYear = LocalDate.now().year

        var hasNextPage = true
        while (hasNextPage) {
            Log.d("ApiWorker", "Requesting $type with release date >= $releaseDateGte")

            // Convert List<Int> to comma-separated String
            val providersString = providers.joinToString(",")

            val response = if (type == "movie") {
                getMovies(page, releaseDateGte, userParams, providersString)
            } else {
                getSeries(page, releaseDateGte, userParams, providersString)
            }

            if (response.isSuccessful) {
                val body = response.body()
                body?.let {
                    Log.d("ApiWorker", "Total pages: ${it.total_pages}")

                    val mediaResults = it.results  // List<Data>
                    mediaList.addAll(mediaResults)

                    if (it.page < it.total_pages && page < 500) {
                        page++
                    } else {
                        if (page >= 500) {
                            // Pass userParams and providers as String
                            releaseDateGte = incrementReleaseDate(
                                releaseDateGte,
                                currentYear,
                                userParams,
                                providersString
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
        return mediaList
    }


    private suspend fun incrementReleaseDate(
        currentDate: String,
        currentYear: Int,
        userParams: UserParams,
        providers: String
    ): String {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val newDate = LocalDate.parse(currentDate, dateFormatter)
        var bestDate: LocalDate? = null

        val increments = listOf(
            newDate.plusMonths(12).withDayOfMonth(1),
            newDate.plusMonths(6).withDayOfMonth(1),
            newDate.plusMonths(3).withDayOfMonth(1),
            newDate.plusMonths(1).withDayOfMonth(1)
        )

        for (increment in increments) {
            if (increment.year <= currentYear && !increment.isAfter(LocalDate.now())) {

                val isValid = checkDate(
                    increment,
                    userParams,
                    providers
                ) // Pass userParams and providers to checkDate
                if (isValid) {
                    Log.d("ApiWorker", "Using date: ${increment.format(dateFormatter)}")
                    return increment.format(dateFormatter)
                } else {
                    bestDate = increment
                }
            }
        }

        return bestDate?.format(dateFormatter) ?: LocalDate.now().format(dateFormatter)
    }

    private suspend fun checkDate(
        date: LocalDate, userParams: UserParams, providers: String
    ): Boolean {
        // Update to use `providers` parameter
        val response =
            getMovies(
                1,
                date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
                userParams,
                providers
            )
        return response.isSuccessful && (response.body()?.total_pages ?: 0) < 500
    }


    private suspend fun getMovies(
        page: Int,
        releaseDateGte: String,
        userParams: UserParams,
        providers: String
    ): Response<APIResponse> {
        return api.getMovies(
            apiWorkerParams.apiKey,
            apiWorkerParams.includeAdult,
            apiWorkerParams.includeVideo,
            userParams.language,
            page,
            releaseDateGte,
            userParams.recentDate,
            apiWorkerParams.sortBy,
            userParams.region,
            userParams.minMovie,
            userParams.maxMovie,
            providers
        )
    }

    private suspend fun getSeries(
        page: Int,
        releaseDateGte: String,
        userParams: UserParams,
        providers: String // Change from List<Int> to String
    ): Response<APIResponse> {
        return api.getSeries(
            apiWorkerParams.apiKey,
            apiWorkerParams.includeAdult,
            apiWorkerParams.includeVideo,
            userParams.language,
            page,
            releaseDateGte,
            userParams.recentDate,
            apiWorkerParams.sortBy,
            userParams.region,
            userParams.minTV,
            userParams.maxTV,
            providers
        )
    }


    private suspend fun processProvidersForUser(
        userId: Int,
        userParams: UserParams
    ): Pair<List<Data>, List<Data>> {
        val providers = getProvidersByPriority(userId)
        val allMovies = mutableListOf<Data>()
        val allSeries = mutableListOf<Data>()

        for (providerId in providers) {
            val movies: List<Data> = getAllMoviesOrSeries("movie", userParams, listOf(providerId))
            val series: List<Data> = getAllMoviesOrSeries("tv", userParams, listOf(providerId))

            // Add posts to database with the current providerId
            addPostsToDatabase("movie", movies, providerId)
            addPostsToDatabase("tv", series, providerId)

            allMovies.addAll(movies)
            allSeries.addAll(series)
        }

        // After adding posts to the database, update them
        // Assume `allMovies` and `allSeries` contain posts already added
        // You might need to modify this if `allMovies` and `allSeries` need to be fetched after insertion
        allMovies.forEach { movie ->
            updatePostData(movie.id, "movie", userParams.language)
        }
        allSeries.forEach { series ->
            updatePostData(series.id, "tv", userParams.language)
        }

        return Pair(allMovies, allSeries)
    }


    private suspend fun updatePostData(postId: Int, type: String, language: String) {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.themoviedb.org/3/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        val apiService = retrofit.create(ApiService::class.java)

        withContext(Dispatchers.IO) {
            transaction {
                val post = Posts.select { Posts.postId eq postId }.singleOrNull()

                post?.let {
                    val tmdbId = it[Posts.tmdbId]
                    var newVideoKey: String? = null
                    var newCredits: String? = null

                    // Launch a coroutine to perform API calls
                    runBlocking {
                        launch {
                            try {
                                when (type) {
                                    "movie" -> {
                                        val videoResponse =
                                            apiService.getMovieVideos(tmdbId, language)
                                        val creditsResponse =
                                            apiService.getMovieCredits(tmdbId, language)

                                        newVideoKey =
                                            videoResponse.body()?.results?.let { results ->
                                                selectBestVideoKey(results)
                                            }

                                        newCredits = creditsResponse.body()?.let { credits ->
                                            parseAndInsertCredits(credits.toString(), postId)
                                            credits.toString()
                                        }
                                    }

                                    "tv" -> {
                                        val videoResponse = apiService.getTvVideos(tmdbId, language)
                                        val creditsResponse =
                                            apiService.getTvCredits(tmdbId, language)

                                        newVideoKey =
                                            videoResponse.body()?.results?.let { results ->
                                                selectBestVideoKey(results)
                                            }

                                        newCredits = creditsResponse.body()?.let { credits ->
                                            parseAndInsertCredits(credits.toString(), postId)
                                            credits.toString()
                                        }
                                    }

                                    else -> {
                                        newVideoKey = null
                                    }
                                }

                                // Update the post with the new video key
                                Posts.update({ Posts.postId eq postId }) {
                                    it[videoKey] = newVideoKey ?: ""
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }.join() // Ensure coroutine completes before proceeding
                    }
                }
            }
        }
    }

    private suspend fun addPostsToDatabase(
        mediaType: String,
        dataList: List<Data>,
        providerId: Int
    ) {
        withContext(Dispatchers.IO) {
            transaction {
                dataList.forEach { data ->
                    Posts.insert {
                        it[tmdbId] = data.id
                        it[title] = data.title
                        it[releaseDate] = data.release_date
                        it[overview] = data.overview
                        it[posterPath] = data.poster_path
                        it[voteAverage] = data.vote_average
                        it[voteCount] = data.vote_count
                        it[genreIds] = data.genre_ids.joinToString(",")
                        it[originalLanguage] = data.original_language
                        it[originalTitle] = data.original_title
                        it[popularity] = data.popularity
                        it[type] = mediaType
                        it[subscription] =
                            providerId.toString()  // Store providerId in subscription column
                    }
                }
            }
        }
    }

}
