package com.malik.ztesmartmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ChromeNavy = Color(0xFF0A2C67)
private val ChromeBlue = Color(0xFF1478F8)
private val ChromeGreen = Color(0xFF15C986)
private val ChromeMuted = Color(0xFF667590)
private val ChromeBorder = Color(0xFFE7EDF5)

@Composable
fun HaiSharedHeader(
    connected: Boolean,
    onDisconnect: () -> Unit,
    onMenu: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(68.dp)
                .padding(horizontal = 18.dp)
        ) {
            Text(
                text = "☰",
                color = ChromeNavy,
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .clickable(onClick = onMenu)
                    .padding(4.dp)
            )

            Text(
                text = "ZTE Smart HAI",
                color = ChromeNavy,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.align(Alignment.Center)
            )

            Row(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .height(38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White)
                    .border(1.dp, ChromeBorder, RoundedCornerShape(20.dp))
                    .clickable(enabled = connected, onClick = onDisconnect)
                    .padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (connected) "متصل" else "غير متصل",
                    color = ChromeNavy,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
                Box(
                    Modifier.size(9.dp).background(
                        if (connected) ChromeGreen else Color(0xFFE15363),
                        CircleShape
                    )
                )
            }
        }
    }
}

@Composable
fun HaiSharedBottomNav(
    selected: String,
    onHome: () -> Unit,
    onStats: () -> Unit,
    onMap: () -> Unit,
    onSettings: () -> Unit,
    onMore: () -> Unit
) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Surface(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().height(82.dp),
            color = Color.White,
            shadowElevation = 9.dp
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 5.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HaiSharedNavItem("☰", "المزيد", selected == "more", Modifier.weight(1f), onMore)
                HaiSharedNavItem("⚙", "الإعدادات", selected == "settings", Modifier.weight(1f), onSettings)
                HaiSharedNavItem("⌖", "الخريطة", selected == "map", Modifier.weight(1f), onMap)
                HaiSharedNavItem("▥", "الإحصائيات", selected == "stats", Modifier.weight(1f), onStats)
                HaiSharedNavItem("⌂", "الرئيسية", selected == "home", Modifier.weight(1f), onHome)
            }
        }
    }
}

@Composable
private fun HaiSharedNavItem(
    icon: String,
    title: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            color = if (selected) ChromeBlue else ChromeMuted,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = title,
            color = if (selected) ChromeBlue else ChromeMuted,
            fontSize = 10.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            maxLines = 1
        )
    }
}
