package com.malik.ztesmartmanager

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MinimalRouterLoginTest {
    private val source = File("src/main/java/com/malik/ztesmartmanager/ZteRouterLoginScreen.kt").readText()

    @Test
    fun loginKeepsOnlyEssentialCopy() {
        assertTrue(source.contains("ZTE Smart HAI"))
        assertTrue(source.contains("عنوان الراوتر"))
        assertTrue(source.contains("كلمة المرور"))
        assertTrue(source.contains("حفظ كلمة المرور"))
        assertTrue(source.contains("اتصال"))

        assertFalse(source.contains("إدارة الراوتر والشبكة"))
        assertFalse(source.contains("الاتصال المحلي بالراوتر"))
        assertFalse(source.contains("أدخل عنوان لوحة الإدارة وكلمة المرور"))
        assertFalse(source.contains("عند التفعيل تُحفظ كلمة المرور مشفرة داخل حماية أندرويد"))
        assertFalse(source.contains("يجب أن يكون الهاتف متصلًا بشبكة الراوتر عند استخدام الاتصال المحلي"))
    }

    @Test
    fun loginKeepsReadableControlSizes() {
        assertTrue(source.contains("fontSize = 28.sp"))
        assertTrue(source.contains("fontSize = 17.sp"))
        assertTrue(source.contains("height(54.dp)"))
    }
}
