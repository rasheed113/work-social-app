package com.rasheed113.worksocial.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rasheed113.worksocial.domain.account.AccountRepository

class AccountViewModelFactory(private val repository: AccountRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AccountViewModel::class.java))
        return AccountViewModel(repository) as T
    }
}
