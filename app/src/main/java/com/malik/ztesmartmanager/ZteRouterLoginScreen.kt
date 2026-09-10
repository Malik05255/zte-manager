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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.malik.ztesmartmanager.core.network.AndroidRouterGatewayDetector
import com.malik.ztesmartmanager.core.presentation.RouterLoginGatewayPolicy
import com.malik.ztesmartmanager.core.storage.SecureRouterCredentialStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

private val LoginBgTop = Color(0xFFF8FAFD)
private val LoginBgBottom = Color(0xFFEDF3F8)
private val LoginCard = Color.White
private val LoginInk = Color(0xFF10243A)
private val LoginMuted = Color(0xFF718096)
private val LoginBlue = Color(0xFF1769E8)
private val LoginBlueDark = Color(0xFF0A438E)
private val LoginLine = Color(0xFFD8E2EC)
private val LoginGreen = Color(0xFF20B66A)
private val LoginError = Color(0xFFD84747)

@Composable
fun ZteRouterLoginScreen(
    routerAddress: String,
    onRouterAddressChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    rememberPassword: Boolean,
    onRememberPasswordChange: (Boolean) -> Unit,
    autoDetectGateway: Boolean,
    status: String,
    busy: Boolean,
    onConnect: () -> Unit
) {
    val context = LocalContext.current
    val credentialStore = remember(context) { SecureRouterCredentialStore(context) }
    var userEditedAddress by rememberSaveable { mutableStateOf(false) }
    var credentialSaveFailed by remember { mutableStateOf(false) }
    val isError = status.startsWith("تعذر") || status.contains("رفض") || status.contains("خطأ")

    // Start only after the parent has confirmed that there is no stored router credential. User
    // typing does not cancel detection; only manually editing the router address prevents overwrite.
    LaunchedEffect(autoDetectGateway, userEditedAddress) {
        if (!autoDetectGateway || userEditedAddress) return@LaunchedEffect
        val detected = withContext(Dispatchers.IO) { AndroidRouterGatewayDetector.detect(context) }
        if (RouterLoginGatewayPolicy.shouldAdoptDetectedGateway(
                currentAddress = routerAddress,
                detectedGateway = detected,
                userEditedAddress = userEditedAddress
            )
        ) {
            onRouterAddressChange(detected!!)
        }
    }

    // "حفظ كلمة المرور" means save now, not only after a successful router login. Debouncing avoids
    // a keystore write on every keystroke while still making the checked state reliable.
    LaunchedEffect(rememberPassword, routerAddress, password) {
        if (rememberPassword && routerAddress.isNotBlank() && password.isNotBlank()) {
            delay(250)
            credentialSaveFailed = !withContext(Dispatchers.IO) {
                credentialStore.save(routerAddress, password)
            }
        } else if (!rememberPassword) {
            credentialSaveFailed = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(LoginBgTop, LoginBgBottom)))
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            RouterMark()
            Spacer(Modifier.height(14.dp))

            Text(
                text = "ZTE Smart HAI",
                color = LoginInk,
                fontSize = 28.sp,
                fontWeight = FontWeight.Black
            )

            Spacer(Modifier.height(22.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .shadow(14.dp, RoundedCornerShape(26.dp)),
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = LoginCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White)
            ) {
                Column(Modifier.padding(horizontal = 20.dp, vertical = 22.dp)) {
                    OutlinedTextField(
                        value = routerAddress,
                        onValueChange = {
                            userEditedAddress = true
                            onRouterAddressChange(it)
                        },
                        label = { Text("عنوان الراوتر", fontSize = 14.sp) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = loginFieldColors()
                    )

                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = onPasswordChange,
                        label = { Text("كلمة المرور", fontSize = 14.sp) },
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        singleLine = true,
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = loginFieldColors()
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberPassword,
                            onCheckedChange = onRememberPasswordChange,
                            enabled = !busy,
                            colors = CheckboxDefaults.colors(
                                checkedColor = LoginBlue,
                                uncheckedColor = LoginMuted
                            )
                        )
                        Text(
                            text = "حفظ كلمة المرور",
                            color = LoginInk,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (credentialSaveFailed) {
                        Text(
                            text = "تعذر حفظ كلمة المرور على هذا الجهاز",
                            color = LoginError,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }

                    Spacer(Modifier.height(12.dp))

                    Button(
                        onClick = onConnect,
                        enabled = !busy && password.isNotBlank() && routerAddress.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LoginBlue,
                            disabledContainerColor = Color(0xFFAFC4DF)
                        )
                    ) {
                        Text(
                            text = if (busy) "جاري الاتصال…" else "اتصال",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (status.isNotBlank() && status != "غير متصل" && status != "جاري الاتصال...") {
                        Spacer(Modifier.height(14.dp))
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isError) Color(0xFFFFF3F3) else Color(0xFFF1F7FF))
                                .border(
                                    1.dp,
                                    if (isError) Color(0xFFFFD6D6) else Color(0xFFD8E8FF),
                                    RoundedCornerShape(14.dp)
                                )
                                .padding(12.dp)
                        ) {
                            Text(
                                text = status,
                                color = if (isError) LoginError else LoginBlueDark,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RouterMark() {
    Box(
        modifier = Modifier
            .size(88.dp)
            .shadow(10.dp, RoundedCornerShape(26.dp))
            .clip(RoundedCornerShape(26.dp))
            .background(Color.White)
            .border(1.dp, LoginLine, RoundedCornerShape(26.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(56.dp)) {
            val center = Offset(size.width / 2f, size.height / 2f)
            drawCircle(LoginBlue.copy(alpha = 0.08f), radius = 25f, center = center)
            drawCircle(LoginBlue, radius = 7f, center = center)
            repeat(3) { index ->
                val inset = 8f + index * 7f
                drawArc(
                    color = LoginBlue.copy(alpha = 0.78f - index * 0.16f),
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = androidx.compose.ui.geometry.Size(size.width - inset * 2, size.height - inset * 2),
                    style = Stroke(width = 2.4f, cap = StrokeCap.Round)
                )
            }
            drawCircle(LoginGreen, radius = 3.6f, center = Offset(center.x, size.height - 7f))
        }
    }
}

@Composable
private fun loginFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = LoginBlue,
    unfocusedBorderColor = LoginLine,
    focusedLabelColor = LoginBlueDark,
    unfocusedLabelColor = LoginMuted,
    cursorColor = LoginBlue,
    focusedTextColor = LoginInk,
    unfocusedTextColor = LoginInk
)
