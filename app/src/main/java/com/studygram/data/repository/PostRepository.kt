package com.studygram.data.repository

import androidx.lifecycle.LiveData
import com.studygram.data.local.PostDao
import com.studygram.data.model.Post
import com.studygram.data.remote.FirestoreManager
import com.studygram.utils.PreferenceManager

class PostRepository(
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

    suspend fun createPost(post: Post): Result<Unit> {
        return try {
            val firestoreResult = firestoreManager.savePost(post)
            if (firestoreResult.isFailure) {
                return Result.failure(firestoreResult.exceptionOrNull() ?: Exception("Failed to create post"))
            }

            postDao.insert(post)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePost(post: Post): Result<Unit> {
        return try {
            val updates = mapOf(
                "title" to post.title,
                "content" to post.content,
                "courseTag" to post.courseTag,
                "difficultyLevel" to post.difficultyLevel,
                "imageUrl" to (post.imageUrl ?: "")
            )

            val firestoreResult = firestoreManager.updatePost(post.id, updates)
            if (firestoreResult.isFailure) {
                return Result.failure(firestoreResult.exceptionOrNull() ?: Exception("Failed to update post"))
            }

            postDao.update(post.copy(lastUpdated = System.currentTimeMillis()))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            val firestoreResult = firestoreManager.deletePost(postId)
            if (firestoreResult.isFailure) {
                return Result.failure(firestoreResult.exceptionOrNull() ?: Exception("Failed to delete post"))
            }

            postDao.deleteById(postId)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            val post = postDao.getPostById(postId) ?: return Result.failure(Exception("Post not found"))

            if (post.isLikedByUser(userId)) {
                val firestoreResult = firestoreManager.unlikePost(postId, userId)
                if (firestoreResult.isFailure) {
                    return Result.failure(firestoreResult.exceptionOrNull() ?: Exception("Failed to unlike"))
                }

                val likedByList = post.getLikedByList().toMutableList()
                likedByList.remove(userId)
                val updatedPost = post.copy(
                    likes = post.likes - 1,
                    likedBy = likedByList.joinToString(","),
                    lastUpdated = System.currentTimeMillis()
                )
                postDao.update(updatedPost)
            } else {
                val firestoreResult = firestoreManager.likePost(postId, userId)
                if (firestoreResult.isFailure) {
                    return Result.failure(firestoreResult.exceptionOrNull() ?: Exception("Failed to like"))
                }

                val likedByList = post.getLikedByList().toMutableList()
                likedByList.add(userId)
                val updatedPost = post.copy(
                    likes = post.likes + 1,
                    likedBy = likedByList.joinToString(","),
                    lastUpdated = System.currentTimeMillis()
                )
                postDao.update(updatedPost)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun syncPosts(): Result<Unit> {
        return try {
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
            Result.failure(e)
        }
    }

    suspend fun refreshPosts(): Result<Unit> {
        return try {
            val result = firestoreManager.getAllPosts()
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Failed to refresh"))
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
            Result.failure(e)
        }
    }
}
