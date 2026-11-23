package com.example.ui.audit

import android.graphics.Bitmap
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AiAuditFinding
import com.example.data.model.AuditIssueStatus
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import kotlin.math.roundToInt

data class AiAuditUiState(
    val isPanelOpen: Boolean = false,
    val isAuditing: Boolean = false,
    val isAutoWatchActive: Boolean = false,
    val isCapturingSnapshot: Boolean = false,
    val activeScreenKey: String = "dashboard",
    val activeScreenTitle: String = "Brohi Ice Factory",
    val latestSummaryEn: String? = null,
    val latestSummaryUr: String? = null,
    val lastErrorMessage: String? = null,
    val lastAuditedAtMillis: Long = 0L,
    val findings: List<AiAuditFinding> = emptyList()
) {
    val pendingCount: Int
        get() = findings.count { it.status == AuditIssueStatus.PENDING }
}

data class AiAuditOverlayController(
    val isEnabledForSuperAdmin: Boolean,
    val pendingCount: Int,
    val isScanning: Boolean,
    val onAuditDialogSnapshot: (dialogTitle: String, bitmap: Bitmap?) -> Unit
)

val LocalAiAuditController = compositionLocalOf<AiAuditOverlayController?> { null }

/**
 * Side-anchored, vertically & horizontally draggable Crystal Floating AI Audit Pill.
 * Visible ONLY for Super Admin when AI Audit is turned ON.
 */
@Composable
fun FloatingAiAuditButton(
    pendingCount: Int,
    isScanning: Boolean,
    autoWatchEnabled: Boolean,
    hiddenForCapture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (hiddenForCapture) return

    val pulseTransition = rememberInfiniteTransition(label = "ai_audit_pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.72f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val density = LocalDensity.current
        val buttonWidthPx = with(density) { 58.dp.toPx() }
        val buttonHeightPx = with(density) { 64.dp.toPx() }
        val marginPx = with(density) { 10.dp.toPx() }
        val maxX = (constraints.maxWidth.toFloat() - buttonWidthPx - marginPx).coerceAtLeast(marginPx)
        val maxY = (constraints.maxHeight.toFloat() - buttonHeightPx - with(density) { 96.dp.toPx() }).coerceAtLeast(marginPx)

        var offsetX by remember(maxX) { mutableFloatStateOf(maxX) }
        var offsetY by remember(maxY) { mutableFloatStateOf((maxY * 0.62f).coerceIn(marginPx, maxY)) }

        Box(
            modifier = Modifier
                .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
                .pointerInput(maxX, maxY) {
                    detectDragGestures(
                        onDragEnd = {
                            // Snap smoothly to left or right side edge so main buttons stay clear
                            offsetX = if (offsetX < maxX / 2f) marginPx else maxX
                        }
                    ) { change, dragAmount ->
                        change.consume()
                        offsetX = (offsetX + dragAmount.x).coerceIn(marginPx, maxX)
                        offsetY = (offsetY + dragAmount.y).coerceIn(marginPx, maxY)
                    }
                }
        ) {
            Box(contentAlignment = Alignment.TopEnd) {
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.3.dp, Color.White.copy(alpha = 0.75f)),
                    color = Color.Transparent,
                    modifier = Modifier
                        .size(54.dp)
                        .shadow(10.dp, RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(DeepBlue, RoyalBlue, Color(0xFF0EA5E9))
                            )
                        )
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Open AI Audit Panel",
                            onClick = onClick
                        )
                        .alpha(if (isScanning) pulseAlpha else 1f)
                        .semantics { contentDescription = "AI Live Audit Button" }
                        .testTag("floating_ai_audit_button")
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 2.2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.AutoFixHigh,
                                contentDescription = "AI Screen Audit",
                                tint = Color.White,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (autoWatchEnabled) Color(0xFF4ADE80) else Color(0xFFFACC15))
                            )
                            Text(
                                text = "AI",
                                color = Color.White,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }

                if (pendingCount > 0) {
                    Box(
                        modifier = Modifier
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(DangerRed)
                            .padding(1.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (pendingCount > 99) "99+" else pendingCount.toString(),
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                } else if (autoWatchEnabled && !isScanning) {
                    Box(
                        modifier = Modifier
                            .offset(x = 2.dp, y = (-2).dp)
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(SuccessGreen)
                    )
                }
            }
        }
    }
}
