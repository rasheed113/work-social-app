package com.rasheed113.worksocial.presentation.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.activity.ActivityMutationResult
import com.rasheed113.worksocial.domain.activity.ActivityNotification
import com.rasheed113.worksocial.domain.activity.ActivityRepository
import com.rasheed113.worksocial.domain.activity.ActivityState
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ActivityViewModel(private val repository: ActivityRepository) : ViewModel() {
    private val _state = MutableStateFlow<ActivityState>(ActivityState.Loading)
    val state: StateFlow<ActivityState> = _state.asStateFlow()

    private val refreshMutex = Mutex()
    private var realtimeJob: Job? = null

    init {
        load()
        realtimeJob = repository.subscribeToActivity()
            .onEach { refreshSilently() }
            .catch { error ->
                if (_state.value !is ActivityState.Success && _state.value !is ActivityState.Empty) {
                    _state.value = ActivityState.Error(error.message ?: "Activity realtime connection failed.")
                }
            }
            .launchIn(viewModelScope)
    }

    fun load() {
        viewModelScope.launch {
            refreshMutex.withLock {
                _state.value = ActivityState.Loading
                refreshFromBackend()
            }
        }
    }

    fun retry() = load()

    fun markRead(item: ActivityNotification, onPostTarget: (String, String?) -> Unit) {
        if (item.isRead) {
            item.postId?.let { onPostTarget(it, item.commentId) }
            return
        }
        viewModelScope.launch {
            when (val result = repository.markAsRead(item.id)) {
                ActivityMutationResult.Success -> {
                    updateReadState(item.id)
                    item.postId?.let { onPostTarget(it, item.commentId) }
                }
                is ActivityMutationResult.Failure -> _state.value = ActivityState.Error(result.message)
            }
        }
    }

    fun markAllRead() {
        viewModelScope.launch {
            when (val result = repository.markAllAsRead()) {
                ActivityMutationResult.Success -> refreshSilently()
                is ActivityMutationResult.Failure -> _state.value = ActivityState.Error(result.message)
            }
        }
    }

    private suspend fun refreshSilently() {
        refreshMutex.withLock { refreshFromBackend() }
    }

    private suspend fun refreshFromBackend() {
        try {
            val items = repository.getActivity()
            _state.value = if (items.isEmpty()) {
                ActivityState.Empty
            } else {
                ActivityState.Success(items, items.count { !it.isRead })
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            _state.value = ActivityState.Error(error.message ?: "Unable to load Activity right now.")
        }
    }

    private fun updateReadState(id: String) {
        val current = _state.value
        if (current is ActivityState.Success) {
            val updated = current.items.map { if (it.id == id) it.copy(isRead = true) else it }
            _state.value = ActivityState.Success(updated, updated.count { !it.isRead })
        }
    }

    override fun onCleared() {
        realtimeJob?.cancel()
        super.onCleared()
    }
}
