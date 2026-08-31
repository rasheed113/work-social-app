package com.rasheed113.worksocial.presentation.social

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rasheed113.worksocial.domain.social.SocialPostRepository

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CreatePostScreen(
    repository: SocialPostRepository,
    onCreated: () -> Unit,
    onBack: () -> Unit,
) {
    val createPostViewModel: CreatePostViewModel = viewModel(
        factory = CreatePostViewModelFactory(repository),
    )
    val state by createPostViewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(state) {
        if (state is CreatePostState.Success) {
            onCreated()
            onBack()
        }
    }

    val content = state.content
    val submitting = state is CreatePostState.Submitting
    val errorMessage = when (val current = state) {
        is CreatePostState.ValidationError -> current.message
        is CreatePostState.BackendError -> current.message
        else -> null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create post") },
                navigationIcon = {
                    TextButton(onClick = onBack, enabled = !submitting) { Text("Cancel") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .verticalScroll(rememberScrollState())
                .imePadding()
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                "Share something with your community",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = content,
                onValueChange = createPostViewModel::onContentChanged,
                modifier = Modifier.fillMaxWidth(),
                minLines = 6,
                enabled = !submitting,
                label = { Text("What's happening?") },
                placeholder = { Text("Write your post…") },
                supportingText = {
                    Text("Text posts only in this phase. The website contract accepts any non-empty text.")
                },
                isError = errorMessage != null,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { createPostViewModel.submit() }),
            )
            errorMessage?.let { message ->
                Text(
                    message,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Button(
                onClick = createPostViewModel::submit,
                enabled = !submitting && content.trim().isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                if (submitting) {
                    CircularProgressIndicator(strokeWidth = 2.dp)
                } else {
                    Text("Post")
                }
            }
            Text(
                "Your post is saved only after Supabase confirms the real database insert.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
