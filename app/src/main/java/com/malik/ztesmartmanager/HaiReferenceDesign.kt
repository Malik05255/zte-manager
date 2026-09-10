package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Visual contract for the approved HAI reference UI.
 *
 * Keep screen-specific code focused on content. Geometry and palette live here so Home, Network,
 * Towers and Tools do not slowly drift into different radii, spacing and surface treatments.
 */
object HaiReferenceDesign {
    val Background = Color(0xFFF7FAFE)
    val Card = Color.White
    val Ink = Color(0xFF0A2C67)
    val Blue = Color(0xFF1478F8)
    val Cyan = Color(0xFF35C7E5)
    val Green = Color(0xFF15C986)
    val GreenInk = Color(0xFF079B64)
    val Purple = Color(0xFFA566F3)
    val Muted = Color(0xFF71809A)
    val Border = Color(0xFFE5ECF5)
    val Soft = Color(0xFFF3F7FC)
    val SoftBlue = Color(0xFFEAF3FF)
    val SoftGreen = Color(0xFFEAFBF4)
    val SoftPurple = Color(0xFFF4EEFF)
    val SoftGold = Color(0xFFFFF4DE)

    val CardRadius: Dp = 18.dp
    val InnerRadius: Dp = 12.dp
    val PillRadius: Dp = 99.dp
    val CardElevation: Dp = 2.dp

    // Approved phone reference geometry at the normalized 393dp design width.
    val HeroHeight: Dp = 160.dp
    val MetricHeight: Dp = 58.dp
    val MiddleRowHeight: Dp = 136.dp
    val TowerRowHeight: Dp = 118.dp
    val BottomRowHeight: Dp = 94.dp
}

@Composable
fun HaiReferenceCard(
    modifier: Modifier = Modifier,
    radius: Dp = HaiReferenceDesign.CardRadius,
    content: @Composable () -> Unit
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(radius),
        colors = CardDefaults.cardColors(containerColor = HaiReferenceDesign.Card),
        elevation = CardDefaults.cardElevation(defaultElevation = HaiReferenceDesign.CardElevation),
        border = BorderStroke(.7.dp, HaiReferenceDesign.Border)
    ) {
        Box(Modifier.fillMaxSize()) { content() }
    }
}
