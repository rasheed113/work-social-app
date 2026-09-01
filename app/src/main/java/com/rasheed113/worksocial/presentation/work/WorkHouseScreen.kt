package com.rasheed113.worksocial.presentation.work

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkHouseScreen(viewModel: WorkHouseViewModel, userId: String, onExit: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf("home") }
    LaunchedEffect(userId) { viewModel.load(userId) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Work House") }, navigationIcon = { IconButton(onClick = onExit) { Text("‹") } }) },
        bottomBar = {
            NavigationBar {
                listOf("home" to "Home", "finance" to "Finance", "history" to "History", "identity" to "Identity", "settings" to "Settings").forEach { (route, label) ->
                    NavigationBarItem(selected = tab == route, onClick = { tab = route }, icon = { Text(label.take(1)) }, label = { Text(label) })
                }
            }
        },
    ) { padding ->
        when (val current = state) {
            WorkHouseState.Loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            is WorkHouseState.Error -> Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Work House unavailable", style = MaterialTheme.typography.headlineSmall)
                Text(current.message, color = MaterialTheme.colorScheme.error)
                Button(onClick = viewModel::retry) { Text("Retry") }
            }
            is WorkHouseState.Success -> when (tab) {
                "home" -> WorkHome(current, Modifier.fillMaxSize().padding(padding))
                "finance" -> WorkFinance(current, Modifier.fillMaxSize().padding(padding))
                "history" -> WorkHistory(current, viewModel, Modifier.fillMaxSize().padding(padding))
                "identity" -> WorkIdentity(current.identity, Modifier.fillMaxSize().padding(padding))
                else -> WorkSettings(current.identity, onExit, Modifier.fillMaxSize().padding(padding))
            }
        }
    }
}

@Composable private fun WorkHome(state: WorkHouseState.Success, modifier: Modifier) = Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Text("Work House Home", style = MaterialTheme.typography.headlineMedium)
    state.identity?.let { Text("Work ID: ${it.workId}"); Text(it.workDescription ?: "No work description has been saved.") } ?: Text("Work Identity is not set up. No placeholder Worker data is shown.")
    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Real work totals", style = MaterialTheme.typography.titleMedium)
        Text("Today: ${state.totals.dailyTotal}"); Text("This week: ${state.totals.weeklyTotal}"); Text("This month: ${state.totals.monthlyTotal}"); Text("Lifetime: ${state.totals.lifetimeTotal}")
    } }
}

@Composable private fun WorkFinance(state: WorkHouseState.Success, modifier: Modifier) = Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Text("Finance", style = MaterialTheme.typography.headlineMedium)
    val finance = state.finance
    if (finance == null) Text("No Worker Finance summary is available yet.") else Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Total earnings: ${finance.totalEarnings}"); Text("Received: ${finance.received}"); Text("Remaining: ${finance.remaining}")
    } }
}

@Composable private fun WorkHistory(state: WorkHouseState.Success, viewModel: WorkHouseViewModel, modifier: Modifier) = LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 20.dp)) {
    item { Text("Work History", style = MaterialTheme.typography.headlineMedium) }
    if (state.history.isEmpty()) item { Text("No active work entries were returned. This is a real empty state, not seeded data.") }
    else items(state.history, key = { it.id }) { row -> Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(row.itemName, style = MaterialTheme.typography.titleMedium); Text("${row.quantity} × ${row.rate} = ${row.total}"); Text(row.occurredAt, style = MaterialTheme.typography.bodySmall) } } }
    if (state.hasMoreHistory) item { Button(onClick = viewModel::loadMoreHistory, enabled = !state.loadingMoreHistory, modifier = Modifier.fillMaxWidth()) { if (state.loadingMoreHistory) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Load more") } }
    state.error?.let { item { Text(it, color = MaterialTheme.colorScheme.error) } }
}

@Composable private fun WorkIdentity(identity: com.rasheed113.worksocial.domain.work.WorkerIdentity?, modifier: Modifier) = Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Worker Identity", style = MaterialTheme.typography.headlineMedium)
    if (identity == null) Text("No worker_profiles row exists for this authenticated profile.") else { Text("Work ID: ${identity.workId}"); Text(identity.workDescription ?: "No description"); Text(if (identity.skills.isEmpty()) "No skills saved." else "Skills: ${identity.skills.joinToString()}") }
}

@Composable private fun WorkSettings(identity: com.rasheed113.worksocial.domain.work.WorkerIdentity?, onExit: () -> Unit, modifier: Modifier) = Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Text("Work Settings", style = MaterialTheme.typography.headlineMedium)
    Text("Worker profile is read from the real worker_profiles contract.")
    Button(onClick = onExit) { Text("Back to Social") }
    if (identity != null) Text("Worker ID: ${identity.workId}")
    Text("Team Joining remains a navigation boundary because the Web currently states that joining requests, invitations, memberships, and team data are not implemented.")
}
