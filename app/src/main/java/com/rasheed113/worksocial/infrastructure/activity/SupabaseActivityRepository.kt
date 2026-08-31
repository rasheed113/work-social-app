package com.rasheed113.worksocial.infrastructure.activity

import com.rasheed113.worksocial.domain.activity.ActivityActor
import com.rasheed113.worksocial.domain.activity.ActivityMutationResult
import com.rasheed113.worksocial.domain.activity.ActivityNotification
import com.rasheed113.worksocial.domain.activity.ActivityRepository
import com.rasheed113.worksocial.domain.activity.ActivityType
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
internal data class ActivityRowDto(
    val id: String,
    val receiver_id: String,
    val sender_id: String,
    val type: String,
    val post_id: String? = null,
    val comment_id: String? = null,
    val is_read: Boolean,
    val created_at: String,
    val metadata: JsonObject = JsonObject(emptyMap()),
)

@Serializable
internal data class ActivityActorDto(
    val id: String,
    val display_name: String? = null,
    val username: String? = null,
    val avatar_url: String? = null,
)

internal fun mapActivityRows(
    rows: List<ActivityRowDto>,
    actors: Map<String, ActivityActorDto>,
): List<ActivityNotification> = rows.map { row ->
    ActivityNotification(
        id = row.id,
        receiverId = row.receiver_id,
        senderId = row.sender_id,
        type = ActivityType.fromRaw(row.type),
        postId = row.post_id,
        commentId = row.comment_id,
        isRead = row.is_read,
        createdAt = row.created_at,
        metadata = row.metadata,
        actor = actors[row.sender_id]?.let { ActivityActor(it.display_name, it.username, it.avatar_url) },
    )
}

internal fun unreadActivityCount(items: List<ActivityNotification>): Int = items.count { !it.isRead }

class SupabaseActivityRepository(
    private val postgrest: Postgrest,
    private val auth: Auth,
    private val realtime: Realtime,
) : ActivityRepository {
    companion object {
        const val PAGE_SIZE = 100L
    }

    override suspend fun getActivity(): List<ActivityNotification> {
        val userId = requireActiveSession()
        val rows = postgrest.from("notifications").select(
            columns = Columns.list("id, receiver_id, sender_id, type, post_id, comment_id, is_read, created_at, metadata"),
        ) {
            filter { eq("receiver_id", userId) }
            order(column = "created_at", order = Order.DESCENDING)
            limit(PAGE_SIZE)
        }.decodeList<ActivityRowDto>()

        if (rows.isEmpty()) return emptyList()

        val senderIds = rows.map(ActivityRowDto::sender_id).distinct()
        val actors = postgrest.from("profiles").select(
            columns = Columns.list("id, display_name, username, avatar_url"),
        ) {
            filter { isIn("id", senderIds) }
        }.decodeList<ActivityActorDto>().associateBy(ActivityActorDto::id)

        return mapActivityRows(rows, actors)
    }

    override suspend fun markAsRead(id: String): ActivityMutationResult {
        val userId = auth.currentSessionOrNull()?.user?.id
            ?: return ActivityMutationResult.Failure("Your Work Social session is no longer active. Please sign in again.")
        if (id.isBlank()) return ActivityMutationResult.Failure("Notification ID cannot be empty.")

        return runCatching {
            postgrest.from("notifications").update(mapOf("is_read" to true)) {
                filter {
                    eq("id", id)
                    eq("receiver_id", userId)
                }
            }
            ActivityMutationResult.Success
        }.getOrElse {
            ActivityMutationResult.Failure(it.message?.takeIf(String::isNotBlank) ?: "Unable to mark this notification as read.")
        }
    }

    override suspend fun markAllAsRead(): ActivityMutationResult {
        val userId = auth.currentSessionOrNull()?.user?.id
            ?: return ActivityMutationResult.Failure("Your Work Social session is no longer active. Please sign in again.")

        return runCatching {
            postgrest.from("notifications").update(mapOf("is_read" to true)) {
                filter {
                    eq("receiver_id", userId)
                    eq("is_read", false)
                }
            }
            ActivityMutationResult.Success
        }.getOrElse {
            ActivityMutationResult.Failure(it.message?.takeIf(String::isNotBlank) ?: "Unable to mark notifications as read.")
        }
    }

    override fun subscribeToActivity(): Flow<Unit> = callbackFlow {
        val userId = auth.currentSessionOrNull()?.user?.id
            ?: run {
                close(IllegalStateException("Your Work Social session is no longer active. Please sign in again."))
                return@callbackFlow
            }

        val channel = realtime.channel("notifications:$userId")
        val insertJob = launch {
            channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "notifications"
                filter("receiver_id", FilterOperator.EQ, userId)
            }.collect { trySend(Unit).isSuccess }
        }
        val updateJob = launch {
            channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "notifications"
                filter("receiver_id", FilterOperator.EQ, userId)
            }.collect { trySend(Unit).isSuccess }
        }

        runCatching { channel.subscribe(blockUntilSubscribed = true) }
            .onFailure { close(it) }

        awaitClose {
            insertJob.cancel()
            updateJob.cancel()
            launch { realtime.removeChannel(channel) }
        }
    }

    private fun requireActiveSession(): String = auth.currentSessionOrNull()?.user?.id
        ?: error("Your Work Social session is no longer active. Please sign in again.")
}
