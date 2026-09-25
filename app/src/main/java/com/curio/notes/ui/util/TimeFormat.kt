package com.curio.notes.ui.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val timestampFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy · HH:mm")

fun formatTimestamp(epochMillis: Long): String {
    if (epochMillis <= 0L) return ""
    return Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).format(timestampFormatter)
}
