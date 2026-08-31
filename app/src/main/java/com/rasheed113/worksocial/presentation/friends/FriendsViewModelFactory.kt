package com.rasheed113.worksocial.presentation.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rasheed113.worksocial.domain.friends.FriendsRepository

class FriendsViewModelFactory(private val repository: FriendsRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(FriendsViewModel::class.java))
        return FriendsViewModel(repository) as T
    }
}
