package com.rasheed113.worksocial.infrastructure.notifications

import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.coroutines.tasks.await

object FcmRegistrationManager {
    suspend fun sync(postgrest: Postgrest, auth: Auth): Result<Unit> = runCatching {
        val token = FirebaseMessaging.getInstance().token.await()
        DevicePushTokenRepository(postgrest, auth).register(token).getOrThrow()
    }

    suspend fun unregister(postgrest: Postgrest, auth: Auth): Result<Unit> = runCatching {
        val token = FirebaseMessaging.getInstance().token.await()
        DevicePushTokenRepository(postgrest, auth).unregister(token).getOrThrow()
    }
}
