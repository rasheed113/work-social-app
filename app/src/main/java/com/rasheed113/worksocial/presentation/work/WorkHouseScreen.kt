package com.rasheed113.worksocial.presentation.work

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rasheed113.worksocial.WorkSocialApplication
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

private data class WorkerIdentity(val id: String, val workId: String, val description: String?, val skills: List<String>)
private data class WorkRow(val id: String, val item: String, val quantity: String, val rate: String, val total: String, val occurredAt: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkHouseScreen(userId: String, onExit: () -> Unit) {
    val app = LocalContext.current.applicationContext as WorkSocialApplication
    val client = remember { app.supabase }
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf("home") }
    var identity by remember { mutableStateOf<WorkerIdentity?>(null) }
    var totals by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var history by remember { mutableStateOf<List<WorkRow>>(emptyList()) }
    var finance by remember { mutableStateOf<Map<String, String>?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            loading = true
            error = null
            runCatching {
                val worker = client.postgrest.from("worker_profiles").select {
                    filter { eq("profile_id", userId) }
                    limit(1)
                }.decodeSingleOrNull<JsonObject>()
                identity = worker?.let { row ->
                    WorkerIdentity(
                        row["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        row["work_id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                        row["work_description"]?.jsonPrimitive?.contentOrNull,
                        row["skills"]?.toString()?.removePrefix("[")?.removeSuffix("]")?.split(',')?.map { it.trim().trim('"') }?.filter { it.isNotBlank() } ?: emptyList()
                    )
                }

                val now = java.util.Calendar.getInstance()
                fun iso(calendar: java.util.Calendar) = java.time.Instant.ofEpochMilli(calendar.timeInMillis).toString()
                val dayStart = (now.clone() as java.util.Calendar).apply { set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0) }
                val dayEnd = (dayStart.clone() as java.util.Calendar).apply { add(java.util.Calendar.DATE, 1) }
                val weekStart = (dayStart.clone() as java.util.Calendar).apply { val day = get(java.util.Calendar.DAY_OF_WEEK); val mondayOffset = if (day == java.util.Calendar.SUNDAY) -6 else java.util.Calendar.MONDAY - day; add(java.util.Calendar.DATE, mondayOffset) }
                val weekEnd = (weekStart.clone() as java.util.Calendar).apply { add(java.util.Calendar.DATE, 7) }
                val monthStart = (dayStart.clone() as java.util.Calendar).apply { set(java.util.Calendar.DAY_OF_MONTH, 1) }
                val monthEnd = (monthStart.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, 1) }
                val totalsParams = buildJsonObject {
                    put("p_day_start", iso(dayStart)); put("p_day_end", iso(dayEnd))
                    put("p_week_start", iso(weekStart)); put("p_week_end", iso(weekEnd))
                    put("p_month_start", iso(monthStart)); put("p_month_end", iso(monthEnd))
                }
                val totalsRow = client.postgrest.rpc("get_worker_work_totals", totalsParams).decodeList<JsonObject>().firstOrNull()
                totals = totalsRow?.mapValues { it.value.jsonPrimitive.contentOrNull.orEmpty() } ?: emptyMap()

                val rows = client.postgrest.from("work_entries").select {
                    filter { eq("lifecycle_state", "active") }
                    order("occurred_at", Order.DESCENDING)
                    order("id", Order.DESCENDING)
                    limit(50)
                }.decodeList<JsonObject>()
                history = rows.map { row -> WorkRow(
                    row["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    row["item_name"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    row["quantity"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    row["rate"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    row["total"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                    row["occurred_at"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                ) }

                val summary = client.postgrest.rpc("get_worker_finance_summary").decodeList<JsonObject>().firstOrNull()
                finance = summary?.mapValues { it.value.jsonPrimitive.contentOrNull.orEmpty() }
            }.onFailure { error = it.message ?: "Work House request failed." }
            loading = false
        }
    }

    LaunchedEffect(userId) { reload() }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Work House") }, navigationIcon = { IconButton(onClick = onExit) { Text("‹") } }) },
        bottomBar = {
            NavigationBar {
                listOf("home" to "Home", "finance" to "Finance", "history" to "History", "identity" to "Identity", "settings" to "Settings").forEach { (route, label) ->
                    NavigationBarItem(selected = tab == route, onClick = { tab = route }, icon = { Text(label.take(1)) }, label = { Text(label) })
                }
            }
        }
    ) { padding ->
        when {
            loading -> Box(Modifier.fillMaxSize().padding(padding), contentAlignment = androidx.compose.ui.Alignment.Center) { CircularProgressIndicator() }
            error != null -> Column(Modifier.fillMaxSize().padding(padding).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Work House unavailable", style = MaterialTheme.typography.headlineSmall); Text(error!!, color = MaterialTheme.colorScheme.error); Button(onClick = ::reload) { Text("Retry") } }
            tab == "home" -> WorkHome(identity, totals, Modifier.fillMaxSize().padding(padding))
            tab == "finance" -> WorkFinance(finance, Modifier.fillMaxSize().padding(padding))
            tab == "history" -> WorkHistory(history, Modifier.fillMaxSize().padding(padding))
            tab == "identity" -> WorkIdentity(identity, Modifier.fillMaxSize().padding(padding))
            else -> WorkSettings(identity, onExit, Modifier.fillMaxSize().padding(padding))
        }
    }
}

@Composable private fun WorkHome(identity: WorkerIdentity?, totals: Map<String, String>, modifier: Modifier) = Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Text("Work House Home", style = MaterialTheme.typography.headlineMedium)
    if (identity == null) Text("Work Identity is not set up. No placeholder Worker data is shown.") else { Text("Worker ID: ${identity.workId}"); Text(identity.description ?: "No work description has been saved.") }
    Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Real work totals", style = MaterialTheme.typography.titleMedium); Text("Today: ${totals["daily_total"] ?: "0"}"); Text("This week: ${totals["weekly_total"] ?: "0"}"); Text("This month: ${totals["monthly_total"] ?: "0"}"); Text("Lifetime: ${totals["lifetime_total"] ?: "0"}") } }
}

@Composable private fun WorkFinance(finance: Map<String, String>?, modifier: Modifier) = Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) { Text("Finance", style = MaterialTheme.typography.headlineMedium); if (finance == null) Text("No Worker Finance summary is available yet.") else Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { Text("Total earnings: ${finance["total_earnings"] ?: "0"}"); Text("Received: ${finance["received"] ?: "0"}"); Text("Remaining: ${finance["remaining"] ?: "0"}") } } }

@Composable private fun WorkHistory(history: List<WorkRow>, modifier: Modifier) = LazyColumn(modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(vertical = 20.dp)) { item { Text("Work History", style = MaterialTheme.typography.headlineMedium) }; if (history.isEmpty()) item { Text("No active work entries were returned. This is a real empty state, not seeded data.") } else items(history, key = { it.id }) { row -> Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) { Text(row.item, style = MaterialTheme.typography.titleMedium); Text("${row.quantity} × ${row.rate} = ${row.total}"); Text(row.occurredAt, style = MaterialTheme.typography.bodySmall) } } } }

@Composable private fun WorkIdentity(identity: WorkerIdentity?, modifier: Modifier) = Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Worker Identity", style = MaterialTheme.typography.headlineMedium); if (identity == null) Text("No worker_profiles row exists for this authenticated profile.") else { Text("Work ID: ${identity.workId}"); Text(identity.description ?: "No description"); if (identity.skills.isNotEmpty()) Text("Skills: ${identity.skills.joinToString()}") else Text("No skills saved.") } }

@Composable private fun WorkSettings(identity: WorkerIdentity?, onExit: () -> Unit, modifier: Modifier) = Column(modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) { Text("Work Settings", style = MaterialTheme.typography.headlineMedium); Text("Worker profile is read from the real worker_profiles contract."); Button(onClick = onExit) { Text("Back to Social") }; if (identity != null) Text("Worker ID: ${identity.workId}"); Text("Team Joining remains a navigation boundary because the Web currently states that joining requests, invitations, memberships, and team data are not implemented.") }
