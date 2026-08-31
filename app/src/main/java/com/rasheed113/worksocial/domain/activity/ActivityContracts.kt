package com.rasheed113.worksocial.domain.activity

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonObject

/** The notification event types currently emitted by the Work Social backend. */
enum class ActivityType(val rawValue: String) {
    LIKE("like"),
    COMMENT("comment"),
    COMMENT_REPLY("comment_reply"),
    MENTION_POST("mention_post"),
    MENTION_COMMENT("mention_comment"),
    FOLLOW("follow"),
    MESSAGE("message"),
    FRIEND_REQUEST("friend_request"),
    FRIEND_ACCEPT("friend_accept"),
    UNKNOWN("unknown");

    companion object {
        fun fromRaw(value: String): ActivityType = entries.firstOrNull { it.rawValue == value } ?: UNKNOWN
    }
}

data class ActivityActor(
    val displayName: String?,
    val username: String?,
    val avatarUrl: String?,
)

data class ActivityNotification(
    val id: String,
    val receiverId: String,
    val senderId: String,
    val type: ActivityType,
    val postId: String?,
    val commentId: String?,
    val isRead: Boolean,
    val createdAt: String,
    val metadata: JsonObject,
    val actor: ActivityActor?,
)

sealed interface ActivityMutationResult {
    data object Success : ActivityMutationResult
    data class Failure(val message: String) : ActivityMutationResult
}

interface ActivityRepository {
    suspend fun getActivity(): List<ActivityNotification>
    suspend fun markAsRead(id: String): ActivityMutationResult
    suspend fun markAllAsRead(): ActivityMutationResult
    fun subscribeToActivity(): Flow<Unit>
}

sealed interface ActivityState {
    data object Loading : ActivityState
    data class Success(
        val items: List<ActivityNotification>,
        val unreadCount: Int,
    ) : ActivityState
    data object Empty : ActivityState
    data class Error(val message: String) : ActivityState
}
