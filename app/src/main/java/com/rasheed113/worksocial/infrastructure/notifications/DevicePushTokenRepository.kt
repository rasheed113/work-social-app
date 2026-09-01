package com.rasheed113.worksocial.infrastructure.notifications

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

class DevicePushTokenRepository(
    private val postgrest: Postgrest,
    private val auth: Auth,
) {
    suspend fun register(token: String): Result<Unit> = runCatching {
        val userId = auth.currentSessionOrNull()?.user?.id
            ?: error("No authenticated Work Social user is available for FCM registration.")
        require(token.isNotBlank())

        val now = Instant.now().toString()
        postgrest.from("device_push_tokens").upsert(
            DevicePushTokenPayload(
                profileId = userId,
                platform = "android",
                provider = "fcm",
                token = token,
                updatedAt = now,
                lastSeenAt = now,
                revokedAt = null,
            ),
        ) {
            onConflict = "token"
            ignoreDuplicates = false
        }
    }

    suspend fun unregister(token: String): Result<Unit> = runCatching {
        val userId = auth.currentSessionOrNull()?.user?.id ?: return@runCatching
        postgrest.from("device_push_tokens").delete {
            filter {
                eq("profile_id", userId)
                eq("token", token)
            }
        }
    }
}

@Serializable
private data class DevicePushTokenPayload(
    @SerialName("profile_id") val profileId: String,
    val platform: String,
    val provider: String,
    val token: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("last_seen_at") val lastSeenAt: String,
    @SerialName("revoked_at") val revokedAt: String?,
)
