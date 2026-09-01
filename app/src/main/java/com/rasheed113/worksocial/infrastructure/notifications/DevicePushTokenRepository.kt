package com.rasheed113.worksocial.infrastructure.notifications

import io.github.jan.supabase.SupabaseClient
import kotlinx.serialization.Serializable
import java.time.Instant

class DevicePushTokenRepository(
    private val supabase: SupabaseClient,
) {
    suspend fun register(token: String): Result<Unit> = runCatching {
        val userId = supabase.auth.currentUserOrNull()?.id
            ?: error("No authenticated Work Social user is available for FCM registration.")
        require(token.isNotBlank())

        val now = Instant.now().toString()
        val payload = DevicePushTokenPayload(
            profileId = userId,
            platform = "android",
            provider = "fcm",
            token = token,
            updatedAt = now,
            lastSeenAt = now,
            revokedAt = null,
        )

        supabase.from("device_push_tokens").upsert(payload, onConflict = "token")
    }

    suspend fun unregister(token: String): Result<Unit> = runCatching {
        val userId = supabase.auth.currentUserOrNull()?.id ?: return@runCatching
        supabase.from("device_push_tokens").delete {
            filter {
                eq("profile_id", userId)
                eq("token", token)
            }
        }
    }
}

@Serializable
private data class DevicePushTokenPayload(
    val profileId: String,
    val platform: String,
    val provider: String,
    val token: String,
    val updatedAt: String,
    val lastSeenAt: String,
    val revokedAt: String?,
)
