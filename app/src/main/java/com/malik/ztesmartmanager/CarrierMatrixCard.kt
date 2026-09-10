package com.malik.ztesmartmanager

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.model.RouterSnapshot
import com.malik.ztesmartmanager.core.presentation.CarrierMatrixKind
import com.malik.ztesmartmanager.core.presentation.CarrierMatrixPresenter
import com.malik.ztesmartmanager.core.presentation.CarrierMatrixRow
import java.util.Locale

private val MatrixInk = Color(0xFF2A241E)
private val MatrixMuted = Color(0xFF776E63)
private val MatrixAccent = Color(0xFF876126)
private val MatrixLine = Color(0xFFD2C8B9)
private val MatrixGood = Color(0xFF567D5B)
private val MatrixSoft = Color(0xFFF0ECE4)

/** Compact live-carrier table. It consumes only CarrierMatrixPresenter's verified rows. */
@Composable
fun CarrierMatrixCard(
    snapshot: RouterSnapshot,
    compact: Boolean,
    modifier: Modifier = Modifier
) {
    val model = remember(snapshot.cells, snapshot.raw, snapshot.caActive) {
        CarrierMatrixPresenter.from(snapshot)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, MatrixLine.copy(alpha = 0.78f), RoundedCornerShape(if (compact) 14.dp else 17.dp))
            .background(MatrixSoft.copy(alpha = 0.42f), RoundedCornerShape(if (compact) 14.dp else 17.dp))
            .padding(horizontal = if (compact) 7.dp else 9.dp, vertical = if (compact) 6.dp else 8.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Live Carrier Matrix", color = MatrixInk, fontSize = 9.sp, fontWeight = FontWeight.Black)
                Text(model.caHeadline, color = if (model.caActive) MatrixGood else MatrixMuted, fontSize = 6.sp, maxLines = 1)
            }
            Text(
                when {
                    model.lteCarrierCount > 0 && model.nrCarrierCount > 0 -> "LTE ${model.lteCarrierCount} • NR ${model.nrCarrierCount}"
                    model.nrCarrierCount > 0 -> "NR ${model.nrCarrierCount}"
                    model.lteCarrierCount > 0 -> "LTE ${model.lteCarrierCount}"
                    else -> "NO VERIFIED CARRIER"
                },
                color = if (model.hasVerifiedCarrier) MatrixAccent else MatrixMuted,
                fontSize = 6.sp,
                fontWeight = FontWeight.Black,
                textAlign = TextAlign.End
            )
        }

        Spacer(Modifier.size(if (compact) 4.dp else 6.dp))
        if (model.rows.isEmpty()) {
            Text(
                model.evidenceMessage,
                color = MatrixMuted,
                fontSize = 7.sp,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )
        } else {
            MatrixHeader(compact)
            Spacer(Modifier.size(3.dp))
            model.rows.forEach { row ->
                MatrixRow(row, compact)
                Spacer(Modifier.size(3.dp))
            }
            Text(
                model.evidenceMessage,
                color = MatrixMuted,
                fontSize = 6.sp,
                maxLines = if (compact) 1 else 2,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MatrixHeader(compact: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .background(MatrixLine.copy(alpha = 0.28f), RoundedCornerShape(9.dp))
            .padding(horizontal = 4.dp, vertical = if (compact) 2.dp else 3.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MatrixCell("Carrier", 1.45f, true)
        MatrixCell("Band", 0.72f, true)
        MatrixCell("PCI", 0.67f, true)
        MatrixCell("ARFCN", 0.95f, true)
        MatrixCell("BW", 0.72f, true)
    }
}

@Composable
private fun MatrixRow(row: CarrierMatrixRow, compact: Boolean) {
    val accent = when (row.kind) {
        CarrierMatrixKind.LTE_PRIMARY -> MatrixAccent
        CarrierMatrixKind.LTE_SECONDARY -> MatrixGood
        CarrierMatrixKind.NR -> MatrixAccent
    }
    Row(
        Modifier
            .fillMaxWidth()
            .border(1.dp, accent.copy(alpha = 0.20f), RoundedCornerShape(9.dp))
            .padding(horizontal = 4.dp, vertical = if (compact) 3.dp else 4.dp),
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MatrixCell(row.roleLabel, 1.45f, false, accent)
        MatrixCell(row.band ?: "—", 0.72f)
        MatrixCell(row.pci?.toString() ?: "—", 0.67f)
        MatrixCell(row.arfcn?.toString() ?: "—", 0.95f)
        MatrixCell(row.bandwidthMhz?.let(::formatBandwidth) ?: "—", 0.72f)
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.MatrixCell(
    text: String,
    weight: Float,
    header: Boolean = false,
    color: Color = if (header) MatrixMuted else MatrixInk
) {
    Box(Modifier.weight(weight), contentAlignment = Alignment.Center) {
        Text(
            text,
            color = color,
            fontSize = if (header) 5.sp else 6.sp,
            fontWeight = if (header) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            textAlign = TextAlign.Center
        )
    }
}

private fun formatBandwidth(value: Double): String =
    if (value % 1.0 == 0.0) "${value.toInt()}M" else String.format(Locale.US, "%.1fM", value)
