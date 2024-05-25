package com.example.firedatabase_assis.database

import androidx.room.Insert
import androidx.room.OnConflictStrategy

interface GenericDao<T> {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<T>)
}