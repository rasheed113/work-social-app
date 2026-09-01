package com.rasheed113.worksocial.infrastructure.notifications

import com.google.firebase.messaging.FirebaseMessaging
import io.github.jan.supabase.SupabaseClient
import kotlinx.coroutines.tasks.await

object FcmRegistrationManager {
    suspend fun sync(supabase: SupabaseClient): Result<Unit> = runCatching {
        val token = FirebaseMessaging.getInstance().token.await()
        DevicePushTokenRepository(supabase.postgrest, supabase.auth).register(token).getOrThrow()
    }

    suspend fun unregister(supabase: SupabaseClient): Result<Unit> = runCatching {
        val token = FirebaseMessaging.getInstance().token.await()
        DevicePushTokenRepository(supabase.postgrest, supabase.auth).unregister(token).getOrThrow()
    }
}
