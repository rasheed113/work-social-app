package com.rasheed113.worksocial.presentation.social

import android.content.Intent
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.rasheed113.worksocial.domain.social.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val WsText = Color(0xFF17202A)
private val WsMuted = Color(0xFF64748B)
private val WsPurple = Color(0xFF6D5DFC)
private val WsIndigo = Color(0xFF5146E5)
private val WsCyan = Color(0xFF22B8D4)
private val WsBorder = Color(0x2264748B)

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

    LaunchedEffect(refreshToken) {
        if (refreshToken == 0) homeViewModel.load() else homeViewModel.refresh()
    }
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

    Column(Modifier.fillMaxSize().background(Color(0xFFF5F7FA))) {
        HomeHeader(onCreatePost = onCreatePost, onRefresh = homeViewModel::refresh)
        HorizontalDivider(color = WsBorder)
        when (val current = state) {
            SocialHomeState.Loading -> LoadingContent()
            SocialHomeState.Empty -> EmptyContent(homeViewModel::refresh)
            is SocialHomeState.Error -> ErrorContent(current.message, homeViewModel::refresh)
            is SocialHomeState.Success -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 14.dp).let { PaddingValues(it.calculateLeftPadding(androidx.compose.ui.unit.LayoutDirection.Ltr), it.calculateTopPadding(), it.calculateRightPadding(androidx.compose.ui.unit.LayoutDirection.Ltr), 112.dp) },
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item { FeedSectionHeader() }
                    items(current.posts, key = SocialPost::id) { post ->
                        SocialPostCard(
                            post = post,
                            isLikeProcessing = post.id in current.likingPostIds,
                            onToggleLike = { homeViewModel.toggleLike(post.id) },
                            onComments = { openCommentsPostId = post.id },
                        )
                    }
                }
                openCommentsPostId?.let { postId ->
                    current.posts.firstOrNull { it.id == postId }?.let { post ->
                        CommentsSheet(
                            post = post,
                            state = current.comments[postId],
                            actionError = current.actionError,
                            mutations = current.commentMutations,
                            onLoad = { homeViewModel.openComments(postId) },
                            onCreate = { homeViewModel.createComment(postId, it) },
                            onDelete = { homeViewModel.deleteComment(postId, it) },
                            onDismiss = { openCommentsPostId = null },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeHeader(onCreatePost: () -> Unit, onRefresh: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Home",
                    fontSize = 36.sp,
                    lineHeight = 40.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1.3).sp,
                    style = LocalTextStyle.current.copy(brush = Brush.linearGradient(listOf(WsPurple, WsCyan, Color(0xFFFF5CA8)))),
                )
                Text("Your Social feed", color = WsMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                HomeActionButton("Create", onCreatePost, filled = true)
                HomeActionButton("Refresh", onRefresh)
            }
        }
    }
}

@Composable
private fun HomeActionButton(label: String, onClick: () -> Unit, filled: Boolean = false) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.height(34.dp),
        contentPadding = PaddingValues(horizontal = 11.dp),
        colors = ButtonDefaults.textButtonColors(
            containerColor = if (filled) WsPurple else Color.White,
            contentColor = if (filled) Color.White else WsText,
        ),
        shape = RoundedCornerShape(10.dp),
    ) { Text(label, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold) }
}

@Composable
private fun FeedSectionHeader() {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(15.dp)).background(Brush.linearGradient(listOf(Color.White, Color(0xFFF1F5FF)))).padding(horizontal = 13.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(7.dp).height(27.dp).clip(RoundedCornerShape(99.dp)).background(Brush.verticalGradient(listOf(WsCyan, WsPurple, Color(0xFFFF5CA8)))))
        Spacer(Modifier.width(10.dp))
        Text("Public posts", color = WsText, fontSize = 17.sp, fontWeight = FontWeight.Black, letterSpacing = (-.3).sp)
    }
}

@Composable
private fun LoadingContent() {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            CircularProgressIndicator(color = WsPurple)
            Text("Loading Social Home…", color = WsMuted, fontSize = 13.sp)
        }
    }
}

@Composable
private fun EmptyContent(onRefresh: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("No public posts yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Black, color = WsText)
            Text("The Social Home feed is currently empty.", color = WsMuted, fontSize = 13.sp)
            HomeActionButton("Refresh", onRefresh)
        }
    }
}

@Composable
private fun ErrorContent(message: String, onRetry: () -> Unit) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 5.dp)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Unable to load Social Home", fontWeight = FontWeight.Black, color = WsText)
                Text(message, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
                Button(onClick = onRetry, colors = ButtonDefaults.buttonColors(containerColor = WsIndigo), shape = RoundedCornerShape(10.dp)) { Text("Try again") }
            }
        }
    }
}

@Composable
private fun SocialPostCard(post: SocialPost, isLikeProcessing: Boolean, onToggleLike: () -> Unit, onComments: () -> Unit) {
    val context = LocalContext.current
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 5.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Avatar(post.author.avatar_url, post.author.display_name)
                Spacer(Modifier.width(9.dp))
                Column(Modifier.weight(1f)) {
                    Text(post.author.display_name, color = WsText, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 13.sp)
                    Text("@${post.author.username} · ${formatTimestamp(post.created_at)}", color = WsMuted, fontSize = 9.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text("Public", color = WsIndigo, fontSize = 9.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.clip(RoundedCornerShape(99.dp)).background(WsPurple.copy(alpha = .09f)).padding(horizontal = 8.dp, vertical = 5.dp))
            }
            if (post.content.isNotBlank()) Text(post.content, Modifier.padding(top = 10.dp), color = WsText, fontSize = 14.sp, lineHeight = 20.sp)
            if (post.media.isNotEmpty()) Column(Modifier.padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) { post.media.forEach { SocialPostMediaView(it) } }
            if (post.location_name != null || post.latitude != null) Text("📍 ${post.location_name ?: listOfNotNull(post.latitude, post.longitude).joinToString(", ")}", Modifier.padding(top = 9.dp), style = MaterialTheme.typography.labelMedium, color = WsMuted)
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = onToggleLike,
                    enabled = !isLikeProcessing,
                    modifier = Modifier.semantics { contentDescription = if (post.isLikedByCurrentUser) "Unlike post" else "Like post" },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    if (isLikeProcessing) CircularProgressIndicator(Modifier.size(17.dp), strokeWidth = 2.dp, color = WsPurple)
                    else {
                        Text(if (post.isLikedByCurrentUser) "♥ Liked" else "♡ Like", color = if (post.isLikedByCurrentUser) WsPurple else WsText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Spacer(Modifier.width(6.dp))
                        Text(post.likeCount.toString(), color = WsMuted, fontSize = 11.sp)
                    }
                }
                TextButton(onClick = onComments, contentPadding = PaddingValues(horizontal = 8.dp)) { Text("Comments", color = WsText, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                TextButton(
                    onClick = {
                        val shareText = buildString {
                            append("${post.author.display_name} on Work Social")
                            if (post.content.isNotBlank()) append("\n\n${post.content}")
                            append("\n\nPost ID: ${post.id}")
                        }
                        context.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, shareText)
                        }, "Share post"))
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) { Text("Share", color = WsText, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommentsSheet(post: SocialPost, state: CommentsState?, actionError: String?, mutations: Set<String>, onLoad: () -> Unit, onCreate: (String) -> Unit, onDelete: (String) -> Unit, onDismiss: () -> Unit) {
    var text by remember(post.id) { mutableStateOf("") }
    LaunchedEffect(post.id) { if (state == null) onLoad() }
    ModalBottomSheet(onDismissRequest = onDismiss, modifier = Modifier.imePadding()) {
        Column(Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp).padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Comments", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Black, color = WsText)
            when (state) {
                null, CommentsState.Loading -> Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = WsPurple) }
                is CommentsState.Error -> Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Unable to load comments", fontWeight = FontWeight.Bold); Text(state.message, color = MaterialTheme.colorScheme.error); TextButton(onClick = onLoad) { Text("Try again") } }
                is CommentsState.Success -> if (state.comments.isEmpty()) Text("No comments yet. Be the first to comment.", color = WsMuted) else LazyColumn(Modifier.fillMaxWidth().height(280.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) { items(state.comments, key = SocialComment::id) { comment -> CommentRow(comment, mutations, onDelete) } }
            }
            actionError?.let { Text(it, color = MaterialTheme.colorScheme.error, maxLines = 2, overflow = TextOverflow.Ellipsis) }
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(value = text, onValueChange = { text = it }, modifier = Modifier.weight(1f), placeholder = { Text("Write a comment…") }, enabled = post.id !in mutations, minLines = 1, maxLines = 4)
                Spacer(Modifier.width(8.dp))
                TextButton(enabled = text.trim().isNotEmpty() && post.id !in mutations, onClick = { onCreate(text); text = "" }) { if (post.id in mutations) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp) else Text("Send") }
            }
        }
    }
}

@Composable
private fun CommentRow(comment: SocialComment, mutations: Set<String>, onDelete: (String) -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        Avatar(comment.author.avatar_url, comment.author.display_name, 34.dp)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(comment.author.display_name, fontWeight = FontWeight.Black, color = WsText, fontSize = 12.sp)
            Text(comment.content, color = WsText, fontSize = 13.sp, modifier = Modifier.padding(top = 2.dp))
            Text(formatTimestamp(comment.createdAt), color = WsMuted, fontSize = 9.sp, modifier = Modifier.padding(top = 3.dp))
        }
        if (comment.isOwnedByCurrentUser) TextButton(enabled = comment.id !in mutations, onClick = { onDelete(comment.id) }) { Text("Delete", fontSize = 11.sp) }
    }
}

@Composable
private fun Avatar(url: String?, name: String, size: androidx.compose.ui.unit.Dp = 44.dp) {
    val ring = Brush.linearGradient(listOf(WsPurple, WsCyan, Color(0xFFFF5CA8)))
    Box(Modifier.size(size).clip(CircleShape).background(ring).padding(2.dp)) {
        if (!url.isNullOrBlank()) AsyncImage(model = url, contentDescription = "$name avatar", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
        else Box(Modifier.fillMaxSize().clip(CircleShape).background(Brush.linearGradient(listOf(Color(0xFFE9E7FF), Color(0xFFDDF8FC)))), contentAlignment = Alignment.Center) { Text(name.firstOrNull()?.uppercase() ?: "?", fontWeight = FontWeight.Black, color = WsIndigo) }
    }
}

@Composable
private fun SocialPostMediaView(media: SocialPostMedia) {
    val context = LocalContext.current
    when (media.kind) {
        "image" -> AsyncImage(model = media.public_url, contentDescription = media.file_name ?: "Post image", modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(13.dp)), contentScale = ContentScale.FillWidth)
        "video" -> AndroidVideoView(media.public_url)
        else -> TextButton(onClick = { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(media.public_url))) }, modifier = Modifier.fillMaxWidth()) { Text("📎 ${media.file_name ?: "Open attachment"}", maxLines = 1, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun AndroidVideoView(url: String) {
    val context = LocalContext.current
    val controller = remember { MediaController(context) }
    androidx.compose.ui.viewinterop.AndroidView(
        factory = { VideoView(it).apply { setMediaController(controller); controller.setAnchorView(this); setVideoPath(url) } },
        update = { view -> if (view.tag != url) { view.tag = url; view.setVideoPath(url) } },
        modifier = Modifier.fillMaxWidth().height(220.dp).clip(RoundedCornerShape(13.dp)),
    )
}

private fun formatTimestamp(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("dd MMM, HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(value))
}.getOrDefault(value)
