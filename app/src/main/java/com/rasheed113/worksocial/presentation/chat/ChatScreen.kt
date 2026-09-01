package com.rasheed113.worksocial.presentation.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasheed113.worksocial.domain.chat.ChatRepository
import com.rasheed113.worksocial.domain.chat.Conversation

@Composable
fun ChatScreen(userId: String, viewModel: ChatViewModel) {
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
            ChatConversation(userId, selected, state, viewModel, draft, { draft = it }) { viewModel.select(userId, selected.id) }
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
private fun ChatConversation(userId: String, conversation: Conversation, state: com.rasheed113.worksocial.domain.chat.ChatState, viewModel: ChatViewModel, draft: String, setDraft: (String) -> Unit, refresh: () -> Unit) {
    val listState = rememberLazyListState()
    LaunchedEffect(state.messages.size) { if (state.messages.isNotEmpty()) listState.animateScrollToItem(state.messages.lastIndex) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Column { Text(conversation.peer?.displayName ?: conversation.title ?: "Conversation", style = MaterialTheme.typography.titleMedium); conversation.peer?.username?.let { Text("@$it", style = MaterialTheme.typography.bodySmall) } }
            TextButton(onClick = refresh) { Text("Refresh") }
        }
        HorizontalDivider()
        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(12.dp)) }
        LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.messages, key = { it.id }) { message ->
                val mine = message.senderId == userId
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start) {
                    Card { Text(if (message.deletedAt != null) "Message deleted" else message.content, modifier = Modifier.padding(12.dp)) }
                }
            }
        }
        Row(Modifier.fillMaxWidth().imePadding().padding(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = draft, onValueChange = setDraft, modifier = Modifier.weight(1f), maxLines = 4, placeholder = { Text("Message") }, enabled = !state.sending)
            Button(onClick = { viewModel.send(userId, conversation.id, draft); setDraft("") }, enabled = draft.trim().isNotEmpty() && !state.sending) { Text("Send") }
        }
    }
}
