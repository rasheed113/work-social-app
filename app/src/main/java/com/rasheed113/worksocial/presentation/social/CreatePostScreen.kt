package com.rasheed113.worksocial.presentation.social

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rasheed113.worksocial.domain.social.CreatePostAttachment
import com.rasheed113.worksocial.domain.social.CreatePostLocation
import com.rasheed113.worksocial.domain.social.SocialPostRepository
import java.util.Locale

private val WsText = Color(0xFF17202A)
private val WsMuted = Color(0xFF64748B)
private val WsIndigo = Color(0xFF5146E5)
private val WsBackground = Color(0xFFF5F7FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(repository: SocialPostRepository, onCreated: () -> Unit, onBack: () -> Unit) {
    val vm: CreatePostViewModel = viewModel(factory = CreatePostViewModelFactory(repository))
    val state by vm.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var locationError by remember { mutableStateOf<String?>(null) }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenMultipleDocuments()) { uris ->
        val picked = uris.mapNotNull { uri ->
            runCatching {
                val resolver = context.contentResolver
                val mime = resolver.getType(uri)
                val name = resolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c -> if (c.moveToFirst()) c.getString(0) else null } ?: "attachment"
                val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@runCatching null
                val kind = when {
                    mime?.startsWith("image/") == true -> "image"
                    mime?.startsWith("video/") == true -> "video"
                    else -> "file"
                }
                CreatePostAttachment(name, mime, bytes, kind)
            }.getOrNull()
        }
        if (picked.isNotEmpty()) vm.setAttachments(state.attachments + picked)
    }

    val locationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        if (grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            readLastKnownLocation(context) { location, error -> locationError = error; if (location != null) vm.setLocation(CreatePostLocation(location.latitude, location.longitude)) }
        } else locationError = "Location permission was not granted."
    }

    LaunchedEffect(state) { if (state is CreatePostState.Success) { onCreated(); onBack() } }
    val submitting = state is CreatePostState.Submitting
    val errorMessage = when (val current = state) {
        is CreatePostState.ValidationError -> current.message
        is CreatePostState.BackendError -> current.message
        else -> null
    }

    Scaffold(
        containerColor = WsBackground,
        topBar = { TopAppBar(title = { Text("Create post", fontWeight = FontWeight.Black, color = WsText) }, navigationIcon = { TextButton(onClick = onBack, enabled = !submitting) { Text("Cancel", color = WsIndigo, fontWeight = FontWeight.Bold) } }) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding().padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White), elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Share with your community", fontSize = 20.sp, fontWeight = FontWeight.Black, color = WsText)
                    Text("Create a real Work Social post with text, media, or your current location.", fontSize = 12.sp, color = WsMuted)
                    OutlinedTextField(value = state.content, onValueChange = vm::onContentChanged, modifier = Modifier.fillMaxWidth(), minLines = 6, enabled = !submitting, label = { Text("What's happening?") }, placeholder = { Text("Write your post…") }, isError = errorMessage != null, keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Default))
                    if (state.attachments.isNotEmpty()) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(state.attachments) { attachment ->
                                Card(shape = RoundedCornerShape(12.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F3FF))) {
                                    Column(Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                        Text(attachment.kind.uppercase(Locale.US), color = WsIndigo, fontSize = 9.sp, fontWeight = FontWeight.Black)
                                        Text(attachment.fileName, color = WsText, fontSize = 11.sp, maxLines = 1)
                                        Text("${attachment.fileSize / 1024} KB", color = WsMuted, fontSize = 9.sp)
                                    }
                                }
                            }
                        }
                    }
                    state.location?.let { Text("📍 ${it.latitude}, ${it.longitude}", color = WsIndigo, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    locationError?.let { Text(it, color = MaterialTheme.colorScheme.error, fontSize = 11.sp) }
                    HorizontalDivider(color = Color(0x22647080))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ComposerAction("Media", !submitting) { filePicker.launch(arrayOf("image/*", "video/*", "application/pdf", "text/*", "application/*")) }
                        ComposerAction("Location", !submitting) {
                            locationError = null
                            val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
                            if (fine || coarse) readLastKnownLocation(context) { location, error -> locationError = error; if (location != null) vm.setLocation(CreatePostLocation(location.latitude, location.longitude)) }
                            else locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                        }
                        if (state.location != null) ComposerAction("Remove", !submitting) { vm.setLocation(null) }
                    }
                }
            }
            errorMessage?.let { message -> Card(shape = RoundedCornerShape(14.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2))) { Text(message, Modifier.padding(13.dp), color = MaterialTheme.colorScheme.error, fontSize = 12.sp) } }
            Button(onClick = vm::submit, enabled = !submitting && (state.content.isNotBlank() || state.attachments.isNotEmpty() || state.location != null), modifier = Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(12.dp), colors = ButtonDefaults.buttonColors(containerColor = WsIndigo)) {
                if (submitting) { CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Publishing…") } else Text("Post", fontWeight = FontWeight.Black)
            }
            Text("Your content is persisted only after Supabase confirms the post and every attachment.", fontSize = 10.sp, color = WsMuted)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ComposerAction(label: String, enabled: Boolean, onClick: () -> Unit) {
    TextButton(onClick = onClick, enabled = enabled, modifier = Modifier.height(36.dp), shape = RoundedCornerShape(10.dp)) { Text(label, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = WsIndigo) }
}

private fun readLastKnownLocation(context: Context, onResult: (android.location.Location?, String?) -> Unit) {
    val manager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    if (manager == null) { onResult(null, "Location service is unavailable."); return }
    val locations = manager.getProviders(true).mapNotNull { provider -> runCatching { manager.getLastKnownLocation(provider) }.getOrNull() }
    val best = locations.maxByOrNull { it.time }
    if (best == null) onResult(null, "No recent device location is available. Turn on location and try again.") else onResult(best, null)
}
