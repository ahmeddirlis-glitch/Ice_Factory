package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.FlipToBack
import androidx.compose.material.icons.filled.FlipToFront
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.data.model.FactorySettings
import com.example.data.model.IdCardPhotoSource
import com.example.data.model.resolveDepartmentForRole
import com.example.data.model.resolveIdCardPhoto
import com.example.data.util.ImageAndQrUtils
import com.example.data.util.PakistaniFormatters
import com.example.ui.components.CrystalFullPhotoViewerDialog
import com.example.ui.components.IcyGlossyButton
import com.example.ui.components.UserAvatarImage
import com.example.ui.components.rememberCameraPhotoLauncher
import com.example.ui.theme.CrystalBorderLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import kotlin.math.cos
import kotlin.math.sin

private val CardNavyDark = Color(0xFF001C55)
private val CardNavyMid = Color(0xFF06398C)
private val CardRoyalLight = Color(0xFF0F5AD1)
private val CardCyanRibbon = Color(0xFF35B4FF)

@Composable
fun DigitalEmployeeIdDialog(
    employeeId: String,
    name: String,
    role: String,
    shift: String,
    cnic: String,
    joinDateRaw: String,
    manualIdPhoto: String?,
    profilePhoto: String?,
    attendanceFacePhoto: String?,
    factorySettings: FactorySettings = FactorySettings(),
    onUpdateManualIdPhoto: (String?) -> Unit = {},
    onOpenProfileClick: () -> Unit = {},
    onOpenFaceEnrollClick: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var showBackSide by remember { mutableStateOf(false) }
    var showQrDetails by remember { mutableStateOf(false) }
    var showIdPhotoViewer by remember { mutableStateOf(false) }

    val resolvedPhoto = remember(manualIdPhoto, profilePhoto, attendanceFacePhoto) {
        resolveIdCardPhoto(
            manualIdPhoto = manualIdPhoto,
            profilePhoto = profilePhoto,
            attendanceFacePhoto = attendanceFacePhoto
        )
    }

    val cleanName = PakistaniFormatters.formatTitleCase(name).ifBlank { "Employee" }
    val cleanCnic = PakistaniFormatters.formatCnic(cnic).ifBlank { "42201-1234567-1" }
    val department = resolveDepartmentForRole(role = role, shift = shift)
    val employeeCode = remember(employeeId, joinDateRaw, cleanCnic) {
        ImageAndQrUtils.formatEmployeeIdCode(employeeId, joinDateRaw, cleanCnic)
    }
    val (joiningDate, expiryDate) = remember(joinDateRaw) {
        ImageAndQrUtils.computeJoiningAndOneYearExpiry(joinDateRaw)
    }
    val qrPayload = remember(cleanCnic, employeeCode, cleanName, role) {
        ImageAndQrUtils.buildEmployeeQrPayload(
            cnic = cleanCnic,
            employeeCode = employeeCode,
            name = cleanName,
            role = role
        )
    }
    val qrBitmap = remember(qrPayload) {
        ImageAndQrUtils.generateQrCodeBitmap(
            content = qrPayload,
            sizePx = 384,
            foregroundArgb = android.graphics.Color.rgb(5, 28, 72),
            backgroundArgb = android.graphics.Color.WHITE
        )?.asImageBitmap()
    }

    // Zero-permission Android Photo Picker for Priority 1 (Manual ID Card Photo)
    val idPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val dataUri = ImageAndQrUtils.encodeUriToJpegDataUri(context, uri, maxDimension = 340, quality = 74)
            if (!dataUri.isNullOrBlank()) {
                onUpdateManualIdPhoto(dataUri)
            } else {
                Toast.makeText(context, "Could not process selected image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val launchIdCamera = rememberCameraPhotoLauncher(maxDimension = 360, quality = 74) { dataUri ->
        onUpdateManualIdPhoto(dataUri)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            shadowElevation = 12.dp,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .widthIn(max = 420.dp)
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Top Bar with Flip Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = if (!showBackSide) "Digital ID Card (فرنٹ سائیڈ)" else "Factory Details (بیک سائیڈ)",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = DeepBlue
                        )
                        Text(
                            text = "Photo Source: ${resolvedPhoto.source.labelEn}",
                            fontSize = 11.sp,
                            color = RoyalBlue,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Surface(
                        onClick = { showBackSide = !showBackSide },
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEAF2FF),
                        border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.35f)),
                        modifier = Modifier.testTag("flip_id_card_button")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(
                                imageVector = if (!showBackSide) Icons.Filled.FlipToBack else Icons.Filled.FlipToFront,
                                contentDescription = "Flip ID Card",
                                tint = DeepBlue,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (!showBackSide) "Back" else "Front",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue
                            )
                        }
                    }
                }

                HorizontalDivider(color = CrystalBorderLight)

                // Scrollable Content Column
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(weight = 1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Visual Lanyard ID Card (Matches Brohi Ice Factory Blue Vertical Reference)
                    Crossfade(
                        targetState = showBackSide,
                        animationSpec = tween(260),
                        label = "idCardFlip"
                    ) { isBack ->
                        if (!isBack) {
                            BrohiFrontLanyardCard(
                                name = cleanName,
                                designation = role,
                                department = department,
                                employeeCode = employeeCode,
                                cnic = cleanCnic,
                                joiningDate = joiningDate,
                                expiryDate = expiryDate,
                                photoData = resolvedPhoto.photoData,
                                qrBitmap = qrBitmap,
                                onTapPhoto = { showIdPhotoViewer = true },
                                onTapCamera = { launchIdCamera() },
                                onTapQr = { showQrDetails = !showQrDetails }
                            )
                        } else {
                            BrohiBackLanyardCard(
                                address = factorySettings.address.ifBlank {
                                    "Plot No. 123, Industrial Area,\nNawabshah, Sindh – Pakistan"
                                },
                                phone = factorySettings.phone.ifBlank { "+92-333-2828285" },
                                email = "info@brohiicefactory.com",
                                website = "www.brohiicefactory.com"
                            )
                        }
                    }

                    if (showQrDetails && !showBackSide) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF0F7FF),
                            border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(10.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(Icons.Filled.QrCodeScanner, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(16.dp))
                                    Text("CNIC QR Verification Data", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                                }
                                Text("CNIC: $cleanCnic", fontSize = 11.sp, color = Color(0xFF1E293B), fontWeight = FontWeight.SemiBold)
                                Text(
                                    text = "ID: $employeeCode • Valid From: $joiningDate • Valid To: $expiryDate",
                                    fontSize = 10.sp,
                                    color = Color(0xFF475569)
                                )
                            }
                        }
                    }

                    // 3-Tier Photo Priority Manager (1. Manual -> 2. Profile -> 3. Attendance)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF6FAFF),
                        border = BorderStroke(1.dp, CrystalBorderLight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "ID Card Photo Priority (تصویر کی ترجیح)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue
                            )

                            PhotoPriorityTierRow(
                                priorityNumber = 1,
                                title = "Manual ID Update (مینوئل تصویر)",
                                subtitle = if (!manualIdPhoto.isNullOrBlank()) "Uploaded for ID card" else "Not uploaded yet",
                                isActive = resolvedPhoto.source == IdCardPhotoSource.MANUAL,
                                isAvailable = !manualIdPhoto.isNullOrBlank(),
                                icon = Icons.Filled.PhotoLibrary
                            )
                            PhotoPriorityTierRow(
                                priorityNumber = 2,
                                title = "Profile Picture (پروفائل تصویر)",
                                subtitle = if (!profilePhoto.isNullOrBlank()) "Synced from User Profile" else "No profile photo set",
                                isActive = resolvedPhoto.source == IdCardPhotoSource.PROFILE,
                                isAvailable = !profilePhoto.isNullOrBlank(),
                                icon = Icons.Filled.Person
                            )
                            PhotoPriorityTierRow(
                                priorityNumber = 3,
                                title = "Attendance Face Photo (اٹینڈنس تصویر)",
                                subtitle = if (!attendanceFacePhoto.isNullOrBlank()) "Captured from Face Enrollment / Scan" else "Complete Face Scan to capture",
                                isActive = resolvedPhoto.source == IdCardPhotoSource.ATTENDANCE,
                                isAvailable = !attendanceFacePhoto.isNullOrBlank(),
                                icon = Icons.Filled.Badge
                            )

                            Spacer(modifier = Modifier.height(2.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                IcyGlossyButton(
                                    onClick = { launchIdCamera() },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("camera_manual_id_photo_button")
                                ) {
                                    Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Camera", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                }

                                IcyGlossyButton(
                                    onClick = {
                                        idPhotoPicker.launch(
                                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                        )
                                    },
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("upload_manual_id_photo_button")
                                ) {
                                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (!manualIdPhoto.isNullOrBlank()) "Change Manual" else "Upload Manual",
                                        color = Color.White,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1
                                    )
                                }

                                if (!manualIdPhoto.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = { onUpdateManualIdPhoto(null) },
                                        border = BorderStroke(1.dp, DangerRed.copy(alpha = 0.55f)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                                        modifier = Modifier.testTag("clear_manual_id_photo_button")
                                    ) {
                                        Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(15.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Use Auto", fontSize = 11.sp, color = DangerRed, fontWeight = FontWeight.Bold)
                                    }
                                } else if (profilePhoto.isNullOrBlank() && attendanceFacePhoto.isNullOrBlank()) {
                                    OutlinedButton(
                                        onClick = {
                                            onDismiss()
                                            onOpenFaceEnrollClick()
                                        },
                                        border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text("Scan Face", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            onDismiss()
                                            onOpenProfileClick()
                                        },
                                        border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.5f)),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp)
                                    ) {
                                        Text("Edit Profile", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                IcyGlossyButton(
                    onClick = onDismiss,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .testTag("close_id_card_button")
                ) {
                    Text("Close (بند کریں)", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                }
            }
        }
    }

    if (showIdPhotoViewer) {
        CrystalFullPhotoViewerDialog(
            title = "ID Card Photo (آئی ڈی تصویر)",
            subtitle = "$cleanName • ${resolvedPhoto.source.labelEn}",
            photoUrl = resolvedPhoto.photoData,
            onDismiss = { showIdPhotoViewer = false },
            onTakeCameraPhoto = { launchIdCamera() },
            onPickFromGallery = {
                idPhotoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            removeLabel = if (!manualIdPhoto.isNullOrBlank()) "Reset to Auto Priority (خودکار)" else null,
            onRemovePhoto = if (!manualIdPhoto.isNullOrBlank()) {
                { onUpdateManualIdPhoto(null) }
            } else {
                null
            }
        )
    }
}

@Composable
private fun BrohiFrontLanyardCard(
    name: String,
    designation: String,
    department: String,
    employeeCode: String,
    cnic: String,
    joiningDate: String,
    expiryDate: String,
    photoData: String?,
    qrBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    onTapPhoto: () -> Unit,
    onTapCamera: () -> Unit,
    onTapQr: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, Color(0xFF9AC2F5)),
        shadowElevation = 8.dp,
        color = Color(0xFFF5FAFF),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 320.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFAFDFF),
                            Color(0xFFE7F3FF),
                            Color(0xFFCDE5FF)
                        )
                    )
                )
        ) {
            // Subtle bottom ice cubes texture + snowy mountain backdrop + dark blue corner wave
            Image(
                painter = painterResource(R.drawable.img_ice_cubes_banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(135.dp)
                    .alpha(0.24f)
            )
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(140.dp)
            ) {
                val w = size.width
                val h = size.height

                // Soft snowy mountain silhouette
                val mountainPath = Path().apply {
                    moveTo(0f, h)
                    lineTo(0f, h * 0.42f)
                    lineTo(w * 0.14f, h * 0.14f)
                    lineTo(w * 0.28f, h * 0.46f)
                    lineTo(w * 0.36f, h * 0.34f)
                    lineTo(w * 0.56f, h * 0.65f)
                    lineTo(w * 0.78f, h * 0.18f)
                    lineTo(w * 0.92f, h * 0.42f)
                    lineTo(w, h * 0.28f)
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = mountainPath,
                    color = Color(0xFF7CB6F5).copy(alpha = 0.22f)
                )

                // Deep royal blue sweeping wave in bottom-right corner
                val cyanSwoosh = Path().apply {
                    moveTo(w * 0.28f, h)
                    cubicTo(w * 0.56f, h * 0.88f, w * 0.80f, h * 0.58f, w, h * 0.32f)
                    lineTo(w, h)
                    close()
                }
                drawPath(path = cyanSwoosh, color = CardCyanRibbon.copy(alpha = 0.72f))

                val navySwoosh = Path().apply {
                    moveTo(w * 0.38f, h)
                    cubicTo(w * 0.64f, h * 0.90f, w * 0.84f, h * 0.64f, w, h * 0.42f)
                    lineTo(w, h)
                    close()
                }
                drawPath(
                    path = navySwoosh,
                    brush = Brush.linearGradient(
                        listOf(CardRoyalLight, CardNavyDark),
                        start = Offset(w * 0.4f, h),
                        end = Offset(w, h * 0.4f)
                    )
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                // Top Navy Wavy Header with Lanyard Cutout + Mountain Snowflake Crest
                BrohiCardWavyHeader()

                // Middle Identity Section: Portrait + 4 Blue Pill Rows
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Portrait Frame with Manual Camera Badge
                        Box(
                            modifier = Modifier.size(width = 88.dp, height = 108.dp)
                        ) {
                            Surface(
                                shape = RoundedCornerShape(11.dp),
                                color = Color.White,
                                border = BorderStroke(2.dp, CardNavyMid),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(11.dp))
                                    .clickable(onClick = onTapPhoto)
                                    .testTag("id_card_portrait_preview_button")
                            ) {
                                UserAvatarImage(
                                    photoUrl = photoData,
                                    contentDescription = "Employee ID Portrait",
                                    shape = RoundedCornerShape(9.dp),
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(2.dp)
                                )
                            }
                            Surface(
                                shape = CircleShape,
                                color = CardRoyalLight,
                                border = BorderStroke(1.5.dp, Color.White),
                                shadowElevation = 2.dp,
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                                    .padding(4.dp)
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .clickable(onClick = onTapCamera)
                                    .testTag("id_card_camera_badge_button")
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Filled.CameraAlt,
                                        contentDescription = "Capture ID Photo with Camera",
                                        tint = Color.White,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        // 4 Structured Pills (Name, Designation, Department, Employee ID)
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            CardDetailPillRow(label = "Name", value = name)
                            CardDetailPillRow(label = "Designation", value = designation)
                            CardDetailPillRow(label = "Department", value = department)
                            CardDetailPillRow(label = "Employee ID", value = employeeCode)
                        }
                    }

                    // Frosted CNIC + Valid From / Valid To 1-Year Validity Ribbon
                    Surface(
                        shape = RoundedCornerShape(9.dp),
                        color = Color.White,
                        border = BorderStroke(0.8.dp, CardNavyMid.copy(alpha = 0.22f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 9.dp, vertical = 5.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "CNIC Number",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = cnic,
                                    fontSize = 9.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CardNavyDark
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Valid From: $joiningDate",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF1E293B)
                                )
                                Text(
                                    text = "Valid To: $expiryDate",
                                    fontSize = 8.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CardNavyMid
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    // Real Scannable CNIC QR Code + Corner Scanner Brackets + "Scan for Details"
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onTapQr)
                            .padding(vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier.size(116.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            // Blue L-shaped corner scanner brackets matching reference image
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val arm = 20.dp.toPx()
                                val stroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                                val pad = 2.dp.toPx()
                                val w = size.width
                                val h = size.height
                                val p = Path().apply {
                                    // Top-left
                                    moveTo(pad, pad + arm)
                                    lineTo(pad, pad)
                                    lineTo(pad + arm, pad)
                                    // Top-right
                                    moveTo(w - pad - arm, pad)
                                    lineTo(w - pad, pad)
                                    lineTo(w - pad, pad + arm)
                                    // Bottom-right
                                    moveTo(w - pad, h - pad - arm)
                                    lineTo(w - pad, h - pad)
                                    lineTo(w - pad - arm, h - pad)
                                    // Bottom-left
                                    moveTo(pad + arm, h - pad)
                                    lineTo(pad, h - pad)
                                    lineTo(pad, h - pad - arm)
                                }
                                drawPath(path = p, color = CardRoyalLight, style = stroke)
                            }

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White,
                                modifier = Modifier.size(98.dp)
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(4.dp)
                                ) {
                                    if (qrBitmap != null) {
                                        Image(
                                            bitmap = qrBitmap,
                                            contentDescription = "CNIC QR Code",
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Filled.QrCodeScanner,
                                            contentDescription = "CNIC QR Code",
                                            tint = CardNavyDark,
                                            modifier = Modifier.size(64.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = CardNavyMid,
                            shadowElevation = 2.dp
                        ) {
                            Text(
                                text = "Scan for Details",
                                color = Color.White,
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun BrohiBackLanyardCard(
    address: String,
    phone: String,
    email: String,
    website: String
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.5.dp, Color(0xFF9AC2F5)),
        shadowElevation = 8.dp,
        color = Color(0xFFF5FAFF),
        modifier = Modifier
            .fillMaxWidth()
            .widthIn(max = 320.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            Color(0xFFFAFDFF),
                            Color(0xFFE7F3FF),
                            Color(0xFFD2E8FF)
                        )
                    )
                )
        ) {
            // Ice cubes visual texture on bottom right above dark wave
            Image(
                painter = painterResource(R.drawable.img_ice_cubes_banner),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .fillMaxWidth()
                    .height(175.dp)
                    .alpha(0.32f)
            )

            // Bottom sweeping deep navy & cyan wave footer
            Canvas(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(112.dp)
            ) {
                val w = size.width
                val h = size.height

                val outerCyan = Path().apply {
                    moveTo(0f, h * 0.24f)
                    cubicTo(w * 0.28f, -h * 0.10f, w * 0.68f, h * 0.34f, w, h * 0.08f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(path = outerCyan, color = CardCyanRibbon.copy(alpha = 0.85f))

                val navyBody = Path().apply {
                    moveTo(0f, h * 0.34f)
                    cubicTo(w * 0.30f, -h * 0.02f, w * 0.66f, h * 0.42f, w, h * 0.20f)
                    lineTo(w, h)
                    lineTo(0f, h)
                    close()
                }
                drawPath(
                    path = navyBody,
                    brush = Brush.linearGradient(
                        listOf(CardNavyDark, CardNavyMid, CardNavyDark),
                        start = Offset(0f, 0f),
                        end = Offset(w, h)
                    )
                )
            }

            Column(modifier = Modifier.fillMaxWidth()) {
                BrohiCardWavyHeader()

                // 4 Circular Navy Icon Contact Rows
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    BackContactRow(icon = Icons.Filled.LocationOn, text = address)
                    BackContactRow(icon = Icons.Filled.Phone, text = phone)
                    BackContactRow(icon = Icons.Filled.Email, text = email)
                    BackContactRow(icon = Icons.Filled.Public, text = website)
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Bottom Thank You Wave Text
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 18.dp, start = 16.dp, end = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Thank You",
                        color = Color.White,
                        fontSize = 23.sp,
                        fontWeight = FontWeight.Bold,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif
                    )
                    Text(
                        text = "for being a part of our team",
                        color = Color(0xFFD8EAFF),
                        fontSize = 11.sp,
                        fontStyle = FontStyle.Italic
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .width(108.dp)
                            .height(1.5.dp)
                            .background(Color.White.copy(alpha = 0.72f))
                    )
                }
            }
        }
    }
}

@Composable
private fun BrohiCardWavyHeader() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(106.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Sky-blue lower swoosh ribbon
            val cyanWave = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.80f)
                cubicTo(w * 0.68f, h * 1.06f, w * 0.30f, h * 0.80f, 0f, h * 0.90f)
                close()
            }
            drawPath(path = cyanWave, color = CardCyanRibbon)

            // Main Deep Royal Blue header wave
            val navyWave = Path().apply {
                moveTo(0f, 0f)
                lineTo(w, 0f)
                lineTo(w, h * 0.68f)
                cubicTo(w * 0.70f, h * 0.98f, w * 0.32f, h * 0.70f, 0f, h * 0.84f)
                close()
            }
            drawPath(
                path = navyWave,
                brush = Brush.linearGradient(
                    listOf(CardNavyDark, CardRoyalLight, CardNavyMid),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Lanyard Slot Cutout Pill at top center
            Box(
                modifier = Modifier
                    .size(width = 52.dp, height = 11.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color.White)
                    .border(1.2.dp, Color(0xFF93B8E8), RoundedCornerShape(50))
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Mountain + Snowflake Crest | BROHI ICE FACTORY Lockup
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                MountainSnowflakeEmblem(modifier = Modifier.size(48.dp))

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .width(1.5.dp)
                        .height(42.dp)
                        .background(Color.White.copy(alpha = 0.85f))
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = "BROHI",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 21.sp,
                        letterSpacing = 2.sp,
                        lineHeight = 22.sp
                    )
                    Text(
                        text = "ICE FACTORY",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        letterSpacing = 1.6.sp,
                        lineHeight = 13.sp
                    )
                    Text(
                        text = "Pure Ice... Pure Life...",
                        color = Color(0xFFD6EAFF),
                        fontSize = 9.5.sp,
                        fontStyle = FontStyle.Italic,
                        fontFamily = FontFamily.Serif
                    )
                }
            }
        }
    }
}

@Composable
private fun MountainSnowflakeEmblem(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height

        // Twin Mountain Peaks outline in white
        val mPath = Path().apply {
            moveTo(w * 0.04f, h * 0.62f)
            lineTo(w * 0.34f, h * 0.16f)
            lineTo(w * 0.56f, h * 0.44f)
            lineTo(w * 0.72f, h * 0.24f)
            lineTo(w * 0.96f, h * 0.62f)
        }
        drawPath(
            path = mPath,
            color = Color.White,
            style = Stroke(width = 2.3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        // Secondary ridge line
        val ridge = Path().apply {
            moveTo(w * 0.24f, h * 0.42f)
            lineTo(w * 0.34f, h * 0.30f)
            lineTo(w * 0.46f, h * 0.42f)
        }
        drawPath(
            path = ridge,
            color = Color.White,
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Geometric 6-Arm Snowflake centered cleanly below peaks
        val cx = w * 0.50f
        val cy = h * 0.68f
        val r = w * 0.24f
        for (i in 0 until 6) {
            val angle = Math.toRadians((i * 60).toDouble())
            val dx = (r * cos(angle)).toFloat()
            val dy = (r * sin(angle)).toFloat()
            drawLine(
                color = Color.White,
                start = Offset(cx, cy),
                end = Offset(cx + dx, cy + dy),
                strokeWidth = 1.8.dp.toPx(),
                cap = StrokeCap.Round
            )
            // Small crystal V-branches near tips
            val midX = cx + dx * 0.62f
            val midY = cy + dy * 0.62f
            val branchLen = r * 0.28f
            val leftA = angle + Math.toRadians(45.0)
            val rightA = angle - Math.toRadians(45.0)
            drawLine(
                color = Color.White,
                start = Offset(midX, midY),
                end = Offset(midX + (branchLen * cos(leftA)).toFloat(), midY + (branchLen * sin(leftA)).toFloat()),
                strokeWidth = 1.3.dp.toPx(),
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color.White,
                start = Offset(midX, midY),
                end = Offset(midX + (branchLen * cos(rightA)).toFloat(), midY + (branchLen * sin(rightA)).toFloat()),
                strokeWidth = 1.3.dp.toPx(),
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun CardDetailPillRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(7.dp))
            .background(Color.White.copy(alpha = 0.88f))
            .border(0.7.dp, CardNavyMid.copy(alpha = 0.16f), RoundedCornerShape(7.dp)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(66.dp)
                .background(
                    Brush.horizontalGradient(listOf(CardNavyMid, CardRoyalLight))
                )
                .padding(horizontal = 6.dp, vertical = 5.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = label,
                color = Color.White,
                fontSize = 8.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
        Text(
            text = value,
            color = CardNavyDark,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 7.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun BackContactRow(icon: ImageVector, text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = CardNavyDark,
            shadowElevation = 2.dp,
            modifier = Modifier.size(30.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        Text(
            text = text,
            color = CardNavyDark,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PhotoPriorityTierRow(
    priorityNumber: Int,
    title: String,
    subtitle: String,
    isActive: Boolean,
    isAvailable: Boolean,
    icon: ImageVector
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isActive) SuccessGreen.copy(alpha = 0.14f) else Color.White.copy(alpha = 0.65f)
            )
            .border(
                width = if (isActive) 1.dp else 0.6.dp,
                color = if (isActive) SuccessGreen else Color(0xFFCBD5E1),
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 9.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            shape = CircleShape,
            color = when {
                isActive -> SuccessGreen
                isAvailable -> RoyalBlue
                else -> Color(0xFFCBD5E1)
            },
            modifier = Modifier.size(22.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = "$priorityNumber",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
            }
        }
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isActive) SuccessGreen else DeepBlue,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                fontSize = 9.5.sp,
                color = Color(0xFF475569),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (isActive) {
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = "Active Source",
                tint = SuccessGreen,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
