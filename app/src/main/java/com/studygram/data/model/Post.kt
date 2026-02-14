package com.studygram.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Entity(tableName = "posts")
data class Post(
    @PrimaryKey
    val id: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorImageUrl: String? = null,
    val title: String = "",
    val content: String = "",
    val courseTag: String = "",
    val difficultyLevel: String = "",
    val imageUrl: String? = null,
    val likes: Int = 0,
    val likedBy: String = "",
    val commentsCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun getLikedByList(): List<String> {
        return if (likedBy.isEmpty()) emptyList() else likedBy.split(",")
    }

    fun isLikedByUser(userId: String): Boolean {
        return getLikedByList().contains(userId)
    }

    fun toFirestoreMap(): Map<String, Any?> {
        return hashMapOf(
            "id" to id,
            "userId" to userId,
            "authorName" to authorName,
            "authorImageUrl" to authorImageUrl,
            "title" to title,
            "content" to content,
            "courseTag" to courseTag,
            "difficultyLevel" to difficultyLevel,
            "imageUrl" to imageUrl,
            "likes" to likes,
            "likedBy" to getLikedByList(),
            "commentsCount" to commentsCount,
            "timestamp" to Timestamp(java.util.Date(timestamp)),
            "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
    }

    companion object {
        fun fromFirestore(data: Map<String, Any>): Post {
            val likedByList = (data["likedBy"] as? List<*>)?.filterIsInstance<String>() ?: emptyList()
            return Post(
                id = data["id"] as? String ?: "",
                userId = data["userId"] as? String ?: "",
                authorName = data["authorName"] as? String ?: "",
                authorImageUrl = data["authorImageUrl"] as? String,
                title = data["title"] as? String ?: "",
                content = data["content"] as? String ?: "",
                courseTag = data["courseTag"] as? String ?: "",
                difficultyLevel = data["difficultyLevel"] as? String ?: "",
                imageUrl = data["imageUrl"] as? String,
                likes = (data["likes"] as? Long)?.toInt() ?: 0,
                likedBy = likedByList.joinToString(","),
                commentsCount = (data["commentsCount"] as? Long)?.toInt() ?: 0,
                timestamp = (data["timestamp"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                lastUpdated = (data["lastUpdated"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
            )
        }
    }
}
