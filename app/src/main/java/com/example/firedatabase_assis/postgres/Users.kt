package com.example.firedatabase_assis.postgres


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface Users {
    @GET("/api/users/username")
    suspend fun getUserByUsername(
        @Query("username") username: String
    ): Response<UserEntity>

    @POST("/api/users/add")
    suspend fun addUser(
        @Body userRequest: UserRequest
    ): Response<Int>

    @POST("/api/users/check-credentials")
    suspend fun checkUserCredentials(
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<Boolean>

    @PUT("/api/users/{username}/login")
    suspend fun updateRecentLogin(
        @Path("username") username: String,
        @Query("timestamp") timestamp: String
    ): Response<ApiResponse>

    @PUT("/api/users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: Int,
        @Body update: UserUpdate
    ): Response<UserEntity>

    @GET("/api/users/{userId}/preferences")
    suspend fun getUserPreferences(
        @Path("userId") userId: Int
    ): Response<UserPreferencesDto>

    @GET("/api/users/{userId}/providers")
    suspend fun getProvidersByPriority(
        @Path("userId") userId: Int
    ): Response<List<Int>>


}