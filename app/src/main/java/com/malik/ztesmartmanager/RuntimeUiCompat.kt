package com.malik.ztesmartmanager

import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot

/**
 * Small runtime compatibility surface retained after removing all superseded dashboards.
 * It contains no screen/UI layout: only the application color scheme used by FinalMainActivity
 * and band-selection helpers used by the verified write/read-back flow.
 */
val PremiumBackground = Color(0xFFF3F6FA)

val PremiumLightColors = lightColorScheme(
    primary = Color(0xFF2D7CFF),
    onPrimary = Color.White,
    secondary = Color(0xFF10C7D5),
    background = PremiumBackground,
    surface = Color.White,
    surfaceVariant = Color(0xFFEAF0F6),
    onSurface = Color(0xFF071525),
    onSurfaceVariant = Color(0xFF728096),
    outline = Color(0xFFE5EAF0)
)

fun premiumCurrentLteBands(snapshot: RouterSnapshot): Set<Int> = buildSet {
    runtimeExtractBand(snapshot.lteBand)?.let(::add)
    snapshot.cells.filter { it.role != CellRole.NR }.forEach { runtimeExtractBand(it.band)?.let(::add) }
}

fun premiumCurrentNrBands(snapshot: RouterSnapshot): Set<Int> = buildSet {
    runtimeExtractBand(snapshot.nrBand)?.let(::add)
    snapshot.cells.filter { it.role == CellRole.NR }.forEach { runtimeExtractBand(it.band)?.let(::add) }
}

fun premiumToggleBand(current: Set<Int>, band: Int): Set<Int> =
    if (band in current) current - band else current + band

private fun runtimeExtractBand(value: String?): Int? =
    Regex("\\d+").find(value.orEmpty())?.value?.toIntOrNull()
