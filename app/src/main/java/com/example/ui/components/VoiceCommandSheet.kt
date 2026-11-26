package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Hearing
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Stop
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun VoiceCommandBottomSheet(
    speechState: SpeechUiState,
    voiceUiState: com.example.ui.viewmodel.VoiceUiState,
    userName: String,
    heyIceWakeEnabled: Boolean,
    strings: AppStrings,
    onSelectLocale: (String) -> Unit,
    onToggleAutoSave: (Boolean) -> Unit,
    onToggleHeyIceWake: (Boolean) -> Unit,
    onStartListening: () -> Unit,
    onStopListening: () -> Unit,
    onStopSpeaking: () -> Unit,
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
            manualInput = ""
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
            manualInput = ""
            keyboardController?.hide()
            onSubmitTranscript(normalized)
        }
    }

    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartListening()
    }

    val wakePermissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        val micGranted = perms[Manifest.permission.RECORD_AUDIO] == true ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        if (micGranted) {
            onToggleHeyIceWake(true)
        }
    }

    val firstName = remember(userName) { userName.trim().substringBefore(" ").ifBlank { "Friend" } }

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
                    Icon(Icons.Filled.AutoAwesome, contentDescription = null, tint = RoyalBlue)
                    Column {
                        Text(
                            text = "Brohi Ice Voice Assistant",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "English • اردو • हिन्दी  |  Hi $firstName",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.testTag("voice_close_button")) {
                    Icon(Icons.Filled.Close, contentDescription = "Close Voice Sheet")
                }
            }

            // Language Chips + Hey Ice Wake-Word + Auto-Save mode
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("ur-PK" to "اردو / Urdu", "hi-IN" to "हिन्दी / Hindi", "en-IN" to "English").forEach { (tag, label) ->
                    FilterChip(
                        selected = speechState.selectedLocaleTag == tag,
                        onClick = { onSelectLocale(tag) },
                        label = { Text(label, maxLines = 1, softWrap = false, fontSize = 12.sp) }
                    )
                }
                FilterChip(
                    selected = heyIceWakeEnabled,
                    onClick = {
                        if (heyIceWakeEnabled) {
                            onToggleHeyIceWake(false)
                        } else {
                            val needed = buildList {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                                    add(Manifest.permission.RECORD_AUDIO)
                                }
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                                ) {
                                    add(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                            if (needed.isEmpty()) onToggleHeyIceWake(true)
                            else wakePermissionsLauncher.launch(needed.toTypedArray())
                        }
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Filled.Hearing,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = {
                        Text(
                            if (heyIceWakeEnabled) "Hey Ice: ON" else "Hey Ice: OFF",
                            maxLines = 1,
                            softWrap = false,
                            fontSize = 12.sp
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SuccessGreenSoft,
                        selectedLabelColor = Color(0xFF0B4F24),
                        selectedLeadingIconColor = SuccessGreen
                    ),
                    modifier = Modifier.testTag("voice_wake_word_chip")
                )
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
                            softWrap = false,
                            fontSize = 12.sp
                        )
                    },
                    modifier = Modifier.testTag("voice_auto_save_chip")
                )
            }

            // Conversational Chat History (Assistant + User multi-turn)
            if (voiceUiState.conversationTurns.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF3F8FF),
                    border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.2f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        voiceUiState.conversationTurns.takeLast(6).forEach { turn ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = if (turn.isUser) Arrangement.End else Arrangement.Start
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(
                                        topStart = 12.dp,
                                        topEnd = 12.dp,
                                        bottomStart = if (turn.isUser) 12.dp else 4.dp,
                                        bottomEnd = if (turn.isUser) 4.dp else 12.dp
                                    ),
                                    color = when {
                                        turn.isUser -> RoyalBlue
                                        turn.isClarificationQuestion -> Color(0xFFE0F2FE)
                                        else -> Color.White
                                    },
                                    border = if (!turn.isUser) {
                                        BorderStroke(
                                            1.dp,
                                            if (turn.isClarificationQuestion) RoyalBlue.copy(alpha = 0.45f)
                                            else Color(0xFFD5E3F8)
                                        )
                                    } else null,
                                    modifier = Modifier.widthIn(max = 285.dp)
                                ) {
                                    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                                        Text(
                                            text = if (turn.isUser) firstName else "Brohi Ice AI",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (turn.isUser) Color.White.copy(alpha = 0.8f) else RoyalBlue
                                        )
                                        Text(
                                            text = turn.text,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp,
                                            color = if (turn.isUser) Color.White else DeepBlue
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Awaiting Clarification Banner (e.g. Rs. 35,000 sale waiting for block count)
            if (voiceUiState.pendingDraft != null && voiceUiState.awaitingField.isNotBlank()) {
                val draft = voiceUiState.pendingDraft
                val draftInfo = buildString {
                    append("Pending: ${draft.intentType}")
                    if (draft.amount > 0) append(" • ${PakistaniFormatters.formatPkr(draft.amount)}")
                    if (draft.quantity > 0) append(" • ${draft.quantity} blocks")
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFFFFBEB),
                    border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "$draftInfo — Reply with ${voiceUiState.awaitingField} (or say 'default rate' / 'cancel')",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF92400E),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }

            // Central Animated Mic / Speaking Orb
            val micBg by animateColorAsState(
                when {
                    speechState.isListening -> DangerRed
                    speechState.isSpeaking -> Color(0xFF0EA5E9)
                    else -> RoyalBlue
                },
                label = "micColor"
            )
            val pulseTransition = rememberInfiniteTransition(label = "micPulse")
            val pulseScale by pulseTransition.animateFloat(
                initialValue = 1f,
                targetValue = if (speechState.isListening || speechState.isSpeaking) 1.12f else 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(650),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "micScale"
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(micBg.copy(alpha = 0.9f), micBg)
                            )
                        )
                        .clickable {
                            when {
                                speechState.isListening -> onStopListening()
                                speechState.isSpeaking -> {
                                    onStopSpeaking()
                                    val granted = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) onStartListening()
                                    else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                                else -> {
                                    val granted = ContextCompat.checkSelfPermission(
                                        context, Manifest.permission.RECORD_AUDIO
                                    ) == PackageManager.PERMISSION_GRANTED
                                    if (granted) onStartListening()
                                    else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        }
                        .testTag("voice_sheet_mic_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when {
                            speechState.isListening -> Icons.Filled.Stop
                            speechState.isSpeaking -> Icons.AutoMirrored.Filled.VolumeUp
                            else -> Icons.Filled.Mic
                        },
                        contentDescription = if (speechState.isListening) "Stop listening" else "Start listening",
                        tint = Color.White,
                        modifier = Modifier.size(34.dp)
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = when {
                        speechState.isListening -> "Listening... Speak in English, Urdu, or Hindi"
                        speechState.isSpeaking -> "Speaking reply... Tap orb to interrupt & talk"
                        voiceUiState.awaitingField.isNotBlank() -> "Waiting for your answer (${voiceUiState.awaitingField})..."
                        else -> "Tap mic or say \"Hey Ice\" (اے آئس / हे आइस) anytime"
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
                leadingIcon = Icons.Filled.GraphicEq,
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
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send to assistant", tint = RoyalBlue)
                    }
                }
            )

            // Prominent Primary Action Button
            IcyGlossyButton(
                onClick = submitCurrentInput,
                enabled = manualInput.isNotBlank() && !voiceUiState.isParsing && !speechState.isListening,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("voice_analyze_action_button")
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (voiceUiState.awaitingField.isNotBlank()) {
                        "Reply to Assistant (جواب بھیجیں)"
                    } else if (voiceUiState.autoSaveEnabled) {
                        "Ask / Auto-Save (بولیں یا محفوظ کریں)"
                    } else {
                        "Ask & Review Entry (تجزیہ کریں)"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Quick Sample Chips demonstrating blocks+total, clarification, and expenses
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "35 hazar sale aur 15 blocks",
                    "35,000 ki ice sale hui",
                    "Rs. 75,000 bijli bill"
                ).forEach { sample ->
                    Surface(
                        onClick = {
                            manualInput = ""
                            keyboardController?.hide()
                            onSubmitTranscript(sample)
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
                    Text("Brohi Ice AI is thinking...", fontSize = 13.sp)
                }
            }

            if (!voiceUiState.spokenAnswer.isNullOrBlank() && voiceUiState.spokenAnswer.startsWith("✓")) {
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
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = SuccessGreen,
                            modifier = Modifier.size(20.dp)
                        )
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
    var blocksText by remember(intent.id) {
        mutableStateOf(if (intent.quantity > 0L) intent.quantity.toString() else "")
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
                    val unitRateLabel = if (intent.quantity > 0L) {
                        val perBlock = (intent.amount / intent.quantity.toDouble()).roundToLong()
                        " (${intent.quantity} blk @ Rs.$perBlock)"
                    } else ""
                    Text(
                        text = PakistaniFormatters.formatPkr(intent.amount) + unitRateLabel,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        softWrap = false,
                        color = SuccessGreen
                    )
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove item", tint = DangerRed, modifier = Modifier.size(18.dp))
                }
            }

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
                            val updatedUnit = if (intent.quantity > 0L && parsed > 0.0) parsed / intent.quantity.toDouble() else 0.0
                            onUpdate(intent.copy(amount = parsed, unitPrice = updatedUnit))
                        }
                    },
                    englishLabel = if (intent.intentType == "PRODUCTION") "Tons" else "Total (Rs.)",
                    leadingIcon = if (intent.intentType == "PRODUCTION") Icons.Filled.Scale else Icons.Filled.Payments,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.weight(1f)
                )
            }
            if (intent.intentType == "SALE" || intent.intentType == "CREDIT_SALE") {
                CrystalTextField(
                    value = blocksText,
                    onValueChange = { raw ->
                        val cleaned = PakistaniFormatters.convertUrduDigits(raw).filter { it.isDigit() }
                        blocksText = cleaned
                        val qty = cleaned.toLongOrNull() ?: 0L
                        val unit = if (qty > 0L && intent.amount > 0.0) intent.amount / qty.toDouble() else 0.0
                        onUpdate(intent.copy(quantity = qty, unitPrice = unit))
                    },
                    englishLabel = "Blocks Sold",
                    urduLabel = "بلاکس کی تعداد",
                    leadingIcon = Icons.Filled.Inventory2,
                    keyboardType = KeyboardType.Number,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
