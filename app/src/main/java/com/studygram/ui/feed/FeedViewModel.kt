package com.studygram.ui.feed

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.studygram.data.model.Post
import com.studygram.data.model.Quote
import com.studygram.data.repository.AuthRepository
import com.studygram.data.repository.PostRepository
import com.studygram.data.repository.QuoteRepository
import com.studygram.utils.ErrorHandler
import com.studygram.utils.Resource
import kotlinx.coroutines.launch

class FeedViewModel(
    private val postRepository: PostRepository,
    private val authRepository: AuthRepository,
    private val quoteRepository: QuoteRepository = QuoteRepository()
) : ViewModel() {

    companion object {
        private const val TAG = "FeedViewModel"
    }

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

    private val _quote = MutableLiveData<Resource<Quote>>()
    val quote: LiveData<Resource<Quote>> = _quote

    private val _likeState = MutableLiveData<Resource<Unit>>()
    val likeState: LiveData<Resource<Unit>> = _likeState

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    val currentUserId: String?
        get() = authRepository.currentUserId

    init {
        Log.d(TAG, "FeedViewModel init started")

        // Clear any existing errors from previous session
        _error.value = null

        // Try to refresh posts from Firestore, but don't block on it
        // Posts from local database will show immediately via LiveData
        viewModelScope.launch {
            try {
                Log.d(TAG, "Starting refresh posts...")
                val result = postRepository.refreshPosts()
                if (result.isSuccess) {
                    Log.d(TAG, "Refresh posts succeeded")
                } else {
                    Log.e(TAG, "Refresh posts failed: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                // Silently fail - local cached posts will still display
                Log.e(TAG, "Background refresh failed on init", e)
            }
        }

        loadQuote()
        Log.d(TAG, "FeedViewModel init completed")
    }

    fun loadQuote() {
        viewModelScope.launch {
            try {
                _quote.value = Resource.Loading()
                _quote.value = quoteRepository.getRandomQuote()
            } catch (e: Exception) {
                Log.e(TAG, "Error loading quote", e)
                // Show error in quote section only, don't set general error
                val errorMessage = "Unable to load quote"
                _quote.value = Resource.Error(errorMessage)
            }
        }
    }

    fun syncPosts() {
        viewModelScope.launch {
            try {
                // Don't show loading for background sync
                val result = postRepository.syncPosts()
                if (result.isSuccess) {
                    _syncState.value = Resource.Success(Unit)
                } else {
                    val exception = result.exceptionOrNull()
                    if (exception != null) {
                        Log.e(TAG, "Background sync failed", exception)
                    }
                    // Don't set error state for background sync - fail silently
                    _syncState.value = Resource.Success(Unit)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Background sync failed", e)
                // Don't set error state for background sync - fail silently
                _syncState.value = Resource.Success(Unit)
            }
        }
    }

    fun refreshPosts() {
        viewModelScope.launch {
            try {
                _syncState.value = Resource.Loading()
                val result = postRepository.refreshPosts()
                _syncState.value = if (result.isSuccess) {
                    Resource.Success(Unit)
                } else {
                    val exception = result.exceptionOrNull()
                    if (exception != null) {
                        Log.e(TAG, "Error refreshing posts", exception)
                        // Show user-friendly message for manual refresh only
                        val errorMessage = "Unable to refresh. Showing cached posts."
                        Resource.Error(errorMessage)
                    } else {
                        Resource.Error("Unable to refresh. Showing cached posts.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error refreshing posts", e)
                val errorMessage = "Unable to refresh. Showing cached posts."
                _syncState.value = Resource.Error(errorMessage)
            }
        }
    }

    fun filterByCourse(course: String?) {
        _selectedCourse.value = course
    }

    fun likePost(postId: String) {
        val userId = currentUserId
        if (userId == null) {
            _error.value = "Please sign in to like posts"
            return
        }

        viewModelScope.launch {
            try {
                _likeState.value = Resource.Loading()
                val result = postRepository.likePost(postId, userId)
                _likeState.value = if (result.isSuccess) {
                    Resource.Success(Unit)
                } else {
                    val exception = result.exceptionOrNull()
                    if (exception != null) {
                        Log.e(TAG, "Error liking post", exception)
                        val errorMessage = ErrorHandler.getErrorMessage(exception)
                        _error.value = errorMessage
                        Resource.Error(errorMessage)
                    } else {
                        Resource.Error("Failed to like post")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error liking post", e)
                val errorMessage = ErrorHandler.getErrorMessage(e)
                _likeState.value = Resource.Error(errorMessage)
                _error.value = errorMessage
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.signOut()
            } catch (e: Exception) {
                Log.e(TAG, "Error logging out", e)
                val errorMessage = ErrorHandler.getErrorMessage(e)
                _error.value = errorMessage
            }
        }
    }

    /**
     * Clear error after it's been displayed
     */
    fun clearError() {
        _error.value = null
    }
}
