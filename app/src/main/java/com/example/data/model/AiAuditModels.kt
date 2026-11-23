package com.example.data.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class AuditSeverity(val label: String) {
    HIGH("High"),
    MEDIUM("Medium"),
    LOW("Low");

    companion object {
        fun fromString(raw: String?): AuditSeverity = when (raw?.trim()?.uppercase()) {
            "HIGH", "CRITICAL" -> HIGH
            "LOW" -> LOW
            else -> MEDIUM
        }
    }
}

@Serializable
enum class AuditCategory(val label: String) {
    UI_LAYOUT("UI & Layout"),
    BROKEN_FEATURE("Error / Broken Feature"),
    UX_WORKFLOW("Corporate UX Standard");

    companion object {
        fun fromString(raw: String?): AuditCategory = when (raw?.trim()?.uppercase()) {
            "BROKEN_FEATURE", "ERROR", "FUNCTIONAL" -> BROKEN_FEATURE
            "UX_WORKFLOW", "UX", "WORKFLOW" -> UX_WORKFLOW
            else -> UI_LAYOUT
        }
    }
}

@Serializable
enum class AuditIssueStatus(val label: String) {
    PENDING("Pending"),
    APPLIED("Applied"),
    IGNORED("Ignored")
}

@Serializable
data class AiAuditFinding(
    val id: String,
    val screenKey: String,
    val screenTitle: String,
    val issueTitle: String,
    val severity: AuditSeverity,
    val category: AuditCategory,
    val explanationEn: String,
    val explanationUr: String,
    val correctionPromptEn: String,
    val status: AuditIssueStatus = AuditIssueStatus.PENDING,
    val capturedAtMillis: Long = System.currentTimeMillis(),
    val sourceMode: String = "INSTANT_CHECK"
)

data class ScreenRuntimeDiagnosticEvent(
    val timestampMillis: Long,
    val screenKey: String,
    val level: String,
    val source: String,
    val message: String
)

data class AiScreenAuditReport(
    val screenKey: String,
    val screenTitle: String,
    val summaryEn: String,
    val summaryUr: String,
    val findings: List<AiAuditFinding>,
    val auditedAtMillis: Long = System.currentTimeMillis()
)

object AiAuditSerializer {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encodeFindings(list: List<AiAuditFinding>): String {
        return runCatching { json.encodeToString(list) }.getOrDefault("[]")
    }

    fun decodeFindings(raw: String): List<AiAuditFinding> {
        if (raw.isBlank()) return emptyList()
        return runCatching { json.decodeFromString<List<AiAuditFinding>>(raw) }.getOrDefault(emptyList())
    }
}
