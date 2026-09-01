package com.rasheed113.worksocial.domain.work

import kotlinx.serialization.Serializable

@Serializable
data class WorkerIdentity(
    val id: String,
    val profileId: String,
    val workId: String,
    val workDescription: String?,
    val skills: List<String>,
)

@Serializable
data class WorkerWorkTotals(
    val dailyTotal: String = "0",
    val weeklyTotal: String = "0",
    val monthlyTotal: String = "0",
    val lifetimeTotal: String = "0",
)

@Serializable
data class WorkHistoryEntry(
    val id: String,
    val workerProfileId: String,
    val itemName: String,
    val quantity: String,
    val rate: String,
    val total: String,
    val occurredAt: String,
    val lifecycleState: String,
)

data class WorkHistoryCursor(val occurredAt: String, val id: String)

data class WorkHistoryPage(
    val entries: List<WorkHistoryEntry>,
    val nextCursor: WorkHistoryCursor?,
    val hasMore: Boolean,
)

@Serializable
data class WorkerFinanceSummary(
    val totalEarnings: String = "0",
    val received: String = "0",
    val remaining: String = "0",
)

interface WorkHouseRepository {
    suspend fun getWorkerIdentity(profileId: String): WorkerIdentity?
    suspend fun getWorkerWorkTotals(): WorkerWorkTotals
    suspend fun getWorkerHistory(limit: Int, cursor: WorkHistoryCursor? = null): WorkHistoryPage
    suspend fun getWorkerFinanceSummary(): WorkerFinanceSummary?
}
