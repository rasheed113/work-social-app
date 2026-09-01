package com.rasheed113.worksocial.platform.calls

import android.content.Context
import android.media.AudioManager
import com.rasheed113.worksocial.domain.calls.CallKind
import com.rasheed113.worksocial.domain.calls.IceCandidateData
import kotlinx.coroutines.suspendCancellableCoroutine
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraVideoCapturer
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.PeerConnection
import org.webrtc.PeerConnectionFactory
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoTrack
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class WebRtcCallEngine(private val context: Context) {
    interface Listener { fun onLocalVideo(track: VideoTrack?); fun onRemoteVideo(track: VideoTrack?); fun onIceCandidate(candidate: IceCandidateData); fun onConnecting(); fun onConnected(); fun onFailed(message: String) }
    var listener: Listener? = null
    var isReady: Boolean = false
        private set
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val eglBase = EglBase.create()
    private val factory: PeerConnectionFactory
    private var peerConnection: PeerConnection? = null
    private var audioTrack: org.webrtc.AudioTrack? = null
    private var videoTrack: VideoTrack? = null
    private var videoSource: org.webrtc.VideoSource? = null
    private var videoCapturer: VideoCapturer? = null
    private var surfaceTextureHelper: SurfaceTextureHelper? = null

    init {
        synchronized(WebRtcCallEngine::class.java) {
            if (!initialized) { PeerConnectionFactory.initialize(PeerConnectionFactory.InitializationOptions.builder(context.applicationContext).createInitializationOptions()); initialized = true }
        }
        factory = PeerConnectionFactory.builder().setVideoEncoderFactory(DefaultVideoEncoderFactory(eglBase.eglBaseContext, true, true)).setVideoDecoderFactory(DefaultVideoDecoderFactory(eglBase.eglBaseContext)).createPeerConnectionFactory()
    }

    suspend fun start(kind: CallKind): SessionDescription { createPeerConnection(kind); val pc = peerConnection ?: error("WebRTC peer connection was not created"); val offer = createOffer(pc); setLocalDescription(pc, offer); return offer }
    suspend fun accept(kind: CallKind, sdpType: String, sdp: String): SessionDescription { createPeerConnection(kind); val pc = peerConnection ?: error("WebRTC peer connection was not created"); setRemoteDescription(pc, SessionDescription(parseType(sdpType), sdp)); val answer = createAnswer(pc); setLocalDescription(pc, answer); return answer }
    suspend fun setRemoteAnswer(sdpType: String, sdp: String) { val pc = peerConnection ?: error("WebRTC peer connection is not ready"); setRemoteDescription(pc, SessionDescription(parseType(sdpType), sdp)) }
    fun addRemoteCandidate(candidate: IceCandidateData) { peerConnection?.addIceCandidate(IceCandidate(candidate.sdpMid, candidate.sdpMLineIndex, candidate.candidate)) }
    fun setMuted(value: Boolean) { audioTrack?.setEnabled(!value) }
    fun setCameraEnabled(value: Boolean) { videoTrack?.setEnabled(value) }
    fun setSpeaker(value: Boolean) { audioManager.mode = AudioManager.MODE_IN_COMMUNICATION; audioManager.isSpeakerphoneOn = value }
    fun switchCamera() { (videoCapturer as? CameraVideoCapturer)?.switchCamera(null) }

    fun close() {
        isReady = false
        runCatching { videoCapturer?.stopCapture() }
        videoCapturer?.dispose(); videoCapturer = null
        surfaceTextureHelper?.dispose(); surfaceTextureHelper = null
        videoTrack?.dispose(); videoTrack = null; videoSource?.dispose(); videoSource = null; audioTrack?.dispose(); audioTrack = null
        peerConnection?.close(); peerConnection?.dispose(); peerConnection = null
        audioManager.isSpeakerphoneOn = false; audioManager.mode = AudioManager.MODE_NORMAL
        listener?.onLocalVideo(null); listener?.onRemoteVideo(null)
    }

    private fun createPeerConnection(kind: CallKind) {
        if (peerConnection != null) return
        val iceServers = listOf(
            PeerConnection.IceServer.builder("stun:stun.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun1.l.google.com:19302").createIceServer(),
            PeerConnection.IceServer.builder("stun:stun.cloudflare.com:3478").createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:80").setUsername("openrelayproject").setPassword("openrelayproject").createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443").setUsername("openrelayproject").setPassword("openrelayproject").createIceServer(),
            PeerConnection.IceServer.builder("turn:openrelay.metered.ca:443?transport=tcp").setUsername("openrelayproject").setPassword("openrelayproject").createIceServer()
        )
        val config = PeerConnection.RTCConfiguration(iceServers).apply { sdpSemantics = PeerConnection.SdpSemantics.UNIFIED_PLAN; continualGatheringPolicy = PeerConnection.ContinualGatheringPolicy.GATHER_CONTINUALLY }
        val observer = object : PeerConnection.Observer {
            override fun onSignalingChange(newState: PeerConnection.SignalingState) = Unit
            override fun onIceConnectionChange(newState: PeerConnection.IceConnectionState) { when (newState) { PeerConnection.IceConnectionState.CONNECTED, PeerConnection.IceConnectionState.COMPLETED -> listener?.onConnected(); PeerConnection.IceConnectionState.CHECKING -> listener?.onConnecting(); PeerConnection.IceConnectionState.FAILED -> listener?.onFailed("ICE failed. The configured STUN/TURN servers could not establish a network path."); else -> Unit } }
            override fun onIceConnectionReceivingChange(receiving: Boolean) = Unit
            override fun onIceGatheringChange(newState: PeerConnection.IceGatheringState) = Unit
            override fun onIceCandidate(candidate: IceCandidate) { listener?.onIceCandidate(IceCandidateData(candidate.sdpMid, candidate.sdpMLineIndex, candidate.sdp)) }
            override fun onIceCandidatesRemoved(candidates: Array<out IceCandidate>) = Unit
            override fun onSelectedCandidatePairChanged(event: CandidatePairChangeEvent) = Unit
            override fun onAddStream(stream: org.webrtc.MediaStream) { stream.videoTracks.firstOrNull()?.let { listener?.onRemoteVideo(it) } }
            override fun onRemoveStream(stream: org.webrtc.MediaStream) = Unit
            override fun onDataChannel(dataChannel: org.webrtc.DataChannel) = Unit
            override fun onRenegotiationNeeded() = Unit
            override fun onAddTrack(receiver: org.webrtc.RtpReceiver, mediaStreams: Array<out org.webrtc.MediaStream>) { (receiver.track() as? VideoTrack)?.let { listener?.onRemoteVideo(it) } }
            override fun onRemoveTrack(receiver: org.webrtc.RtpReceiver) = Unit
            override fun onTrack(transceiver: org.webrtc.RtpTransceiver) { (transceiver.receiver.track() as? VideoTrack)?.let { listener?.onRemoteVideo(it) } }
            override fun onConnectionChange(newState: PeerConnection.PeerConnectionState) { when (newState) { PeerConnection.PeerConnectionState.CONNECTED -> listener?.onConnected(); PeerConnection.PeerConnectionState.CONNECTING -> listener?.onConnecting(); PeerConnection.PeerConnectionState.FAILED -> listener?.onFailed("WebRTC peer connection failed."); else -> Unit } }
            override fun onStandardizedIceConnectionChange(newState: PeerConnection.IceConnectionState) = Unit
        }
        peerConnection = factory.createPeerConnection(config, observer) ?: error("WebRTC peer connection creation failed")
        createLocalMedia(kind)
        isReady = true
    }

    private fun createLocalMedia(kind: CallKind) {
        val audioSource = factory.createAudioSource(MediaConstraints())
        audioTrack = factory.createAudioTrack("worksocial-audio", audioSource).also { it.setEnabled(true) }
        peerConnection?.addTrack(audioTrack, listOf("worksocial-stream"))
        audioManager.mode = AudioManager.MODE_IN_COMMUNICATION
        if (kind == CallKind.VIDEO) {
            val enumerator = Camera2Enumerator(context)
            val cameraName = enumerator.deviceNames.firstOrNull { enumerator.isFrontFacing(it) } ?: enumerator.deviceNames.firstOrNull() ?: error("No camera is available")
            val capturer = enumerator.createCapturer(cameraName, null) ?: error("Camera capture could not be created")
            videoCapturer = capturer
            surfaceTextureHelper = SurfaceTextureHelper.create("WorkSocialCamera", eglBase.eglBaseContext)
            videoSource = factory.createVideoSource(false)
            capturer.initialize(surfaceTextureHelper, context.applicationContext, videoSource?.capturerObserver)
            capturer.startCapture(1280, 720, 30)
            videoTrack = factory.createVideoTrack("worksocial-video", videoSource!!).also { it.setEnabled(true) }
            peerConnection?.addTrack(videoTrack, listOf("worksocial-stream"))
            listener?.onLocalVideo(videoTrack)
        }
    }

    private suspend fun createOffer(pc: PeerConnection): SessionDescription = suspendCancellableCoroutine { continuation -> pc.createOffer(object : SdpObserver { override fun onCreateSuccess(description: SessionDescription) { continuation.resume(description) }; override fun onSetSuccess() = Unit; override fun onCreateFailure(error: String) { continuation.resumeWithException(IllegalStateException(error)) }; override fun onSetFailure(error: String) = Unit }, MediaConstraints()) }
    private suspend fun createAnswer(pc: PeerConnection): SessionDescription = suspendCancellableCoroutine { continuation -> pc.createAnswer(object : SdpObserver { override fun onCreateSuccess(description: SessionDescription) { continuation.resume(description) }; override fun onSetSuccess() = Unit; override fun onCreateFailure(error: String) { continuation.resumeWithException(IllegalStateException(error)) }; override fun onSetFailure(error: String) = Unit }, MediaConstraints()) }
    private suspend fun setLocalDescription(pc: PeerConnection, description: SessionDescription) = suspendCancellableCoroutine<Unit> { continuation -> pc.setLocalDescription(object : SdpObserver { override fun onCreateSuccess(description: SessionDescription) = Unit; override fun onSetSuccess() { continuation.resume(Unit) }; override fun onCreateFailure(error: String) { continuation.resumeWithException(IllegalStateException(error)) }; override fun onSetFailure(error: String) { continuation.resumeWithException(IllegalStateException(error)) } }, description) }
    private suspend fun setRemoteDescription(pc: PeerConnection, description: SessionDescription) = suspendCancellableCoroutine<Unit> { continuation -> pc.setRemoteDescription(object : SdpObserver { override fun onCreateSuccess(description: SessionDescription) = Unit; override fun onSetSuccess() { continuation.resume(Unit) }; override fun onCreateFailure(error: String) { continuation.resumeWithException(IllegalStateException(error)) }; override fun onSetFailure(error: String) { continuation.resumeWithException(IllegalStateException(error)) } }, description) }
    private fun parseType(value: String): SessionDescription.Type = when (value.lowercase()) { "offer" -> SessionDescription.Type.OFFER; "answer" -> SessionDescription.Type.ANSWER; "pranswer" -> SessionDescription.Type.PRANSWER; else -> error("Unsupported SDP type: $value") }
    companion object { @Volatile private var initialized = false }
}
