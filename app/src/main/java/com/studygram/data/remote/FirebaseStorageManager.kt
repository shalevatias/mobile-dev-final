package com.studygram.data.remote

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import com.studygram.utils.Constants
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirebaseStorageManager {

    private val storage: FirebaseStorage = FirebaseStorage.getInstance()

    suspend fun uploadProfileImage(userId: String, imageUri: Uri): Result<String> {
        return try {
            val fileName = "${userId}_${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference
                .child(Constants.STORAGE_PROFILE_IMAGES)
                .child(userId)
                .child(fileName)

            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun uploadPostImage(userId: String, imageUri: Uri): Result<String> {
        return try {
            val fileName = "${userId}_${UUID.randomUUID()}.jpg"
            val storageRef = storage.reference
                .child(Constants.STORAGE_POST_IMAGES)
                .child(userId)
                .child(fileName)

            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteImage(imageUrl: String): Result<Unit> {
        return try {
            val storageRef = storage.getReferenceFromUrl(imageUrl)
            storageRef.delete().await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
