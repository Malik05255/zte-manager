package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimalRouterLoginTest {
    private val source = File("src/main/java/com/malik/ztesmartmanager/ZteRouterLoginScreen.kt").readText()

    @Test
    fun loginKeepsEssentialConnectionFlowWithoutLegacyCopy() {
        assertTrue(source.contains("ZTE Smart HAI"))
        assertTrue(source.contains("اتصل براوترك"))
        assertTrue(source.contains("عنوان الراوتر"))
        assertTrue(source.contains("كلمة مرور الإدارة"))
        assertTrue(source.contains("تذكر بيانات الدخول"))
        assertTrue(source.contains("فتح لوحة HAI"))
        assertTrue(source.contains("محلي فقط"))

        assertFalse(source.contains("إدارة الراوتر والشبكة"))
        assertFalse(source.contains("الاتصال المحلي بالراوتر"))
        assertFalse(source.contains("أدخل عنوان لوحة الإدارة وكلمة المرور"))
        assertFalse(source.contains("حفظ كلمة المرور"))
    }

    @Test
    fun loginIsResponsiveAndKeepsControlsCompactButReadable() {
        assertTrue(source.contains("verticalScroll(rememberScrollState())"))
        assertTrue(source.contains("imePadding()"))
        assertTrue(source.contains("widthIn(max = 470.dp)"))
        assertTrue(source.contains("height(50.dp)"))
        assertTrue(source.contains("fontSize = 21.sp"))
        assertTrue(source.contains("fontSize = 11.sp"))
        assertFalse(source.contains("height(70.dp)"))
        assertFalse(source.contains("fontSize = 34.sp"))
    }

    @Test
    fun loginPortalHasVisualMotionWithoutBlockingConnection() {
        assertTrue(source.contains("rememberInfiniteTransition"))
        assertTrue(source.contains("PortalHero(busy)"))
        assertTrue(source.contains("graphicsLayer"))
        assertTrue(source.contains("enabled = !busy && password.isNotBlank() && routerAddress.isNotBlank()"))
    }
}
