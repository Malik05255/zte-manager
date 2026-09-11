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

internal val ZteBg = Color(0xFFF4F8FF)
internal val ZtePaper = Color(0xFFFFFFFF)
internal val ZteInk = Color(0xFF102A56)
internal val ZteMuted = Color(0xFF687EA8)
internal val ZteLine = Color(0xFFDCE8F8)
internal val ZteBlue = Color(0xFF0B7CFF)
internal val ZteBlue2 = Color(0xFF2B9BFF)
internal val ZteDeepBlue = Color(0xFF123A91)
internal val ZteCyan = Color(0xFF19C5E8)
internal val ZtePurple = Color(0xFF7958FF)
internal val ZteGreen = Color(0xFF2DCA72)
internal val ZteAmber = Color(0xFFFFB547)
internal val ZteRed = Color(0xFFEB5E70)
internal val ZteSoftBlue = Color(0xFFEAF4FF)
internal val ZteSoftPurple = Color(0xFFF0EDFF)
internal val ZteSoftGreen = Color(0xFFEAFBF2)

internal enum class ZteScreen { HOME, NETWORK, TOOLS, MORE }

@Composable
internal fun ZteCard(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(24.dp),
                ambientColor = ZteBlue.copy(alpha = 0.08f),
                spotColor = ZteBlue.copy(alpha = 0.08f)
            ),
        shape = RoundedCornerShape(24.dp),
        color = ZtePaper,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.98f))
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Composable
internal fun ZteSectionHeader(
    title: String,
    subtitle: String? = null,
    action: String? = null,
    onAction: (() -> Unit)? = null
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(title, color = ZteInk, fontSize = 17.sp, fontWeight = FontWeight.Black)
            if (!subtitle.isNullOrBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(subtitle, color = ZteMuted, fontSize = 13.sp, lineHeight = 18.sp)
            }
        }
        if (!action.isNullOrBlank() && onAction != null) {
            Surface(
                modifier = Modifier.clickable(onClick = onAction),
                shape = RoundedCornerShape(50),
                color = ZteSoftBlue
            ) {
                Text(
                    action,
                    color = ZteBlue,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
internal fun ZtePrimaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
        shape = RoundedCornerShape(17.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = ZteBlue,
            disabledContainerColor = ZteBlue.copy(alpha = 0.35f)
        )
    ) {
        Text(label, fontSize = 15.sp, fontWeight = FontWeight.Black)
    }
}

@Composable
internal fun ZteSecondaryButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth().heightIn(min = 50.dp),
        shape = RoundedCornerShape(17.dp),
        border = BorderStroke(1.dp, ZteLine)
    ) {
        Text(label, color = if (enabled) ZteInk else ZteMuted, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun ZteChoiceChip(
    label: String,
    selected: Boolean,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.heightIn(min = 46.dp).clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(15.dp),
        color = if (selected) ZteBlue else Color(0xFFF5F8FD),
        border = BorderStroke(1.dp, if (selected) ZteBlue else ZteLine)
    ) {
        Box(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                color = if (selected) Color.White else ZteInk,
                fontSize = 14.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
internal fun ZteStatusPill(label: String, good: Boolean) {
    Surface(shape = RoundedCornerShape(50), color = if (good) ZteSoftGreen else Color(0xFFFFF3E5)) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(9.dp).clip(CircleShape).background(if (good) ZteGreen else ZteAmber))
            Spacer(Modifier.width(7.dp))
            Text(label, color = ZteInk, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun ZteInfoTile(title: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(17.dp),
        color = Color(0xFFF7FAFF),
        border = BorderStroke(1.dp, ZteLine)
    ) {
        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, color = ZteMuted, fontSize = 12.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Text(value, color = ZteInk, fontSize = 14.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center, maxLines = 2)
        }
    }
}

@Composable
internal fun ZteTopBar(connected: Boolean, title: String) {
    Surface(color = Color.Transparent) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = ZteSoftBlue) {
                Box(Modifier.size(46.dp), contentAlignment = Alignment.Center) {
                    Text("≡", color = ZteDeepBlue, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(title, color = ZteInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Text("تحكم أكبر… اتصال أقوى", color = ZteMuted, fontSize = 13.sp)
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color.White,
                border = BorderStroke(1.dp, ZteLine),
                shadowElevation = 4.dp
            ) {
                Row(Modifier.padding(horizontal = 13.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(12.dp).clip(CircleShape).background(if (connected) ZteBlue else ZteMuted))
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(if (connected) "متصل" else "غير متصل", color = ZteBlue, fontSize = 14.sp, fontWeight = FontWeight.Black)
                        Text(if (connected) "الراوتر جاهز" else "بانتظار الراوتر", color = ZteMuted, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
internal fun ZteBottomBar(current: ZteScreen, onNavigate: (ZteScreen) -> Unit) {
    NavigationBar(containerColor = Color.White, tonalElevation = 10.dp) {
        val items = listOf(
            Triple(ZteScreen.HOME, "⌂", "الرئيسية"),
            Triple(ZteScreen.NETWORK, "▥", "الشبكة"),
            Triple(ZteScreen.TOOLS, "✦", "الأدوات"),
            Triple(ZteScreen.MORE, "☷", "المزيد")
        )
        items.forEach { (screen, symbol, label) ->
            NavigationBarItem(
                selected = current == screen,
                onClick = { onNavigate(screen) },
                icon = { Text(symbol, fontSize = 22.sp, fontWeight = FontWeight.Black) },
                label = { Text(label, fontSize = 12.sp, fontWeight = if (current == screen) FontWeight.Black else FontWeight.Medium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = ZteBlue,
                    selectedTextColor = ZteBlue,
                    indicatorColor = ZteSoftBlue,
                    unselectedIconColor = ZteMuted,
                    unselectedTextColor = ZteMuted
                )
            )
        }
    }
}

@Composable
internal fun ZteDisconnectedState(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        ZteCard(Modifier.wrapContentHeight()) {
            Box(Modifier.size(68.dp).clip(CircleShape).background(ZteSoftBlue), contentAlignment = Alignment.Center) {
                Text("ZTE", color = ZteBlue, fontSize = 18.sp, fontWeight = FontWeight.Black)
            }
            Spacer(Modifier.height(14.dp))
            Text(message, color = ZteInk, fontSize = 17.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
        }
    }
}

internal fun zteOperator(snapshot: RouterSnapshot): String =
    snapshot.raw["network_provider_fullname"]?.takeIf { it.isNotBlank() }
        ?: snapshot.raw["network_provider"]?.takeIf { it.isNotBlank() }
        ?: snapshot.operatorCode?.takeIf { it.isNotBlank() }
        ?: "شبكتك"

internal fun zteIsFiveG(snapshot: RouterSnapshot): Boolean =
    snapshot.nrRsrp != null || snapshot.nrSinr != null ||
        snapshot.networkType.orEmpty().contains("5G", true) ||
        snapshot.networkType.orEmpty().contains("NR", true)

internal fun zteNetworkLabel(snapshot: RouterSnapshot): String = when {
    zteIsFiveG(snapshot) && snapshot.networkType.orEmpty().contains("NSA", true) -> "5G NSA"
    zteIsFiveG(snapshot) -> snapshot.networkType?.takeIf { it.isNotBlank() } ?: "5G"
    snapshot.networkType.orEmpty().contains("LTE", true) || snapshot.networkType.orEmpty().contains("4G", true) -> "4G LTE"
    snapshot.networkType.isNullOrBlank() -> "غير مؤكد"
    else -> snapshot.networkType.orEmpty()
}

internal fun zteQualityWord(score: Int): String = when {
    score >= 92 -> "ممتاز جدًا"
    score >= 82 -> "ممتاز"
    score >= 70 -> "جيد جدًا"
    score >= 58 -> "جيد"
    score >= 43 -> "مقبول"
    score > 0 -> "ضعيف"
    else -> "غير مؤكد"
}

internal fun zteQualityColor(score: Int): Color = when {
    score >= 82 -> ZteGreen
    score >= 58 -> ZteBlue
    score >= 43 -> ZteAmber
    score > 0 -> ZteRed
    else -> ZteMuted
}

internal fun zteMetricWord(metric: String, value: Double?): String {
    if (value == null) return "غير متاح"
    return when (metric) {
        "RSRP" -> when {
            value >= -85 -> "ممتاز"
            value >= -95 -> "جيد جدًا"
            value >= -105 -> "جيد"
            value >= -115 -> "مقبول"
            else -> "ضعيف"
        }
        "SINR" -> when {
            value >= 20 -> "ممتاز"
            value >= 13 -> "جيد جدًا"
            value >= 5 -> "جيد"
            value >= 0 -> "مقبول"
            else -> "ضعيف"
        }
        "RSRQ" -> when {
            value >= -10 -> "ممتاز"
            value >= -13 -> "جيد جدًا"
            value >= -16 -> "جيد"
            value >= -19 -> "مقبول"
            else -> "ضعيف"
        }
        else -> "مقاس"
    }
}

internal fun ztePlacementWords(reading: PlacementReading?): Triple<String, String, Color> {
    if (reading == null) return Triple("ابدأ البحث", "حرّك الراوتر ببطء وسنقارن المكان الحالي بما سبقه.", ZteBlue)
    return when (reading.guidance) {
        PlacementGuidance.EXCELLENT_HOLD -> Triple("ممتاز — ثبّت الراوتر هنا", "هذا المكان من أفضل ما قسناه.", ZteGreen)
        PlacementGuidance.BEST_SO_FAR -> Triple("أفضل مكان حتى الآن", "هذا أفضل موقع مررت به في الجولة.", ZteGreen)
        PlacementGuidance.MUCH_BETTER -> Triple("تحسن كبير", "استمر قليلًا في نفس الاتجاه.", ZteGreen)
        PlacementGuidance.BETTER -> Triple("أصبح أفضل", "أنت تتحرك في الاتجاه الصحيح.", ZteBlue)
        PlacementGuidance.RETURN_TO_BEST -> Triple("ارجع للمكان السابق", "ابتعدت عن أفضل نقطة سجلناها.", ZteAmber)
        PlacementGuidance.CELL_CHANGED_WORSE -> Triple("الخلية تغيّرت للأسوأ", "انتقل الراوتر إلى خلية أضعف.", ZteRed)
        PlacementGuidance.WORSE -> Triple("النتيجة تراجعت", "ارجع خطوة للخلف.", ZteAmber)
        PlacementGuidance.STABLE -> Triple(
            if (reading.score.total >= 82) "ممتاز — بقيت خطوة بسيطة" else "المكان ثابت",
            if (reading.score.total >= 82) "يمكنك تثبيت الراوتر هنا." else "حرّكه قليلًا وقارن.",
            if (reading.score.total >= 82) ZteGreen else ZteBlue
        )
        PlacementGuidance.INITIAL -> Triple(zteQualityWord(reading.score.total), "هذه أول قراءة؛ حرّك الراوتر قليلًا للمقارنة.", zteQualityColor(reading.score.total))
    }
}

internal fun zteDuration(seconds: Long?): String {
    if (seconds == null) return "غير متاحة"
    val days = seconds / 86_400
    val hours = (seconds % 86_400) / 3_600
    val minutes = (seconds % 3_600) / 60
    return when {
        days > 0 -> "$days يوم $hours ساعة"
        hours > 0 -> "$hours ساعة $minutes دقيقة"
        else -> "$minutes دقيقة"
    }
}

internal fun zteActiveBands(snapshot: RouterSnapshot): List<String> {
    val fromCells = snapshot.cells.mapNotNull { it.band?.trim()?.takeIf(String::isNotBlank) }.distinct()
    if (fromCells.isNotEmpty()) return fromCells
    return buildList {
        snapshot.nrBand?.trim()?.takeIf(String::isNotBlank)?.let { add(it) }
        snapshot.lteBand?.trim()?.takeIf(String::isNotBlank)?.let { add(it) }
    }.distinct()
}

internal fun zteMetricValue(value: Double?, unit: String): String = value?.let { "${it.roundToInt()} $unit" } ?: "—"

internal val ZteHeroBrush = Brush.linearGradient(listOf(Color(0xFFF7FBFF), Color(0xFFE5F3FF), Color(0xFFF9FCFF)))
