package com.rasheed113.worksocial.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.social.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SocialHomeViewModel(private val repository: SocialPostRepository) : ViewModel() {
    private val _state = MutableStateFlow<SocialHomeState>(SocialHomeState.Loading)
    val state: StateFlow<SocialHomeState> = _state.asStateFlow()
    private var loaded = false
    private val loadingCommentPosts = mutableSetOf<String>()
    private val creatingCommentPosts = mutableSetOf<String>()
    private val deletingComments = mutableSetOf<String>()

    fun load() { if (!loaded) refreshInternal() }
    fun refresh() { refreshInternal() }

    fun toggleLike(postId: String) {
        val current = _state.value as? SocialHomeState.Success ?: return
        if (postId in current.likingPostIds) return
        val post = current.posts.firstOrNull { it.id == postId } ?: return
        val nextLiked = !post.isLikedByCurrentUser
        val optimisticPost = post.copy(
            likeCount = (post.likeCount + if (nextLiked) 1 else -1).coerceAtLeast(0),
            isLikedByCurrentUser = nextLiked,
        )
        _state.value = current.copy(
            posts = current.posts.map { if (it.id == postId) optimisticPost else it },
            likingPostIds = current.likingPostIds + postId,
            actionError = null,
        )
        viewModelScope.launch {
            val result = if (nextLiked) repository.likePost(postId) else repository.unlikePost(postId)
            val latest = _state.value as? SocialHomeState.Success ?: return@launch
            _state.value = when (result) {
                is LikeMutationResult.Success -> latest.copy(
                    posts = latest.posts.map { if (it.id == postId) it.copy(likeCount = result.likeCount, isLikedByCurrentUser = result.isLikedByCurrentUser) else it },
                    likingPostIds = latest.likingPostIds - postId,
                )
                is LikeMutationResult.Failure -> latest.copy(
                    posts = latest.posts.map { if (it.id == postId) post else it },
                    likingPostIds = latest.likingPostIds - postId,
                    actionError = result.message,
                )
            }
        }
    }

    fun openComments(postId: String) {
        val current = _state.value as? SocialHomeState.Success ?: return
        if (postId in loadingCommentPosts) return
        loadingCommentPosts += postId
        _state.value = current.copy(comments = current.comments + (postId to CommentsState.Loading))
        viewModelScope.launch {
            val result = repository.getComments(postId)
            loadingCommentPosts -= postId
            val latest = _state.value as? SocialHomeState.Success ?: return@launch
            val commentState = when (result) {
                is CommentsResult.Success -> CommentsState.Success(result.comments)
                is CommentsResult.Failure -> CommentsState.Error(result.message)
            }
            _state.value = latest.copy(comments = latest.comments + (postId to commentState))
        }
    }

    fun createComment(postId: String, content: String) {
        if (postId in creatingCommentPosts) return
        if (content.trim().isEmpty()) { setActionError("Comment cannot be empty."); return }
        creatingCommentPosts += postId
        updateCommentMutations { it + postId }
        viewModelScope.launch {
            val result = repository.createComment(postId, content)
            creatingCommentPosts -= postId
            updateCommentMutations { it - postId }
            when (result) {
                is CreateCommentResult.Created -> openComments(postId)
                is CreateCommentResult.Failure -> setActionError(result.message)
            }
        }
    }

    fun deleteComment(postId: String, commentId: String) {
        if (commentId in deletingComments) return
        deletingComments += commentId
        updateCommentMutations { it + commentId }
        viewModelScope.launch {
            val result = repository.deleteComment(commentId)
            deletingComments -= commentId
            updateCommentMutations { it - commentId }
            when (result) {
                is DeleteCommentResult.Deleted -> openComments(postId)
                is DeleteCommentResult.Failure -> setActionError(result.message)
            }
        }
    }

    fun clearActionError() {
        val current = _state.value as? SocialHomeState.Success ?: return
        _state.value = current.copy(actionError = null)
    }

    private fun setActionError(message: String) {
        val current = _state.value as? SocialHomeState.Success ?: return
        _state.value = current.copy(actionError = message)
    }

    private fun updateCommentMutations(transform: (Set<String>) -> Set<String>) {
        val current = _state.value as? SocialHomeState.Success ?: return
        _state.value = current.copy(commentMutations = transform(current.commentMutations))
    }

    private fun refreshInternal() {
        viewModelScope.launch {
            _state.value = SocialHomeState.Loading
            runCatching { repository.getHomePosts() }
                .onSuccess { posts ->
                    loaded = true
                    _state.value = if (posts.isEmpty()) SocialHomeState.Empty else SocialHomeState.Success(posts)
                }
                .onFailure {
                    loaded = false
                    _state.value = SocialHomeState.Error(it.message?.takeIf(String::isNotBlank) ?: "Unable to load Social Home right now.")
                }
        }
    }
}
