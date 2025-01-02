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
    ): Response<UserEntity?>

    @POST("/api/users/add")
    suspend fun addUser(
        @Body userRequest: UserRequest
    ): Response<Int>

    @POST("/api/users/check-credentials")
    suspend fun checkUserCredentials(
        @Query("username") username: String,
        @Query("password") password: String
    ): Response<Boolean>

    @PUT("/api/users/update-login")
    suspend fun updateRecentLogin(
        @Query("username") username: String
    ): Response<Void>

    @GET("/api/users/params/{userId}")
    suspend fun fetchUserParams(
        @Path("userId") userId: Int
    ): Response<UserParams?>

    @GET("/api/users/info/{userId}")
    suspend fun getUserInfo(
        @Path("userId") userId: Int
    ): Response<UserPreferencesDto>

    @PUT("/api/users/{userId}")
    suspend fun updateUser(
        @Path("userId") userId: Int,
        @Body userUpdateRequest: UserUpdateRequest
    ): Response<String>

    @GET("/api/users/{userId}/providers")
    suspend fun getProvidersByPriority(
        @Path("userId") userId: Int
    ): Response<List<Int>>  // Returns list of provider IDs sorted by priority
}
