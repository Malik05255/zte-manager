package com.malik.ztesmartmanager

import androidx.compose.runtime.Composable

/** Keeps redesigned screens concise while delegating state persistence to Compose. */
@Composable
internal fun <T : Any> rememberSaveable(init: () -> T): T =
    androidx.compose.runtime.saveable.rememberSaveable(init = init)
