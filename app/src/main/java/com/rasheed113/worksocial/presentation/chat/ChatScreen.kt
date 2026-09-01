package com.rasheed113.worksocial.presentation.chat

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.rasheed113.worksocial.domain.calls.CallKind
import com.rasheed113.worksocial.domain.chat.ChatContent
import com.rasheed113.worksocial.domain.chat.ChatRepository
import com.rasheed113.worksocial.domain.chat.Conversation
import com.rasheed113.worksocial.domain.chat.MediaDescriptor
import com.rasheed113.worksocial.infrastructure.chat.ChatMessageMediaParser
import com.rasheed113.worksocial.platform.calls.CallViewModel

@Composable
fun ChatScreen(userId: String, viewModel: ChatViewModel, callViewModel: CallViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var draft by remember { mutableStateOf("") }
    val selected = state.conversations.firstOrNull { it.id == state.selectedConversationId }
    LaunchedEffect(userId) { viewModel.load(userId) }
    Column(Modifier.fillMaxSize().navigationBarsPadding()) {
        if (selected == null) {
            Text("Inbox", style = MaterialTheme.typography.headlineSmall, modifier = Modifier.padding(16.dp))
            state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 16.dp)) }
            if (state.loading) CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            if (!state.loading && state.conversations.isEmpty()) Text("No conversations yet.", modifier = Modifier.padding(16.dp))
            LazyColumn(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                items(state.conversations, key = { it.id }) { conversation -> ConversationRow(conversation) { viewModel.select(userId, conversation.id) } }
            }
        } else {
            ChatConversation(userId, selected, state, viewModel, callViewModel, draft, { draft = it }) { viewModel.select(userId, selected.id) }
        }
    }
}

@Composable
private fun ConversationRow(conversation: Conversation, onClick: () -> Unit) {
    TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column(Modifier.fillMaxWidth()) {
            Text(conversation.peer?.displayName ?: conversation.title ?: "Conversation", style = MaterialTheme.typography.titleMedium)
            conversation.peer?.username?.let { Text("@$it", style = MaterialTheme.typography.bodySmall) }
        }
    }
    HorizontalDivider()
}

@Composable
private fun ChatConversation(userId: String, conversation: Conversation, state: com.rasheed113.worksocial.domain.chat.ChatState, viewModel: ChatViewModel, callViewModel: CallViewModel, draft: String, setDraft: (String) -> Unit, refresh: () -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) { if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(conversation.peer?.displayName ?: conversation.title ?: "Conversation", style = MaterialTheme.typography.titleMedium); conversation.peer?.username?.let { Text("@$it", style = MaterialTheme.typography.bodySmall) } }
            Row {
                conversation.peer?.let { peer ->
                    TextButton(onClick = { callViewModel.startOutgoing(conversation.id, com.rasheed113.worksocial.domain.calls.CallPeer(peer.id, peer.displayName, peer.username, peer.avatarUrl), CallKind.AUDIO) }) { Text("Call") }
                    TextButton(onClick = { callViewModel.startOutgoing(conversation.id, com.rasheed113.worksocial.domain.calls.CallPeer(peer.id, peer.displayName, peer.username, peer.avatarUrl), CallKind.VIDEO) }) { Text("Video") }
                }
                TextButton(onClick = refresh) { Text("Refresh") }
            }
        }
        HorizontalDivider()
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.messages, key = { it.id }) { message ->
                val mine = message.senderId == userId
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Card { ChatMessageContent(message.content) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().imePadding().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = draft, onValueChange = setDraft, modifier = Modifier.weight(1f), maxLines = 4, placeholder = { Text("Message") }, enabled = !state.sending)
            Button(onClick = { viewModel.send(userId, conversation.id, draft); setDraft("") }, enabled = draft.trim().isNotEmpty() && !state.sending) { Text("Send") }
        }
    }
}

@Composable
private fun ChatMessageContent(rawContent: String) {
    when (val content = ChatMessageMediaParser.parse(rawContent)) {
        is ChatContent.Text -> Text(content.value, modifier = Modifier.padding(12.dp))
        is ChatContent.Media -> MediaContent(content.descriptor)
    }
}

@Composable
private fun MediaContent(media: MediaDescriptor) {
    val context = LocalContext.current
    when (media) {
        is MediaDescriptor.Image -> AsyncImage(model = media.payload, contentDescription = "Chat image", modifier = Modifier.widthIn(max = 280.dp).heightIn(max = 280.dp).padding(6.dp), contentScale = ContentScale.Fit)
        is MediaDescriptor.Video -> ChatVideo(media.payload)
        is MediaDescriptor.File -> TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(media.payload))) }, modifier = Modifier.padding(6.dp)) { Text("Open file") }
    }
}

@Composable
private fun ChatVideo(url: String) {
    val context = LocalContext.current
    val controller = remember { MediaController(context) }
    AndroidViewVideo(url, controller)
}

@Composable
private fun AndroidViewVideo(url: String, controller: MediaController) {
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { context -> VideoView(context).apply { setMediaController(controller); controller.setAnchorView(this); setVideoPath(url) } },
        update = { view -> if (view.tag != url) { view.tag = url; view.setVideoPath(url) } },
        modifier = Modifier.widthIn(max = 300.dp).height(220.dp).padding(6.dp),
    )
}
