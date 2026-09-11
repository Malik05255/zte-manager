package com.malik.ztesmartmanager

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.tower.NearbyCell
import com.malik.ztesmartmanager.core.tower.TowerGuardStatus
import com.malik.ztesmartmanager.core.tower.TowerTarget

@Composable
internal fun GlassNetworkScreen(
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    selectedLte: Set<Int>,
    selectedNr: Set<Int>,
    controlBusy: Boolean,
    scanBusy: Boolean,
    nearbyCells: List<NearbyCell>,
    towerTarget: TowerTarget?,
    towerGuardEnabled: Boolean,
    towerGuardStatus: TowerGuardStatus?,
    onMode: (String) -> Unit,
    onLteToggle: (Int) -> Unit,
    onNrToggle: (Int) -> Unit,
    onApplyLte: () -> Unit,
    onApplyNr: () -> Unit,
    onScan: () -> Unit,
    onLockCurrent: () -> Unit,
    onLockNearby: (NearbyCell) -> Unit,
    onClear: () -> Unit,
    onGuardChange: (Boolean) -> Unit
) {
    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { GlassMapCard(snapshot, onOpenNetwork = {}) }
        item { GlassNetworkModeCard(snapshot, controlBusy, onMode) }
        item {
            GlassBandControlCard(
                capabilities, selectedLte, selectedNr, controlBusy,
                onLteToggle, onNrToggle, onApplyLte, onApplyNr
            )
        }
        item {
            GlassTowerControlCard(
                scanBusy, controlBusy, towerTarget, towerGuardEnabled, towerGuardStatus,
                onScan, onLockCurrent, onClear, onGuardChange
            )
        }
        if (nearbyCells.isNotEmpty()) {
            item { Text("الخلايا القريبة", color = GlassInk, fontSize = 14.sp, fontWeight = FontWeight.Black) }
            items(nearbyCells) { cell ->
                GlassNearbyCellCard(cell, controlBusy) { onLockNearby(cell) }
            }
        }
        item { GlassTechnicalReadings(snapshot) }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
