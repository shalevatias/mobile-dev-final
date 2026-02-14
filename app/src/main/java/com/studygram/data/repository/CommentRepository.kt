package com.studygram.data.repository

import androidx.lifecycle.LiveData
import com.studygram.data.local.CommentDao
import com.studygram.data.local.PostDao
import com.studygram.data.model.Comment
import com.studygram.data.remote.FirestoreManager

class CommentRepository(
    private val commentDao: CommentDao,
    private val postDao: PostDao,
    private val firestoreManager: FirestoreManager
) {

    fun getCommentsByPost(postId: String): LiveData<List<Comment>> {
        return commentDao.getCommentsByPost(postId)
    }

    suspend fun addComment(postId: String, comment: Comment): Result<Unit> {
        return try {
            val firestoreResult = firestoreManager.saveComment(postId, comment)
            if (firestoreResult.isFailure) {
                return Result.failure(firestoreResult.exceptionOrNull() ?: Exception("Failed to add comment"))
            }

            commentDao.insert(comment)

            val post = postDao.getPostById(postId)
            if (post != null) {
                val updatedPost = post.copy(
                    commentsCount = post.commentsCount + 1,
                    lastUpdated = System.currentTimeMillis()
                )
                postDao.update(updatedPost)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return try {
            val firestoreResult = firestoreManager.deleteComment(postId, commentId)
            if (firestoreResult.isFailure) {
                return Result.failure(firestoreResult.exceptionOrNull() ?: Exception("Failed to delete comment"))
            }

            commentDao.deleteById(commentId)

            val post = postDao.getPostById(postId)
            if (post != null) {
                val updatedPost = post.copy(
                    commentsCount = maxOf(0, post.commentsCount - 1),
                    lastUpdated = System.currentTimeMillis()
                )
                postDao.update(updatedPost)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncComments(postId: String): Result<Unit> {
        return try {
            val result = firestoreManager.getComments(postId)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Failed to sync comments"))
            }

            val comments = result.getOrNull() ?: emptyList()
            commentDao.deleteCommentsByPost(postId)
            commentDao.insertAll(comments)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
