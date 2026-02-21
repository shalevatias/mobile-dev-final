package com.studygram.data.remote

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.studygram.data.model.Comment
import com.studygram.data.model.Post
import com.studygram.data.model.User
import com.studygram.utils.Constants
import kotlinx.coroutines.tasks.await

class FirestoreManager {

    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()

    init {
        // Disable Firestore offline persistence to avoid "client is offline" errors
        // We handle offline mode with Room database instead
        firestore.firestoreSettings = com.google.firebase.firestore.FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(false)
            .build()
    }

    suspend fun saveUser(user: User): Result<Unit> {
        return try {
            firestore.collection(Constants.USERS_COLLECTION)
                .document(user.id)
                .set(user.toFirestoreMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreManager", "Error saving user ${user.id}: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getUser(userId: String): Result<User> {
        return try {
            val document = firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .get()
                .await()
            val data = document.data ?: throw Exception("User not found")
            Result.success(User.fromFirestore(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val updateMap = updates.toMutableMap()
            updateMap[Constants.FIELD_LAST_UPDATED] = FieldValue.serverTimestamp()

            firestore.collection(Constants.USERS_COLLECTION)
                .document(userId)
                .set(updateMap, com.google.firebase.firestore.SetOptions.merge())
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreManager", "Error updating user $userId: ${e.message}", e)
            Result.failure(e)
        }
    }

    fun generatePostId(): String {
        return firestore.collection(Constants.POSTS_COLLECTION).document().id
    }

    suspend fun savePost(post: Post): Result<Unit> {
        return try {
            firestore.collection(Constants.POSTS_COLLECTION)
                .document(post.id)
                .set(post.toFirestoreMap())
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getPost(postId: String): Result<Post> {
        return try {
            val document = firestore.collection(Constants.POSTS_COLLECTION)
                .document(postId)
                .get()
                .await()
            val data = document.data ?: throw Exception("Post not found")
            Result.success(Post.fromFirestore(data))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllPosts(): Result<List<Post>> {
        return try {
            val snapshot = try {
                firestore.collection(Constants.POSTS_COLLECTION)
                    .orderBy(Constants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                    .get()
                    .await()
            } catch (e: Exception) {
                firestore.collection(Constants.POSTS_COLLECTION)
                    .get()
                    .await()
            }

            val posts = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.data?.let { Post.fromFirestore(it) }
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreManager", "Error parsing post ${doc.id}", e)
                    null
                }
            }

            val sortedPosts = posts.sortedByDescending { it.timestamp }
            Result.success(sortedPosts)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreManager", "Error fetching posts", e)
            Result.failure(e)
        }
    }

    suspend fun getPostsUpdatedAfter(timestamp: Long): Result<List<Post>> {
        return try {
            val snapshot = firestore.collection(Constants.POSTS_COLLECTION)
                .whereGreaterThan(Constants.FIELD_LAST_UPDATED, com.google.firebase.Timestamp(java.util.Date(timestamp)))
                .get()
                .await()
            val posts = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Post.fromFirestore(it) }
            }
            Result.success(posts)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updatePost(postId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            val updateMap = updates.toMutableMap()
            updateMap[Constants.FIELD_LAST_UPDATED] = FieldValue.serverTimestamp()

            firestore.collection(Constants.POSTS_COLLECTION)
                .document(postId)
                .update(updateMap)
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deletePost(postId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.POSTS_COLLECTION)
                .document(postId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun likePost(postId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.POSTS_COLLECTION)
                .document(postId)
                .update(
                    mapOf(
                        "likedBy" to FieldValue.arrayUnion(userId),
                        "likes" to FieldValue.increment(1),
                        Constants.FIELD_LAST_UPDATED to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unlikePost(postId: String, userId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.POSTS_COLLECTION)
                .document(postId)
                .update(
                    mapOf(
                        "likedBy" to FieldValue.arrayRemove(userId),
                        "likes" to FieldValue.increment(-1),
                        Constants.FIELD_LAST_UPDATED to FieldValue.serverTimestamp()
                    )
                )
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun saveComment(postId: String, comment: Comment): Result<Unit> {
        return try {
            firestore.collection(Constants.POSTS_COLLECTION)
                .document(postId)
                .collection(Constants.COMMENTS_COLLECTION)
                .document(comment.id)
                .set(comment.toFirestoreMap())
                .await()

            firestore.collection(Constants.POSTS_COLLECTION)
                .document(postId)
                .update(
                    mapOf(
                        "commentsCount" to FieldValue.increment(1),
                        Constants.FIELD_LAST_UPDATED to FieldValue.serverTimestamp()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getComments(postId: String): Result<List<Comment>> {
        return try {
            val snapshot = firestore.collection(Constants.POSTS_COLLECTION)
                .document(postId)
                .collection(Constants.COMMENTS_COLLECTION)
                .orderBy(Constants.FIELD_TIMESTAMP, Query.Direction.DESCENDING)
                .get()
                .await()
            val comments = snapshot.documents.mapNotNull { doc ->
                doc.data?.let { Comment.fromFirestore(it) }
            }
            Result.success(comments)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteComment(postId: String, commentId: String): Result<Unit> {
        return try {
            firestore.collection(Constants.POSTS_COLLECTION)
                .document(postId)
                .collection(Constants.COMMENTS_COLLECTION)
                .document(commentId)
                .delete()
                .await()

            firestore.collection(Constants.POSTS_COLLECTION)
                .document(postId)
                .update(
                    mapOf(
                        "commentsCount" to FieldValue.increment(-1),
                        Constants.FIELD_LAST_UPDATED to FieldValue.serverTimestamp()
                    )
                )
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
