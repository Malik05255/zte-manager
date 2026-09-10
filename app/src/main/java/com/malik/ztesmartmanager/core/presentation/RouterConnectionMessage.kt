package com.malik.ztesmartmanager.core.presentation

import java.util.Locale

object RouterConnectionMessage {
    fun from(error: Throwable?, routerAddress: String): String {
        val raw = error?.message.orEmpty().trim()
        if (raw.any { it in '\u0600'..'\u06FF' }) return raw

        val normalized = raw.lowercase(Locale.ROOT)
        return when {
            normalized.contains("failed to connect") ||
                normalized.contains("connect timed out") ||
                normalized.contains("timed out") ||
                normalized.contains("timeout") ->
                "تعذر الوصول إلى الراوتر على $routerAddress. تأكد أن الهاتف متصل بشبكة الراوتر وأن عنوان الراوتر صحيح، ثم حاول مرة أخرى."

            normalized.contains("connection refused") || normalized.contains("refused") ->
                "تم الوصول إلى عنوان الراوتر، لكن الاتصال رُفض. تأكد من عنوان الراوتر وإعدادات الإدارة ثم حاول مرة أخرى."

            normalized.contains("unable to resolve host") ||
                normalized.contains("unknownhost") ||
                normalized.contains("no address associated") ->
                "تعذر العثور على عنوان الراوتر. تحقق من العنوان المدخل ومن اتصال الهاتف بشبكة الراوتر."

            normalized.contains("cleartext") ->
                "أوقف أندرويد الاتصال المحلي بالراوتر بسبب إعداد أمان. حدّث التطبيق ثم حاول مرة أخرى."

            else ->
                "تعذر الاتصال بالراوتر. تأكد من اتصال الهاتف بشبكة الراوتر وصحة العنوان، ثم حاول مرة أخرى."
        }
    }
}
