package com.malik.ztesmartmanager.core.tower

import com.malik.ztesmartmanager.core.model.RouterSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostLockVerificationAnalyzerTest {
    private val target = TowerTarget(100, 1300, "B3", null, null)

    @Test
    fun threeExactMatchesAndOneUnknown_verifyRadioLock() {
        val samples = listOf(
            sample(100, 1300, cellId = 0x12345, rsrp = -91.0),
            sample(100, 1300, cellId = 0x12345, rsrp = -90.0),
            null,
            sample(100, 1300, cellId = 0x12345, rsrp = -89.0)
        )

        val report = PostLockVerificationAnalyzer.analyze(target, samples)

        assertTrue(report.radioVerified)
        assertEquals(3, report.exactRadioMatches)
        assertEquals(1, report.unknownSamples)
        assertEquals(0, report.driftSamples)
        assertEquals(0x12345L, report.stableCellId)
        assertEquals(-90.0, report.medianRsrp ?: 0.0, 0.001)
    }

    @Test
    fun anyCompleteDriftSample_rejectsOtherwiseMatchingLock() {
        val samples = listOf(
            sample(100, 1300),
            sample(100, 1300),
            sample(101, 1300),
            sample(100, 1300)
        )

        val report = PostLockVerificationAnalyzer.analyze(target, samples)

        assertFalse(report.radioVerified)
        assertEquals(3, report.exactRadioMatches)
        assertEquals(1, report.driftSamples)
    }

    @Test
    fun onlyTwoMatches_isInsufficientEvenWithNoDrift() {
        val samples = listOf(
            sample(100, 1300),
            null,
            RouterSnapshot(networkType = "غير مؤكد"),
            sample(100, 1300)
        )

        val report = PostLockVerificationAnalyzer.analyze(target, samples)

        assertFalse(report.radioVerified)
        assertEquals(2, report.exactRadioMatches)
        assertEquals(2, report.unknownSamples)
    }

    @Test
    fun changingCellId_doesNotInvalidateRadioLockButSuppressesPhysicalIdentity() {
        val samples = listOf(
            sample(100, 1300, cellId = 0x12345),
            sample(100, 1300, cellId = 0x12346),
            sample(100, 1300, cellId = 0x12345),
            sample(100, 1300, cellId = 0x12346)
        )

        val report = PostLockVerificationAnalyzer.analyze(target, samples)

        assertTrue(report.radioVerified)
        assertNull(report.stableCellId)
        assertNull(report.stableEnodebId)
    }

    @Test
    fun oneCellIdObservation_isNotPromotedToStablePhysicalIdentity() {
        val samples = listOf(
            sample(100, 1300, cellId = 0x12345),
            sample(100, 1300, cellId = null),
            sample(100, 1300, cellId = null),
            null
        )

        val report = PostLockVerificationAnalyzer.analyze(target, samples)

        assertTrue(report.radioVerified)
        assertNull(report.stableCellId)
    }

    @Test
    fun medianMetrics_useOnlyExactMatchedSamples() {
        val samples = listOf(
            sample(100, 1300, rsrp = -90.0, rsrq = -10.0, sinr = 12.0),
            sample(100, 1300, rsrp = -92.0, rsrq = -12.0, sinr = 10.0),
            sample(100, 1300, rsrp = -91.0, rsrq = -11.0, sinr = 11.0),
            null
        )

        val report = PostLockVerificationAnalyzer.analyze(target, samples)

        assertTrue(report.radioVerified)
        assertEquals(-91.0, report.medianRsrp ?: 0.0, 0.001)
        assertEquals(-11.0, report.medianRsrq ?: 0.0, 0.001)
        assertEquals(11.0, report.medianSinr ?: 0.0, 0.001)
    }

    private fun sample(
        pci: Int?,
        earfcn: Int?,
        cellId: Long? = null,
        rsrp: Double? = -90.0,
        rsrq: Double? = -10.0,
        sinr: Double? = 15.0
    ) = RouterSnapshot(
        networkType = "4G",
        lteBand = "B3",
        pci = pci,
        earfcn = earfcn,
        cellId = cellId,
        lteRsrp = rsrp,
        lteRsrq = rsrq,
        lteSinr = sinr,
        raw = mapOf("_zte_lte_active_verified" to "true")
    )
}
