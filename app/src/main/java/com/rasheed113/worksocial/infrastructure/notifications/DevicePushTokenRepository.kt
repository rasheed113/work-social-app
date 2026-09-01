package com.rasheed113.worksocial.infrastructure.notifications

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant

class DevicePushTokenRepository(
    private val postgrest: Postgrest,
    private val auth: Auth,
) {
    suspend fun register(token: String): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: error("No authenticated Work Social user is available for FCM registration.")
        require(token.isNotBlank())
        val now = Instant.now().toString()
        val payload = buildJsonObject {
            put("profile_id", userId)
            put("platform", "android")
            put("provider", "fcm")
            put("token", token)
            put("updated_at", now)
            put("last_seen_at", now)
            put("revoked_at", null)
        }
        postgrest.from("device_push_tokens").upsert(payload, onConflict = "token")
    }

    suspend fun unregister(token: String): Result<Unit> = runCatching {
        val userId = auth.currentUserOrNull()?.id ?: return@runCatching
        postgrest.from("device_push_tokens").delete {
            filter {
                eq("profile_id", userId)
                eq("token", token)
            }
        }
    }
}
