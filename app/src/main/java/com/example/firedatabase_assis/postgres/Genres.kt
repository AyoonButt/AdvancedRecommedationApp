package com.example.firedatabase_assis.postgres


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface Genres {
    @GET("/api/genres/filter")
    suspend fun filterGenres(@Query("query") query: String): Response<List<GenreEntity>>

    @GET("/api/genres/filter/avoid")
    suspend fun filterAvoidGenres(@Query("query") query: String): Response<List<GenreEntity>>

    @POST("/api/genres/add")
    suspend fun addGenre(@Body genre: GenreEntity): Response<String>

    @POST("/api/genres/insertDefaults")
    suspend fun insertDefaultGenres(): Response<String>

    @GET("/api/genres/ids")
    suspend fun getGenreIdsByNames(
        @Query("names") names: List<String>
    ): Response<List<Int>>

}
