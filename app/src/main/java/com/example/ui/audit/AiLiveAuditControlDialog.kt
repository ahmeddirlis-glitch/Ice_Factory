package com.example.ui.audit

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.DoNotDisturbOn
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AiAuditFinding
import com.example.data.model.AuditIssueStatus
import com.example.data.model.AuditSeverity
import com.example.data.util.RuntimeDiagnosticLogger
import com.example.ui.components.IcyGlossyButton
import com.example.ui.components.PrimaryBlueGradient
import com.example.ui.theme.CrystalBorderLight
import com.example.ui.theme.CrystalIceBgMid
import com.example.ui.theme.CrystalIceBgTop
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedSoft
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.WarningOrangeSoft
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AiLiveAuditControlDialog(
    uiState: AiAuditUiState,
    autoWatchEnabled: Boolean,
    explainLanguage: String,
    initialTab: Int = 0,
    onToggleAuditEnabled: (Boolean) -> Unit,
    onToggleAutoWatch: (Boolean) -> Unit,
    onChangeExplainLanguage: (String) -> Unit,
    onScanCurrentScreen: () -> Unit,
    onUpdateFindingStatus: (findingId: String, status: AuditIssueStatus) -> Unit,
    onClearAllFindings: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    var statusFilter by remember { mutableStateOf<AuditIssueStatus?>(AuditIssueStatus.PENDING) }

    val currentScreenFindings = remember(uiState.findings, uiState.activeScreenKey) {
        uiState.findings.filter { it.screenKey == uiState.activeScreenKey }
    }
    val inboxFindings = remember(uiState.findings, statusFilter) {
        if (statusFilter == null) uiState.findings else uiState.findings.filter { it.status == statusFilter }
    }
    val displayedFindings = if (selectedTab == 0) currentScreenFindings else inboxFindings
    val errorCount = remember(uiState.isAuditing, uiState.activeScreenKey) {
        RuntimeDiagnosticLogger.getRecentErrorCount()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFFF3F8FF),
            border = BorderStroke(1.2.dp, CrystalBorderLight),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .widthIn(max = 500.dp)
                .statusBarsPadding()
                .navigationBarsPadding()
                .heightIn(max = 740.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Crystal Header with Direct Turn Off Switch
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryBlueGradient)
                        .padding(start = 14.dp, end = 6.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(RoundedCornerShape(11.dp))
                                .background(Color.White.copy(alpha = 0.18f))
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "AI Editor & Prompt Generator",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Super Admin Live Watcher • ${uiState.activeScreenTitle}",
                                color = Color(0xFFDCE8FF),
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Immediate ON/OFF Toggle from Live Panel
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text("ON", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            Switch(
                                checked = true,
                                onCheckedChange = { checked ->
                                    if (!checked) {
                                        onToggleAuditEnabled(false)
                                        onDismiss()
                                    }
                                },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.White,
                                    checkedTrackColor = SuccessGreen
                                ),
                                modifier = Modifier.testTag("live_panel_audit_toggle")
                            )
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close audit panel", tint = Color.White)
                        }
                    }
                }

                // Controls Bar: Auto-Watch, Explanation Language (Urdu / English), Scan Now
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(CrystalIceBgTop, CrystalIceBgMid)))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Visibility, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                            Column {
                                Text("Auto-Watch Mode (خودکار نگرانی)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                                Text(
                                    text = if (autoWatchEnabled) "Quietly audits visited screens • $errorCount log alerts" else "Manual Instant Check only",
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                        Switch(
                            checked = autoWatchEnabled,
                            onCheckedChange = onToggleAutoWatch,
                            modifier = Modifier.testTag("panel_autowatch_switch")
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = explainLanguage == "ur",
                                onClick = { onChangeExplainLanguage("ur") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue, selectedLabelColor = Color.White),
                                label = { Text("اردو وضاحت", fontSize = 11.sp) },
                                modifier = Modifier.testTag("audit_lang_ur")
                            )
                            FilterChip(
                                selected = explainLanguage == "en",
                                onClick = { onChangeExplainLanguage("en") },
                                colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue, selectedLabelColor = Color.White),
                                label = { Text("English", fontSize = 11.sp) },
                                modifier = Modifier.testTag("audit_lang_en")
                            )
                        }

                        IcyGlossyButton(
                            onClick = onScanCurrentScreen,
                            enabled = !uiState.isAuditing,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                            modifier = Modifier
                                .heightIn(min = 42.dp)
                                .testTag("instant_screen_check_button")
                        ) {
                            if (uiState.isAuditing) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scanning...", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Scan Now", color = Color.White, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                TabRow(selectedTabIndex = selectedTab) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("This Screen (${currentScreenFindings.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Audit Inbox (${uiState.pendingCount})", fontSize = 12.sp, fontWeight = FontWeight.Bold) }
                    )
                }

                if (selectedTab == 1) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.White)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            listOf(
                                AuditIssueStatus.PENDING to "Pending",
                                AuditIssueStatus.APPLIED to "Applied",
                                AuditIssueStatus.IGNORED to "Ignored",
                                null to "All"
                            ).forEach { (st, lbl) ->
                                FilterChip(
                                    selected = statusFilter == st,
                                    onClick = { statusFilter = st },
                                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DeepBlue, selectedLabelColor = Color.White),
                                    label = { Text(lbl, fontSize = 10.sp) }
                                )
                            }
                        }
                        if (uiState.findings.isNotEmpty()) {
                            IconButton(onClick = onClearAllFindings, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Filled.DeleteSweep, contentDescription = "Clear Inbox", tint = DangerRed, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Body List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f, fill = true)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!uiState.lastErrorMessage.isNullOrBlank()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = DangerRedSoft,
                                border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(Icons.Filled.ErrorOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                                    Text(uiState.lastErrorMessage, fontSize = 11.5.sp, color = Color(0xFF991B1B), fontWeight = FontWeight.Medium)
                                }
                            }
                        }
                    }

                    if (selectedTab == 0 && (!uiState.latestSummaryEn.isNullOrBlank() || !uiState.latestSummaryUr.isNullOrBlank())) {
                        item {
                            val verdict = if (explainLanguage == "ur") {
                                uiState.latestSummaryUr ?: uiState.latestSummaryEn.orEmpty()
                            } else {
                                uiState.latestSummaryEn ?: uiState.latestSummaryUr.orEmpty()
                            }
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, CrystalBorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = if (explainLanguage == "ur") "اسکرین کا مجموعی جائزہ" else "Latest Screen Summary",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = RoyalBlue
                                    )
                                    Text(verdict, fontSize = 12.sp, color = DeepBlue, lineHeight = 17.sp)
                                }
                            }
                        }
                    }

                    if (displayedFindings.isEmpty()) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = Color.White,
                                border = BorderStroke(1.dp, CrystalBorderLight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.AutoFixHigh, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(28.dp))
                                    Text(
                                        text = if (uiState.isAuditing) {
                                            "Analyzing screen layout & runtime logs..."
                                        } else if (selectedTab == 0) {
                                            "Tap 'Scan Now' to run a Gemini UI & Runtime Diagnostics audit on this screen."
                                        } else {
                                            "No audit items match this filter yet."
                                        },
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = DeepBlue
                                    )
                                }
                            }
                        }
                    } else {
                        items(displayedFindings, key = { it.id }) { finding ->
                            AuditFindingCard(
                                finding = finding,
                                explainLanguage = explainLanguage,
                                onCopyPrompt = { prompt ->
                                    clipboard.setText(AnnotatedString(prompt))
                                    Toast.makeText(context, "English prompt copied!", Toast.LENGTH_SHORT).show()
                                },
                                onChangeStatus = { newStatus ->
                                    onUpdateFindingStatus(finding.id, newStatus)
                                }
                            )
                        }
                    }
                }

                HorizontalDivider(color = CrystalBorderLight)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Advisory & Prompt Only • Device Local",
                        fontSize = 10.5.sp,
                        color = Color(0xFF475569),
                        fontWeight = FontWeight.Medium
                    )
                    TextButton(onClick = onDismiss) {
                        Text("Close", fontWeight = FontWeight.Bold, color = DeepBlue)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AuditFindingCard(
    finding: AiAuditFinding,
    explainLanguage: String,
    onCopyPrompt: (String) -> Unit,
    onChangeStatus: (AuditIssueStatus) -> Unit
) {
    var copiedRecently by remember(finding.id) { mutableStateOf(false) }
    val (sevBg, sevText) = when (finding.severity) {
        AuditSeverity.HIGH -> DangerRedSoft to DangerRed
        AuditSeverity.MEDIUM -> WarningOrangeSoft to WarningOrange
        AuditSeverity.LOW -> Color(0xFFEAF2FF) to RoyalBlue
    }
    val timeLabel = remember(finding.capturedAtMillis) {
        SimpleDateFormat("HH:mm", Locale.US).format(Date(finding.capturedAtMillis))
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(
            width = 1.dp,
            color = when (finding.status) {
                AuditIssueStatus.APPLIED -> SuccessGreen.copy(alpha = 0.45f)
                AuditIssueStatus.IGNORED -> Color(0xFFCBD5E1)
                AuditIssueStatus.PENDING -> CrystalBorderLight
            }
        ),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // 1. Issue & Severity Badges
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Surface(shape = RoundedCornerShape(6.dp), color = sevBg) {
                    Text(
                        text = finding.severity.label.uppercase(),
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = sevText,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFF1F5F9)) {
                    Text(
                        text = finding.category.label,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepBlue,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFE0F2FE)) {
                    Text(
                        text = "${finding.screenTitle} • $timeLabel",
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0369A1),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = when (finding.status) {
                        AuditIssueStatus.APPLIED -> SuccessGreenSoft
                        AuditIssueStatus.IGNORED -> Color(0xFFF1F5F9)
                        AuditIssueStatus.PENDING -> WarningOrangeSoft
                    }
                ) {
                    Text(
                        text = finding.status.label,
                        fontSize = 9.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (finding.status) {
                            AuditIssueStatus.APPLIED -> SuccessGreen
                            AuditIssueStatus.IGNORED -> Color(0xFF64748B)
                            AuditIssueStatus.PENDING -> WarningOrange
                        },
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Text(
                text = finding.issueTitle,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.ExtraBold,
                color = DeepBlue
            )

            // 2. Explanation (Urdu or English toggle)
            val explanation = if (explainLanguage == "ur") {
                finding.explanationUr.ifBlank { finding.explanationEn }
            } else {
                finding.explanationEn.ifBlank { finding.explanationUr }
            }
            Text(
                text = explanation,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 17.sp
            )

            // 3. Copy-Ready English Correction Prompt
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Correction Prompt (English)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = RoyalBlue
                        )
                        OutlinedButton(
                            onClick = {
                                onCopyPrompt(finding.correctionPromptEn)
                                copiedRecently = true
                            },
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 32.dp)
                        ) {
                            Icon(
                                imageVector = if (copiedRecently) Icons.Filled.CheckCircle else Icons.Filled.ContentCopy,
                                contentDescription = "Copy Prompt",
                                tint = if (copiedRecently) SuccessGreen else DeepBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (copiedRecently) "Copied" else "Copy",
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (copiedRecently) SuccessGreen else DeepBlue
                            )
                        }
                    }
                    Text(
                        text = finding.correctionPromptEn,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF0F172A),
                        lineHeight = 15.sp
                    )
                }
            }

            // Status Action Buttons: Applied / Ignore / Reopen
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (finding.status != AuditIssueStatus.APPLIED) {
                    OutlinedButton(
                        onClick = { onChangeStatus(AuditIssueStatus.APPLIED) },
                        border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Applied", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = SuccessGreen)
                    }
                }
                if (finding.status != AuditIssueStatus.IGNORED) {
                    OutlinedButton(
                        onClick = { onChangeStatus(AuditIssueStatus.IGNORED) },
                        border = BorderStroke(1.dp, Color(0xFF94A3B8)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp)
                    ) {
                        Icon(Icons.Filled.DoNotDisturbOn, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Ignore", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B))
                    }
                }
                if (finding.status != AuditIssueStatus.PENDING) {
                    OutlinedButton(
                        onClick = { onChangeStatus(AuditIssueStatus.PENDING) },
                        border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.45f)),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.weight(1f).heightIn(min = 40.dp)
                    ) {
                        Text("Reopen", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = RoyalBlue)
                    }
                }
            }
        }
    }
}
