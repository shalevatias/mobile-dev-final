package com.studygram.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.studygram.data.repository.AuthRepository
import com.studygram.data.repository.PostRepository

class MyContentViewModelFactory(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MyContentViewModel::class.java)) {
            return MyContentViewModel(postRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
