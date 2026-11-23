package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SupervisorAccount
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.R
import com.example.data.face.FaceDetectionAnalyzer
import com.example.data.face.LiveFaceAnalysis
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.data.model.ProfileUpdateRequest
import com.example.data.model.REGISTRATION_ROLE_OPTIONS
import com.example.data.model.RegistrationRequest
import com.example.data.model.UserRole
import com.example.data.model.WORK_SHIFT_OPTIONS
import com.example.data.util.AppStrings
import com.example.data.util.ExportHelper
import com.example.data.util.FaceCryptoHelper
import com.example.data.util.PakistaniFormatters
import com.example.ui.components.BilingualPillLabel
import com.example.ui.components.BrohiCard
import com.example.ui.components.CnicCrystalTextField
import com.example.ui.components.CrystalFormDialog
import com.example.ui.components.CrystalTextField
import com.example.ui.components.IcyGlossyButton
import com.example.ui.components.IcyGlossyTone
import com.example.ui.components.ModernRoleSelectorCardGrid
import com.example.ui.components.ModernShiftSelector
import com.example.ui.components.PhoneCrystalTextField
import com.example.ui.components.PrimaryBlueGradient
import com.example.ui.components.ValidationPopupDialog
import com.example.ui.theme.CrystalBorderLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedSoft
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.WarningOrangeSoft
import java.util.concurrent.Executors

@Composable
fun CameraFacePreviewBox(
    modifier: Modifier = Modifier,
    isCircle: Boolean = false,
    isVerified: Boolean = false,
    resetLivenessKey: Int = 0,
    captureSnapshotForKey: String? = null,
    onCapturedSnapshot: ((android.graphics.Bitmap) -> Unit)? = null,
    onAnalysis: (LiveFaceAnalysis) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    if (!hasCameraPermission) {
        // Spacious rounded card so permission text and button are NEVER clipped inside a circle
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.5.dp, RoyalBlue.copy(alpha = 0.35f)),
            shadowElevation = 2.dp,
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFEAF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Filled.CameraAlt,
                        contentDescription = "Camera access required",
                        tint = RoyalBlue,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Camera Permission Required",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Grant camera access for live face verification and blink liveness detection.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                IcyGlossyButton(
                    onClick = { launcher.launch(Manifest.permission.CAMERA) },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("enable_camera_button")
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Camera", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 1)
                }
            }
        }
    } else {
        val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
        val faceAnalyzer = remember { FaceDetectionAnalyzer(isFrontCamera = true, onAnalysisResult = onAnalysis) }
        var boundCameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
        LaunchedEffect(resetLivenessKey) {
            if (resetLivenessKey > 0) {
                faceAnalyzer.resetLiveness()
            }
        }
        LaunchedEffect(captureSnapshotForKey) {
            if (!captureSnapshotForKey.isNullOrBlank() && onCapturedSnapshot != null) {
                faceAnalyzer.captureNextSnapshot { bmp, _ ->
                    onCapturedSnapshot(bmp)
                }
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                runCatching { boundCameraProvider?.unbindAll() }
                faceAnalyzer.close()
                analyzerExecutor.shutdown()
            }
        }

        val frameBoxModifier = if (isCircle) {
            Modifier
                .size(204.dp)
                .clip(CircleShape)
                .border(4.dp, if (isVerified) SuccessGreen else RoyalBlue, CircleShape)
        } else {
            modifier.clip(RoundedCornerShape(18.dp))
        }

        Box(
            modifier = if (isCircle) modifier.fillMaxWidth() else frameBoxModifier,
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = if (isCircle) frameBoxModifier else Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                AndroidView(
                    factory = { ctx ->
                        val previewView = PreviewView(ctx).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                        }
                        val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                        cameraProviderFuture.addListener({
                            try {
                                val cameraProvider = cameraProviderFuture.get()
                                boundCameraProvider = cameraProvider
                                val preview = Preview.Builder().build().also {
                                    it.surfaceProvider = previewView.surfaceProvider
                                }
                                val imageAnalysis = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { it.setAnalyzer(analyzerExecutor, faceAnalyzer) }
                                val selector = if (cameraProvider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)) {
                                    CameraSelector.DEFAULT_FRONT_CAMERA
                                } else {
                                    CameraSelector.DEFAULT_BACK_CAMERA
                                }
                                cameraProvider.unbindAll()
                                cameraProvider.bindToLifecycle(lifecycleOwner, selector, preview, imageAnalysis)
                            } catch (_: Exception) {
                            }
                        }, ContextCompat.getMainExecutor(ctx))
                        previewView
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (isCircle && isVerified) {
                Box(
                    modifier = Modifier
                        .size(204.dp)
                        .padding(6.dp),
                    contentAlignment = Alignment.BottomEnd
                ) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Verified",
                        tint = SuccessGreen,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaceEnrollmentScreen(
    employees: List<Employee>,
    onSavePreEncryptedFaceTemplate: (employeeId: String, encryptedTemplate: String) -> Unit,
    onSaveAttendanceFacePhoto: ((employeeId: String, portraitDataUri: String) -> Unit)? = null,
    onToggleSkipLiveness: (employeeId: String, skip: Boolean) -> Unit
) {
    var selectedEmployee by remember(employees) { mutableStateOf(employees.firstOrNull()) }
    var empExpanded by remember { mutableStateOf(false) }
    var showGuidedEnrollDialog by remember { mutableStateOf(false) }

    val selectedHasFace = !selectedEmployee?.faceTemplateEncrypted.isNullOrBlank()
    val selectedHasLegacyFace = selectedHasFace && FaceCryptoHelper.isLegacyTemplate(
        selectedEmployee?.ownerId?.ifBlank { "global" } ?: "global",
        selectedEmployee?.faceTemplateEncrypted
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (employees.isNotEmpty()) {
            ExposedDropdownMenuBox(
                expanded = empExpanded,
                onExpandedChange = { empExpanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                CrystalTextField(
                    value = selectedEmployee?.name ?: "Select Employee",
                    onValueChange = {},
                    readOnly = true,
                    englishLabel = "Employee",
                    urduLabel = "ملازم منتخب کریں",
                    leadingIcon = Icons.Filled.Person,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = empExpanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                )
                ExposedDropdownMenu(expanded = empExpanded, onDismissRequest = { empExpanded = false }) {
                    employees.forEach { emp ->
                        val enrolled = !emp.faceTemplateEncrypted.isNullOrBlank()
                        val isLegacy = enrolled && FaceCryptoHelper.isLegacyTemplate(
                            emp.ownerId.ifBlank { "global" },
                            emp.faceTemplateEncrypted
                        )
                        val badge = when {
                            !enrolled -> "Missing Face"
                            isLegacy -> "⚠ Upgrade Scan Required"
                            else -> "✓ Secured (MP2)"
                        }
                        DropdownMenuItem(
                            text = {
                                Text("${emp.name} (${emp.role}) • $badge")
                            },
                            onClick = {
                                selectedEmployee = emp
                                empExpanded = false
                            }
                        )
                    }
                }
            }
        }

        BrohiCard(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = when {
                        selectedHasLegacyFace -> WarningOrangeSoft
                        selectedHasFace -> SuccessGreenSoft
                        else -> Color(0xFFEAF2FF)
                    },
                    modifier = Modifier.size(68.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (selectedHasFace && !selectedHasLegacyFace) Icons.Filled.CheckCircle else Icons.Filled.Face,
                            contentDescription = null,
                            tint = when {
                                selectedHasLegacyFace -> WarningOrange
                                selectedHasFace -> SuccessGreen
                                else -> RoyalBlue
                            },
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = when {
                        selectedHasLegacyFace -> "Security Upgrade Required (دوبارہ اسکین کریں)"
                        selectedHasFace -> "4-Pose Multimodal Biometrics Active"
                        else -> "Guided 4-Pose Face Enrollment"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = if (selectedHasLegacyFace) {
                        "This employee has an older geometry-only template. Re-enroll now to activate strict anti-proxy texture + shading protection.\nپرانا فیس ریکارڈ اپ گریڈ کرنا ضروری ہے تاکہ کوئی اور شخص حاضری نہ لگا سکے۔"
                    } else {
                        "Captures Front, Left, Right & Tilt-up angles automatically inside the oval ring and builds an encrypted geometry + texture biometric signature.\nصرف اصل ملازم ہی حاضری لگا سکے گا — کسی دوسرے شخص کا چہرہ قبول نہیں ہوگا۔"
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 17.sp
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ChecklistRow("1. Look straight ahead (Front)", selectedHasFace)
                    ChecklistRow("2. Turn head left (Left)", selectedHasFace)
                    ChecklistRow("3. Turn head right (Right)", selectedHasFace)
                    ChecklistRow("4. Tilt head slightly up (Tilt up)", selectedHasFace)
                }

                IcyGlossyButton(
                    onClick = { showGuidedEnrollDialog = true },
                    enabled = selectedEmployee != null,
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .testTag("capture_face_angle_button")
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (selectedHasFace) {
                            "Re-enroll 4-Pose Face • دوبارہ اسکین"
                        } else {
                            "Start 4-Pose Face Scan • ابھی اسکین کریں"
                        },
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        maxLines = 2
                    )
                }
            }
        }

        selectedEmployee?.let { emp ->
            BrohiCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Skip Liveness Check", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        Text(
                            "Allow attendance bypass if blink sensor fails",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = emp.skipLiveness,
                        onCheckedChange = { onToggleSkipLiveness(emp.id, it) }
                    )
                }
            }
        }
    }

    if (showGuidedEnrollDialog && selectedEmployee != null) {
        GuidedFaceEnrollmentDialog(
            ownerKey = selectedEmployee!!.ownerId.ifBlank { "global" },
            onDismiss = { showGuidedEnrollDialog = false },
            onPortraitCaptured = { portraitUri ->
                if (!portraitUri.isNullOrBlank()) {
                    onSaveAttendanceFacePhoto?.invoke(selectedEmployee!!.id, portraitUri)
                }
            },
            onCompleted = { encrypted ->
                showGuidedEnrollDialog = false
                onSavePreEncryptedFaceTemplate(selectedEmployee!!.id, encrypted)
            }
        )
    }
}

@Composable
private fun ChecklistRow(label: String, done: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (done) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
            contentDescription = null,
            tint = if (done) SuccessGreen else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceScreen(
    activeRole: UserRole,
    currentUserName: String,
    currentUserId: String,
    employees: List<Employee>,
    attendanceRecords: List<AttendanceRecord>,
    strings: AppStrings,
    onMatchFace: (List<Float>) -> Pair<Employee, Float>?,
    onCheckIn: (employeeId: String, employeeName: String, bypassed: Boolean) -> Unit,
    onCheckOut: (AttendanceRecord, Boolean) -> Unit,
    onSaveAttendanceRecord: (AttendanceRecord) -> Unit,
    onDeleteAttendance: (String) -> Unit,
    onSavePreEncryptedFaceTemplate: ((employeeId: String, encryptedTemplate: String) -> Unit)? = null,
    onSaveAttendanceFacePhoto: ((employeeId: String, portraitDataUri: String) -> Unit)? = null
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(0) }
    var liveAnalysis by remember { mutableStateOf(LiveFaceAnalysis()) }
    var reportPeriod by remember { mutableStateOf("This Month") }
    var statusFilter by remember { mutableStateOf("All") } // "All", "Present", "Absent", "Leave"

    var selectedEmployeeDetail by remember { mutableStateOf<Employee?>(null) }
    var selectedEmployeeNameFallback by remember { mutableStateOf<String?>(null) }
    var editingRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var addingManualRecord by remember { mutableStateOf(false) }
    var deleteConfirmRecord by remember { mutableStateOf<AttendanceRecord?>(null) }
    var showQuickEnrollSheet by remember { mutableStateOf(false) }

    // Default ALWAYS to strict self-attendance verification for the logged-in account.
    // Admins/Super Admins can explicitly toggle Kiosk Scan mode when scanning other workers.
    var kioskScanMode by remember { mutableStateOf(false) }
    var analyzerResetToken by remember { mutableIntStateOf(0) }
    var consecutiveVerifiedFrames by remember { mutableIntStateOf(0) }
    var verifiedCandidateId by remember { mutableStateOf<String?>(null) }

    val isStaff = activeRole == UserRole.STAFF
    val myEmployee = remember(employees, currentUserId) {
        employees.firstOrNull { it.authUid == currentUserId }
            ?: employees.firstOrNull { it.id == currentUserId }
    }
    val hasMyFaceEnrolled = !myEmployee?.faceTemplateEncrypted.isNullOrBlank()
    val hasMyLegacyFace = hasMyFaceEnrolled && FaceCryptoHelper.isLegacyTemplate(
        myEmployee?.ownerId?.ifBlank { "global" } ?: "global",
        myEmployee?.faceTemplateEncrypted
    )

    // Strict anti-proxy multimodal biometric matching (44 landmarks + 64 aligned intensity + 64 Sobel texture)
    val liveDescriptor = liveAnalysis.descriptor
    val faceInFrame = liveAnalysis.faceDetected && liveDescriptor.size >= FaceCryptoHelper.MULTIMODAL_DIM
    val isFrontPoseForVerify = liveAnalysis.faceDetected &&
        kotlin.math.abs(liveAnalysis.headYaw) <= 15f &&
        kotlin.math.abs(liveAnalysis.headPitch) <= 15f &&
        liveAnalysis.isCentered &&
        liveAnalysis.isWellLit

    val matchResult = remember(liveDescriptor, employees, myEmployee, kioskScanMode, activeRole) {
        if (!faceInFrame) return@remember null
        if (!kioskScanMode || !activeRole.canManageStaff) {
            // STRICT self verification: ONLY the logged-in user's own MP2 front biometric can match
            val targetEmp = myEmployee
            if (targetEmp != null && !targetEmp.faceTemplateEncrypted.isNullOrBlank()) {
                val ownerKey = targetEmp.ownerId.ifBlank { "global" }
                if (FaceCryptoHelper.isLegacyTemplate(ownerKey, targetEmp.faceTemplateEncrypted)) {
                    Triple(null, 0f, false) // Require security upgrade re-scan
                } else {
                    val frontPose = FaceCryptoHelper.decryptTemplate(ownerKey, targetEmp.faceTemplateEncrypted)
                    val score = if (frontPose.size >= FaceCryptoHelper.MULTIMODAL_DIM) {
                        FaceCryptoHelper.strictBiometricMatchScore(liveDescriptor, frontPose)
                    } else {
                        null
                    }
                    if (score != null && score >= FaceCryptoHelper.MIN_MULTIMODAL_SCORE) {
                        Triple(targetEmp, score, false)
                    } else {
                        Triple(null, score ?: 0f, true)
                    }
                }
            } else {
                Triple(null, 0f, false)
            }
        } else {
            val matched = onMatchFace(liveDescriptor)
            if (matched != null) {
                Triple(matched.first, matched.second, false)
            } else {
                Triple(null, 0f, true)
            }
        }
    }

    val matchedCandidate = matchResult?.first
    val matchConfidence = matchResult?.second
    val isFaceMismatch = matchResult?.third == true && faceInFrame
    // Strict liveness: requires actual eye blink (not simple head-wobble photo tilt) unless explicitly waived
    val livenessOk = liveAnalysis.blinkConfirmed || (matchedCandidate?.skipLiveness == true)

    // 3-frame temporal consensus: requires 3 consecutive straight-ahead matching frames before unlocking
    LaunchedEffect(liveAnalysis, matchedCandidate?.id, isFaceMismatch, kioskScanMode) {
        if (matchedCandidate != null && !isFaceMismatch && livenessOk && isFrontPoseForVerify) {
            if (verifiedCandidateId == matchedCandidate.id) {
                consecutiveVerifiedFrames = (consecutiveVerifiedFrames + 1).coerceAtMost(5)
            } else {
                verifiedCandidateId = matchedCandidate.id
                consecutiveVerifiedFrames = 1
            }
        } else {
            consecutiveVerifiedFrames = 0
            verifiedCandidateId = null
        }
    }

    val isVerifyingHold = matchedCandidate != null &&
        !isFaceMismatch &&
        livenessOk &&
        isFrontPoseForVerify &&
        consecutiveVerifiedFrames in 1..2
    val isStrictlyVerified = matchedCandidate != null &&
        !isFaceMismatch &&
        livenessOk &&
        isFrontPoseForVerify &&
        consecutiveVerifiedFrames >= 3
    val verifiedEmployee = if (isStrictlyVerified) matchedCandidate else null

    val today = PakistaniFormatters.todayIso()
    val activeEmpId = verifiedEmployee?.id ?: (myEmployee?.id ?: currentUserId)
    val activeEmpName = verifiedEmployee?.name ?: (myEmployee?.name ?: currentUserName.ifBlank { "Ahmed Khan" })

    // Strict per-employee Check-Out binding: never attach a colleague's scan to the signed-in user's open shift!
    val todayOpenRecord = remember(attendanceRecords, today, kioskScanMode, verifiedEmployee, myEmployee, currentUserId) {
        if (kioskScanMode && activeRole.canManageStaff) {
            if (verifiedEmployee == null) {
                null
            } else {
                attendanceRecords.firstOrNull {
                    it.date == today &&
                        it.employeeId == verifiedEmployee.id &&
                        it.checkOut.isBlank() &&
                        it.status == "Present"
                }
            }
        } else {
            val selfId = myEmployee?.id ?: currentUserId
            attendanceRecords.firstOrNull {
                it.date == today &&
                    (it.employeeId == selfId || it.employeeId == currentUserId) &&
                    it.checkOut.isBlank() &&
                    it.status == "Present"
            }
        }
    }

    val resetVerificationSession = {
        analyzerResetToken += 1
        consecutiveVerifiedFrames = 0
        verifiedCandidateId = null
        liveAnalysis = LiveFaceAnalysis()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (activeRole.canManageStaff) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Check-In / Out") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Attendance Report") }
                )
            }
        }

        if (selectedTab == 0 || !activeRole.canManageStaff) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (activeRole.canManageStaff) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFEAF2FF),
                        border = BorderStroke(1.2.dp, CrystalBorderLight),
                        shadowElevation = 2.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                Triple(false, "My Attendance", "میری حاضری") to Icons.Filled.Person,
                                Triple(true, "Staff Kiosk", "اسٹاف اسکین") to Icons.Filled.SupervisorAccount
                            ).forEach { (item, icon) ->
                                val (isKioskTab, enTitle, urTitle) = item
                                val selected = kioskScanMode == isKioskTab
                                val interactionSource = remember { MutableInteractionSource() }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 54.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .then(
                                            if (selected) {
                                                Modifier.background(PrimaryBlueGradient)
                                            } else {
                                                Modifier
                                                    .background(Color.White.copy(alpha = 0.72f))
                                                    .border(0.8.dp, CrystalBorderLight.copy(alpha = 0.75f), RoundedCornerShape(12.dp))
                                            }
                                        )
                                        .clickable(
                                            interactionSource = interactionSource,
                                            indication = null
                                        ) {
                                            if (kioskScanMode != isKioskTab) {
                                                kioskScanMode = isKioskTab
                                                resetVerificationSession()
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 8.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                                        ) {
                                            Icon(
                                                imageVector = icon,
                                                contentDescription = null,
                                                tint = if (selected) Color.White else RoyalBlue,
                                                modifier = Modifier.size(15.dp)
                                            )
                                            Text(
                                                text = enTitle,
                                                color = if (selected) Color.White else DeepBlue,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.5.sp,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = urTitle,
                                            color = if (selected) Color(0xFFDCE8FF) else RoyalBlue,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 11.sp,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Missing or legacy biometric template banner (applies to ALL roles in Self mode)
                if (!kioskScanMode && (!hasMyFaceEnrolled || hasMyLegacyFace)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = WarningOrangeSoft,
                        border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.5f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(Icons.Filled.Face, contentDescription = null, tint = WarningOrange)
                                Text(
                                    text = if (hasMyLegacyFace) {
                                        "Biometric Security Upgrade Required (سیکیورٹی اپ گریڈ)"
                                    } else {
                                        "Face Biometrics Missing! (چہرہ رجسٹرڈ نہیں ہے)"
                                    },
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = Color(0xFF9A3412)
                                )
                            }
                            Text(
                                text = if (hasMyLegacyFace) {
                                    "Anti-proxy protection has been upgraded to block colleague mismatches. Please run a quick 4-pose re-scan once so only your exact face can Check-In and Check-Out.\nکسی دوسرے ساتھی کے چہرے سے حاضری روکنے کے لیے سیکیورٹی اپ گریڈ کر دی گئی ہے۔ براہ کرم اپنا چہرہ دوبارہ رجسٹر کریں۔"
                                } else {
                                    "آپ کا چہرہ ابھی تک سسٹم میں رجسٹر نہیں ہوا۔ بغیر چہرہ رجسٹریشن کے حاضری نہیں لگ سکتی۔ براہ کرم ابھی کیمرہ کھول کر اپنا چہرہ رجسٹر کریں۔"
                                },
                                fontSize = 11.sp,
                                color = Color(0xFF7C2D12),
                                lineHeight = 15.sp
                            )
                            if (onSavePreEncryptedFaceTemplate != null) {
                                IcyGlossyButton(
                                    onClick = { showQuickEnrollSheet = true },
                                    tone = IcyGlossyTone.ORANGE,
                                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 46.dp)
                                ) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (hasMyLegacyFace) {
                                            "Upgrade Face Scan Now • چہرہ اپ گریڈ کریں"
                                        } else {
                                            "Register My Face Now • ابھی چہرہ اسکین کریں"
                                        },
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center,
                                        maxLines = 2
                                    )
                                }
                            }
                        }
                    }
                }

                if (showQuickEnrollSheet && onSavePreEncryptedFaceTemplate != null) {
                    GuidedFaceEnrollmentDialog(
                        ownerKey = myEmployee?.ownerId?.ifBlank { "global" } ?: "global",
                        onDismiss = { showQuickEnrollSheet = false },
                        onPortraitCaptured = { portraitUri ->
                            val targetId = myEmployee?.id ?: currentUserId
                            if (!portraitUri.isNullOrBlank()) {
                                onSaveAttendanceFacePhoto?.invoke(targetId, portraitUri)
                            }
                        },
                        onCompleted = { encrypted ->
                            showQuickEnrollSheet = false
                            val targetId = myEmployee?.id ?: currentUserId
                            onSavePreEncryptedFaceTemplate(targetId, encrypted)
                            resetVerificationSession()
                        }
                    )
                }

                // Clean, unclipped camera preview container with subtle factory backdrop
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(22.dp))
                        .background(Color(0xFFEAF2FF)),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(R.drawable.img_factory_skyline),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        alpha = 0.22f,
                        modifier = Modifier
                            .matchParentSize()
                            .clip(RoundedCornerShape(22.dp))
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraFacePreviewBox(
                            isCircle = true,
                            isVerified = isStrictlyVerified,
                            resetLivenessKey = analyzerResetToken,
                            captureSnapshotForKey = if (isStrictlyVerified && verifiedEmployee != null && verifiedEmployee.attendanceFacePhoto.isNullOrBlank()) {
                                verifiedEmployee.id
                            } else {
                                null
                            },
                            onCapturedSnapshot = { bmp ->
                                val targetEmp = verifiedEmployee
                                if (targetEmp != null && !bmp.isRecycled) {
                                    val uri = com.example.data.util.ImageAndQrUtils.encodeBitmapToJpegDataUri(
                                        bitmap = bmp,
                                        maxDimension = 300,
                                        quality = 72
                                    )
                                    runCatching { bmp.recycle() }
                                    onSaveAttendanceFacePhoto?.invoke(targetEmp.id, uri)
                                } else {
                                    runCatching { bmp.recycle() }
                                }
                            },
                            onAnalysis = { liveAnalysis = it }
                        )
                    }
                }

                // Status Pill (Bilingual with clear anti-proxy verification stages)
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = when {
                        isStrictlyVerified -> SuccessGreenSoft
                        isFaceMismatch -> DangerRedSoft
                        liveAnalysis.faceDetected && (!livenessOk || !isFrontPoseForVerify || isVerifyingHold) -> WarningOrangeSoft
                        else -> Color(0xFFEAF2FF)
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isStrictlyVerified -> SuccessGreen.copy(alpha = 0.7f)
                            isFaceMismatch -> DangerRed.copy(alpha = 0.7f)
                            liveAnalysis.faceDetected && (!livenessOk || !isFrontPoseForVerify || isVerifyingHold) -> WarningOrange.copy(alpha = 0.5f)
                            else -> RoyalBlue.copy(alpha = 0.3f)
                        }
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isStrictlyVerified -> Icons.Filled.CheckCircle
                                isFaceMismatch -> Icons.Filled.Cancel
                                else -> Icons.Filled.Face
                            },
                            contentDescription = null,
                            tint = when {
                                isStrictlyVerified -> SuccessGreen
                                isFaceMismatch -> DangerRed
                                liveAnalysis.faceDetected && (!livenessOk || !isFrontPoseForVerify || isVerifyingHold) -> WarningOrange
                                else -> RoyalBlue
                            },
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val headlineEn = when {
                                isStrictlyVerified -> {
                                    val pct = matchConfidence?.let { " (${(it * 100).toInt()}%)" } ?: ""
                                    "Original Employee Verified!$pct: ${verifiedEmployee?.name}"
                                }
                                isFaceMismatch -> "Face Does Not Match!"
                                isVerifyingHold -> "Verifying Live Identity ($consecutiveVerifiedFrames/3)..."
                                liveAnalysis.faceDetected && !isFrontPoseForVerify ->
                                    if (!liveAnalysis.isWellLit) "Low Light — Move to brighter area"
                                    else "Look straight & center face in ring"
                                liveAnalysis.faceDetected && !livenessOk -> "Face Matched — Blink your eyes once"
                                else -> "Position face inside camera ring"
                            }
                            val headlineUr = when {
                                isStrictlyVerified -> "اصل ملازم کی تصدیق ہو گئی"
                                isFaceMismatch -> "چہرہ میچ نہیں ہوا"
                                isVerifyingHold -> "لائیو شناخت کی تصدیق ہو رہی ہے"
                                liveAnalysis.faceDetected && !isFrontPoseForVerify ->
                                    if (!liveAnalysis.isWellLit) "روشنی بڑھائیں" else "سیدھا کیمرے میں دیکھیں"
                                liveAnalysis.faceDetected && !livenessOk -> "تصدیق کے لیے ایک بار پلکیں جھپکائیں"
                                else -> "چہرہ کیمرہ کے سامنے لائیں"
                            }
                            val headlineColor = when {
                                isStrictlyVerified -> Color(0xFF0B5D29)
                                isFaceMismatch -> DangerRed
                                liveAnalysis.faceDetected && (!livenessOk || !isFrontPoseForVerify || isVerifyingHold) -> Color(0xFF9A3412)
                                else -> DeepBlue
                            }
                            Text(
                                text = headlineEn,
                                color = headlineColor,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                lineHeight = 17.sp
                            )
                            Text(
                                text = headlineUr,
                                color = headlineColor.copy(alpha = 0.9f),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                            Text(
                                text = when {
                                    isStrictlyVerified -> "You can now confirm check-in or check-out safely"
                                    isFaceMismatch -> "Only the original registered employee can mark attendance. Proxy blocked."
                                    isVerifyingHold -> "Confirming 3 consecutive biometric frames for anti-proxy protection..."
                                    liveAnalysis.faceDetected && !isFrontPoseForVerify -> "Keep head level and straight inside the circular camera frame"
                                    liveAnalysis.faceDetected && !livenessOk -> "Live blink liveness required to prevent photo/colleague spoofing"
                                    else -> "Look directly into the camera to mark your attendance"
                                },
                                color = when {
                                    isStrictlyVerified -> Color(0xFF166534)
                                    isFaceMismatch -> Color(0xFF991B1B)
                                    liveAnalysis.faceDetected && (!livenessOk || !isFrontPoseForVerify || isVerifyingHold) -> Color(0xFF7C2D12)
                                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                                },
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                }

                BrohiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Check-In Time", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = PakistaniFormatters.currentTimeDisplay(),
                            fontSize = 28.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = PakistaniFormatters.todayDisplay(),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                if (todayOpenRecord == null) {
                    val checkInEnabled = isStrictlyVerified && !hasMyLegacyFace
                    IcyGlossyButton(
                        onClick = {
                            onCheckIn(activeEmpId, activeEmpName, false)
                            resetVerificationSession()
                        },
                        enabled = checkInEnabled,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .testTag("confirm_checkin_button")
                    ) {
                        val contentTint = if (checkInEnabled) Color.White else Color.White.copy(alpha = 0.85f)
                        Icon(Icons.Filled.Check, contentDescription = null, tint = contentTint, modifier = Modifier.size(19.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Confirm Check-In",
                                color = contentTint,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (checkInEnabled) activeEmpName else "Original Face Required • اصل چہرہ ضروری ہے",
                                color = contentTint,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (activeRole.canManageStaff && !kioskScanMode) {
                        OutlinedButton(
                            onClick = {
                                onCheckIn(activeEmpId, activeEmpName, true)
                                resetVerificationSession()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CrystalBorderLight),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("bypass_checkin_button")
                        ) {
                            Text(
                                text = "Admin Manual Bypass • دستی اجازت",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val checkoutBiometricAllowed = isStrictlyVerified &&
                        com.example.ui.viewmodel.canCheckoutAttendanceRecord(
                            verifiedEmployeeId = verifiedEmployee?.id,
                            verifiedEmployeeAuthUid = verifiedEmployee?.authUid,
                            currentUserId = currentUserId,
                            kioskMode = kioskScanMode,
                            record = todayOpenRecord,
                            isManualBypass = false,
                            role = activeRole
                        )

                    IcyGlossyButton(
                        onClick = {
                            onCheckOut(todayOpenRecord, false)
                            resetVerificationSession()
                        },
                        enabled = checkoutBiometricAllowed,
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .testTag("confirm_checkout_button")
                    ) {
                        val contentTint = if (checkoutBiometricAllowed) Color.White else Color.White.copy(alpha = 0.85f)
                        Icon(Icons.Filled.Check, contentDescription = null, tint = contentTint, modifier = Modifier.size(19.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = if (checkoutBiometricAllowed) "Confirm Check-Out" else "Check-Out Locked",
                                color = contentTint,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = if (checkoutBiometricAllowed) {
                                    "Checked in at ${todayOpenRecord.checkIn}"
                                } else {
                                    "Original Face Required • اصل چہرہ ضروری ہے"
                                },
                                color = contentTint,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    if (activeRole.canManageStaff) {
                        OutlinedButton(
                            onClick = {
                                onCheckOut(todayOpenRecord, true)
                                resetVerificationSession()
                            },
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, CrystalBorderLight),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 48.dp)
                                .testTag("bypass_checkout_button")
                        ) {
                            Text(
                                text = "Admin Manual Bypass Check-Out • دستی چیک آؤٹ",
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                if (attendanceRecords.isNotEmpty()) {
                    Text(
                        text = if (activeRole.canManageStaff) "Recent Logs (حالیہ حاضری)" else "My Recent Attendance (میری حاضری)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        modifier = Modifier.align(Alignment.Start)
                    )
                    attendanceRecords.take(8).forEach { rec ->
                        BrohiCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(rec.employeeName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        "${rec.date} • In: ${rec.checkIn.ifBlank { "—" }} • Out: ${rec.checkOut.ifBlank { "—" }}",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    AttendanceStatusBadge(status = rec.status, bypassed = rec.bypassed)
                                    if (activeRole.canEditOrDelete) {
                                        IconButton(
                                            onClick = { editingRecord = rec },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit attendance", tint = RoyalBlue, modifier = Modifier.size(18.dp))
                                        }
                                        IconButton(
                                            onClick = { deleteConfirmRecord = rec },
                                            modifier = Modifier.size(36.dp)
                                        ) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete attendance", tint = DangerRed, modifier = Modifier.size(18.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Super Admin & Admin Interactive Attendance Report
            val monthPrefix = PakistaniFormatters.currentMonthIso()
            val periodRecords = attendanceRecords.filter {
                if (reportPeriod == "Today") it.date == today else it.date.startsWith(monthPrefix)
            }
            val presentCount = periodRecords.count { it.status == "Present" || it.status == "Checked Out" }
            val absentCount = periodRecords.count { it.status == "Absent" }
            val leaveCount = periodRecords.count { it.status == "Leave" }
            val totalStaff = employees.count { it.active }.coerceAtLeast(periodRecords.map { it.employeeId }.distinct().size)

            val visibleRecords = when (statusFilter) {
                "Present" -> periodRecords.filter { it.status == "Present" || it.status == "Checked Out" }
                "Absent" -> periodRecords.filter { it.status == "Absent" }
                "Leave" -> periodRecords.filter { it.status == "Leave" }
                else -> periodRecords
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("This Month", "Today").forEach { f ->
                            FilterChip(
                                selected = reportPeriod == f,
                                onClick = { reportPeriod = f },
                                label = { Text(f) }
                            )
                        }
                    }
                    OutlinedButton(
                        onClick = { addingManualRecord = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.heightIn(min = 38.dp)
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Mark", fontSize = 12.sp, maxLines = 1)
                    }
                }

                // 4 Clickable Summary Filter Cards (Total, Present, Absent, Leave)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AttendanceStatTile(
                        title = "Total Staff",
                        value = "$totalStaff",
                        icon = Icons.Filled.Group,
                        color = RoyalBlue,
                        selected = statusFilter == "All",
                        onClick = { statusFilter = "All" },
                        modifier = Modifier.weight(1f).testTag("att_filter_all")
                    )
                    AttendanceStatTile(
                        title = "Present",
                        value = "$presentCount",
                        icon = Icons.Filled.CheckCircle,
                        color = SuccessGreen,
                        selected = statusFilter == "Present",
                        onClick = { statusFilter = if (statusFilter == "Present") "All" else "Present" },
                        modifier = Modifier.weight(1f).testTag("att_filter_present")
                    )
                    AttendanceStatTile(
                        title = "Absent",
                        value = "$absentCount",
                        icon = Icons.Filled.Cancel,
                        color = DangerRed,
                        selected = statusFilter == "Absent",
                        onClick = { statusFilter = if (statusFilter == "Absent") "All" else "Absent" },
                        modifier = Modifier.weight(1f).testTag("att_filter_absent")
                    )
                    AttendanceStatTile(
                        title = "Leave",
                        value = "$leaveCount",
                        icon = Icons.Filled.EventBusy,
                        color = WarningOrange,
                        selected = statusFilter == "Leave",
                        onClick = { statusFilter = if (statusFilter == "Leave") "All" else "Leave" },
                        modifier = Modifier.weight(1f).testTag("att_filter_leave")
                    )
                }

                // Active Filter Label Banner
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFFEAF2FF),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val filterTitle = when (statusFilter) {
                            "Present" -> "Present Staff Report (${visibleRecords.size})"
                            "Absent" -> "Absent Staff Report (${visibleRecords.size})"
                            "Leave" -> "On Leave Staff Report (${visibleRecords.size})"
                            else -> "All Attendance Logs (${visibleRecords.size}) • Tap row for monthly detail"
                        }
                        Text(filterTitle, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepBlue)
                        if (statusFilter != "All") {
                            Text(
                                "Show All",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = RoyalBlue,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { statusFilter = "All" }
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }

                // Interactive Clean Attendance Table Card (Tapping any employee opens monthly detail)
                BrohiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Staff", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.4f))
                            Text("Status", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.05f))
                            Text("In / Out", fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1.0f))
                        }
                        HorizontalDivider()
                        if (visibleRecords.isEmpty()) {
                            Text(
                                text = "No records match the selected filter.",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 18.dp)
                            )
                        }
                        visibleRecords.forEach { row ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val emp = employees.firstOrNull {
                                            it.id == row.employeeId || it.name.equals(row.employeeName, ignoreCase = true)
                                        }
                                        selectedEmployeeDetail = emp
                                        selectedEmployeeNameFallback = row.employeeName
                                    }
                                    .padding(horizontal = 4.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Column(modifier = Modifier.weight(1.4f)) {
                                    Text(
                                        text = row.employeeName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = PakistaniFormatters.formatDisplayDate(row.date),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Box(modifier = Modifier.weight(1.05f)) {
                                    AttendanceStatusBadge(status = row.status, bypassed = row.bypassed)
                                }
                                Column(modifier = Modifier.weight(0.85f)) {
                                    Text(
                                        text = row.checkIn.ifBlank { "—" },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                    Text(
                                        text = row.checkOut.ifBlank { "—" },
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "View monthly details",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val lines = visibleRecords.map {
                                "${it.date} | ${it.employeeName} | ${it.status}${if (it.bypassed) " (Bypassed)" else ""} | In: ${it.checkIn} | Out: ${it.checkOut}"
                            }
                            ExportHelper.exportAndSharePdf(
                                context,
                                "attendance_report",
                                "Attendance Report ($reportPeriod - $statusFilter)",
                                "Total: $totalStaff | Present: $presentCount | Absent: $absentCount | Leave: $leaveCount",
                                lines
                            )
                        },
                        modifier = Modifier.weight(1f).testTag("export_attendance_pdf")
                    ) {
                        Icon(Icons.Filled.Description, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.exportPdf, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val headers = listOf("Date", "Name", "Status", "Bypassed", "Check-In", "Check-Out", "Hours")
                            val rows = visibleRecords.map {
                                listOf(it.date, it.employeeName, it.status, it.bypassed.toString(), it.checkIn, it.checkOut, it.hours.toString())
                            }
                            ExportHelper.exportAndShareCsv(context, "attendance_report", headers, rows)
                        },
                        modifier = Modifier.weight(1f).testTag("export_attendance_excel")
                    ) {
                        Icon(Icons.Filled.TableChart, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(strings.exportExcel, fontSize = 12.sp)
                    }
                }
            }
        }
    }

    // Employee Monthly Detail ModalBottomSheet (Tap on any employee in report)
    val detailName = selectedEmployeeDetail?.name ?: selectedEmployeeNameFallback
    if (detailName != null) {
        val monthPrefix = PakistaniFormatters.currentMonthIso()
        val empRecords = attendanceRecords.filter { rec ->
            val idMatches = selectedEmployeeDetail != null && rec.employeeId == selectedEmployeeDetail!!.id
            val nameMatches = rec.employeeName.equals(detailName, ignoreCase = true)
            (idMatches || nameMatches) && rec.date.startsWith(monthPrefix)
        }
        val mPresent = empRecords.count { it.status == "Present" || it.status == "Checked Out" }
        val mAbsent = empRecords.count { it.status == "Absent" }
        val mLeave = empRecords.count { it.status == "Leave" }
        val mBypass = empRecords.count { it.bypassed }
        val mHours = empRecords.sumOf { it.hours }

        ModalBottomSheet(
            onDismissRequest = {
                selectedEmployeeDetail = null
                selectedEmployeeNameFallback = null
            },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(detailName, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
                        val meta = selectedEmployeeDetail?.let {
                            val formattedCnic = PakistaniFormatters.formatCnic(it.cnic)
                            "${it.role} • ${it.phone}${if (formattedCnic.isNotBlank()) " • CNIC: $formattedCnic" else ""}"
                        } ?: "Monthly Attendance Summary ($monthPrefix)"
                        Text(meta, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    IconButton(onClick = {
                        selectedEmployeeDetail = null
                        selectedEmployeeNameFallback = null
                    }) {
                        Icon(Icons.Filled.Close, contentDescription = "Close")
                    }
                }

                // Monthly KPI Breakdown Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MonthMiniStat("Present", "$mPresent", SuccessGreen, SuccessGreenSoft, Modifier.weight(1f))
                    MonthMiniStat("Absent", "$mAbsent", DangerRed, DangerRedSoft, Modifier.weight(1f))
                    MonthMiniStat("Leave", "$mLeave", WarningOrange, WarningOrangeSoft, Modifier.weight(1f))
                    MonthMiniStat("Hours", String.format("%.1f", mHours), RoyalBlue, Color(0xFFEAF2FF), Modifier.weight(1f))
                }
                if (mBypass > 0) {
                    Text(
                        text = "Note: $mBypass check-in(s) were marked via verification bypass this month.",
                        fontSize = 11.sp,
                        color = WarningOrange
                    )
                }

                // Quick Mark Today buttons for Super Admin & Admin
                if (activeRole.canEditOrDelete) {
                    Text("Quick Mark Today ($today)", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        listOf("Present" to SuccessGreen, "Absent" to DangerRed, "Leave" to WarningOrange).forEach { (st, col) ->
                            OutlinedButton(
                                onClick = {
                                    onSaveAttendanceRecord(
                                        AttendanceRecord(
                                            employeeId = selectedEmployeeDetail?.id ?: activeEmpId,
                                            employeeName = detailName,
                                            authUid = selectedEmployeeDetail?.authUid ?: currentUserId,
                                            date = today,
                                            checkIn = if (st == "Present") PakistaniFormatters.currentTimeDisplay() else "",
                                            checkOut = "",
                                            status = st,
                                            bypassed = false,
                                            hours = 0.0
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f),
                                contentPadding = PaddingValues(vertical = 6.dp)
                            ) {
                                Text(st, fontSize = 12.sp, color = col, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                HorizontalDivider()
                Text("This Month's Logs (${empRecords.size})", fontWeight = FontWeight.Bold, fontSize = 14.sp)

                if (empRecords.isEmpty()) {
                    Text("No logs found for $monthPrefix.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    empRecords.forEach { rec ->
                        BrohiCard(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(PakistaniFormatters.formatDisplayDate(rec.date), fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                    Text(
                                        "In: ${rec.checkIn.ifBlank { "—" }} • Out: ${rec.checkOut.ifBlank { "—" }} • ${String.format("%.1f", rec.hours)} hrs",
                                        fontSize = 11.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    AttendanceStatusBadge(rec.status, rec.bypassed)
                                    if (activeRole.canEditOrDelete) {
                                        IconButton(onClick = { editingRecord = rec }, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Filled.Edit, contentDescription = "Edit log", tint = RoyalBlue, modifier = Modifier.size(17.dp))
                                        }
                                        IconButton(onClick = { deleteConfirmRecord = rec }, modifier = Modifier.size(34.dp)) {
                                            Icon(Icons.Filled.Delete, contentDescription = "Delete log", tint = DangerRed, modifier = Modifier.size(17.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Add / Edit Attendance Dialog
    if (addingManualRecord || editingRecord != null) {
        val base = editingRecord ?: AttendanceRecord(
            employeeId = employees.firstOrNull()?.id ?: currentUserId,
            employeeName = employees.firstOrNull()?.name ?: currentUserName,
            authUid = currentUserId,
            date = today,
            checkIn = "08:30 AM",
            checkOut = "",
            status = "Present",
            bypassed = false
        )
        var selectedEmpId by remember(base) { mutableStateOf(base.employeeId) }
        var selectedEmpName by remember(base) { mutableStateOf(base.employeeName) }
        var empMenuOpen by remember { mutableStateOf(false) }
        var dateText by remember(base) { mutableStateOf(base.date) }
        var status by remember(base) { mutableStateOf(base.status) }
        var checkInText by remember(base) { mutableStateOf(base.checkIn) }
        var checkOutText by remember(base) { mutableStateOf(base.checkOut) }

        CrystalFormDialog(
            onDismissRequest = {
                addingManualRecord = false
                editingRecord = null
            },
            title = if (editingRecord != null) "Edit Attendance Record" else "Mark Staff Attendance",
            subtitle = "ملازم کی حاضری درج یا تبدیل کریں",
            icon = Icons.Filled.CalendarMonth,
            confirmText = "Save (محفوظ)",
            dismissText = "Cancel",
            onConfirm = {
                val resolvedStatus = if (status == "Present" && checkOutText.isNotBlank()) "Checked Out" else status
                onSaveAttendanceRecord(
                    base.copy(
                        employeeId = selectedEmpId,
                        employeeName = selectedEmpName,
                        date = dateText.trim().ifBlank { today },
                        status = resolvedStatus,
                        checkIn = if (resolvedStatus == "Absent" || resolvedStatus == "Leave") "" else checkInText.trim(),
                        checkOut = if (resolvedStatus == "Absent" || resolvedStatus == "Leave") "" else checkOutText.trim()
                    )
                )
                addingManualRecord = false
                editingRecord = null
            }
        ) {
            if (editingRecord == null && employees.isNotEmpty()) {
                ExposedDropdownMenuBox(expanded = empMenuOpen, onExpandedChange = { empMenuOpen = it }) {
                    CrystalTextField(
                        value = selectedEmpName,
                        onValueChange = {},
                        readOnly = true,
                        englishLabel = "Staff Member",
                        urduLabel = "ملازم",
                        leadingIcon = Icons.Filled.Person,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = empMenuOpen) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable)
                    )
                    ExposedDropdownMenu(expanded = empMenuOpen, onDismissRequest = { empMenuOpen = false }) {
                        employees.forEach { emp ->
                            DropdownMenuItem(
                                text = { Text("${emp.name} (${emp.role})") },
                                onClick = {
                                    selectedEmpId = emp.id
                                    selectedEmpName = emp.name
                                    empMenuOpen = false
                                }
                            )
                        }
                    }
                }
            } else {
                Text(selectedEmpName, fontWeight = FontWeight.Bold, color = DeepBlue, fontSize = 14.sp)
            }
            CrystalTextField(
                value = dateText,
                onValueChange = { dateText = it },
                englishLabel = "Date (YYYY-MM-DD)",
                urduLabel = "تاریخ",
                leadingIcon = Icons.Filled.CalendarMonth
            )
            Text("Status (حالت)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepBlue)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Present", "Absent", "Leave").forEach { st ->
                    FilterChip(
                        selected = status == st,
                        onClick = { status = st },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = RoyalBlue, selectedLabelColor = Color.White),
                        label = { Text(st, fontSize = 11.sp) }
                    )
                }
            }
            if (status == "Present" || status == "Checked Out") {
                CrystalTextField(
                    value = checkInText,
                    onValueChange = { checkInText = it },
                    englishLabel = "Check-In (e.g. 08:15 AM)",
                    urduLabel = "آمد کا وقت",
                    leadingIcon = Icons.Filled.Schedule
                )
                CrystalTextField(
                    value = checkOutText,
                    onValueChange = { checkOutText = it },
                    englishLabel = "Check-Out (optional)",
                    urduLabel = "واپسی کا وقت",
                    leadingIcon = Icons.Filled.Schedule
                )
            }
        }
    }

    deleteConfirmRecord?.let { rec ->
        CrystalFormDialog(
            onDismissRequest = { deleteConfirmRecord = null },
            title = "Delete Attendance Record?",
            subtitle = "حاضری کا ریکارڈ حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteAttendance(rec.id)
                deleteConfirmRecord = null
            }
        ) {
            Text("Delete attendance log for ${rec.employeeName} on ${rec.date}?", fontSize = 13.sp)
        }
    }

    if (showQuickEnrollSheet && myEmployee != null && onSavePreEncryptedFaceTemplate != null) {
        GuidedFaceEnrollmentDialog(
            ownerKey = myEmployee.ownerId.ifBlank { "global" },
            onDismiss = { showQuickEnrollSheet = false },
            onPortraitCaptured = { portraitUri ->
                if (!portraitUri.isNullOrBlank()) {
                    onSaveAttendanceFacePhoto?.invoke(myEmployee.id, portraitUri)
                }
            },
            onCompleted = { encrypted ->
                showQuickEnrollSheet = false
                onSavePreEncryptedFaceTemplate(myEmployee.id, encrypted)
            }
        )
    }
}

@Composable
private fun AttendanceStatusBadge(status: String, bypassed: Boolean) {
    val (bg, fg) = when (status) {
        "Absent" -> DangerRedSoft to DangerRed
        "Leave" -> WarningOrangeSoft to WarningOrange
        else -> if (bypassed) WarningOrangeSoft to WarningOrange else SuccessGreenSoft to SuccessGreen
    }
    Surface(shape = RoundedCornerShape(8.dp), color = bg) {
        Text(
            text = if (bypassed && status != "Absent" && status != "Leave") "$status*" else status,
            color = fg,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun MonthMiniStat(
    label: String,
    value: String,
    color: Color,
    bg: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = bg,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color)
        }
    }
}

@Composable
private fun AttendanceStatTile(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tileColor = if (selected) {
        lerp(MaterialTheme.colorScheme.surface, color, 0.13f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = tileColor,
        border = BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) color else MaterialTheme.colorScheme.outline
        ),
        shadowElevation = if (selected) 0.dp else 2.dp,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                color = if (selected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UsersRolesScreen(
    activeRole: UserRole,
    employees: List<Employee>,
    registrationRequests: List<RegistrationRequest>,
    profileUpdateRequests: List<ProfileUpdateRequest> = emptyList(),
    onSaveEmployee: (Employee) -> Unit,
    onDeleteEmployee: (employeeId: String, employeeName: String) -> Unit,
    onApproveRequest: (RegistrationRequest, role: String, shift: String, startingSalary: Double, note: String?) -> Unit,
    onRejectRequest: (RegistrationRequest, reason: String?) -> Unit,
    onDeleteRequest: (String) -> Unit,
    onApproveProfileUpdate: (ProfileUpdateRequest, note: String?) -> Unit = { _, _ -> },
    onRejectProfileUpdate: (ProfileUpdateRequest, reason: String?) -> Unit = { _, _ -> },
    onDeleteProfileUpdate: (String) -> Unit = {},
    onClearDemoData: () -> Unit,
    onGoToFaceEnroll: () -> Unit
) {
    val pendingRegCount = remember(registrationRequests) {
        registrationRequests.count { it.status.equals("PENDING", ignoreCase = true) }
    }
    val pendingProfileCount = remember(profileUpdateRequests) {
        profileUpdateRequests.count { it.status.equals("PENDING", ignoreCase = true) }
    }
    val pendingCount = pendingRegCount + pendingProfileCount
    var selectedTab by remember { mutableIntStateOf(0) }
    var editingEmployee by remember { mutableStateOf<Employee?>(null) }
    var showStaffSheet by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Employee?>(null) }
    var showClearDemoConfirm by remember { mutableStateOf(false) }

    var approveTarget by remember { mutableStateOf<RegistrationRequest?>(null) }
    var rejectTarget by remember { mutableStateOf<RegistrationRequest?>(null) }
    var deleteReqTarget by remember { mutableStateOf<RegistrationRequest?>(null) }

    var approveProfileTarget by remember { mutableStateOf<ProfileUpdateRequest?>(null) }
    var rejectProfileTarget by remember { mutableStateOf<ProfileUpdateRequest?>(null) }
    var deleteProfileTarget by remember { mutableStateOf<ProfileUpdateRequest?>(null) }

    val hasDemoStaff = employees.any { it.name.contains("[Demo]", ignoreCase = true) }

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = {
                    Text(
                        text = "Staff (${employees.size})",
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = {
                    Text(
                        text = if (pendingCount > 0) "Approvals ($pendingCount)" else "Approvals",
                        fontSize = 12.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (selectedTab == 0) {
                IcyGlossyButton(
                    onClick = {
                        editingEmployee = null
                        showStaffSheet = true
                    },
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .testTag("add_employee_button")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    BilingualPillLabel(
                        english = "Add Staff / User",
                        urdu = "نیا اسٹاف شامل کریں",
                        textColor = Color.White
                    )
                }

                if (activeRole.isOwner && hasDemoStaff) {
                    OutlinedButton(
                        onClick = { showClearDemoConfirm = true },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                        modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp)
                    ) {
                        Icon(Icons.Filled.DeleteSweep, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Clear All [Demo] Sample Records", color = DangerRed, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }

                if (employees.isEmpty()) {
                    BrohiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.Group, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(40.dp))
                            Text("No Factory Staff Added Yet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "Tap \"Add Staff / User\" above to register staff with CNIC, WhatsApp, email, and salary.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                employees.forEach { emp ->
                    val displayName = PakistaniFormatters.formatTitleCase(emp.name)
                    BrohiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(42.dp)
                                            .clip(CircleShape)
                                            .background(PrimaryBlueGradient),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = displayName.firstOrNull()?.uppercase() ?: "S",
                                            color = Color.White,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 16.sp
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        if (emp.fatherName.isNotBlank()) {
                                            Text("S/O ${PakistaniFormatters.formatTitleCase(emp.fatherName)}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("${emp.role} • ${emp.shift}", fontSize = 12.sp, color = RoyalBlue, fontWeight = FontWeight.Medium)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = if (emp.active) SuccessGreenSoft else DangerRedSoft
                                ) {
                                    Text(
                                        text = if (emp.active) "Active" else "Inactive",
                                        color = if (emp.active) SuccessGreen else DangerRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                val formattedEmpCnic = PakistaniFormatters.formatCnic(emp.cnic)
                                if (formattedEmpCnic.isNotBlank()) StaffMetaLine(Icons.Filled.Badge, "CNIC: $formattedEmpCnic")
                                val formattedEmpPhone = PakistaniFormatters.formatPhone(emp.phone)
                                val formattedEmpWa = PakistaniFormatters.formatPhone(emp.whatsappNumber)
                                val formattedEmpEmg = PakistaniFormatters.formatPhone(emp.emergencyContact)
                                if (formattedEmpPhone.isNotBlank()) StaffMetaLine(Icons.Filled.Phone, "Mobile: $formattedEmpPhone")
                                if (formattedEmpWa.isNotBlank()) StaffMetaLine(Icons.Filled.Chat, "WhatsApp: $formattedEmpWa")
                                if (emp.email.isNotBlank()) StaffMetaLine(Icons.Filled.Email, "Email: ${emp.email}")
                                if (formattedEmpEmg.isNotBlank()) StaffMetaLine(Icons.Filled.ContactPhone, "Emergency: $formattedEmpEmg")
                                StaffMetaLine(
                                    Icons.Filled.Payments,
                                    "Salary: ${PakistaniFormatters.formatPkr(emp.salary)} • Face: ${if (emp.faceTemplateEncrypted != null) "Enrolled" else "Not enrolled"}"
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        editingEmployee = emp
                                        showStaffSheet = true
                                    },
                                    modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Edit", fontSize = 12.sp, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = onGoToFaceEnroll,
                                    modifier = Modifier.weight(1.1f).heightIn(min = 42.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Filled.Face, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Face", fontSize = 12.sp, maxLines = 1)
                                }
                                OutlinedButton(
                                    onClick = { deleteTarget = emp },
                                    border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.5f)),
                                    modifier = Modifier.weight(1f).heightIn(min = 42.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = null, tint = DangerRed, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Delete", fontSize = 12.sp, color = DangerRed, maxLines = 1)
                                }
                            }
                        }
                    }
                }
            } else {
                // Approvals Tab: Registration Requests + Profile Update Requests (Super Admin & Admin review queue)
                if (pendingCount > 0) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = WarningOrangeSoft,
                        border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "$pendingCount pending request(s) awaiting approval ($pendingRegCount account, $pendingProfileCount profile) • منظوری درکار ہے",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF9A3412),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
                        )
                    }
                }

                if (registrationRequests.isEmpty() && profileUpdateRequests.isEmpty()) {
                    BrohiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(Icons.Filled.SupervisorAccount, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(40.dp))
                            Text("No Approval Requests Yet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                            Text(
                                "New staff registrations and profile change requests appear here for Super Admin / Admin approval.",
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (profileUpdateRequests.isNotEmpty()) {
                    Text(
                        text = "Profile Update Requests (${profileUpdateRequests.size}) • پروفائل اپڈیٹ",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DeepBlue
                    )
                    profileUpdateRequests.forEach { profReq ->
                        val statusUpper = profReq.status.uppercase()
                        val (badgeBg, badgeFg) = when (statusUpper) {
                            "APPROVED" -> SuccessGreenSoft to SuccessGreen
                            "REJECTED" -> DangerRedSoft to DangerRed
                            else -> WarningOrangeSoft to WarningOrange
                        }
                        val dispName = PakistaniFormatters.formatTitleCase(profReq.name)
                        BrohiCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                        Text(dispName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                        if (profReq.fatherName.isNotBlank()) {
                                            Text(
                                                "S/O ${PakistaniFormatters.formatTitleCase(profReq.fatherName)}",
                                                fontSize = 11.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            "Profile Edit Request • ${profReq.shift}",
                                            fontSize = 12.sp,
                                            color = RoyalBlue,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                    Surface(shape = RoundedCornerShape(8.dp), color = badgeBg) {
                                        Text(
                                            text = statusUpper,
                                            color = badgeFg,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 10.sp,
                                            maxLines = 1,
                                            softWrap = false,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }

                                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    val cnicFormatted = PakistaniFormatters.formatCnic(profReq.cnic)
                                    if (cnicFormatted.isNotBlank()) StaffMetaLine(Icons.Filled.Badge, "CNIC: $cnicFormatted")
                                    val phoneFormatted = PakistaniFormatters.formatPhone(profReq.phone)
                                    val waFormatted = PakistaniFormatters.formatPhone(profReq.whatsappNumber)
                                    val emgFormatted = PakistaniFormatters.formatPhone(profReq.emergencyContact)
                                    if (phoneFormatted.isNotBlank()) StaffMetaLine(Icons.Filled.Phone, "Mobile: $phoneFormatted")
                                    if (waFormatted.isNotBlank()) StaffMetaLine(Icons.Filled.Chat, "WhatsApp: $waFormatted")
                                    if (profReq.email.isNotBlank()) StaffMetaLine(Icons.Filled.Email, "Email: ${profReq.email}")
                                    if (emgFormatted.isNotBlank()) StaffMetaLine(Icons.Filled.ContactPhone, "Emergency: $emgFormatted")
                                    if (profReq.joinDate.isNotBlank()) StaffMetaLine(Icons.Filled.CalendarMonth, "Join Date: ${profReq.joinDate}")
                                    if (!profReq.reviewNote.isNullOrBlank()) {
                                        StaffMetaLine(Icons.Filled.Description, "Note: ${profReq.reviewNote}")
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (statusUpper == "PENDING") {
                                        IcyGlossyButton(
                                            onClick = { approveProfileTarget = profReq },
                                            tone = IcyGlossyTone.GREEN,
                                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                            modifier = Modifier.weight(1.3f).heightIn(min = 42.dp)
                                        ) {
                                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("Approve", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                        }
                                        OutlinedButton(
                                            onClick = { rejectProfileTarget = profReq },
                                            border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.6f)),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                            modifier = Modifier.weight(1f).heightIn(min = 42.dp)
                                        ) {
                                            Text("Reject", fontSize = 12.sp, color = WarningOrange, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                        }
                                    }
                                    IconButton(
                                        onClick = { deleteProfileTarget = profReq },
                                        modifier = Modifier.size(40.dp)
                                    ) {
                                        Icon(Icons.Filled.Delete, contentDescription = "Delete profile update request", tint = DangerRed, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                if (registrationRequests.isNotEmpty()) {
                    Text(
                        text = "New Account Registrations (${registrationRequests.size}) • نئے اکاؤنٹس",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = DeepBlue
                    )
                }

                registrationRequests.forEach { req ->
                    val statusUpper = req.status.uppercase()
                    val (badgeBg, badgeFg) = when (statusUpper) {
                        "APPROVED" -> SuccessGreenSoft to SuccessGreen
                        "REJECTED" -> DangerRedSoft to DangerRed
                        else -> WarningOrangeSoft to WarningOrange
                    }
                    val reqDisplayName = PakistaniFormatters.formatTitleCase(req.name)
                    BrohiCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                                    Text(reqDisplayName, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                    if (req.fatherName.isNotBlank()) {
                                        Text(
                                            "S/O ${PakistaniFormatters.formatTitleCase(req.fatherName)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        "Requested: ${req.requestedRole} • ${req.shift}",
                                        fontSize = 12.sp,
                                        color = RoyalBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Surface(shape = RoundedCornerShape(8.dp), color = badgeBg) {
                                    Text(
                                        text = statusUpper,
                                        color = badgeFg,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        softWrap = false,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                val formattedReqCnic = PakistaniFormatters.formatCnic(req.cnic)
                                if (formattedReqCnic.isNotBlank()) StaffMetaLine(Icons.Filled.Badge, "CNIC: $formattedReqCnic")
                                val formattedReqPhone = PakistaniFormatters.formatPhone(req.phone)
                                val formattedReqWa = PakistaniFormatters.formatPhone(req.whatsappNumber)
                                val formattedReqEmg = PakistaniFormatters.formatPhone(req.emergencyContact)
                                if (formattedReqPhone.isNotBlank()) StaffMetaLine(Icons.Filled.Phone, "Mobile: $formattedReqPhone")
                                if (formattedReqWa.isNotBlank()) StaffMetaLine(Icons.Filled.Chat, "WhatsApp: $formattedReqWa")
                                if (req.email.isNotBlank()) StaffMetaLine(Icons.Filled.Email, "Email: ${req.email}")
                                if (formattedReqEmg.isNotBlank()) StaffMetaLine(Icons.Filled.ContactPhone, "Emergency: $formattedReqEmg")
                                if (!req.reviewNote.isNullOrBlank()) {
                                    StaffMetaLine(Icons.Filled.Description, "Note: ${req.reviewNote}")
                                }
                                StaffMetaLine(
                                    Icons.Filled.Face,
                                    if (!req.faceTemplateEncrypted.isNullOrBlank()) {
                                        "Face Biometrics: Registered ✓ (چہرہ رجسٹرڈ ہے)"
                                    } else {
                                        "Face Biometrics: Not captured yet"
                                    }
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (statusUpper == "PENDING") {
                                    IcyGlossyButton(
                                        onClick = { approveTarget = req },
                                        tone = IcyGlossyTone.GREEN,
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                        modifier = Modifier.weight(1.3f).heightIn(min = 42.dp)
                                    ) {
                                        Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Approve", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = { rejectTarget = req },
                                        border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.6f)),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier.weight(1f).heightIn(min = 42.dp)
                                    ) {
                                        Text("Reject", fontSize = 12.sp, color = WarningOrange, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                    }
                                }
                                IconButton(
                                    onClick = { deleteReqTarget = req },
                                    modifier = Modifier.size(40.dp)
                                ) {
                                    Icon(Icons.Filled.Delete, contentDescription = "Delete request", tint = DangerRed, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modern Crystal Bilingual ModalBottomSheet Form for Add / Edit Staff
    if (showStaffSheet) {
        val base = editingEmployee
        var name by remember(base) { mutableStateOf(base?.name ?: "") }
        var fatherName by remember(base) { mutableStateOf(base?.fatherName ?: "") }
        var cnicDigits by remember(base) { mutableStateOf(PakistaniFormatters.normalizeCnicDigits(base?.cnic ?: "")) }
        var phoneDigits by remember(base) { mutableStateOf(PakistaniFormatters.normalizePhoneDigits(base?.phone ?: "")) }
        var whatsappDigits by remember(base) { mutableStateOf(PakistaniFormatters.normalizePhoneDigits(base?.whatsappNumber ?: "")) }
        var email by remember(base) { mutableStateOf(base?.email ?: "") }
        var emergencyDigits by remember(base) { mutableStateOf(PakistaniFormatters.normalizePhoneDigits(base?.emergencyContact ?: "")) }
        var role by remember(base) { mutableStateOf(base?.role ?: "Staff") }
        var shift by remember(base) { mutableStateOf(base?.shift ?: "Morning (8AM - 4PM)") }
        var salary by remember(base) { mutableStateOf(if (base != null) base.salary.toLong().toString() else "35000") }
        var initialSalary by remember(base) {
            val initVal = base?.initialSalary?.takeIf { it > 0 } ?: base?.salary
            mutableStateOf(if (initVal != null) initVal.toLong().toString() else "")
        }
        var joinDate by remember(base) { mutableStateOf(base?.joinDate?.ifBlank { PakistaniFormatters.todayIso() } ?: PakistaniFormatters.todayIso()) }
        var isActive by remember(base) { mutableStateOf(base?.active ?: true) }
        var triedSubmit by remember { mutableStateOf(false) }
        var formError by remember { mutableStateOf<String?>(null) }
        var popupTitle by remember { mutableStateOf<String?>(null) }
        var popupMessage by remember { mutableStateOf<String?>(null) }
        val emergencyMatchesPrimary = PakistaniFormatters.isEmergencySameAsPrimary(
            emergencyRaw = emergencyDigits,
            mobileRaw = phoneDigits,
            whatsappRaw = whatsappDigits
        )

        ModalBottomSheet(
            onDismissRequest = { showStaffSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            dragHandle = null
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryBlueGradient)
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Filled.Badge, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                            }
                            Column {
                                Text(
                                    text = if (base == null) "Add Factory Staff (نیا اسٹاف)" else "Edit Staff Profile (ترمیم)",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 17.sp
                                )
                                Text(
                                    text = "Complete identity, contact & salary details",
                                    color = Color(0xFFDCE8FF),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        IconButton(onClick = { showStaffSheet = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    FormSectionCard(title = "Personal Identity (ذاتی تفصیلات)", icon = Icons.Filled.Person) {
                        CrystalTextField(
                            value = name,
                            onValueChange = { name = it; formError = null },
                            englishLabel = "Full Name *",
                            urduLabel = "پورا نام",
                            leadingIcon = Icons.Filled.Person,
                            placeholder = "e.g. Muhammad Tariq"
                        )
                        CrystalTextField(
                            value = fatherName,
                            onValueChange = { fatherName = it },
                            englishLabel = "Father's Name",
                            urduLabel = "والد کا نام",
                            leadingIcon = Icons.Filled.SupervisorAccount
                        )
                        CnicCrystalTextField(
                            digitsValue = cnicDigits,
                            onDigitsChange = {
                                cnicDigits = it
                                formError = null
                            },
                            leadingIcon = Icons.Filled.Badge,
                            isError = triedSubmit && cnicDigits.isNotEmpty() && !PakistaniFormatters.isValidCnic(cnicDigits)
                        )
                        CrystalTextField(
                            value = email,
                            onValueChange = { email = it },
                            englishLabel = "Email Address",
                            urduLabel = "ای میل",
                            leadingIcon = Icons.Filled.Email,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                        )
                    }

                    FormSectionCard(title = "Contact Numbers (رابطہ نمبر)", icon = Icons.Filled.Phone) {
                        PhoneCrystalTextField(
                            digitsValue = phoneDigits,
                            onDigitsChange = { phoneDigits = it; formError = null },
                            englishLabel = "Mobile Number *",
                            urduLabel = "موبائل نمبر",
                            leadingIcon = Icons.Filled.Phone,
                            isError = triedSubmit && !PakistaniFormatters.isValidPhone(phoneDigits)
                        )
                        PhoneCrystalTextField(
                            digitsValue = whatsappDigits,
                            onDigitsChange = { whatsappDigits = it; formError = null },
                            englishLabel = "WhatsApp Number",
                            urduLabel = "واٹس ایپ نمبر",
                            leadingIcon = Icons.Filled.Chat,
                            helperText = "Can match Mobile or enter different (موبائل جیسا بھی رکھ سکتے ہیں)",
                            isError = triedSubmit && whatsappDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(whatsappDigits),
                            actionChip = {
                                if (phoneDigits.isNotEmpty() && whatsappDigits != phoneDigits) {
                                    TextButton(
                                        onClick = { whatsappDigits = phoneDigits; formError = null },
                                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                    ) {
                                        Text("Same", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        )
                        PhoneCrystalTextField(
                            digitsValue = emergencyDigits,
                            onDigitsChange = { emergencyDigits = it; formError = null },
                            englishLabel = "Emergency Contact",
                            urduLabel = "ہنگامی نمبر",
                            leadingIcon = Icons.Filled.ContactPhone,
                            helperText = "Must be different from Mobile / WhatsApp (الگ نمبر درج کریں)",
                            errorMessage = if (emergencyMatchesPrimary) {
                                "Emergency contact cannot be same as Mobile / WhatsApp (ہنگامی نمبر مختلف ہونا ضروری ہے)"
                            } else {
                                "Number is not complete: enter 11 digits e.g. 0333-2828285 (نمبر مکمل نہیں ہے)"
                            },
                            isError = emergencyMatchesPrimary ||
                                (triedSubmit && emergencyDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(emergencyDigits))
                        )
                    }

                    FormSectionCard(title = "Job Role & Salary (عہدہ اور تنخواہ)", icon = Icons.Filled.Payments) {
                        Text("Access Role (عہدہ)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        val availableRoles = if (activeRole.isOwner) {
                            REGISTRATION_ROLE_OPTIONS
                        } else {
                            listOf("Admin", "Manager", "Staff")
                        }
                        ModernRoleSelectorCardGrid(
                            selectedRole = role,
                            roles = availableRoles,
                            onSelectRole = { role = it }
                        )

                        Text("Work Shift / Timing (شفٹ یا مقررہ وقت نہیں)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        ModernShiftSelector(
                            selectedShift = shift,
                            shifts = WORK_SHIFT_OPTIONS,
                            onSelectShift = { shift = it }
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                            CrystalTextField(
                                value = initialSalary,
                                onValueChange = { initialSalary = it },
                                englishLabel = "Start Salary",
                                urduLabel = "شروع تنخواہ",
                                leadingIcon = Icons.Filled.Payments,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            CrystalTextField(
                                value = salary,
                                onValueChange = { salary = it },
                                englishLabel = "Current Salary",
                                urduLabel = "موجودہ تنخواہ",
                                leadingIcon = Icons.Filled.Payments,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        CrystalTextField(
                            value = joinDate,
                            onValueChange = { joinDate = it },
                            englishLabel = "Joining Date",
                            urduLabel = "شمولیت کی تاریخ",
                            leadingIcon = Icons.Filled.CalendarMonth,
                            placeholder = "YYYY-MM-DD"
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Active Staff Member (فعال)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            Switch(checked = isActive, onCheckedChange = { isActive = it })
                        }
                    }

                    if (!formError.isNullOrBlank()) {
                        Text(formError!!, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                Surface(
                    tonalElevation = 4.dp,
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showStaffSheet = false },
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier.weight(0.38f).heightIn(min = 48.dp)
                        ) {
                            Text("Cancel", maxLines = 1)
                        }
                        IcyGlossyButton(
                            onClick = {
                                triedSubmit = true
                                if (name.isBlank()) {
                                    formError = "Please enter staff member's full name"
                                    popupTitle = "Full Name Required (نام ضروری ہے)"
                                    popupMessage = formError
                                    return@IcyGlossyButton
                                }
                                if (cnicDigits.isNotEmpty() && !PakistaniFormatters.isValidCnic(cnicDigits)) {
                                    formError = "CNIC is not complete. Please enter 13 digits (e.g. 44204-1253835-5)."
                                    popupTitle = "CNIC Number Not Complete"
                                    popupMessage = "CNIC number is not complete. Please enter all 13 digits (e.g. 44204-1253835-5).\n\nشناختی کارڈ نمبر مکمل نہیں ہے، براہ کرم 13 ہندسے درج کریں۔"
                                    return@IcyGlossyButton
                                }
                                if (!PakistaniFormatters.isValidPhone(phoneDigits)) {
                                    formError = "Mobile Number is not complete (11 digits required)."
                                    popupTitle = "Number Is Not Complete (نمبر مکمل نہیں ہے)"
                                    popupMessage = "Mobile number is not complete. Please enter all 11 digits (e.g. 0333-2828285).\n\nموبائل نمبر مکمل نہیں ہے، براہ کرم پورے 11 ہندسے درج کریں (مثلاً 0333-2828285)۔"
                                    return@IcyGlossyButton
                                }
                                if (whatsappDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(whatsappDigits)) {
                                    formError = "WhatsApp Number is not complete (11 digits required)."
                                    popupTitle = "Number Is Not Complete (نمبر مکمل نہیں ہے)"
                                    popupMessage = "WhatsApp number is not complete. Please enter all 11 digits (e.g. 0333-2828285).\n\nواٹس ایپ نمبر مکمل نہیں ہے، براہ کرم پورے 11 ہندسے درج کریں۔"
                                    return@IcyGlossyButton
                                }
                                if (emergencyDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(emergencyDigits)) {
                                    formError = "Emergency Contact Number is not complete (11 digits required)."
                                    popupTitle = "Number Is Not Complete (نمبر مکمل نہیں ہے)"
                                    popupMessage = "Emergency contact number is not complete. Please enter all 11 digits (e.g. 0333-2828285).\n\nہنگامی نمبر مکمل نہیں ہے، براہ کرم پورے 11 ہندسے درج کریں۔"
                                    return@IcyGlossyButton
                                }
                                if (emergencyMatchesPrimary) {
                                    formError = "Emergency contact cannot be same as Mobile or WhatsApp number."
                                    popupTitle = "Different Emergency Number Required"
                                    popupMessage = "Emergency contact number cannot be the same as Mobile or WhatsApp number. Please enter a different family or alternate number.\n\nہنگامی رابطہ نمبر موبائل یا واٹس ایپ جیسا نہیں ہو سکتا، براہ کرم کوئی دوسرا نمبر درج کریں۔"
                                    return@IcyGlossyButton
                                }
                                val currentSal = salary.toDoubleOrNull() ?: 35000.0
                                val startSal = initialSalary.toDoubleOrNull() ?: currentSal
                                onSaveEmployee(
                                    (base ?: Employee()).copy(
                                        name = PakistaniFormatters.formatTitleCase(name),
                                        fatherName = PakistaniFormatters.formatTitleCase(fatherName),
                                        phone = PakistaniFormatters.formatPhone(phoneDigits),
                                        whatsappNumber = PakistaniFormatters.formatPhone(whatsappDigits),
                                        email = email.trim(),
                                        cnic = PakistaniFormatters.formatCnic(cnicDigits),
                                        role = role,
                                        salary = currentSal,
                                        initialSalary = startSal,
                                        shift = shift,
                                        joinDate = joinDate.trim().ifBlank { PakistaniFormatters.todayIso() },
                                        emergencyContact = PakistaniFormatters.formatPhone(emergencyDigits),
                                        active = isActive
                                    )
                                )
                                showStaffSheet = false
                            },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(0.62f)
                                .heightIn(min = 48.dp)
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            BilingualPillLabel(english = "Save Profile", urdu = "محفوظ", textColor = Color.White)
                        }
                    }
                }
            }
        }

        ValidationPopupDialog(
            title = popupTitle,
            message = popupMessage,
            onDismiss = { popupMessage = null }
        )
    }

    // Edit & Approve Dialog for Registration Requests
    approveTarget?.let { req ->
        val superAdminOnlyBlocked = !activeRole.isOwner &&
            UserRole.fromRequestedRole(req.requestedRole) == UserRole.OWNER
        val initialAssignedRole = if (superAdminOnlyBlocked) "Admin" else req.requestedRole.ifBlank { "Staff" }
        var assignedRole by remember(req.id) { mutableStateOf(initialAssignedRole) }
        var assignedShift by remember(req.id) { mutableStateOf(req.shift.ifBlank { "Morning (8AM - 4PM)" }) }
        var startSalaryText by remember(req.id) { mutableStateOf("35000") }
        var reviewNote by remember(req.id) { mutableStateOf("") }
        val allowedApproveRoles = if (activeRole.isOwner) {
            REGISTRATION_ROLE_OPTIONS
        } else {
            listOf("Admin", "Manager", "Staff")
        }

        CrystalFormDialog(
            onDismissRequest = { approveTarget = null },
            title = "Approve ${PakistaniFormatters.formatTitleCase(req.name)}",
            subtitle = "عہدہ، شفٹ اور تنخواہ مقرر کر کے منظوری دیں",
            icon = Icons.Filled.CheckCircle,
            confirmText = "Approve & Add",
            dismissText = "Cancel",
            accentColor = SuccessGreen,
            onConfirm = {
                val sal = startSalaryText.toDoubleOrNull() ?: 35000.0
                onApproveRequest(req, assignedRole, assignedShift, sal, reviewNote.trim().ifBlank { null })
                approveTarget = null
            }
        ) {
            Text(
                "Review and adjust role, shift, and starting salary before adding to Staff Directory:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (superAdminOnlyBlocked) {
                Text(
                    text = "Note: Super Admin requested — only an existing Super Admin can grant Super Admin access.",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = WarningOrange
                )
            }
            Text("Assign Role (عہدہ)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepBlue)
            ModernRoleSelectorCardGrid(
                selectedRole = assignedRole,
                roles = allowedApproveRoles,
                onSelectRole = { assignedRole = it }
            )
            Text("Work Shift / Timing (شفٹ)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = DeepBlue)
            ModernShiftSelector(
                selectedShift = assignedShift,
                shifts = WORK_SHIFT_OPTIONS,
                onSelectShift = { assignedShift = it }
            )
            CrystalTextField(
                value = startSalaryText,
                onValueChange = { startSalaryText = it },
                englishLabel = "Starting Salary (Rs.)",
                urduLabel = "شروع تنخواہ",
                leadingIcon = Icons.Filled.Payments,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            CrystalTextField(
                value = reviewNote,
                onValueChange = { reviewNote = it },
                englishLabel = "Approval Note (Optional)",
                urduLabel = "نوٹ",
                leadingIcon = Icons.Filled.Description
            )
        }
    }

    rejectTarget?.let { req ->
        var reason by remember(req.id) { mutableStateOf("") }
        CrystalFormDialog(
            onDismissRequest = { rejectTarget = null },
            title = "Reject Registration?",
            subtitle = "رجسٹریشن درخواست مسترد کریں",
            icon = Icons.Filled.Cancel,
            confirmText = "Decline Request",
            dismissText = "Cancel",
            accentColor = WarningOrange,
            onConfirm = {
                onRejectRequest(req, reason.trim().ifBlank { null })
                rejectTarget = null
            }
        ) {
            Text("Decline registration request for ${PakistaniFormatters.formatTitleCase(req.name)}?", fontSize = 13.sp)
            CrystalTextField(
                value = reason,
                onValueChange = { reason = it },
                englishLabel = "Reason (Optional)",
                urduLabel = "وجہ",
                leadingIcon = Icons.Filled.Description
            )
        }
    }

    deleteReqTarget?.let { req ->
        CrystalFormDialog(
            onDismissRequest = { deleteReqTarget = null },
            title = "Delete Request?",
            subtitle = "درخواست مستقل حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteRequest(req.id)
                deleteReqTarget = null
            }
        ) {
            Text("Permanently remove registration request from ${req.name}?", fontSize = 13.sp)
        }
    }

    approveProfileTarget?.let { profReq ->
        var reviewNote by remember(profReq.id) { mutableStateOf("") }
        CrystalFormDialog(
            onDismissRequest = { approveProfileTarget = null },
            title = "Approve Profile Update",
            subtitle = "پروفائل میں تبدیلیاں منظور کریں",
            icon = Icons.Filled.CheckCircle,
            confirmText = "Approve Update",
            dismissText = "Cancel",
            accentColor = SuccessGreen,
            onConfirm = {
                onApproveProfileUpdate(profReq, reviewNote.trim().ifBlank { null })
                approveProfileTarget = null
            }
        ) {
            Text(
                text = "Confirm applying these updated details for ${PakistaniFormatters.formatTitleCase(profReq.name)}:",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, CrystalBorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Name: ${PakistaniFormatters.formatTitleCase(profReq.name)}", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    if (profReq.fatherName.isNotBlank()) Text("Father: ${PakistaniFormatters.formatTitleCase(profReq.fatherName)}", fontSize = 12.sp)
                    if (profReq.cnic.isNotBlank()) Text("CNIC: ${PakistaniFormatters.formatCnic(profReq.cnic)}", fontSize = 12.sp)
                    if (profReq.phone.isNotBlank()) Text("Mobile: ${PakistaniFormatters.formatPhone(profReq.phone)}", fontSize = 12.sp)
                    if (profReq.whatsappNumber.isNotBlank()) Text("WhatsApp: ${PakistaniFormatters.formatPhone(profReq.whatsappNumber)}", fontSize = 12.sp)
                    if (profReq.email.isNotBlank()) Text("Email: ${profReq.email}", fontSize = 12.sp)
                    if (profReq.emergencyContact.isNotBlank()) Text("Emergency: ${PakistaniFormatters.formatPhone(profReq.emergencyContact)}", fontSize = 12.sp)
                    Text("Shift: ${profReq.shift} • Joined: ${profReq.joinDate}", fontSize = 12.sp, color = RoyalBlue)
                }
            }
            CrystalTextField(
                value = reviewNote,
                onValueChange = { reviewNote = it },
                englishLabel = "Approval Note (Optional)",
                urduLabel = "نوٹ",
                leadingIcon = Icons.Filled.Description
            )
        }
    }

    rejectProfileTarget?.let { profReq ->
        var reason by remember(profReq.id) { mutableStateOf("") }
        CrystalFormDialog(
            onDismissRequest = { rejectProfileTarget = null },
            title = "Reject Profile Update?",
            subtitle = "پروفائل تبدیلی مسترد کریں",
            icon = Icons.Filled.Cancel,
            confirmText = "Decline Update",
            dismissText = "Cancel",
            accentColor = WarningOrange,
            onConfirm = {
                onRejectProfileUpdate(profReq, reason.trim().ifBlank { null })
                rejectProfileTarget = null
            }
        ) {
            Text("Decline profile update request from ${PakistaniFormatters.formatTitleCase(profReq.name)}?", fontSize = 13.sp)
            CrystalTextField(
                value = reason,
                onValueChange = { reason = it },
                englishLabel = "Reason (Optional)",
                urduLabel = "وجہ",
                leadingIcon = Icons.Filled.Description
            )
        }
    }

    deleteProfileTarget?.let { profReq ->
        CrystalFormDialog(
            onDismissRequest = { deleteProfileTarget = null },
            title = "Delete Update Request?",
            subtitle = "پروفائل درخواست حذف کریں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteProfileUpdate(profReq.id)
                deleteProfileTarget = null
            }
        ) {
            Text("Remove profile update request from ${profReq.name}?", fontSize = 13.sp)
        }
    }

    deleteTarget?.let { emp ->
        CrystalFormDialog(
            onDismissRequest = { deleteTarget = null },
            title = "Delete Staff Member?",
            subtitle = "ملازم کو فہرست سے ہٹائیں",
            icon = Icons.Filled.Delete,
            confirmText = "Delete",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onDeleteEmployee(emp.id, emp.name)
                deleteTarget = null
            }
        ) {
            Text("Are you sure you want to permanently remove ${emp.name} from the factory staff list?", fontSize = 13.sp)
        }
    }

    if (showClearDemoConfirm) {
        CrystalFormDialog(
            onDismissRequest = { showClearDemoConfirm = false },
            title = "Clear All [Demo] Data?",
            subtitle = "نمونہ ریکارڈز مستقل حذف کریں",
            icon = Icons.Filled.DeleteSweep,
            confirmText = "Clear Demo Data",
            dismissText = "Cancel",
            isDestructive = true,
            onConfirm = {
                onClearDemoData()
                showClearDemoConfirm = false
            }
        ) {
            Text(
                "This will delete all pre-loaded [Demo] sample records (staff, attendance, sales, expenses, machines, etc.) while keeping your own real records intact.",
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun StaffMetaLine(icon: ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(icon, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(14.dp))
        Text(text, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun FormSectionCard(
    title: String,
    icon: ImageVector,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(17.dp))
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurface)
            }
            content()
        }
    }
}
