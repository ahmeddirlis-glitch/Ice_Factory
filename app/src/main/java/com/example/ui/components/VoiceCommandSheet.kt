package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.model.ParsedVoiceIntent
import com.example.data.util.AppStrings
import com.example.data.util.PakistaniFormatters
import com.example.data.voice.SpeechUiState
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceCommandBottomSheet(
    speechState: SpeechUiState,
    voiceUiState: com.example.ui.viewmodel.VoiceUiState,
    strings: AppStrings,
    onSelectLocale: (String) -> Unit,
    onToggleAutoSave: (Boolean) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onSubmitTranscript: (String) -> Unit,
    onUpdateIntent: (ParsedVoiceIntent) -> Unit,
    onRemoveIntent: (String) -> Unit,
    onConfirmAndSave: () -> Unit,
    onCancelPending: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var manualInput by remember { mutableStateOf("") }

    // Reflect live partial/final speech text into input box
    LaunchedEffect(speechState.partialText, speechState.finalText) {
        val latest = speechState.finalText.ifBlank { speechState.partialText }
        if (latest.isNotBlank()) {
            manualInput = latest
        }
    }

    // Automatically submit captured speech as soon as recognition completes or promotes partial results
    LaunchedEffect(speechState.resultVersion) {
        if (speechState.resultVersion > 0L && speechState.finalText.isNotBlank()) {
            val normalized = PakistaniFormatters.normalizeVoiceTranscript(speechState.finalText)
            manualInput = normalized
            keyboardController?.hide()
            onSubmitTranscript(normalized)
        }
    }

    // Clear input after a confirmed entry receipt is saved
    LaunchedEffect(voiceUiState.spokenAnswer) {
        if (voiceUiState.spokenAnswer?.startsWith("✓") == true && voiceUiState.pendingIntents.isEmpty()) {
            manualInput = ""
        }
    }

    val submitCurrentInput: () -> Unit = {
        val normalized = PakistaniFormatters.normalizeVoiceTranscript(manualInput)
        if (normalized.isNotBlank()) {
            manualInput = normalized
            keyboardController?.hide()
            onSubmitTranscript(normalized)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartListening()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = RoyalBlue)
                    Text(
                        text = strings.voiceCommandTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("voice_close_button")) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Voice Sheet")
                }
            }

            // Language Chips + Confirmation Pop-up / Auto-Save mode toggle
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("ur-PK" to "Urdu", "en-IN" to "English", "hi-IN" to "Hindi").forEach { (tag, label) ->
                    FilterChip(
                        selected = speechState.selectedLocaleTag == tag,
                        onClick = { onSelectLocale(tag) },
                        label = { Text(label, maxLines = 1, softWrap = false) }
                    )
                }
                FilterChip(
                    selected = voiceUiState.autoSaveEnabled,
                    onClick = { onToggleAutoSave(!voiceUiState.autoSaveEnabled) },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Bolt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            if (voiceUiState.autoSaveEnabled) "Auto-Save: ON" else "Confirm Pop-up: ON",
                            maxLines = 1,
                            softWrap = false
                        )
                    },
                    modifier = Modifier.testTag("voice_auto_save_chip")
                )
            }

            // Central Mic Trigger
            val micBg by animateColorAsState(
                if (speechState.isListening) DangerRed else RoyalBlue,
                label = "micColor"
            )
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(micBg)
                        .clickable {
                            if (speechState.isListening) {
                                onStopListening()
                            } else {
                                val granted = ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.RECORD_AUDIO
                                ) == PackageManager.PERMISSION_GRANTED
                                if (granted) onStartListening()
                                else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                        .testTag("voice_sheet_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (speechState.isListening) Icons.Filled.Stop else Icons.Filled.Mic,
                        contentDescription = if (speechState.isListening) "Stop listening" else "Start listening",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when {
                        speechState.isListening -> "Listening... Tap Stop when done speaking"
                        voiceUiState.autoSaveEnabled -> "Auto-saves directly to Expenses, Sales, or Credit Recovery"
                        else -> "Speak or type below — a confirmation pop-up opens before saving"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (!speechState.errorMessage.isNullOrBlank()) {
                Text(
                    text = speechState.errorMessage,
                    color = DangerRed,
                    fontSize = 12.sp
                )
            }

            // Manual / Transcript Text Box with IME Send + Trailing Send Icon
            CrystalTextField(
                value = manualInput,
                onValueChange = { manualInput = it },
                englishLabel = strings.speakOrTypeHint,
                urduLabel = "آواز یا ٹیکسٹ لکھیں",
                leadingIcon = Icons.Filled.Mic,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { submitCurrentInput() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("voice_transcript_input"),
                trailingIcon = {
                    IconButton(
                        onClick = submitCurrentInput,
                        enabled = manualInput.isNotBlank() && !voiceUiState.isParsing,
                        modifier = Modifier.testTag("voice_parse_button")
                    ) {
                        Icon(Icons.Filled.Send, contentDescription = "Process voice command", tint = RoyalBlue)
                    }
                }
            )

            // Prominent Primary Action Button so users never miss how to process & record typed/spoken text
            IcyGlossyButton(
                onClick = submitCurrentInput,
                enabled = manualInput.isNotBlank() && !voiceUiState.isParsing && !speechState.isListening,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("voice_analyze_action_button")
            ) {
                Icon(
                    Icons.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (voiceUiState.autoSaveEnabled) {
                        "Analyze & Save Entry (انٹری محفوظ کریں)"
                    } else {
                        "Analyze & Open Confirmation (انٹری چیک کریں)"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Quick Sample Hints
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "Rs. 75,000 electricity bill",
                    "Rs. 15,000 ki baraf sale hui",
                    "Al-Madina ne Rs. 10,000 udhar diya"
                ).forEach { sample ->
                    Surface(
                        onClick = {
                            val normalized = PakistaniFormatters.normalizeVoiceTranscript(sample)
                            manualInput = normalized
                            keyboardController?.hide()
                            onSubmitTranscript(normalized)
                        },
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = sample,
                            fontSize = 10.sp,
                            lineHeight = 13.sp,
                            modifier = Modifier.padding(8.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (voiceUiState.isParsing) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Analyzing command...", fontSize = 13.sp)
                }
            }

            if (!voiceUiState.spokenAnswer.isNullOrBlank()) {
                val isReceipt = voiceUiState.spokenAnswer.startsWith("✓")
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SuccessGreenSoft,
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isReceipt) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = SuccessGreen,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Text(
                            text = voiceUiState.spokenAnswer,
                            color = Color(0xFF0B4F24),
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (!voiceUiState.errorMessage.isNullOrBlank() && voiceUiState.pendingIntents.isEmpty()) {
                Text(
                    text = voiceUiState.errorMessage,
                    color = DangerRed,
                    fontSize = 13.sp
                )
            }
        }
    }

    // Dedicated Confirmation Pop-up Box shown prominently on top of screen whenever entries are parsed
    if (voiceUiState.pendingIntents.isNotEmpty()) {
        VoiceConfirmationPopupDialog(
            intents = voiceUiState.pendingIntents,
            isSaving = voiceUiState.isSaving,
            errorMessage = voiceUiState.errorMessage,
            strings = strings,
            onUpdateIntent = onUpdateIntent,
            onRemoveIntent = onRemoveIntent,
            onConfirmAndSave = onConfirmAndSave,
            onDismiss = onCancelPending
        )
    }
}

@Composable
private fun VoiceConfirmationPopupDialog(
    intents: List<ParsedVoiceIntent>,
    isSaving: Boolean,
    errorMessage: String?,
    strings: AppStrings,
    onUpdateIntent: (ParsedVoiceIntent) -> Unit,
    onRemoveIntent: (String) -> Unit,
    onConfirmAndSave: () -> Unit,
    onDismiss: () -> Unit
) {
    CrystalFormDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        title = "Confirm Voice Entry (${intents.size})",
        subtitle = "محفوظ کرنے سے پہلے انٹری کی تصدیق کریں",
        icon = Icons.Filled.CheckCircle,
        confirmText = if (isSaving) "Saving..." else strings.confirmAndSave,
        confirmEnabled = !isSaving,
        dismissText = strings.cancel,
        confirmTestTag = "voice_confirm_save_button",
        dismissTestTag = "voice_confirm_cancel_button",
        onConfirm = onConfirmAndSave
    ) {
        if (!errorMessage.isNullOrBlank()) {
            Text(
                text = errorMessage,
                color = DangerRed,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
        intents.forEach { intent ->
            EditableIntentCard(
                intent = intent,
                onUpdate = onUpdateIntent,
                onDelete = { onRemoveIntent(intent.id) }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditableIntentCard(
    intent: ParsedVoiceIntent,
    onUpdate: (ParsedVoiceIntent) -> Unit,
    onDelete: () -> Unit
) {
    var desc by remember(intent.id) { mutableStateOf(intent.description) }
    var amtText by remember(intent.id) {
        mutableStateOf(
            when {
                intent.intentType == "PRODUCTION" && intent.tons > 0 -> intent.tons.toString()
                intent.amount > 0 -> intent.amount.toLong().toString()
                else -> ""
            }
        )
    }
    var category by remember(intent.id) { mutableStateOf(intent.category) }
    var customerOrSource by remember(intent.id) { mutableStateOf(intent.customerOrSourceName) }

    val ledgerLabel = when (intent.intentType) {
        "SALE" -> "Cash Sale (فروخت)"
        "CREDIT_SALE" -> "Credit Sale (ادھار فروخت)"
        "CREDIT_PAYMENT" -> "Credit Recovery (وصولی)"
        "EXPENSE" -> "Expense (اخراجات)"
        "FUNDING" -> "Funding (فنڈنگ)"
        "FACTORY_BUILD" -> "Factory Build (تعمیر)"
        "PRODUCTION" -> "Production (پیداوار)"
        else -> intent.intentType
    }

    BrohiCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = RoyalBlue.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = ledgerLabel,
                        color = DeepBlue,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                if (intent.intentType == "PRODUCTION" && intent.tons > 0) {
                    Text(
                        text = PakistaniFormatters.formatTons(intent.tons),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = RoyalBlue
                    )
                } else if (intent.amount > 0) {
                    Text(
                        text = PakistaniFormatters.formatPkr(intent.amount),
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = SuccessGreen
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove item", tint = DangerRed, modifier = Modifier.size(18.dp))
                }
            }

            // Quick Entry Type Switcher (lets user switch Expense <-> Sale <-> Credit if needed before saving)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "EXPENSE" to "Expense",
                    "SALE" to "Sale",
                    "CREDIT_SALE" to "Credit Sale",
                    "CREDIT_PAYMENT" to "Recovery",
                    "PRODUCTION" to "Production"
                ).forEach { (typeKey, shortLabel) ->
                    FilterChip(
                        selected = intent.intentType == typeKey,
                        onClick = {
                            val nextCat = when {
                                typeKey == "EXPENSE" && category.isBlank() -> "Other"
                                typeKey == "SALE" -> "Ice Sale"
                                typeKey == "PRODUCTION" -> "Production"
                                else -> category
                            }
                            category = nextCat
                            onUpdate(intent.copy(intentType = typeKey, category = nextCat))
                        },
                        label = { Text(shortLabel, fontSize = 11.sp, maxLines = 1, softWrap = false) }
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))

            CrystalTextField(
                value = desc,
                onValueChange = {
                    desc = it
                    onUpdate(intent.copy(description = it))
                },
                englishLabel = "Description",
                urduLabel = "تفصیل",
                leadingIcon = Icons.Filled.Description,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (intent.intentType in listOf("EXPENSE", "FUNDING")) {
                    CrystalTextField(
                        value = category,
                        onValueChange = {
                            category = it
                            onUpdate(intent.copy(category = it))
                        },
                        englishLabel = "Category",
                        leadingIcon = Icons.Filled.Category,
                        modifier = Modifier.weight(1f)
                    )
                } else if (intent.intentType in listOf("SALE", "CREDIT_SALE", "CREDIT_PAYMENT")) {
                    CrystalTextField(
                        value = customerOrSource,
                        onValueChange = {
                            customerOrSource = it
                            onUpdate(intent.copy(customerOrSourceName = it))
                        },
                        englishLabel = "Customer",
                        leadingIcon = Icons.Filled.Person,
                        modifier = Modifier.weight(1f)
                    )
                }
                CrystalTextField(
                    value = amtText,
                    onValueChange = { raw ->
                        val cleaned = PakistaniFormatters.convertUrduDigits(raw)
                        amtText = cleaned
                        val parsed = cleaned.toDoubleOrNull() ?: 0.0
                        if (intent.intentType == "PRODUCTION") {
                            onUpdate(intent.copy(tons = parsed))
                        } else {
                            onUpdate(intent.copy(amount = parsed))
                        }
                    },
                    englishLabel = if (intent.intentType == "PRODUCTION") "Tons" else "Amount (Rs.)",
                    leadingIcon = if (intent.intentType == "PRODUCTION") Icons.Filled.Scale else Icons.Filled.Payments,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
