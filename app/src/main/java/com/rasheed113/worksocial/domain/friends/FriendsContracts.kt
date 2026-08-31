package com.rasheed113.worksocial.domain.friends

/** Real relationship state derived from the Work Social friend_requests/friends tables. */
enum class RelationshipState { NONE, OUTGOING_PENDING, INCOMING_PENDING, FRIENDS }

data class FriendProfile(
    val id: String,
    val username: String,
    val displayName: String,
    val avatarUrl: String?,
)

data class FriendRequest(
    val id: String,
    val senderId: String,
    val receiverId: String,
    val status: String,
    val createdAt: String,
    val profile: FriendProfile,
)

data class FriendPerson(
    val profile: FriendProfile,
    val relationship: RelationshipState,
)

data class FriendsData(
    val people: List<FriendPerson>,
    val incomingRequests: List<FriendRequest>,
    val outgoingRequests: List<FriendRequest>,
)

sealed interface FriendsResult {
    data class Success(val data: FriendsData) : FriendsResult
    data class Failure(val message: String) : FriendsResult
}

sealed interface FriendMutationResult {
    data object Success : FriendMutationResult
    data class Failure(val message: String) : FriendMutationResult
}

interface FriendsRepository {
    suspend fun getFriends(): FriendsResult
    suspend fun sendRequest(receiverId: String): FriendMutationResult
    suspend fun acceptRequest(requestId: String): FriendMutationResult
    suspend fun rejectRequest(requestId: String): FriendMutationResult
    suspend fun cancelRequest(requestId: String): FriendMutationResult
}

internal fun relationshipState(
    profileId: String,
    currentUserId: String,
    friendIds: Set<String>,
    incomingBySender: Set<String>,
    outgoingByReceiver: Set<String>,
): RelationshipState = when {
    profileId == currentUserId -> RelationshipState.FRIENDS
    profileId in friendIds -> RelationshipState.FRIENDS
    profileId in incomingBySender -> RelationshipState.INCOMING_PENDING
    profileId in outgoingByReceiver -> RelationshipState.OUTGOING_PENDING
    else -> RelationshipState.NONE
}
