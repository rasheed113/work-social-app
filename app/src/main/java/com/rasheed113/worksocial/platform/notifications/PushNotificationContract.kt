package com.rasheed113.worksocial.platform.notifications

/** Native push boundary. No fake delivery is implemented in the foundation phase. */
interface PushNotificationContract {
    suspend fun registerAuthenticatedDevice(): Result<Unit>
    suspend fun unregisterAuthenticatedDevice(): Result<Unit>
}
