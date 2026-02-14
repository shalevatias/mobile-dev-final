package com.studygram.ui.feed

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.studygram.data.model.Post
import com.studygram.data.repository.AuthRepository
import com.studygram.data.repository.PostRepository
import com.studygram.utils.Resource
import kotlinx.coroutines.launch

class FeedViewModel(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _selectedCourse = MutableLiveData<String?>(null)

    val posts: LiveData<List<Post>> = _selectedCourse.switchMap { course ->
        if (course.isNullOrEmpty()) {
            postRepository.getAllPosts()
        } else {
            postRepository.getPostsByCourse(course)
        }
    }

    val courseTags: LiveData<List<String>> = postRepository.getAllCourseTags()

    private val _syncState = MutableLiveData<Resource<Unit>>()
    val syncState: LiveData<Resource<Unit>> = _syncState

    val currentUserId: String?
        get() = authRepository.currentUserId

    init {
        syncPosts()
    }

    fun syncPosts() {
        viewModelScope.launch {
            _syncState.value = Resource.Loading()
            val result = postRepository.syncPosts()
            _syncState.value = if (result.isSuccess) {
                Resource.Success(Unit)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Sync failed")
            }
        }
    }

    fun refreshPosts() {
        viewModelScope.launch {
            _syncState.value = Resource.Loading()
            val result = postRepository.refreshPosts()
            _syncState.value = if (result.isSuccess) {
                Resource.Success(Unit)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Refresh failed")
            }
        }
    }

    fun filterByCourse(course: String?) {
        _selectedCourse.value = course
    }

    fun likePost(postId: String) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            postRepository.likePost(postId, userId)
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }
}
