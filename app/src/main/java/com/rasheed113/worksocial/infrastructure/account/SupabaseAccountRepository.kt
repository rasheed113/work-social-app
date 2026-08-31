package com.rasheed113.worksocial.infrastructure.account

import com.rasheed113.worksocial.domain.account.AccountProfile
import com.rasheed113.worksocial.domain.account.AccountRepository
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns

class SupabaseAccountRepository(private val postgrest: Postgrest) : AccountRepository {
    override suspend fun getProfile(profileId: String): AccountProfile? {
        return postgrest.from("profiles")
            .select(
                columns = Columns.list(
                    "id, username, display_name, bio, avatar_url, location"
                )
            ) {
                filter {
                    eq("id", profileId)
                }
            }
            .decodeList<AccountProfile>()
            .firstOrNull()
    }
}
