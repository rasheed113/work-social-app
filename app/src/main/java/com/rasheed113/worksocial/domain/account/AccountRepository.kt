package com.rasheed113.worksocial.domain.account

import kotlinx.serialization.Serializable
import java.net.URL

@Serializable data class AccountProfile(val id: String, val username: String, val display_name: String, val bio: String? = null, val avatar_url: String? = null, val date_of_birth: String? = null, val gender: String? = null, val location: String? = null, val website: String? = null, val created_at: String? = null, val updated_at: String? = null)
@Serializable data class ProfileUpdateInput(val display_name: String, val bio: String, val date_of_birth: String, val gender: String, val location: String, val website: String)
@Serializable data class ProfileBlockStatus(val blocked_by_me: Boolean = false, val blocked_me: Boolean = false, val blocked: Boolean = false)
fun validateProfileUpdate(input: ProfileUpdateInput): String? {
    if (input.display_name.trim().isEmpty()) return "Display name is required."
    val website = input.website.trim()
    if (website.isNotEmpty()) {
        val valid = runCatching { URL(website).protocol.lowercase() in setOf("http", "https") }.getOrDefault(false)
        if (!valid) return "Website must be a valid HTTP or HTTPS URL."
    }
    return null
}
sealed interface AccountState { data object Loading : AccountState; data class Success(val profile: AccountProfile) : AccountState; data object Empty : AccountState; data class Error(val message: String) : AccountState }
data class ProfileRelationship(val following: Boolean, val blockStatus: ProfileBlockStatus)
interface AccountRepository {
    suspend fun getProfile(profileId: String): AccountProfile?
    suspend fun getCurrentProfile(): AccountProfile? = null
    suspend fun updateCurrentProfile(input: ProfileUpdateInput): AccountProfile? = null
    suspend fun uploadCurrentAvatar(jpegBytes: ByteArray): AccountProfile? = null
    suspend fun getProfileRelationship(profileId: String): ProfileRelationship = ProfileRelationship(false, ProfileBlockStatus())
    suspend fun setFollowing(profileId: String, following: Boolean) = Unit
    suspend fun getBlockStatus(profileId: String): ProfileBlockStatus = ProfileBlockStatus()
    suspend fun blockProfile(profileId: String) = Unit
    suspend fun unblockProfile(profileId: String) = Unit
}
