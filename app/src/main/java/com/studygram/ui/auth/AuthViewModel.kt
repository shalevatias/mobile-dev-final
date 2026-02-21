package com.studygram.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.studygram.data.model.User
import com.studygram.data.repository.AuthRepository
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
                _authState.value = AuthState(
                    error = result.exceptionOrNull()?.message ?: "Login failed"
                )
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
                _authState.value = AuthState(
                    error = result.exceptionOrNull()?.message ?: "Registration failed"
                )
            }
        }
    }
}
