package com.studygram.ui.comments

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.studygram.data.model.Comment
import com.studygram.data.repository.CommentRepository
import com.studygram.utils.ErrorHandler
import kotlinx.coroutines.launch
import java.util.UUID

class CommentsViewModel(
    private val commentRepository: CommentRepository
) : ViewModel() {

    companion object {
        private const val TAG = "CommentsViewModel"
    }

    private val _postId = MutableLiveData<String>()
    val comments: LiveData<List<Comment>> = _postId.switchMap { postId ->
        commentRepository.getCommentsByPost(postId)
    }

    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error

    private val _success = MutableLiveData<String?>()
    val success: LiveData<String?> = _success

    private val currentUser = FirebaseAuth.getInstance().currentUser

    fun loadComments(postId: String) {
        _postId.value = postId
        syncComments(postId)
    }

    private fun syncComments(postId: String) {
        viewModelScope.launch {
            try {
                val result = commentRepository.syncComments(postId)
                if (result.isFailure) {
                    Log.w(TAG, "Failed to sync comments: ${result.exceptionOrNull()?.message}")
                    // Don't show error - just display cached comments
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error syncing comments", e)
                // Don't show error - just display cached comments
            }
        }
    }

    fun addComment(postId: String, content: String) {
        if (currentUser == null) {
            _error.value = "Please sign in to comment"
            return
        }

        viewModelScope.launch {
            try {
                val comment = Comment(
                    id = UUID.randomUUID().toString(),
                    postId = postId,
                    userId = currentUser.uid,
                    authorName = currentUser.displayName ?: "Anonymous",
                    authorImageUrl = currentUser.photoUrl?.toString(),
                    content = content,
                    timestamp = System.currentTimeMillis()
                )

                val result = commentRepository.addComment(postId, comment)
                if (result.isSuccess) {
                    _success.value = "Comment added"
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Error adding comment", exception)
                    val errorMessage = ErrorHandler.getErrorMessage(exception ?: Exception("Failed to add comment"))
                    _error.value = errorMessage
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error adding comment", e)
                val errorMessage = ErrorHandler.getErrorMessage(e)
                _error.value = errorMessage
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        viewModelScope.launch {
            try {
                val result = commentRepository.deleteComment(postId, commentId)
                if (result.isSuccess) {
                    _success.value = "Comment deleted"
                } else {
                    val exception = result.exceptionOrNull()
                    Log.e(TAG, "Error deleting comment", exception)
                    val errorMessage = ErrorHandler.getErrorMessage(exception ?: Exception("Failed to delete comment"))
                    _error.value = errorMessage
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting comment", e)
                val errorMessage = ErrorHandler.getErrorMessage(e)
                _error.value = errorMessage
            }
        }
    }

    fun clearError() {
        _error.value = null
    }

    fun clearSuccess() {
        _success.value = null
    }
}
