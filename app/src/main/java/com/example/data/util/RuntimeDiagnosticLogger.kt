package com.example.data.util

import com.example.data.model.ScreenRuntimeDiagnosticEvent
import com.example.data.model.UserRole
import java.text.SimpleDateFormat
import java.util.ArrayDeque
import java.util.Date
import java.util.Locale

/**
 * Lightweight in-memory ring buffer capturing navigation visits, runtime exceptions,
 * sync/network warnings, and failed user actions so the Super Admin Live Watcher can
 * identify non-working or broken features without background video recording.
 */
object RuntimeDiagnosticLogger {

    private const val MAX_EVENTS = 50
    private val lock = Any()
    private val events = ArrayDeque<ScreenRuntimeDiagnosticEvent>(MAX_EVENTS)

    @Volatile
    var currentScreenKey: String = "dashboard"
        private set

    @Volatile
    var currentScreenTitle: String = "Brohi Ice Factory"
        private set

    @Volatile
    var lastErrorEpochMillis: Long = 0L
        private set

    fun recordScreenVisit(screenKey: String, screenTitle: String, activeRole: UserRole) {
        currentScreenKey = screenKey
        currentScreenTitle = screenTitle
        appendEvent(
            screenKey = screenKey,
            level = "INFO",
            source = "Navigation",
            message = "Visited '$screenTitle' (route=$screenKey, role=${activeRole.label})"
        )
    }

    fun recordError(source: String, message: String, screenKey: String? = null) {
        val cleanMsg = message.trim()
        if (cleanMsg.isBlank()) return
        lastErrorEpochMillis = System.currentTimeMillis()
        appendEvent(
            screenKey = screenKey ?: currentScreenKey,
            level = "ERROR",
            source = source,
            message = cleanMsg
        )
    }

    fun recordWarning(source: String, message: String, screenKey: String? = null) {
        val cleanMsg = message.trim()
        if (cleanMsg.isBlank()) return
        lastErrorEpochMillis = System.currentTimeMillis()
        appendEvent(
            screenKey = screenKey ?: currentScreenKey,
            level = "WARN",
            source = source,
            message = cleanMsg
        )
    }

    fun getRecentEvents(limit: Int = 25): List<ScreenRuntimeDiagnosticEvent> = synchronized(lock) {
        events.toList().takeLast(limit)
    }

    fun getRecentErrorCount(): Int = synchronized(lock) {
        events.count { it.level == "ERROR" || it.level == "WARN" }
    }

    fun buildDiagnosticsSummary(targetScreenKey: String): String {
        val list = getRecentEvents(25)
        if (list.isEmpty()) return "No runtime errors or warnings recorded in this session."
        val timeFmt = SimpleDateFormat("HH:mm:ss", Locale.US)
        val screenSpecific = list.filter { it.screenKey == targetScreenKey || it.level == "ERROR" }
        val chosen = if (screenSpecific.isNotEmpty()) screenSpecific.takeLast(15) else list.takeLast(12)
        return chosen.joinToString("\n") { ev ->
            val ts = timeFmt.format(Date(ev.timestampMillis))
            "[$ts] [${ev.level}] (${ev.screenKey}/${ev.source}): ${ev.message}"
        }
    }

    fun clear() {
        synchronized(lock) {
            events.clear()
            lastErrorEpochMillis = 0L
        }
    }

    private fun appendEvent(screenKey: String, level: String, source: String, message: String) {
        synchronized(lock) {
            if (events.size >= MAX_EVENTS) {
                events.removeFirst()
            }
            events.addLast(
                ScreenRuntimeDiagnosticEvent(
                    timestampMillis = System.currentTimeMillis(),
                    screenKey = screenKey,
                    level = level,
                    source = source,
                    message = message.take(280)
                )
            )
        }
    }
}
