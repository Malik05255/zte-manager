package com.malik.ztesmartmanager

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Loading-state overload for the dashboard shell.
 *
 * Before a verified RouterSnapshot exists there is no safe section data to navigate to. Keeping the
 * navigation footprint reserved prevents the entire screen from jumping vertically when the first
 * verified snapshot arrives, while the fully interactive section navigation remains owned by the
 * M3Section-aware overload in ArabicHaiDashboardV3.kt.
 */
@Composable
fun <T> M3BottomNav(selected: T, onSelect: (T) -> Unit) {
    @Suppress("UNUSED_VARIABLE")
    val loadingState = selected
    @Suppress("UNUSED_VARIABLE")
    val deferredNavigation = onSelect
    Spacer(Modifier.fillMaxWidth().height(82.dp))
}
