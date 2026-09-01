package com.rasheed113.worksocial.infrastructure.work

import com.rasheed113.worksocial.domain.work.FinanceHistoryCursors
import com.rasheed113.worksocial.domain.work.FinanceHistoryFilter
import com.rasheed113.worksocial.domain.work.FinanceHistoryPage
import com.rasheed113.worksocial.domain.work.FinanceReceivedCursor
import com.rasheed113.worksocial.domain.work.FinanceReceivedRecord
import com.rasheed113.worksocial.domain.work.FinanceReceivedType
import com.rasheed113.worksocial.domain.work.WorkHistoryCursor
import com.rasheed113.worksocial.domain.work.WorkHistoryEntry
import com.rasheed113.worksocial.domain.work.WorkHistoryPage
import com.rasheed113.worksocial.domain.work.WorkHouseRepository
import com.rasheed113.worksocial.domain.work.WorkerFinanceSummary
import com.rasheed113.worksocial.domain.work.WorkerIdentity
import com.rasheed113.worksocial.domain.work.WorkerWorkTotals
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.math.BigDecimal
import java.util.Calendar

private const val WORKER_PROFILE_COLUMNS = "id, profile_id, work_id, work_description, skills"
private const val WORK_ENTRY_COLUMNS = "id, worker_profile_id, lifecycle_state, item_name, quantity, rate, total, occurred_at"
private const val FINANCE_RECEIVED_COLUMNS = "id, worker_profile_id, entry_type, amount, received_at, created_at, deleted_at"

class SupabaseWorkHouseRepository(
    private val postgrest: Postgrest,
    private val auth: Auth,
) : WorkHouseRepository {
    override suspend fun getWorkerIdentity(profileId: String): WorkerIdentity? {
        if (profileId.isBlank()) return null
        val row = postgrest.from("worker_profiles").select(columns = Columns.list(WORKER_PROFILE_COLUMNS)) {
            filter { eq("profile_id", profileId) }
            limit(1)
        }.decodeList<JsonObject>().firstOrNull() ?: return null
        return WorkerIdentity(
            id = row.string("id"),
            profileId = row.string("profile_id"),
            workId = row.string("work_id"),
            workDescription = row["work_description"]?.jsonPrimitive?.contentOrNull,
            skills = row["skills"]?.toString()?.removePrefix("[")?.removeSuffix("]")?.split(',')?.map { it.trim().trim('"') }?.filter { it.isNotBlank() } ?: emptyList(),
        )
    }

    override suspend fun getWorkerWorkTotals(): WorkerWorkTotals {
        requireAuthenticated()
        val row = postgrest.rpc("get_worker_work_totals", periodBounds()).decodeList<JsonObject>().firstOrNull()
        return WorkerWorkTotals(
            dailyTotal = row?.stringOrZero("daily_total") ?: "0",
            weeklyTotal = row?.stringOrZero("weekly_total") ?: "0",
            monthlyTotal = row?.stringOrZero("monthly_total") ?: "0",
            lifetimeTotal = row?.stringOrZero("lifetime_total") ?: "0",
        )
    }

    override suspend fun getWorkerHistory(limit: Int, cursor: WorkHistoryCursor?): WorkHistoryPage {
        requireAuthenticated()
        val pageSize = limit.coerceIn(1, 100)
        val rows = postgrest.from("work_entries").select(columns = Columns.list(WORK_ENTRY_COLUMNS)) {
            filter {
                eq("lifecycle_state", "active")
                if (cursor != null) {
                    or {
                        lt("occurred_at", cursor.occurredAt)
                        and { eq("occurred_at", cursor.occurredAt); lt("id", cursor.id) }
                    }
                }
            }
            order("occurred_at", Order.DESCENDING)
            order("id", Order.DESCENDING)
            limit((pageSize + 1).toLong())
        }.decodeList<JsonObject>()
        val hasMore = rows.size > pageSize
        val entries = rows.take(pageSize).map(::toWorkHistoryEntry)
        val next = entries.lastOrNull()?.let { WorkHistoryCursor(it.occurredAt, it.id) }
        return WorkHistoryPage(entries, if (hasMore) next else null, hasMore)
    }

    override suspend fun getWorkerFinanceSummary(): WorkerFinanceSummary? {
        requireAuthenticated()
        val row = postgrest.rpc("get_worker_finance_summary").decodeList<JsonObject>().firstOrNull() ?: return null
        return WorkerFinanceSummary(row.stringOrZero("total_earnings"), row.stringOrZero("received"), row.stringOrZero("remaining"))
    }

    override suspend fun getWorkerFinanceHistory(
        profileId: String,
        filter: FinanceHistoryFilter,
        limit: Int,
        cursors: FinanceHistoryCursors,
    ): FinanceHistoryPage {
        requireAuthenticated()
        val workerProfileId = requireWorkerProfile(profileId)
        val pageSize = limit.coerceIn(1, 100)
        val includeEarnings = filter == FinanceHistoryFilter.all || filter == FinanceHistoryFilter.earnings
        val includeReceived = filter == FinanceHistoryFilter.all || filter == FinanceHistoryFilter.payments || filter == FinanceHistoryFilter.advances || filter == FinanceHistoryFilter.received

        val earningsPage = if (includeEarnings) getWorkerHistory(pageSize, cursors.earnings) else WorkHistoryPage(emptyList(), cursors.earnings, false)

        val receivedRows = if (includeReceived) {
            var query = postgrest.from("worker_finance_received").select(columns = Columns.list(FINANCE_RECEIVED_COLUMNS)) {
                filter {
                    eq("worker_profile_id", workerProfileId)
                    exact("deleted_at", null)
                    if (filter == FinanceHistoryFilter.payments) eq("entry_type", "payment")
                    if (filter == FinanceHistoryFilter.advances) eq("entry_type", "advance")
                    cursors.received?.let { cursor ->
                        or {
                            lt("received_at", cursor.receivedAt)
                            and { eq("received_at", cursor.receivedAt); lt("id", cursor.id) }
                        }
                    }
                }
                order("received_at", Order.DESCENDING)
                order("id", Order.DESCENDING)
                limit((pageSize + 1).toLong())
            }
            query.decodeList<JsonObject>()
        } else emptyList()

        val hasMoreReceived = receivedRows.size > pageSize
        val received = receivedRows.take(pageSize).map(::toFinanceReceivedRecord)
        val nextReceived = received.lastOrNull()?.let { FinanceReceivedCursor(it.receivedAt, it.id) }
        return FinanceHistoryPage(
            earnings = earningsPage.entries,
            received = received,
            nextCursors = FinanceHistoryCursors(
                earnings = earningsPage.nextCursor,
                received = if (hasMoreReceived) nextReceived else null,
            ),
            hasMoreEarnings = earningsPage.hasMore,
            hasMoreReceived = hasMoreReceived,
        )
    }

    override suspend fun addFinanceReceived(profileId: String, type: FinanceReceivedType, amount: String) {
        requireAuthenticated()
        val workerProfileId = requireWorkerProfile(profileId)
        postgrest.from("worker_finance_received").insert(buildJsonObject {
            put("worker_profile_id", workerProfileId)
            put("entry_type", type.name)
            put("amount", canonicalAmount(amount))
        })
    }

    override suspend fun editFinanceReceived(profileId: String, id: String, type: FinanceReceivedType, amount: String) {
        requireAuthenticated()
        val workerProfileId = requireWorkerProfile(profileId)
        postgrest.from("worker_finance_received").update(buildJsonObject {
            put("entry_type", type.name)
            put("amount", canonicalAmount(amount))
        }) {
            filter {
                eq("id", id)
                eq("worker_profile_id", workerProfileId)
                exact("deleted_at", null)
            }
        }
    }

    override suspend fun softDeleteFinanceReceived(profileId: String, id: String) {
        requireAuthenticated()
        val workerProfileId = requireWorkerProfile(profileId)
        postgrest.from("worker_finance_received").update(buildJsonObject { put("deleted_at", java.time.Instant.now().toString()) }) {
            filter {
                eq("id", id)
                eq("worker_profile_id", workerProfileId)
                exact("deleted_at", null)
            }
        }
    }

    override suspend fun restoreFinanceReceived(profileId: String, id: String) {
        requireAuthenticated()
        val workerProfileId = requireWorkerProfile(profileId)
        postgrest.from("worker_finance_received").update(buildJsonObject { put("deleted_at", null as String?) }) {
            filter {
                eq("id", id)
                eq("worker_profile_id", workerProfileId)
                filterNot("deleted_at", FilterOperator.IS, null)
            }
        }
    }

    private suspend fun requireWorkerProfile(profileId: String): String {
        check(profileId.isNotBlank()) { "Authenticated profile is unavailable." }
        val currentUserId = auth.currentSessionOrNull()?.user?.id
        check(currentUserId == profileId) { "The requested worker profile is not the active account." }
        return getWorkerIdentity(profileId)?.id ?: error("Set up Work Identity before using Finance.")
    }

    private fun requireAuthenticated() {
        checkNotNull(auth.currentSessionOrNull()?.user?.id) { "Your Work Social session is no longer active. Please sign in again." }
    }

    private fun periodBounds(): JsonObject {
        val now = Calendar.getInstance()
        fun startOfDay(source: Calendar) = (source.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        fun iso(calendar: Calendar) = java.time.Instant.ofEpochMilli(calendar.timeInMillis).toString()
        val dayStart = startOfDay(now)
        val dayEnd = (dayStart.clone() as Calendar).apply { add(Calendar.DATE, 1) }
        val day = dayStart.get(Calendar.DAY_OF_WEEK)
        val mondayOffset = if (day == Calendar.SUNDAY) -6 else Calendar.MONDAY - day
        val weekStart = (dayStart.clone() as Calendar).apply { add(Calendar.DATE, mondayOffset) }
        val weekEnd = (weekStart.clone() as Calendar).apply { add(Calendar.DATE, 7) }
        val monthStart = (dayStart.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }
        val monthEnd = (monthStart.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
        return buildJsonObject {
            put("p_day_start", iso(dayStart)); put("p_day_end", iso(dayEnd))
            put("p_week_start", iso(weekStart)); put("p_week_end", iso(weekEnd))
            put("p_month_start", iso(monthStart)); put("p_month_end", iso(monthEnd))
        }
    }

    private fun toWorkHistoryEntry(row: JsonObject) = WorkHistoryEntry(
        id = row.string("id"), workerProfileId = row.string("worker_profile_id"), itemName = row.string("item_name"),
        quantity = row.string("quantity"), rate = row.string("rate"), total = row.string("total"),
        occurredAt = row.string("occurred_at"), lifecycleState = row.string("lifecycle_state"),
    )

    private fun toFinanceReceivedRecord(row: JsonObject) = FinanceReceivedRecord(
        id = row.string("id"), workerProfileId = row.string("worker_profile_id"),
        entryType = when (row.string("entry_type")) {
            "advance" -> FinanceReceivedType.advance
            else -> FinanceReceivedType.payment
        },
        amount = canonicalAmount(row.stringOrZero("amount")),
        receivedAt = row.string("received_at"), createdAt = row.string("created_at"),
        deletedAt = row["deleted_at"]?.jsonPrimitive?.contentOrNull,
    )

    private fun canonicalAmount(value: String): String = runCatching { BigDecimal(value.trim()).setScale(4).stripTrailingZeros().toPlainString() }.getOrElse { value.trim() }
    private fun JsonObject.string(key: String) = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
    private fun JsonObject.stringOrZero(key: String) = this[key]?.jsonPrimitive?.contentOrNull ?: "0"
}
