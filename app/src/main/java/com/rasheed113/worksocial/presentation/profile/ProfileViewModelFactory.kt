package com.rasheed113.worksocial.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rasheed113.worksocial.domain.account.AccountRepository
import com.rasheed113.worksocial.domain.friends.FriendsRepository
import com.rasheed113.worksocial.domain.social.SocialPostRepository

class ProfileViewModelFactory(
    private val accountRepository: AccountRepository,
    private val friendsRepository: FriendsRepository,
    private val socialPostRepository: SocialPostRepository,
    private val currentUserId: String,
    private val targetProfileId: String?,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ProfileViewModel::class.java))
        return ProfileViewModel(accountRepository, friendsRepository, socialPostRepository, currentUserId, targetProfileId) as T
    }
}
