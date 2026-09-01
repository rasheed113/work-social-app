package com.rasheed113.worksocial.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.social.CreatePostAttachment
import com.rasheed113.worksocial.domain.social.CreatePostLocation
import com.rasheed113.worksocial.domain.social.CreatePostResult
import com.rasheed113.worksocial.domain.social.SocialPostRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface CreatePostState {
    val content: String
    val attachments: List<CreatePostAttachment>
    val location: CreatePostLocation?
    data class Idle(override val content: String = "", override val attachments: List<CreatePostAttachment> = emptyList(), override val location: CreatePostLocation? = null) : CreatePostState
    data class Editing(override val content: String, override val attachments: List<CreatePostAttachment> = emptyList(), override val location: CreatePostLocation? = null) : CreatePostState
    data class Submitting(override val content: String, override val attachments: List<CreatePostAttachment>, override val location: CreatePostLocation?) : CreatePostState
    data class Success(val postId: String) : CreatePostState { override val content = ""; override val attachments = emptyList<CreatePostAttachment>(); override val location = null }
    data class ValidationError(override val content: String, val message: String, override val attachments: List<CreatePostAttachment> = emptyList(), override val location: CreatePostLocation? = null) : CreatePostState
    data class BackendError(override val content: String, val message: String, override val attachments: List<CreatePostAttachment> = emptyList(), override val location: CreatePostLocation? = null) : CreatePostState
}

class CreatePostViewModel(private val repository: SocialPostRepository) : ViewModel() {
    private val _state = MutableStateFlow<CreatePostState>(CreatePostState.Idle())
    val state: StateFlow<CreatePostState> = _state.asStateFlow()

    fun onContentChanged(content: String) = update { copyEditing(content = content) }
    fun setAttachments(attachments: List<CreatePostAttachment>) = update { copyEditing(attachments = attachments) }
    fun setLocation(location: CreatePostLocation?) = update { copyEditing(location = location) }

    private fun update(block: CreatePostState.Editing.() -> CreatePostState.Editing) {
        val current = _state.value
        if (current is CreatePostState.Submitting) return
        val editing = when (current) {
            is CreatePostState.Editing -> current
            is CreatePostState.ValidationError -> CreatePostState.Editing(current.content, current.attachments, current.location)
            is CreatePostState.BackendError -> CreatePostState.Editing(current.content, current.attachments, current.location)
            else -> CreatePostState.Editing(current.content, current.attachments, current.location)
        }
        _state.value = block(editing)
    }

    private fun CreatePostState.Editing.copyEditing(
        content: String = this.content,
        attachments: List<CreatePostAttachment> = this.attachments,
        location: CreatePostLocation? = this.location,
    ) = CreatePostState.Editing(content, attachments, location)

    fun submit() {
        val current = _state.value
        if (current is CreatePostState.Submitting) return
        val content = current.content.trim()
        val attachments = current.attachments
        val location = current.location
        if (content.isEmpty() && attachments.isEmpty() && location == null) {
            _state.value = CreatePostState.ValidationError(current.content, "Post cannot be empty.", attachments, location)
            return
        }
        _state.value = CreatePostState.Submitting(current.content, attachments, location)
        viewModelScope.launch {
            val result = if (attachments.isEmpty() && location == null) {
                // Preserve the established text-only repository contract for existing
                // implementations and test fakes. The interface delegates this entry
                // point to the full contract for repositories that only implement that path.
                repository.createPost(content)
            } else {
                repository.createPost(content, attachments, location)
            }
            when (result) {
                is CreatePostResult.Created -> _state.value = CreatePostState.Success(result.postId)
                is CreatePostResult.Failure -> _state.value = CreatePostState.BackendError(current.content, result.message, attachments, location)
            }
        }
    }
}

class CreatePostViewModelFactory(private val repository: SocialPostRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(CreatePostViewModel::class.java))
        return CreatePostViewModel(repository) as T
    }
}
