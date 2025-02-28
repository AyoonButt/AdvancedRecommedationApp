package com.example.firedatabase_assis.postgres


import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface Comments {
    @GET("/api/comments/post/{postId}")
    suspend fun getCommentsByPost(@Path("postId") postId: Int): Response<List<CommentDto>>

    @POST("/api/comments/insert")
    suspend fun addComment(@Body newComment: CommentDto): Response<CommentResponse>


    @GET("/api/comments/{commentId}/all-replies")
    suspend fun getAllReplies(
        @Path("commentId") commentId: Int,
        @Query("limit") limit: Int = 10,
        @Query("offset") offset: Int = 0
    ): Response<List<CommentDto>>


    @GET("/api/comments/{commentId}/parent-username")
    suspend fun getParentCommentUsername(@Path("commentId") commentId: Int): Response<ApiResponse>

    @GET("/api/comments/reply-counts")
    suspend fun getReplyCountsForComments(
        @Query("parentIds") parentIds: List<Int>
    ): Response<List<ReplyCountDto>>

    @GET("/api/comments/{commentId}/root-parent")
    suspend fun getRootParentComment(@Path("commentId") commentId: Int): Response<CommentDto>

    @GET("/api/comments/{userId}")
    suspend fun getCommentsByUserIdAndType(
        @Path("userId") userId: Int,
        @Query("commentType") commentType: String,
        @Query("page") page: Int = 0,
        @Query("pageSize") pageSize: Int = 10
    ): Response<List<CommentDto>>
}
