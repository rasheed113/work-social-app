package com.rasheed113.worksocial.presentation.social

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.rasheed113.worksocial.domain.social.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun SocialHomeScreen(
    repository: SocialPostRepository,
    refreshToken: Int = 0,
    onCreatePost: () -> Unit = {},
    targetPostId: String? = null,
    targetCommentId: String? = null,
    onTargetConsumed: () -> Unit = {},
) {
    val homeViewModel: SocialHomeViewModel = viewModel(factory = SocialHomeViewModelFactory(repository))
    val state by homeViewModel.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    var openCommentsPostId by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(refreshToken) { if (refreshToken == 0) homeViewModel.load() else homeViewModel.refresh() }
    LaunchedEffect(targetPostId, targetCommentId, state) {
        val current = state
        if (targetPostId.isNullOrBlank() || current !is SocialHomeState.Success) return@LaunchedEffect
        val index = current.posts.indexOfFirst { it.id == targetPostId }
        if (index >= 0) {
            listState.animateScrollToItem(index)
            if (!targetCommentId.isNullOrBlank()) openCommentsPostId = targetPostId
            onTargetConsumed()
        }
    }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Home", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold); Text("Your Social feed", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            TextButton(onClick = onCreatePost) { Text("Create") }; TextButton(onClick = homeViewModel::refresh) { Text("Refresh") }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = .55f))
        when (val current = state) {
            SocialHomeState.Loading -> LoadingContent()
            SocialHomeState.Empty -> EmptyContent(homeViewModel::refresh)
            is SocialHomeState.Error -> ErrorContent(current.message, homeViewModel::refresh)
            is SocialHomeState.Success -> {
                LazyColumn(state = listState, modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(current.posts, key = SocialPost::id) { post -> SocialPostCard(post, post.id in current.likingPostIds, { homeViewModel.toggleLike(post.id) }, { openCommentsPostId = post.id }) }
                }
                openCommentsPostId?.let { postId -> current.posts.firstOrNull { it.id == postId }?.let { post -> CommentsSheet(post, current.comments[postId], current.actionError, current.commentMutations, { homeViewModel.openComments(postId) }, { homeViewModel.createComment(postId, it) }, { homeViewModel.deleteComment(postId, it) }, { openCommentsPostId = null }) } }
            }
        }
    }
}

@Composable private fun LoadingContent() { Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { CircularProgressIndicator(); Text("Loading Social Home…", color = MaterialTheme.colorScheme.onSurfaceVariant) } } }
@Composable private fun EmptyContent(onRefresh: () -> Unit) { Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("No public posts yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold); Text("The Social Home feed is currently empty.", color = MaterialTheme.colorScheme.onSurfaceVariant); TextButton(onClick = onRefresh) { Text("Refresh") } } } }
@Composable private fun ErrorContent(message: String, onRetry: () -> Unit) { Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) { Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = .72f)), shape = RoundedCornerShape(18.dp)) { Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) { Text("Unable to load Social Home", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onErrorContainer); Text(message, color = MaterialTheme.colorScheme.onErrorContainer); Button(onClick = onRetry) { Text("Try again") } } } } }

@Composable private fun SocialPostCard(post: SocialPost, isLikeProcessing: Boolean, onToggleLike: () -> Unit, onComments: () -> Unit) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), elevation = CardDefaults.cardElevation(defaultElevation = 3.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.fillMaxWidth().padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Avatar(post.author.avatar_url, post.author.display_name); Spacer(Modifier.width(10.dp)); Column(Modifier.weight(1f)) { Text(post.author.display_name, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis); Text("@${post.author.username} · ${formatTimestamp(post.created_at)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis) }; Text("Public", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.clip(RoundedCornerShape(50)).background(MaterialTheme.colorScheme.primary.copy(alpha = .09f)).padding(horizontal = 8.dp, vertical = 5.dp)) }
            if (post.content.isNotBlank()) Text(post.content, Modifier.padding(top = 12.dp), style = MaterialTheme.typography.bodyLarge)
            if (post.media.isNotEmpty()) Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { post.media.forEach { SocialPostMediaView(it) } }
            if (post.location_name != null || post.latitude != null) Text("📍 ${post.location_name ?: listOfNotNull(post.latitude, post.longitude).joinToString(", ")}", Modifier.padding(top = 10.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(Modifier.fillMaxWidth().padding(top = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onToggleLike, enabled = !isLikeProcessing, modifier = Modifier.semantics { contentDescription = if (post.isLikedByCurrentUser) "Unlike post" else "Like post" }) { if (isLikeProcessing) { CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp); Spacer(Modifier.width(7.dp)); Text("Updating…") } else { Text(if (post.isLikedByCurrentUser) "♥ Liked" else "♡ Like"); Spacer(Modifier.width(7.dp)); Text(post.likeCount.toString(), color = MaterialTheme.colorScheme.onSurfaceVariant) } }
                TextButton(onClick = onComments) { Text("Comments") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable private fun CommentsSheet(post: SocialPost, state: CommentsState?, actionError: String?, mutations: Set<String>, onLoad: () -> Unit, onCreate: (String) -> Unit, onDelete: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember(post.id) { mutableStateOf("") }
    LaunchedEffect(post.id) { if (state == null) onLoad() }
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.imePadding()) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Comments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            when (state) {
                null, CommentsState.Loading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                is CommentsState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Unable to load comments", fontWeight = FontWeight.SemiBold); Text(state.message, color = MaterialTheme.colorScheme.error); TextButton(onClick = onLoad) { Text("Try again") } }
                is CommentsState.Success -> if (state.comments.isEmpty()) Text("No comments yet. Be the first to comment.", color = MaterialTheme.colorScheme.onSurfaceVariant) else LazyColumn(Modifier.fillMaxWidth().height(280.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { items(state.comments, key = SocialComment::id) { comment -> CommentRow(comment, mutations, onDelete) } }
            }
            actionError?.let { Text(it, color = MaterialTheme.colorScheme.error, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) { OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), placeholder = { Text("Write a comment…") }, enabled = post.id !in mutations, minLines = 1, maxLines = 4); Spacer(Modifier.width(8.dp)); TextButton(enabled = text.trim().isNotEmpty() && post.id !in mutations, onClick = { onCreate(text); text = "" }) { if (post.id in mutations) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Send") } }
        }
    }
}

@Composable private fun CommentRow(comment: SocialComment, mutations: Set<String>, onDelete: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Avatar(comment.author.avatar_url, comment.author.display_name); Spacer(Modifier.width(8.dp)); Column(Modifier.weight(1f)) { Text(comment.author.display_name, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.labelLarge); Text(comment.content, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp)); Text(formatTimestamp(comment.createdAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 3.dp)) }
        if (comment.isOwnedByCurrentUser) TextButton(enabled = comment.id !in mutations, onClick = { onDelete(comment.id) }) { Text("Delete") }
    }
}

@Composable private fun Avatar(url: String?, name: String) { if (!url.isNullOrBlank()) AsyncImage(model = url, contentDescription = "$name avatar", modifier = Modifier.size(44.dp).clip(CircleShape), contentScale = ContentScale.Crop) else Box(Modifier.size(44.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary.copy(alpha = .12f)), contentAlignment = Alignment.Center) { Text(name.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) } }
@Composable private fun SocialPostMediaView(media: SocialPostMedia) { val context = LocalContext.current; when (media.kind) { "image" -> AsyncImage(model = media.public_url, contentDescription = media.file_name ?: "Post image", modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)), contentScale = ContentScale.FillWidth); "video" -> AndroidVideoView(media.public_url); else -> TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(media.public_url))) }, modifier = Modifier.fillMaxWidth()) { Text("📎 ${media.file_name ?: "Open attachment"}", maxLines = 1, overflow = TextOverflow.Ellipsis) } } }
@Composable private fun AndroidVideoView(url: String) { val context = LocalContext.current; val controller = remember { MediaController(context) }; androidx.compose.ui.viewinterop.AndroidView(factory = { VideoView(it).apply { setMediaController(controller); controller.setAnchorView(this); setVideoPath(url) } }, update = { view -> if (view.tag != url) { view.tag = url; view.setVideoPath(url) } }, modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(14.dp))) }
private fun formatTimestamp(value: String): String = runCatching { DateTimeFormatter.ofPattern("dd MMM, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(value)) }.getOrDefault(value)
