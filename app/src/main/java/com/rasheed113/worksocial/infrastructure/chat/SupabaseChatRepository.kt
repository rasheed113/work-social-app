package com.rasheed113.worksocial.infrastructure.chat

import com.rasheed113.worksocial.domain.chat.ChatProfile
import com.rasheed113.worksocial.domain.chat.ChatRepository
import com.rasheed113.worksocial.domain.chat.Conversation
import com.rasheed113.worksocial.domain.chat.Message
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.decodeSingle
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
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable private data class ConversationRow(val id: String, val kind: String, val title: String? = null, val avatar_url: String? = null, val updated_at: String)
@Serializable private data class MemberRow(val conversation_id: String, val profile_id: String)
@Serializable private data class ProfileRow(val id: String, val display_name: String? = null, val username: String? = null, val avatar_url: String? = null)
@Serializable private data class MessageRow(val id: String, val conversation_id: String, val sender_id: String, val content: String, val created_at: String, val read_at: String? = null, val deleted_at: String? = null, val edited_at: String? = null)
@Serializable private data class TextPayload(val conversation_id: String, val content: String)
@Serializable private data class ReadPayload(val last_read_at: String)

class SupabaseChatRepository(private val postgrest: Postgrest, private val auth: Auth, private val realtime: Realtime) : ChatRepository {
    private fun requireUser(userId: String) { check(auth.currentSessionOrNull()?.user?.id == userId) { "Your Work Social session is no longer active. Please sign in again." } }

    override suspend fun load(userId: String): Result<List<Conversation>> = runCatching {
        requireUser(userId)
        val memberships = postgrest.from("conversation_members").select(columns = Columns.list("conversation_id,profile_id")) {
            filter { eq("profile_id", userId) }
        }.decodeList<MemberRow>()
        val ids = memberships.map { it.conversation_id }.distinct()
        if (ids.isEmpty()) return@runCatching emptyList()
        val conversations = postgrest.from("conversations").select(columns = Columns.list("id,kind,title,avatar_url,updated_at")) {
            filter { isIn("id", ids) }
            order(column = "updated_at", order = Order.DESCENDING)
        }.decodeList<ConversationRow>()
        val members = postgrest.from("conversation_members").select(columns = Columns.list("conversation_id,profile_id")) {
            filter { isIn("conversation_id", ids) }
        }.decodeList<MemberRow>()
        val peerIds = members.filter { it.profile_id != userId }.map { it.profile_id }.distinct()
        val profiles = if (peerIds.isEmpty()) emptyList() else postgrest.from("profiles").select(columns = Columns.list("id,display_name,username,avatar_url")) {
            filter { isIn("id", peerIds) }
        }.decodeList<ProfileRow>()
        val profileById = profiles.associateBy { it.id }
        val peerByConversation = members.filter { it.profile_id != userId }.groupBy { it.conversation_id }.mapValues { (_, rows) -> profileById[rows.firstOrNull()?.profile_id] }
        conversations.map { c ->
            Conversation(c.id, c.kind, c.title, c.avatar_url, c.updated_at, peerByConversation[c.id]?.let {
                ChatProfile(it.id, it.display_name ?: it.username ?: "User", it.username, it.avatar_url)
            })
        }
    }

    override suspend fun messages(userId: String, conversationId: String): Result<List<Message>> = runCatching {
        requireUser(userId)
        postgrest.from("messages").select(columns = Columns.list("id,conversation_id,sender_id,content,created_at,read_at,deleted_at,edited_at")) {
            filter { eq("conversation_id", conversationId) }
            order(column = "created_at", order = Order.ASCENDING)
        }.decodeList<MessageRow>().map { it.toModel() }
    }

    override suspend fun openDirect(userId: String, targetProfileId: String): Result<String> = runCatching {
        requireUser(userId)
        require(targetProfileId.isNotBlank() && targetProfileId != userId) { "A chat needs another Work Social member." }
        val response = postgrest.rpc("create_direct_conversation", buildJsonObject { put("target_profile", targetProfileId) })
        response.decodeList<String>().firstOrNull() ?: error("The conversation could not be created.")
    }

    override suspend fun sendText(userId: String, conversationId: String, content: String): Result<Message> = runCatching {
        requireUser(userId)
        require(conversationId.isNotBlank()) { "Conversation ID cannot be empty." }
        val normalized = content.trim()
        require(normalized.isNotEmpty()) { "Message cannot be empty." }
        require(normalized.length <= 10_000) { "Message is too long. Maximum length is 10,000 characters." }
        val inserted = postgrest.from("messages").insert(TextPayload(conversationId, normalized)) {
            select(columns = Columns.list("id,conversation_id,sender_id,content,created_at,read_at,deleted_at,edited_at"))
        }.decodeSingle<MessageRow>()
        check(inserted.sender_id == userId) { "The server returned a message owned by another user." }
        inserted.toModel()
    }

    override suspend fun markRead(userId: String, conversationId: String): Result<Unit> = runCatching {
        requireUser(userId)
        require(conversationId.isNotBlank()) { "Conversation ID cannot be empty." }
        postgrest.from("conversation_members").update(ReadPayload(java.time.Instant.now().toString())) {
            filter { eq("conversation_id", conversationId); eq("profile_id", userId) }
        }
    }

    override fun observeMessages(userId: String, conversationId: String): Flow<Unit> = callbackFlow {
        val activeUserId = auth.currentSessionOrNull()?.user?.id
        if (activeUserId != userId) {
            close(IllegalStateException("Your Work Social session is no longer active. Please sign in again."))
            return@callbackFlow
        }
        require(conversationId.isNotBlank()) { "Conversation ID cannot be empty." }
        val channel = realtime.channel("android-chat:$userId:$conversationId")
        val insertJob = launch {
            channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "messages"
                filter("conversation_id", FilterOperator.EQ, conversationId)
            }.collect { trySend(Unit).isSuccess }
        }
        val updateJob = launch {
            channel.postgresChangeFlow<PostgresAction.Update>(schema = "public") {
                table = "messages"
                filter("conversation_id", FilterOperator.EQ, conversationId)
            }.collect { trySend(Unit).isSuccess }
        }
        val deleteJob = launch {
            channel.postgresChangeFlow<PostgresAction.Delete>(schema = "public") {
                table = "messages"
                filter("conversation_id", FilterOperator.EQ, conversationId)
            }.collect { trySend(Unit).isSuccess }
        }
        runCatching { channel.subscribe(blockUntilSubscribed = true) }
            .onFailure { close(it) }
        awaitClose {
            insertJob.cancel()
            updateJob.cancel()
            deleteJob.cancel()
            launch { realtime.removeChannel(channel) }
        }
    }

    private fun MessageRow.toModel() = Message(id, conversation_id, sender_id, content, created_at, read_at, deleted_at, edited_at)
}
