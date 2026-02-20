package com.studygram.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.studygram.data.local.CommentDao
import com.studygram.data.local.PostDao
import com.studygram.data.model.Comment
import com.studygram.data.remote.FirestoreManager
import com.studygram.utils.NetworkManager
import java.net.UnknownHostException
import java.io.IOException

class CommentRepository(
    private val context: Context,
    private val commentDao: CommentDao,
    private val postDao: PostDao,
    private val firestoreManager: FirestoreManager
) {

    fun getCommentsByPost(postId: String): LiveData<List<Comment>> {
        return commentDao.getCommentsByPost(postId)
    }

    private fun isNetworkAvailable(): Boolean {
        return NetworkManager.isNetworkAvailable(context)
    }

    private fun getNetworkError(e: Throwable): Exception {
        return when (e) {
            is UnknownHostException, is IOException ->
                Exception("No internet connection. Changes will sync when you're back online.")
            is Exception -> e
            else -> Exception(e.message ?: "An error occurred", e)
        }
    }

    suspend fun addComment(postId: String, comment: Comment): Result<Unit> {
        return try {
            // Save locally first
            commentDao.insert(comment)

            // Update post comment count locally
            val post = postDao.getPostById(postId)
            if (post != null) {
                val updatedPost = post.copy(
                    commentsCount = post.commentsCount + 1,
                    lastUpdated = System.currentTimeMillis()
                )
                postDao.update(updatedPost)
            }

            // Sync with Firestore if online
            if (isNetworkAvailable()) {
                val firestoreResult = firestoreManager.saveComment(postId, comment)
                if (firestoreResult.isFailure) {
                    return Result.failure(
                        getNetworkError(firestoreResult.exceptionOrNull() ?: Exception("Failed to add comment"))
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(getNetworkError(e))
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return try {
            // Check network for delete operations
            if (!isNetworkAvailable()) {
                return Result.failure(Exception("Cannot delete comments while offline. Please check your internet connection."))
            }

            val firestoreResult = firestoreManager.deleteComment(postId, commentId)
            if (firestoreResult.isFailure) {
                return Result.failure(
                    getNetworkError(firestoreResult.exceptionOrNull() ?: Exception("Failed to delete comment"))
                )
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
            Result.failure(getNetworkError(e))
        }
    }

    suspend fun syncComments(postId: String): Result<Unit> {
        return try {
            // Skip sync if offline
            if (!isNetworkAvailable()) {
                return Result.failure(Exception("Cannot sync while offline. Showing cached content."))
            }

            val result = firestoreManager.getComments(postId)
            if (result.isFailure) {
                return Result.failure(
                    getNetworkError(result.exceptionOrNull() ?: Exception("Failed to sync comments"))
                )
            }

            val comments = result.getOrNull() ?: emptyList()
            commentDao.deleteCommentsByPost(postId)
            commentDao.insertAll(comments)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(getNetworkError(e))
        }
    }
}
