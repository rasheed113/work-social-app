package com.rasheed113.worksocial.infrastructure.work

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
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

private const val WORKER_PROFILE_COLUMNS = "id, profile_id, work_id, work_description, skills"
private const val WORK_ENTRY_COLUMNS = "id, worker_profile_id, lifecycle_state, item_name, quantity, rate, total, occurred_at"

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
            skills = row["skills"]?.toString()
                ?.removePrefix("[")?.removeSuffix("]")
                ?.split(',')?.map { it.trim().trim('"') }
                ?.filter { it.isNotBlank() } ?: emptyList(),
        )
    }

    override suspend fun getWorkerWorkTotals(): WorkerWorkTotals {
        requireAuthenticated()
        val bounds = periodBounds()
        val row = postgrest.rpc("get_worker_work_totals", bounds).decodeList<JsonObject>().firstOrNull()
        return WorkerWorkTotals(
            dailyTotal = row?.stringOrZero("daily_total"),
            weeklyTotal = row?.stringOrZero("weekly_total"),
            monthlyTotal = row?.stringOrZero("monthly_total"),
            lifetimeTotal = row?.stringOrZero("lifetime_total"),
        )
    }

    override suspend fun getWorkerHistory(limit: Int, cursor: WorkHistoryCursor?): WorkHistoryPage {
        requireAuthenticated()
        val pageSize = limit.coerceIn(1, 100)
        var query = postgrest.from("work_entries").select(columns = Columns.list(WORK_ENTRY_COLUMNS)) {
            filter { eq("lifecycle_state", "active") }
            order("occurred_at", Order.DESCENDING)
            order("id", Order.DESCENDING)
            limit(pageSize + 1)
        }
        if (cursor != null) {
            query = postgrest.from("work_entries").select(columns = Columns.list(WORK_ENTRY_COLUMNS)) {
                filter {
                    eq("lifecycle_state", "active")
                    or("occurred_at.lt.${cursor.occurredAt},and(occurred_at.eq.${cursor.occurredAt},id.lt.${cursor.id})")
                }
                order("occurred_at", Order.DESCENDING)
                order("id", Order.DESCENDING)
                limit(pageSize + 1)
            }
        }
        val rows = query.decodeList<JsonObject>()
        val hasMore = rows.size > pageSize
        val pageRows = rows.take(pageSize)
        val entries = pageRows.map { row ->
            WorkHistoryEntry(
                id = row.string("id"),
                workerProfileId = row.string("worker_profile_id"),
                itemName = row.string("item_name"),
                quantity = row.string("quantity"),
                rate = row.string("rate"),
                total = row.string("total"),
                occurredAt = row.string("occurred_at"),
                lifecycleState = row.string("lifecycle_state"),
            )
        }
        val next = entries.lastOrNull()?.let { WorkHistoryCursor(it.occurredAt, it.id) }
        return WorkHistoryPage(entries, if (hasMore) next else null, hasMore)
    }

    override suspend fun getWorkerFinanceSummary(): WorkerFinanceSummary? {
        requireAuthenticated()
        val row = postgrest.rpc("get_worker_finance_summary").decodeList<JsonObject>().firstOrNull() ?: return null
        return WorkerFinanceSummary(
            totalEarnings = row.stringOrZero("total_earnings"),
            received = row.stringOrZero("received"),
            remaining = row.stringOrZero("remaining"),
        )
    }

    private fun requireAuthenticated() {
        checkNotNull(auth.currentSessionOrNull()?.user?.id) { "Your Work Social session is no longer active. Please sign in again." }
    }

    private fun periodBounds(): kotlinx.serialization.json.JsonObject {
        val now = java.util.Calendar.getInstance()
        fun startOfDay(source: java.util.Calendar) = (source.clone() as java.util.Calendar).apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0); set(java.util.Calendar.MINUTE, 0); set(java.util.Calendar.SECOND, 0); set(java.util.Calendar.MILLISECOND, 0)
        }
        fun iso(calendar: java.util.Calendar) = java.time.Instant.ofEpochMilli(calendar.timeInMillis).toString()
        val dayStart = startOfDay(now)
        val dayEnd = (dayStart.clone() as java.util.Calendar).apply { add(java.util.Calendar.DATE, 1) }
        val day = dayStart.get(java.util.Calendar.DAY_OF_WEEK)
        val mondayOffset = if (day == java.util.Calendar.SUNDAY) -6 else java.util.Calendar.MONDAY - day
        val weekStart = (dayStart.clone() as java.util.Calendar).apply { add(java.util.Calendar.DATE, mondayOffset) }
        val weekEnd = (weekStart.clone() as java.util.Calendar).apply { add(java.util.Calendar.DATE, 7) }
        val monthStart = (dayStart.clone() as java.util.Calendar).apply { set(java.util.Calendar.DAY_OF_MONTH, 1) }
        val monthEnd = (monthStart.clone() as java.util.Calendar).apply { add(java.util.Calendar.MONTH, 1) }
        return kotlinx.serialization.json.buildJsonObject {
            put("p_day_start", iso(dayStart)); put("p_day_end", iso(dayEnd))
            put("p_week_start", iso(weekStart)); put("p_week_end", iso(weekEnd))
            put("p_month_start", iso(monthStart)); put("p_month_end", iso(monthEnd))
        }
    }

    private fun JsonObject.string(key: String) = this[key]?.jsonPrimitive?.contentOrNull.orEmpty()
    private fun JsonObject.stringOrZero(key: String) = this[key]?.jsonPrimitive?.contentOrNull ?: "0"
}
