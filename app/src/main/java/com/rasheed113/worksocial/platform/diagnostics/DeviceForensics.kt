package com.rasheed113.worksocial.platform.diagnostics

import android.util.Log
import com.rasheed113.worksocial.BuildConfig
import java.net.URI

/** Debug-only diagnostics for physical-device forensic verification. */
object DeviceForensics {
    private const val TAG = "WorkSocialForensics"

    fun recordSupabaseInitialization(success: Boolean, error: Throwable? = null) {
        if (!BuildConfig.DEBUG) return
        val status = if (success) "success" else "failure"
        Log.d(TAG, "supabase_init status=$status app_version=${BuildConfig.VERSION_NAME} build=${BuildConfig.VERSION_CODE} build_config_host=${safeHost(BuildConfig.SUPABASE_URL)}")
        if (error != null) recordException("supabase_init", error)
    }

    fun recordRequestFailure(error: Throwable) {
        if (!BuildConfig.DEBUG) return
        val message = sanitize(error.message.orEmpty())
        val host = extractHost(message)
        Log.e(TAG, "request_failure host=${host ?: "unknown"} exception=${error::class.java.name} message=$message")
        Log.e(TAG, "request_failure_stack=${sanitize(error.stackTraceToString())}")
    }

    private fun recordException(stage: String, error: Throwable) {
        Log.e(TAG, "$stage exception=${error::class.java.name} message=${sanitize(error.message.orEmpty())}")
        Log.e(TAG, "$stage stack=${sanitize(error.stackTraceToString())}")
    }

    private fun extractHost(message: String): String? {
        val candidates = Regex("(?:https?://)?([A-Za-z0-9.-]+\\.[A-Za-z]{2,})(?:[:/)]|\\s|$)")
            .findAll(message)
            .map { it.groupValues[1].trimEnd('.', ')') }
            .filter { it.contains('.') }
            .toList()
        return candidates.firstOrNull()
    }

    private fun safeHost(url: String): String = runCatching { URI(url).host ?: "invalid" }.getOrDefault("invalid")

    private fun sanitize(value: String): String = value
        .replace(Regex("(?i)bearer\\s+[A-Za-z0-9._-]+"), "Bearer [REDACTED]")
        .replace(Regex("(?i)(access[_-]?token|refresh[_-]?token|session[_-]?token|password)\\s*[=:]\\s*[^\\s,;]+"), "$1=[REDACTED]")
        .replace(Regex("(?i)eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"), "[JWT_REDACTED]")
}
