package com.studygram.ui.post

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygram.data.model.Post
import com.studygram.data.remote.FirebaseStorageManager
import com.studygram.data.repository.AuthRepository
import com.studygram.data.repository.PostRepository
import com.studygram.utils.Resource
import kotlinx.coroutines.launch
import java.util.UUID

class CreatePostViewModel(
    private val postRepository: PostRepository,
    private val storageManager: FirebaseStorageManager,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _createPostState = MutableLiveData<Resource<Post>>()
    val createPostState: LiveData<Resource<Post>> = _createPostState

    private val _imageUploadState = MutableLiveData<Resource<String>>()
    val imageUploadState: LiveData<Resource<String>> = _imageUploadState

    val currentUserId: String?
        get() = authRepository.currentUserId

    fun uploadImage(imageUri: Uri) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _imageUploadState.value = Resource.Loading()
            val result = storageManager.uploadPostImage(userId, imageUri)
            _imageUploadState.value = if (result.isSuccess) {
                Resource.Success(result.getOrNull()!!)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Image upload failed")
            }
        }
    }

    fun createPost(
        title: String,
        content: String,
        courseTag: String,
        difficultyLevel: String,
        imageUrl: String?,
        authorName: String,
        authorImageUrl: String?
    ) {
        if (!validateInput(title, content, courseTag, difficultyLevel)) return

        val userId = currentUserId ?: run {
            _createPostState.value = Resource.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            _createPostState.value = Resource.Loading()

            val post = Post(
                id = UUID.randomUUID().toString(),
                userId = userId,
                authorName = authorName,
                authorImageUrl = authorImageUrl,
                title = title,
                content = content,
                courseTag = courseTag,
                difficultyLevel = difficultyLevel,
                imageUrl = imageUrl,
                likes = 0,
                likedBy = "",
                commentsCount = 0,
                timestamp = System.currentTimeMillis(),
                lastUpdated = System.currentTimeMillis()
            )

            val result = postRepository.createPost(post)
            _createPostState.value = if (result.isSuccess) {
                Resource.Success(post)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Failed to create post")
            }
        }
    }

    fun updatePost(
        postId: String,
        title: String,
        content: String,
        courseTag: String,
        difficultyLevel: String,
        imageUrl: String?,
        existingPost: Post
    ) {
        if (!validateInput(title, content, courseTag, difficultyLevel)) return

        viewModelScope.launch {
            _createPostState.value = Resource.Loading()

            val updatedPost = existingPost.copy(
                title = title,
                content = content,
                courseTag = courseTag,
                difficultyLevel = difficultyLevel,
                imageUrl = imageUrl,
                lastUpdated = System.currentTimeMillis()
            )

            val result = postRepository.updatePost(updatedPost)
            _createPostState.value = if (result.isSuccess) {
                Resource.Success(updatedPost)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Failed to update post")
            }
        }
    }

    private fun validateInput(
        title: String,
        content: String,
        courseTag: String,
        difficultyLevel: String
    ): Boolean {
        return when {
            title.isBlank() -> {
                _createPostState.value = Resource.Error("Title is required")
                false
            }
            title.length < 3 -> {
                _createPostState.value = Resource.Error("Title must be at least 3 characters")
                false
            }
            content.isBlank() -> {
                _createPostState.value = Resource.Error("Content is required")
                false
            }
            content.length < 10 -> {
                _createPostState.value = Resource.Error("Content must be at least 10 characters")
                false
            }
            courseTag.isBlank() -> {
                _createPostState.value = Resource.Error("Course tag is required")
                false
            }
            difficultyLevel.isBlank() -> {
                _createPostState.value = Resource.Error("Difficulty level is required")
                false
            }
            else -> true
        }
    }
}
