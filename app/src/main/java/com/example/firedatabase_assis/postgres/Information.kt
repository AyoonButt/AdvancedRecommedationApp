package com.example.firedatabase_assis.postgres


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query

interface Information {
    @POST("api/info/save")
    suspend fun saveInfo(
        @Body infoDto: InfoDto
    ): Response<ApiResponse>

    @POST("api/info/create")
    suspend fun createDto(
        @Query("tmdbId") tmdbId: Int,
        @Query("type") type: String,
        @Body jsonData: String
    ): Response<Any>  // Will be either MediaDto or PersonDto

}