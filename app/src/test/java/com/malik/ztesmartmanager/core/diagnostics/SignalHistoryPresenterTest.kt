package com.malik.ztesmartmanager.core.diagnostics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SignalHistoryPresenterTest {
    @Test
    fun preservesGapsAndNeverTurnsMissingLteIntoZero() {
        val samples = listOf(
            sample(0, mode = "4G", lte = -95.0),
            sample(1, mode = "4G", lte = null),
            sample(2, mode = "4G", lte = -91.0)
        )

        val model = SignalHistoryPresenter.from(samples)

        assertEquals(3, model.sampleSlots)
        assertEquals(listOf(0, 2), model.lte.points.map { it.slot })
        assertEquals(listOf(-95.0, -91.0), model.lte.points.map { it.dbm })
        assertEquals(SignalTrend.INSUFFICIENT, model.lte.trend)
        assertNull(model.lte.changeDb)
    }

    @Test
    fun nrRequiresVerifiedNrOnTheSameSample() {
        val samples = listOf(
            sample(0, mode = "5G NSA", nr = -80.0, nrVerified = false),
            sample(1, mode = "5G NSA", nr = -82.0, nrVerified = true),
            sample(2, mode = "4G", nr = -70.0, nrVerified = false)
        )

        val model = SignalHistoryPresenter.from(samples)

        assertEquals(1, model.nr.points.size)
        assertEquals(1, model.nr.points.single().slot)
        assertEquals(-82.0, model.nr.points.single().dbm, 0.0)
    }

    @Test
    fun lteIsExcludedFromVerifiedSaAndUnknownModes() {
        val samples = listOf(
            sample(0, mode = "5G SA", lte = -75.0),
            sample(1, mode = null, lte = -70.0),
            sample(2, mode = "5G NSA", lte = -90.0),
            sample(3, mode = "4G", lte = -92.0)
        )

        val model = SignalHistoryPresenter.from(samples)

        assertEquals(listOf(2, 3), model.lte.points.map { it.slot })
    }

    @Test
    fun trendNeedsFiveActualPointsAndUsesLessNegativeAsImprovement() {
        val improving = (0 until 6).map { index ->
            sample(index, mode = "4G", lte = -105.0 + index * 2.0)
        }
        val model = SignalHistoryPresenter.from(improving)

        assertEquals(SignalTrend.IMPROVING, model.lte.trend)
        assertTrue((model.lte.changeDb ?: 0.0) >= 3.0)

        val onlyFour = SignalHistoryPresenter.from(improving.take(4))
        assertEquals(SignalTrend.INSUFFICIENT, onlyFour.lte.trend)
        assertNull(onlyFour.lte.changeDb)
    }

    @Test
    fun capsAtLastThirtySlotsAndPreservesChronology() {
        val samples = (0 until 40).map { index -> sample(index, mode = "4G", lte = -100.0 + index / 10.0) }
        val model = SignalHistoryPresenter.from(samples)

        assertEquals(30, model.sampleSlots)
        assertEquals(30, model.lte.points.size)
        assertEquals(10L, model.lte.points.first().timestampEpochMs)
        assertEquals(39L, model.lte.points.last().timestampEpochMs)
        assertEquals(0, model.lte.points.first().slot)
        assertEquals(29, model.lte.points.last().slot)
    }

    @Test
    fun rejectsImplausibleRsrpInsteadOfPlottingIt() {
        val model = SignalHistoryPresenter.from(
            listOf(
                sample(0, mode = "4G", lte = 0.0),
                sample(1, mode = "4G", lte = -200.0),
                sample(2, mode = "5G NSA", nr = Double.NaN, nrVerified = true)
            )
        )

        assertFalse(model.hasAnySignal)
        assertTrue(model.lte.points.isEmpty())
        assertTrue(model.nr.points.isEmpty())
    }

    private fun sample(
        timestamp: Int,
        mode: String?,
        lte: Double? = null,
        nr: Double? = null,
        nrVerified: Boolean = false
    ) = SafeTelemetrySample(
        timestampEpochMs = timestamp.toLong(),
        networkType = mode,
        lteBand = null,
        nrBand = null,
        lteRsrp = lte,
        lteRsrq = null,
        lteSinr = null,
        nrRsrp = nr,
        nrSinr = null,
        pci = null,
        earfcn = null,
        caActive = false,
        carrierCount = 0,
        nrVerified = nrVerified,
        caVerified = false
    )
}
