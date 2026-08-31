package com.rasheed113.worksocial.presentation.friends

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.collectAsState
import coil3.compose.AsyncImage
import com.rasheed113.worksocial.domain.friends.FriendPerson
import com.rasheed113.worksocial.domain.friends.FriendRequest
import com.rasheed113.worksocial.domain.friends.RelationshipState

@Composable
fun FriendsScreen(viewModel: FriendsViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var query by remember { mutableStateOf("") }
    LaunchedEffect(Unit) { viewModel.load() }

    when (val current = state) {
        FriendsUiState.Loading -> LoadingFriends()
        is FriendsUiState.Error -> ErrorFriends(current.message, viewModel::load)
        is FriendsUiState.Success -> {
            val people = current.data.people.filter { person ->
                val q = query.trim().lowercase()
                q.isEmpty() || person.profile.displayName.lowercase().contains(q) || person.profile.username.lowercase().contains(q)
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    Spacer(Modifier.height(4.dp))
                    Text("Friends", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Text("Real Work Social relationships and requests", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(8.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        label = { Text("Search people") },
                    )
                }
                if (current.data.incomingRequests.isNotEmpty()) {
                    item { SectionTitle("Friend requests", current.data.incomingRequests.size) }
                    items(current.data.incomingRequests, key = { "in-${it.id}" }) { request ->
                        RequestRow(request, current.busyRequestIds.contains(request.id), viewModel)
                    }
                }
                if (current.data.outgoingRequests.isNotEmpty()) {
                    item { SectionTitle("Pending requests", current.data.outgoingRequests.size) }
                    items(current.data.outgoingRequests, key = { "out-${it.id}" }) { request ->
                        OutgoingRow(request, current.busyRequestIds.contains(request.id), viewModel)
                    }
                }
                item { SectionTitle("People", people.size) }
                if (people.isEmpty()) {
                    item { EmptyPeople(query.isNotBlank()) }
                } else {
                    items(people, key = { it.profile.id }) { person ->
                        PersonRow(person, current.busyRequestIds.contains(person.profile.id), viewModel)
                    }
                }
                item { Spacer(Modifier.height(12.dp)) }
            }
        }
    }
}

@Composable
private fun SectionTitle(title: String, count: Int) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.weight(1f))
        Text(count.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun RequestRow(request: FriendRequest, busy: Boolean, viewModel: FriendsViewModel) {
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            ProfileIdentity(request.profile)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(start = 52.dp, top = 8.dp)) {
                Button(enabled = !busy, onClick = { viewModel.acceptRequest(request.id) }) { Text(if (busy) "Working…" else "Accept") }
                OutlinedButton(enabled = !busy, onClick = { viewModel.rejectRequest(request.id) }) { Text("Reject") }
            }
        }
    }
}

@Composable
private fun OutgoingRow(request: FriendRequest, busy: Boolean, viewModel: FriendsViewModel) {
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileIdentity(request.profile, Modifier.weight(1f))
            TextButton(enabled = !busy, onClick = { viewModel.cancelRequest(request.id) }) { Text(if (busy) "Working…" else "Cancel") }
        }
    }
}

@Composable
private fun PersonRow(person: FriendPerson, busy: Boolean, viewModel: FriendsViewModel) {
    Surface(shape = RoundedCornerShape(16.dp), tonalElevation = 1.dp) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            ProfileIdentity(person.profile, Modifier.weight(1f))
            when (person.relationship) {
                RelationshipState.FRIENDS -> Text("✓ Friends", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                RelationshipState.OUTGOING_PENDING -> Text("Pending", color = MaterialTheme.colorScheme.onSurfaceVariant)
                RelationshipState.INCOMING_PENDING -> Text("Request", color = MaterialTheme.colorScheme.onSurfaceVariant)
                RelationshipState.NONE -> Button(enabled = !busy, onClick = { viewModel.sendRequest(person.profile.id) }) { Text(if (busy) "Working…" else "Add friend") }
            }
        }
    }
}

@Composable
private fun ProfileIdentity(profile: com.rasheed113.worksocial.domain.friends.FriendProfile, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        if (profile.avatarUrl.isNullOrBlank()) {
            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                Box(contentAlignment = Alignment.Center) { Text(profile.displayName.take(1).uppercase()) }
            }
        } else {
            AsyncImage(
                model = profile.avatarUrl,
                contentDescription = "",
                modifier = Modifier.size(44.dp).clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        }
        Column(Modifier.weight(1f)) {
            Text(profile.displayName, fontWeight = FontWeight.SemiBold)
            Text("@${profile.username}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LoadingFriends() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
}

@Composable
private fun ErrorFriends(message: String, retry: () -> Unit) {
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Could not load Friends", style = MaterialTheme.typography.titleLarge)
        Text(message, color = MaterialTheme.colorScheme.error)
        Button(onClick = retry) { Text("Retry") }
    }
}

@Composable
private fun EmptyPeople(searching: Boolean) {
    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(if (searching) "No people found" else "No other Work Social members are available.")
    }
}
