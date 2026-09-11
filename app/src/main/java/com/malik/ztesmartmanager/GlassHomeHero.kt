package com.malik.ztesmartmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.model.RouterSnapshot

@Composable
internal fun GlassHeroSection(snapshot: RouterSnapshot) {
    val quality = remember(snapshot) { qualityFor(snapshot) }
    val active5g = glassIsFiveG(snapshot)

    GlassCard {
        BoxWithConstraints(Modifier.fillMaxWidth().padding(14.dp)) {
            if (maxWidth >= 340.dp) {
                Row(Modifier.fillMaxWidth().height(202.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassRouterVisual(snapshot, Modifier.weight(0.38f).fillMaxHeight())
                    GlassNetworkHero(
                        active5g = active5g,
                        operator = glassOperator(snapshot),
                        score = quality.total,
                        signal = quality.signal,
                        cleanliness = quality.cleanliness,
                        quality = quality.quality,
                        caActive = snapshot.caActive,
                        modifier = Modifier.weight(0.62f).fillMaxHeight()
                    )
                }
            } else {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GlassRouterVisual(snapshot, Modifier.fillMaxWidth().height(150.dp))
                    GlassNetworkHero(
                        active5g, glassOperator(snapshot), quality.total, quality.signal,
                        quality.cleanliness, quality.quality, snapshot.caActive, Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun GlassRouterVisual(snapshot: RouterSnapshot, modifier: Modifier = Modifier) {
    Box(
        modifier.clip(RoundedCornerShape(22.dp)).background(
            Brush.verticalGradient(listOf(Color(0xFFEAF3FF), Color(0xFFF8FBFF)))
        ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.48f)
            drawCircle(GlassBlue.copy(alpha = 0.07f), radius = size.minDimension * 0.36f, center = center)
            drawCircle(GlassCyan.copy(alpha = 0.08f), radius = size.minDimension * 0.27f, center = center)
        }
        Box(
            Modifier.width(84.dp).height(122.dp).shadow(9.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp)).background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("ZTE", color = GlassDeepBlue, fontWeight = FontWeight.Black, fontSize = 16.sp)
                Spacer(Modifier.height(17.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(3) { index ->
                        Box(Modifier.size(6.dp).clip(CircleShape).background(if (index == 0) GlassMint else Color(0xFFD7E1EF)))
                    }
                }
                Spacer(Modifier.height(18.dp))
                Text(
                    snapshot.model ?: "الراوتر",
                    color = GlassMuted,
                    fontSize = 9.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GlassNetworkHero(
    active5g: Boolean,
    operator: String,
    score: Int,
    signal: Int,
    cleanliness: Int,
    quality: Int,
    caActive: Boolean,
    modifier: Modifier = Modifier
) {
    Column(modifier.padding(vertical = 2.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("حالة الشبكة", color = GlassMuted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(operator, color = GlassInk, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Surface(shape = RoundedCornerShape(50), color = Color(0xFFE8F2FF)) {
                Text(
                    if (active5g) "جيل خامس" else "جيل رابع",
                    color = GlassBlue,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
        }

        Column {
            Text(if (active5g) "5G" else "4G", fontSize = 46.sp, lineHeight = 46.sp, color = GlassDeepBlue, fontWeight = FontWeight.ExtraBold)
            Text(glassQualityWord(score), fontSize = 15.sp, color = glassQualityColor(score), fontWeight = FontWeight.Black)
            Text(glassQualityAdvice(score), fontSize = 9.sp, color = GlassMuted, lineHeight = 13.sp)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            GlassSignalBubble("القوة", glassComponentWord(signal), Modifier.weight(1f))
            GlassSignalBubble("النظافة", glassComponentWord(cleanliness), Modifier.weight(1f))
            GlassSignalBubble("الجودة", glassComponentWord(quality), Modifier.weight(1f))
        }

        Text(
            if (caActive) "الراوتر يدمج أكثر من مسار للسرعة الآن" else "الدمج غير ظاهر الآن",
            color = if (caActive) GlassMint else GlassMuted,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
