package com.rasheed113.worksocial.infrastructure.supabase

import com.rasheed113.worksocial.BuildConfig
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.storage.Storage

object SupabaseClientProvider {
    fun create() = createSupabaseClient(BuildConfig.SUPABASE_URL, BuildConfig.SUPABASE_PUBLISHABLE_KEY) {
        install(Auth) {
            scheme = "worksocial"
            host = "auth"
        }
        install(Postgrest)
        install(Storage)
    }
}
