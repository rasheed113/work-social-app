package com.rasheed113.worksocial.domain.work

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FinanceReceivedRecord(
    val id: String,
    @SerialName("worker_profile_id") val workerProfileId: String,
    @SerialName("entry_type") val entryType: FinanceReceivedType,
    val amount: String,
    @SerialName("received_at") val receivedAt: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("deleted_at") val deletedAt: String?,
)

@Serializable
enum class FinanceReceivedType { payment, advance }

enum class FinanceHistoryFilter { all, earnings, payments, advances, received }

data class FinanceReceivedCursor(val receivedAt: String, val id: String)

data class FinanceHistoryCursors(
    val earnings: WorkHistoryCursor? = null,
    val received: FinanceReceivedCursor? = null,
)

data class FinanceHistoryPage(
    val earnings: List<WorkHistoryEntry>,
    val received: List<FinanceReceivedRecord>,
    val nextCursors: FinanceHistoryCursors,
    val hasMoreEarnings: Boolean,
    val hasMoreReceived: Boolean,
)

sealed interface FinanceListEntry {
    val id: String
    val amount: String
    val occurredAt: String
    data class Earning(val entry: WorkHistoryEntry) : FinanceListEntry {
        override val id = "earning:${entry.id}"
        override val amount = entry.total
        override val occurredAt = entry.occurredAt
    }
    data class Received(val record: FinanceReceivedRecord) : FinanceListEntry {
        override val id = "received:${record.id}"
        override val amount = record.amount
        override val occurredAt = record.receivedAt
    }
}
