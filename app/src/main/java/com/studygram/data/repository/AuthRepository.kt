package com.studygram.data.repository

import android.content.Context
import com.studygram.data.local.UserDao
import com.studygram.data.model.User
import com.studygram.data.remote.FirebaseAuthManager
import com.studygram.data.remote.FirestoreManager
import com.studygram.utils.NetworkManager
import com.studygram.utils.PreferenceManager
import java.net.UnknownHostException
import java.io.IOException

class AuthRepository(
    private val context: Context,
    private val authManager: FirebaseAuthManager,
    private val firestoreManager: FirestoreManager,
    private val userDao: UserDao,
    private val preferenceManager: PreferenceManager
) {

    val currentUserId: String?
        get() = authManager.currentUserId

    val isUserLoggedIn: Boolean
        get() = authManager.isUserLoggedIn

    private fun isNetworkAvailable(): Boolean {
        return NetworkManager.isNetworkAvailable(context)
    }

    private fun getNetworkError(e: Throwable): Exception {
        return when (e) {
            is UnknownHostException, is IOException ->
                Exception("No internet connection. Authentication requires internet.")
            is Exception -> e
            else -> Exception(e.message ?: "An error occurred", e)
        }
    }

    suspend fun signUp(email: String, password: String, username: String): Result<User> {
        return try {
            // Check network availability
            if (!isNetworkAvailable()) {
                return Result.failure(Exception("Cannot sign up while offline. Please check your internet connection."))
            }

            val authResult = authManager.signUp(email, password, username)
            if (authResult.isFailure) {
                return Result.failure(
                    getNetworkError(authResult.exceptionOrNull() ?: Exception("Sign up failed"))
                )
            }

            val firebaseUser = authResult.getOrNull()!!
            val user = User(
                id = firebaseUser.uid,
                email = email,
                username = username,
                profileImageUrl = null
            )

            val firestoreResult = firestoreManager.saveUser(user)
            if (firestoreResult.isFailure) {
                return Result.failure(
                    getNetworkError(firestoreResult.exceptionOrNull() ?: Exception("Failed to save user"))
                )
            }

            userDao.insert(user)
            preferenceManager.userId = user.id

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(getNetworkError(e))
        }
    }

    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            // Check network availability for authentication
            if (!isNetworkAvailable()) {
                return Result.failure(Exception("Cannot sign in while offline. Please check your internet connection."))
            }

            val authResult = authManager.signIn(email, password)
            if (authResult.isFailure) {
                return Result.failure(
                    getNetworkError(authResult.exceptionOrNull() ?: Exception("Sign in failed"))
                )
            }

            val firebaseUser = authResult.getOrNull()!!
            val userId = firebaseUser.uid

            // Try to get user from Firestore
            val firestoreResult = firestoreManager.getUser(userId)
            val user = if (firestoreResult.isSuccess) {
                // Successfully got user from Firestore
                val firestoreUser = firestoreResult.getOrNull()!!
                userDao.insert(firestoreUser)
                firestoreUser
            } else {
                // Firestore failed (offline mode), try to get cached user
                val cachedUser = userDao.getUserById(userId)
                if (cachedUser != null) {
                    // Use cached user data
                    cachedUser
                } else {
                    // No cached data and Firestore failed - create minimal user
                    val minimalUser = User(
                        id = userId,
                        email = email,
                        username = email.substringBefore("@"),
                        profileImageUrl = null
                    )
                    userDao.insert(minimalUser)
                    minimalUser
                }
            }

            preferenceManager.userId = user.id
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(getNetworkError(e))
        }
    }

    suspend fun signOut() {
        authManager.signOut()
        preferenceManager.clear()
        userDao.deleteAll()
    }

    suspend fun getCurrentUser(): User? {
        val userId = currentUserId ?: return null
        return userDao.getUserById(userId)
    }

    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            // Update local database first
            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(
                    username = updates["username"] as? String ?: user.username,
                    profileImageUrl = updates["profileImageUrl"] as? String ?: user.profileImageUrl,
                    yearOfStudy = updates["yearOfStudy"] as? String ?: user.yearOfStudy,
                    degree = updates["degree"] as? String ?: user.degree,
                    lastUpdated = System.currentTimeMillis()
                )
                userDao.update(updatedUser)

                // Sync with Firestore if online
                if (isNetworkAvailable()) {
                    // First ensure user document exists in Firestore
                    val getUserResult = firestoreManager.getUser(userId)
                    if (getUserResult.isFailure) {
                        // User doesn't exist in Firestore, create it
                        android.util.Log.d("AuthRepository", "User doesn't exist in Firestore, creating...")
                        val saveResult = firestoreManager.saveUser(updatedUser)
                        if (saveResult.isFailure) {
                            return Result.failure(
                                getNetworkError(saveResult.exceptionOrNull() ?: Exception("Failed to create user"))
                            )
                        }
                    } else {
                        // User exists, update it
                        val firestoreResult = firestoreManager.updateUser(userId, updates)
                        if (firestoreResult.isFailure) {
                            return Result.failure(
                                getNetworkError(firestoreResult.exceptionOrNull() ?: Exception("Update failed"))
                            )
                        }
                    }
                }
            } else {
                return Result.failure(Exception("User not found in local database"))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("AuthRepository", "Error updating user profile", e)
            Result.failure(getNetworkError(e))
        }
    }
}
