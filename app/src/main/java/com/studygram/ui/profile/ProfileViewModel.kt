package com.studygram.ui.profile

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import com.studygram.data.model.User
import com.studygram.data.remote.FirebaseStorageManager
import com.studygram.data.repository.AuthRepository
import com.studygram.data.repository.UserRepository
import com.studygram.utils.Resource
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val storageManager: FirebaseStorageManager
) : ViewModel() {

    val currentUserId: String?
        get() = authRepository.currentUserId

    val user: LiveData<User?> = userRepository.getUserById(currentUserId ?: "")

    private val _imageUploadState = MutableLiveData<Resource<String>>()
    val imageUploadState: LiveData<Resource<String>> = _imageUploadState

    private val _updateState = MutableLiveData<Resource<Unit>>()
    val updateState: LiveData<Resource<Unit>> = _updateState

    fun uploadProfileImage(imageUri: Uri) {
        val userId = currentUserId ?: return
        viewModelScope.launch {
            _imageUploadState.value = Resource.Loading()
            val result = storageManager.uploadProfileImage(userId, imageUri)
            _imageUploadState.value = if (result.isSuccess) {
                Resource.Success(result.getOrNull()!!)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Image upload failed")
            }
        }
    }

    fun updateProfile(username: String, profileImageUrl: String?) {
        if (!validateUsername(username)) return

        val userId = currentUserId ?: run {
            _updateState.value = Resource.Error("User not logged in")
            return
        }

        viewModelScope.launch {
            _updateState.value = Resource.Loading()

            val updates = mutableMapOf<String, Any>(
                "username" to username
            )
            profileImageUrl?.let { updates["profileImageUrl"] = it }

            val result = authRepository.updateUserProfile(userId, updates)
            _updateState.value = if (result.isSuccess) {
                Resource.Success(Unit)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Failed to update profile")
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    private fun validateUsername(username: String): Boolean {
        return when {
            username.isBlank() -> {
                _updateState.value = Resource.Error("Username is required")
                false
            }
            username.length < 3 -> {
                _updateState.value = Resource.Error("Username must be at least 3 characters")
                false
            }
            else -> true
        }
    }
}
