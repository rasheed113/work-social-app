package com.rasheed113.worksocial.platform.calls

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.rasheed113.worksocial.domain.calls.CallRepository

class CallViewModelFactory(
    private val userId: String,
    private val repository: CallRepository,
    private val engine: WebRtcCallEngine,
    private val context: Context
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        CallViewModel(userId, repository, engine, context.applicationContext) as T
}
