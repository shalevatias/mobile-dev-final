package com.studygram.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.studygram.data.remote.FirebaseStorageManager
import com.studygram.data.repository.AuthRepository
import com.studygram.data.repository.UserRepository

class ProfileViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val storageManager: FirebaseStorageManager
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ProfileViewModel::class.java)) {
            return ProfileViewModel(authRepository, userRepository, storageManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
