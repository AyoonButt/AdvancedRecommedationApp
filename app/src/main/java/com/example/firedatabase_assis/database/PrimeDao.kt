package com.example.firedatabase_assis.database

import androidx.room.Dao
import androidx.room.Query
import com.example.firedatabase_assis.home_page.Post


@Dao
interface PrimeDao : GenericDao<PrimeMovieTable> {
    @Query("SELECT * FROM PrimeMovieTable LIMIT :limit OFFSET :offset")
    suspend fun getMoviesWithPagination(limit: Int, offset: Int): List<Post>
}
