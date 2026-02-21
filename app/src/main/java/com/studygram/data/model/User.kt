package com.studygram.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.firebase.Timestamp

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    val id: String = "",
    val email: String = "",
    val username: String = "",
    val profileImageUrl: String? = null,
    val yearOfStudy: String = "",
    val degree: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toFirestoreMap(): Map<String, Any?> {
        return hashMapOf(
            "id" to id,
            "email" to email,
            "username" to username,
            "profileImageUrl" to profileImageUrl,
            "yearOfStudy" to yearOfStudy,
            "degree" to degree,
            "createdAt" to Timestamp(java.util.Date(createdAt)),
            "lastUpdated" to com.google.firebase.firestore.FieldValue.serverTimestamp()
        )
    }

    companion object {
        fun fromFirestore(data: Map<String, Any>): User {
            return User(
                id = data["id"] as? String ?: "",
                email = data["email"] as? String ?: "",
                username = data["username"] as? String ?: "",
                profileImageUrl = data["profileImageUrl"] as? String,
                yearOfStudy = data["yearOfStudy"] as? String ?: "",
                degree = data["degree"] as? String ?: "",
                createdAt = (data["createdAt"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis(),
                lastUpdated = (data["lastUpdated"] as? Timestamp)?.toDate()?.time ?: System.currentTimeMillis()
            )
        }
    }
}
