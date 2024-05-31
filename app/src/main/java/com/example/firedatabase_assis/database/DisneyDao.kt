package com.example.firedatabase_assis.database

import androidx.room.Dao
import androidx.room.Query
import com.example.firedatabase_assis.home_page.Post

@Dao
interface DisneyDao : GenericDao<DisneyMovieTable> {
    @Query("SELECT * FROM DisneyMovieTable")
    suspend fun getAllMovies(): List<Post>
}