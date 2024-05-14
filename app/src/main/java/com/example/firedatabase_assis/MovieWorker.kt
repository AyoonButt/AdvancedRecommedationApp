package com.example.firedatabase_assis


import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class MovieWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    private val apiWorkerParams = ApiWorkerParams(
        apiKey = "eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkOWRkOWQzYWU4MzhkYjE4ZDUxZjg4Y2Q1MGU0NzllNCIsInN1YiI6IjY2MjZiM2ZkMjU4ODIzMDE2NDkxODliMSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.wIF16waIO_pGrRKxWr4ye8QFYUFMGP6WBDX5Wg2JOpM",
        watchProviders = "8",
        watchRegion = "US",
        language = "en-US",
        includeAdult = false,
        includeVideo = true,
        sortBy = "popularity.desc",
        releaseDateGte = "2022-01-01",
        page = 1
    )

    private val apiWorker = ApiWorker(applicationContext, params, apiWorkerParams)

    override fun doWork(): Result {
        CoroutineScope(Dispatchers.IO).launch {
            apiWorker.doWork()
        }
        return Result.success()
    }
}