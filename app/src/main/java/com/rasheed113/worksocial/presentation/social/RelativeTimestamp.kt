package com.rasheed113.worksocial.presentation.social

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

internal fun formatRelativeTimestamp(value: String, now: Instant = Instant.now()): String = runCatching {
    val created = Instant.parse(value)
    val seconds = Duration.between(created, now).seconds
    when {
        seconds < 0 -> "just now"
        seconds < 60 -> "just now"
        seconds < 3_600 -> "${seconds / 60}m ago"
        seconds < 86_400 -> "${seconds / 3_600}h ago"
        seconds < 7 * 86_400 -> "${seconds / 86_400}d ago"
        else -> DateTimeFormatter.ofPattern("dd MMM, HH:mm", Locale.US)
            .withZone(ZoneId.systemDefault()).format(created)
    }
}.getOrDefault(value)
