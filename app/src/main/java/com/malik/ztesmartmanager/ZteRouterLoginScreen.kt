package com.malik.ztesmartmanager

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val PortalBg = Color(0xFFF3F6FA)
private val PortalInk = Color(0xFF071525)
private val PortalMuted = Color(0xFF728096)
private val PortalBlue = Color(0xFF2D7CFF)
private val PortalCyan = Color(0xFF10C7D5)
private val PortalMint = Color(0xFF20C888)
private val PortalRed = Color(0xFFE45D68)
private val PortalNight = Color(0xFF06101F)
private val PortalNight2 = Color(0xFF0A3343)
private val PortalLine = Color(0xFFE2E8EF)

@Composable
fun ZteRouterLoginScreen(
    routerAddress: String,
    onRouterAddressChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    rememberPassword: Boolean,
    onRememberPasswordChange: (Boolean) -> Unit,
    status: String,
    busy: Boolean,
    onConnect: () -> Unit
) {
    val isError = status.startsWith("تعذر") || status.contains("رفض") || status.contains("خطأ")

    Box(
        Modifier.fillMaxSize().background(PortalBg).statusBarsPadding().navigationBarsPadding().imePadding()
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("HAI", color = PortalInk, fontSize = 18.sp, fontWeight = FontWeight.Black)
                    Text("بوابة الراوتر", color = PortalMuted, fontSize = 9.sp)
                }
                Text(
                    "محلي فقط", color = PortalMint, fontSize = 8.sp, fontWeight = FontWeight.Black,
                    modifier = Modifier.clip(RoundedCornerShape(50)).background(Color(0xFFE8F8F1)).padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }

            Spacer(Modifier.height(20.dp))
            PortalHero(busy)
            Spacer(Modifier.height(14.dp))

            Surface(
                Modifier.fillMaxWidth().widthIn(max = 470.dp).shadow(7.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp), color = Color.White
            ) {
                Column(Modifier.padding(17.dp)) {
                    Text("اتصل براوترك", color = PortalInk, fontSize = 16.sp, fontWeight = FontWeight.Black)
                    Text("العنوان وكلمة المرور لا يغادران مسار الاتصال المحلي", color = PortalMuted, fontSize = 8.sp)
                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = routerAddress,
                        onValueChange = onRouterAddressChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("عنوان الراوتر") },
                        supportingText = { Text("مثال: 192.168.0.1", fontSize = 8.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        enabled = !busy,
                        shape = RoundedCornerShape(17.dp),
                        colors = portalFieldColors()
                    )
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("كلمة مرور الإدارة") },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        enabled = !busy,
                        shape = RoundedCornerShape(17.dp),
                        colors = portalFieldColors()
                    )

                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = rememberPassword,
                            onCheckedChange = onRememberPasswordChange,
                            enabled = !busy,
                            colors = CheckboxDefaults.colors(checkedColor = PortalBlue, uncheckedColor = PortalMuted)
                        )
                        Column {
                            Text("تذكر بيانات الدخول", color = PortalInk, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Text("تُحفظ مشفرة على الجهاز", color = PortalMuted, fontSize = 8.sp)
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Button(
                        onClick = onConnect,
                        enabled = !busy && password.isNotBlank() && routerAddress.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PortalNight, disabledContainerColor = Color(0xFFB8C2CF))
                    ) {
                        Text(if (busy) "جاري فتح الجلسة…" else "فتح لوحة HAI", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Black)
                    }

                    if (status.isNotBlank() && status != "غير متصل" && status != "جاري الاتصال...") {
                        Spacer(Modifier.height(11.dp))
                        Text(
                            status,
                            color = if (isError) PortalRed else PortalBlue,
                            fontSize = 9.sp,
                            lineHeight = 13.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
                                .background(if (isError) Color(0xFFFFECEE) else Color(0xFFEAF3FF)).padding(11.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PortalHero(busy: Boolean) {
    val transition = rememberInfiniteTransition(label = "portal")
    val pulse by transition.animateFloat(
        initialValue = .88f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse), label = "portal-pulse"
    )

    Box(
        Modifier.fillMaxWidth().widthIn(max = 470.dp).height(190.dp).clip(RoundedCornerShape(31.dp))
            .background(Brush.linearGradient(listOf(PortalNight, PortalNight2)))
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(PortalCyan.copy(alpha = .08f), radius = size.minDimension * .44f, center = Offset(size.width * .86f, size.height * .14f))
            drawCircle(Color.White.copy(alpha = .035f), radius = size.minDimension * .38f, center = Offset(size.width * .08f, size.height * .88f))
        }
        Row(Modifier.fillMaxSize().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("ZTE Smart HAI", color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.Black)
                Text("اتصال واحد، ثم كل شيء أمامك", color = Color.White.copy(alpha = .62f), fontSize = 9.sp)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                    PortalTag("قراءة حيّة")
                    PortalTag("read-back")
                }
            }
            Box(Modifier.size(88.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.fillMaxSize().graphicsLayer(scaleX = if (busy) pulse else 1f, scaleY = if (busy) pulse else 1f)) {
                    repeat(3) { i ->
                        drawCircle(PortalCyan.copy(alpha = .26f - i * .07f), radius = 20f + i * 17f, style = Stroke(2.2f, cap = StrokeCap.Round))
                    }
                    drawCircle(PortalCyan, radius = 7f)
                }
                Text("H", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

@Composable
private fun PortalTag(text: String) {
    Text(
        text, color = Color.White.copy(alpha = .82f), fontSize = 8.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .08f)).padding(horizontal = 9.dp, vertical = 6.dp)
    )
}

@Composable
private fun portalFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = PortalBlue,
    unfocusedBorderColor = PortalLine,
    focusedLabelColor = PortalBlue,
    unfocusedLabelColor = PortalMuted,
    cursorColor = PortalBlue,
    focusedTextColor = PortalInk,
    unfocusedTextColor = PortalInk
)
