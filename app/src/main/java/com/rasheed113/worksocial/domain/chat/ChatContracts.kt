package com.rasheed113.worksocial.domain.chat

import kotlinx.coroutines.flow.Flow


data class ChatProfile(val id: String, val displayName: String, val username: String?, val avatarUrl: String?)
data class Conversation(val id: String, val kind: String, val title: String?, val avatarUrl: String?, val updatedAt: String, val peer: ChatProfile?)
data class Message(val id: String, val conversationId: String, val senderId: String, val content: String, val createdAt: String, val readAt: String?, val deletedAt: String?, val editedAt: String?)
data class ChatState(val loading: Boolean = true, val conversations: List<Conversation> = emptyList(), val messages: List<Message> = emptyList(), val selectedConversationId: String? = null, val error: String? = null, val sending: Boolean = false)

interface ChatRepository {
    suspend fun load(userId: String): Result<List<Conversation>>
    suspend fun messages(userId: String, conversationId: String): Result<List<Message>>
    suspend fun openDirect(userId: String, targetProfileId: String): Result<String>
    suspend fun sendText(userId: String, conversationId: String, content: String): Result<Message>
    suspend fun markRead(userId: String, conversationId: String): Result<Unit>
    fun observeMessages(userId: String, conversationId: String): Flow<Unit>
}
