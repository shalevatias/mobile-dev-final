package com.studygram.ui.post

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.studygram.data.model.Comment
import com.studygram.data.model.Post
import com.studygram.data.repository.AuthRepository
import com.studygram.data.repository.CommentRepository
import com.studygram.data.repository.PostRepository
import com.studygram.utils.Resource
import kotlinx.coroutines.launch
import java.util.UUID

class PostDetailViewModel(
    private val postRepository: PostRepository,
    private val commentRepository: CommentRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _postId = MutableLiveData<String>()

    val post: LiveData<Post?> = _postId.switchMap { postId ->
        postRepository.getPostById(postId)
    }

    val comments: LiveData<List<Comment>> = _postId.switchMap { postId ->
        commentRepository.getCommentsByPost(postId)
    }

    private val _commentState = MutableLiveData<Resource<Unit>>()
    val commentState: LiveData<Resource<Unit>> = _commentState

    private val _deleteState = MutableLiveData<Resource<Unit>>()
    val deleteState: LiveData<Resource<Unit>> = _deleteState

    val currentUserId: String?
        get() = authRepository.currentUserId

    fun setPostId(postId: String) {
        _postId.value = postId
        syncComments(postId)
    }

    fun likePost(postId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            postRepository.likePost(postId, userId)
        }
    }

    fun addComment(postId: String, content: String, authorName: String, authorImageUrl: String?) {
        if (content.isBlank()) {
            _commentState.value = Resource.Error("Comment cannot be empty")
            return
        }

        val userId = currentUserId ?: run {
            _commentState.value = Resource.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            _commentState.value = Resource.Loading()

            val comment = Comment(
                id = UUID.randomUUID().toString(),
                postId = postId,
                userId = userId,
                authorName = authorName,
                authorImageUrl = authorImageUrl,
                content = content,
                timestamp = System.currentTimeMillis()
            )

            val result = commentRepository.addComment(postId, comment)
            _commentState.value = if (result.isSuccess) {
                Resource.Success(Unit)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Failed to add comment")
            }
        }
    }

    fun deletePost(postId: String) {
        viewModelScope.launch {
            _deleteState.value = Resource.Loading()
            val result = postRepository.deletePost(postId)
            _deleteState.value = if (result.isSuccess) {
                Resource.Success(Unit)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Failed to delete post")
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            commentRepository.deleteComment(postId, commentId)
        }
    }

    private fun syncComments(postId: String) {
        viewModelScope.launch {
            commentRepository.syncComments(postId)
        }
    }
}
