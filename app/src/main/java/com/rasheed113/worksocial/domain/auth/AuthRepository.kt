package com.rasheed113.worksocial.domain.auth

import kotlinx.coroutines.flow.Flow

data class AuthenticatedIdentity(val userId: String, val email: String?)

sealed interface AuthState {
    data object Initializing : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val identity: AuthenticatedIdentity) : AuthState
    data class Error(val message: String) : AuthState
}

interface AuthRepository {
    val authState: Flow<AuthState>
    suspend fun signIn(email: String, password: String)
    suspend fun signUp(email: String, password: String, displayName: String)
    suspend fun signOut()
}
