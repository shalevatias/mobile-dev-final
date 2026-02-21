package com.studygram.data.repository

import android.content.Context
import androidx.lifecycle.LiveData
import com.studygram.data.local.PostDao
import com.studygram.data.model.Post
import com.studygram.data.remote.FirestoreManager
import com.studygram.utils.NetworkManager
import com.studygram.utils.PreferenceManager
import java.net.UnknownHostException
import java.io.IOException

class PostRepository(
    private val context: Context,
    private val postDao: PostDao,
    private val firestoreManager: FirestoreManager,
    private val preferenceManager: PreferenceManager
) {

    fun getAllPosts(): LiveData<List<Post>> {
        return postDao.getAllPosts()
    }

    fun getPostsByUser(userId: String): LiveData<List<Post>> {
        return postDao.getPostsByUser(userId)
    }

    fun getPostsByCourse(courseTag: String): LiveData<List<Post>> {
        return postDao.getPostsByCourse(courseTag)
    }

    fun getPostById(postId: String): LiveData<Post?> {
        return postDao.getPostByIdLive(postId)
    }

    fun getAllCourseTags(): LiveData<List<String>> {
        return postDao.getAllCourseTags()
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

    suspend fun createPostLocalOnly(post: Post): Result<Post> {
        return try {
            // Generate a Firestore document ID if the post doesn't have one
            val postWithId = if (post.id.isEmpty()) {
                post.copy(id = firestoreManager.generatePostId())
            } else {
                post
            }

            // Save to local database only
            postDao.insert(postWithId)
            Result.success(postWithId)
        } catch (e: Exception) {
            Result.failure(getNetworkError(e))
        }
    }

    suspend fun syncPostToFirestore(post: Post): Result<Unit> {
        return try {
            if (!isNetworkAvailable()) {
                return Result.failure(Exception("No network connection"))
            }

            val firestoreResult = firestoreManager.savePost(post)
            if (firestoreResult.isFailure) {
                return Result.failure(
                    getNetworkError(firestoreResult.exceptionOrNull() ?: Exception("Failed to sync post"))
                )
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(getNetworkError(e))
        }
    }

    suspend fun createPost(post: Post): Result<Unit> {
        return try {
            // Generate a Firestore document ID if the post doesn't have one
            val postWithId = if (post.id.isEmpty()) {
                post.copy(id = firestoreManager.generatePostId())
            } else {
                post
            }

            // Save to local database first
            postDao.insert(postWithId)

            // Try to sync with Firestore if online
            if (isNetworkAvailable()) {
                val firestoreResult = firestoreManager.savePost(postWithId)
                if (firestoreResult.isFailure) {
                    return Result.failure(
                        getNetworkError(firestoreResult.exceptionOrNull() ?: Exception("Failed to create post"))
                    )
                }
            } else {
                // Offline: Post saved locally, will sync later
                return Result.success(Unit)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(getNetworkError(e))
        }
    }

    suspend fun updatePost(post: Post): Result<Unit> {
        return try {
            // Update local database first
            val updatedPost = post.copy(lastUpdated = System.currentTimeMillis())
            postDao.update(updatedPost)

            // Try to sync with Firestore if online
            if (isNetworkAvailable()) {
                val updates = mapOf(
                    "title" to post.title,
                    "content" to post.content,
                    "courseTag" to post.courseTag,
                    "difficultyLevel" to post.difficultyLevel,
                    "imageUrl" to (post.imageUrl ?: "")
                )

                val firestoreResult = firestoreManager.updatePost(post.id, updates)
                if (firestoreResult.isFailure) {
                    return Result.failure(
                        getNetworkError(firestoreResult.exceptionOrNull() ?: Exception("Failed to update post"))
                    )
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(getNetworkError(e))
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            // Check network for delete operations (cannot delete offline)
            if (!isNetworkAvailable()) {
                return Result.failure(Exception("Cannot delete posts while offline. Please check your internet connection."))
            }

            val firestoreResult = firestoreManager.deletePost(postId)
            if (firestoreResult.isFailure) {
                return Result.failure(
                    getNetworkError(firestoreResult.exceptionOrNull() ?: Exception("Failed to delete post"))
                )
            }

            postDao.deleteById(postId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(getNetworkError(e))
        }
    }

    suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            val post = postDao.getPostById(postId) ?: return Result.failure(Exception("Post not found"))

            // Update local database first for immediate UI feedback
            if (post.isLikedByUser(userId)) {
                val likedByList = post.getLikedByList().toMutableList()
                likedByList.remove(userId)
                val updatedPost = post.copy(
                    likes = post.likes - 1,
                    likedBy = likedByList.joinToString(","),
                    lastUpdated = System.currentTimeMillis()
                )
                postDao.update(updatedPost)

                // Sync with Firestore if online
                if (isNetworkAvailable()) {
                    val firestoreResult = firestoreManager.unlikePost(postId, userId)
                    if (firestoreResult.isFailure) {
                        // Revert local changes if Firestore fails
                        postDao.update(post)
                        return Result.failure(
                            getNetworkError(firestoreResult.exceptionOrNull() ?: Exception("Failed to unlike"))
                        )
                    }
                }
            } else {
                val likedByList = post.getLikedByList().toMutableList()
                likedByList.add(userId)
                val updatedPost = post.copy(
                    likes = post.likes + 1,
                    likedBy = likedByList.joinToString(","),
                    lastUpdated = System.currentTimeMillis()
                )
                postDao.update(updatedPost)

                // Sync with Firestore if online
                if (isNetworkAvailable()) {
                    val firestoreResult = firestoreManager.likePost(postId, userId)
                    if (firestoreResult.isFailure) {
                        // Revert local changes if Firestore fails
                        postDao.update(post)
                        return Result.failure(
                            getNetworkError(firestoreResult.exceptionOrNull() ?: Exception("Failed to like"))
                        )
                    }
                }
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(getNetworkError(e))
        }
    }

    suspend fun syncPosts(): Result<Unit> {
        return try {
            // Skip sync if offline
            if (!isNetworkAvailable()) {
                return Result.failure(Exception("Cannot sync while offline. Showing cached content."))
            }

            val lastSync = preferenceManager.lastSyncTimestamp
            val updatedPosts = if (lastSync == 0L) {
                firestoreManager.getAllPosts().getOrNull() ?: emptyList()
            } else {
                firestoreManager.getPostsUpdatedAfter(lastSync).getOrNull() ?: emptyList()
            }

            if (updatedPosts.isNotEmpty()) {
                postDao.insertAll(updatedPosts)
                val maxTimestamp = updatedPosts.maxOfOrNull { it.lastUpdated } ?: System.currentTimeMillis()
                preferenceManager.lastSyncTimestamp = maxTimestamp
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(getNetworkError(e))
        }
    }

    suspend fun refreshPosts(): Result<Unit> {
        return try {
            if (!isNetworkAvailable()) {
                return Result.failure(Exception("Cannot refresh while offline. Showing cached content."))
            }

            val result = firestoreManager.getAllPosts()
            if (result.isFailure) {
                android.util.Log.e("PostRepository", "Firestore fetch failed: ${result.exceptionOrNull()?.message}")
                return Result.failure(
                    getNetworkError(result.exceptionOrNull() ?: Exception("Failed to refresh"))
                )
            }

            val posts = result.getOrNull() ?: emptyList()
            postDao.deleteAll()
            postDao.insertAll(posts)

            if (posts.isNotEmpty()) {
                val maxTimestamp = posts.maxOfOrNull { it.lastUpdated } ?: System.currentTimeMillis()
                preferenceManager.lastSyncTimestamp = maxTimestamp
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("PostRepository", "Refresh failed with exception", e)
            Result.failure(getNetworkError(e))
        }
    }
}
