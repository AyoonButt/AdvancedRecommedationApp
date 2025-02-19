package com.example.firedatabase_assis.workers

import android.util.Log
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.postgres.UserPreferencesDto
import com.example.firedatabase_assis.postgres.Users
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


// Retrofit setup for API calls
private val apiRetrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.POSTRGRES_API_URL) // Replace with your custom API's base URL
    .addConverterFactory(GsonConverterFactory.create())
    .build()

private val usersApi = apiRetrofit.create(Users::class.java)


suspend fun getUserPreferences(userId: Int): UserPreferencesDto? {
    return try {
        // Log the request attempt with userId
        Log.d("API Request", "Fetching user preferences for userId: $userId")

        val response = usersApi.getUserPreferences(userId)

        if (response.isSuccessful) {
            // Log successful response
            Log.d("API Success", "User preferences fetched successfully for userId: $userId")
            response.body()  // Return the UserPreferencesDto object if successful
        } else {
            // Log error response
            Log.e(
                "API Error",
                "Failed to fetch user preferences for userId: $userId, Error: ${
                    response.errorBody()?.string()
                }"
            )
            null  // Return null if response is unsuccessful
        }
    } catch (e: Exception) {
        // Log the exception with stack trace
        Log.e(
            "API Exception",
            "Exception occurred while fetching user preferences: ${e.localizedMessage}"
        )
        e.printStackTrace()
        null  // Return null if an exception occurs
    }
}


suspend fun getProvidersByPriority(userId: Int): List<Int> {
    // Call the Retrofit interface method to get the provider IDs sorted by priority
    val response = usersApi.getProvidersByPriority(userId)

    // Check if the response is successful and return the result, or handle error
    return if (response.isSuccessful) {
        response.body() ?: emptyList() // Return the list of provider IDs or an empty list if null
    } else {
        // Handle the error appropriately (e.g., throw an exception, log the error)
        throw Exception("Failed to fetch provider IDs by priority: ${response.message()}")
    }
}

