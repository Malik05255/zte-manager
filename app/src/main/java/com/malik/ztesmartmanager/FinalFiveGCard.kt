package com.malik.ztesmartmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.model.RouterCapabilities
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.presentation.VerifiedFiveGMode
import com.malik.ztesmartmanager.core.presentation.VerifiedFiveGPresenter

private val FiveGBg = Color(0xFFF7F2E9)
private val FiveGDeep = Color(0xFF876126)
private val FiveGGold = Color(0xFFD2A84D)
private val FiveGBright = Color(0xFFFFD96E)
private val FiveGInk = Color(0xFF2A241E)
private val FiveGMuted = Color(0xFF776E63)
private val FiveGLine = Color(0xFFD2C8B9)
private val FiveGGood = Color(0xFF567D5B)
private val FiveGWarn = Color(0xFFA96432)

@Composable
fun FinalFiveGPrimaryCard(
    snapshot: RouterSnapshot,
    capabilities: RouterCapabilities,
    selectedNr: Set<Int>,
    busy: Boolean,
    compact: Boolean,
    onNrToggle: (Int) -> Unit,
    onApplyNr: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state = VerifiedFiveGPresenter.from(snapshot)
    var expanded by rememberSaveable { mutableStateOf(false) }
    val shape = RoundedCornerShape(topStart = 26.dp, topEnd = 54.dp, bottomEnd = 34.dp, bottomStart = 54.dp)

    Card(
        modifier = modifier.shadow(8.dp, shape),
        shape = shape,
        colors = CardDefaults.cardColors(containerColor = FiveGBg)
    ) {
        Column(Modifier.padding(if (compact) 12.dp else 15.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("5G NR", color = FiveGDeep, fontSize = 9.sp, fontWeight = FontWeight.Black)
                    Text(
                        state.headline,
                        color = if (state.verified) FiveGDeep else FiveGInk,
                        fontSize = if (compact) 23.sp else 28.sp,
                        fontWeight = FontWeight.Black,
                        maxLines = 1
                    )
                }
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (state.verified) FiveGDeep else FiveGLine.copy(alpha = 0.45f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        if (state.verified) "LIVE VERIFIED" else "NOT VERIFIED",
                        color = if (state.verified) FiveGBright else FiveGMuted,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Black
                    )
                }
            }

            Spacer(Modifier.size(7.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                FiveGModePill(
                    "4G",
                    snapshot.raw["_zte_lte_active_verified"].equals("true", true),
                    Modifier.weight(1f)
                )
                FiveGModePill("5G NSA", state.mode == VerifiedFiveGMode.NSA, Modifier.weight(1f))
                FiveGModePill("5G SA", state.mode == VerifiedFiveGMode.SA, Modifier.weight(1f))
            }

            Spacer(Modifier.size(8.dp))
            if (state.verified) {
                val columns = if (compact) 3 else 5
                val metrics = listOf(
                    Triple("NR Band", state.band ?: "—", ""),
                    Triple("NR PCI", state.pci?.toString() ?: "—", ""),
                    Triple("NR ARFCN", state.arfcn?.toString() ?: "—", ""),
                    Triple("NR RSRP", state.rsrp?.let(::fiveGNumber) ?: "—", "dBm"),
                    Triple("NR SINR", state.sinr?.let(::fiveGNumber) ?: "—", "dB")
                )
                metrics.chunked(columns).forEach { rowMetrics ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        rowMetrics.forEach { metric ->
                            FiveGMetric(metric.first, metric.second, metric.third, Modifier.weight(1f))
                        }
                        repeat(columns - rowMetrics.size) { Spacer(Modifier.weight(1f)) }
                    }
                    Spacer(Modifier.size(5.dp))
                }

                if (state.mode == VerifiedFiveGMode.NSA) {
                    Text(
                        "LTE Anchor الموثّق: ${state.lteAnchorBand ?: "—"} • PCI ${state.lteAnchorPci ?: "—"} • EARFCN ${state.lteAnchorArfcn ?: "—"}",
                        color = FiveGMuted,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(FiveGLine.copy(alpha = 0.18f))
                        .padding(10.dp)
                ) {
                    Text(state.evidenceMessage, color = FiveGWarn, fontSize = 8.sp, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(Modifier.size(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.weight(1f),
                    enabled = capabilities.supportsNrBandLock,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (expanded) FiveGDeep else FiveGLine.copy(alpha = 0.50f),
                        contentColor = if (expanded) Color.White else FiveGInk
                    ),
                    contentPadding = PaddingValues(vertical = 6.dp, horizontal = 8.dp)
                ) {
                    Text(if (expanded) "إخفاء ترددات 5G" else "ترددات 5G", fontSize = 8.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(50))
                        .border(1.dp, if (state.verified) FiveGGood else FiveGLine, RoundedCornerShape(50))
                        .padding(vertical = 7.dp, horizontal = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        if (state.verified) "الحالة الحية مؤكدة" else "الاتصال الحي غير مؤكد",
                        color = if (state.verified) FiveGGood else FiveGMuted,
                        fontSize = 7.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }

            if (!capabilities.supportsNrBandLock) {
                Spacer(Modifier.size(5.dp))
                Text(
                    "هذا Profile لا يعلن دعم NR Band Lock؛ حالة 5G الحية ستظل ظاهرة إذا أمكن التحقق منها.",
                    color = FiveGMuted,
                    fontSize = 7.sp
                )
            }

            if (expanded && capabilities.supportsNrBandLock) {
                Spacer(Modifier.size(10.dp))
                Text("إعداد 5G المطلوب", color = FiveGInk, fontSize = 10.sp, fontWeight = FontWeight.Black)
                Text(
                    "هذه اختيارات إعداد فقط. لا تتحول إلى «5G متصل» إلا بعد أن يثبت الراوتر Carrier NR حيًا.",
                    color = FiveGMuted,
                    fontSize = 7.sp
                )
                Spacer(Modifier.size(6.dp))

                if (capabilities.supportedNrBands.isEmpty()) {
                    Text("لا توجد قائمة NR موثّقة لهذا Profile.", color = FiveGWarn, fontSize = 8.sp)
                } else {
                    val columns = if (compact) 4 else 5
                    capabilities.supportedNrBands.sorted().chunked(columns).forEach { bands ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            bands.forEach { band ->
                                FilterChip(
                                    selected = band in selectedNr,
                                    onClick = { onNrToggle(band) },
                                    label = { Text("N$band", fontSize = 8.sp, fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f),
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = FiveGGold.copy(alpha = 0.25f)
                                    )
                                )
                            }
                            repeat(columns - bands.size) { Spacer(Modifier.weight(1f)) }
                        }
                    }
                    Spacer(Modifier.size(6.dp))
                    Button(
                        onClick = onApplyNr,
                        enabled = selectedNr.isNotEmpty() && !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(containerColor = FiveGDeep)
                    ) {
                        Text(
                            if (busy) "جاري التطبيق والتحقق..." else "تطبيق إعداد 5G والتحقق بالـ read-back",
                            fontSize = 8.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FiveGModePill(text: String, active: Boolean, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) FiveGGold.copy(alpha = 0.22f) else Color.Transparent)
            .border(1.dp, if (active) FiveGGold else FiveGLine, RoundedCornerShape(50))
            .padding(vertical = 5.dp, horizontal = 3.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            color = if (active) FiveGDeep else FiveGMuted,
            fontSize = 7.sp,
            fontWeight = if (active) FontWeight.Black else FontWeight.Medium,
            maxLines = 1
        )
    }
}

@Composable
private fun FiveGMetric(label: String, value: String, unit: String, modifier: Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(FiveGLine.copy(alpha = 0.17f))
            .padding(vertical = 7.dp, horizontal = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = FiveGInk, fontSize = 9.sp, fontWeight = FontWeight.Black, maxLines = 1)
            if (unit.isNotBlank()) Text(unit, color = FiveGMuted, fontSize = 6.sp)
            Text(label, color = FiveGMuted, fontSize = 6.sp, maxLines = 1)
        }
    }
}

private fun fiveGNumber(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
