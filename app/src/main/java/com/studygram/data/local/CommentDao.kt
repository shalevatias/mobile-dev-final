package com.studygram.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.studygram.data.model.Comment

@Dao
interface CommentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: Comment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<Comment>)

    @Update
    suspend fun update(comment: Comment)

    @Delete
    suspend fun delete(comment: Comment)

    @Query("SELECT * FROM comments WHERE postId = :postId ORDER BY timestamp DESC")
    fun getCommentsByPost(postId: String): LiveData<List<Comment>>

    @Query("SELECT * FROM comments WHERE id = :commentId")
    suspend fun getCommentById(commentId: String): Comment?

    @Query("SELECT * FROM comments WHERE userId = :userId ORDER BY timestamp DESC")
    fun getCommentsByUser(userId: String): LiveData<List<Comment>>

    @Query("DELETE FROM comments WHERE postId = :postId")
    suspend fun deleteCommentsByPost(postId: String)

    @Query("DELETE FROM comments WHERE id = :commentId")
    suspend fun deleteById(commentId: String)

    @Query("DELETE FROM comments")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM comments WHERE postId = :postId")
    suspend fun getCommentsCount(postId: String): Int
}
