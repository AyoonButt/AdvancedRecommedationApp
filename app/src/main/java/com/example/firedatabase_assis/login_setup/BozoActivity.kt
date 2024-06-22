package com.example.firedatabase_assis.login_setup

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.firedatabase_assis.R
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class BozoActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_setup)

        // Start coroutine to fetch providers
        CoroutineScope(Dispatchers.IO).launch {
            fetchProviders()
        }
    }

    private suspend fun fetchProviders() {
        val client = OkHttpClient()

        val request = Request.Builder()
            .url("https://api.themoviedb.org/3/watch/providers/movie?language=en-US")
            .get()
            .addHeader("accept", "application/json")
            .addHeader(
                "Authorization",
                "Bearer eyJhbGciOiJIUzI1NiJ9.eyJhdWQiOiJkOWRkOWQzYWU4MzhkYjE4ZDUxZjg4Y2Q1MGU0NzllNCIsIm5iZiI6MTcxOTAzNTYxMS40MjM0NDgsInN1YiI6IjY2MjZiM2ZkMjU4ODIzMDE2NDkxODliMSIsInNjb3BlcyI6WyJhcGlfcmVhZCJdLCJ2ZXJzaW9uIjoxfQ.yDtneQaozCSDZZgvaIF4Dufey-QNNqPcw_BTfdUR2J4"
            )
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (responseBody != null) {
                val providers = parseProviders(responseBody)
                val dbHelper = InfoDatabase(applicationContext)

                // Insert providers into database
                providers.forEach { provider ->
                    dbHelper.insertProvider(provider)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun parseProviders(jsonResponse: String): List<Provider> {
        val providers = mutableListOf<Provider>()
        val jsonElement = JsonParser.parseString(jsonResponse)
        val results = jsonElement.asJsonObject.getAsJsonArray("results")

        results.forEach { element ->
            val jsonObject = element.asJsonObject
            val providerName = jsonObject.get("provider_name").asString
            val providerId = jsonObject.get("provider_id").asInt
            providers.add(Provider(provider_name = providerName, provider_id = providerId))
        }

        return providers
    }
}
