package com.rasheed113.worksocial.infrastructure.auth

import com.google.firebase.messaging.FirebaseMessaging
import com.rasheed113.worksocial.domain.auth.AuthRepository
import com.rasheed113.worksocial.domain.auth.AuthState
import com.rasheed113.worksocial.domain.auth.AuthenticatedIdentity
import com.rasheed113.worksocial.domain.auth.SignUpOutcome
import com.rasheed113.worksocial.infrastructure.notifications.DevicePushTokenRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.status.SessionStatus
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseAuthRepository(
    private val auth: Auth,
    private val postgrest: Postgrest,
) : AuthRepository {
    override val authState: Flow<AuthState> = auth.sessionStatus.map { status ->
        when (status) {
            SessionStatus.Initializing -> AuthState.Initializing
            is SessionStatus.Authenticated -> {
                val user = status.session.user
                if (user == null) AuthState.Error("Authenticated session has no user id.")
                else AuthState.SignedIn(AuthenticatedIdentity(user.id, user.email))
            }
            is SessionStatus.RefreshFailure -> AuthState.Error("Your session could not be refreshed. Please sign in again.")
            is SessionStatus.NotAuthenticated -> AuthState.SignedOut
        }
    }

    override suspend fun signIn(email: String, password: String) {
        auth.signInWith(Email) {
            this.email = email.trim()
            this.password = password
        }
    }

    override suspend fun signUp(email: String, password: String, displayName: String): SignUpOutcome {
        auth.signUpWith(Email) {
            this.email = email.trim()
            this.password = password
            data = buildJsonObject { put("display_name", displayName.trim()) }
        }
        return SignUpOutcome(sessionEstablished = auth.currentSessionOrNull() != null)
    }

    override suspend fun signOut() {
        if (auth.currentSessionOrNull() != null) {
            runCatching {
                val token = FirebaseMessaging.getInstance().token.await()
                DevicePushTokenRepository(postgrest, auth).unregister(token).getOrThrow()
            }
        }
        auth.signOut()
    }
}
