package com.example.firedatabase_assis.postgres

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface Providers {
    @POST("/api/providers/add")
    suspend fun addProviders(@Body providersList: List<SubscriptionProvider>): Response<String>


    @GET("/api/providers/filter")
    suspend fun filterProviders(@Query("query") query: String): Response<List<SubscriptionProvider>>

    @GET("/api/providers/ids")
    suspend fun getProviderIdsByNames(
        @Query("names") names: List<String> // Accept a list of provider names as a query parameter
    ): Response<List<Int>> // Return a list of provider IDs
}