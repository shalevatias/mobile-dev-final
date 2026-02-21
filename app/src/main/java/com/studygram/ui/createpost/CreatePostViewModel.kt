package com.studygram.ui.createpost

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygram.data.model.Post
import com.studygram.data.remote.FirebaseStorageManager
import com.studygram.data.repository.PostRepository
import com.studygram.utils.ErrorHandler
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

    companion object {
        private const val TAG = "CreatePostViewModel"
    }

    private val _createPostState = MutableStateFlow<Resource<Unit>?>(null)
    val createPostState: StateFlow<Resource<Unit>?> = _createPostState.asStateFlow()

    val currentUserId: String?
        get() = preferenceManager.userId

    val currentUserName: String?
        get() = "User" // Using default username for current version

    fun createPost(post: Post, imageUri: Uri? = null) {
        viewModelScope.launch {
            _createPostState.value = Resource.Loading()

            try {
                // Validate user is logged in
                if (currentUserId.isNullOrEmpty()) {
                    _createPostState.value = Resource.Error("Please sign in to create a post")
                    return@launch
                }

                // Save post locally ONLY - no Firestore sync yet
                Log.d(TAG, "Creating post locally...")
                val result = postRepository.createPostLocalOnly(post)

                if (result.isFailure) {
                    val exception = result.exceptionOrNull()
                    val errorMessage = if (exception != null) {
                        ErrorHandler.getErrorMessage(exception)
                    } else {
                        "Failed to create post. Please try again."
                    }
                    Log.e(TAG, "Failed to create post locally: $errorMessage", exception)
                    _createPostState.value = Resource.Error(errorMessage)
                    return@launch
                }

                val savedPost = result.getOrNull()!!
                Log.d(TAG, "Post created locally with ID: ${savedPost.id}")

                // Add a realistic delay (1.5 seconds)
                kotlinx.coroutines.delay(1500)

                // Return success so UI can navigate back
                _createPostState.value = Resource.Success(Unit)

                // Sync to Firestore and upload image in background using GlobalScope
                // so it continues even after Fragment is destroyed
                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        Log.d(TAG, "Starting background sync...")
                        var finalPost = savedPost

                        // Upload image if provided
                        if (imageUri != null) {
                            Log.d(TAG, "Compressing image...")
                            // Compress image (already on IO thread)
                            val compressedUri = ImageUtils.compressImage(context, imageUri)

                            if (compressedUri != null) {
                                Log.d(TAG, "Uploading compressed image...")
                                // Upload compressed image
                                val uploadResult = storageManager.uploadPostImage(currentUserId ?: "", compressedUri)

                                if (uploadResult.isSuccess) {
                                    val imageUrl = uploadResult.getOrNull()
                                    Log.d(TAG, "Image uploaded successfully: $imageUrl")
                                    // Update post with image URL locally
                                    finalPost = savedPost.copy(imageUrl = imageUrl)
                                    postRepository.updatePost(finalPost)
                                    Log.d(TAG, "Post updated with image URL")
                                } else {
                                    val exception = uploadResult.exceptionOrNull()
                                    Log.e(TAG, "Image upload failed: ${ErrorHandler.getErrorMessage(exception ?: Exception())}", exception)
                                }
                            } else {
                                Log.e(TAG, "Image compression failed")
                            }
                        }

                        // Sync final post to Firestore
                        Log.d(TAG, "Syncing post to Firestore...")
                        val syncResult = postRepository.syncPostToFirestore(finalPost)
                        if (syncResult.isSuccess) {
                            Log.d(TAG, "Post synced to Firestore successfully")
                        } else {
                            val exception = syncResult.exceptionOrNull()
                            Log.e(TAG, "Firestore sync failed: ${ErrorHandler.getErrorMessage(exception ?: Exception())}", exception)
                        }
                    } catch (e: Exception) {
                        // Background sync failed, but post is saved locally
                        Log.e(TAG, "Background sync exception: ${ErrorHandler.getErrorMessage(e)}", e)
                    }
                }
            } catch (e: Exception) {
                val errorMessage = ErrorHandler.getErrorMessage(e)
                Log.e(TAG, "Failed to create post: $errorMessage", e)
                _createPostState.value = Resource.Error(errorMessage)
            }
        }
    }
}
