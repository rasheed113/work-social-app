package com.rasheed113.worksocial.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.social.SocialHomeState
import com.rasheed113.worksocial.domain.social.SocialPostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SocialHomeViewModel(
    private val repository: SocialPostRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<SocialHomeState>(SocialHomeState.Loading)
    val state: StateFlow<SocialHomeState> = _state.asStateFlow()

    private var loaded = false

    fun load() {
        if (loaded) return
        refreshInternal()
    }

    fun refresh() {
        refreshInternal()
    }

    private fun refreshInternal() {
        viewModelScope.launch {
            _state.value = SocialHomeState.Loading
            runCatching { repository.getHomePosts() }
                .onSuccess { posts ->
                    loaded = true
                    _state.value = if (posts.isEmpty()) {
                        SocialHomeState.Empty
                    } else {
                        SocialHomeState.Success(posts)
                    }
                }
                .onFailure { error ->
                    loaded = false
                    _state.value = SocialHomeState.Error(
                        error.message?.takeIf(String::isNotBlank)
                            ?: "Unable to load Social Home right now.",
                    )
                }
        }
    }
}
