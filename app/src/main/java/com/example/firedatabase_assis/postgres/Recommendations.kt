package com.example.firedatabase_assis.postgres

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface Recommendations {
    @GET("api/recommendations/{userId}")
    suspend fun getRecommendations(
        @Path("userId") userId: Int,
        @Query("contentType") contentType: String? = null,
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<List<PostDto>>
}

