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
                if (currentUserId.isNullOrEmpty()) {
                    _createPostState.value = Resource.Error("Please sign in to create a post")
                    return@launch
                }

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
                kotlinx.coroutines.delay(1500)
                _createPostState.value = Resource.Success(Unit)

                GlobalScope.launch(Dispatchers.IO) {
                    try {
                        var finalPost = savedPost

                        if (imageUri != null) {
                            val compressedUri = ImageUtils.compressImage(context, imageUri)

                            if (compressedUri != null) {
                                val uploadResult = storageManager.uploadPostImage(currentUserId ?: "", compressedUri)

                                if (uploadResult.isSuccess) {
                                    val imageUrl = uploadResult.getOrNull()
                                    finalPost = savedPost.copy(imageUrl = imageUrl)
                                    postRepository.updatePost(finalPost)
                                } else {
                                    val exception = uploadResult.exceptionOrNull()
                                    Log.e(TAG, "Image upload failed", exception)
                                }
                            } else {
                                Log.e(TAG, "Image compression failed")
                            }
                        }

                        val syncResult = postRepository.syncPostToFirestore(finalPost)
                        if (syncResult.isFailure) {
                            val exception = syncResult.exceptionOrNull()
                            Log.e(TAG, "Firestore sync failed", exception)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Background sync exception", e)
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
