package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.data.util.ImageAndQrUtils
import com.example.data.util.ScreenSnapshotHelper
import com.example.ui.audit.LocalAiAuditController
import com.example.ui.theme.CrystalBorderLight
import com.example.ui.theme.CrystalIceBgMid
import com.example.ui.theme.CrystalIceBgTop
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBorderDark
import java.io.File

/**
 * Reusable Crystal-Glass modal dialog with vibrant ice-blue gradient header,
 * frosted icon badge, scrollable crystal form body, and uniform action footer.
 */
@Composable
fun CrystalFormDialog(
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String = "",
    icon: ImageVector,
    confirmText: String,
    onConfirm: () -> Unit,
    confirmEnabled: Boolean = true,
    dismissText: String = "Cancel",
    showDismissButton: Boolean = true,
    accentColor: Color = RoyalBlue,
    isDestructive: Boolean = false,
    confirmTestTag: String? = null,
    dismissTestTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val containerBg = if (darkTheme) Color(0xFF0E2042) else Color(0xFFF3F8FF)
    val footerBg = if (darkTheme) Color(0xFF12274F) else Color.White
    val borderColor = when {
        isDestructive -> DangerRed.copy(alpha = 0.45f)
        darkTheme -> SoftBorderDark
        else -> CrystalBorderLight
    }
    val headerBrush = when {
        isDestructive -> Brush.horizontalGradient(listOf(Color(0xFF991B1B), DangerRed))
        accentColor != RoyalBlue -> Brush.horizontalGradient(listOf(lerp(accentColor, DeepBlue, 0.45f), accentColor))
        else -> PrimaryBlueGradient
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = containerBg,
            border = BorderStroke(1.2.dp, borderColor),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 460.dp)
                .heightIn(max = 680.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Crystal Header Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(headerBrush)
                        .padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 14.dp)
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
                                .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(11.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    color = Color(0xFFDCE8FF),
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        val auditController = LocalAiAuditController.current
                        val dialogView = LocalView.current
                        if (auditController != null && auditController.isEnabledForSuperAdmin) {
                            IconButton(
                                onClick = {
                                    val bmp = ScreenSnapshotHelper.captureViewBitmap(dialogView)
                                    auditController.onAuditDialogSnapshot(title, bmp)
                                },
                                modifier = Modifier.testTag("dialog_ai_audit_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoFixHigh,
                                    contentDescription = "Audit Dialog UI",
                                    tint = Color(0xFFFDE68A),
                                    modifier = Modifier.size(19.dp)
                                )
                            }
                        }
                        IconButton(onClick = onDismissRequest) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Close dialog",
                                tint = Color.White.copy(alpha = 0.9f),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Crystal Body
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .background(
                            if (darkTheme) {
                                Brush.verticalGradient(listOf(Color(0xFF0E2042), Color(0xFF10244A)))
                            } else {
                                Brush.verticalGradient(listOf(CrystalIceBgTop, CrystalIceBgMid, Color(0xFFEAF3FF)))
                            }
                        )
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content
                )

                HorizontalDivider(
                    color = if (darkTheme) SoftBorderDark else CrystalBorderLight.copy(alpha = 0.8f)
                )

                // Footer Action Bar
                Surface(color = footerBg, modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (showDismissButton) {
                            OutlinedButton(
                                onClick = onDismissRequest,
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, if (darkTheme) SoftBorderDark else CrystalBorderLight),
                                colors = ButtonDefaults.outlinedButtonColors(
                                    contentColor = if (darkTheme) Color.White else DeepBlue
                                ),
                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                                modifier = Modifier
                                    .weight(0.42f)
                                    .heightIn(min = 46.dp)
                                    .let { if (dismissTestTag != null) it.testTag(dismissTestTag) else it }
                            ) {
                                Text(
                                    text = dismissText,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val confirmModifier = Modifier
                            .weight(if (showDismissButton) 0.58f else 1f)
                            .heightIn(min = 46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .let {
                                if (!isDestructive && accentColor == RoyalBlue && confirmEnabled) {
                                    it.background(PrimaryBlueGradient)
                                } else {
                                    it
                                }
                            }
                            .let { if (confirmTestTag != null) it.testTag(confirmTestTag) else it }

                        Button(
                            onClick = onConfirm,
                            enabled = confirmEnabled,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = when {
                                    isDestructive -> DangerRed
                                    accentColor != RoyalBlue -> accentColor
                                    !confirmEnabled -> RoyalBlue.copy(alpha = 0.45f)
                                    else -> Color.Transparent
                                }
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = confirmModifier
                        ) {
                            Icon(
                                imageVector = if (isDestructive) Icons.Filled.DeleteOutline else Icons.Filled.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = confirmText,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * International-standard system camera photo capture launcher.
 * Handles CAMERA runtime permission, creates a FileProvider URI in cacheDir,
 * preserves temp path across Activity recreation, and returns a compressed JPEG data URI.
 */
@Composable
fun rememberCameraPhotoLauncher(
    maxDimension: Int = 360,
    quality: Int = 74,
    onPhotoCaptured: (String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    var pendingPhotoPath by rememberSaveable { mutableStateOf<String?>(null) }

    val takePictureLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val path = pendingPhotoPath
        pendingPhotoPath = null
        if (path != null) {
            val file = File(path)
            if (success && file.exists()) {
                val uri = Uri.fromFile(file)
                val encoded = ImageAndQrUtils.encodeUriToJpegDataUri(
                    context = context,
                    uri = uri,
                    maxDimension = maxDimension,
                    quality = quality
                )
                runCatching { file.delete() }
                if (!encoded.isNullOrBlank()) {
                    onPhotoCaptured(encoded)
                } else {
                    Toast.makeText(context, "Could not process captured photo", Toast.LENGTH_SHORT).show()
                }
            } else {
                runCatching { file.delete() }
            }
        }
    }

    fun launchSystemCamera() {
        runCatching {
            val dir = File(context.cacheDir, "camera_captures").apply { mkdirs() }
            val tempFile = File(dir, "photo_${System.currentTimeMillis()}.jpg")
            pendingPhotoPath = tempFile.absolutePath
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )
            takePictureLauncher.launch(contentUri)
        }.onFailure {
            pendingPhotoPath = null
            Toast.makeText(
                context,
                "Unable to open camera on this device (کیمرہ دستیاب نہیں ہے)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            launchSystemCamera()
        } else {
            Toast.makeText(
                context,
                "Camera permission is required to take a photo (کیمرہ کی اجازت ضروری ہے)",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    return {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
        if (granted) {
            launchSystemCamera()
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }
}

/**
 * International-standard full-view photo preview dialog with direct Camera capture,
 * Gallery upload (Android Photo Picker), and optional Remove/Reset actions.
 */
@Composable
fun CrystalFullPhotoViewerDialog(
    title: String,
    subtitle: String = "",
    photoUrl: String?,
    onDismiss: () -> Unit,
    onTakeCameraPhoto: () -> Unit,
    onPickFromGallery: () -> Unit,
    removeLabel: String? = null,
    onRemovePhoto: (() -> Unit)? = null
) {
    val darkTheme = isSystemInDarkTheme()
    val containerBg = if (darkTheme) Color(0xFF0E2042) else Color(0xFFF3F8FF)

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = containerBg,
            border = BorderStroke(1.2.dp, if (darkTheme) SoftBorderDark else CrystalBorderLight),
            shadowElevation = 14.dp,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 420.dp)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(PrimaryBlueGradient)
                        .padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 15.5.sp
                            )
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    color = Color(0xFFDCE8FF),
                                    fontSize = 11.sp
                                )
                            }
                        }
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Filled.Close, contentDescription = "Close preview", tint = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White,
                        border = BorderStroke(2.dp, CrystalBorderLight),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth(0.84f)
                            .aspectRatio(1f)
                    ) {
                        UserAvatarImage(
                            photoUrl = photoUrl,
                            contentDescription = title,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onDismiss()
                                onTakeCameraPhoto()
                            },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(PrimaryBlueGradient)
                                .testTag("photo_viewer_camera_button")
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Camera", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, textAlign = TextAlign.Center)
                        }

                        OutlinedButton(
                            onClick = {
                                onDismiss()
                                onPickFromGallery()
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.2.dp, RoyalBlue.copy(alpha = 0.45f)),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = if (darkTheme) Color(0xFF14284E) else Color.White,
                                contentColor = if (darkTheme) Color.White else DeepBlue
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 46.dp)
                                .testTag("photo_viewer_gallery_button")
                        ) {
                            Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(17.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Upload Photo", fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 2, textAlign = TextAlign.Center)
                        }
                    }

                    if (!removeLabel.isNullOrBlank() && onRemovePhoto != null) {
                        TextButton(
                            onClick = {
                                onRemovePhoto()
                                onDismiss()
                            }
                        ) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = null, tint = DangerRed, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(removeLabel, color = DangerRed, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}
