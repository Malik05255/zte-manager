package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun ZteReferenceTopBar(
    connected: Boolean,
    sectionTitle: String,
    onMenu: () -> Unit,
    onSettings: () -> Unit
) {
    Surface(color = ZteBg) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .background(
                    Brush.horizontalGradient(
                        listOf(
                            Color(0xFFF4FAFF),
                            Color(0xFFFFFFFF),
                            Color(0xFFF2F8FF)
                        )
                    )
                )
                .padding(horizontal = 16.dp, vertical = 10.dp)
        ) {
            val compact = maxWidth < 560.dp

            if (compact) {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        ZteChromeIcon("☰", onMenu)
                        Spacer(Modifier.width(11.dp))
                        ZteBrandBlock(sectionTitle, Modifier.weight(1f))
                        Spacer(Modifier.width(8.dp))
                        ZteChromeIcon("⚙", onSettings)
                    }
                    Spacer(Modifier.height(10.dp))
                    ZteConnectionCard(connected, Modifier.fillMaxWidth())
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    ZteChromeIcon("☰", onMenu)
                    Spacer(Modifier.width(12.dp))
                    ZteBrandBlock(sectionTitle, Modifier.weight(1f))
                    Spacer(Modifier.width(14.dp))
                    ZteConnectionCard(connected, Modifier.widthIn(min = 220.dp, max = 300.dp))
                    Spacer(Modifier.width(10.dp))
                    ZteChromeIcon("●", onSettings)
                    Spacer(Modifier.width(8.dp))
                    ZteChromeIcon("⚙", onSettings)
                }
            }
        }
    }
}

@Composable
private fun ZteBrandBlock(sectionTitle: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("ZTE", color = ZteBlue, fontSize = 28.sp, lineHeight = 31.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.width(6.dp))
            Text("Manager", color = ZteDeepBlue, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.height(2.dp))
        Text(
            if (sectionTitle == "ZTE Manager") "تحكم أكبر … اتصال أقوى" else sectionTitle,
            color = ZteMuted,
            fontSize = 14.sp,
            lineHeight = 19.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun ZteConnectionCard(connected: Boolean, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        color = Color.White.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, Color(0xFFE1ECF8)),
        shadowElevation = 4.dp
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 11.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(28.dp).clip(CircleShape).background(if (connected) ZteSoftBlue else ZteSoftAmber),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(12.dp).clip(CircleShape).background(if (connected) ZteBlue else ZteAmber))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(if (connected) "متصل" else "غير متصل", color = if (connected) ZteBlue else ZteAmber, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text(
                    if (connected) "جهازك يعمل بشكل ممتاز" else "بانتظار الاتصال بالراوتر",
                    color = ZteMuted,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
            }
        }
    }
}

@Composable
private fun ZteChromeIcon(symbol: String, onClick: () -> Unit) {
    Surface(modifier = Modifier.size(48.dp).clickable(onClick = onClick), shape = CircleShape, color = Color.Transparent) {
        Box(contentAlignment = Alignment.Center) {
            Text(symbol, color = ZteDeepBlue, fontSize = 24.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
        }
    }
}

@Composable
internal fun ZteReferenceBottomBar(current: ZteScreen, onNavigate: (ZteScreen) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        color = Color.White,
        border = BorderStroke(1.dp, Color(0xFFE2ECF7)),
        shadowElevation = 10.dp
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 86.dp).padding(horizontal = 6.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            ZteReferenceNavItem(current == ZteScreen.HOME, "⌂", "الرئيسية", { onNavigate(ZteScreen.HOME) }, Modifier.weight(1f))
            ZteReferenceNavItem(current == ZteScreen.NETWORK, "▥", "الشبكة", { onNavigate(ZteScreen.NETWORK) }, Modifier.weight(1f))

            Surface(
                modifier = Modifier.padding(horizontal = 4.dp).size(72.dp).clickable { onNavigate(ZteScreen.HOME) },
                shape = CircleShape,
                color = Color(0xFFF0F7FF),
                border = BorderStroke(2.dp, Color(0xFFBFDFFF)),
                shadowElevation = 7.dp
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text("◉", color = ZteBlue, fontSize = 25.sp, fontWeight = FontWeight.Black)
                    Text("ZTE", color = ZteBlue, fontSize = 14.sp, fontWeight = FontWeight.Black)
                }
            }

            ZteReferenceNavItem(current == ZteScreen.TOOLS, "▦", "الأدوات", { onNavigate(ZteScreen.TOOLS) }, Modifier.weight(1f))
            ZteReferenceNavItem(current == ZteScreen.MORE, "☷", "المزيد", { onNavigate(ZteScreen.MORE) }, Modifier.weight(1f))
        }
    }
}

@Composable
private fun ZteReferenceNavItem(selected: Boolean, symbol: String, label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.heightIn(min = 68.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(22.dp),
        color = if (selected) ZteSoftBlue else Color.Transparent
    ) {
        Column(
            Modifier.padding(horizontal = 4.dp, vertical = 7.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(symbol, color = if (selected) ZteBlue else ZteMuted, fontSize = 23.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(3.dp))
            Text(
                label,
                color = if (selected) ZteBlue else ZteMuted,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}
