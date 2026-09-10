package com.malik.ztesmartmanager.core.presentation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RouterConnectionMessageTest {
    @Test
    fun timeoutBecomesArabicMessage() {
        val message = RouterConnectionMessage.from(
            RuntimeException("connection timed out"),
            "192.168.0.1"
        )
        assertTrue(message.contains("تعذر الوصول إلى الراوتر"))
        assertTrue(message.contains("192.168.0.1"))
        assertFalse(message.contains("timed out"))
    }

    @Test
    fun ArabicProtocolMessageIsPreserved() {
        val message = RouterConnectionMessage.from(
            RuntimeException("رفض الراوتر تسجيل الدخول"),
            "192.168.0.1"
        )
        assertTrue(message == "رفض الراوتر تسجيل الدخول")
    }
}
