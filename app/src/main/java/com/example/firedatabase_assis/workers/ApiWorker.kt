package com.example.firedatabase_assis.workers


import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.postgres.Credits
import com.example.firedatabase_assis.postgres.PostEntity
import com.example.firedatabase_assis.postgres.Posts
import com.example.firedatabase_assis.postgres.UserParams
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.jakewharton.threetenabp.AndroidThreeTen
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    private val api: ApiService


    private val apiWorkerParams: ApiWorkerParams = ApiWorkerParams(
        apiKey = BuildConfig.TMDB_API_KEY,
        includeAdult = false,
        includeVideo = false,
        sortBy = "popularity.desc"
    )

    // Initialize Retrofit for TMDB API and your custom API
    private val gson = GsonBuilder().setLenient().create()
    private val tmdbRetrofit = Retrofit.Builder()
        .baseUrl("https://api.themoviedb.org/3/")
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val apiRetrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build()

    private val tmdbApiService = tmdbRetrofit.create(ApiService::class.java)
    private val postsApi = apiRetrofit.create(Posts::class.java)
    private val creditsApi = apiRetrofit.create(Credits::class.java)

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
            Log.i("ApiWorker", "doWork started with inputData: $inputData")

            // Retrieve user parameters from worker input data
            val userId = inputData.getInt("userId", -1) // Get userId from the input data
            if (userId == -1) {
                Log.e("ApiWorker", "Invalid userId (-1) in inputData")
                return@withContext Result.failure()
            }

            val userParams = fetchUserParams(userId) // Retrieve userParams from the database
            if (userParams == null) {
                Log.e("ApiWorker", "No userParams found for userId: $userId")
                return@withContext Result.failure()
            }

            Log.i(
                "ApiWorker",
                "Processing providers for userId: $userId with userParams: $userParams"
            )

            // Process providers based on the userParams
            processProvidersForUser(userId, userParams)

            Log.i("ApiWorker", "doWork completed successfully for userId: $userId")
            return@withContext Result.success()
        } catch (e: Exception) {
            // Log the full exception with details
            Log.e("ApiWorker", "Unexpected error in doWork: ${e.message}", e)
            return@withContext Result.failure()
        }
    }


    private suspend fun incrementReleaseDate(
        currentDate: String,
        currentYear: Int,
        userParams: UserParams,
        providers: String
    ): String {
        val dateFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd")
        val newDate = LocalDate.parse(currentDate, dateFormatter)
        var bestDate: LocalDate? = null

        // Define increments in order of preference
        val increments = listOf(


            newDate.plusYears(1).withDayOfMonth(1),   // 1 year increment
            newDate.plusMonths(6).withDayOfMonth(1),  // 6 months increment
            newDate.plusMonths(3).withDayOfMonth(1),  // 3 months increment
            newDate.plusMonths(1).withDayOfMonth(1),  // 1 month increment
            newDate.plusWeeks(1),                     // 1 week increment
            newDate.plusDays(1),


            )

        // Sort increments in descending order to prioritize larger increments
        val sortedIncrements = increments.sortedByDescending { it }

        for (increment in sortedIncrements) {
            // Additional check to ensure we don't go beyond the current year or future dates
            if (increment.year <= currentYear && !increment.isAfter(LocalDate.now())) {
                try {
                    val isValid = checkDate(
                        increment,
                        userParams,
                        providers
                    )

                    if (isValid) {
                        Log.d("ApiWorker", "Using date: ${increment.format(dateFormatter)}")
                        return increment.format(dateFormatter)
                    } else {
                        // Keep track of the last invalid date as a fallback
                        bestDate = increment
                    }
                } catch (e: Exception) {
                    // Log any exceptions during date checking
                    Log.e("ApiWorker", "Error checking date: ${increment.format(dateFormatter)}", e)
                }
            }
        }

        // Fallback to the best date or current date if no suitable date found
        return bestDate?.format(dateFormatter)
            ?: LocalDate.now().format(dateFormatter).also {
                Log.w("ApiWorker", "No suitable date found. Using current date.")
            }
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

    private fun selectBestVideoKey(videos: List<Video>): String? {
        println("Selecting the best video key from results: $videos")

        // Define the priority order for video types
        val priorityOrder = listOf("Short", "Trailer", "Teaser", "Featurette", "Clip")

        // Filter and sort videos based on priority and official status
        val filteredVideos = videos
            .filter { video ->
                video.isOfficial == true && priorityOrder.contains(video.type)
            }
            .sortedWith(
                compareBy(
                    { priorityOrder.indexOf(it.type) },  // Prioritize by type
                    { it.publishedAt ?: "" }  // Handle null values
                )
            )

        // If no official videos found, look for unofficial trailers or most recently published videos
        val bestVideo = filteredVideos.lastOrNull()
            ?: videos
                .filter { video ->
                    video.type == "Trailer" && video.isOfficial == false
                }
                .maxByOrNull { it.publishedAt ?: "" }  // Handle null values
            ?: videos.maxByOrNull { it.publishedAt ?: "" }  // Handle null values

        // Return the selected video's key or null if no videos are available
        return bestVideo?.key
    }


    private suspend fun processProvidersForUser(userId: Int, userParams: UserParams) =
        coroutineScope {
            val providers = getProvidersByPriority(userId)
            Log.d("ProvidersProcessing", "Processing ${providers.size} providers for user: $userId")

            // Concurrently process all providers
            providers.map { providerId ->
                async {
                    Log.d("ProviderProcessing", "Processing provider $providerId for user: $userId")

                    // Process movies and TV series concurrently for each provider
                    coroutineScope {
                        val movieJob = async { processMediaType("movie", userParams, providerId) }
                        val tvJob = async { processMediaType("tv", userParams, providerId) }

                        // Wait for both movie and TV processing to complete
                        movieJob.await()
                        tvJob.await()
                    }
                }
            }.awaitAll() // Wait for all provider processing to complete

            Log.d("ProvidersProcessing", "Completed processing all providers for user: $userId")
        }


    private suspend fun processMediaType(
        type: String,
        userParams: UserParams,
        providerId: Int
    ) {
        var page = 1
        var releaseDateGte = userParams.oldestDate
        val currentYear = LocalDate.now().year
        val providersString = providerId.toString()

        var hasNextPage = true
        while (hasNextPage) {
            val response = if (type == "movie") {
                getMovies(page, releaseDateGte, userParams, providersString)
            } else {
                getSeries(page, releaseDateGte, userParams, providersString)
            }

            if (response.isSuccessful) {
                val body = response.body()
                body?.let { apiResponse ->
                    val mediaResults = apiResponse.results

                    // Process media in batches with concurrent video key and post insertion
                    processMediaBatch(type, mediaResults, providerId)

                    // Pagination and date incrementation logic
                    if (apiResponse.page < apiResponse.total_pages && page < 500) {
                        page++
                    } else {
                        if (page >= 500) {
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
    }

    private suspend fun processMediaBatch(
        mediaType: String,
        mediaResults: List<Data>,
        providerId: Int
    ) = coroutineScope {
        val batchSize = 20

        // Process items in batches
        for (i in mediaResults.indices step batchSize) {
            val batch = mediaResults.slice(i until minOf(i + batchSize, mediaResults.size))
            Log.d(
                "BatchProcessing",
                "Processing batch ${i / batchSize + 1} for $mediaType with ${batch.size} items"
            )

            // Concurrent processing of video keys
            val videoKeysDeferred = batch.map { data ->
                async {
                    try {
                        Log.d("VideoKeyFetch", "Fetching video key for TMDB ID: ${data.id}")
                        getVideoKeyFromTmdb(
                            tmdbApiService,
                            data.id,
                            mediaType,
                            inputData.getString("language") ?: "en"
                        )
                    } catch (e: Exception) {
                        Log.e(
                            "VideoKeyError",
                            "Error fetching video key for TMDB ID: ${data.id}",
                            e
                        )
                        null // Return null on failure
                    }
                }
            }

            val videoKeys = videoKeysDeferred.awaitAll()
            Log.d("VideoKeysResult", "Video keys fetched: $videoKeys")

            // Counter for successfully processed posts
            var processedCount = 0

            // Concurrent post insertion and credit fetching
            val postInsertionDeferred = batch.zip(videoKeys).map { (data, videoKey) ->
                async {
                    try {
                        val title = data.title
                        if (title.isNotEmpty() && videoKey != null) {
                            val postEntity = PostEntity(
                                tmdbId = data.id,
                                title = title,
                                releaseDate = data.release_date,
                                overview = data.overview,
                                posterPath = data.poster_path,
                                voteAverage = data.vote_average,
                                voteCount = data.vote_count,
                                genreIds = data.genre_ids.joinToString(","),
                                originalLanguage = data.original_language,
                                originalTitle = data.original_title,
                                popularity = data.popularity,
                                type = mediaType,
                                subscription = providerId.toString(),
                                videoKey = videoKey
                            )

                            // Insert post and fetch credits
                            postsApi.addPosts(mediaType, providerId, listOf(postEntity))
                            insertCreditsBasedOnType(
                                tmdbApiService,
                                data.id,
                                mediaType,
                                inputData.getString("language") ?: "en-US"
                            )
                            synchronized(this) {
                                processedCount++ // Increment the counter
                            }
                        } else {
                            Log.w(
                                "PostEntitySkip",
                                "Skipping post creation for TMDB ID: ${data.id}, title or video key is missing."
                            )
                        }
                    } catch (e: Exception) {
                        Log.e("PostInsertError", "Error inserting post for TMDB ID: ${data.id}", e)
                    }
                }
            }

            postInsertionDeferred.awaitAll()

            // Log the number of processed items in the batch
            Log.d(
                "BatchProcessing",
                "Batch ${i / batchSize + 1} processed. Successfully handled $processedCount out of ${batch.size} items for $mediaType."
            )
        }
    }


    // Function to retrieve the best video key from TMDB based on post type
    private suspend fun getVideoKeyFromTmdb(
        tmdbApiService: ApiService,
        tmdbId: Int,
        type: String,
        language: String
    ): String? {
        Log.d("Language", "Language used for request: $language")

        return try {
            // Log the request details
            Log.d(
                "Video Fetch",
                "Initiating fetch for tmdbId: $tmdbId, type: $type, language: $language"
            )

            val videoResponse = when (type) {
                "movie" -> {
                    Log.d("Video Fetch", "Fetching videos for movie ID: $tmdbId")
                    tmdbApiService.getMovieVideos(tmdbId, language, BuildConfig.TMDB_API_KEY)
                }

                "tv" -> {
                    Log.d("Video Fetch", "Fetching videos for TV show ID: $tmdbId")
                    tmdbApiService.getTvVideos(tmdbId, language, BuildConfig.TMDB_API_KEY)
                }

                else -> {
                    Log.e("Video Fetch", "Invalid media type: $type for tmdbId: $tmdbId")
                    null
                }
            }

            if (videoResponse == null) {
                Log.e("Video Fetch", "Failed to fetch video response for tmdbId: $tmdbId")
                return null
            }

            // Log HTTP response code
            Log.d("Video Fetch", "HTTP Response Code for tmdbId $tmdbId: ${videoResponse.code()}")

            // Log the response body or indicate its absence
            val rawBody = videoResponse.body()
            if (rawBody == null) {
                Log.e("Video Fetch", "Response body is null for tmdbId: $tmdbId")
                return null
            } else {
                Log.d("Video Fetch", "Response body for tmdbId: $tmdbId: $rawBody")
            }


            // Process the results
            rawBody.results.let { results ->
                if (results.isEmpty()) {
                    Log.e("Video Fetch", "No video results for tmdbId: $tmdbId")
                    null
                } else {
                    Log.d("Video Results", "Video results for tmdbId: $tmdbId: $results")
                    selectBestVideoKey(results)
                }
            } ?: run {
                Log.e("Video Fetch", "Results are null for tmdbId: $tmdbId")
                null
            }
        } catch (e: HttpException) {
            Log.e("Video Fetch", "HTTP exception for tmdbId: $tmdbId, code: ${e.code()}", e)
            null
        } catch (e: IOException) {
            Log.e("Video Fetch", "Network error while fetching videos for tmdbId: $tmdbId", e)
            null
        } catch (e: Exception) {
            Log.e("Video Fetch", "Unexpected error for tmdbId: $tmdbId", e)
            null
        }
    }


    // Function to insert credits based on the type (movie or tv)
    private suspend fun insertCreditsBasedOnType(
        tmdbApiService: ApiService,
        tmdbId: Int,
        type: String,
        language: String
    ) {
        try {
            val creditsResponse = when (type) {
                "movie" -> tmdbApiService.getMovieCredits(
                    tmdbId,
                    language,
                    BuildConfig.TMDB_API_KEY
                )

                "tv" -> tmdbApiService.getTvCredits(tmdbId, language, BuildConfig.TMDB_API_KEY)
                else -> null
            }

            val postIdResponse = postsApi.getPostIdByTmdbId(tmdbId)
            val postId = postIdResponse.body()  // Assuming this API returns Response<Int?>

            if (postId == null) {
                Log.e("Post ID Error", "No post found for tmdbId: $tmdbId")
                return
            }

            val gson = Gson()
            val creditsJson = try {
                val json = gson.toJson(creditsResponse?.body()).trim().removePrefix("\"")
                json
            } catch (e: Exception) {
                Log.e("Serialization Error", "Failed to serialize credits response", e)
                return
            }

            Log.d("CreditsJSON", "Credits JSON for postId $postId: $creditsJson")

            creditsJson.let {
                val creditsApiResponse = creditsApi.insertCredits(postId, it)
                if (!creditsApiResponse.isSuccessful) {
                    Log.e("API Error", "Failed to insert credits for postId: $postId")
                }
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}
