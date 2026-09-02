package com.rasheed113.worksocial.presentation.ai

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.ai.AiMessage
import com.rasheed113.worksocial.domain.ai.AiPendingAction
import com.rasheed113.worksocial.domain.ai.AiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface AiUiState {
    data object Loading : AiUiState
    data class Ready(
        val conversationId: String? = null,
        val messages: List<AiMessage> = emptyList(),
        val pendingAction: AiPendingAction? = null,
        val sending: Boolean = false,
        val confirming: Boolean = false,
        val error: String? = null,
    ) : AiUiState
}

class AiViewModel(private val repository: AiRepository) : ViewModel() {
    private val _state = MutableStateFlow<AiUiState>(AiUiState.Loading)
    val state: StateFlow<AiUiState> = _state.asStateFlow()

    init { restore() }

    private fun ready(transform: (AiUiState.Ready) -> AiUiState.Ready) {
        val current = _state.value as? AiUiState.Ready ?: AiUiState.Ready()
        _state.value = transform(current)
    }

    fun restore() {
        viewModelScope.launch {
            _state.value = AiUiState.Loading
            runCatching { repository.loadHistory() }
                .onSuccess { history -> _state.value = AiUiState.Ready(conversationId = history?.conversationId, messages = history?.messages ?: emptyList()) }
                .onFailure { error -> _state.value = AiUiState.Ready(error = error.message ?: "Could not restore AI conversation.") }
        }
    }

    fun send(message: String) {
        val trimmed = message.trim()
        if (trimmed.isEmpty()) return
        val current = _state.value as? AiUiState.Ready ?: return
        ready { it.copy(sending = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.sendMessage(current.conversationId, trimmed) }
                .onSuccess { result ->
                    val now = java.time.Instant.now().toString()
                    ready {
                        it.copy(
                            conversationId = result.conversationId,
                            messages = it.messages + AiMessage("local-user-$now", "user", trimmed, now) + AiMessage("local-assistant-$now", "assistant", result.message, now),
                            pendingAction = result.pendingActions.firstOrNull(),
                            sending = false,
                            error = null,
                        )
                    }
                }
                .onFailure { error -> ready { state -> state.copy(sending = false, error = error.message ?: "AI request failed.") } }
        }
    }

    fun confirm(action: AiPendingAction) {
        ready { it.copy(confirming = true, error = null) }
        viewModelScope.launch {
            runCatching { repository.confirmAction(action.id) }
                .onSuccess { result ->
                    val now = java.time.Instant.now().toString()
                    val text = if (result.success && result.entry != null) "Done 😎 Real entry created: ${result.entry.title ?: result.entry.content}" else "I couldn't confirm that entry."
                    ready { it.copy(messages = it.messages + AiMessage("local-confirm-$now", "assistant", text, now), pendingAction = null, confirming = false, error = null) }
                }
                .onFailure { error -> ready { state -> state.copy(confirming = false, error = error.message ?: "Confirmation failed.") } }
        }
    }

    fun cancel(action: AiPendingAction) {
        viewModelScope.launch {
            runCatching { repository.cancelAction(action.id) }
                .onSuccess { ready { it.copy(pendingAction = null, error = null) } }
                .onFailure { error -> ready { state -> state.copy(error = error.message ?: "Could not cancel the action.") } }
        }
    }
}

class AiViewModelFactory(private val repository: AiRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = AiViewModel(repository) as T
}
