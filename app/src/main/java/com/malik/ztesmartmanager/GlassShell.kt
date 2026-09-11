package com.malik.ztesmartmanager

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal enum class GlassScreen { HOME, NETWORK, TOOLS, MORE }

@Composable
internal fun GlassTopNavigationBar(connected: Boolean, title: String) {
    Row(
        modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(title, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold, color = GlassDeepBlue)
            Text("تحكم أوضح • اتصال أقوى", fontSize = 10.sp, color = GlassMuted)
        }
        Row(
            Modifier.clip(RoundedCornerShape(50))
                .background(if (connected) Color(0xFFE8F8F2) else Color(0xFFFFECEE))
                .padding(horizontal = 11.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (connected) GlassMint else GlassRed))
            Spacer(Modifier.width(6.dp))
            Text(if (connected) "متصل" else "غير متصل", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = GlassInk)
        }
    }
}

@Composable
internal fun GlassBottomNavigationBar(screen: GlassScreen, onSelect: (GlassScreen) -> Unit) {
    Surface(color = Color.White, shadowElevation = 10.dp) {
        Row(
            Modifier.navigationBarsPadding().fillMaxWidth().padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassNavItem("الرئيسية", screen == GlassScreen.HOME, Modifier.weight(1f)) { onSelect(GlassScreen.HOME) }
            GlassNavItem("الشبكة", screen == GlassScreen.NETWORK, Modifier.weight(1f)) { onSelect(GlassScreen.NETWORK) }
            Box(Modifier.weight(0.9f), contentAlignment = Alignment.Center) {
                Surface(
                    modifier = Modifier.size(50.dp).clickable { onSelect(GlassScreen.HOME) },
                    shape = CircleShape,
                    color = Color(0xFFE7F1FF)
                ) { Box(contentAlignment = Alignment.Center) { Text("ZTE", color = GlassBlue, fontWeight = FontWeight.Black, fontSize = 12.sp) } }
            }
            GlassNavItem("الأدوات", screen == GlassScreen.TOOLS, Modifier.weight(1f)) { onSelect(GlassScreen.TOOLS) }
            GlassNavItem("المزيد", screen == GlassScreen.MORE, Modifier.weight(1f)) { onSelect(GlassScreen.MORE) }
        }
    }
}

@Composable
private fun GlassNavItem(label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
            .background(if (selected) Color(0xFFEAF3FF) else Color.Transparent)
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            color = if (selected) GlassBlue else GlassMuted
        )
    }
}
