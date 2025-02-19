package com.example.firedatabase_assis.login_setup

import android.util.Log
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.postgres.Providers
import com.example.firedatabase_assis.postgres.SubscriptionProvider
import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class ProvidersManager {

    private val retrofit = Retrofit.Builder()
        .baseUrl(BuildConfig.POSTRGRES_API_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val providersApi: Providers = retrofit.create(Providers::class.java)

    private val client = OkHttpClient()

    // Fetch providers and send to server
    suspend fun fetchAndSendProviders() {
        val request = Request.Builder()
            .url("https://api.themoviedb.org/3/watch/providers/movie?language=en-US")
            .get()
            .addHeader("accept", "application/json")
            .addHeader("Authorization", "Bearer ${BuildConfig.TMDB_API_KEY_BEARER}")
            .build()

        try {
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            if (responseBody != null) {
                val providers = parseProviders(responseBody)
                val addResponse = providersApi.addProviders(providers)


                if (addResponse.isSuccessful) {
                    Log.d("ProviderManager", "Providers sent successfully to the server.")

                } else {
                    Log.e(
                        "ProviderManager",
                        "Failed to send providers: ${addResponse.errorBody()?.string()}"
                    )
                }
            }
        } catch (e: IOException) {
            Log.e("ProviderManager", "Error fetching providers", e)
        }
    }

    // Parse JSON response to SubscriptionProvider objects
    private fun parseProviders(jsonResponse: String): List<SubscriptionProvider> {
        val providers = mutableListOf<SubscriptionProvider>()
        val jsonElement = JsonParser.parseString(jsonResponse)
        val results = jsonElement.asJsonObject.getAsJsonArray("results")

        results.forEach { element ->
            val jsonObject = element.asJsonObject
            val providerName = jsonObject.get("provider_name").asString
            val providerId = jsonObject.get("provider_id").asInt
            providers.add(
                SubscriptionProvider(providerId = providerId, providerName = providerName)
            )
        }

        return providers
    }
}
