package com.rasheed113.worksocial.presentation.calls

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasheed113.worksocial.domain.calls.CallKind
import com.rasheed113.worksocial.domain.calls.CallState
import com.rasheed113.worksocial.platform.calls.CallViewModel
import org.webrtc.EglBase
import org.webrtc.SurfaceViewRenderer
import org.webrtc.VideoTrack

@Composable
fun CallHost(viewModel: CallViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        viewModel.onPermissionResult(result.values.all { it })
    }
    LaunchedEffect(state.permissionRequest) {
        val kind = state.permissionRequest ?: return@LaunchedEffect
        launcher.launch(
            if (kind == CallKind.VIDEO) arrayOf(android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.CAMERA)
            else arrayOf(android.Manifest.permission.RECORD_AUDIO)
        )
    }

    val session = state.session ?: return
    if (session.callerId == session.peer.id && session.calleeId != session.peer.id) {
        IncomingCallDialog(session.peer.displayName, session.kind, viewModel)
    } else {
        ActiveCallDialog(viewModel)
    }
}

@Composable
private fun IncomingCallDialog(name: String, kind: CallKind, viewModel: CallViewModel) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(if (kind == CallKind.VIDEO) "Incoming video call" else "Incoming voice call") },
        text = { Text(name) },
        confirmButton = { Button(onClick = viewModel::acceptIncoming) { Text("Accept") } },
        dismissButton = { TextButton(onClick = viewModel::rejectIncoming) { Text("Reject") } }
    )
}

@Composable
private fun ActiveCallDialog(viewModel: CallViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val session = state.session ?: return
    val context = LocalContext.current
    var elapsed by remember(session.id) { mutableStateOf(0L) }
    LaunchedEffect(session.id, state.connectedAtMs) {
        while (true) {
            elapsed = state.connectedAtMs?.let { (System.currentTimeMillis() - it).coerceAtLeast(0L) } ?: 0L
            kotlinx.coroutines.delay(1000)
        }
    }
    androidx.compose.ui.window.Dialog(onDismissRequest = {}) {
        Card(Modifier.fillMaxSize().padding(12.dp)) {
            Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(session.peer.displayName, style = MaterialTheme.typography.headlineSmall)
                Text(
                    when (session.state) {
                        CallState.RINGING -> "Calling…"
                        CallState.CONNECTING -> "Connecting…"
                        CallState.CONNECTED -> formatDuration(elapsed)
                        CallState.REJECTED -> "Call rejected"
                        CallState.MISSED -> "Missed call"
                        CallState.ENDED -> "Call ended"
                        CallState.FAILED -> state.error ?: "Call failed"
                    }
                )
                if (session.kind == CallKind.VIDEO) {
                    Box(Modifier.weight(1f).fillMaxWidth()) {
                        state.remoteVideo?.let { VideoRenderer(it, false, Modifier.fillMaxSize()) }
                        state.localVideo?.let { VideoRenderer(it, true, Modifier.size(128.dp).align(Alignment.TopEnd).padding(8.dp)) }
                    }
                } else {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Voice call") }
                }
                state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    OutlinedButton(onClick = { viewModel.setMuted(!state.muted) }) { Text(if (state.muted) "Unmute" else "Mute") }
                    OutlinedButton(onClick = { viewModel.setSpeaker(!state.speaker) }) { Text(if (state.speaker) "Earpiece" else "Speaker") }
                    if (session.kind == CallKind.VIDEO) {
                        OutlinedButton(onClick = { viewModel.setCameraEnabled(!state.cameraEnabled) }) { Text(if (state.cameraEnabled) "Camera off" else "Camera on") }
                        OutlinedButton(onClick = viewModel::switchCamera) { Text("Switch") }
                    }
                    Button(onClick = viewModel::endCall) { Text("End") }
                }
            }
        }
    }
}

@Composable
private fun VideoRenderer(track: VideoTrack, mirror: Boolean, modifier: Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            SurfaceViewRenderer(context).also { view ->
                view.init(EglBase.create().eglBaseContext, null)
                view.setEnableHardwareScaler(true)
                view.setMirror(mirror)
                track.addSink(view)
            }
        },
        update = { it.setMirror(mirror) },
        onRelease = { view ->
            track.removeSink(view)
            view.release()
        }
    )
}

private fun formatDuration(ms: Long): String {
    val totalSeconds = ms / 1000
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
