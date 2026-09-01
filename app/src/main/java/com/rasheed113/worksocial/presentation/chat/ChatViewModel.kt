package com.rasheed113.worksocial.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.chat.ChatRepository
import com.rasheed113.worksocial.domain.chat.ChatState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatViewModel(private val repository: ChatRepository) : ViewModel() {
    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    private var realtimeJob: Job? = null

    fun load(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            repository.load(userId).fold(
                onSuccess = { _state.update { s -> s.copy(loading = false, conversations = it) } },
                onFailure = { _state.update { s -> s.copy(loading = false, error = it.message ?: "Unable to load conversations.") } }
            )
        }
    }

    fun select(userId: String, conversationId: String) {
        _state.update { it.copy(selectedConversationId = conversationId, error = null) }
        realtimeJob?.cancel()
        viewModelScope.launch {
            repository.messages(userId, conversationId).fold(
                onSuccess = { rows -> _state.update { it.copy(messages = rows) }; repository.markRead(userId, conversationId) },
                onFailure = { e -> _state.update { it.copy(error = e.message ?: "Unable to load messages.") } }
            )
        }
        realtimeJob = viewModelScope.launch {
            repository.observeMessages(userId, conversationId).collectLatest {
                repository.messages(userId, conversationId).onSuccess { rows -> _state.update { it.copy(messages = rows) } }
            }
        }
    }

    fun send(userId: String, conversationId: String, content: String) {
        if (content.trim().isEmpty() || _state.value.sending) return
        viewModelScope.launch {
            _state.update { it.copy(sending = true, error = null) }
            repository.sendText(userId, conversationId, content).fold(
                onSuccess = { sent -> _state.update { s -> s.copy(sending = false, messages = if (s.messages.any { it.id == sent.id }) s.messages else s.messages + sent) } },
                onFailure = { e -> _state.update { s -> s.copy(sending = false, error = e.message ?: "Message could not be sent.") } }
            )
        }
    }

    fun openDirect(userId: String, targetProfileId: String) {
        viewModelScope.launch {
            repository.openDirect(userId, targetProfileId).fold(
                onSuccess = { id -> load(userId); select(userId, id) },
                onFailure = { e -> _state.update { it.copy(error = e.message ?: "Could not open chat.") } }
            )
        }
    }

    override fun onCleared() { realtimeJob?.cancel(); super.onCleared() }
}

class ChatViewModelFactory(private val repository: ChatRepository) : androidx.lifecycle.ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ChatViewModel(repository) as T
}
