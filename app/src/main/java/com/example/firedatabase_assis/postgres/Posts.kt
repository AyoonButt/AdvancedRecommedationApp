package com.example.firedatabase_assis.postgres


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface Posts {

    @POST("/api/posts/batch/{mediaType}/{language}/{providerId}")
    suspend fun addPosts(
        @Path("mediaType") mediaType: String,
        @Path("language") language: String,
        @Path("providerId") providerId: Int,
        @Body dataList: List<PostDto>
    ): Response<String>

    @GET("/api/posts/list")
    suspend fun getPosts(
        @Query("limit") limit: Int,
        @Query("offset") offset: Int
    ): Response<List<PostDto>>

    @PUT("/api/posts/{postId}/like")
    suspend fun updateLikeCount(@Path("postId") postId: Int): Response<String>

    @PUT("/api/posts/{postId}/trailer-like")
    suspend fun updateTrailerLikeCount(@Path("postId") postId: Int): Response<String>

    @GET("api/posts/paged")
    suspend fun getPagedPostDtos(
        @Query("interactionIds") interactionIds: List<Int>,
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 20
    ): Response<List<PostDto>>

    @GET("/api/posts/language/count/{language}")
    suspend fun getPostCountByLanguage(
        @Path("language") language: String
    ): Response<Map<String, Int>>

    @GET("/api/posts/after/{timestamp}")
    suspend fun getPostsAfterTimestamp(
        @Path("timestamp") timestamp: Long
    ): Response<List<PostDto>>

    @GET("/api/posts/provider/{tmdbId}")
    suspend fun getProviderNameByTmdbId(@Path("tmdbId") tmdbId: Int): Response<Map<String, String?>>

}
