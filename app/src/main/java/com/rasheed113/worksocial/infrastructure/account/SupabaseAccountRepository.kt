package com.rasheed113.worksocial.infrastructure.account

import com.rasheed113.worksocial.domain.account.AccountProfile
import com.rasheed113.worksocial.domain.account.AccountRepository
import com.rasheed113.worksocial.domain.account.ProfileBlockStatus
import com.rasheed113.worksocial.domain.account.ProfileRelationship
import com.rasheed113.worksocial.domain.account.ProfileUpdateInput
import com.rasheed113.worksocial.domain.account.validateProfileUpdate
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.storage.Storage
import io.ktor.http.ContentType
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.time.Instant
import java.util.UUID

@Serializable private data class ProfileUpdatePayload(val display_name: String, val bio: String?, val date_of_birth: String?, val gender: String?, val location: String?, val website: String?, val updated_at: String)
@Serializable private data class FollowPayload(val follower_id: String, val following_id: String)
@Serializable private data class FollowRow(val following_id: String)
private const val PROFILE_COLUMNS = "id, username, display_name, bio, avatar_url, date_of_birth, gender, location, website, created_at, updated_at"

class SupabaseAccountRepository(private val postgrest: Postgrest, private val auth: Auth, private val storage: Storage) : AccountRepository {
    override suspend fun getProfile(profileId: String): AccountProfile? {
        if (profileId.isBlank()) return null
        return postgrest.from("profiles").select(columns = Columns.list(PROFILE_COLUMNS)) { filter { eq("id", profileId) } }.decodeList<AccountProfile>().firstOrNull()
    }
    override suspend fun getCurrentProfile(): AccountProfile? { val userId=currentUserIdOrNull()?:return null; return getProfile(userId) }
    override suspend fun updateCurrentProfile(input: ProfileUpdateInput): AccountProfile? {
        validateProfileUpdate(input)?.let { throw IllegalArgumentException(it) }; val userId=currentUserIdOrNull()?:throw IllegalStateException(authRequiredMessage())
        postgrest.from("profiles").update(ProfileUpdatePayload(input.display_name.trim(),input.bio.trim().ifEmpty{null},input.date_of_birth.trim().ifEmpty{null},input.gender.trim().ifEmpty{null},input.location.trim().ifEmpty{null},input.website.trim().ifEmpty{null},Instant.now().toString())){filter{eq("id",userId)}}
        return getCurrentProfile()
    }
    override suspend fun uploadCurrentAvatar(jpegBytes: ByteArray): AccountProfile? {
        if(jpegBytes.isEmpty())throw IllegalArgumentException("Avatar image is empty."); val userId=currentUserIdOrNull()?:throw IllegalStateException(authRequiredMessage()); val path="$userId/${UUID.randomUUID()}.jpg"
        try{storage.from("avatars").upload(path,jpegBytes){upsert=false;contentType=ContentType.Image.JPEG};val publicUrl=storage.from("avatars").publicUrl(path);postgrest.from("profiles").update(mapOf("avatar_url" to publicUrl,"updated_at" to Instant.now().toString())){filter{eq("id",userId)}};return getCurrentProfile()}catch(error:Throwable){runCatching{storage.from("avatars").delete(path)};throw error}
    }
    override suspend fun getProfileRelationship(profileId:String):ProfileRelationship{val userId=currentUserIdOrNull()?:throw IllegalStateException(authRequiredMessage());val following=postgrest.from("follows").select(columns=Columns.list("following_id")){filter{eq("follower_id",userId);eq("following_id",profileId)}}.decodeList<FollowRow>().isNotEmpty();return ProfileRelationship(following,getBlockStatus(profileId))}
    override suspend fun setFollowing(profileId:String,following:Boolean){val userId=currentUserIdOrNull()?:throw IllegalStateException(authRequiredMessage());if(following)postgrest.from("follows").insert(FollowPayload(userId,profileId))else postgrest.from("follows").delete{filter{eq("follower_id",userId);eq("following_id",profileId)}}}
    override suspend fun getBlockStatus(profileId:String):ProfileBlockStatus=postgrest.rpc("get_block_status",buildJsonObject{put("p_other_id",profileId)}).decodeList<ProfileBlockStatus>().firstOrNull()?:ProfileBlockStatus()
    override suspend fun blockProfile(profileId:String){postgrest.rpc("block_user",buildJsonObject{put("p_blocked_id",profileId)})}
    override suspend fun unblockProfile(profileId:String){postgrest.rpc("unblock_user",buildJsonObject{put("p_blocked_id",profileId)})}
    private fun currentUserIdOrNull():String?=auth.currentSessionOrNull()?.user?.id
    private fun authRequiredMessage()="Your Work Social session is no longer active. Please sign in again."
}
