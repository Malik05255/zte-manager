package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.model.CellRole
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.OptimizationGoal
import java.util.Locale

@Composable
internal fun V6BottomNav(selected: V6Section, onSelect: (V6Section) -> Unit, modifier: Modifier = Modifier) {
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(
            modifier.fillMaxWidth().navigationBarsPadding().height(78.dp).shadow(10.dp, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)).background(Color.White).padding(horizontal = 4.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            V6Nav("☰", "المزيد", selected == V6Section.BANDS, Modifier.weight(1f)) { onSelect(V6Section.BANDS) }
            V6Nav("▣", "السجلات", selected == V6Section.TOWERS, Modifier.weight(1f)) { onSelect(V6Section.TOWERS) }
            V6Nav("⚒", "الأدوات", selected == V6Section.TOOLS, Modifier.weight(1f)) { onSelect(V6Section.TOOLS) }
            V6Nav("▂▄▆█", "الشبكة", selected == V6Section.NETWORK, Modifier.weight(1f)) { onSelect(V6Section.NETWORK) }
            V6Nav("⌂", "الرئيسية", selected == V6Section.HOME, Modifier.weight(1f)) { onSelect(V6Section.HOME) }
        }
    }
}

@Composable
internal fun V6Nav(icon: String, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.clickable(onClick = onClick).padding(vertical = 3.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, color = if (selected) V6Blue else V6Muted, fontSize = if (icon.length > 2) 14.sp else 20.sp, fontWeight = FontWeight.Bold)
        Text(label, color = if (selected) V6Blue else V6Muted, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Black else FontWeight.Medium, maxLines = 1)
    }
}

@Composable
internal fun V6Card(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Card(modifier = modifier.fillMaxWidth().shadow(3.dp, RoundedCornerShape(20.dp)), shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = V6Card), border = BorderStroke(1.dp, V6Border)) { content() }
}

@Composable
internal fun V6Value(title: String, value: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(13.dp)).background(Color(0xFFF4F6FA)).padding(9.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, color = V6Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        Text(value, color = V6Ink, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
internal fun V6Action(text: String, enabled: Boolean, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().height(50.dp).padding(vertical = 3.dp).clip(RoundedCornerShape(13.dp)).background(if (enabled) V6SoftBlue else Color(0xFFF4F6FA))
        .clickable(enabled = enabled, onClick = onClick).padding(horizontal = 13.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = if (enabled) V6Ink else V6Muted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("‹", color = if (enabled) V6Blue else V6Muted, fontSize = 20.sp)
    }
}

@Composable
internal fun V6Primary(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(50.dp).clip(RoundedCornerShape(13.dp)).background(if (enabled) V6Blue else Color(0xFFE7EAF0)).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) Color.White else V6Muted, fontSize = 12.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
internal fun V6Outline(text: String, enabled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(50.dp).clip(RoundedCornerShape(13.dp)).background(Color.White).border(1.dp, V6Border, RoundedCornerShape(13.dp)).clickable(enabled = enabled, onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = if (enabled) V6Ink else V6Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun V6Chip(text: String, active: Boolean, onClick: () -> Unit) {
    Box(Modifier.clip(RoundedCornerShape(50)).background(if (active) V6SoftBlue else Color(0xFFF4F6FA)).clickable(onClick = onClick).padding(horizontal = 11.dp, vertical = 7.dp)) {
        Text(text, color = if (active) V6Blue else V6Muted, fontSize = 10.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun V6Choice(text: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(40.dp).clip(RoundedCornerShape(50)).background(if (selected) V6Blue else Color(0xFFF4F6FA)).clickable(onClick = onClick), contentAlignment = Alignment.Center) {
        Text(text, color = if (selected) Color.White else V6Ink, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

internal fun v6Title(section: V6Section) = when (section) {
    V6Section.HOME -> "الرئيسية"
    V6Section.NETWORK -> "الشبكة"
    V6Section.TOWERS -> "الأبراج والسجلات"
    V6Section.BANDS -> "الترددات"
    V6Section.TOOLS -> "الأدوات"
}

internal fun v6Network(snapshot: RouterSnapshot): String {
    val nr = snapshot.raw["_zte_nr_active_verified"].equals("true", true)
    val lte = snapshot.raw["_zte_lte_active_verified"].equals("true", true)
    return when { nr -> "5G"; lte -> "4G"; else -> snapshot.networkType?.takeIf { it.isNotBlank() } ?: "—" }
}

internal fun v6Rsrp(snapshot: RouterSnapshot): Double? =
    if (snapshot.raw["_zte_nr_active_verified"].equals("true", true)) snapshot.nrRsrp else snapshot.lteRsrp

internal fun v6ActiveBands(snapshot: RouterSnapshot): List<String> {
    val cells = snapshot.cells.filter { it.role != CellRole.NR }.mapNotNull { it.band?.trim()?.uppercase() }.distinct()
    return if (cells.isNotEmpty()) cells else listOfNotNull(snapshot.lteBand?.trim()?.uppercase()).distinct()
}

internal fun v6BandNumber(value: String?): Int? = value?.trim()?.removePrefix("n")?.removePrefix("N")?.removePrefix("b")?.removePrefix("B")?.toIntOrNull()
internal fun v6Goal(goal: OptimizationGoal) = when (goal) {
    OptimizationGoal.BALANCED -> "متوازن"
    OptimizationGoal.SPEED -> "سرعة"
    OptimizationGoal.GAMING -> "ألعاب"
    OptimizationGoal.STABILITY -> "ثبات"
}
internal fun v6Fmt(value: Double) = String.format(Locale.US, "%.1f", value)
