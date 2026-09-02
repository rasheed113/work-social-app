package com.rasheed113.worksocial.domain.ai

import kotlinx.serialization.Serializable

@Serializable
data class AiPendingAction(
    val id: String,
    val displaySummary: String,
    val expiresAt: String,
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
    suspend fun sendMessage(conversationId: String?, message: String): AiChatResult
    suspend fun confirmAction(actionId: String): AiConfirmationResult
}
