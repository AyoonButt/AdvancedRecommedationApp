package com.example.firedatabase_assis


import android.content.Context
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MovieWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val apiWorkerParams = ApiWorkerParams(
        apiKey = "d9dd9d3ae838db18d51f88cd50e479e4",
        includeAdult = false,
        includeVideo = false,
        language = "en-US",
        page = 1,
        releaseDateGte = "2022-01-01",
        sortBy = "popularity.desc",
        watchRegion = "US",
        watchProviders = "8",
    )

    private val apiWorker = ApiWorker(applicationContext, params, apiWorkerParams)

    override fun doWork(): Result {
        Log.d("MovieWorker", "Worker started")
        CoroutineScope(Dispatchers.IO).launch {
            apiWorker.doWork()
        }
        return Result.success()
    }
}
