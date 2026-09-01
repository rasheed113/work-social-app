package com.rasheed113.worksocial.infrastructure.calls

import com.rasheed113.worksocial.domain.calls.CallKind
import com.rasheed113.worksocial.domain.calls.CallPeer
import com.rasheed113.worksocial.domain.calls.CallRepository
import com.rasheed113.worksocial.domain.calls.CallSignal
import com.rasheed113.worksocial.domain.calls.IceCandidateData
import com.rasheed113.worksocial.domain.calls.OutgoingCallSignal
import com.rasheed113.worksocial.domain.calls.SignalType
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.postgrest.query.Columns
import io.github.jan.supabase.realtime.PostgresAction
import io.github.jan.supabase.realtime.Realtime
import io.github.jan.supabase.realtime.channel
import io.github.jan.supabase.realtime.postgresChangeFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put

@Serializable
private data class ProfileRow(val id: String, val display_name: String? = null, val username: String? = null, val avatar_url: String? = null)

@Serializable
private data class CallSignalRow(val id: String, val call_id: String, val conversation_id: String, val sender_id: String, val recipient_id: String, val kind: String, val signal_type: String, val sdp: JsonObject? = null, val candidate: JsonObject? = null, val created_at: String)

private val rowJson = Json { ignoreUnknownKeys = true }

private fun CallSignalRow.toModel(): CallSignal = CallSignal(
    id, call_id, conversation_id, sender_id, recipient_id,
    if (kind == "video") CallKind.VIDEO else CallKind.AUDIO,
    when (signal_type) { "offer" -> SignalType.OFFER; "answer" -> SignalType.ANSWER; "ice" -> SignalType.ICE; "reject" -> SignalType.REJECT; else -> SignalType.HANGUP },
    sdp?.get("type")?.jsonPrimitive?.content,
    sdp?.get("sdp")?.jsonPrimitive?.content,
    candidate?.let { IceCandidateData(it["sdpMid"]?.jsonPrimitive?.content, it["sdpMLineIndex"]?.jsonPrimitive?.int ?: 0, it["candidate"]?.jsonPrimitive?.content.orEmpty()) },
    created_at
)

class SupabaseCallRepository(private val postgrest: Postgrest, private val auth: Auth, private val realtime: Realtime) : CallRepository {
    private fun requireUser(userId: String) { check(auth.currentSessionOrNull()?.user?.id == userId) { "Your Work Social session is no longer active. Please sign in again." } }

    override suspend fun sendSignal(userId: String, signal: OutgoingCallSignal): Result<Unit> = runCatching {
        requireUser(userId)
        require(signal.callId.isNotBlank() && signal.conversationId.isNotBlank() && signal.recipientId.isNotBlank() && signal.recipientId != userId)
        val payload = buildJsonObject {
            put("call_id", signal.callId); put("conversation_id", signal.conversationId); put("sender_id", userId); put("recipient_id", signal.recipientId)
            put("kind", if (signal.kind == CallKind.VIDEO) "video" else "audio"); put("signal_type", signal.type.name.lowercase())
            signal.sdp?.let { value -> put("sdp", buildJsonObject { signal.sdpType?.let { put("type", it) }; put("sdp", value) }) }
            signal.candidate?.let { value -> put("candidate", buildJsonObject { value.sdpMid?.let { put("sdpMid", it) }; put("sdpMLineIndex", value.sdpMLineIndex); put("candidate", value.candidate) }) }
        }
        postgrest.from("call_signals").insert(payload)
    }

    override fun observeIncomingSignals(userId: String): Flow<CallSignal> = observe(userId, "recipient_id", userId)
    override fun observeCallSignals(userId: String, callId: String): Flow<CallSignal> = observe(userId, "call_id", callId)

    private fun observe(userId: String, filterColumn: String, filterValue: String): Flow<CallSignal> = callbackFlow {
        if (auth.currentSessionOrNull()?.user?.id != userId) { close(IllegalStateException("Your Work Social session is no longer active. Please sign in again.")); return@callbackFlow }
        val channel = realtime.channel("android-call-signals:$userId:$filterColumn:$filterValue")
        val job = launch {
            channel.postgresChangeFlow<PostgresAction.Insert>(schema = "public") {
                table = "call_signals"
                filter(filterColumn, io.github.jan.supabase.realtime.filter.FilterOperator.EQ, filterValue)
            }.collect { action ->
                val row = rowJson.decodeFromJsonElement(CallSignalRow.serializer(), action.record)
                if (row.sender_id != userId && row.recipient_id == userId) trySend(row.toModel()).isSuccess
            }
        }
        runCatching { channel.subscribe(blockUntilSubscribed = true) }.onFailure { close(it) }
        awaitClose { job.cancel(); launch { realtime.removeChannel(channel) } }
    }

    override suspend fun resolvePeer(userId: String, peerId: String): Result<CallPeer> = runCatching {
        requireUser(userId); require(peerId.isNotBlank() && peerId != userId)
        val row = postgrest.from("profiles").select(columns = Columns.list("id,display_name,username,avatar_url")) { filter { eq("id", peerId) } }.decodeList<ProfileRow>().firstOrNull()
            ?: error("The Work Social caller profile could not be loaded.")
        CallPeer(row.id, row.display_name?.takeIf { it.isNotBlank() } ?: row.username ?: "Work Social member", row.username, row.avatar_url)
    }
}
