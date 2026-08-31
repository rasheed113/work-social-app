package com.rasheed113.worksocial.domain.account

import kotlinx.coroutines.flow.Flow

@kotlinx.serialization.Serializable
data class AccountProfile(
    val id: String,
    val username: String,
    val display_name: String,
    val bio: String? = null,
    val avatar_url: String? = null,
    val location: String? = null,
)

sealed interface AccountState {
    data object Loading : AccountState
    data class Success(val profile: AccountProfile) : AccountState
    data object Empty : AccountState
    data class Error(val message: String) : AccountState
}

interface AccountRepository {
    suspend fun getProfile(profileId: String): AccountProfile?
}
