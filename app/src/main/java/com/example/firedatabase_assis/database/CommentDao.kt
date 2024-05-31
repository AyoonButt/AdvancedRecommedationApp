package com.example.firedatabase_assis.database


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.firedatabase_assis.home_page.Comment

@Dao
interface CommentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertComment(comment: Comment)

    @Query("SELECT * FROM comments WHERE postId = :postId")
    suspend fun getCommentsForPost(postId: Int): List<Comment>
}
