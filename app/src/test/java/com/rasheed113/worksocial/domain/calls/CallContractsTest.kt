package com.rasheed113.worksocial.domain.calls

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class CallContractsTest {
    @Test fun callKindsMatchBackendSignalValues() {
        assertEquals(CallKind.AUDIO, CallKind.valueOf("AUDIO"))
        assertEquals(CallKind.VIDEO, CallKind.valueOf("VIDEO"))
    }

    @Test fun signalTypesCoverWebsiteContract() {
        assertEquals(setOf("OFFER", "ANSWER", "ICE", "HANGUP", "REJECT"), SignalType.entries.map { it.name }.toSet())
    }

    @Test fun callStatesCoverTerminalAndActiveStates() {
        assertEquals(setOf("RINGING", "CONNECTING", "CONNECTED", "REJECTED", "MISSED", "ENDED", "FAILED"), CallState.entries.map { it.name }.toSet())
    }

    @Test fun candidateDataIsValueBasedAndDistinctWhenContentsDiffer() {
        val first = IceCandidateData("0", 0, "candidate:a")
        val second = IceCandidateData("0", 0, "candidate:b")
        assertNotEquals(first, second)
    }
}
