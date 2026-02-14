package com.studygram.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Entity(tableName = "comments")
data class Comment(
    @PrimaryKey
    val id: String = "",
    val postId: String = "",
    val userId: String = "",
    val authorName: String = "",
    val authorImageUrl: String? = null,
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any?> {
        return hashMapOf(
            "id" to id,
            "postId" to postId,
            "userId" to userId,
            "authorName" to authorName,
            "authorImageUrl" to authorImageUrl,
            "content" to content,
            "timestamp" to Timestamp(java.util.Date(timestamp))
        )
    }

    companion object {
        fun fromFirestore(data: Map<String, Any>): Comment {
            return Comment(
                id = data["id"] as? String ?: "",
                postId = data["postId"] as? String ?: "",
                userId = data["userId"] as? String ?: "",
                authorName = data["authorName"] as? String ?: "",
                authorImageUrl = data["authorImageUrl"] as? String,
                content = data["content"] as? String ?: "",
                timestamp = (data["timestamp"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
            )
        }
    }
}
