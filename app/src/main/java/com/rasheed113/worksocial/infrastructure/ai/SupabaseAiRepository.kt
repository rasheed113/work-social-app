package com.rasheed113.worksocial.infrastructure.ai

import com.rasheed113.worksocial.BuildConfig
import com.rasheed113.worksocial.domain.ai.AiChatResult
import com.rasheed113.worksocial.domain.ai.AiConfirmationResult
import com.rasheed113.worksocial.domain.ai.AiConversationHistory
import com.rasheed113.worksocial.domain.ai.AiMessage
import com.rasheed113.worksocial.domain.ai.AiRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

@Serializable
private data class ChatRequest(val message: String, val conversation_id: String? = null)

@Serializable
private data class ConfirmRequest(val action: String = "confirm", val action_id: String)

@Serializable
private data class AiMessageRow(
    val id: String,
    val role: String,
    val content: String,
    @kotlinx.serialization.SerialName("created_at") val createdAt: String,
)

private val json = Json { ignoreUnknownKeys = true }

class SupabaseAiRepository(
    private val auth: Auth,
    private val postgrest: Postgrest,
) : AiRepository {
    private val client = HttpClient(Android) { install(ContentNegotiation) { json(json) } }
    private val endpoint = "${BuildConfig.SUPABASE_URL}/functions/v1/work-social-ai"

    override suspend fun loadHistory(conversationId: String?): AiConversationHistory? {
        val userId = auth.currentSessionOrNull()?.user?.id ?: return null
        val conversation = if (conversationId != null) {
            postgrest.from("ai_conversations").select(columns = Columns.list("id")) {
                filter { eq("id", conversationId); eq("user_id", userId) }
                limit(1)
            }.decodeList<AiConversationIdRow>().firstOrNull()
        } else {
            postgrest.from("ai_conversations").select(columns = Columns.list("id")) {
                filter { eq("user_id", userId); eq("status", "active") }
                order("updated_at", Order.DESCENDING)
                limit(1)
            }.decodeList<AiConversationIdRow>().firstOrNull()
        } ?: return null

        val rows = postgrest.from("ai_messages").select(columns = Columns.list("id,role,content,created_at")) {
            filter { eq("conversation_id", conversation.id); eq("user_id", userId) }
            order("created_at", Order.ASCENDING)
            limit(100)
        }.decodeList<AiMessageRow>()

        return AiConversationHistory(conversation.id, rows.filter { it.role == "user" || it.role == "assistant" }.map { AiMessage(it.id, it.role, it.content, it.createdAt) })
    }

    override suspend fun sendMessage(conversationId: String?, message: String): AiChatResult = client.post(endpoint) {
        header(HttpHeaders.Authorization, bearerToken())
        contentType(ContentType.Application.Json)
        setBody(ChatRequest(message = message, conversation_id = conversationId))
    }.body()

    override suspend fun confirmAction(actionId: String): AiConfirmationResult = client.post(endpoint) {
        header(HttpHeaders.Authorization, bearerToken())
        contentType(ContentType.Application.Json)
        setBody(ConfirmRequest(action_id = actionId))
    }.body()

    override suspend fun cancelAction(actionId: String) {
        val userId = auth.currentSessionOrNull()?.user?.id ?: error("Your Work Social session is no longer active.")
        postgrest.from("ai_pending_actions").update(buildJsonObject { put("status", "cancelled") }) {
            filter { eq("id", actionId); eq("user_id", userId); eq("status", "pending") }
        }
    }

    private fun bearerToken(): String {
        val token = auth.currentSessionOrNull()?.accessToken
        check(!token.isNullOrBlank()) { "Your Work Social session is no longer active. Please sign in again." }
        return "Bearer $token"
    }
}

@Serializable
private data class AiConversationIdRow(val id: String)
