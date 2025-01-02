package com.example.firedatabase_assis.postgres


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface PostInteractions {
    @POST("/api/interactions/save")
    suspend fun saveInteractionData(@Body interactionData: UserPostInteractionDto): Response<String>

    @PUT("/api/interactions/updateTimestamp")
    suspend fun updateInteractionTimestamp(
        @Query("postId") postId: Int,
        @Query("userId") userId: Int,
        @Query("timestamp") timestamp: Long
    ): Response<String>

    @GET("/api/interactions/user/{userId}")
    suspend fun getPostInteractionsByUser(@Path("userId") userId: Int): Response<List<UserPostInteractionDto>>

    @GET("/api/interactions/user/{userId}/post/{postId}")
    suspend fun getPostInteraction(
        @Path("userId") userId: Int,
        @Path("postId") postId: Int
    ): Response<UserPostInteractionDto>

    @GET("/api/interactions/liked/user/{userId}")
    suspend fun getLikedPosts(@Path("userId") userId: Int): Response<List<Int>>

    @GET("/api/interactions/saved/user/{userId}")
    suspend fun getSavedPosts(@Path("userId") userId: Int): Response<List<Int>>

    @GET("/api/interactions/commented/user/{userId}")
    suspend fun getCommentMadePosts(@Path("userId") userId: Int): Response<List<Int>>
}
