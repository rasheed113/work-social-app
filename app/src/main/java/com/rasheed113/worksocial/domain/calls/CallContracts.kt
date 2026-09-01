package com.rasheed113.worksocial.domain.calls

import kotlinx.coroutines.flow.Flow

enum class CallKind { AUDIO, VIDEO }

enum class CallState { RINGING, CONNECTING, CONNECTED, REJECTED, MISSED, ENDED, FAILED }

data class CallPeer(
    val id: String,
    val displayName: String,
    val username: String?,
    val avatarUrl: String?
)

data class CallSession(
    val id: String,
    val conversationId: String,
    val callerId: String,
    val calleeId: String,
    val kind: CallKind,
    val state: CallState,
    val peer: CallPeer
)

data class CallSignal(
    val id: String,
    val callId: String,
    val conversationId: String,
    val senderId: String,
    val recipientId: String,
    val kind: CallKind,
    val type: SignalType,
    val sdpType: String? = null,
    val sdp: String? = null,
    val candidate: IceCandidateData? = null,
    val createdAt: String
)

enum class SignalType { OFFER, ANSWER, ICE, HANGUP, REJECT }

data class IceCandidateData(
    val sdpMid: String?,
    val sdpMLineIndex: Int,
    val candidate: String
)

interface CallRepository {
    suspend fun sendSignal(userId: String, signal: OutgoingCallSignal): Result<Unit>
    fun observeIncomingSignals(userId: String): Flow<CallSignal>
    fun observeCallSignals(userId: String, callId: String): Flow<CallSignal>
    suspend fun getIncomingOffer(userId: String, callId: String): Result<CallSignal?>
    suspend fun resolvePeer(userId: String, peerId: String): Result<CallPeer>
}

data class OutgoingCallSignal(
    val callId: String,
    val conversationId: String,
    val recipientId: String,
    val kind: CallKind,
    val type: SignalType,
    val sdpType: String? = null,
    val sdp: String? = null,
    val candidate: IceCandidateData? = null
)
