package com.rasheed113.worksocial.presentation.work

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.work.FinanceHistoryCursors
import com.rasheed113.worksocial.domain.work.FinanceHistoryFilter
import com.rasheed113.worksocial.domain.work.FinanceListEntry
import com.rasheed113.worksocial.domain.work.FinanceReceivedType
import com.rasheed113.worksocial.domain.work.WorkHouseRepository
import com.rasheed113.worksocial.domain.work.WorkerFinanceSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Instant

sealed interface FinanceState {
    data object Loading : FinanceState
    data class Success(
        val summary: WorkerFinanceSummary,
        val entries: List<FinanceListEntry>,
        val filter: FinanceHistoryFilter,
        val cursors: FinanceHistoryCursors,
        val hasMore: Boolean,
        val loadingMore: Boolean = false,
        val saving: Boolean = false,
        val notice: String? = null,
        val error: String? = null,
    ) : FinanceState
    data class Error(val message: String) : FinanceState
}

class FinanceViewModel(private val repository: WorkHouseRepository) : ViewModel() {
    private val _state = MutableStateFlow<FinanceState>(FinanceState.Loading)
    val state: StateFlow<FinanceState> = _state.asStateFlow()
    private var profileId: String? = null

    fun load(profileId: String, filter: FinanceHistoryFilter = FinanceHistoryFilter.all) {
        if (this.profileId == profileId && (_state.value as? FinanceState.Success)?.filter == filter) return
        this.profileId = profileId
        _state.value = FinanceState.Loading
        viewModelScope.launch { fetchInitial(profileId, filter) }
    }

    fun setFilter(filter: FinanceHistoryFilter) {
        val id = profileId ?: return
        if ((_state.value as? FinanceState.Success)?.filter == filter) return
        _state.value = FinanceState.Loading
        viewModelScope.launch { fetchInitial(id, filter) }
    }

    fun add(type: FinanceReceivedType, amount: String) = mutate("Saved successfully") { id -> repository.addFinanceReceived(id, type, amount) }
    fun edit(recordId: String, type: FinanceReceivedType, amount: String) = mutate("Updated successfully") { id -> repository.editFinanceReceived(id, recordId, type, amount) }
    fun delete(recordId: String) = mutate("Deleted — you can restore it") { id -> repository.softDeleteFinanceReceived(id, recordId) }
    fun restore(recordId: String) = mutate("Restored successfully") { id -> repository.restoreFinanceReceived(id, recordId) }

    fun clearNotice() {
        val current = _state.value as? FinanceState.Success ?: return
        _state.value = current.copy(notice = null)
    }

    fun loadMore() {
        val current = _state.value as? FinanceState.Success ?: return
        val id = profileId ?: return
        if (!current.hasMore || current.loadingMore || current.saving) return
        _state.value = current.copy(loadingMore = true, error = null)
        viewModelScope.launch {
            runCatching { repository.getWorkerFinanceHistory(id, current.filter, 10, current.cursors) }
                .onSuccess { page ->
                    val additions = mergeAndSort(page.earnings.map(FinanceListEntry::Earning) + page.received.map(FinanceListEntry::Received))
                    _state.value = current.copy(
                        entries = mergeAndSort(current.entries + additions),
                        cursors = page.nextCursors,
                        hasMore = page.hasMoreEarnings || page.hasMoreReceived,
                        loadingMore = false,
                    )
                }
                .onFailure { _state.value = current.copy(loadingMore = false, error = it.message ?: "Unable to load more Finance history.") }
        }
    }

    private suspend fun fetchInitial(id: String, filter: FinanceHistoryFilter) {
        runCatching {
            val summary = repository.getWorkerFinanceSummary() ?: WorkerFinanceSummary()
            val page = repository.getWorkerFinanceHistory(id, filter, 5, FinanceHistoryCursors())
            FinanceState.Success(
                summary = summary,
                entries = mergeAndSort(page.earnings.map(FinanceListEntry::Earning) + page.received.map(FinanceListEntry::Received)),
                filter = filter,
                cursors = page.nextCursors,
                hasMore = page.hasMoreEarnings || page.hasMoreReceived,
            )
        }.onSuccess { _state.value = it }
            .onFailure { _state.value = FinanceState.Error(it.message ?: "Unable to load Finance history.") }
    }

    private fun mutate(successMessage: String, operation: suspend (String) -> Unit) {
        val current = _state.value as? FinanceState.Success ?: return
        val id = profileId ?: return
        _state.value = current.copy(saving = true, error = null, notice = null)
        viewModelScope.launch {
            runCatching { operation(id) }
                .onSuccess { fetchInitial(id, current.filter); (_state.value as? FinanceState.Success)?.let { _state.value = it.copy(notice = successMessage) } }
                .onFailure { _state.value = current.copy(saving = false, error = it.message ?: "Finance operation failed.") }
        }
    }

    private fun mergeAndSort(entries: List<FinanceListEntry>): List<FinanceListEntry> = entries
        .distinctBy { it.id }
        .sortedWith(compareByDescending<FinanceListEntry> { runCatching { Instant.parse(it.occurredAt).toEpochMilli() }.getOrDefault(0L) }.thenByDescending { it.id })
}

class FinanceViewModelFactory(private val repository: WorkHouseRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = FinanceViewModel(repository) as T
}
