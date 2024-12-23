package com.example.firedatabase_assis.workers

import android.util.Log
import com.example.firedatabase_assis.BuildConfig
import com.example.firedatabase_assis.postgres.UserParams
import com.example.firedatabase_assis.postgres.Users
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory


// Retrofit setup for API calls
private val apiRetrofit = Retrofit.Builder()
    .baseUrl(BuildConfig.POSTRGRES_API_URL) // Replace with your custom API's base URL
    .addConverterFactory(GsonConverterFactory.create())
    .build()

private val usersApi = apiRetrofit.create(Users::class.java)


suspend fun fetchUserParams(userId: Int): UserParams? {
    return try {
        val response = usersApi.fetchUserParams(userId)
        if (response.isSuccessful) {
            response.body()  // Return the UserParams object if successful
        } else {
            Log.e(
                "API Error",
                "Failed to fetch user params for userId: $userId, Error: ${
                    response.errorBody()?.string()
                }"
            )
            null  // Return null if response is unsuccessful
        }
    } catch (e: Exception) {
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

