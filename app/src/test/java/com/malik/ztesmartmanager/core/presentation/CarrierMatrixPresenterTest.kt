package com.malik.ztesmartmanager.core.presentation

import com.malik.ztesmartmanager.core.model.CarrierCell
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CarrierMatrixPresenterTest {
    @Test
    fun configuredMasksWithoutVerifiedCellsNeverCreateRows() {
        val snapshot = RouterSnapshot(
            raw = mapOf(
                "lte_band_lock" to "3",
                "nr5g_band_lock" to "78",
                "_zte_lte_active_verified" to "false",
                "_zte_nr_active_verified" to "false",
                "_zte_ca_verified" to "false"
            )
        )

        val model = CarrierMatrixPresenter.from(snapshot)

        assertFalse(model.hasVerifiedCarrier)
        assertEquals("LTE CA غير مؤكد", model.caHeadline)
    }

    @Test
    fun verifiedLteCaShowsPrimaryAndSecondaryCarriers() {
        val snapshot = RouterSnapshot(
            caActive = true,
            cells = listOf(
                CarrierCell(CellRole.PRIMARY, "B3", 123, 1300, 20.0),
                CarrierCell(CellRole.SECONDARY, "B1", 321, 100, 15.0),
                CarrierCell(CellRole.SECONDARY, "B28", 222, 9410, 10.0)
            ),
            raw = mapOf(
                "_zte_lte_active_verified" to "true",
                "_zte_nr_active_verified" to "false",
                "_zte_ca_verified" to "true",
                "_zte_ca_active" to "true"
            )
        )

        val model = CarrierMatrixPresenter.from(snapshot)

        assertEquals(3, model.rows.size)
        assertEquals(3, model.lteCarrierCount)
        assertEquals("LTE PCell", model.rows[0].roleLabel)
        assertEquals("LTE SCell 1", model.rows[1].roleLabel)
        assertEquals("LTE SCell 2", model.rows[2].roleLabel)
        assertEquals("LTE CA موثّق • 3 Carriers", model.caHeadline)
    }

    @Test
    fun staleNrCellIsSuppressedWhenNrIsNotVerified() {
        val snapshot = RouterSnapshot(
            cells = listOf(CarrierCell(CellRole.NR, "N78", 777, 630000, 100.0)),
            raw = mapOf("_zte_nr_active_verified" to "false")
        )

        val model = CarrierMatrixPresenter.from(snapshot)

        assertTrue(model.rows.isEmpty())
        assertEquals(0, model.nrCarrierCount)
    }

    @Test
    fun verifiedNrCellAppearsWithNeutralRoleLabel() {
        val snapshot = RouterSnapshot(
            cells = listOf(CarrierCell(CellRole.NR, "N78", 777, 630000, 100.0)),
            raw = mapOf("_zte_nr_active_verified" to "true")
        )

        val model = CarrierMatrixPresenter.from(snapshot)

        assertEquals(1, model.rows.size)
        assertEquals(CarrierMatrixKind.NR, model.rows.single().kind)
        assertEquals("NR Carrier 1", model.rows.single().roleLabel)
        assertEquals(630000, model.rows.single().arfcn)
    }

    @Test
    fun missingCarrierFieldsRemainMissingRatherThanInvented() {
        val snapshot = RouterSnapshot(
            cells = listOf(CarrierCell(CellRole.PRIMARY, "B3", null, null, null)),
            raw = mapOf("_zte_lte_active_verified" to "true")
        )

        val row = CarrierMatrixPresenter.from(snapshot).rows.single()

        assertEquals("B3", row.band)
        assertNull(row.pci)
        assertNull(row.arfcn)
        assertNull(row.bandwidthMhz)
    }

    @Test
    fun secondaryCellIsSuppressedWhenCaIsNotVerifiedActive() {
        val snapshot = RouterSnapshot(
            caActive = false,
            cells = listOf(
                CarrierCell(CellRole.PRIMARY, "B3", 123, 1300, 20.0),
                CarrierCell(CellRole.SECONDARY, "B1", 321, 100, 15.0)
            ),
            raw = mapOf(
                "_zte_lte_active_verified" to "true",
                "_zte_ca_verified" to "false",
                "_zte_ca_active" to "true"
            )
        )

        val model = CarrierMatrixPresenter.from(snapshot)

        assertEquals(1, model.rows.size)
        assertEquals(CarrierMatrixKind.LTE_PRIMARY, model.rows.single().kind)
        assertEquals("LTE CA غير مؤكد", model.caHeadline)
    }
}
