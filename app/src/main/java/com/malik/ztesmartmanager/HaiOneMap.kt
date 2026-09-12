package com.malik.ztesmartmanager

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.malik.ztesmartmanager.core.model.RouterSnapshot

@Composable
internal fun HaiOneMap(snapshot: RouterSnapshot, modifier: Modifier = Modifier) {
    ZteManagerMap(snapshot, modifier)
}
