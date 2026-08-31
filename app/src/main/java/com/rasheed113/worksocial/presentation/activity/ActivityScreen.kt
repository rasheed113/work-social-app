package com.rasheed113.worksocial.presentation.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.rasheed113.worksocial.domain.activity.ActivityNotification
import com.rasheed113.worksocial.domain.activity.ActivityState
import com.rasheed113.worksocial.domain.activity.ActivityType
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityScreen(
    viewModel: ActivityViewModel,
    onOpenPost: (postId: String, commentId: String?) -> Unit,
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("Activity", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Text(
                    text = when (state) {
                        is ActivityState.Success -> if (state.unreadCount == 0) "All caught up" else "${state.unreadCount} unread"
                        else -> "Your social activity"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (state is ActivityState.Success && state.unreadCount > 0) {
                TextButton(onClick = viewModel::markAllRead) { Text("Mark all read") }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))

        when (state) {
            ActivityState.Loading -> ActivityLoading()
            ActivityState.Empty -> ActivityEmpty()
            is ActivityState.Error -> ActivityError(state.message, viewModel::retry)
            is ActivityState.Success -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                items(state.items, key = ActivityNotification::id) { item ->
                    ActivityItem(item) { viewModel.markRead(item, onOpenPost) }
                }
            }
        }
    }
}

@Composable
private fun ActivityItem(item: ActivityNotification, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (item.isRead) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primaryContainer.copy(alpha = .28f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (item.isRead) 1.dp else 3.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ActivityAvatar(item)
            Spacer(Modifier.size(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.actor?.displayName ?: item.actor?.username ?: "Someone",
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.size(5.dp))
                    Text(activityIcon(item.type), style = MaterialTheme.typography.labelLarge)
                }
                Text(
                    text = activityAction(item.type),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = formatTimestamp(item.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = .72f),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
            if (!item.isRead) {
                Text("new", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ActivityAvatar(item: ActivityNotification) {
    val actorName = item.actor?.displayName ?: item.actor?.username ?: "Someone"
    if (!item.actor?.avatarUrl.isNullOrBlank()) {
        AsyncImage(
            model = item.actor?.avatarUrl,
            contentDescription = "$actorName avatar",
            modifier = Modifier.size(44.dp).clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = .12f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(actorName.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun ActivityLoading() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator()
            Text("Loading Activity…", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityEmpty() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("🔔", style = MaterialTheme.typography.headlineMedium)
            Text("No notifications yet.", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("Activity from your community will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ActivityError(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = .72f)),
            shape = RoundedCornerShape(18.dp),
        ) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Unable to load Activity", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer)
                Text(message, color = MaterialTheme.colorScheme.onErrorContainer)
                Button(onClick = onRetry) { Text("Try again") }
            }
        }
    }
}

internal fun activityAction(type: ActivityType): String = when (type) {
    ActivityType.LIKE -> "liked your post"
    ActivityType.COMMENT -> "commented on your post"
    ActivityType.COMMENT_REPLY -> "replied to your comment"
    ActivityType.MENTION_POST -> "mentioned you in a post"
    ActivityType.MENTION_COMMENT -> "mentioned you in a comment"
    ActivityType.FOLLOW -> "started following you"
    ActivityType.MESSAGE -> "sent you a message"
    ActivityType.FRIEND_REQUEST -> "sent you a friend request"
    ActivityType.FRIEND_ACCEPT -> "accepted your friend request"
    ActivityType.UNKNOWN -> "sent you a notification"
}

internal fun activityIcon(type: ActivityType): String = when (type) {
    ActivityType.LIKE -> "❤️"
    ActivityType.COMMENT -> "💬"
    ActivityType.COMMENT_REPLY -> "↩️"
    ActivityType.MENTION_POST, ActivityType.MENTION_COMMENT -> "@"
    ActivityType.FOLLOW -> "✨"
    ActivityType.MESSAGE -> "💌"
    ActivityType.FRIEND_REQUEST -> "👥"
    ActivityType.FRIEND_ACCEPT -> "🤝"
    ActivityType.UNKNOWN -> "🔔"
}

private fun formatTimestamp(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("dd MMM, HH:mm")
        .withZone(ZoneId.systemDefault())
        .format(Instant.parse(value))
}.getOrDefault(value)
