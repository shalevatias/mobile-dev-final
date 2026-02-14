package com.studygram.data.repository

import androidx.lifecycle.LiveData
import com.studygram.data.local.UserDao
import com.studygram.data.model.User
import com.studygram.data.remote.FirestoreManager

class UserRepository(
    private val userDao: UserDao,
    private val firestoreManager: FirestoreManager
) {

    fun getUserById(userId: String): LiveData<User?> {
        return userDao.getUserByIdLive(userId)
    }

    suspend fun getUserByIdSync(userId: String): User? {
        return userDao.getUserById(userId)
    }

    suspend fun syncUser(userId: String): Result<Unit> {
        return try {
            val result = firestoreManager.getUser(userId)
            if (result.isFailure) {
                return Result.failure(result.exceptionOrNull() ?: Exception("Failed to sync user"))
            }

            val user = result.getOrNull()!!
            userDao.insert(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUser(user: User): Result<Unit> {
        return try {
            userDao.update(user)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
