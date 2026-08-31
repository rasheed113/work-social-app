package com.rasheed113.worksocial.platform.calls

/**
 * Boundary for native incoming-call handling.
 *
 * The website already persists call signaling in public.call_signals, but Android
 * background/locked-device wake-up still needs a real push/device contract.
 */
interface IncomingCallContract {
    suspend fun startListeningForAuthenticatedUser()
    suspend fun stopListening()
}
