package com.rasheed113.worksocial.infrastructure.supabase

import com.rasheed113.worksocial.BuildConfig
import com.rasheed113.worksocial.platform.diagnostics.DeviceForensics
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    fun create() = runCatching {
        createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
            install(Auth) {
                scheme = "worksocial"
                host = "auth"
            }
            install(Postgrest)
            install(Realtime)
            install(Storage)
        }
    }.onSuccess {
        DeviceForensics.recordSupabaseInitialization(success = true)
    }.onFailure {
        DeviceForensics.recordSupabaseInitialization(success = false, error = it)
    }.getOrThrow()
}
