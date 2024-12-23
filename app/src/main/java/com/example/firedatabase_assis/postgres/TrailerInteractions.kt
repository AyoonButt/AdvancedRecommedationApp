package com.example.firedatabase_assis.postgres


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface TrailerInteractions {

    @PUT("/api/trailer-interactions/{postId}/timestamp")
    suspend fun updateInteractionTimestamp(
        @Path("postId") postId: Int,
        @Query("timestamp") timestamp: String
    ): Response<String>

    @POST("/api/trailer-interactions/save")
    suspend fun saveInteractionData(
        @Body interactionData: UserTrailerInteraction
    ): Response<String>

    @GET("/api/trailer-interactions/user/{userId}")
    suspend fun getTrailerInteractionsByUser(
        @Path("userId") userId: Int
    ): Response<List<UserTrailerInteraction>>

    @GET("/api/trailer-interactions/user/{userId}/post/{postId}")
    suspend fun getTrailerInteraction(
        @Path("userId") userId: Int,
        @Path("postId") postId: Int
    ): Response<UserTrailerInteraction?>

    @GET("/api/trailer-interactions/user/{userId}/liked")
    suspend fun getLikedTrailers(
        @Path("userId") userId: Int
    ): Response<List<PostEntity>>

    @GET("/api/trailer-interactions/user/{userId}/saved")
    suspend fun getSavedTrailers(
        @Path("userId") userId: Int
    ): Response<List<PostEntity>>

    @GET("/api/trailer-interactions/user/{userId}/commented")
    suspend fun getCommentMadeTrailers(
        @Path("userId") userId: Int
    ): Response<List<PostEntity>>
}
