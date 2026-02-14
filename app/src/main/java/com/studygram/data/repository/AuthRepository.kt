package com.studygram.data.repository

import com.studygram.data.local.UserDao
import com.studygram.data.model.User
import com.studygram.data.remote.FirebaseAuthManager
import com.studygram.data.remote.FirestoreManager
import com.studygram.utils.PreferenceManager

class AuthRepository(
    private val authManager: FirebaseAuthManager,
    private val firestoreManager: FirestoreManager,
    private val userDao: UserDao,
    private val preferenceManager: PreferenceManager
) {

    val currentUserId: String?
        get() = authManager.currentUserId

    val isUserLoggedIn: Boolean
        get() = authManager.isUserLoggedIn

    suspend fun signUp(email: String, password: String, username: String): Result<User> {
        return try {
            val authResult = authManager.signUp(email, password, username)
            if (authResult.isFailure) {
                return Result.failure(authResult.exceptionOrNull() ?: Exception("Sign up failed"))
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
                return Result.failure(firestoreResult.exceptionOrNull() ?: Exception("Failed to save user"))
            }

            userDao.insert(user)
            preferenceManager.userId = user.id

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
            val authResult = authManager.signIn(email, password)
            if (authResult.isFailure) {
                return Result.failure(authResult.exceptionOrNull() ?: Exception("Sign in failed"))
            }

            val firebaseUser = authResult.getOrNull()!!
            val userId = firebaseUser.uid

            val firestoreResult = firestoreManager.getUser(userId)
            if (firestoreResult.isFailure) {
                return Result.failure(firestoreResult.exceptionOrNull() ?: Exception("Failed to get user"))
            }

            val user = firestoreResult.getOrNull()!!
            userDao.insert(user)
            preferenceManager.userId = user.id

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
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
            val firestoreResult = firestoreManager.updateUser(userId, updates)
            if (firestoreResult.isFailure) {
                return Result.failure(firestoreResult.exceptionOrNull() ?: Exception("Update failed"))
            }

            val user = userDao.getUserById(userId)
            if (user != null) {
                val updatedUser = user.copy(
                    username = updates["username"] as? String ?: user.username,
                    profileImageUrl = updates["profileImageUrl"] as? String ?: user.profileImageUrl,
                    lastUpdated = System.currentTimeMillis()
                )
                userDao.update(updatedUser)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
