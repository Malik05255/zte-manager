package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimalRouterLoginTest {
    private val source = File("src/main/java/com/malik/ztesmartmanager/ZteRouterLoginScreen.kt").readText()

    @Test
    fun loginKeepsEssentialConnectionFlowInNewDesign() {
        assertTrue(source.contains("ZteRouterLoginScreen("))
        assertTrue(source.contains("ZTE Manager"))
        assertTrue(source.contains("بوابة الاتصال بالراوتر"))
        assertTrue(source.contains("اتصل براوترك"))
        assertTrue(source.contains("عنوان الراوتر"))
        assertTrue(source.contains("كلمة مرور الإدارة"))
        assertTrue(source.contains("تذكر بيانات الدخول"))
        assertTrue(source.contains("الاتصال بالراوتر"))
        assertTrue(source.contains("enabled = !busy && password.isNotBlank() && routerAddress.isNotBlank()"))
        assertTrue(source.contains("PasswordVisualTransformation()"))
        assertTrue(source.contains("Checkbox("))
    }

    @Test
    fun loginIsResponsiveForHonor200AndAvoidsTinyControls() {
        assertTrue(source.contains("verticalScroll(rememberScrollState())"))
        assertTrue(source.contains("imePadding()"))
        assertTrue(source.contains("navigationBarsPadding()"))
        assertTrue(source.contains("statusBarsPadding()"))
        assertTrue(source.contains("widthIn(max = 520.dp)"))
        assertTrue(source.contains("heightIn(min = 54.dp)"))
        assertTrue(source.contains("ZteCard("))
        assertTrue(source.contains("ZteStatusPill("))

        val regex = Regex("fontSize\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\.sp")
        val tooTiny = regex.findAll(source)
            .map { it.groupValues[1].toDouble() }
            .filter { it < 12.0 }
            .toList()
        assertTrue("شاشة الدخول تحتوي نصًا أصغر من 12sp: $tooTiny", tooTiny.isEmpty())
    }

    @Test
    fun oldPortalDecorationIsGoneAndConnectionRemainsFunctional() {
        assertFalse(source.contains("rememberInfiniteTransition"))
        assertFalse(source.contains("PortalHero("))
        assertFalse(source.contains("PortalTag("))
        assertFalse(source.contains("graphicsLayer"))
        assertFalse(source.contains("فتح لوحة HAI"))
        assertTrue(source.contains("onClick = onConnect"))
        assertTrue(source.contains("OutlinedTextField("))
    }
}
