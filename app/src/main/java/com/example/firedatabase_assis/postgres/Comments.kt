package com.example.firedatabase_assis.postgres


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface Comments {
    @GET("/api/comments/post/{postId}")
    suspend fun getCommentsByPost(@Path("postId") postId: Int): Response<List<CommentEntity>>

    @POST("/api/comments")
    suspend fun addComment(@Body newComment: CommentEntity): Response<String>

    @POST("/api/comments/{userId}/comments/{commentId}/replies")
    suspend fun addReply(
        @Path("userId") userId: Int,
        @Path("commentId") commentId: Int,
        @Body replyRequest: ReplyRequest
    ): Response<String>


    @GET("/api/comments/{commentId}/all-replies")
    suspend fun getAllReplies(
        @Path("commentId") commentId: Int,
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): Response<List<CommentEntity>>
}
