package com.rasheed113.worksocial.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.social.LikeMutationResult
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

    fun toggleLike(postId: String) {
        val current = _state.value as? SocialHomeState.Success ?: return
        if (postId in current.likingPostIds) return
        val post = current.posts.firstOrNull { it.id == postId } ?: return

        _state.value = current.copy(
            likingPostIds = current.likingPostIds + postId,
            actionError = null,
        )

        viewModelScope.launch {
            val result = if (post.isLikedByCurrentUser) {
                repository.unlikePost(postId)
            } else {
                repository.likePost(postId)
            }

            when (result) {
                is LikeMutationResult.Success -> {
                    val latest = _state.value as? SocialHomeState.Success
                    if (latest != null) {
                        _state.value = latest.copy(
                            posts = latest.posts.map { item ->
                                if (item.id == postId) {
                                    item.copy(
                                        likeCount = result.likeCount,
                                        isLikedByCurrentUser = result.isLikedByCurrentUser,
                                    )
                                } else item
                            },
                            likingPostIds = latest.likingPostIds - postId,
                            actionError = null,
                        )
                    }
                }
                is LikeMutationResult.Failure -> {
                    val latest = _state.value as? SocialHomeState.Success
                    if (latest != null) {
                        _state.value = latest.copy(
                            likingPostIds = latest.likingPostIds - postId,
                            actionError = result.message,
                        )
                    }
                }
            }
        }
    }

    fun clearActionError() {
        val current = _state.value as? SocialHomeState.Success ?: return
        if (current.actionError != null) _state.value = current.copy(actionError = null)
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
