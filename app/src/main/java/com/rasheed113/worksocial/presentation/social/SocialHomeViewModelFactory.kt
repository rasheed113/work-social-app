package com.rasheed113.worksocial.presentation.social

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rasheed113.worksocial.domain.social.SocialPostRepository

class SocialHomeViewModelFactory(
    private val repository: SocialPostRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(SocialHomeViewModel::class.java))
        return SocialHomeViewModel(repository) as T
    }
}
