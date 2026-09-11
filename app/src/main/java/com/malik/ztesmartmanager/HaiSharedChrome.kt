package com.malik.ztesmartmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private enum class ChromeGlyph { SETTINGS, SEARCH, MENU, HOME, NETWORK, TOOLS, LOGS, MORE }

/** Shared production header. Geometry intentionally mirrors the approved reference on every page. */
@Composable
fun HaiSharedHeader(
    connected: Boolean,
    onDisconnect: () -> Unit,
    onMenu: () -> Unit,
    onSettings: () -> Unit = {},
    onSearch: () -> Unit = {}
) {
    val ui = LocalHaiUiMetrics.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(ui.headerHeight)
                .padding(horizontal = ui.pagePadding),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChromeCircle(ChromeGlyph.SETTINGS, onSettings)
                ChromeCircle(ChromeGlyph.SEARCH, onSearch)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ZTE Smart HAI",
                    color = HaiReferenceDesign.Ink,
                    fontSize = 22.sp,
                    lineHeight = 23.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    text = "إدارة شبكتك ... بكل سهولة",
                    color = HaiReferenceDesign.Muted,
                    fontSize = 10.sp,
                    lineHeight = 12.sp,
                    maxLines = 1
                )
            }

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .height(36.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (connected) HaiReferenceDesign.SoftGreen else Color(0xFFFFEEEE))
                        .clickable(enabled = connected, onClick = onDisconnect)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier.size(7.dp).background(
                            if (connected) HaiReferenceDesign.Green else Color(0xFFE15363),
                            CircleShape
                        )
                    )
                    Text(
                        text = if (connected) "متصل" else "غير متصل",
                        color = HaiReferenceDesign.Ink,
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(7.dp))
                Box(
                    modifier = Modifier.size(ui.chromeButtonSize).clickable(onClick = onMenu),
                    contentAlignment = Alignment.Center
                ) {
                    ChromeGlyphView(ChromeGlyph.MENU, HaiReferenceDesign.Ink, Modifier.size(25.dp))
                }
            }
        }
    }
}

@Composable
private fun ChromeCircle(glyph: ChromeGlyph, onClick: () -> Unit) {
    val ui = LocalHaiUiMetrics.current
    Box(
        modifier = Modifier
            .size(ui.chromeButtonSize)
            .clip(CircleShape)
            .background(HaiReferenceDesign.Soft)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        ChromeGlyphView(glyph, HaiReferenceDesign.Ink, Modifier.size(19.dp))
    }
}

/** Bottom navigation from the approved reference: المزيد، السجلات، الأدوات، الشبكة، الرئيسية. */
@Composable
fun HaiSharedBottomNav(
    selected: String,
    onHome: () -> Unit,
    onNetwork: () -> Unit,
    onTools: () -> Unit,
    onLogs: () -> Unit,
    onMore: () -> Unit
) {
    val ui = LocalHaiUiMetrics.current
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(ui.bottomNavHeight),
            color = Color.White,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(topStart = ui.cardRadius, topEnd = ui.cardRadius)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChromeNavItem(ChromeGlyph.MORE, "المزيد", selected == "more", Modifier.weight(1f), onMore)
                ChromeNavItem(ChromeGlyph.LOGS, "السجلات", selected == "logs", Modifier.weight(1f), onLogs)
                ChromeNavItem(ChromeGlyph.TOOLS, "الأدوات", selected == "tools", Modifier.weight(1f), onTools)
                ChromeNavItem(ChromeGlyph.NETWORK, "الشبكة", selected == "network", Modifier.weight(1f), onNetwork)
                ChromeNavItem(ChromeGlyph.HOME, "الرئيسية", selected == "home", Modifier.weight(1f), onHome)
            }
        }
    }
}

@Composable
private fun ChromeNavItem(
    glyph: ChromeGlyph,
    title: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    val color = if (selected) HaiReferenceDesign.Blue else Color(0xFF667590)
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ChromeGlyphView(glyph, color, Modifier.size(24.dp))
        Spacer(Modifier.height(3.dp))
        Text(
            text = title,
            color = color,
            fontSize = 9.2.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            textAlign = TextAlign.Center,
            maxLines = 1
        )
    }
}

@Composable
private fun ChromeGlyphView(glyph: ChromeGlyph, color: Color, modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val stroke = (w * .10f).coerceAtLeast(1.6f)
        when (glyph) {
            ChromeGlyph.SEARCH -> {
                drawCircle(color, radius = w * .28f, center = Offset(w * .43f, h * .42f), style = Stroke(stroke))
                drawLine(color, Offset(w * .62f, h * .61f), Offset(w * .84f, h * .84f), stroke, StrokeCap.Round)
            }
            ChromeGlyph.SETTINGS -> {
                drawCircle(color, radius = w * .18f, center = Offset(w / 2f, h / 2f), style = Stroke(stroke))
                repeat(8) { i ->
                    val a = Math.toRadians((i * 45.0))
                    val x1 = w / 2f + kotlin.math.cos(a).toFloat() * w * .30f
                    val y1 = h / 2f + kotlin.math.sin(a).toFloat() * h * .30f
                    val x2 = w / 2f + kotlin.math.cos(a).toFloat() * w * .43f
                    val y2 = h / 2f + kotlin.math.sin(a).toFloat() * h * .43f
                    drawLine(color, Offset(x1, y1), Offset(x2, y2), stroke, StrokeCap.Round)
                }
            }
            ChromeGlyph.MENU, ChromeGlyph.MORE -> {
                listOf(.27f, .50f, .73f).forEach { y ->
                    drawLine(color, Offset(w * .14f, h * y), Offset(w * .86f, h * y), stroke, StrokeCap.Round)
                }
            }
            ChromeGlyph.HOME -> {
                val roof = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * .16f, h * .47f); lineTo(w * .50f, h * .17f); lineTo(w * .84f, h * .47f)
                }
                drawPath(roof, color, style = Stroke(stroke, cap = StrokeCap.Round))
                drawRoundRect(
                    color = color,
                    topLeft = Offset(w * .24f, h * .43f),
                    size = Size(w * .52f, h * .42f),
                    cornerRadius = CornerRadius(w * .05f),
                    style = Stroke(stroke)
                )
            }
            ChromeGlyph.NETWORK -> {
                val heights = listOf(.28f, .46f, .68f, .92f)
                val bw = w * .13f
                heights.forEachIndexed { i, factor ->
                    val bh = h * factor
                    drawRoundRect(
                        color,
                        Offset(w * (.08f + i * .23f), h - bh),
                        Size(bw, bh),
                        CornerRadius(bw / 2f)
                    )
                }
            }
            ChromeGlyph.LOGS -> {
                drawRoundRect(color, Offset(w*.20f,h*.14f), Size(w*.60f,h*.72f), CornerRadius(w*.05f), style=Stroke(stroke))
                listOf(.34f,.50f,.66f).forEach { y -> drawLine(color, Offset(w*.31f,h*y), Offset(w*.69f,h*y), stroke*.72f, StrokeCap.Round) }
            }
            ChromeGlyph.TOOLS -> {
                drawLine(color, Offset(w*.20f,h*.20f), Offset(w*.80f,h*.80f), stroke, StrokeCap.Round)
                drawLine(color, Offset(w*.78f,h*.18f), Offset(w*.20f,h*.78f), stroke, StrokeCap.Round)
                drawCircle(color, w*.10f, Offset(w*.20f,h*.20f), style=Stroke(stroke*.75f))
                drawCircle(color, w*.10f, Offset(w*.80f,h*.80f), style=Stroke(stroke*.75f))
            }
        }
    }
}
