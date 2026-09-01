package com.rasheed113.worksocial.platform.calls

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rasheed113.worksocial.domain.calls.CallKind
import com.rasheed113.worksocial.domain.calls.CallPeer
import com.rasheed113.worksocial.domain.calls.CallRepository
import com.rasheed113.worksocial.domain.calls.CallSession
import com.rasheed113.worksocial.domain.calls.CallSignal
import com.rasheed113.worksocial.domain.calls.CallState
import com.rasheed113.worksocial.domain.calls.IceCandidateData
import com.rasheed113.worksocial.domain.calls.OutgoingCallSignal
import com.rasheed113.worksocial.domain.calls.SignalType
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.webrtc.VideoTrack
import java.util.UUID

data class CallUiState(
    val session: CallSession? = null,
    val permissionRequest: CallKind? = null,
    val connectedAtMs: Long? = null,
    val muted: Boolean = false,
    val speaker: Boolean = false,
    val cameraEnabled: Boolean = true,
    val localVideo: VideoTrack? = null,
    val remoteVideo: VideoTrack? = null,
    val error: String? = null
)

class CallViewModel(private val userId: String, private val repository: CallRepository, private val engine: WebRtcCallEngine, private val appContext: Context) : ViewModel() {
    private val _state = MutableStateFlow(CallUiState())
    val state: StateFlow<CallUiState> = _state.asStateFlow()
    private var incomingJob: Job? = null
    private var callSignalsJob: Job? = null
    private var timeoutJob: Job? = null
    private var pendingAction: (suspend () -> Unit)? = null
    private var pendingCandidates = mutableListOf<IceCandidateData>()
    private val seenSignals = mutableSetOf<String>()
    private var pendingOffer: CallSignal? = null

    init {
        engine.listener = object : WebRtcCallEngine.Listener {
            override fun onLocalVideo(track: VideoTrack?) { _state.value = _state.value.copy(localVideo = track) }
            override fun onRemoteVideo(track: VideoTrack?) { _state.value = _state.value.copy(remoteVideo = track) }
            override fun onIceCandidate(candidate: IceCandidateData) {
                val session = _state.value.session ?: return
                viewModelScope.launch { repository.sendSignal(userId, OutgoingCallSignal(callId = session.id, conversationId = session.conversationId, recipientId = peerId(session), kind = session.kind, type = SignalType.ICE, candidate = candidate)).onFailure { _state.value = _state.value.copy(error = it.message ?: "ICE signaling failed") } }
            }
            override fun onConnected() { _state.value = _state.value.copy(session = _state.value.session?.copy(state = CallState.CONNECTED), connectedAtMs = System.currentTimeMillis(), error = null); timeoutJob?.cancel() }
            override fun onConnecting() { _state.value = _state.value.copy(session = _state.value.session?.copy(state = CallState.CONNECTING)) }
            override fun onFailed(message: String) { _state.value = _state.value.copy(session = _state.value.session?.copy(state = CallState.FAILED), error = message) }
        }
        startIncomingObservation()
    }

    private fun startIncomingObservation() {
        incomingJob?.cancel()
        incomingJob = viewModelScope.launch { repository.observeIncomingSignals(userId).collect { signal -> if (seenSignals.add(signal.id)) handleSignal(signal) } }
    }

    fun resumeIncomingCall(callId: String) {
        if (_state.value.session != null || callId.isBlank()) return
        viewModelScope.launch {
            repository.getIncomingOffer(userId, callId).getOrNull()?.let { signal ->
                if (seenSignals.add(signal.id)) handleSignal(signal)
            }
        }
    }

    fun startOutgoing(conversationId: String, peer: CallPeer, kind: CallKind) {
        if (_state.value.session != null) return
        val action: suspend () -> Unit = {
            val callId = UUID.randomUUID().toString()
            _state.value = CallUiState(session = CallSession(callId, conversationId, userId, peer.id, kind, CallState.RINGING, peer))
            observeCall(callId)
            runCatching { engine.start(kind) }.onSuccess { offer ->
                repository.sendSignal(userId, OutgoingCallSignal(callId = callId, conversationId = conversationId, recipientId = peer.id, kind = kind, type = SignalType.OFFER, sdpType = offer.type.canonicalForm(), sdp = offer.description)).onFailure { failCall(it.message ?: "The call could not be sent") }
            }.onFailure { failCall(it.message ?: "The call could not be started") }
            startTimeout(callId, 45_000L, CallState.ENDED)
        }
        requestPermissionsOrRun(kind, action)
    }

    fun acceptIncoming() {
        val session = _state.value.session ?: return
        if (session.state != CallState.RINGING || session.callerId == userId) return
        val action: suspend () -> Unit = {
            val offer = pendingOffer
            if (offer == null || offer.sdp.isNullOrBlank() || offer.sdpType.isNullOrBlank()) {
                failCall("Incoming call offer is missing")
            } else {
                _state.value = _state.value.copy(session = session.copy(state = CallState.CONNECTING), error = null)
                runCatching { engine.accept(session.kind, offer.sdpType, offer.sdp) }.onSuccess { answer ->
                    repository.sendSignal(userId, OutgoingCallSignal(callId = session.id, conversationId = session.conversationId, recipientId = session.callerId, kind = session.kind, type = SignalType.ANSWER, sdpType = answer.type.canonicalForm(), sdp = answer.description)).onFailure { failCall(it.message ?: "The call answer could not be sent") }
                    pendingCandidates.forEach { engine.addRemoteCandidate(it) }
                    pendingCandidates.clear()
                }.onFailure { failCall(it.message ?: "The incoming call could not be accepted") }
            }
        }
        requestPermissionsOrRun(session.kind, action)
    }

    fun rejectIncoming() {
        val session = _state.value.session ?: return
        if (session.callerId == userId) return
        viewModelScope.launch { repository.sendSignal(userId, OutgoingCallSignal(callId = session.id, conversationId = session.conversationId, recipientId = session.callerId, kind = session.kind, type = SignalType.REJECT)); clear() }
    }

    fun endCall() {
        val session = _state.value.session ?: return
        viewModelScope.launch { repository.sendSignal(userId, OutgoingCallSignal(callId = session.id, conversationId = session.conversationId, recipientId = peerId(session), kind = session.kind, type = SignalType.HANGUP)); clear() }
    }

    fun onPermissionResult(granted: Boolean) {
        val action = pendingAction
        pendingAction = null
        _state.value = _state.value.copy(permissionRequest = null)
        if (granted && action != null) viewModelScope.launch { action() }
        else if (!granted) _state.value = _state.value.copy(error = "Required calling permission was denied.")
    }

    fun setMuted(value: Boolean) { engine.setMuted(value); _state.value = _state.value.copy(muted = value) }
    fun setSpeaker(value: Boolean) { engine.setSpeaker(value); _state.value = _state.value.copy(speaker = value) }
    fun setCameraEnabled(value: Boolean) { engine.setCameraEnabled(value); _state.value = _state.value.copy(cameraEnabled = value) }
    fun switchCamera() { engine.switchCamera() }

    private fun handleSignal(signal: CallSignal) {
        val current = _state.value.session
        if (signal.type == SignalType.OFFER) {
            if (current != null) return
            viewModelScope.launch {
                val peer = repository.resolvePeer(userId, signal.senderId).getOrElse { _state.value = _state.value.copy(error = it.message ?: "Caller profile could not be loaded"); return@launch }
                pendingOffer = signal
                _state.value = CallUiState(session = CallSession(signal.callId, signal.conversationId, signal.senderId, userId, signal.kind, CallState.RINGING, peer))
                observeCall(signal.callId)
                startTimeout(signal.callId, 45_000L, CallState.MISSED)
            }
            return
        }
        if (current?.id != signal.callId) return
        when (signal.type) {
            SignalType.ANSWER -> viewModelScope.launch { _state.value = _state.value.copy(session = current.copy(state = CallState.CONNECTING)); runCatching { engine.setRemoteAnswer(signal.sdpType ?: "answer", signal.sdp.orEmpty()) }.onFailure { failCall(it.message ?: "The call answer could not be applied") }; pendingCandidates.forEach { engine.addRemoteCandidate(it) }; pendingCandidates.clear() }
            SignalType.ICE -> signal.candidate?.let { if (engine.isReady) engine.addRemoteCandidate(it) else pendingCandidates += it }
            SignalType.REJECT, SignalType.HANGUP -> clear()
            SignalType.OFFER -> Unit
        }
    }

    private fun observeCall(callId: String) {
        callSignalsJob?.cancel()
        callSignalsJob = viewModelScope.launch { repository.observeCallSignals(userId, callId).collect { signal -> if (seenSignals.add(signal.id)) handleSignal(signal) } }
    }

    private fun requestPermissionsOrRun(kind: CallKind, action: suspend () -> Unit) {
        val microphone = ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        val camera = kind != CallKind.VIDEO || ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        if (microphone && camera) viewModelScope.launch { action() } else { pendingAction = action; _state.value = _state.value.copy(permissionRequest = kind) }
    }

    private fun startTimeout(callId: String, timeoutMs: Long, terminal: CallState) {
        timeoutJob?.cancel()
        timeoutJob = viewModelScope.launch { delay(timeoutMs); if (_state.value.session?.id == callId && _state.value.session?.state != CallState.CONNECTED) clear() }
    }

    private fun failCall(message: String) { _state.value = _state.value.copy(session = _state.value.session?.copy(state = CallState.FAILED), error = message); engine.close() }
    private fun peerId(session: CallSession): String = if (session.callerId == userId) session.calleeId else session.callerId
    private fun clear() { timeoutJob?.cancel(); callSignalsJob?.cancel(); pendingOffer = null; pendingCandidates.clear(); engine.close(); _state.value = CallUiState() }

    override fun onCleared() { incomingJob?.cancel(); callSignalsJob?.cancel(); timeoutJob?.cancel(); engine.close(); super.onCleared() }
}
