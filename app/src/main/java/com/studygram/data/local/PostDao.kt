package com.studygram.data.local

import androidx.lifecycle.LiveData
import androidx.room.*
import com.studygram.data.model.Post

@Dao
interface PostDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(post: Post)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(posts: List<Post>)

    @Update
    suspend fun update(post: Post)

    @Delete
    suspend fun delete(post: Post)

    @Query("SELECT * FROM posts ORDER BY timestamp DESC")
    fun getAllPosts(): LiveData<List<Post>>

    @Query("SELECT * FROM posts WHERE id = :postId")
    suspend fun getPostById(postId: String): Post?

    @Query("SELECT * FROM posts WHERE id = :postId")
    fun getPostByIdLive(postId: String): LiveData<Post?>

    @Query("SELECT * FROM posts WHERE userId = :userId ORDER BY timestamp DESC")
    fun getPostsByUser(userId: String): LiveData<List<Post>>

    @Query("SELECT * FROM posts WHERE courseTag = :courseTag ORDER BY timestamp DESC")
    fun getPostsByCourse(courseTag: String): LiveData<List<Post>>

    @Query("SELECT * FROM posts WHERE lastUpdated > :timestamp ORDER BY timestamp DESC")
    suspend fun getPostsUpdatedAfter(timestamp: Long): List<Post>

    @Query("SELECT MAX(lastUpdated) FROM posts")
    suspend fun getLastUpdatedTimestamp(): Long?

    @Query("DELETE FROM posts")
    suspend fun deleteAll()

    @Query("DELETE FROM posts WHERE id = :postId")
    suspend fun deleteById(postId: String)

    @Query("SELECT DISTINCT courseTag FROM posts ORDER BY courseTag ASC")
    fun getAllCourseTags(): LiveData<List<String>>
}
