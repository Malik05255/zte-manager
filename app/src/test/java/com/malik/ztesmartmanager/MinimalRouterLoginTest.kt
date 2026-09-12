package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimalRouterLoginTest {
    private val source = File("src/main/java/com/malik/ztesmartmanager/HaiOneLogin.kt").readText()

    @Test
    fun loginKeepsEssentialConnectionFlow() {
        assertTrue(source.contains("ZteRouterLoginScreen("))
        assertTrue(source.contains("ZTE Manager"))
        assertTrue(source.contains("عنوان الراوتر"))
        assertTrue(source.contains("كلمة المرور"))
        assertTrue(source.contains("تذكرني"))
        assertTrue(source.contains("enabled = !busy && password.isNotBlank() && routerAddress.isNotBlank()"))
        assertTrue(source.contains("PasswordVisualTransformation()"))
        assertTrue(source.contains("Checkbox("))
        assertTrue(source.contains("onClick = onConnect"))
    }

    @Test
    fun loginIsResponsiveAndReadable() {
        assertTrue(source.contains("verticalScroll(rememberScrollState())"))
        assertTrue(source.contains("imePadding()"))
        assertTrue(source.contains("navigationBarsPadding()"))
        assertTrue(source.contains("statusBarsPadding()"))
        assertTrue(source.contains("widthIn(max = 540.dp)"))
        assertTrue(source.contains("BoxWithConstraints"))
        assertTrue(source.contains("fillMaxWidth().aspectRatio("))

        val regex = Regex("fontSize\\s*=\\s*([0-9]+(?:\\.[0-9]+)?)\\.sp")
        val tooTiny = regex.findAll(source)
            .map { it.groupValues[1].toDouble() }
            .filter { it < 14.0 }
            .toList()
        assertTrue("شاشة الدخول تحتوي نصًا أصغر من 14sp: $tooTiny", tooTiny.isEmpty())
    }

    @Test
    fun loginHasNoOldExplanatoryDecoration() {
        assertFalse(source.contains("اتصال مباشر بالراوتر"))
        assertFalse(source.contains("أدخل عنوان الراوتر وكلمة مرور الإدارة"))
        assertFalse(source.contains("rememberInfiniteTransition"))
        assertFalse(source.contains("PortalHero("))
        assertFalse(source.contains("graphicsLayer"))
        assertTrue(source.contains("OutlinedTextField("))
    }
}
