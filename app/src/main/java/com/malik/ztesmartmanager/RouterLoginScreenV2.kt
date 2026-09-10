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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.storage.RouterCredentialStore

private val LoginBackground = Color(0xFFF4F7FB)
private val LoginCard = Color(0xFFFFFFFF)
private val LoginInk = Color(0xFF102A56)
private val LoginMuted = Color(0xFF6E7D98)
private val LoginBlue = Color(0xFF1368E8)
private val LoginBlueDark = Color(0xFF0847A6)
private val LoginCyan = Color(0xFF18C6D5)
private val LoginLine = Color(0xFFDCE5F1)
private val LoginDanger = Color(0xFFB4232E)

@Composable
fun RouterLoginScreenV2(
    routerAddress: String,
    onRouterAddressChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    status: String,
    busy: Boolean,
    onConnect: () -> Unit
) {
    val context = LocalContext.current
    val credentialStore = remember(context) { RouterCredentialStore(context) }
    var rememberPassword by rememberSaveable { mutableStateOf(credentialStore.hasSavedPassword()) }

    LaunchedEffect(Unit) {
        if (password.isBlank()) {
            credentialStore.loadPassword()?.let(onPasswordChange)
        }
    }

    Box(
        Modifier.fillMaxSize().background(
            Brush.verticalGradient(listOf(LoginBackground, Color(0xFFEAF1FA)))
        ).padding(horizontal = 18.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier.fillMaxWidth().widthIn(max = 460.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            RouterBrandMark()
            Spacer(Modifier.height(20.dp))

            Column(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp))
                    .background(LoginCard)
                    .border(1.dp, LoginLine, RoundedCornerShape(28.dp))
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Text(
                    "الاتصال بالراوتر",
                    color = LoginInk,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "اتصل بشبكة الراوتر ثم أدخل بيانات الإدارة.",
                    color = LoginMuted,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(22.dp))

                OutlinedTextField(
                    value = routerAddress,
                    onValueChange = onRouterAddressChange,
                    label = { Text("عنوان الراوتر") },
                    supportingText = { Text("مثال: 192.168.0.1") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = loginFieldColors()
                )
                Spacer(Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = onPasswordChange,
                    label = { Text("كلمة مرور الإدارة") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = loginFieldColors()
                )

                Row(
                    Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = rememberPassword,
                        onCheckedChange = { checked ->
                            rememberPassword = checked
                            if (!checked) credentialStore.clearPassword()
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = LoginBlue,
                            uncheckedColor = LoginMuted
                        )
                    )
                    Column {
                        Text("حفظ كلمة المرور", color = LoginInk, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("تُحفظ مشفّرة على هذا الجهاز فقط", color = LoginMuted, fontSize = 12.sp)
                    }
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        if (rememberPassword && password.isNotBlank()) {
                            runCatching { credentialStore.savePassword(password) }
                        }
                        onConnect()
                    },
                    enabled = !busy && password.isNotBlank() && routerAddress.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = LoginBlue)
                ) {
                    Text(
                        if (busy) "جاري الاتصال…" else "اتصال بالراوتر",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                val friendlyStatus = friendlyRouterStatus(status)
                if (friendlyStatus.isNotBlank() && friendlyStatus != "غير متصل") {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        friendlyStatus,
                        color = if (isConnectionFailure(status)) LoginDanger else LoginMuted,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            Spacer(Modifier.height(14.dp))
            Text(
                "الاتصال يتم مباشرة بين الهاتف والراوتر داخل الشبكة المحلية.",
                color = LoginMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RouterBrandMark() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(74.dp).clip(RoundedCornerShape(22.dp)).background(
                Brush.linearGradient(listOf(LoginBlueDark, LoginBlue, LoginCyan))
            ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(44.dp)) {
                val baseY = size.height * .72f
                val step = size.width / 6f
                repeat(4) { i ->
                    val x = step * (i + 1)
                    val top = baseY - (i + 1) * size.height * .11f
                    drawLine(
                        Color.White,
                        Offset(x, baseY),
                        Offset(x, top),
                        strokeWidth = 5.5f,
                        cap = StrokeCap.Round
                    )
                }
                drawCircle(Color.White, radius = 4.5f, center = Offset(size.width * .5f, size.height * .26f))
            }
        }
        Spacer(Modifier.height(12.dp))
        Text("ZTE Smart HAI", color = LoginInk, fontSize = 26.sp, fontWeight = FontWeight.Black)
        Text("إدارة ذكية لاتصال الراوتر", color = LoginMuted, fontSize = 14.sp)
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LoginBlue,
    unfocusedBorderColor = LoginLine,
    focusedLabelColor = LoginBlue,
    unfocusedLabelColor = LoginMuted,
    cursorColor = LoginBlue,
    focusedTextColor = LoginInk,
    unfocusedTextColor = LoginInk
)

private fun isConnectionFailure(status: String): Boolean {
    val value = status.lowercase()
    return value.contains("failed to connect") ||
        value.contains("timeout") ||
        value.contains("timed out") ||
        value.contains("connectexception") ||
        value.contains("تعذر الاتصال") ||
        value.contains("رفض الراوتر")
}

private fun friendlyRouterStatus(status: String): String {
    if (status.isBlank()) return ""
    val value = status.lowercase()
    return when {
        value.contains("failed to connect") || value.contains("timeout") || value.contains("timed out") ->
            "تعذر الوصول إلى الراوتر. تأكد أنك متصل بشبكة الراوتر وأن عنوان الراوتر صحيح، ثم حاول مرة أخرى."
        value.contains("connection refused") || value.contains("refused to connect") ->
            "الراوتر موجود على الشبكة لكنه رفض الاتصال. تأكد من عنوان الإدارة وإعدادات الراوتر."
        value.contains("unknownhost") || value.contains("unable to resolve") ->
            "تعذر العثور على عنوان الراوتر. تحقق من العنوان المكتوب ثم أعد المحاولة."
        value.contains("رفض الراوتر تسجيل الدخول") ->
            "تعذر تسجيل الدخول. تحقق من كلمة مرور الإدارة وحاول مرة أخرى."
        value.contains("تعذر الاتصال بالراوتر") ->
            "تعذر الاتصال بالراوتر. تأكد من شبكة Wi‑Fi وعنوان الراوتر ثم حاول مرة أخرى."
        else -> status
    }
}
