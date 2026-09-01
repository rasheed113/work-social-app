package com.rasheed113.worksocial.presentation.work

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.work.WorkHistoryCursor
import com.rasheed113.worksocial.domain.work.WorkHistoryEntry
import com.rasheed113.worksocial.domain.work.WorkHouseRepository
import com.rasheed113.worksocial.domain.work.WorkerFinanceSummary
import com.rasheed113.worksocial.domain.work.WorkerIdentity
import com.rasheed113.worksocial.domain.work.WorkerWorkTotals
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface WorkHouseState {
    data object Loading : WorkHouseState
    data class Success(
        val identity: WorkerIdentity?,
        val totals: WorkerWorkTotals,
        val finance: WorkerFinanceSummary?,
        val history: List<WorkHistoryEntry>,
        val hasMoreHistory: Boolean,
        val loadingMoreHistory: Boolean = false,
        val error: String? = null,
    ) : WorkHouseState
    data class Error(val message: String) : WorkHouseState
}

class WorkHouseViewModel(
    private val repository: WorkHouseRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<WorkHouseState>(WorkHouseState.Loading)
    val state: StateFlow<WorkHouseState> = _state.asStateFlow()

    private var profileId: String? = null
    private var historyCursor: WorkHistoryCursor? = null

    fun load(profileId: String) {
        if (profileId == this.profileId && _state.value !is WorkHouseState.Error) return
        this.profileId = profileId
        historyCursor = null
        _state.value = WorkHouseState.Loading
        viewModelScope.launch {
            runCatching {
                val identity = repository.getWorkerIdentity(profileId)
                val totals = repository.getWorkerWorkTotals()
                val finance = repository.getWorkerFinanceSummary()
                val page = repository.getWorkerHistory(limit = 50)
                historyCursor = page.nextCursor
                WorkHouseState.Success(identity, totals, finance, page.entries, page.hasMore)
            }.onSuccess { _state.value = it }
                .onFailure { _state.value = WorkHouseState.Error(it.message ?: "Work House request failed.") }
        }
    }

    fun retry() {
        profileId?.let {
            profileId = null
            load(it)
        }
    }

    fun loadMoreHistory() {
        val current = _state.value as? WorkHouseState.Success ?: return
        if (!current.hasMoreHistory || current.loadingMoreHistory) return
        _state.value = current.copy(loadingMoreHistory = true, error = null)
        viewModelScope.launch {
            runCatching { repository.getWorkerHistory(50, historyCursor) }
                .onSuccess { page ->
                    historyCursor = page.nextCursor
                    _state.value = current.copy(
                        history = current.history + page.entries,
                        hasMoreHistory = page.hasMore,
                        loadingMoreHistory = false,
                    )
                }
                .onFailure { _state.value = current.copy(loadingMoreHistory = false, error = it.message ?: "Unable to load more work history.") }
        }
    }
}

class WorkHouseViewModelFactory(
    private val repository: WorkHouseRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = WorkHouseViewModel(repository) as T
}
