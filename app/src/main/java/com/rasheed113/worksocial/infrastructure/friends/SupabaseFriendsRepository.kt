package com.rasheed113.worksocial.infrastructure.friends

import com.rasheed113.worksocial.domain.friends.FriendMutationResult
import com.rasheed113.worksocial.domain.friends.FriendProfile
import com.rasheed113.worksocial.domain.friends.FriendRequest
import com.rasheed113.worksocial.domain.friends.FriendPerson
import com.rasheed113.worksocial.domain.friends.FriendsData
import com.rasheed113.worksocial.domain.friends.FriendsRepository
import com.rasheed113.worksocial.domain.friends.FriendsResult
import com.rasheed113.worksocial.domain.friends.relationshipState
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable

@Serializable
internal data class ProfileDto(val id: String, val username: String, val display_name: String, val avatar_url: String? = null)

@Serializable
internal data class FriendRequestDto(val id: String, val sender_id: String, val receiver_id: String, val status: String, val created_at: String)

@Serializable
internal data class FriendPairDto(val profile_a_id: String, val profile_b_id: String)

@Serializable
private data class FriendRequestPayload(val sender_id: String, val receiver_id: String, val status: String)

@Serializable
private data class FriendshipPayload(val profile_a_id: String, val profile_b_id: String)

@Serializable
private data class StatusPayload(val status: String)

internal fun mapFriendProfile(dto: ProfileDto) = FriendProfile(dto.id, dto.username, dto.display_name, dto.avatar_url)

internal fun mapFriendRequest(dto: FriendRequestDto, profile: FriendProfile) = FriendRequest(dto.id, dto.sender_id, dto.receiver_id, dto.status, dto.created_at, profile)

class SupabaseFriendsRepository(private val postgrest: Postgrest, private val auth: Auth) : FriendsRepository {
    override suspend fun getFriends(): FriendsResult {
        val userId = currentUserIdOrNull() ?: return FriendsResult.Failure(authRequiredMessage())
        return runCatching {
            val people = postgrest.from("profiles").select(columns = Columns.list("id, username, display_name, avatar_url")) {
                filter { neq("id", userId) }
                order("display_name", Order.ASCENDING)
            }.decodeList<ProfileDto>()
            val incoming = postgrest.from("friend_requests").select(columns = Columns.list("id, sender_id, receiver_id, status, created_at")) {
                filter { eq("receiver_id", userId); eq("status", "pending") }
                order("created_at", Order.DESCENDING)
            }.decodeList<FriendRequestDto>()
            val outgoing = postgrest.from("friend_requests").select(columns = Columns.list("id, sender_id, receiver_id, status, created_at")) {
                filter { eq("sender_id", userId); eq("status", "pending") }
                order("created_at", Order.DESCENDING)
            }.decodeList<FriendRequestDto>()
            val friendships = postgrest.from("friends").select(columns = Columns.list("profile_a_id, profile_b_id")) {
                filter { or { eq("profile_a_id", userId); eq("profile_b_id", userId) } }
            }.decodeList<FriendPairDto>()

            val friendIds = friendships.map { if (it.profile_a_id == userId) it.profile_b_id else it.profile_a_id }.toSet()
            val incomingBySender = incoming.map(FriendRequestDto::sender_id).toSet()
            val outgoingByReceiver = outgoing.map(FriendRequestDto::receiver_id).toSet()
            val profileById = people.associateBy(ProfileDto::id).mapValues { mapFriendProfile(it.value) }
            val incomingModels = incoming.mapNotNull { request -> profileById[request.sender_id]?.let { mapFriendRequest(request, it) } }
            val outgoingModels = outgoing.mapNotNull { request -> profileById[request.receiver_id]?.let { mapFriendRequest(request, it) } }
            val peopleModels = people.map { profile ->
                val mapped = mapFriendProfile(profile)
                FriendPerson(mapped, relationshipState(mapped.id, userId, friendIds, incomingBySender, outgoingByReceiver))
            }
            FriendsResult.Success(FriendsData(peopleModels, incomingModels, outgoingModels))
        }.getOrElse { FriendsResult.Failure(it.message?.takeIf(String::isNotBlank) ?: "Unable to load Friends right now.") }
    }

    override suspend fun sendRequest(receiverId: String): FriendMutationResult {
        val userId = currentUserIdOrNull() ?: return FriendMutationResult.Failure(authRequiredMessage())
        if (receiverId.isBlank() || receiverId == userId) return FriendMutationResult.Failure("A friend request needs another Work Social member.")
        return mutate { postgrest.from("friend_requests").insert(FriendRequestPayload(userId, receiverId, "pending")) }
    }

    override suspend fun acceptRequest(requestId: String): FriendMutationResult {
        val userId = currentUserIdOrNull() ?: return FriendMutationResult.Failure(authRequiredMessage())
        if (requestId.isBlank()) return FriendMutationResult.Failure("Friend request ID cannot be empty.")
        return runCatching {
            val request = postgrest.from("friend_requests").select(columns = Columns.list("id, sender_id, receiver_id, status, created_at")) {
                filter { eq("id", requestId); eq("receiver_id", userId); eq("status", "pending") }
            }.decodeList<FriendRequestDto>().firstOrNull() ?: return FriendMutationResult.Failure("That friend request is no longer pending.")
            postgrest.from("friend_requests").update(StatusPayload("accepted")) { filter { eq("id", request.id); eq("receiver_id", userId); eq("status", "pending") } }
            val (a, b) = listOf(userId, request.sender_id).sorted()
            postgrest.from("friends").insert(FriendshipPayload(a, b))
            FriendMutationResult.Success
        }.getOrElse { FriendMutationResult.Failure(it.message?.takeIf(String::isNotBlank) ?: "Unable to accept this friend request right now.") }
    }

    override suspend fun rejectRequest(requestId: String): FriendMutationResult = updateRequestStatus(requestId, "rejected")

    override suspend fun cancelRequest(requestId: String): FriendMutationResult {
        val userId = currentUserIdOrNull() ?: return FriendMutationResult.Failure(authRequiredMessage())
        if (requestId.isBlank()) return FriendMutationResult.Failure("Friend request ID cannot be empty.")
        return mutate { postgrest.from("friend_requests").delete { filter { eq("id", requestId); eq("sender_id", userId); eq("status", "pending") } } }
    }

    private suspend fun updateRequestStatus(requestId: String, status: String): FriendMutationResult {
        val userId = currentUserIdOrNull() ?: return FriendMutationResult.Failure(authRequiredMessage())
        if (requestId.isBlank()) return FriendMutationResult.Failure("Friend request ID cannot be empty.")
        return mutate { postgrest.from("friend_requests").update(StatusPayload(status)) { filter { eq("id", requestId); eq("receiver_id", userId); eq("status", "pending") } } }
    }

    private suspend fun mutate(operation: suspend () -> Unit): FriendMutationResult = runCatching {
        operation()
        FriendMutationResult.Success
    }.getOrElse { FriendMutationResult.Failure(it.message?.takeIf(String::isNotBlank) ?: "Unable to update this friendship right now.") }

    private fun currentUserIdOrNull(): String? = auth.currentSessionOrNull()?.user?.id
    private fun authRequiredMessage() = "Your Work Social session is no longer active. Please sign in again."
}
