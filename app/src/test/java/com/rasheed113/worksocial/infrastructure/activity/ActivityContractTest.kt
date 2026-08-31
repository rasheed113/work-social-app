package com.rasheed113.worksocial.infrastructure.activity

import com.rasheed113.worksocial.domain.activity.ActivityActor
import com.rasheed113.worksocial.domain.activity.ActivityNotification
import com.rasheed113.worksocial.domain.activity.ActivityType
import com.rasheed113.worksocial.presentation.activity.activityAction
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ActivityContractTest {
    @Test
    fun mapsAllBackendEventTypesAndUnknownSafely() {
        val known = listOf(
            "like" to ActivityType.LIKE,
            "comment" to ActivityType.COMMENT,
            "comment_reply" to ActivityType.COMMENT_REPLY,
            "mention_post" to ActivityType.MENTION_POST,
            "mention_comment" to ActivityType.MENTION_COMMENT,
            "follow" to ActivityType.FOLLOW,
            "message" to ActivityType.MESSAGE,
            "friend_request" to ActivityType.FRIEND_REQUEST,
            "friend_accept" to ActivityType.FRIEND_ACCEPT,
        )
        known.forEach { (raw, expected) -> assertEquals(expected, ActivityType.fromRaw(raw)) }
        assertEquals(ActivityType.UNKNOWN, ActivityType.fromRaw("future_backend_event"))
    }

    @Test
    fun mapsRealNotificationFieldsAndActorProfileWithoutFabricatingData() {
        val row = ActivityRowDto(
            id = "notification-1",
            receiver_id = "receiver-1",
            sender_id = "sender-1",
            type = "comment_reply",
            post_id = "post-1",
            comment_id = "comment-1",
            is_read = false,
            created_at = "2026-08-31T17:00:00Z",
            metadata = buildJsonObject { put("parent_comment_id", "parent-1") },
        )
        val actor = ActivityActorDto("sender-1", "Alice", "alice", "https://example.invalid/a.png")

        val mapped = mapActivityRows(listOf(row), mapOf(actor.id to actor)).single()

        assertEquals("notification-1", mapped.id)
        assertEquals("receiver-1", mapped.receiverId)
        assertEquals("sender-1", mapped.senderId)
        assertEquals(ActivityType.COMMENT_REPLY, mapped.type)
        assertEquals("post-1", mapped.postId)
        assertEquals("comment-1", mapped.commentId)
        assertEquals(false, mapped.isRead)
        assertEquals("2026-08-31T17:00:00Z", mapped.createdAt)
        assertEquals("parent-1", mapped.metadata["parent_comment_id"]?.toString()?.trim('"'))
        assertEquals(ActivityActor("Alice", "alice", "https://example.invalid/a.png"), mapped.actor)
    }

    @Test
    fun missingActorDoesNotCreateFakeProfile() {
        val row = ActivityRowDto(
            id = "notification-2",
            receiver_id = "receiver-1",
            sender_id = "deleted-sender",
            type = "like",
            is_read = true,
            created_at = "2026-08-31T17:00:00Z",
        )

        val mapped = mapActivityRows(listOf(row), emptyMap()).single()

        assertNull(mapped.actor)
    }

    @Test
    fun unreadCountComesOnlyFromPersistedReadState() {
        val items = listOf(
            notification("1", false),
            notification("2", true),
            notification("3", false),
        )

        assertEquals(2, unreadActivityCount(items))
    }

    @Test
    fun labelsMatchWebsiteContractAndUnknownUsesSafeFallback() {
        assertEquals("liked your post", activityAction(ActivityType.LIKE))
        assertEquals("commented on your post", activityAction(ActivityType.COMMENT))
        assertEquals("accepted your friend request", activityAction(ActivityType.FRIEND_ACCEPT))
        assertEquals("sent you a notification", activityAction(ActivityType.UNKNOWN))
    }

    private fun notification(id: String, isRead: Boolean) = ActivityNotification(
        id = id,
        receiverId = "receiver",
        senderId = "sender",
        type = ActivityType.LIKE,
        postId = null,
        commentId = null,
        isRead = isRead,
        createdAt = "2026-08-31T17:00:00Z",
        metadata = buildJsonObject {},
        actor = null,
    )
}
