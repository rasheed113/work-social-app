package com.rasheed113.worksocial.infrastructure.ai

import com.rasheed113.worksocial.BuildConfig
import com.rasheed113.worksocial.domain.ai.AiChatResult
import com.rasheed113.worksocial.domain.ai.AiConfirmationResult
import com.rasheed113.worksocial.domain.ai.AiRepository
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.currentSessionOrNull
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

@Serializable
private data class ChatRequest(
    val message: String,
    val conversation_id: String? = null,
)

@Serializable
private data class ConfirmRequest(
    val action: String = "confirm",
    val action_id: String,
)

private val json = Json { ignoreUnknownKeys = true }

class SupabaseAiRepository(
    private val auth: Auth,
) : AiRepository {
    private val client = HttpClient(Android) {
        install(ContentNegotiation) { json(json) }
    }

    private val endpoint = "${BuildConfig.SUPABASE_URL}/functions/v1/work-social-ai"

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

    private fun bearerToken(): String {
        val token = auth.currentSessionOrNull()?.accessToken
        check(!token.isNullOrBlank()) { "Your Work Social session is no longer active. Please sign in again." }
        return "Bearer $token"
    }
}
