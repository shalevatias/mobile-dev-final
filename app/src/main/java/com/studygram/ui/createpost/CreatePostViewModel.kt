package com.studygram.ui.createpost

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygram.data.model.Post
import com.studygram.data.remote.FirebaseStorageManager
import com.studygram.data.repository.PostRepository
import com.studygram.utils.ImageUtils
import com.studygram.utils.PreferenceManager
import com.studygram.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CreatePostViewModel(
    private val postRepository: PostRepository,
    private val preferenceManager: PreferenceManager,
    private val context: Context,
    private val storageManager: FirebaseStorageManager = FirebaseStorageManager()
) : ViewModel() {

    private val _createPostState = MutableStateFlow<Resource<Unit>?>(null)
    val createPostState: StateFlow<Resource<Unit>?> = _createPostState.asStateFlow()

    val currentUserId: String?
        get() = preferenceManager.userId

    val currentUserName: String?
        get() = "User" // TODO: Get from user repository

    fun createPost(post: Post, imageUri: Uri? = null) {
        viewModelScope.launch {
            _createPostState.value = Resource.Loading()

            try {
                // Save post locally ONLY - no Firestore sync yet
                val result = postRepository.createPostLocalOnly(post)

                if (result.isFailure) {
                    _createPostState.value = Resource.Error(result.exceptionOrNull()?.message ?: "Failed to create post")
                    return@launch
                }

                val savedPost = result.getOrNull()!!

                // Add a realistic delay (1.5 seconds)
                kotlinx.coroutines.delay(1500)

                // Return success so UI can navigate back
                _createPostState.value = Resource.Success(Unit)

                // Sync to Firestore and upload image in background using GlobalScope
                // so it continues even after Fragment is destroyed
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        android.util.Log.d("CreatePostViewModel", "Starting background sync...")
                        var finalPost = savedPost

                        // Upload image if provided
                        if (imageUri != null) {
                            android.util.Log.d("CreatePostViewModel", "Compressing image...")
                            // Compress image (already on IO thread)
                            val compressedUri = ImageUtils.compressImage(context, imageUri)

                            if (compressedUri != null) {
                                android.util.Log.d("CreatePostViewModel", "Uploading compressed image...")
                                // Upload compressed image
                                val uploadResult = storageManager.uploadPostImage(currentUserId ?: "", compressedUri)

                                if (uploadResult.isSuccess) {
                                    val imageUrl = uploadResult.getOrNull()
                                    android.util.Log.d("CreatePostViewModel", "Image uploaded successfully: $imageUrl")
                                    // Update post with image URL locally
                                    finalPost = savedPost.copy(imageUrl = imageUrl)
                                    postRepository.updatePost(finalPost)
                                    android.util.Log.d("CreatePostViewModel", "Post updated with image URL")
                                } else {
                                    android.util.Log.e("CreatePostViewModel", "Image upload failed")
                                }
                            } else {
                                android.util.Log.e("CreatePostViewModel", "Image compression failed")
                            }
                        }

                        // Sync final post to Firestore
                        android.util.Log.d("CreatePostViewModel", "Syncing post to Firestore...")
                        val syncResult = postRepository.syncPostToFirestore(finalPost)
                        if (syncResult.isSuccess) {
                            android.util.Log.d("CreatePostViewModel", "Post synced to Firestore successfully")
                        } else {
                            android.util.Log.e("CreatePostViewModel", "Firestore sync failed: ${syncResult.exceptionOrNull()?.message}")
                        }
                    } catch (e: Exception) {
                        // Background sync failed, but post is saved locally
                        android.util.Log.e("CreatePostViewModel", "Background sync exception", e)
                        e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                _createPostState.value = Resource.Error(e.message ?: "An error occurred")
            }
        }
    }
}
