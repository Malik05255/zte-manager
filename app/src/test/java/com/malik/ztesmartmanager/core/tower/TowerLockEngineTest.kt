package com.malik.ztesmartmanager.core.tower

import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.protocol.ZteRouterClient
import org.junit.Assert.assertEquals
import org.junit.Test

class TowerLockEngineTest {
    private val engine = TowerLockEngine(ZteRouterClient("192.0.2.1"))

    @Test
    fun exactRadioAndIdentityMatch_isMatched() {
        val target = TowerTarget(100, 1300, "B3", 123456L, "482")
        val snapshot = snapshot(100, 1300, 123456L, "482")
        assertEquals(TowerMatch.MATCHED, engine.compare(target, snapshot))
    }

    @Test
    fun samePciEarfcnButDifferentCellId_isIdentityChanged() {
        val target = TowerTarget(100, 1300, "B3", 123456L, "482")
        val snapshot = snapshot(100, 1300, 123999L, "482")
        assertEquals(TowerMatch.RADIO_MATCH_ID_CHANGED, engine.compare(target, snapshot))
    }

    @Test
    fun differentEarfcn_isDrifted() {
        val target = TowerTarget(100, 1300, "B3", 123456L, "482")
        val snapshot = snapshot(100, 1525, 123456L, "482")
        assertEquals(TowerMatch.DRIFTED, engine.compare(target, snapshot))
    }

    @Test
    fun missingLiveRadioIdentity_isUnknown() {
        val target = TowerTarget(100, 1300, "B3", 123456L, "482")
        assertEquals(TowerMatch.UNKNOWN, engine.compare(target, RouterSnapshot()))
    }

    private fun snapshot(pci: Int, earfcn: Int, cellId: Long, enodeb: String) = RouterSnapshot(
        networkType = "4G",
        lteRsrp = -90.0,
        lteBand = "B3",
        pci = pci,
        earfcn = earfcn,
        cellId = cellId,
        raw = mapOf("enodeb_id" to enodeb, "_zte_lte_active_verified" to "true")
    )
}
