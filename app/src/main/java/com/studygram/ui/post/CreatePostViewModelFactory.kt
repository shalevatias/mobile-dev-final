package com.studygram.ui.post

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.studygram.data.remote.FirebaseStorageManager
import com.studygram.data.repository.AuthRepository
import com.studygram.data.repository.PostRepository

class CreatePostViewModelFactory(
    private val postRepository: PostRepository,
    private val storageManager: FirebaseStorageManager,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreatePostViewModel::class.java)) {
            return CreatePostViewModel(postRepository, storageManager, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
