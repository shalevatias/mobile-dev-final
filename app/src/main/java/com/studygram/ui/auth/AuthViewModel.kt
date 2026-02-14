package com.studygram.ui.auth

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygram.data.model.User
import com.studygram.data.repository.AuthRepository
import com.studygram.utils.Resource
import com.studygram.utils.isValidEmail
import com.studygram.utils.isValidPassword
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableLiveData<Resource<User>>()
    val authState: LiveData<Resource<User>> = _authState

    val isUserLoggedIn: Boolean
        get() = authRepository.isUserLoggedIn

    fun signUp(email: String, password: String, username: String) {
        if (!validateSignUp(email, password, username)) return

        viewModelScope.launch {
            _authState.value = Resource.Loading()
            val result = authRepository.signUp(email, password, username)
            _authState.value = if (result.isSuccess) {
                Resource.Success(result.getOrNull()!!)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Sign up failed")
            }
        }
    }

    fun signIn(email: String, password: String) {
        if (!validateSignIn(email, password)) return

        viewModelScope.launch {
            _authState.value = Resource.Loading()
            val result = authRepository.signIn(email, password)
            _authState.value = if (result.isSuccess) {
                Resource.Success(result.getOrNull()!!)
            } else {
                Resource.Error(result.exceptionOrNull()?.message ?: "Sign in failed")
            }
        }
    }

    private fun validateSignUp(email: String, password: String, username: String): Boolean {
        return when {
            username.isBlank() -> {
                _authState.value = Resource.Error("Username is required")
                false
            }
            username.length < 3 -> {
                _authState.value = Resource.Error("Username must be at least 3 characters")
                false
            }
            !email.isValidEmail() -> {
                _authState.value = Resource.Error("Invalid email address")
                false
            }
            !password.isValidPassword() -> {
                _authState.value = Resource.Error("Password must be at least 6 characters")
                false
            }
            else -> true
        }
    }

    private fun validateSignIn(email: String, password: String): Boolean {
        return when {
            !email.isValidEmail() -> {
                _authState.value = Resource.Error("Invalid email address")
                false
            }
            password.isBlank() -> {
                _authState.value = Resource.Error("Password is required")
                false
            }
            else -> true
        }
    }
}
