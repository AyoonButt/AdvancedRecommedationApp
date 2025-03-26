package com.example.firedatabase_assis.workers


import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.postgres.Credits
import com.example.firedatabase_assis.postgres.PostDto
import com.example.firedatabase_assis.postgres.Posts
import com.example.firedatabase_assis.postgres.UserPreferencesDto
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

            val userPreferences = getUserPreferences(userId)
            if (userPreferences == null) {
                Log.e("ApiWorker", "No userPrefereneces found for userId: $userId")
                return@withContext Result.failure()
            }

            Log.i(
                "ApiWorker",
                "Processing providers for userId: $userId with userPreferences: $userPreferences"
            )

            // Process providers based on the userPreferences
            processProvidersForUser(userId, userPreferences)

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
        userPreferences: UserPreferencesDto,
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
                        userPreferences,
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
        date: LocalDate,
        userPreferences: UserPreferencesDto,
        providers: String
    ): Boolean {
        val response = getMovies(
            1,
            date.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")),
            userPreferences,
            providers
        )
        return if (response != null) {
            response.isSuccessful && (response.body()?.total_pages ?: 0) < 500
        } else {
            false
        }
    }


    private suspend fun getMovies(
        page: Int,
        releaseDateGte: String,
        userPreferences: UserPreferencesDto?,
        providers: String
    ): Response<APIResponse>? {
        return if (userPreferences != null) {
            userPreferences.minMovie?.let { minMovie ->
                userPreferences.maxMovie?.let { maxMovie ->
                    api.getMovies(
                        apiWorkerParams.apiKey,
                        apiWorkerParams.includeAdult,
                        apiWorkerParams.includeVideo,
                        userPreferences.language,
                        page,
                        releaseDateGte,
                        userPreferences.recentDate,
                        apiWorkerParams.sortBy,
                        userPreferences.region,
                        minMovie,
                        maxMovie,
                        providers
                    )
                }
            }
        } else {
            null
        }
    }

    private suspend fun getSeries(
        page: Int,
        releaseDateGte: String,
        userPreferences: UserPreferencesDto?,
        providers: String
    ): Response<APIResponse>? {
        return if (userPreferences != null) {
            userPreferences.minTv?.let { minTv ->
                userPreferences.maxTv?.let { maxTv ->
                    api.getSeries(
                        apiWorkerParams.apiKey,
                        apiWorkerParams.includeAdult,
                        apiWorkerParams.includeVideo,
                        userPreferences.language,
                        page,
                        releaseDateGte,
                        userPreferences.recentDate,
                        apiWorkerParams.sortBy,
                        userPreferences.region,
                        minTv,
                        maxTv,
                        providers
                    )
                }
            }
        } else {
            null
        }
    }

    private fun selectBestVideoKey(videos: List<Video>): String? {
        Log.d("Video Selection", "Selecting the best video key from ${videos.size} results")

        // First, try to find official videos with preferred types
        val priorityOrder = listOf("Trailer", "Teaser", "Clip", "Featurette", "Short")

        // Filter for YouTube site first (as it's most likely to work)
        val youtubeVideos =
            videos.filter { it.publishedAt?.equals("YouTube", ignoreCase = true) == true }
        val otherVideos =
            videos.filter { it.publishedAt?.equals("YouTube", ignoreCase = true) != true }

        // Sort videos with various fallback options
        val rankedVideos = listOf(
            // 1. Official YouTube videos by priority type
            youtubeVideos.filter { it.isOfficial == true && priorityOrder.contains(it.type) }
                .sortedBy { priorityOrder.indexOf(it.type) },

            // 2. Any official YouTube videos
            youtubeVideos.filter { it.isOfficial == true },

            // 3. Unofficial YouTube trailers
            youtubeVideos.filter { it.type == "Trailer" },

            // 4. Any YouTube videos
            youtubeVideos,

            // 5. Official videos from other sites
            otherVideos.filter { it.isOfficial == true },

            // 6. Any remaining videos
            otherVideos
        ).flatten()

        // Among equally ranked videos, prefer more recent ones
        val bestVideo = rankedVideos
            .sortedWith(compareByDescending { it.publishedAt ?: "" })
            .firstOrNull()

        val selectedKey = bestVideo?.key

        if (selectedKey != null) {
            Log.d(
                "Video Selection", "type: ${bestVideo.type}, " +
                        "site: ${bestVideo.publishedAt}, official: ${bestVideo.isOfficial}, key: $selectedKey"
            )
        } else {
            Log.e("Video Selection", "No suitable video found from available options")
        }

        return selectedKey
    }

    private suspend fun processProvidersForUser(userId: Int, userPreferences: UserPreferencesDto?) =
        coroutineScope {
            val providers =
                getProvidersByPriority(userId) // Get providers, but we'll process them concurrently
            Log.d("ProvidersProcessing", "Processing ${providers.size} providers for user: $userId")

            // Launch all provider processing concurrently
            val jobs = providers.flatMap { providerId ->
                // For each provider, create two jobs (one for movies, one for TV)
                listOf(
                    async {
                        Log.d("MediaProcessing", "Processing movies for provider $providerId")
                        if (userPreferences != null) {
                            processMediaType("movie", userPreferences, providerId)
                        }
                    },
                    async {
                        Log.d("MediaProcessing", "Processing TV for provider $providerId")
                        if (userPreferences != null) {
                            processMediaType("tv", userPreferences, providerId)
                        }
                    }
                )
            }

            // Wait for all processing to complete
            jobs.awaitAll()

            Log.d("ProvidersProcessing", "Completed processing all providers for user: $userId")
        }


    private suspend fun processMediaType(
        type: String,
        userPreferences: UserPreferencesDto,
        providerId: Int
    ) {
        var page = 1
        var releaseDateGte = userPreferences.oldestDate
        val currentYear = LocalDate.now().year
        val providersString = providerId.toString()

        var hasNextPage = true
        while (hasNextPage) {
            val response = if (type == "movie") {
                getMovies(page, releaseDateGte, userPreferences, providersString)
            } else {
                getSeries(page, releaseDateGte, userPreferences, providersString)
            }

            if (response != null) {
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
                                    userPreferences,
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
                    // Inside the try block of postInsertionDeferred
                    try {
                        val title = data.title


                        if (!title.isNullOrEmpty() && videoKey != null) {
                            val postEntity = PostDto(
                                postId = null,
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

                            // Insert post and wait for the response
                            val postResponse = postsApi.addPosts(
                                mediaType,
                                inputData.getString("language") ?: "en-US",
                                providerId,
                                listOf(postEntity)
                            )

                            // Only proceed with credits if post insertion was successful
                            if (postResponse.isSuccessful) {
                                Log.d(
                                    "PostInsert",
                                    "Successfully added post for TMDB ID: ${data.id}"
                                )

                                // Now insert credits
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
                                Log.e(
                                    "PostInsertError",
                                    "Failed to add post for TMDB ID: ${data.id}. Response: ${postResponse.code()} - ${postResponse.message()}"
                                )
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


    private suspend fun getVideoKeyFromTmdb(
        tmdbApiService: ApiService,
        tmdbId: Int,
        type: String,
        language: String
    ): String? {
        Log.d("Language", "Language used for request: $language")

        // First try with the specified language
        var videoKey = tryGetVideoKey(tmdbApiService, tmdbId, type, language)

        // If no key found, fall back to English
        if (videoKey == null && language != "en-US") {
            Log.d("Video Fetch", "No video found in $language, trying fallback to en-US")
            videoKey = tryGetVideoKey(tmdbApiService, tmdbId, type, "en-US")
        }

        // If still no key, try with null language (to get all available videos)
        if (videoKey == null) {
            Log.d(
                "Video Fetch",
                "No video found in specific languages, trying without language filter"
            )
            videoKey = tryGetVideoKey(tmdbApiService, tmdbId, type, null)
        }

        return videoKey
    }

    private suspend fun tryGetVideoKey(
        tmdbApiService: ApiService,
        tmdbId: Int,
        type: String,
        language: String?
    ): String? {
        return try {
            // Log the request details
            Log.d(
                "Video Fetch",
                "Initiating fetch for tmdbId: $tmdbId, type: $type, language: $language"
            )

            val videoResponse = when (type) {
                "movie" -> {
                    Log.d("Video Fetch", "Fetching videos for movie ID: $tmdbId")
                    tmdbApiService.getMovieVideos(tmdbId, language ?: "", BuildConfig.TMDB_API_KEY)
                }

                "tv" -> {
                    Log.d("Video Fetch", "Fetching videos for TV show ID: $tmdbId")
                    tmdbApiService.getTvVideos(tmdbId, language ?: "", BuildConfig.TMDB_API_KEY)
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


            val gson = Gson()
            val creditsJson = try {
                val json = gson.toJson(creditsResponse?.body()).trim().removePrefix("\"")
                json
            } catch (e: Exception) {
                Log.e("Serialization Error", "Failed to serialize credits response", e)
                return
            }

            Log.d("CreditsJSON", "Credits JSON for tmdbId $tmdbId: $creditsJson")

            creditsJson.let {
                val creditsApiResponse = creditsApi.insertCredits(tmdbId, it)
                if (!creditsApiResponse.isSuccessful) {
                    Log.e("API Error", "Failed to insert credits for postId: $tmdbId")
                }
            }


        } catch (e: Exception) {
            e.printStackTrace()
        }
    }


}
