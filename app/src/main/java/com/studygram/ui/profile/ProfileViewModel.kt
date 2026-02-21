package com.studygram.ui.profile

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygram.data.model.User
import com.studygram.data.repository.AuthRepository
import com.studygram.utils.ErrorHandler
import com.studygram.utils.Resource
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _updateState = MutableLiveData<Resource<Unit>?>()
    val updateState: LiveData<Resource<Unit>?> = _updateState

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            try {
                val currentUser = authRepository.getCurrentUser()
                _user.value = currentUser
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Error loading user", e)
                // Silently fail - user might be logged out
            }
        }
    }

    fun updateProfile(name: String, yearOfStudy: String, degree: String) {
        viewModelScope.launch {
            _updateState.value = Resource.Loading()
            try {
                val userId = authRepository.currentUserId
                android.util.Log.d("ProfileViewModel", "Current userId: $userId")

                if (userId == null) {
                    _updateState.value = Resource.Error("User not logged in")
                    return@launch
                }

                val updates = mapOf(
                    "username" to name,
                    "yearOfStudy" to yearOfStudy,
                    "degree" to degree
                )

                android.util.Log.d("ProfileViewModel", "Updating profile with: $updates")

                val result = authRepository.updateUserProfile(userId, updates)
                if (result.isSuccess) {
                    android.util.Log.d("ProfileViewModel", "Profile updated successfully")
                    _updateState.value = Resource.Success(Unit)
                    loadUser() // Reload user data
                } else {
                    val exception = result.exceptionOrNull()
                    android.util.Log.e("ProfileViewModel", "Update failed", exception)
                    val errorMessage = if (exception != null) {
                        ErrorHandler.getErrorMessage(exception)
                    } else {
                        "Failed to update profile"
                    }
                    _updateState.value = Resource.Error(errorMessage)
                }
            } catch (e: Exception) {
                android.util.Log.e("ProfileViewModel", "Exception during update", e)
                val errorMessage = ErrorHandler.getErrorMessage(e)
                _updateState.value = Resource.Error(errorMessage)
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun clearUpdateState() {
        _updateState.value = null
    }
}
