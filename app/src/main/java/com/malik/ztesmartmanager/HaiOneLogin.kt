package com.malik.ztesmartmanager

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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
        Modifier.fillMaxSize().background(HaiBg).statusBarsPadding().navigationBarsPadding().imePadding()
    ) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("ZTE Manager", color = HaiInk, fontSize = 30.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(14.dp))
            HaiCard(modifier = Modifier.fillMaxWidth().widthIn(max = 540.dp), padding = PaddingValues(0.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(26.dp)).background(HaiHeroBrush)) {
                    val ratio = if (maxWidth < 520.dp) 1.18f else 2.0f
                    Box(Modifier.fillMaxWidth().aspectRatio(ratio)) {
                        Canvas(Modifier.fillMaxSize()) {
                            val grid = 42.dp.toPx()
                            var x = 0f
                            while (x < size.width) { drawLine(Color.White.copy(alpha = 0.04f), Offset(x, 0f), Offset(x, size.height), 1.dp.toPx()); x += grid }
                            var y = 0f
                            while (y < size.height) { drawLine(Color.White.copy(alpha = 0.04f), Offset(0f, y), Offset(size.width, y), 1.dp.toPx()); y += grid }
                            drawCircle(HaiBlue2.copy(alpha = 0.10f), size.minDimension * 0.28f, Offset(size.width * 0.16f, size.height * 0.34f))
                        }
                        Row(Modifier.fillMaxSize().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                modifier = Modifier.fillMaxHeight(0.72f).aspectRatio(0.58f),
                                shape = RoundedCornerShape(24.dp),
                                color = Color.White.copy(alpha = 0.96f),
                                shadowElevation = 8.dp
                            ) {
                                Column(Modifier.fillMaxSize().padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.SpaceBetween) {
                                    Box(Modifier.width(40.dp).height(5.dp).clip(CircleShape).background(Color(0xFF243850)))
                                    Text("ZTE", color = HaiMuted, fontSize = 20.sp, fontWeight = FontWeight.Black)
                                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) { repeat(5) { i -> Box(Modifier.size(6.dp).clip(CircleShape).background(if (i < 4) HaiGreen else HaiBlue)) } }
                                }
                            }
                            Spacer(Modifier.width(18.dp))
                            Column(Modifier.weight(1f)) {
                                Text("اتصل", color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Black)
                                Spacer(Modifier.height(5.dp))
                                HaiPill("محلي", true)
                            }
                        }
                    }
                }
                Column(Modifier.padding(18.dp)) {
                    OutlinedTextField(
                        value = routerAddress,
                        onValueChange = onRouterAddressChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("عنوان الراوتر", fontSize = 15.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        enabled = !busy,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HaiBlue, unfocusedBorderColor = HaiLine, focusedLabelColor = HaiBlue, unfocusedLabelColor = HaiMuted, cursorColor = HaiBlue, focusedTextColor = HaiInk, unfocusedTextColor = HaiInk)
                    )
                    Spacer(Modifier.height(13.dp))
                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("كلمة المرور", fontSize = 15.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        enabled = !busy,
                        shape = RoundedCornerShape(18.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = HaiBlue, unfocusedBorderColor = HaiLine, focusedLabelColor = HaiBlue, unfocusedLabelColor = HaiMuted, cursorColor = HaiBlue, focusedTextColor = HaiInk, unfocusedTextColor = HaiInk)
                    )
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = rememberPassword, onCheckedChange = onRememberPasswordChange, enabled = !busy, colors = CheckboxDefaults.colors(checkedColor = HaiBlue, uncheckedColor = HaiMuted))
                        Spacer(Modifier.width(6.dp))
                        Text("تذكرني", color = HaiInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(14.dp))
                    HaiPrimaryButton(if (busy) "جاري الاتصال" else "دخول", enabled = !busy && password.isNotBlank() && routerAddress.isNotBlank(), onClick = onConnect)
                    if (status.isNotBlank() && status != "غير متصل" && status != "جاري الاتصال...") {
                        Spacer(Modifier.height(12.dp))
                        Surface(shape = RoundedCornerShape(18.dp), color = if (isError) HaiRed.copy(alpha = 0.09f) else HaiSoftBlue) {
                            Text(status, color = if (isError) HaiRed else HaiInk, fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth().padding(14.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}
