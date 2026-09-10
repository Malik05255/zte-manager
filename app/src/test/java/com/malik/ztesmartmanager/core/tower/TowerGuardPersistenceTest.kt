package com.malik.ztesmartmanager.core.tower

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TowerGuardPersistenceTest {
    private val target = TowerTarget(
        pci = 100,
        earfcn = 1300,
        band = "B3",
        cellId = 0x12345,
        enodebId = "291"
    )

    private fun state(guardRequested: Boolean = true) = PersistedTowerGuardState(
        routerAddress = "192.168.0.1",
        profileId = "zte-mc801a",
        target = target,
        guardRequested = guardRequested,
        savedAtEpochMs = 123456789L
    )

    @Test
    fun codec_roundTrip_preservesVerifiedRadioIdentityOnly() {
        val decoded = PersistedTowerGuardStateCodec.decode(
            PersistedTowerGuardStateCodec.encode(state())
        )

        assertEquals(state(), decoded)
    }

    @Test
    fun exactRouterLockAndLiveMatch_resumeRequestedGuard() {
        val decision = TowerGuardResumeVerifier.decide(
            saved = state(true),
            currentRouterAddress = "192.168.0.1",
            currentProfileId = "zte-mc801a",
            configuredPci = "100",
            configuredEarfcn = "1300",
            liveMatch = TowerMatch.MATCHED
        )

        assertEquals(TowerGuardResumeKind.RESUME_VERIFIED, decision.kind)
        assertEquals(target, decision.target)
        assertTrue(decision.enableGuard)
        assertFalse(decision.discardPersistedState)
    }

    @Test
    fun exactRouterLockAndLiveMatch_doesNotResumeWhenUserLeftGuardOff() {
        val decision = TowerGuardResumeVerifier.decide(
            saved = state(false),
            currentRouterAddress = "192.168.0.1",
            currentProfileId = "zte-mc801a",
            configuredPci = "100",
            configuredEarfcn = "1300",
            liveMatch = TowerMatch.MATCHED
        )

        assertEquals(TowerGuardResumeKind.TARGET_VERIFIED_GUARD_OFF, decision.kind)
        assertEquals(target, decision.target)
        assertFalse(decision.enableGuard)
    }

    @Test
    fun exactRouterLockButDriftedLiveCell_keepsConfiguredTargetButGuardOff() {
        val decision = TowerGuardResumeVerifier.decide(
            saved = state(true),
            currentRouterAddress = "192.168.0.1",
            currentProfileId = "zte-mc801a",
            configuredPci = "100",
            configuredEarfcn = "1300",
            liveMatch = TowerMatch.DRIFTED
        )

        assertEquals(TowerGuardResumeKind.CONFIGURED_ONLY, decision.kind)
        assertEquals(target, decision.target)
        assertFalse(decision.enableGuard)
        assertFalse(decision.discardPersistedState)
    }

    @Test
    fun changedRouterLock_discardsStaleLocalState() {
        val decision = TowerGuardResumeVerifier.decide(
            saved = state(true),
            currentRouterAddress = "192.168.0.1",
            currentProfileId = "zte-mc801a",
            configuredPci = "101",
            configuredEarfcn = "1300",
            liveMatch = TowerMatch.MATCHED
        )

        assertEquals(TowerGuardResumeKind.STALE, decision.kind)
        assertNull(decision.target)
        assertFalse(decision.enableGuard)
        assertTrue(decision.discardPersistedState)
    }

    @Test
    fun missingReadBack_neverRestoresTargetOrGuard() {
        val decision = TowerGuardResumeVerifier.decide(
            saved = state(true),
            currentRouterAddress = "192.168.0.1",
            currentProfileId = "zte-mc801a",
            configuredPci = null,
            configuredEarfcn = "1300",
            liveMatch = TowerMatch.MATCHED
        )

        assertEquals(TowerGuardResumeKind.CONFIG_UNKNOWN, decision.kind)
        assertNull(decision.target)
        assertFalse(decision.enableGuard)
        assertFalse(decision.discardPersistedState)
    }

    @Test
    fun profileMismatch_discardsBeforeUsingLockEvidence() {
        val decision = TowerGuardResumeVerifier.decide(
            saved = state(true),
            currentRouterAddress = "192.168.0.1",
            currentProfileId = "zte-generic",
            configuredPci = "100",
            configuredEarfcn = "1300",
            liveMatch = TowerMatch.MATCHED
        )

        assertEquals(TowerGuardResumeKind.STALE, decision.kind)
        assertTrue(decision.discardPersistedState)
        assertFalse(decision.enableGuard)
    }

    @Test
    fun nonDecimalConfiguredLock_isUnknownNotGuessed() {
        val decision = TowerGuardResumeVerifier.decide(
            saved = state(true),
            currentRouterAddress = "192.168.0.1",
            currentProfileId = "zte-mc801a",
            configuredPci = "0x64",
            configuredEarfcn = "1300",
            liveMatch = TowerMatch.MATCHED
        )

        assertEquals(TowerGuardResumeKind.CONFIG_UNKNOWN, decision.kind)
        assertFalse(decision.enableGuard)
        assertNull(decision.target)
    }
}
