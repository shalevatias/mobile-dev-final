package com.studygram.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygram.data.model.User
import com.studygram.data.repository.AuthRepository
import com.studygram.utils.ErrorHandler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthState(
    val isLoading: Boolean = false,
    val user: User? = null,
    val error: String? = null
)

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState())
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = AuthState(isLoading = true)

            val result = authRepository.signIn(email, password)

            if (result.isSuccess) {
                _authState.value = AuthState(user = result.getOrNull())
            } else {
                val exception = result.exceptionOrNull()
                val errorMessage = if (exception != null) {
                    ErrorHandler.getErrorMessage(exception)
                } else {
                    "Login failed. Please try again."
                }
                _authState.value = AuthState(error = errorMessage)
            }
        }
    }

    fun signUp(email: String, password: String, username: String) {
        viewModelScope.launch {
            _authState.value = AuthState(isLoading = true)

            val result = authRepository.signUp(email, password, username)

            if (result.isSuccess) {
                _authState.value = AuthState(user = result.getOrNull())
            } else {
                val exception = result.exceptionOrNull()
                val errorMessage = if (exception != null) {
                    ErrorHandler.getErrorMessage(exception)
                } else {
                    "Registration failed. Please try again."
                }
                _authState.value = AuthState(error = errorMessage)
            }
        }
    }

    /**
     * Clear the error state
     */
    fun clearError() {
        _authState.value = _authState.value.copy(error = null)
    }
}
