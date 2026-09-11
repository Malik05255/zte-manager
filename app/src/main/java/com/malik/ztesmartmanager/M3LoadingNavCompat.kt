package com.malik.ztesmartmanager

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Loading-state overload for the dashboard shell.
 *
 * It reserves exactly the same navigation footprint as the verified dashboard, so the first
 * RouterSnapshot cannot move the page vertically when it arrives.
 */
@Composable
fun <T> M3BottomNav(selected: T, onSelect: (T) -> Unit) {
    @Suppress("UNUSED_VARIABLE")
    val loadingState = selected
    @Suppress("UNUSED_VARIABLE")
    val deferredNavigation = onSelect
    Spacer(Modifier.fillMaxWidth().height(LocalHaiUiMetrics.current.bottomNavHeight))
}
