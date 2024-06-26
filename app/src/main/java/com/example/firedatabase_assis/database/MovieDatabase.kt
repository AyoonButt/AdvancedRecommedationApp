package com.example.firedatabase_assis.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.firedatabase_assis.home_page.Comment

@Database(
    entities = [MovieTable::class, DisneyMovieTable::class, PrimeMovieTable::class, Comment::class],
    version = 2,
    exportSchema = false
)
abstract class MovieDatabase : RoomDatabase() {
    abstract fun primeMovieDao(): PrimeDao
    abstract fun disneyMovieDao(): DisneyDao

    abstract fun commentDao(): CommentDao

    companion object {
        @Volatile
        private var INSTANCE: MovieDatabase? = null

        fun getDatabase(context: Context): MovieDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MovieDatabase::class.java,
                    "movie_database"
                )
                    // Use destructive migration from version 1
                    .fallbackToDestructiveMigrationFrom(1)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
