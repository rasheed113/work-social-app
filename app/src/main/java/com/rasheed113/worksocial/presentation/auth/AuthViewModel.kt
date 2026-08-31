package com.rasheed113.worksocial.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.auth.AuthRepository
import com.rasheed113.worksocial.domain.auth.AuthState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class AuthUiState(
    val auth: AuthState = AuthState.Initializing,
    val busy: Boolean = false,
    val error: String? = null,
    val notice: String? = null,
)

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.authState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    auth = state,
                    busy = false,
                    error = if (state is AuthState.Error) state.message else null,
                )
            }
        }
    }

    fun signIn(email: String, password: String) = runAuth {
        repository.signIn(email, password)
    }

    fun signUp(email: String, password: String, displayName: String) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null, notice = null)
            runCatching { repository.signUp(email, password, displayName) }
                .onSuccess { outcome ->
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        notice = if (outcome.sessionEstablished) null
                        else "Account created. Check your email to confirm your account before signing in.",
                    )
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        busy = false,
                        error = error.message?.takeIf { it.isNotBlank() } ?: "Sign up failed.",
                    )
                }
        }
    }

    fun signOut() = runAuth { repository.signOut() }

    private fun runAuth(action: suspend () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(busy = true, error = null, notice = null)
            runCatching { action() }.onFailure {
                _uiState.value = _uiState.value.copy(
                    busy = false,
                    error = it.message?.takeIf(String::isNotBlank) ?: "Authentication request failed.",
                )
            }
        }
    }
}
