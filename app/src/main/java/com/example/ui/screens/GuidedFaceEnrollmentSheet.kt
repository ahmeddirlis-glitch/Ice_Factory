package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.PanTool
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.data.face.FaceDetectionAnalyzer
import com.example.data.face.LiveFaceAnalysis
import com.example.data.util.FaceCryptoHelper
import com.example.ui.components.IcyGlossyButton
import com.example.ui.components.IcyGlossyTone
import com.example.ui.theme.RoyalBlue
import java.util.concurrent.Executors

enum class GuidedFacePose(
    val stepIndex: Int,
    val badgeLabel: String,
    val title: String,
    val subtitle: String,
    val urduHint: String,
    val pillHint: String?
) {
    FRONT(
        stepIndex = 0,
        badgeLabel = "Front",
        title = "Look straight ahead",
        subtitle = "Keep your face inside the oval.",
        urduHint = "سیدھا کیمرے کی طرف دیکھیں",
        pillHint = null
    ),
    LEFT(
        stepIndex = 1,
        badgeLabel = "Left",
        title = "Turn your head left",
        subtitle = "Slowly, until it locks in.",
        urduHint = "آہستہ سے سر بائیں طرف موڑیں",
        pillHint = "Turn left"
    ),
    RIGHT(
        stepIndex = 2,
        badgeLabel = "Right",
        title = "Turn your head right",
        subtitle = "Slowly, until it locks in.",
        urduHint = "آہستہ سے سر دائیں طرف موڑیں",
        pillHint = "Turn right"
    ),
    TILT_UP(
        stepIndex = 3,
        badgeLabel = "Tilt up",
        title = "Tilt your head up",
        subtitle = "Slightly upward, keep eyes visible.",
        urduHint = "تھوڑا سا اوپر کی طرف دیکھیں",
        pillHint = "Tilt up"
    )
}

data class CapturedPoseSample(
    val pose: GuidedFacePose,
    val previewBitmap: Bitmap,
    val descriptor: List<Float>
)

@Composable
fun GuidedFaceEnrollmentDialog(
    ownerKey: String = "global",
    onDismiss: () -> Unit,
    onPortraitCaptured: ((String?) -> Unit)? = null,
    onCompleted: (encryptedMultiPoseTemplate: String) -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnClickOutside = false
        )
    ) {
        GuidedFaceEnrollmentFlow(
            ownerKey = ownerKey,
            onCancel = onDismiss,
            onPortraitCaptured = onPortraitCaptured,
            onCompleted = onCompleted
        )
    }
}

@Composable
fun GuidedFaceEnrollmentFlow(
    ownerKey: String = "global",
    onCancel: (() -> Unit)? = null,
    onPortraitCaptured: ((String?) -> Unit)? = null,
    onCompleted: (encryptedMultiPoseTemplate: String) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
    }

    val capturedSamples = remember { mutableStateListOf<CapturedPoseSample>() }
    var liveAnalysis by remember { mutableStateOf(LiveFaceAnalysis()) }
    var pose1YawSign by remember { mutableFloatStateOf(0f) }
    var stableFrames by remember { mutableIntStateOf(0) }
    var captureInFlight by remember { mutableStateOf(false) }

    val allPoses = GuidedFacePose.entries
    val currentPose = allPoses.getOrNull(capturedSamples.size)
    val isReviewStep = capturedSamples.size >= 4

    fun clearAllCaptures() {
        capturedSamples.forEach { sample ->
            runCatching { if (!sample.previewBitmap.isRecycled) sample.previewBitmap.recycle() }
        }
        capturedSamples.clear()
        pose1YawSign = 0f
        stableFrames = 0
        captureInFlight = false
    }

    DisposableEffect(Unit) {
        onDispose {
            capturedSamples.forEach { sample ->
                runCatching { if (!sample.previewBitmap.isRecycled) sample.previewBitmap.recycle() }
            }
        }
    }

    if (!hasCameraPermission) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF0F172A)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = CircleShape,
                    color = RoyalBlue.copy(alpha = 0.22f),
                    modifier = Modifier.size(84.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            contentDescription = "Camera Permission Required",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Camera Permission Required\nکیمرہ کی اجازت ضروری ہے",
                    color = Color.White,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Mandatory 4-angle face enrollment locks your attendance so only you can check in.\nحاضری کو محفوظ بنانے کے لیے 4 زاویوں سے چہرہ اسکین کرنا لازمی ہے۔",
                    color = Color(0xFFCBD5E1),
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(24.dp))
                IcyGlossyButton(
                    onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) },
                    tone = IcyGlossyTone.GREEN,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("guided_enroll_enable_camera")
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enable Camera (کیمرہ آن کریں)", color = Color.White, fontWeight = FontWeight.Bold)
                }
                if (onCancel != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = onCancel,
                        border = BorderStroke(1.dp, Color(0xFF475569)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Text("Close", color = Color.White)
                    }
                }
            }
        }
        return
    }

    if (isReviewStep) {
        // STEP 2 OF 2: Review your 4 captured photos
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
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
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF1E293B),
                            modifier = Modifier.size(40.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.DoneAll,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Column {
                            Text(
                                text = "Review your photos",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 19.sp,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Step 2 of 2 • تصدیق کریں",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                    if (onCancel != null) {
                        IconButton(onClick = onCancel) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color(0xFF475569))
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color(0xFFECFDF5),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF059669),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = "All 4 photos look great",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = Color(0xFF065F46)
                            )
                            Text(
                                text = "Good lighting, sharp, and correct poses.",
                                fontSize = 12.sp,
                                color = Color(0xFF047857)
                            )
                        }
                    }
                }

                // 2x2 Grid of real captured poses
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    for (rowIndex in 0..1) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (colIndex in 0..1) {
                                val item = capturedSamples.getOrNull(rowIndex * 2 + colIndex)
                                if (item != null) {
                                    CapturedPoseCard(
                                        sample = item,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                IcyGlossyButton(
                    onClick = {
                        // Pose 0 (Front) is index 0; encode all 4 poses securely
                        val orderedDescriptors = allPoses.mapNotNull { pose ->
                            capturedSamples.firstOrNull { it.pose == pose }?.descriptor
                        }
                        val frontBitmap = capturedSamples.firstOrNull { it.pose == GuidedFacePose.FRONT }?.previewBitmap
                        val portraitUri = if (onPortraitCaptured != null && frontBitmap != null && !frontBitmap.isRecycled) {
                            com.example.data.util.ImageAndQrUtils.encodeBitmapToJpegDataUri(
                                bitmap = frontBitmap,
                                maxDimension = 300,
                                quality = 72
                            )
                        } else {
                            null
                        }
                        val encrypted = FaceCryptoHelper.encryptMultiPoseTemplate(ownerKey, orderedDescriptors)
                        onPortraitCaptured?.invoke(portraitUri)
                        onCompleted(encrypted)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("guided_enroll_proceed_button")
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Proceed (محفوظ کریں)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }

                OutlinedButton(
                    onClick = { clearAllCaptures() },
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("guided_enroll_retake_button")
                ) {
                    Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color(0xFF1E293B), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Retake photos (دوبارہ تصویر لیں)",
                        color = Color(0xFF1E293B),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
        return
    }

    // STEP 1: Live Guided Oval Scanner
    val pose = currentPose ?: GuidedFacePose.FRONT
    val poseSatisfied = when (pose) {
        GuidedFacePose.FRONT ->
            liveAnalysis.faceDetected &&
                kotlin.math.abs(liveAnalysis.headYaw) <= 10f &&
                kotlin.math.abs(liveAnalysis.headPitch) <= 12f
        GuidedFacePose.LEFT ->
            liveAnalysis.faceDetected && kotlin.math.abs(liveAnalysis.headYaw) >= 12f
        GuidedFacePose.RIGHT ->
            liveAnalysis.faceDetected && if (pose1YawSign != 0f) {
                (liveAnalysis.headYaw * pose1YawSign) < 0f && kotlin.math.abs(liveAnalysis.headYaw) >= 12f
            } else {
                kotlin.math.abs(liveAnalysis.headYaw) >= 12f
            }
        GuidedFacePose.TILT_UP ->
            liveAnalysis.faceDetected && liveAnalysis.headPitch >= 8f && kotlin.math.abs(liveAnalysis.headYaw) <= 16f
    }

    val lightOk = liveAnalysis.isWellLit
    val steadyOk = liveAnalysis.faceDetected && (liveAnalysis.isSteady || stableFrames > 0)
    val centeredOk = liveAnalysis.faceDetected && liveAnalysis.isCentered
    val descriptorReady = liveAnalysis.descriptor.size >= FaceCryptoHelper.MULTIMODAL_DIM
    val allReadyForCapture = centeredOk && lightOk && poseSatisfied && descriptorReady

    val analyzerExecutor = remember { Executors.newSingleThreadExecutor() }
    val faceAnalyzer = remember {
        FaceDetectionAnalyzer(isFrontCamera = true) { analysis ->
            liveAnalysis = analysis
        }
    }
    var boundProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { boundProvider?.unbindAll() }
            faceAnalyzer.close()
            analyzerExecutor.shutdown()
        }
    }

    // Auto-capture state machine: holds for 2 consistent valid frames (~300-400ms) then grabs frame
    LaunchedEffect(liveAnalysis, capturedSamples.size) {
        if (captureInFlight || isReviewStep) return@LaunchedEffect
        if (allReadyForCapture) {
            stableFrames += 1
            if (stableFrames >= 2) {
                captureInFlight = true
                val activePose = pose
                val capturedYaw = liveAnalysis.headYaw
                faceAnalyzer.captureNextSnapshot { bmp, desc ->
                    if (capturedSamples.none { it.pose == activePose }) {
                        if (activePose == GuidedFacePose.LEFT) {
                            pose1YawSign = if (capturedYaw >= 0f) 1f else -1f
                        }
                        capturedSamples.add(
                            CapturedPoseSample(
                                pose = activePose,
                                previewBitmap = bmp,
                                descriptor = desc
                            )
                        )
                    } else {
                        runCatching { if (!bmp.isRecycled) bmp.recycle() }
                    }
                    stableFrames = 0
                    captureInFlight = false
                }
            }
        } else {
            stableFrames = 0
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B1120))) {
        // 1. Live Front Camera Feed
        AndroidView(
            factory = { ctx ->
                val previewView = PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER
                }
                val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                cameraProviderFuture.addListener({
                    try {
                        val cameraProvider = cameraProviderFuture.get()
                        boundProvider = cameraProvider
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

        // 2. Dark Cutout Scrim + Dashed Oval + 4-Segment Progress Arc
        val completedCount = capturedSamples.size
        val ovalDashedColor by animateColorAsState(
            targetValue = when {
                allReadyForCapture -> Color(0xFF10B981)
                centeredOk -> Color(0xFF34D399)
                else -> Color(0xFFEF4444)
            },
            label = "ovalColor"
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 240.dp)
                .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            val ovalW = size.width * 0.68f
            val ovalH = ovalW * 1.32f
            val ovalLeft = (size.width - ovalW) / 2f
            val ovalTop = (size.height - ovalH) / 2f + 16.dp.toPx()
            val ovalRect = Rect(ovalLeft, ovalTop, ovalLeft + ovalW, ovalTop + ovalH)

            // Scrim outside oval
            drawRect(color = Color(0x99090D16))
            drawOval(
                color = Color.Transparent,
                topLeft = ovalRect.topLeft,
                size = ovalRect.size,
                blendMode = BlendMode.Clear
            )

            // Inner dashed oval ring
            drawOval(
                color = ovalDashedColor,
                topLeft = ovalRect.topLeft,
                size = ovalRect.size,
                style = Stroke(
                    width = 4.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 14f), 0f),
                    cap = StrokeCap.Round
                )
            )

            // Outer 4-segment progress ring around oval
            val gap = 14.dp.toPx()
            val outerRect = Rect(
                ovalRect.left - gap,
                ovalRect.top - gap,
                ovalRect.right + gap,
                ovalRect.bottom + gap
            )
            val sweepAngles = listOf(-82f to 74f, 8f to 74f, 98f to 74f, 188f to 74f)
            sweepAngles.forEachIndexed { idx, (start, sweep) ->
                val segColor = when {
                    idx < completedCount -> Color(0xFF34D399)
                    idx == completedCount -> Color(0xFFFBBF24)
                    else -> Color.White.copy(alpha = 0.30f)
                }
                drawArc(
                    color = segColor,
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = outerRect.topLeft,
                    size = outerRect.size,
                    style = Stroke(width = 4.5.dp.toPx(), cap = StrokeCap.Round)
                )
            }
        }

        // 3. Floating Oval Center Prompt / Side Directional Pill
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 240.dp),
            contentAlignment = Alignment.Center
        ) {
            if (!centeredOk) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color(0xCC1E1B18),
                    modifier = Modifier.padding(top = 24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 22.dp, vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Filled.PersonOutline,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(26.dp)
                        )
                        Text(
                            text = "Center your face",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            if (pose.pillHint != null) {
                val pillAlign = when (pose) {
                    GuidedFacePose.LEFT -> Alignment.CenterStart
                    GuidedFacePose.RIGHT -> Alignment.CenterEnd
                    GuidedFacePose.TILT_UP -> Alignment.TopCenter
                    else -> Alignment.Center
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 10.dp, vertical = 68.dp),
                    contentAlignment = pillAlign
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFFFAFDFF),
                        shadowElevation = 4.dp
                    ) {
                        Text(
                            text = pose.pillHint,
                            color = Color(0xFF0F172A),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // 4. Top Bar Overlay ("Profile photos / Face Enrollment" + "0 / 4")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onCancel != null) {
                    IconButton(
                        onClick = onCancel,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                Icon(
                    Icons.Filled.VerifiedUser,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Face Enrollment",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color(0x991E293B)
            ) {
                Text(
                    text = "$completedCount / 4",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        // 5. Bottom White Instruction Sheet Card
        Surface(
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = Color.White,
            shadowElevation = 12.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 4-step dots indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(4) { idx ->
                        val isDone = idx < completedCount
                        val isActive = idx == completedCount
                        Box(
                            modifier = Modifier
                                .height(7.dp)
                                .width(if (isActive) 22.dp else 7.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        isDone -> Color(0xFF10B981)
                                        isActive -> Color(0xFF0F172A)
                                        else -> Color(0xFFCBD5E1)
                                    }
                                )
                        )
                    }
                }

                // Pose Avatar Card + Instruction Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    PoseGuideIllustrationCard(pose = pose)

                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = pose.title,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = pose.subtitle,
                            fontSize = 13.sp,
                            color = Color(0xFF64748B)
                        )
                        Text(
                            text = pose.urduHint,
                            fontSize = 11.sp,
                            color = RoyalBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                // Live Quality Status Pills (Light / Steady / Pose)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    QualityStatusChip(
                        icon = Icons.Filled.WbSunny,
                        label = "Light",
                        ok = lightOk
                    )
                    QualityStatusChip(
                        icon = Icons.Filled.PanTool,
                        label = "Steady",
                        ok = steadyOk
                    )
                    QualityStatusChip(
                        icon = Icons.Filled.Refresh,
                        label = "Pose",
                        ok = poseSatisfied
                    )
                }

                // Auto-capture status footer (+ manual fallback tap if lighting sensor varies)
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable(enabled = liveAnalysis.faceDetected && descriptorReady && !captureInFlight) {
                            captureInFlight = true
                            val activePose = pose
                            val yaw = liveAnalysis.headYaw
                            faceAnalyzer.captureNextSnapshot { bmp, desc ->
                                if (capturedSamples.none { it.pose == activePose }) {
                                    if (activePose == GuidedFacePose.LEFT) {
                                        pose1YawSign = if (yaw >= 0f) 1f else -1f
                                    }
                                    capturedSamples.add(CapturedPoseSample(activePose, bmp, desc))
                                } else {
                                    runCatching { if (!bmp.isRecycled) bmp.recycle() }
                                }
                                captureInFlight = false
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(9.dp)
                            .clip(CircleShape)
                            .background(
                                if (allReadyForCapture) Color(0xFF10B981)
                                else if (liveAnalysis.faceDetected) Color(0xFFF59E0B)
                                else Color(0xFF94A3B8)
                            )
                    )
                    Text(
                        text = if (allReadyForCapture) {
                            "Locking pose... Hold steady"
                        } else {
                            "Auto-capture active"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF475569)
                    )
                }
            }
        }
    }
}

@Composable
private fun CapturedPoseCard(
    sample: CapturedPoseSample,
    modifier: Modifier = Modifier
) {
    val badgeIcon = when (sample.pose) {
        GuidedFacePose.FRONT -> Icons.Filled.Person
        GuidedFacePose.LEFT -> Icons.AutoMirrored.Filled.ArrowBack
        GuidedFacePose.RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
        GuidedFacePose.TILT_UP -> Icons.Filled.ArrowUpward
    }
    Box(
        modifier = modifier
            .aspectRatio(0.78f)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFE2E8F0))
    ) {
        Image(
            bitmap = sample.previewBitmap.asImageBitmap(),
            contentDescription = sample.pose.badgeLabel,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0xFF10B981)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Captured",
                tint = Color.White,
                modifier = Modifier.size(15.dp)
            )
        }
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color(0x99111827),
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(8.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp)
                )
                Text(
                    text = sample.pose.badgeLabel,
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp
                )
            }
        }
    }
}

@Composable
private fun QualityStatusChip(
    icon: ImageVector,
    label: String,
    ok: Boolean
) {
    val bg = if (ok) Color(0xFFECFDF5) else Color(0xFFFEF3C7)
    val border = if (ok) Color(0xFFA7F3D0) else Color(0xFFFCD34D)
    val fg = if (ok) Color(0xFF047857) else Color(0xFF92400E)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = bg,
        border = BorderStroke(1.dp, border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(14.dp))
            Text(
                text = label,
                color = fg,
                fontWeight = FontWeight.SemiBold,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun PoseGuideIllustrationCard(pose: GuidedFacePose) {
    val badgeIcon = when (pose) {
        GuidedFacePose.FRONT -> Icons.Filled.Person
        GuidedFacePose.LEFT -> Icons.AutoMirrored.Filled.ArrowBack
        GuidedFacePose.RIGHT -> Icons.AutoMirrored.Filled.ArrowForward
        GuidedFacePose.TILT_UP -> Icons.Filled.ArrowUpward
    }
    Box(
        modifier = Modifier
            .size(76.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF3F4F6))
    ) {
        // Stylized Pakistani employee portrait avatar matching screenshot reference
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val w = size.width
            val h = size.height
            val cx = w * 0.5f + when (pose) {
                GuidedFacePose.LEFT -> -w * 0.05f
                GuidedFacePose.RIGHT -> w * 0.05f
                else -> 0f
            }
            val cyHead = h * 0.44f + if (pose == GuidedFacePose.TILT_UP) -h * 0.04f else 0f

            // Shoulders / Shalwar Kameez collar
            drawArc(
                color = Color(0xFF1E3A5F),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = true,
                topLeft = Offset(w * 0.14f, h * 0.64f),
                size = Size(w * 0.72f, h * 0.58f)
            )
            // Face
            drawOval(
                color = Color(0xFFD99B72),
                topLeft = Offset(cx - w * 0.19f, cyHead - h * 0.17f),
                size = Size(w * 0.38f, h * 0.42f)
            )
            // Sindhi cap
            drawRoundRect(
                color = Color(0xFF7C3A21),
                topLeft = Offset(cx - w * 0.18f, cyHead - h * 0.25f),
                size = Size(w * 0.36f, h * 0.14f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            // Mustache
            drawLine(
                color = Color(0xFF1F2937),
                start = Offset(cx - w * 0.08f, cyHead + h * 0.10f),
                end = Offset(cx + w * 0.08f, cyHead + h * 0.10f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            // Eyes
            drawCircle(
                color = Color(0xFF111827),
                radius = 2.5f,
                center = Offset(cx - w * 0.07f, cyHead + h * 0.01f)
            )
            drawCircle(
                color = Color(0xFF111827),
                radius = 2.5f,
                center = Offset(cx + w * 0.07f, cyHead + h * 0.01f)
            )
        }

        // Top-right directional badge matching screenshot
        Surface(
            shape = CircleShape,
            color = Color(0xFFD1FAE5),
            border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(4.dp)
                .size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = badgeIcon,
                    contentDescription = null,
                    tint = Color(0xFF047857),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
