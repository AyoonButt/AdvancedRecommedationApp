package com.example.firedatabase_assis.workers


import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.firedatabase_assis.database.ApiWorkerParams
import com.example.firedatabase_assis.database.MovieDatabase
import com.example.firedatabase_assis.database.toDisneyMovieTable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class DisneyWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private val apiWorkerParams = ApiWorkerParams(
        apiKey = "d9dd9d3ae838db18d51f88cd50e479e4",
        includeAdult = false,
        includeVideo = false,
        language = "en-US",
        page = 1,
        releaseDateGte = "2022-01-01",
        sortBy = "popularity.desc",
        watchRegion = "US",
        watchProviders = "337",
    )

    private val disneyDao = MovieDatabase.getDatabase(context).disneyMovieDao()  // Your new DAO
    private val apiWorker = ApiWorker(
        applicationContext,
        params,
        apiWorkerParams,
        disneyDao,
        { movie -> movie.toDisneyMovieTable() }  // Mapping function to your new table
    )

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d("DisneyWorker", "Worker started")
        try {
            apiWorker.doWork()
        } catch (e: Exception) {
            Log.e("DisneyWorker", "Error: ${e.message}", e)
            Result.failure()
        }
    }
}
