package com.rasheed113.worksocial.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.account.AccountRepository
import com.rasheed113.worksocial.domain.account.AccountState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AccountViewModel(private val repository: AccountRepository) : ViewModel() {
    private val _state = MutableStateFlow<AccountState>(AccountState.Loading)
    val state: StateFlow<AccountState> = _state.asStateFlow()

    fun load(profileId: String) {
        if (_state.value is AccountState.Success) return
        viewModelScope.launch {
            _state.value = AccountState.Loading
            runCatching { repository.getProfile(profileId) }
                .onSuccess { profile ->
                    _state.value = profile?.let(AccountState::Success) ?: AccountState.Empty
                }
                .onFailure { error ->
                    _state.value = AccountState.Error(
                        error.message?.takeIf { it.isNotBlank() } ?: "Unable to load your Work Social account."
                    )
                }
        }
    }

    fun retry(profileId: String) {
        _state.value = AccountState.Loading
        load(profileId)
    }
}
