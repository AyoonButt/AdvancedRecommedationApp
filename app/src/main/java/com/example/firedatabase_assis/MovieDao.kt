package com.example.firedatabase_assis

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update

@Dao
interface MovieDao {
    @Query("SELECT * FROM movie_table")
    fun getAll(): List<MovieTable>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(movie: MovieTable)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(movies: List<MovieTable>)

    @Update
    fun update(movie: MovieTable)

    @Delete
    fun delete(movie: MovieTable)
}