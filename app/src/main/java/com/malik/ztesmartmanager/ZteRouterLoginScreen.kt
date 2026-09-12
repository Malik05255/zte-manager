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
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(shape = CircleShape, color = ZteSoftBlue) {
                Box(Modifier.size(72.dp), contentAlignment = Alignment.Center) {
                    Text("ZTE", color = ZteBlue, fontSize = 20.sp, fontWeight = FontWeight.Black)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text("ZTE Manager", color = ZteInk, fontSize = 28.sp, lineHeight = 34.sp, fontWeight = FontWeight.Black)
            Spacer(Modifier.height(4.dp))
            Text("اتصال مباشر بالراوتر", color = ZteMuted, fontSize = 15.sp, lineHeight = 21.sp)
            Spacer(Modifier.height(12.dp))
            ZteStatusPill("محلي وآمن", true)

            Spacer(Modifier.height(22.dp))

            ZteCard(
                modifier = Modifier.fillMaxWidth().widthIn(max = 520.dp),
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        modifier = Modifier.width(92.dp).height(132.dp),
                        shape = RoundedCornerShape(26.dp),
                        color = ZteSoftBlue,
                        shadowElevation = 4.dp
                    ) {
                        Column(
                            Modifier.padding(vertical = 15.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Box(Modifier.width(42.dp).height(5.dp).clip(CircleShape).background(ZteInk))
                            Text("ZTE", color = ZteMuted, fontSize = 19.sp, fontWeight = FontWeight.Black)
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                repeat(4) { index ->
                                    Box(Modifier.size(7.dp).clip(CircleShape).background(if (index < 3) ZteGreen else ZteBlue))
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("اتصل براوترك", color = ZteInk, fontSize = 24.sp, lineHeight = 30.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "أدخل عنوان الراوتر وكلمة مرور الإدارة. لا نعرض بيانات وهمية إذا لم يرد الراوتر بالمعلومة.",
                        color = ZteMuted,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(Modifier.height(22.dp))

                OutlinedTextField(
                    value = routerAddress,
                    onValueChange = onRouterAddressChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("عنوان الراوتر", fontSize = 15.sp) },
                    supportingText = { Text("مثال: 192.168.0.1", fontSize = 14.sp) },
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

                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("كلمة مرور الإدارة", fontSize = 15.sp) },
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

                Spacer(Modifier.height(10.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = rememberPassword,
                        onCheckedChange = onRememberPasswordChange,
                        enabled = !busy,
                        colors = CheckboxDefaults.colors(checkedColor = ZteBlue, uncheckedColor = ZteMuted)
                    )
                    Spacer(Modifier.width(6.dp))
                    Column(Modifier.weight(1f)) {
                        Text("تذكر بيانات الدخول", color = ZteInk, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text("تُحفظ مشفرة على هذا الجهاز", color = ZteMuted, fontSize = 14.sp, lineHeight = 20.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))

                Button(
                    onClick = onConnect,
                    enabled = !busy && password.isNotBlank() && routerAddress.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
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
                        shape = RoundedCornerShape(18.dp),
                        color = if (isError) ZteRed.copy(alpha = 0.09f) else ZteSoftBlue
                    ) {
                        Text(
                            status,
                            color = if (isError) ZteRed else ZteInk,
                            fontSize = 14.sp,
                            lineHeight = 21.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier.fillMaxWidth().padding(15.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))
        }
    }
}
