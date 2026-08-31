package com.rasheed113.worksocial.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.social.CreatePostResult
import com.rasheed113.worksocial.domain.social.SocialPostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CreatePostState {
    val content: String

    data class Idle(override val content: String = "") : CreatePostState
    data class Editing(override val content: String) : CreatePostState
    data class Submitting(override val content: String) : CreatePostState
    data class Success(val postId: String) : CreatePostState {
        override val content: String = ""
    }
    data class ValidationError(
        override val content: String,
        val message: String,
    ) : CreatePostState
    data class BackendError(
        override val content: String,
        val message: String,
    ) : CreatePostState
}

class CreatePostViewModel(
    private val repository: SocialPostRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<CreatePostState>(CreatePostState.Idle())
    val state: StateFlow<CreatePostState> = _state.asStateFlow()

    fun onContentChanged(content: String) {
        if (_state.value is CreatePostState.Submitting) return
        _state.value = CreatePostState.Editing(content)
    }

    fun submit() {
        val current = _state.value
        if (current is CreatePostState.Submitting) return

        val content = current.content.trim()
        if (content.isEmpty()) {
            _state.value = CreatePostState.ValidationError(
                content = current.content,
                message = "Post cannot be empty.",
            )
            return
        }

        _state.value = CreatePostState.Submitting(current.content)
        viewModelScope.launch {
            when (val result = repository.createPost(content)) {
                is CreatePostResult.Created -> _state.value = CreatePostState.Success(result.postId)
                is CreatePostResult.Failure -> _state.value = CreatePostState.BackendError(
                    content = current.content,
                    message = result.message,
                )
            }
        }
    }
}

class CreatePostViewModelFactory(
    private val repository: SocialPostRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CreatePostViewModel::class.java))
        return CreatePostViewModel(repository) as T
    }
}
