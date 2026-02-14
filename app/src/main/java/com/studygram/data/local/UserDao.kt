package com.studygram.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.studygram.data.model.User

@Dao
interface UserDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<User>)

    @Update
    suspend fun update(user: User)

    @Delete
    suspend fun delete(user: User)

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("SELECT * FROM users WHERE id = :userId")
    fun getUserByIdLive(userId: String): LiveData<User?>

    @Query("SELECT * FROM users")
    fun getAllUsers(): LiveData<List<User>>

    @Query("DELETE FROM users")
    suspend fun deleteAll()

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): User?
}
