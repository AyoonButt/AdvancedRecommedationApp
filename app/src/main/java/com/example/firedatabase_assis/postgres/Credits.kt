package com.example.firedatabase_assis.postgres


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

interface Credits {
    @POST("/api/credits/post/{tmdbId}")
    suspend fun insertCredits(
        @Path("tmdbId") tmdbId: Int,
        @Body creditsJson: String
    ): Response<String>

    @GET("/api/credits/post/{postId}")
    suspend fun getCredits(@Path("postId") postId: Int): Response<Map<String, List<Map<String, Any?>>>>
}
