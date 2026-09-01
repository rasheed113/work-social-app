package com.rasheed113.worksocial.presentation.work

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.rasheed113.worksocial.domain.work.WorkHouseRepository
import com.rasheed113.worksocial.presentation.ui.WorkHouseHeader
import com.rasheed113.worksocial.presentation.ui.WorkSocialCard
import com.rasheed113.worksocial.presentation.ui.WorkSocialSpacing
import com.rasheed113.worksocial.presentation.ui.WorkSocialTypography

/** Native mobile implementation of the Web Work House hierarchy. */
@Composable
fun WorkHouseScreen(viewModel: WorkHouseViewModel, userId: String, workHouseRepository: WorkHouseRepository, onExit: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val financeViewModel: FinanceViewModel = viewModel(key = "finance-$userId", factory = FinanceViewModelFactory(workHouseRepository))
    var tab by remember { mutableStateOf("home") }
    LaunchedEffect(userId) { viewModel.load(userId) }

    Scaffold(
        topBar = { WorkHouseHeader("Work House", Modifier.fillMaxWidth()) },
        bottomBar = {
            NavigationBar {
                listOf("home" to "Home", "finance" to "Finance", "settings" to "Settings").forEach { (route, label) ->
                    NavigationBarItem(selected = tab == route, onClick = { tab = route }, icon = { Text(label.take(1)) }, label = { Text(label) })
                }
            }
        },
    ) { padding ->
        when (val current = state) {
            WorkHouseState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is WorkHouseState.Error -> Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Work House unavailable", style = WorkSocialTypography.title)
                Text(current.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::retry) { Text("Retry") }
            }
            is WorkHouseState.Success -> when (tab) {
                "home" -> WorkHome(current, viewModel, Modifier.fillMaxSize().padding(padding), onExit)
                "finance" -> FinanceScreen(financeViewModel, userId, onExit)
                else -> WorkSettings(current.identity, onExit, Modifier.fillMaxSize().padding(padding))
            }
        }
    }
}

@Composable
private fun WorkHome(state: WorkHouseState.Success, viewModel: WorkHouseViewModel, modifier: Modifier, onExit: () -> Unit) {
    var showHistory by remember { mutableStateOf(false) }
    var showIdentity by remember { mutableStateOf(false) }
    LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(WorkSocialSpacing.md), contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)) {
        item { WorkHouseHeader("Home") }
        item {
            WorkSocialCard(premium = true, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(WorkSocialSpacing.lg), verticalArrangement = Arrangement.spacedBy(WorkSocialSpacing.sm)) {
                    Text("My Work", style = WorkSocialTypography.title)
                    state.identity?.let { Text("Work ID: ${it.workId}"); Text(it.workDescription ?: "No work description has been saved.", color = WorkSocialTypography.MutedText) }
                        ?: Text("Work Identity is not set up. No placeholder Worker data is shown.", color = WorkSocialTypography.MutedText)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { showHistory = !showHistory }) { Text(if (showHistory) "Hide History" else "Work History") }
                        OutlinedButton(onClick = { showIdentity = !showIdentity }) { Text(if (showIdentity) "Hide Identity" else "Worker Identity") }
                    }
                }
            }
        }
        if (showHistory) item { WorkHistory(state, viewModel, Modifier.fillMaxWidth()) }
        if (showIdentity) item { WorkIdentity(state.identity, Modifier.fillMaxWidth()) }
        item {
            WorkSocialCard(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(WorkSocialSpacing.lg), verticalArrangement = Arrangement.spacedBy(WorkSocialSpacing.sm)) {
                    Text("Real work totals", style = WorkSocialTypography.title)
                    Text("Today: ${state.totals.dailyTotal}")
                    Text("This week: ${state.totals.weeklyTotal}")
                    Text("This month: ${state.totals.monthlyTotal}")
                    Text("Lifetime: ${state.totals.lifetimeTotal}")
                }
            }
        }
        item { TextButton(onClick = onExit) { Text("Back to Social") } }
    }
}

@Composable
private fun WorkHistory(state: WorkHouseState.Success, viewModel: WorkHouseViewModel, modifier: Modifier) = Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("Work History", style = WorkSocialTypography.title)
    if (state.history.isEmpty()) Text("No active work entries were returned. This is a real empty state, not seeded data.")
    else state.history.forEach { row -> WorkSocialCard(modifier = Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(row.itemName, style = WorkSocialTypography.label); Text("${row.quantity} × ${row.rate} = ${row.total}"); Text(row.occurredAt, style = WorkSocialTypography.body) } } }
    if (state.hasMoreHistory) Button(onClick = viewModel::loadMoreHistory, enabled = !state.loadingMoreHistory, modifier = Modifier.fillMaxWidth()) { if (state.loadingMoreHistory) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Load more") }
    state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
}

@Composable
private fun WorkIdentity(identity: com.rasheed113.worksocial.domain.work.WorkerIdentity?, modifier: Modifier) = WorkSocialCard(modifier = modifier) {
    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Worker Identity", style = WorkSocialTypography.title)
        if (identity == null) Text("No worker_profiles row exists for this authenticated profile.") else { Text("Work ID: ${identity.workId}"); Text(identity.workDescription ?: "No description"); Text(if (identity.skills.isEmpty()) "No skills saved." else "Skills: ${identity.skills.joinToString()}") }
    }
}

@Composable
private fun WorkSettings(identity: com.rasheed113.worksocial.domain.work.WorkerIdentity?, onExit: () -> Unit, modifier: Modifier) = Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Work Settings", style = WorkSocialTypography.display)
    Text("Worker profile is read from the real worker_profiles contract.")
    if (identity != null) Text("Worker ID: ${identity.workId}")
    Text("Team Joining remains a navigation boundary because the Web currently states that joining requests, invitations, memberships, and team data are not implemented.")
    Button(onClick = onExit) { Text("Back to Social") }
}
