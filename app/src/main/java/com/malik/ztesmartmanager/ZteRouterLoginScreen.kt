package com.malik.ztesmartmanager

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
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
        Modifier
            .fillMaxSize()
            .background(ZteBg)
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Surface(shape = CircleShape, color = ZteSoftBlue) {
                    Box(Modifier.size(50.dp), contentAlignment = Alignment.Center) {
                        Text("ZTE", color = ZteBlue, fontSize = 14.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("ZTE Manager", color = ZteInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    Text("بوابة الاتصال بالراوتر", color = ZteMuted, fontSize = 13.sp)
                }
                ZteStatusPill("محلي وآمن", true)
            }

            Spacer(Modifier.height(24.dp))

            ZteCard(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("اتصل براوترك", color = ZteInk, fontSize = 24.sp, fontWeight = FontWeight.Black)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "أدخل عنوان الراوتر وكلمة مرور الإدارة. البيانات تبقى ضمن جلسة الاتصال المحلية.",
                            color = ZteMuted,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Surface(
                        modifier = Modifier.width(86.dp).height(126.dp),
                        shape = RoundedCornerShape(24.dp),
                        color = ZteSoftBlue,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            Modifier.padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(Modifier.width(40.dp).height(4.dp).clip(CircleShape).background(ZteInk))
                            Text("ZTE", color = ZteMuted, fontSize = 17.sp, fontWeight = FontWeight.Black)
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                repeat(4) { index ->
                                    Box(
                                        Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (index < 3) ZteGreen else ZteBlue)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))

                OutlinedTextField(
                    value = routerAddress,
                    onValueChange = onRouterAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان الراوتر", fontSize = 14.sp) },
                    supportingText = { Text("مثال: 192.168.0.1", fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    enabled = !busy,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZteBlue,
                        unfocusedBorderColor = ZteLine,
                        focusedLabelColor = ZteBlue,
                        unfocusedLabelColor = ZteMuted,
                        cursorColor = ZteBlue,
                        focusedTextColor = ZteInk,
                        unfocusedTextColor = ZteInk
                    )
                )

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("كلمة مرور الإدارة", fontSize = 14.sp) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    enabled = !busy,
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ZteBlue,
                        unfocusedBorderColor = ZteLine,
                        focusedLabelColor = ZteBlue,
                        unfocusedLabelColor = ZteMuted,
                        cursorColor = ZteBlue,
                        focusedTextColor = ZteInk,
                        unfocusedTextColor = ZteInk
                    )
                )

                Spacer(Modifier.height(8.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberPassword,
                        onCheckedChange = onRememberPasswordChange,
                        enabled = !busy,
                        colors = CheckboxDefaults.colors(checkedColor = ZteBlue, uncheckedColor = ZteMuted)
                    )
                    Column {
                        Text("تذكر بيانات الدخول", color = ZteInk, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Text("تُحفظ مشفرة على هذا الجهاز", color = ZteMuted, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(14.dp))

                Button(
                    onClick = onConnect,
                    enabled = !busy && password.isNotBlank() && routerAddress.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ZteBlue,
                        disabledContainerColor = ZteBlue.copy(alpha = 0.35f)
                    )
                ) {
                    Text(if (busy) "جاري فتح الجلسة…" else "الاتصال بالراوتر", fontSize = 16.sp, fontWeight = FontWeight.Black)
                }

                if (status.isNotBlank() && status != "غير متصل" && status != "جاري الاتصال...") {
                    Spacer(Modifier.height(14.dp))
                    Surface(
                        shape = RoundedCornerShape(17.dp),
                        color = if (isError) ZteRed.copy(alpha = 0.09f) else ZteSoftBlue
                    ) {
                        Text(
                            status,
                            color = if (isError) ZteRed else ZteInk,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth().padding(14.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "بعد الاتصال ستظهر القياسات التي يعيدها الراوتر فعليًا، ولن تُعرض بيانات تخمينية عند غياب الدليل.",
                color = ZteMuted,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp)
            )
            Spacer(Modifier.height(18.dp))
        }
    }
}
