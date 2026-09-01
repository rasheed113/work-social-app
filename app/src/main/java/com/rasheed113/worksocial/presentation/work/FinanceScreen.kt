package com.rasheed113.worksocial.presentation.work

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.rasheed113.worksocial.domain.work.*
import java.math.BigDecimal
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun FinanceScreen(viewModel: FinanceViewModel, userId: String, onBack: () -> Unit) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    LaunchedEffect(userId) { viewModel.load(userId) }
    var editor by remember { mutableStateOf<FinanceReceivedRecord?>(null) }
    var addOpen by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<FinanceReceivedRecord?>(null) }
    var details by remember { mutableStateOf<FinanceListEntry?>(null) }
    var lastDeletedId by remember { mutableStateOf<String?>(null) }

    when (val current = state) {
        FinanceState.Loading -> Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { CircularProgressIndicator() }
        is FinanceState.Error -> Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Finance unavailable", style = MaterialTheme.typography.headlineSmall)
            Text(current.message, color = MaterialTheme.colorScheme.error)
            Button(onClick = { viewModel.load(userId, FinanceHistoryFilter.all) }) { Text("Retry") }
            OutlinedButton(onClick = onBack) { Text("Back to Work House") }
        }
        is FinanceState.Success -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column { Text("Finance", style = MaterialTheme.typography.headlineMedium); Text("Real Worker Finance", color = MaterialTheme.colorScheme.onSurfaceVariant) }
                    Button(onClick = { addOpen = true }) { Text("+ Add") }
                }
            }
            item {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SummaryCard("Total earnings", current.summary.totalEarnings)
                    SummaryCard("Received", current.summary.received)
                    SummaryCard("Remaining", current.summary.remaining)
                }
            }
            item { LazyFilterRow(current.filter, viewModel::setFilter) }
            current.notice?.let { notice ->
                item {
                    Card(Modifier.fillMaxWidth()) { Row(Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(notice, Modifier.weight(1f))
                        lastDeletedId?.let { id -> TextButton(onClick = { viewModel.restore(id); lastDeletedId = null }) { Text("Restore") } }
                        TextButton(onClick = viewModel::clearNotice) { Text("Dismiss") }
                    } }
                }
            }
            if (current.entries.isEmpty()) item {
                Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("No finance records yet")
                    Text("Real earnings and received amounts will appear here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } }
            } else {
                items(current.entries, key = { it.id }) { entry -> FinanceRow(entry, { details = it }, { editor = it }, { deleteTarget = it }) }
                if (current.hasMore) item { Button(onClick = viewModel::loadMore, enabled = !current.loadingMore, modifier = Modifier.fillMaxWidth()) { if (current.loadingMore) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Load More") } }
            }
            item { OutlinedButton(onClick = onBack, Modifier.fillMaxWidth()) { Text("Back to Work House") } }
        }
    }

    if (addOpen || editor != null) ReceivedEditorDialog(
        initial = editor,
        saving = (state as? FinanceState.Success)?.saving == true,
        onDismiss = { addOpen = false; editor = null },
        onSave = { type, amount -> if (editor == null) viewModel.add(type, amount) else viewModel.edit(editor!!.id, type, amount); addOpen = false; editor = null },
    )
    deleteTarget?.let { record -> AlertDialog(
        onDismissRequest = { deleteTarget = null },
        title = { Text("Delete received amount?") },
        text = { Text("This is a reversible soft-delete. The record will be hidden from active Finance history and can be restored.") },
        confirmButton = { TextButton(onClick = { viewModel.delete(record.id); lastDeletedId = record.id; deleteTarget = null }) { Text("Delete") } },
        dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } },
    ) }
    details?.let { entry -> AlertDialog(onDismissRequest = { details = null }, title = { Text("Entry details") }, text = { EntryDetails(entry) }, confirmButton = { TextButton(onClick = { details = null }) { Text("Close") } }) }
}

@Composable private fun RowScope.SummaryCard(label: String, value: String) = Card(Modifier.weight(1f)) { Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) { Text(label, style = MaterialTheme.typography.labelSmall); Text("Rs. ${formatAmount(value)}", style = MaterialTheme.typography.titleMedium) } }

@Composable private fun LazyFilterRow(selected: FinanceHistoryFilter, onSelect: (FinanceHistoryFilter) -> Unit) {
    Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        listOf(FinanceHistoryFilter.all, FinanceHistoryFilter.earnings, FinanceHistoryFilter.payments, FinanceHistoryFilter.advances, FinanceHistoryFilter.received).forEach { filter ->
            FilterChip(selected = selected == filter, onClick = { onSelect(filter) }, label = { Text(filterLabel(filter)) })
        }
    }
}

@Composable private fun FinanceRow(entry: FinanceListEntry, onDetails: (FinanceListEntry) -> Unit, onEdit: (FinanceReceivedRecord) -> Unit, onDelete: (FinanceReceivedRecord) -> Unit) {
    Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(when (entry) { is FinanceListEntry.Earning -> entry.entry.itemName; is FinanceListEntry.Received -> if (entry.record.entryType == FinanceReceivedType.payment) "Payment received" else "Advance received" }, style = MaterialTheme.typography.titleMedium)
            Text("Rs. ${formatAmount(entry.amount)}", style = MaterialTheme.typography.titleMedium)
        }
        Text(dateLabel(entry.occurredAt), color = MaterialTheme.colorScheme.onSurfaceVariant)
        HorizontalDivider()
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = { onDetails(entry) }) { Text("Details") }
            if (entry is FinanceListEntry.Received) { TextButton(onClick = { onEdit(entry.record) }) { Text("Edit") }; TextButton(onClick = { onDelete(entry.record) }) { Text("Delete") } }
        }
    } }
}

@Composable private fun EntryDetails(entry: FinanceListEntry) = Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
    when (entry) {
        is FinanceListEntry.Earning -> { Text("Type: Earnings"); Text("Item: ${entry.entry.itemName}"); Text("Quantity: ${entry.entry.quantity}"); Text("Rate: ${entry.entry.rate}"); Text("Total: Rs. ${formatAmount(entry.entry.total)}"); Text("Occurred: ${dateLabel(entry.entry.occurredAt)}"); Text("Lifecycle: ${entry.entry.lifecycleState}") }
        is FinanceListEntry.Received -> { Text("Type: ${if (entry.record.entryType == FinanceReceivedType.payment) "Payment" else "Advance"}"); Text("Amount: Rs. ${formatAmount(entry.record.amount)}"); Text("Received: ${dateLabel(entry.record.receivedAt)}"); Text("Created: ${dateLabel(entry.record.createdAt)}") }
    }
}

@Composable private fun ReceivedEditorDialog(initial: FinanceReceivedRecord?, saving: Boolean, onDismiss: () -> Unit, onSave: (FinanceReceivedType, String) -> Unit) {
    var type by remember(initial) { mutableStateOf(initial?.entryType ?: FinanceReceivedType.payment) }
    var amount by remember(initial) { mutableStateOf(initial?.amount ?: "") }
    var error by remember(initial) { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add received amount" else "Edit received amount") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { FilterChip(type == FinanceReceivedType.payment, { type = FinanceReceivedType.payment }, label = { Text("Payment") }); FilterChip(type == FinanceReceivedType.advance, { type = FinanceReceivedType.advance }, label = { Text("Advance") }) }
            OutlinedTextField(value = amount, onValueChange = { amount = it }, label = { Text("Amount") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth(), enabled = !saving)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        } },
        confirmButton = { TextButton(enabled = !saving, onClick = { val normalized = amount.trim(); val valid = Regex("^(?:0|[1-9]\\d*)(?:\\.\\d{1,4})?$").matches(normalized) && runCatching { BigDecimal(normalized) > BigDecimal.ZERO }.getOrDefault(false); if (!valid) error = "Enter an amount greater than zero, with up to 4 decimal places." else { error = null; onSave(type, normalized) } }) { if (saving) CircularProgressIndicator(strokeWidth = 2.dp) else Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !saving) { Text("Cancel") } },
    )
}

private fun filterLabel(value: FinanceHistoryFilter) = when (value) { FinanceHistoryFilter.all -> "All"; FinanceHistoryFilter.earnings -> "Earnings"; FinanceHistoryFilter.payments -> "Payments"; FinanceHistoryFilter.advances -> "Advances"; FinanceHistoryFilter.received -> "All received" }
private fun formatAmount(value: String) = runCatching { BigDecimal(value).stripTrailingZeros().toPlainString() }.getOrDefault(value)
private fun dateLabel(value: String) = runCatching { DateTimeFormatter.ofPattern("MMM d, yyyy").withZone(ZoneId.systemDefault()).format(Instant.parse(value)) }.getOrDefault(value)
