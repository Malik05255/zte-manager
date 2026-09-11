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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
internal fun GlassCard(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().shadow(
            elevation = 7.dp,
            shape = RoundedCornerShape(20.dp),
            ambientColor = GlassBlue.copy(alpha = 0.07f),
            spotColor = GlassBlue.copy(alpha = 0.07f)
        ),
        shape = RoundedCornerShape(20.dp),
        color = GlassPaper,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.94f))
    ) { content() }
}

@Composable
internal fun GlassActionButton(label: String, enabled: Boolean = true, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GlassBlue)
    ) { Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold) }
}

@Composable
internal fun GlassModeButton(title: String, subtitle: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFFF5F8FC),
        border = BorderStroke(1.dp, GlassLine)
    ) {
        Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = GlassInk, fontSize = 11.sp, fontWeight = FontWeight.Black)
                Text(subtitle, color = GlassMuted, fontSize = 8.sp)
            }
            Text("›", color = GlassBlue, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
internal fun GlassNotice(message: String) {
    Surface(shape = RoundedCornerShape(16.dp), color = Color(0xFFEAF3FF), border = BorderStroke(1.dp, Color(0xFFD5E6FF))) {
        Text(message, color = GlassInk, fontSize = 10.sp, lineHeight = 14.sp, modifier = Modifier.padding(12.dp))
    }
}

@Composable
internal fun GlassEmpty(message: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        GlassCard(Modifier.wrapContentHeight()) {
            Column(Modifier.padding(22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFFEAF3FF)), contentAlignment = Alignment.Center) {
                    Text("ZTE", color = GlassBlue, fontWeight = FontWeight.Black, fontSize = 12.sp)
                }
                Spacer(Modifier.height(10.dp))
                Text(message, color = GlassInk, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
internal fun GlassTechnicalRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, color = GlassMuted, fontSize = 9.sp)
        Text(value, color = GlassInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
internal fun GlassSignalBubble(title: String, status: String, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = Color(0xFFF3F7FC), border = BorderStroke(1.dp, GlassLine)) {
        Column(Modifier.padding(horizontal = 5.dp, vertical = 7.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(title, fontSize = 8.sp, color = GlassMuted)
            Text(status, fontSize = 9.sp, fontWeight = FontWeight.Black, color = GlassInk, maxLines = 1)
        }
    }
}

@Composable
internal fun GlassInfoItem(title: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.padding(horizontal = 5.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 8.sp, color = GlassMuted)
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Black, color = GlassInk, textAlign = TextAlign.Center, maxLines = 2)
    }
}
