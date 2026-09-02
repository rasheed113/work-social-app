package com.rasheed113.worksocial.domain.ai

import kotlinx.serialization.Serializable

@Serializable
data class AiPendingAction(
    val id: String,
    val displaySummary: String,
    val expiresAt: String,
)

@Serializable
data class AiMessage(
    val id: String,
    val role: String,
    val content: String,
    val createdAt: String,
)

@Serializable
data class AiConversationHistory(
    val conversationId: String,
    val messages: List<AiMessage> = emptyList(),
)

@Serializable
data class AiChatResult(
    val conversationId: String,
    val message: String,
    val pendingActions: List<AiPendingAction> = emptyList(),
)

@Serializable
data class AiCreatedEntry(
    val id: String,
    val entryType: String,
    val title: String? = null,
    val content: String,
    val completed: Boolean? = null,
)

@Serializable
data class AiConfirmationResult(
    val success: Boolean,
    val entry: AiCreatedEntry? = null,
)

interface AiRepository {
    suspend fun loadHistory(conversationId: String? = null): AiConversationHistory?
    suspend fun sendMessage(conversationId: String?, message: String): AiChatResult
    suspend fun confirmAction(actionId: String): AiConfirmationResult
    suspend fun cancelAction(actionId: String)
}
