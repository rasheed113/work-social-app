package com.rasheed113.worksocial.presentation.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.friends.FriendMutationResult
import com.rasheed113.worksocial.domain.friends.FriendsData
import com.rasheed113.worksocial.domain.friends.FriendsRepository
import com.rasheed113.worksocial.domain.friends.FriendsResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FriendsUiState {
    data object Loading : FriendsUiState
    data class Success(val data: FriendsData, val busyRequestIds: Set<String> = emptySet()) : FriendsUiState
    data class Error(val message: String) : FriendsUiState
}

class FriendsViewModel(private val repository: FriendsRepository) : ViewModel() {
    private val _state = MutableStateFlow<FriendsUiState>(FriendsUiState.Loading)
    val state: StateFlow<FriendsUiState> = _state.asStateFlow()

    fun load() {
        _state.value = FriendsUiState.Loading
        viewModelScope.launch {
            _state.value = when (val result = repository.getFriends()) {
                is FriendsResult.Success -> FriendsUiState.Success(result.data)
                is FriendsResult.Failure -> FriendsUiState.Error(result.message)
            }
        }
    }

    fun sendRequest(receiverId: String) = mutate(receiverId) { repository.sendRequest(receiverId) }
    fun acceptRequest(requestId: String) = mutate(requestId) { repository.acceptRequest(requestId) }
    fun rejectRequest(requestId: String) = mutate(requestId) { repository.rejectRequest(requestId) }
    fun cancelRequest(requestId: String) = mutate(requestId) { repository.cancelRequest(requestId) }

    private fun mutate(id: String, operation: suspend () -> FriendMutationResult) {
        val current = _state.value as? FriendsUiState.Success ?: return
        if (id in current.busyRequestIds) return
        _state.value = current.copy(busyRequestIds = current.busyRequestIds + id)
        viewModelScope.launch {
            when (val result = operation()) {
                FriendMutationResult.Success -> reloadAfterMutation()
                is FriendMutationResult.Failure -> {
                    _state.value = FriendsUiState.Error(result.message)
                }
            }
        }
    }

    private suspend fun reloadAfterMutation() {
        _state.value = when (val result = repository.getFriends()) {
            is FriendsResult.Success -> FriendsUiState.Success(result.data)
            is FriendsResult.Failure -> FriendsUiState.Error(result.message)
        }
    }
}
