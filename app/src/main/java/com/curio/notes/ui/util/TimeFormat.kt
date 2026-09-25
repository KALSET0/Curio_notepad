package com.curio.notes.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

fun formatTimestamp(epochMillis: Long, locale: Locale = Locale.getDefault()): String {
    if (epochMillis <= 0L) return ""
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm", locale))
}

/** Locale of the app resources, following the in-app language choice. */
@Composable
fun appLocale(): Locale = LocalContext.current.resources.configuration.locales[0]
