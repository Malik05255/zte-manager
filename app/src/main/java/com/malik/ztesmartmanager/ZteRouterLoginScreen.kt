package com.malik.ztesmartmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RouterLoginBgTop = Color(0xFFF8FAFC)
private val RouterLoginBgBottom = Color(0xFFEFF4F8)
private val RouterLoginCard = Color(0xFFFFFFFF)
private val RouterLoginInk = Color(0xFF14202B)
private val RouterLoginMuted = Color(0xFF687784)
private val RouterLoginBlue = Color(0xFF1769E8)
private val RouterLoginBlueDark = Color(0xFF0B438F)
private val RouterLoginLine = Color(0xFFD7E0E8)
private val RouterLoginGreen = Color(0xFF20B66A)
private val RouterLoginError = Color(0xFFD84747)

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
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(RouterLoginBgTop, RouterLoginBgBottom)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(12.dp))
            RouterDeviceIllustration()
            Spacer(Modifier.height(14.dp))

            Text(
                text = "ZTE Smart HAI",
                color = RouterLoginInk,
                fontSize = 29.sp,
                fontWeight = FontWeight.Black
            )
            Text(
                text = "إدارة الراوتر والشبكة",
                color = RouterLoginMuted,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 3.dp)
            )

            Spacer(Modifier.height(22.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 500.dp)
                    .shadow(18.dp, RoundedCornerShape(28.dp)),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = RouterLoginCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier
                                .size(11.dp)
                                .clip(CircleShape)
                                .background(RouterLoginGreen)
                        )
                        Spacer(Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "الاتصال المحلي بالراوتر",
                                color = RouterLoginInk,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "أدخل عنوان لوحة الإدارة وكلمة المرور",
                                color = RouterLoginMuted,
                                fontSize = 13.sp
                            )
                        }
                    }

                    Spacer(Modifier.height(22.dp))

                    OutlinedTextField(
                        value = routerAddress,
                        onValueChange = onRouterAddressChange,
                        label = { Text("عنوان الراوتر", fontSize = 14.sp) },
                        supportingText = { Text("مثال: 192.168.0.1", fontSize = 12.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = routerFieldColors()
                    )

                    Spacer(Modifier.height(10.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("كلمة مرور الإدارة", fontSize = 14.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = routerFieldColors()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberPassword,
                            onCheckedChange = onRememberPasswordChange,
                            enabled = !busy,
                            colors = CheckboxDefaults.colors(
                                checkedColor = RouterLoginBlue,
                                uncheckedColor = RouterLoginMuted
                            )
                        )
                        Text(
                            text = "حفظ كلمة المرور على هذا الجهاز",
                            color = RouterLoginInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Text(
                        text = "عند التفعيل تُحفظ كلمة المرور مشفرة داخل حماية أندرويد.",
                        color = RouterLoginMuted,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 12.dp, top = 1.dp)
                    )

                    Spacer(Modifier.height(18.dp))

                    Button(
                        onClick = onConnect,
                        enabled = !busy && password.isNotBlank() && routerAddress.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RouterLoginBlue,
                            disabledContainerColor = Color(0xFFAFC4DF)
                        )
                    ) {
                        Text(
                            text = if (busy) "جاري الاتصال…" else "اتصال بالراوتر",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (status.isNotBlank() && status != "غير متصل") {
                        Spacer(Modifier.height(14.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isError) Color(0xFFFFF2F2) else Color(0xFFF0F7FF))
                                .border(
                                    1.dp,
                                    if (isError) Color(0xFFFFD2D2) else Color(0xFFD6E8FF),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(13.dp)
                        ) {
                            Text(
                                text = status,
                                color = if (isError) RouterLoginError else RouterLoginBlueDark,
                                fontSize = 14.sp,
                                lineHeight = 21.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }

            Text(
                text = "يجب أن يكون الهاتف متصلًا بشبكة الراوتر عند استخدام الاتصال المحلي.",
                color = RouterLoginMuted,
                fontSize = 13.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = 460.dp).padding(horizontal = 14.dp, vertical = 18.dp)
            )
        }
    }
}

@Composable
private fun RouterDeviceIllustration() {
    Box(
        modifier = Modifier
            .size(width = 112.dp, height = 132.dp)
            .shadow(12.dp, RoundedCornerShape(32.dp))
            .clip(RoundedCornerShape(32.dp))
            .background(Color.White)
            .border(1.dp, RouterLoginLine, RoundedCornerShape(32.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(width = 78.dp, height = 102.dp)) {
            val centerX = size.width / 2f
            val bodyTop = size.height * 0.10f
            val bodyBottom = size.height * 0.87f
            drawRoundRect(
                color = Color(0xFFF7FAFC),
                topLeft = Offset(size.width * 0.18f, bodyTop),
                size = androidx.compose.ui.geometry.Size(size.width * 0.64f, bodyBottom - bodyTop),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f)
            )
            drawRoundRect(
                color = RouterLoginLine,
                topLeft = Offset(size.width * 0.18f, bodyTop),
                size = androidx.compose.ui.geometry.Size(size.width * 0.64f, bodyBottom - bodyTop),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(18f, 18f),
                style = Stroke(width = 2.2f)
            )
            drawLine(
                color = RouterLoginBlueDark,
                start = Offset(centerX, size.height * 0.25f),
                end = Offset(centerX, size.height * 0.52f),
                strokeWidth = 3.2f,
                cap = StrokeCap.Round
            )
            repeat(3) { index ->
                drawCircle(
                    color = if (index == 0) RouterLoginGreen else Color(0xFFB7C4CE),
                    radius = 4.2f,
                    center = Offset(centerX, size.height * (0.62f + index * 0.075f))
                )
            }
            repeat(3) { ring ->
                drawArc(
                    color = RouterLoginBlue.copy(alpha = 0.50f - ring * 0.10f),
                    startAngle = 205f,
                    sweepAngle = 130f,
                    useCenter = false,
                    topLeft = Offset(centerX - 14f - ring * 7f, size.height * 0.03f - ring * 5f),
                    size = androidx.compose.ui.geometry.Size(28f + ring * 14f, 26f + ring * 10f),
                    style = Stroke(width = 2f, cap = StrokeCap.Round)
                )
            }
        }
    }
}

@Composable
private fun routerFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = RouterLoginBlue,
    unfocusedBorderColor = RouterLoginLine,
    focusedLabelColor = RouterLoginBlueDark,
    unfocusedLabelColor = RouterLoginMuted,
    cursorColor = RouterLoginBlue,
    focusedTextColor = RouterLoginInk,
    unfocusedTextColor = RouterLoginInk
)
