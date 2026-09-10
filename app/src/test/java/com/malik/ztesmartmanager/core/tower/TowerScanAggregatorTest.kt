package com.malik.ztesmartmanager.core.tower

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TowerScanAggregatorTest {
    @Test
    fun sameFrequencyDifferentPci_remainSeparateCells() {
        val samples = listOf(
            listOf(
                cell(pci = 10, arfcn = 1300, rsrp = -90.0),
                cell(pci = 11, arfcn = 1300, rsrp = -92.0)
            ),
            listOf(
                cell(pci = 10, arfcn = 1300, rsrp = -91.0),
                cell(pci = 11, arfcn = 1300, rsrp = -93.0)
            )
        )

        val ranked = TowerScanAggregator.rank(samples)

        assertEquals(2, ranked.size)
        assertEquals(setOf(10, 11), ranked.mapNotNull { it.cell.pci }.toSet())
        assertTrue(ranked.all { it.cell.arfcn == 1300 })
    }

    @Test
    fun medianSignal_rejectsSingleExtremeOutlier() {
        val samples = listOf(-90.0, -91.0, -89.0, -90.0, -130.0).map { rsrp ->
            listOf(cell(pci = 100, arfcn = 1850, rsrp = rsrp))
        }

        val ranked = TowerScanAggregator.rank(samples).single()

        assertEquals(-90.0, ranked.medianRsrp ?: 0.0, 0.001)
        assertEquals(100, ranked.presencePercent)
        assertEquals(CellConfidence.HIGH, ranked.confidence)
    }

    @Test
    fun stableRepeatedCell_outranksOneOffStrongCell() {
        val stable = cell(pci = 20, arfcn = 1650, rsrp = -92.0, rsrq = -10.0, sinr = 16.0)
        val oneOff = cell(pci = 21, arfcn = 1650, rsrp = -78.0, rsrq = -8.0, sinr = 22.0)
        val samples = listOf(
            listOf(stable, oneOff),
            listOf(stable.copy(rsrp = -91.0)),
            listOf(stable.copy(rsrp = -93.0)),
            listOf(stable.copy(rsrp = -92.0)),
            listOf(stable.copy(rsrp = -91.5))
        )

        val ranked = TowerScanAggregator.rank(samples)

        assertEquals(20, ranked.first().cell.pci)
        assertEquals(100, ranked.first().presencePercent)
        assertEquals(20, ranked.last().presencePercent)
    }

    @Test
    fun missingRfMetrics_doesNotInventEvidenceScore() {
        val samples = List(5) { listOf(cell(pci = 30, arfcn = 900, rsrp = null, rsrq = null, sinr = null)) }

        val ranked = TowerScanAggregator.rank(samples).single()

        assertNull(ranked.evidenceScore)
        assertEquals(CellConfidence.LOW, ranked.confidence)
        assertEquals(100, ranked.presencePercent)
    }

    private fun cell(
        pci: Int,
        arfcn: Int,
        rsrp: Double?,
        rsrq: Double? = null,
        sinr: Double? = null
    ) = NearbyCell(
        rat = "LTE",
        band = "B3",
        pci = pci,
        arfcn = arfcn,
        rsrp = rsrp,
        rsrq = rsrq,
        sinr = sinr
    )
}
