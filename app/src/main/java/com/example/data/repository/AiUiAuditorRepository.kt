package com.example.data.repository

import android.graphics.Bitmap
import android.util.Log
import com.example.data.model.AiAuditFinding
import com.example.data.model.AiScreenAuditReport
import com.example.data.model.AuditCategory
import com.example.data.model.AuditIssueStatus
import com.example.data.model.AuditSeverity
import com.example.data.model.UserRole
import com.example.data.util.RuntimeDiagnosticLogger
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "AiUiAuditorRepository"

class AiUiAuditorRepository {

    private val auditSchema = Schema.obj(
        properties = mapOf(
            "summaryEn" to Schema.string(
                description = "1-2 sentence overall English quality verdict of this screen or dialog"
            ),
            "summaryUr" to Schema.string(
                description = "1-2 sentence overall Urdu (اردو رسم الخط) quality verdict of this screen or dialog"
            ),
            "findings" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "issueTitle" to Schema.string(
                            description = "Concise title of the UI, functional, or UX issue"
                        ),
                        "severity" to Schema.enumeration(
                            values = listOf("HIGH", "MEDIUM", "LOW"),
                            description = "HIGH for broken features/blocked taps/severe overlaps, MEDIUM for layout/sizing/UX gaps, LOW for visual polish"
                        ),
                        "category" to Schema.enumeration(
                            values = listOf("UI_LAYOUT", "BROKEN_FEATURE", "UX_WORKFLOW"),
                            description = "Category of the issue"
                        ),
                        "explanationEn" to Schema.string(
                            description = "Detailed English explanation of what is wrong and exactly how it should look or work under top corporate/government app standards"
                        ),
                        "explanationUr" to Schema.string(
                            description = "Detailed natural Urdu (اردو) explanation describing the problem and how it should be designed/fixed"
                        ),
                        "correctionPromptEn" to Schema.string(
                            description = "Complete, copy-ready English instruction prompt that a Super Admin can paste directly to a coding agent to fix this exact issue cleanly in Jetpack Compose / Kotlin"
                        )
                    )
                ),
                description = "Up to 5 most important UI, diagnostic/broken-feature, and UX workflow findings for this screen"
            )
        )
    )

    private val systemPrompt = content {
        text(
            """
            You are an elite Principal Android UI/UX Architect & Live QA Auditor inspecting "Brohi Ice Factory", a Jetpack Compose Material 3 Crystal-Glass business management app in Pakistan (English + Urdu RTL bilingual UI).
            Your ONLY job is advisory inspection and prompt generation for the Super Admin — never pretend to apply edits yourself.

            Analyze BOTH the attached in-app screenshot (if provided) AND the live Runtime Diagnostics log lines against:
            1. Button Sizing, Touch Targets & Layout Polish:
               - Check if buttons are disproportionately huge, too tiny (< 48dp touch height), misaligned, awkwardly stretched, or inconsistent with Crystal-Glass styling.
               - Check for clipped/truncated text, cramped columns, overlapping Urdu + English bilingual labels, system navigation/status bar overlap, or wasted empty space.
            2. Non-Working or Incomplete Features (via Runtime Diagnostics):
               - Look closely at any ERROR/WARN lines in Runtime Diagnostics (e.g., Firestore permission failures, AI timeouts, OCR/Face/Voice failures, sync errors, dead/failed actions) and flag them as BROKEN_FEATURE with HIGH severity.
            3. International Government & Top Corporate App Standards:
               - Evaluate visual hierarchy, WCAG contrast, scannability, single-focus card grouping, error recovery affordances, and enterprise workflow polish.

            Output Rules:
            - Return ONLY JSON matching the schema.
            - Prioritize up to 5 highest-impact, genuinely visible or logged issues (avoid nitpicking things that already look clean).
            - For each finding provide BOTH clear English (`explanationEn`) and natural Urdu (`explanationUr`) explanations.
            - `correctionPromptEn` MUST be written in clear, actionable English so the Super Admin can copy-paste it directly into their AI code editor to fix the exact UI/logic issue.
            - Do NOT invent errors that are neither visible in the screenshot nor present in the runtime diagnostics.
            """.trimIndent()
        )
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = auditSchema
            },
            systemInstruction = systemPrompt
        )
    }

    suspend fun auditScreen(
        screenKey: String,
        screenTitle: String,
        activeRole: UserRole,
        isDarkMode: Boolean,
        appLanguage: String,
        diagnosticsSummary: String,
        screenshot: Bitmap?,
        sourceMode: String
    ): Result<AiScreenAuditReport> {
        return try {
            val contextPrompt = buildString {
                appendLine("Audit Target Screen: $screenTitle (route key: $screenKey)")
                appendLine("Audit Mode: $sourceMode")
                appendLine("Active Role View: ${activeRole.label}")
                appendLine("Theme: ${if (isDarkMode) "Dark Blue" else "Crystal Ice Light"}, App Language: $appLanguage")
                appendLine()
                appendLine("--- RECENT RUNTIME DIAGNOSTICS LOG ---")
                appendLine(diagnosticsSummary)
                appendLine("--- END LOG ---")
                if (screenshot != null) {
                    appendLine("Inspect the attached live screen snapshot carefully for button sizing/height, text clipping, alignment, spacing, overlaps, and visual polish alongside the diagnostics above.")
                } else {
                    appendLine("No bitmap snapshot was captured for this step; audit based on the screen role and runtime diagnostics above.")
                }
            }

            val response = withTimeout(60_000L) {
                model.generateContent(
                    content {
                        if (screenshot != null) {
                            image(screenshot)
                        }
                        text(contextPrompt)
                    }
                )
            }

            val rawJson = response.text?.trim()
                ?: throw IllegalStateException("Gemini returned an empty audit response.")

            val root = JSONObject(rawJson)
            val summaryEn = root.optString("summaryEn", "Screen audit completed.")
            val summaryUr = root.optString("summaryUr", "اسکرین کا آڈٹ مکمل ہو گیا۔")
            val arr = root.optJSONArray("findings")
            val now = System.currentTimeMillis()
            val findings = mutableListOf<AiAuditFinding>()
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val title = obj.optString("issueTitle", "").trim()
                    if (title.isBlank()) continue
                    findings += AiAuditFinding(
                        id = "aud_${now}_${UUID.randomUUID().toString().take(6)}",
                        screenKey = screenKey,
                        screenTitle = screenTitle,
                        issueTitle = title,
                        severity = AuditSeverity.fromString(obj.optString("severity")),
                        category = AuditCategory.fromString(obj.optString("category")),
                        explanationEn = obj.optString("explanationEn", "").trim(),
                        explanationUr = obj.optString("explanationUr", "").trim(),
                        correctionPromptEn = obj.optString("correctionPromptEn", "").trim(),
                        status = AuditIssueStatus.PENDING,
                        capturedAtMillis = now,
                        sourceMode = sourceMode
                    )
                }
            }

            Result.success(
                AiScreenAuditReport(
                    screenKey = screenKey,
                    screenTitle = screenTitle,
                    summaryEn = summaryEn,
                    summaryUr = summaryUr,
                    findings = findings,
                    auditedAtMillis = now
                )
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "AI Screen Audit failed for $screenKey", e)
            RuntimeDiagnosticLogger.recordError("AiUiAuditor", e.localizedMessage ?: "Audit failed", screenKey)
            Result.failure(e)
        }
    }
}
