package com.malik.ztesmartmanager

import android.media.AudioManager
import android.media.ToneGenerator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import com.malik.ztesmartmanager.core.smart.PlacementReading
import kotlinx.coroutines.delay

@Composable
internal fun PlacementSoundEffect(enabled: Boolean, active: Boolean, reading: PlacementReading?) {
    val tone = remember { runCatching { ToneGenerator(AudioManager.STREAM_MUSIC, 55) }.getOrNull() }
    DisposableEffect(tone) {
        onDispose { runCatching { tone?.release() } }
    }

    val score = reading?.score?.total ?: 0
    LaunchedEffect(enabled, active, score, tone) {
        if (!enabled || !active || tone == null || reading == null) return@LaunchedEffect
        while (true) {
            val toneType = if (score >= 82) ToneGenerator.TONE_PROP_BEEP2 else ToneGenerator.TONE_PROP_BEEP
            tone.startTone(toneType, if (score >= 82) 90 else 55)
            delay(
                when {
                    score >= 90 -> 330L
                    score >= 82 -> 500L
                    score >= 70 -> 750L
                    score >= 55 -> 1_100L
                    else -> 1_550L
                }
            )
        }
    }
}
