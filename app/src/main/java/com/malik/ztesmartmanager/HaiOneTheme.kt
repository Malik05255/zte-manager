package com.malik.ztesmartmanager

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.smart.PlacementGuidance
import com.malik.ztesmartmanager.core.smart.PlacementReading
import kotlin.math.roundToInt

internal val HaiBg = Color(0xFFF2F7FF)
internal val HaiPaper = Color(0xFFFFFFFF)
internal val HaiInk = Color(0xFF082044)
internal val HaiMuted = Color(0xFF6B7E9E)
internal val HaiLine = Color(0xFFDCE8F6)
internal val HaiBlue = Color(0xFF087BFF)
internal val HaiBlue2 = Color(0xFF43B5FF)
internal val HaiNavy = Color(0xFF062B61)
internal val HaiCyan = Color(0xFF10C8D8)
internal val HaiPurple = Color(0xFF7557F5)
internal val HaiGreen = Color(0xFF22C978)
internal val HaiAmber = Color(0xFFF4B84A)
internal val HaiRed = Color(0xFFF15D72)
internal val HaiSoftBlue = Color(0xFFE7F2FF)
internal val HaiSoftGreen = Color(0xFFE7FAF1)
internal val HaiSoftAmber = Color(0xFFFFF5DF)
internal val HaiSoftPurple = Color(0xFFF0EDFF)

internal val ZteDeepBlue = HaiNavy
internal val ZteInk = HaiInk
internal val ZteCyan = HaiCyan
internal val ZteBlue = HaiBlue

internal val HaiHeroBrush = Brush.linearGradient(
    listOf(Color(0xFF061C42), Color(0xFF063D7E), Color(0xFF067E9D))
)

internal enum class HaiScreen { HOME, NETWORK, TOWERS, DEVICES, TOOLS }

@Composable
internal fun HaiCard(
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(18.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth().shadow(
            elevation = 8.dp,
            shape = RoundedCornerShape(26.dp),
            ambientColor = HaiBlue.copy(alpha = 0.035f),
            spotColor = HaiBlue.copy(alpha = 0.045f)
        ),
        shape = RoundedCornerShape(26.dp),
        color = HaiPaper,
        border = BorderStroke(1.dp, HaiLine.copy(alpha = 0.75f))
    ) {
        Column(Modifier.padding(padding), content = content)
    }
}

@Composable
internal fun HaiSectionTitle(title: String, value: String? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, color = HaiInk, fontSize = 20.sp, lineHeight = 24.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f))
        if (!value.isNullOrBlank()) {
            Surface(shape = CircleShape, color = HaiSoftBlue) {
                Text(value, color = HaiBlue, fontSize = 14.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp), maxLines = 1)
            }
        }
    }
}

@Composable
internal fun HaiPrimaryButton(label: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 56.dp),
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(containerColor = HaiBlue, disabledContainerColor = HaiBlue.copy(alpha = 0.30f))
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Black, maxLines = 1)
    }
}

@Composable
internal fun HaiGhostButton(label: String, enabled: Boolean = true, modifier: Modifier = Modifier, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().heightIn(min = 54.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, HaiLine)
    ) {
        Text(label, color = if (enabled) HaiInk else HaiMuted, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
internal fun HaiPill(label: String, good: Boolean, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = CircleShape, color = if (good) HaiSoftGreen else HaiSoftAmber) {
        Row(Modifier.padding(horizontal = 13.dp, vertical = 9.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (good) HaiGreen else HaiAmber))
            Spacer(Modifier.width(8.dp))
            Text(label, color = HaiInk, fontSize = 14.sp, fontWeight = FontWeight.Black, maxLines = 1, softWrap = false)
        }
    }
}

@Composable
internal fun HaiChoice(label: String, selected: Boolean, enabled: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Surface(
        modifier = modifier.heightIn(min = 58.dp).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) HaiBlue else Color(0xFFF7FAFE),
        border = BorderStroke(1.dp, if (selected) HaiBlue else HaiLine)
    ) {
        Box(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
            Text(label, color = if (selected) Color.White else HaiInk, fontSize = 15.sp, lineHeight = 19.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
internal fun HaiTopBar(connected: Boolean) {
    Surface(color = HaiBg) {
        BoxWithConstraints(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 16.dp, vertical = 10.dp)) {
            if (maxWidth < 520.dp) {
                Column(Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = HaiSoftBlue) { Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) { Text("H", color = HaiBlue, fontSize = 22.sp, fontWeight = FontWeight.Black) } }
                        Spacer(Modifier.width(12.dp))
                        Text("ZTE Manager", color = HaiInk, fontSize = 25.sp, lineHeight = 29.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1)
                    }
                    Spacer(Modifier.height(9.dp))
                    HaiPill(if (connected) "متصل" else "غير متصل", connected)
                }
            } else {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Surface(shape = CircleShape, color = HaiSoftBlue) { Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) { Text("H", color = HaiBlue, fontSize = 23.sp, fontWeight = FontWeight.Black) } }
                    Spacer(Modifier.width(12.dp))
                    Text("ZTE Manager", color = HaiInk, fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.weight(1f), maxLines = 1)
                    HaiPill(if (connected) "متصل" else "غير متصل", connected)
                }
            }
        }
    }
}

@Composable
internal fun HaiBottomBar(current: HaiScreen, onNavigate: (HaiScreen) -> Unit) {
    NavigationBar(modifier = Modifier.navigationBarsPadding(), containerColor = Color.White, tonalElevation = 8.dp) {
        val items = listOf(
            Triple(HaiScreen.HOME, "⌂", "الرئيسية"),
            Triple(HaiScreen.NETWORK, "◫", "الشبكة"),
            Triple(HaiScreen.TOWERS, "⌖", "الأبراج"),
            Triple(HaiScreen.TOOLS, "✦", "الأدوات")
        )
        items.forEach { (screen, symbol, label) ->
            NavigationBarItem(
                selected = current == screen,
                onClick = { onNavigate(screen) },
                icon = { Text(symbol, fontSize = 21.sp, fontWeight = FontWeight.Black) },
                label = { Text(label, fontSize = 14.sp, maxLines = 1, fontWeight = if (current == screen) FontWeight.Black else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(selectedIconColor = HaiBlue, selectedTextColor = HaiBlue, indicatorColor = HaiSoftBlue, unselectedIconColor = HaiMuted, unselectedTextColor = HaiMuted)
            )
        }
    }
}

internal fun haiOperator(snapshot: RouterSnapshot): String = snapshot.raw["network_provider_fullname"]?.takeIf { it.isNotBlank() } ?: snapshot.raw["network_provider"]?.takeIf { it.isNotBlank() } ?: snapshot.operatorCode?.takeIf { it.isNotBlank() } ?: "الشبكة"

internal fun haiIs5G(snapshot: RouterSnapshot): Boolean = snapshot.nrRsrp != null || snapshot.nrSinr != null || snapshot.networkType.orEmpty().contains("5G", true) || snapshot.networkType.orEmpty().contains("NR", true)

internal fun haiNetworkLabel(snapshot: RouterSnapshot): String = when {
    haiIs5G(snapshot) && snapshot.networkType.orEmpty().contains("NSA", true) -> "5G NSA"
    haiIs5G(snapshot) -> "5G"
    snapshot.networkType.orEmpty().contains("LTE", true) || snapshot.networkType.orEmpty().contains("4G", true) -> "4G LTE"
    snapshot.networkType.isNullOrBlank() -> "—"
    else -> snapshot.networkType.orEmpty()
}

internal fun haiQualityWord(score: Int): String = when {
    score >= 90 -> "ممتاز جدًا"
    score >= 80 -> "ممتاز"
    score >= 68 -> "جيد جدًا"
    score >= 55 -> "جيد"
    score >= 40 -> "مقبول"
    score > 0 -> "ضعيف"
    else -> "—"
}

internal fun haiQualityColor(score: Int): Color = when {
    score >= 80 -> HaiGreen
    score >= 55 -> HaiBlue
    score >= 40 -> HaiAmber
    score > 0 -> HaiRed
    else -> HaiMuted
}

internal fun haiMetricWord(metric: String, value: Double?): String {
    if (value == null) return "—"
    return when (metric) {
        "RSRP" -> when { value >= -85 -> "ممتاز"; value >= -95 -> "جيد جدًا"; value >= -105 -> "جيد"; value >= -115 -> "مقبول"; else -> "ضعيف" }
        "SINR" -> when { value >= 20 -> "ممتاز"; value >= 13 -> "جيد جدًا"; value >= 5 -> "جيد"; value >= 0 -> "مقبول"; else -> "ضعيف" }
        "RSRQ" -> when { value >= -10 -> "ممتاز"; value >= -13 -> "جيد جدًا"; value >= -16 -> "جيد"; value >= -19 -> "مقبول"; else -> "ضعيف" }
        else -> "—"
    }
}

internal fun haiPlacementState(reading: PlacementReading?): Pair<String, Color> {
    if (reading == null) return "جاهز" to HaiBlue
    return when (reading.guidance) {
        PlacementGuidance.EXCELLENT_HOLD -> "ثبّت هنا" to HaiGreen
        PlacementGuidance.BEST_SO_FAR -> "الأفضل الآن" to HaiGreen
        PlacementGuidance.MUCH_BETTER -> "تحسن كبير" to HaiGreen
        PlacementGuidance.BETTER -> "أفضل" to HaiBlue
        PlacementGuidance.RETURN_TO_BEST -> "ارجع خطوة" to HaiAmber
        PlacementGuidance.CELL_CHANGED_WORSE -> "غيّر المكان" to HaiRed
        PlacementGuidance.WORSE -> "أضعف" to HaiAmber
        PlacementGuidance.STABLE -> if (reading.score.total >= 82) "ثبّت هنا" to HaiGreen else "ثابت" to HaiBlue
        PlacementGuidance.INITIAL -> haiQualityWord(reading.score.total) to haiQualityColor(reading.score.total)
    }
}

internal fun haiActiveBands(snapshot: RouterSnapshot): List<String> {
    val fromCells = snapshot.cells.mapNotNull { it.band?.trim()?.takeIf(String::isNotBlank) }.distinct()
    if (fromCells.isNotEmpty()) return fromCells
    return buildList {
        snapshot.nrBand?.trim()?.takeIf(String::isNotBlank)?.let { add(it) }
        snapshot.lteBand?.trim()?.takeIf(String::isNotBlank)?.let { add(it) }
    }.distinct()
}

internal fun haiRounded(value: Double?, unit: String): String = value?.let { "${it.roundToInt()} $unit" } ?: "—"
