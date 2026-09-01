package com.rasheed113.worksocial.platform.calls

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object PendingIncomingCallStore {
    private val _callId = MutableStateFlow<String?>(null)
    val callId: StateFlow<String?> = _callId.asStateFlow()

    fun accept(callId: String?) {
        if (!callId.isNullOrBlank()) _callId.value = callId
    }

    fun clear(callId: String?) {
        if (callId != null && _callId.value == callId) _callId.value = null
    }
}
