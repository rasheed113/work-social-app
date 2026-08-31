package com.rasheed113.worksocial.infrastructure.friends

import com.rasheed113.worksocial.domain.friends.RelationshipState
import com.rasheed113.worksocial.domain.friends.relationshipState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FriendsContractTest {
    private val currentUser = "00000000-0000-0000-0000-000000000001"
    private val otherUser = "00000000-0000-0000-0000-000000000002"

    @Test fun profileMappingUsesOnlyRealProfileFields() {
        val profile = mapFriendProfile(ProfileDto(otherUser, "worker.two", "Worker Two", "https://example.com/avatar.jpg"))
        assertEquals(otherUser, profile.id)
        assertEquals("worker.two", profile.username)
        assertEquals("Worker Two", profile.displayName)
        assertEquals("https://example.com/avatar.jpg", profile.avatarUrl)
    }

    @Test fun requestMappingPreservesBackendIdentityAndStatus() {
        val request = mapFriendRequest(
            FriendRequestDto("request-1", otherUser, currentUser, "pending", "2026-08-31T10:00:00Z"),
            mapFriendProfile(ProfileDto(otherUser, "worker.two", "Worker Two", null)),
        )
        assertEquals("request-1", request.id)
        assertEquals(otherUser, request.senderId)
        assertEquals(currentUser, request.receiverId)
        assertEquals("pending", request.status)
        assertEquals(otherUser, request.profile.id)
    }

    @Test fun relationshipStatePrefersFriendshipOverPendingRequest() {
        assertEquals(
            RelationshipState.FRIENDS,
            relationshipState(otherUser, currentUser, setOf(otherUser), setOf(otherUser), setOf(otherUser)),
        )
    }

    @Test fun relationshipStateMapsIncomingPending() {
        assertEquals(RelationshipState.INCOMING_PENDING, relationshipState(otherUser, currentUser, emptySet(), setOf(otherUser), emptySet()))
    }

    @Test fun relationshipStateMapsOutgoingPending() {
        assertEquals(RelationshipState.OUTGOING_PENDING, relationshipState(otherUser, currentUser, emptySet(), emptySet(), setOf(otherUser)))
    }

    @Test fun relationshipStateMapsNone() {
        assertEquals(RelationshipState.NONE, relationshipState(otherUser, currentUser, emptySet(), emptySet(), emptySet()))
    }

    @Test fun relationshipStateNeverTreatsAnotherProfileAsCurrentUser() {
        assertTrue(relationshipState(otherUser, currentUser, emptySet(), emptySet(), emptySet()) != RelationshipState.FRIENDS)
    }
}
