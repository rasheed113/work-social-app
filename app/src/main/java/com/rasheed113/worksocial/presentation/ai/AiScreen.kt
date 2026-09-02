package com.rasheed113.worksocial.presentation.ai

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.rasheed113.worksocial.domain.ai.AiMessage
import com.rasheed113.worksocial.domain.ai.AiPendingAction

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiScreen(viewModel: AiViewModel, onBack: () -> Unit) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()
    var draft by remember { mutableStateOf("") }
    val messages = (state as? AiUiState.Ready)?.messages ?: emptyList()

    LaunchedEffect(messages.size) { if (messages.isNotEmpty()) listState.animateScrollToItem(messages.lastIndex) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Work Social AI", fontWeight = FontWeight.SemiBold) },
                navigationIcon = { IconButton(onClick = onBack) { Text("‹", style = MaterialTheme.typography.headlineMedium) } },
                actions = { Text("AI", modifier = Modifier.padding(end = 16.dp), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
            )
        },
        bottomBar = {
            val ready = state as? AiUiState.Ready
            Column(Modifier.fillMaxWidth().imePadding().navigationBarsPadding().padding(12.dp)) {
                ready?.error?.let { Text(it, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.Bottom) {
                    OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.weight(1f), placeholder = { Text("Ask anything…") }, maxLines = 4, enabled = ready?.sending != true)
                    Button(onClick = { viewModel.send(draft); draft = "" }, enabled = ready?.sending != true && draft.isNotBlank(), modifier = Modifier.size(56.dp)) { Text("↑") }
                }
            }
        },
    ) { padding ->
        when (val current = state) {
            AiUiState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is AiUiState.Ready -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (current.messages.isEmpty()) item {
                    Card(Modifier.fillMaxWidth().padding(top = 20.dp)) {
                        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("✨ Ask Work Social AI", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text("Gap-shap bhi chalegi 😄 — ya apni real Work Social activity, posts, notifications aur work entries ke baare mein poochho.")
                            Text("Try: \"Yaar kal presentation hai 😂\"", color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
                items(current.messages, key = { it.id }) { AiBubble(it) }
                current.pendingAction?.let { action -> item(key = "pending-${action.id}") { ConfirmationCard(action, current.confirming, viewModel::confirm, viewModel::cancel) } }
                if (current.sending) item { TypingIndicator() }
            }
        }
    }
}

@Composable
private fun AiBubble(message: AiMessage) {
    val user = message.role == "user"
    Row(Modifier.fillMaxWidth(), horizontalArrangement = if (user) Arrangement.End else Arrangement.Start) {
        Box(Modifier.widthIn(max = 320.dp).clip(RoundedCornerShape(18.dp)).background(if (user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant).padding(horizontal = 14.dp, vertical = 10.dp)) {
            Text(message.content, color = if (user) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConfirmationCard(action: AiPendingAction, confirming: Boolean, onConfirm: (AiPendingAction) -> Unit, onCancel: (AiPendingAction) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Create Entry", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(action.displaySummary)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(enabled = !confirming, onClick = { onCancel(action) }) { Text("Cancel") }
                Button(enabled = !confirming, onClick = { onConfirm(action) }) {
                    if (confirming) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Create")
                }
            }
        }
    }
}

@Composable
private fun TypingIndicator() { Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), horizontalArrangement = Arrangement.Start) { Text("Work Social AI is thinking…", color = MaterialTheme.colorScheme.onSurfaceVariant) } }
