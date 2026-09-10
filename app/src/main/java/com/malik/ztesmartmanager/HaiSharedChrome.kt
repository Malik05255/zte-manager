package com.malik.ztesmartmanager

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
private val ChromeSoft = Color(0xFFF0F5FB)
private val ChromeSoftGreen = Color(0xFFEAFBF4)

/** Shared production header; every section uses the same geometry and system insets. */
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
                horizontalArrangement = Arrangement.spacedBy(ui.sectionGap),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChromeCircle("⚙", onSettings)
                ChromeCircle("⌕", onSearch)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ZTE Smart HAI",
                    color = ChromeNavy,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                Text(
                    text = "إدارة شبكتك ... بكل سهولة",
                    color = ChromeMuted,
                    fontSize = 8.sp,
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
                        .height(32.dp)
                        .clip(RoundedCornerShape(ui.cardRadius))
                        .background(if (connected) ChromeSoftGreen else Color(0xFFFFEEEE))
                        .clickable(enabled = connected, onClick = onDisconnect)
                        .padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        Modifier.size(7.dp).background(
                            if (connected) ChromeGreen else Color(0xFFE15363),
                            CircleShape
                        )
                    )
                    Text(
                        text = if (connected) "متصل" else "غير متصل",
                        color = ChromeNavy,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
                Spacer(Modifier.width(ui.sectionGap))
                Box(
                    modifier = Modifier
                        .size(ui.chromeButtonSize)
                        .clickable(onClick = onMenu),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "☰",
                        color = ChromeNavy,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }
        }
    }
}

@Composable
private fun ChromeCircle(label: String, onClick: () -> Unit) {
    val ui = LocalHaiUiMetrics.current
    Box(
        modifier = Modifier
            .size(ui.chromeButtonSize)
            .clip(CircleShape)
            .background(ChromeSoft)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = ChromeNavy, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }
}

/** Shared reference navigation: المزيد، السجلات، الأدوات، الشبكة، الرئيسية. */
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
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(ui.bottomNavHeight),
            color = Color.White,
            shadowElevation = 8.dp,
            shape = RoundedCornerShape(topStart = ui.cardRadius, topEnd = ui.cardRadius)
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 4.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ChromeNavItem("☰", "المزيد", selected == "more", Modifier.weight(1f), onMore)
                ChromeNavItem("▤", "السجلات", selected == "logs", Modifier.weight(1f), onLogs)
                ChromeNavItem("⚒", "الأدوات", selected == "tools", Modifier.weight(1f), onTools)
                ChromeNavItem("▥", "الشبكة", selected == "network", Modifier.weight(1f), onNetwork)
                ChromeNavItem("⌂", "الرئيسية", selected == "home", Modifier.weight(1f), onHome)
            }
        }
    }
}

@Composable
private fun ChromeNavItem(
    icon: String,
    title: String,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Column(
        modifier = modifier.clickable(onClick = onClick).padding(vertical = 1.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            color = if (selected) ChromeBlue else ChromeMuted,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = title,
            color = if (selected) ChromeBlue else ChromeMuted,
            fontSize = 7.5.sp,
            fontWeight = if (selected) FontWeight.Black else FontWeight.Medium,
            maxLines = 1
        )
    }
}
