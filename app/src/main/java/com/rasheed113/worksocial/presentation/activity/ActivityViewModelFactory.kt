package com.rasheed113.worksocial.presentation.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rasheed113.worksocial.domain.activity.ActivityRepository

class ActivityViewModelFactory(
    private val repository: ActivityRepository,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(ActivityViewModel::class.java))
        return ActivityViewModel(repository) as T
    }
}
