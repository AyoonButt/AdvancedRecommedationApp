package com.example.firedatabase_assis.postgres


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface Posts {

    @POST("/api/posts/add")
    suspend fun addPosts(
        @Query("mediaType") mediaType: String,
        @Query("providerId") providerId: Int,
        @Body dataList: List<PostDto>
    ): Response<String>

    @GET("/api/posts/list")
    suspend fun getPosts(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<List<PostDto>>

    @PUT("/api/posts//{postId}/like")
    suspend fun updateLikeCount(@Path("postId") postId: Int): Response<String>

    @PUT("/api/posts/{postId}/trailer-like")
    suspend fun updateTrailerLikeCount(@Path("postId") postId: Int): Response<String>

    @GET("/api/posts/videos")
    suspend fun getVideos(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<List<VideoDto>>

    @GET("/api/posts/{postId}")
    suspend fun getPostIdByTmdbId(@Query("tmdbId") tmdbId: Int): Response<Int?>
}
