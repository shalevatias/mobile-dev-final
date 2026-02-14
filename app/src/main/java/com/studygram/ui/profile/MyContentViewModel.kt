package com.studygram.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygram.data.model.Post
import com.studygram.data.repository.AuthRepository
import com.studygram.data.repository.PostRepository
import kotlinx.coroutines.launch

class MyContentViewModel(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    val currentUserId: String?
        get() = authRepository.currentUserId

    val myPosts: LiveData<List<Post>> = postRepository.getPostsByUser(currentUserId ?: "")

    fun likePost(postId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            postRepository.likePost(postId, userId)
        }
    }
}
