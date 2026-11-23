package com.example.ui.components

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.util.ImageAndQrUtils
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AllInclusive
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Bolt
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalMall
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.ManageAccounts
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Payment
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.UserRole
import com.example.data.util.AppStrings
import com.example.data.util.PakistaniFormatters
import com.example.ui.navigation.AppRoute
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CrystalBorderLight
import com.example.ui.theme.CrystalIceBgBottom
import com.example.ui.theme.CrystalIceBgMid
import com.example.ui.theme.CrystalIceBgTop
import com.example.ui.theme.CrystalSurfaceDark
import com.example.ui.theme.CrystalSurfaceLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.DrawerActiveBg
import com.example.ui.theme.DrawerDarkBlue
import com.example.ui.theme.IceBackgroundDark
import com.example.ui.theme.IcyGlossBottom
import com.example.ui.theme.IcyGlossMid
import com.example.ui.theme.IcyGlossRim
import com.example.ui.theme.IcyGlossTop
import com.example.ui.theme.IcyGlowShadow
import com.example.ui.theme.IcyGoldRim
import com.example.ui.theme.IcyPillBg
import com.example.ui.theme.IcyPillBorder
import com.example.ui.theme.IcyWaveBright
import com.example.ui.theme.IcyWaveLight
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBorderDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.DaySalesBar

val PrimaryBlueGradient = Brush.horizontalGradient(
    colors = listOf(Color(0xFF0A3288), Color(0xFF0E6CF5))
)

enum class IcyGlossyTone { BLUE, GREEN, RED, ORANGE }

@Composable
fun IcyGlossyButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: IcyGlossyTone = IcyGlossyTone.BLUE,
    shape: Shape = CircleShape,
    contentPadding: PaddingValues = PaddingValues(horizontal = 18.dp, vertical = 10.dp),
    content: @Composable RowScope.() -> Unit
) {
    val (topCol, midCol, bottomCol, rimCol, glowCol) = when (tone) {
        IcyGlossyTone.BLUE -> listOf(
            IcyGlossTop,
            IcyGlossMid,
            IcyGlossBottom,
            IcyGlossRim,
            IcyGlowShadow
        )
        IcyGlossyTone.GREEN -> listOf(
            Color(0xFF47E685),
            Color(0xFF16A34A),
            Color(0xFF0A662D),
            Color(0xFF86EFAC),
            Color(0xFF16A34A)
        )
        IcyGlossyTone.RED -> listOf(
            Color(0xFFFF7A7A),
            Color(0xFFDC2626),
            Color(0xFF931414),
            Color(0xFFFCA5A5),
            Color(0xFFDC2626)
        )
        IcyGlossyTone.ORANGE -> listOf(
            Color(0xFFFFBC5E),
            Color(0xFFF97316),
            Color(0xFFC2410C),
            Color(0xFFFDBA74),
            Color(0xFFF97316)
        )
    }

    val fillBrush = if (enabled) {
        Brush.verticalGradient(listOf(topCol, midCol, bottomCol))
    } else {
        Brush.verticalGradient(
            listOf(
                Color(0xFFB5C7E3),
                Color(0xFF8FA7CC),
                Color(0xFF728CB6)
            )
        )
    }
    val borderBrush = Brush.verticalGradient(
        listOf(
            Color.White.copy(alpha = if (enabled) 0.90f else 0.45f),
            rimCol.copy(alpha = if (enabled) 0.85f else 0.35f)
        )
    )

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .shadow(
                elevation = if (enabled) 8.dp else 1.dp,
                shape = shape,
                ambientColor = glowCol.copy(alpha = 0.55f),
                spotColor = glowCol.copy(alpha = 0.65f)
            )
            .clip(shape)
            .background(fillBrush)
            .border(width = 1.2.dp, brush = borderBrush, shape = shape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        // Upper specular gloss highlight
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(horizontal = 4.dp, vertical = 2.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = if (enabled) 0.38f else 0.14f),
                        0.48f to Color.White.copy(alpha = if (enabled) 0.08f else 0.02f),
                        0.52f to Color.Transparent,
                        1.0f to Color.Transparent
                    )
                )
        )
        Row(
            modifier = Modifier.padding(contentPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            content = content
        )
    }
}

@Composable
fun IcyGlossyIconTile(
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    shape: Shape = RoundedCornerShape(18.dp),
    tone: IcyGlossyTone = IcyGlossyTone.BLUE,
    outerRingBrush: Brush? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val (topCol, midCol, bottomCol, rimCol, glowCol) = when (tone) {
        IcyGlossyTone.BLUE -> listOf(
            IcyGlossTop,
            IcyGlossMid,
            IcyGlossBottom,
            IcyGlossRim,
            IcyGlowShadow
        )
        IcyGlossyTone.GREEN -> listOf(
            Color(0xFF47E685),
            Color(0xFF16A34A),
            Color(0xFF0A662D),
            Color(0xFF86EFAC),
            Color(0xFF16A34A)
        )
        IcyGlossyTone.RED -> listOf(
            Color(0xFFFF7A7A),
            Color(0xFFDC2626),
            Color(0xFF931414),
            Color(0xFFFCA5A5),
            Color(0xFFDC2626)
        )
        IcyGlossyTone.ORANGE -> listOf(
            Color(0xFFFFBC5E),
            Color(0xFFF97316),
            Color(0xFFC2410C),
            Color(0xFFFDBA74),
            Color(0xFFF97316)
        )
    }
    val ring = outerRingBrush ?: Brush.verticalGradient(
        listOf(Color.White.copy(alpha = 0.92f), rimCol)
    )
    Box(
        modifier = modifier
            .size(size)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                ambientColor = glowCol.copy(alpha = 0.55f),
                spotColor = glowCol.copy(alpha = 0.65f)
            )
            .clip(shape)
            .background(Brush.verticalGradient(listOf(topCol, midCol, bottomCol)))
            .border(width = 1.6.dp, brush = ring, shape = shape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .padding(2.dp)
                .clip(shape)
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.White.copy(alpha = 0.36f),
                        0.45f to Color.White.copy(alpha = 0.06f),
                        0.55f to Color.Transparent
                    )
                )
        )
        content()
    }
}

@Composable
fun IcyPillChip(
    text: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
) {
    val darkTheme = isSystemInDarkTheme()
    val bg = if (darkTheme) Color(0xFF19325E) else IcyPillBg
    val border = if (darkTheme) SoftBorderDark else IcyPillBorder
    val txtColor = if (darkTheme) Color(0xFFE8F1FF) else DeepBlue
    Surface(
        shape = CircleShape,
        color = bg,
        border = BorderStroke(1.dp, border),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 11.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (darkTheme) IcyGlossTop else RoyalBlue,
                    modifier = Modifier.size(13.dp)
                )
            }
            Text(
                text = text,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = txtColor,
                maxLines = 1,
                softWrap = false
            )
        }
    }
}

@Composable
fun IcyCardWaveCorner(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme()
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val wave1 = Path().apply {
            moveTo(w * 0.48f, h)
            cubicTo(w * 0.72f, h * 0.88f, w * 0.86f, h * 0.56f, w, h * 0.28f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = wave1,
            brush = Brush.linearGradient(
                colors = if (darkTheme) {
                    listOf(Color(0x2238BDF8), Color(0x440E6CF5))
                } else {
                    listOf(Color(0x55B8DCFF), Color(0xAA4BB4FF))
                },
                start = Offset(w * 0.5f, h),
                end = Offset(w, h * 0.25f)
            )
        )
        val wave2 = Path().apply {
            moveTo(w * 0.68f, h)
            cubicTo(w * 0.84f, h * 0.90f, w * 0.93f, h * 0.72f, w, h * 0.54f)
            lineTo(w, h)
            close()
        }
        drawPath(
            path = wave2,
            brush = Brush.linearGradient(
                colors = if (darkTheme) {
                    listOf(Color(0x330F6DF7), Color(0x660648B8))
                } else {
                    listOf(Color(0x883CA7FF), Color(0xCC0D68F0))
                },
                start = Offset(w * 0.65f, h),
                end = Offset(w, h * 0.52f)
            )
        )
    }
}

@Composable
fun IcySplashProgressBar(
    progressPercent: Int,
    modifier: Modifier = Modifier
) {
    val clamped = progressPercent.coerceIn(0, 100)
    val animatedFraction by animateFloatAsState(
        targetValue = clamped / 100f,
        animationSpec = tween(durationMillis = 360),
        label = "icy_progress_fraction"
    )
    val darkTheme = isSystemInDarkTheme()
    val trackTop = if (darkTheme) Color(0xFF172F5B) else Color(0xFFCDE3FD)
    val trackMid = if (darkTheme) Color(0xFF214079) else Color(0xFFF5FAFF)
    val trackBottom = if (darkTheme) Color(0xFF102347) else Color(0xFFD5E9FF)
    val trackBorder = if (darkTheme) Color(0xFF4772BA) else Color(0xFF8ABDF8)

    Box(
        modifier = modifier.height(42.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // Frosted Empty Capsule Track (24.dp centered vertically so water splashes & cubes can overflow above)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .shadow(
                    elevation = 4.dp,
                    shape = CircleShape,
                    ambientColor = IcyGlowShadow.copy(alpha = 0.25f),
                    spotColor = IcyGlowShadow.copy(alpha = 0.25f)
                )
                .clip(CircleShape)
                .background(Brush.verticalGradient(listOf(trackTop, trackMid, trackBottom)))
                .border(width = 1.3.dp, color = trackBorder, shape = CircleShape)
        ) {
            // Subtle bottom inner rim reflection
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Transparent, Color.White.copy(alpha = if (darkTheme) 0.14f else 0.55f))
                        )
                    )
            )
        }

        // Vector Crystal Water Fill, Faceted 3D Ice Cubes & Splashing Water Droplets
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val barH = 24.dp.toPx().coerceAtMost(h * 0.72f)
            val barTop = (h - barH) / 2f
            val barBottom = barTop + barH
            val radius = barH / 2f

            val fillFrac = if (clamped <= 0) 0f else animatedFraction.coerceIn(0.10f, 1f)
            val fillWidth = (w * fillFrac).coerceAtLeast(if (clamped > 0) barH else 0f)
            val splashReachX = if (clamped <= 0) {
                0f
            } else {
                (fillWidth + w * 0.42f).coerceAtMost(w * 0.96f)
            }

            if (clamped > 0) {
                // 1. Clipped Vibrant Cyan-Blue Flowing Water Inside Capsule
                val capsuleClip = Path().apply {
                    addRoundRect(
                        androidx.compose.ui.geometry.RoundRect(
                            left = 1.dp.toPx(),
                            top = barTop + 1.dp.toPx(),
                            right = w - 1.dp.toPx(),
                            bottom = barBottom - 1.dp.toPx(),
                            cornerRadius = CornerRadius(radius, radius)
                        )
                    )
                }
                clipRect(left = 0f, top = barTop, right = fillWidth, bottom = barBottom) {
                    drawPath(
                        path = capsuleClip,
                        brush = Brush.horizontalGradient(
                            0.0f to Color(0xFF0A52CC),
                            0.45f to Color(0xFF1497FF),
                            0.85f to Color(0xFF42D1FF),
                            1.0f to Color(0xFF9DF2FF),
                            endX = fillWidth.coerceAtLeast(1f)
                        )
                    )
                    // Bright horizontal water sheen inside capsule
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.65f),
                                Color.White.copy(alpha = 0.10f),
                                Color.Transparent
                            ),
                            startY = barTop,
                            endY = barTop + barH * 0.55f
                        ),
                        topLeft = Offset(2.dp.toPx(), barTop + 2.dp.toPx()),
                        size = Size((fillWidth - 4.dp.toPx()).coerceAtLeast(0f), barH * 0.48f),
                        cornerRadius = CornerRadius(radius, radius)
                    )
                }

                // 2. Splashing Translucent Water Trail Extending Along the Crest
                val crestWave = Path().apply {
                    moveTo(radius * 0.5f, barBottom - 3.dp.toPx())
                    cubicTo(
                        splashReachX * 0.30f, barTop + 4.dp.toPx(),
                        splashReachX * 0.65f, barTop - 4.dp.toPx(),
                        splashReachX, barBottom - 4.dp.toPx()
                    )
                    cubicTo(
                        splashReachX * 0.70f, barBottom + 1.dp.toPx(),
                        splashReachX * 0.32f, barBottom - 1.dp.toPx(),
                        radius * 0.5f, barBottom - 3.dp.toPx()
                    )
                    close()
                }
                drawPath(
                    path = crestWave,
                    brush = Brush.horizontalGradient(
                        0.0f to Color(0xCC2EB8FF),
                        0.55f to Color(0x995CE1FF),
                        1.0f to Color(0x00A6F4FF),
                        endX = splashReachX.coerceAtLeast(1f)
                    )
                )

                // Crisp arcing splash crown above bar edge
                val splashArc = Path().apply {
                    moveTo(splashReachX * 0.46f, barTop + 5.dp.toPx())
                    cubicTo(
                        splashReachX * 0.64f, barTop - 7.dp.toPx(),
                        splashReachX * 0.80f, barTop - 6.dp.toPx(),
                        splashReachX * 0.90f, barTop + 6.dp.toPx()
                    )
                }
                drawPath(
                    path = splashArc,
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xBB38BDF8), Color(0xFFE0F7FF), Color.Transparent),
                        startX = splashReachX * 0.44f,
                        endX = splashReachX * 0.94f
                    ),
                    style = Stroke(width = 2.2.dp.toPx(), cap = StrokeCap.Round)
                )

                // 3. Faceted 3D Crystal Ice Cubes Riding the Water Wave
                val cubeSpecs = listOf(
                    Triple(0.18f, -14f, 18.dp.toPx()),
                    Triple(0.38f, 12f, 20.dp.toPx()),
                    Triple(0.58f, -16f, 19.dp.toPx()),
                    Triple(0.79f, 14f, 17.dp.toPx())
                )
                cubeSpecs.forEach { (frac, deg, cubeSize) ->
                    val cx = splashReachX * frac
                    if (cx > cubeSize * 0.45f && cx < w - cubeSize * 0.35f) {
                        val cy = h * 0.46f
                        rotate(degrees = deg, pivot = Offset(cx, cy)) {
                            val half = cubeSize / 2f
                            val topLeft = Offset(cx - half, cy - half)
                            val cr = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                            // Outer crystal cube body
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xEEFFFFFF),
                                        Color(0xCC9BE4FF),
                                        Color(0xDD1C9BFF)
                                    ),
                                    start = Offset(cx - half, cy - half),
                                    end = Offset(cx + half, cy + half)
                                ),
                                topLeft = topLeft,
                                size = Size(cubeSize, cubeSize),
                                cornerRadius = cr
                            )
                            // Inner bevel facet highlight
                            drawRoundRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        Color.White.copy(alpha = 0.88f),
                                        Color(0x557CE2FF),
                                        Color(0xAA0C6EF0)
                                    ),
                                    start = Offset(cx - half * 0.6f, cy - half * 0.6f),
                                    end = Offset(cx + half * 0.6f, cy + half * 0.6f)
                                ),
                                topLeft = Offset(cx - half * 0.65f, cy - half * 0.65f),
                                size = Size(cubeSize * 0.65f, cubeSize * 0.65f),
                                cornerRadius = CornerRadius(2.5.dp.toPx(), 2.5.dp.toPx())
                            )
                            // Bright icy rim border
                            drawRoundRect(
                                color = Color.White.copy(alpha = 0.92f),
                                topLeft = topLeft,
                                size = Size(cubeSize, cubeSize),
                                cornerRadius = cr,
                                style = Stroke(width = 1.2.dp.toPx())
                            )
                        }
                    }
                }

                // 4. Crisp Flying Water Droplets Splashing Above & Around Wave
                val droplets = listOf(
                    Triple(0.14f, 0.18f, 1.7.dp.toPx()),
                    Triple(0.27f, 0.11f, 2.2.dp.toPx()),
                    Triple(0.45f, 0.13f, 1.8.dp.toPx()),
                    Triple(0.67f, 0.12f, 2.4.dp.toPx()),
                    Triple(0.75f, 0.08f, 1.6.dp.toPx()),
                    Triple(0.86f, 0.19f, 2.1.dp.toPx()),
                    Triple(0.92f, 0.28f, 1.5.dp.toPx())
                )
                droplets.forEach { (dxFrac, dyFrac, r) ->
                    val dx = splashReachX * dxFrac
                    if (dx in 4f..(w - 4f)) {
                        val dy = h * dyFrac
                        drawCircle(
                            brush = Brush.radialGradient(
                                listOf(Color.White, Color(0xFF42C7FF)),
                                center = Offset(dx - r * 0.25f, dy - r * 0.25f),
                                radius = r * 1.3f
                            ),
                            radius = r,
                            center = Offset(dx, dy)
                        )
                    }
                }
            }
        }
    }
}

data class DrawerMenuItem(
    val route: AppRoute,
    val label: String,
    val labelUrdu: String,
    val icon: ImageVector,
    val staffLabel: String? = null,
    val staffLabelUrdu: String? = null
)

fun buildDrawerMenuItems(): List<DrawerMenuItem> = listOf(
    DrawerMenuItem(AppRoute.Dashboard, "Dashboard", "ڈیش بورڈ", Icons.Outlined.Home),
    DrawerMenuItem(AppRoute.UsersRoles, "Users & Approvals", "اسٹاف اور منظوری", Icons.Outlined.Group),
    DrawerMenuItem(AppRoute.Attendance, "Attendance", "حاضری", Icons.Outlined.EventAvailable, staffLabel = "My Attendance", staffLabelUrdu = "میری حاضری"),
    DrawerMenuItem(AppRoute.FaceVerification, "Face Enrollment", "چہرہ تصدیق", Icons.Outlined.FaceRetouchingNatural),
    DrawerMenuItem(AppRoute.Notices, "Notices & Holidays", "اعلانات و چھٹیاں", Icons.Outlined.Campaign),
    DrawerMenuItem(AppRoute.Production, "Production", "پیداوار", Icons.Outlined.PrecisionManufacturing),
    DrawerMenuItem(AppRoute.Inventory, "Inventory & Cold Storage", "کولڈ اسٹوریج", Icons.Outlined.Inventory2),
    DrawerMenuItem(AppRoute.Sales, "Sales & Invoicing", "فروخت و بلنگ", Icons.Outlined.ShoppingCart),
    DrawerMenuItem(AppRoute.Customers, "Customers & Credit", "گاہک اور ادھار", Icons.Outlined.Storefront),
    DrawerMenuItem(AppRoute.Deliveries, "Deliveries & Vehicles", "ڈلیوری اور گاڑیاں", Icons.Outlined.LocalShipping),
    DrawerMenuItem(AppRoute.Expenses, "Expenses & Finance", "اخراجات و مالیات", Icons.Outlined.AccountBalanceWallet),
    DrawerMenuItem(AppRoute.Salary, "Salary & Payroll", "تنخواہیں", Icons.Outlined.Payments, staffLabel = "My Salary", staffLabelUrdu = "میری تنخواہ"),
    DrawerMenuItem(AppRoute.Utilities, "Electricity / Diesel / Water", "بجلی، ڈیزل، پانی", Icons.Outlined.Bolt),
    DrawerMenuItem(AppRoute.Maintenance, "Machine Maintenance", "مشینری مرمت", Icons.Outlined.Build),
    DrawerMenuItem(AppRoute.Suppliers, "Suppliers", "سپلائرز", Icons.Outlined.LocalMall),
    DrawerMenuItem(AppRoute.Incidents, "Safety Incidents", "حفاظتی شکایت", Icons.Outlined.WarningAmber),
    DrawerMenuItem(AppRoute.Reports, "Reports & Analytics", "رپورٹس", Icons.Outlined.BarChart),
    DrawerMenuItem(AppRoute.Profile, "My Profile", "میرا پروفائل", Icons.Outlined.Person),
    DrawerMenuItem(AppRoute.Settings, "Settings", "سیٹنگز", Icons.Outlined.Settings),
    DrawerMenuItem(AppRoute.About, "About", "تعارف", Icons.Outlined.Info),
    DrawerMenuItem(AppRoute.Updates, "App Updates", "ایپ اپڈیٹ", Icons.Outlined.SystemUpdate)
)

@Composable
fun CrystalBackground(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val brush = if (darkTheme) {
        Brush.verticalGradient(listOf(IceBackgroundDark, Color(0xFF0C1D3B)))
    } else {
        Brush.verticalGradient(listOf(CrystalIceBgTop, CrystalIceBgMid, CrystalIceBgBottom))
    }
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(brush)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val topRibbon = Path().apply {
                moveTo(0f, h * 0.05f)
                cubicTo(w * 0.28f, h * 0.16f, w * 0.65f, h * 0.08f, w, h * 0.18f)
                lineTo(w, h * 0.24f)
                cubicTo(w * 0.62f, h * 0.14f, w * 0.24f, h * 0.23f, 0f, h * 0.10f)
                close()
            }
            drawPath(
                path = topRibbon,
                brush = Brush.horizontalGradient(
                    colors = if (darkTheme) {
                        listOf(Color(0x1438BDF8), Color(0x220F6DF7))
                    } else {
                        listOf(Color(0x558AC7FF), Color(0x8857B8FF))
                    }
                )
            )
            val bottomRibbon = Path().apply {
                moveTo(0f, h * 0.83f)
                cubicTo(w * 0.34f, h * 0.74f, w * 0.70f, h * 0.90f, w, h * 0.78f)
                lineTo(w, h * 0.92f)
                cubicTo(w * 0.66f, h * 0.98f, w * 0.30f, h * 0.85f, 0f, h * 0.94f)
                close()
            }
            drawPath(
                path = bottomRibbon,
                brush = Brush.horizontalGradient(
                    colors = if (darkTheme) {
                        listOf(Color(0x160F6DF7), Color(0x0A38BDF8))
                    } else {
                        listOf(Color(0x40A7D4FF), Color(0x667CC3FF))
                    }
                )
            )
        }
        content()
    }
}

@Composable
fun CrystalTextField(
    value: String,
    onValueChange: (String) -> Unit,
    englishLabel: String,
    urduLabel: String = "",
    leadingIcon: ImageVector? = null,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    singleLine: Boolean = true,
    readOnly: Boolean = false,
    keyboardType: KeyboardType? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: (@Composable () -> Unit)? = null,
    supportingText: (@Composable () -> Unit)? = null,
    isError: Boolean = false
) {
    val darkTheme = isSystemInDarkTheme()
    val containerColor = if (darkTheme) Color(0xFF152C56) else Color.White
    val unfocusedBorder = if (darkTheme) SoftBorderDark else CrystalBorderLight
    val effectiveKeyboardOptions = if (keyboardType != null) {
        keyboardOptions.copy(keyboardType = keyboardType)
    } else {
        keyboardOptions
    }

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        readOnly = readOnly,
        label = {
            Text(
                text = if (urduLabel.isNotBlank()) "$englishLabel • $urduLabel" else englishLabel,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        placeholder = if (placeholder.isNotBlank()) {
            { Text(placeholder, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)) }
        } else null,
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = if (isError) DangerRed else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        trailingIcon = trailingIcon,
        supportingText = supportingText,
        singleLine = singleLine,
        isError = isError,
        keyboardOptions = effectiveKeyboardOptions,
        keyboardActions = keyboardActions,
        visualTransformation = visualTransformation,
        shape = RoundedCornerShape(14.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = containerColor,
            unfocusedContainerColor = containerColor,
            disabledContainerColor = containerColor.copy(alpha = 0.7f),
            errorContainerColor = if (darkTheme) Color(0xFF2D1622) else Color(0xFFFFF5F5),
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = unfocusedBorder,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Automatically displays Pakistani NADRA CNIC dashes (XXXXX-XXXXXXX-X, e.g. 44204-1253835-5)
 * while the underlying state stores only up to 13 raw digits.
 */
class CnicVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val formatted = buildString {
            for (i in raw.indices) {
                append(raw[i])
                if (i == 4 && i + 1 < raw.length) append('-')
                if (i == 11 && i + 1 < raw.length) append('-')
            }
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                val dashesBefore = when {
                    raw.length <= 5 || clamped <= 5 -> 0
                    raw.length <= 12 || clamped <= 12 -> 1
                    else -> 2
                }
                return (clamped + dashesBefore).coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                var dashesSeen = 0
                if (raw.length > 5 && clamped > 5) dashesSeen++
                if (raw.length > 12 && clamped > 13) dashesSeen++
                return (clamped - dashesSeen).coerceIn(0, raw.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
fun CnicCrystalTextField(
    digitsValue: String,
    onDigitsChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    englishLabel: String = "CNIC Number",
    urduLabel: String = "شناختی کارڈ نمبر",
    leadingIcon: ImageVector = Icons.Outlined.Badge,
    isError: Boolean = false
) {
    val safeDigits = PakistaniFormatters.normalizeCnicDigits(digitsValue)
    val cnicTransformation = remember { CnicVisualTransformation() }
    val isComplete = safeDigits.length == PakistaniFormatters.CNIC_DIGIT_COUNT

    CrystalTextField(
        value = safeDigits,
        onValueChange = { incoming ->
            val rawDigitsOnly = PakistaniFormatters.convertUrduDigits(incoming).filter { it.isDigit() }
            val nextDigits = if (safeDigits.length >= PakistaniFormatters.CNIC_DIGIT_COUNT &&
                rawDigitsOnly.length > PakistaniFormatters.CNIC_DIGIT_COUNT
            ) {
                // Strictly block typing additional digits anywhere once 13 digits are already entered
                safeDigits
            } else {
                rawDigitsOnly.take(PakistaniFormatters.CNIC_DIGIT_COUNT)
            }
            onDigitsChange(nextDigits)
        },
        englishLabel = englishLabel,
        urduLabel = urduLabel,
        leadingIcon = leadingIcon,
        placeholder = "44204-1253835-5",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = cnicTransformation,
        isError = isError,
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                if (isComplete) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Complete CNIC",
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "${safeDigits.length}/13",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isError -> DangerRed
                        isComplete -> SuccessGreen
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        supportingText = {
            Text(
                text = if (isError) {
                    "Enter complete 13 digits: 44204-1253835-5 (13 ہندسے مکمل لکھیں)"
                } else {
                    "Auto-formatted: 44204-1253835-5 (ڈیش خود بخود لگیں گے)"
                },
                fontSize = 11.sp,
                color = if (isError) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier
    )
}

/**
 * Automatically displays Pakistani mobile dashes (XXXX-XXXXXXX, e.g. 0333-2828285)
 * while the underlying state stores only up to 11 raw digits.
 */
class PhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        val formatted = buildString {
            for (i in raw.indices) {
                append(raw[i])
                if (i == 3 && i + 1 < raw.length) append('-')
            }
        }
        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                val dashBefore = if (raw.length > 4 && clamped > 4) 1 else 0
                return (clamped + dashBefore).coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                val dashSeen = if (raw.length > 4 && clamped > 4) 1 else 0
                return (clamped - dashSeen).coerceIn(0, raw.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

@Composable
fun PhoneCrystalTextField(
    digitsValue: String,
    onDigitsChange: (String) -> Unit,
    englishLabel: String,
    urduLabel: String,
    modifier: Modifier = Modifier,
    leadingIcon: ImageVector = Icons.Outlined.Phone,
    placeholder: String = "0333-2828285",
    helperText: String = "Auto-formatted: 0333-2828285 (11 ہندسے)",
    errorMessage: String? = null,
    isError: Boolean = false,
    actionChip: (@Composable () -> Unit)? = null
) {
    val safeDigits = PakistaniFormatters.normalizePhoneDigits(digitsValue)
    val phoneTransformation = remember { PhoneVisualTransformation() }
    val isComplete = safeDigits.length == PakistaniFormatters.PHONE_DIGIT_COUNT

    CrystalTextField(
        value = safeDigits,
        onValueChange = { incoming ->
            val rawDigitsOnly = PakistaniFormatters.convertUrduDigits(incoming).filter { it.isDigit() }
            val nextDigits = if (safeDigits.length >= PakistaniFormatters.PHONE_DIGIT_COUNT &&
                rawDigitsOnly.length > PakistaniFormatters.PHONE_DIGIT_COUNT
            ) {
                // Strictly block typing additional digits anywhere once 11 digits are already entered
                safeDigits
            } else {
                rawDigitsOnly.take(PakistaniFormatters.PHONE_DIGIT_COUNT)
            }
            onDigitsChange(nextDigits)
        },
        englishLabel = englishLabel,
        urduLabel = urduLabel,
        leadingIcon = leadingIcon,
        placeholder = placeholder,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        visualTransformation = phoneTransformation,
        isError = isError,
        trailingIcon = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(end = 8.dp)
            ) {
                actionChip?.invoke()
                if (isComplete && !isError) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = "Complete Number",
                        tint = SuccessGreen,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Text(
                    text = "${safeDigits.length}/11",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        isError -> DangerRed
                        isComplete -> SuccessGreen
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        },
        supportingText = {
            Text(
                text = if (isError) {
                    errorMessage?.takeIf { it.isNotBlank() }
                        ?: "Number is not complete: enter 11 digits e.g. 0333-2828285 (نمبر مکمل نہیں ہے)"
                } else {
                    helperText
                },
                fontSize = 11.sp,
                color = if (isError) DangerRed else MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        modifier = modifier
    )
}

@Composable
fun ValidationPopupDialog(
    title: String?,
    message: String?,
    onDismiss: () -> Unit
) {
    if (message.isNullOrBlank()) return
    CrystalFormDialog(
        onDismissRequest = onDismiss,
        title = title?.ifBlank { "Incomplete Details" } ?: "Incomplete Details",
        subtitle = "معلومات نامکمل ہیں",
        icon = Icons.Outlined.WarningAmber,
        confirmText = "OK (ٹھیک ہے)",
        onConfirm = onDismiss,
        showDismissButton = false,
        isDestructive = true,
        confirmTestTag = "validation_popup_ok"
    ) {
        Text(
            text = message,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
fun BilingualPillLabel(
    english: String,
    urdu: String,
    textColor: Color = Color.Unspecified,
    urduColor: Color = textColor
) {
    androidx.compose.foundation.layout.FlowRow(
        horizontalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterHorizontally),
        verticalArrangement = Arrangement.spacedBy(2.dp, Alignment.CenterVertically)
    ) {
        Text(
            text = english,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
        Text(
            text = "($urdu)",
            color = urduColor.copy(alpha = 0.92f),
            fontWeight = FontWeight.Medium,
            fontSize = 11.5.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun UserAvatarImage(
    photoUrl: String?,
    contentDescription: String,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape
) {
    val decodedBitmap = remember(photoUrl) {
        if (ImageAndQrUtils.isDataUri(photoUrl)) {
            ImageAndQrUtils.decodeDataUriToBitmap(photoUrl)?.asImageBitmap()
        } else {
            null
        }
    }
    when {
        decodedBitmap != null -> {
            Image(
                bitmap = decodedBitmap,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier.clip(shape)
            )
        }
        !photoUrl.isNullOrBlank() && !ImageAndQrUtils.isDataUri(photoUrl) -> {
            AsyncImage(
                model = photoUrl,
                contentDescription = contentDescription,
                placeholder = painterResource(R.drawable.img_avatar_default),
                error = painterResource(R.drawable.img_avatar_default),
                contentScale = ContentScale.Crop,
                modifier = modifier.clip(shape)
            )
        }
        else -> {
            Image(
                painter = painterResource(R.drawable.img_avatar_default),
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = modifier.clip(shape)
            )
        }
    }
}

@Composable
fun BrohiDashboardTopBar(
    photoUrl: String?,
    notificationCount: Int,
    onMenuClick: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryBlueGradient)
            .statusBarsPadding()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onMenuClick,
                modifier = Modifier.testTag("top_menu_button")
            ) {
                Icon(
                    imageVector = Icons.Filled.Menu,
                    contentDescription = "Open navigation menu",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AcUnit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(22.dp)
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "BROHI",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        lineHeight = 16.sp
                    )
                    Text(
                        text = "ICE FACTORY",
                        color = Color(0xFFDCE8FF),
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        lineHeight = 12.sp
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box {
                    IconButton(
                        onClick = onNotificationClick,
                        modifier = Modifier.testTag("top_notifications_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Notifications,
                            contentDescription = "Notifications",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (notificationCount > 0) {
                        Surface(
                            shape = CircleShape,
                            color = DangerRed,
                            border = BorderStroke(1.dp, Color.White),
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(top = 4.dp, end = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (notificationCount > 99) "99+" else notificationCount.toString(),
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
                UserAvatarImage(
                    photoUrl = photoUrl,
                    contentDescription = "Open Profile",
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .border(2.dp, Color.White, CircleShape)
                        .clickable(onClick = onProfileClick)
                        .testTag("top_profile_avatar")
                )
            }
        }
    }
}

@Composable
fun BrohiSubScreenTopBar(
    title: String,
    onBackClick: () -> Unit,
    trailingAction: (@Composable () -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(PrimaryBlueGradient)
            .statusBarsPadding()
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.testTag("subscreen_back_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Navigate back",
                    tint = Color.White
                )
            }
            Text(
                text = title,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 4.dp)
            )
            if (trailingAction != null) {
                trailingAction()
            } else {
                Spacer(modifier = Modifier.width(48.dp))
            }
        }
    }
}

@Composable
fun Crystal3DNavigationDrawer(
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    drawerContent: @Composable () -> Unit,
    content: @Composable () -> Unit
) {
    BackHandler(enabled = isOpen) {
        onOpenChange(false)
    }

    val progress by animateFloatAsState(
        targetValue = if (isOpen) 1f else 0f,
        animationSpec = tween(
            durationMillis = 380,
            easing = CubicBezierEasing(0.22f, 1f, 0.36f, 1f)
        ),
        label = "drawer_3d_progress"
    )
    val density = LocalDensity.current
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val dirSign = if (isRtl) -1f else 1f

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val drawerWidth = (maxWidth * 0.64f).coerceIn(244.dp, 316.dp)
        val shiftFraction = if (maxWidth > 600.dp) 0.44f else 0.65f
        val targetShiftPx = with(density) { (maxWidth * shiftFraction).toPx() }

        // Full-screen deep navy 3D backdrop + subtle crystal watermark grid
        if (progress > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF07152E),
                                Color(0xFF0D2349),
                                Color(0xFF132F61)
                            )
                        )
                    )
            ) {
                // Subtle crystal watermarks across backdrop matching app identity
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 24.dp),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    repeat(5) { rowIdx ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            repeat(3) { colIdx ->
                                Icon(
                                    imageVector = Icons.Filled.AcUnit,
                                    contentDescription = null,
                                    tint = Color.White.copy(
                                        alpha = (if ((rowIdx + colIdx) % 2 == 0) 0.045f else 0.025f) * progress
                                    ),
                                    modifier = Modifier.size(64.dp)
                                )
                            }
                        }
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(drawerWidth)
                        .align(Alignment.CenterStart)
                        .graphicsLayer {
                            alpha = (0.25f + 0.75f * progress).coerceIn(0f, 1f)
                            translationX = -28f * density.density * (1f - progress) * dirSign
                        }
                ) {
                    drawerContent()
                }
            }
        }

        // Main screen transformed in 3D space (scale down, perspective Y rotation, rounded card, blur)
        val cardShape = RoundedCornerShape((28f * progress).dp)
        val blurModifier = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && progress > 0.02f) {
            Modifier.blur((2.5f * progress).dp)
        } else {
            Modifier
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val scale = 1f - (0.20f * progress)
                    scaleX = scale
                    scaleY = scale
                    translationX = targetShiftPx * progress * dirSign
                    rotationY = -14f * progress * dirSign
                    cameraDistance = 14f * density.density
                    shape = cardShape
                    clip = progress > 0.005f
                    shadowElevation = (26f * progress * density.density)
                }
                .then(
                    if (progress > 0.02f) {
                        Modifier.border(
                            width = 1.2.dp,
                            color = Color.White.copy(alpha = 0.28f * progress),
                            shape = cardShape
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .then(blurModifier)
            ) {
                content()
            }

            if (isOpen || progress > 0.05f) {
                val clickSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF061229).copy(alpha = 0.14f * progress))
                        .pointerInput(isRtl) {
                            var dragAccum = 0f
                            detectHorizontalDragGestures(
                                onDragStart = { dragAccum = 0f },
                                onHorizontalDrag = { _, dragAmount ->
                                    dragAccum += dragAmount
                                    if ((!isRtl && dragAccum < -42f) || (isRtl && dragAccum > 42f)) {
                                        onOpenChange(false)
                                    }
                                }
                            )
                        }
                        .clickable(
                            interactionSource = clickSource,
                            indication = null,
                            onClick = { onOpenChange(false) }
                        )
                )
            }
        }
    }
}

@Composable
fun BrohiDrawerContent(
    userName: String,
    photoUrl: String?,
    activeRole: UserRole,
    currentRouteKey: String,
    strings: AppStrings,
    isUpdateAvailable: Boolean = false,
    onSelectRoute: (AppRoute) -> Unit,
    onProfileClick: () -> Unit,
    onLogoutClick: () -> Unit
) {
    val visibleItems = buildDrawerMenuItems().filter { item ->
        AppRoute.isAllowedFor(activeRole, item.route.key)
    }
    val formattedName = PakistaniFormatters.formatTitleCase(userName.ifBlank { "Ahmed Khan" })

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.AcUnit,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        text = "BROHI",
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp
                    )
                    Text(
                        text = "ICE FACTORY • بروہی آئس فیکٹری",
                        color = Color(0xFFBBD3FF),
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onProfileClick)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                UserAvatarImage(
                    photoUrl = photoUrl,
                    contentDescription = "User photo",
                    modifier = Modifier
                        .size(44.dp)
                        .border(2.dp, Color(0xFF8AB4FF), CircleShape)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formattedName,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(SuccessGreen)
                        )
                        Text(
                            text = activeRole.label,
                            color = Color(0xFFCFE0FF),
                            fontSize = 12.sp
                        )
                    }
                }
            }

            HorizontalDivider(
                color = Color.White.copy(alpha = 0.14f),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                items(visibleItems, key = { it.route.key }) { item ->
                    val selected = currentRouteKey == item.route.key
                    val bg = if (selected) DrawerActiveBg else Color.Transparent
                    val textColor = if (selected) DarkNavy else Color.White.copy(alpha = 0.95f)
                    val urduColor = if (selected) RoyalBlue else Color(0xFFAEC8FA)
                    val iconTint = if (selected) RoyalBlue else Color(0xFF9AC0FF)
                    val enText = if (activeRole == UserRole.STAFF && item.staffLabel != null) item.staffLabel else item.label
                    val urText = if (activeRole == UserRole.STAFF && item.staffLabelUrdu != null) item.staffLabelUrdu else item.labelUrdu
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(bg)
                            .clickable { onSelectRoute(item.route) }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                            .testTag("drawer_item_${item.route.key}"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(19.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = enText,
                                color = textColor,
                                fontSize = 13.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = urText,
                                color = urduColor,
                                fontSize = 10.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        if (isUpdateAvailable && item.route == AppRoute.Updates) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = SuccessGreen
                            ) {
                                Text(
                                    text = "NEW",
                                    color = Color.White,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onLogoutClick)
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .testTag("drawer_logout_button"),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = Color(0xFFFFB4B4),
                    modifier = Modifier.size(19.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "${strings.logout} (لاگ آؤٹ)",
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun BrohiBottomTaskbar(
    currentRouteKey: String,
    activeRole: UserRole,
    strings: AppStrings,
    onNavigate: (AppRoute) -> Unit,
    onMicClick: () -> Unit,
    onMenuClick: () -> Unit
) {
    val darkTheme = isSystemInDarkTheme()
    val fourthRoute = if (activeRole == UserRole.STAFF) AppRoute.Salary else AppRoute.Sales
    val fourthLabel = if (activeRole == UserRole.STAFF) "Salary" else strings.sales
    val fourthIcon = if (activeRole == UserRole.STAFF) Icons.Outlined.Payments else Icons.Outlined.ShoppingCart
    val fourthTag = if (activeRole == UserRole.STAFF) "bottom_nav_salary" else "bottom_nav_sales"

    val dockBg = if (darkTheme) Color(0xFF13264B) else Color(0xFFFDFEFF)
    val dockBorder = if (darkTheme) SoftBorderDark else Color.White.copy(alpha = 0.95f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            color = dockBg,
            shape = RoundedCornerShape(30.dp),
            tonalElevation = 2.dp,
            shadowElevation = 14.dp,
            border = BorderStroke(1.2.dp, dockBorder),
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                BottomTaskItem(
                    label = strings.home,
                    icon = Icons.Outlined.Home,
                    selected = currentRouteKey == AppRoute.Dashboard.key,
                    testTag = "bottom_nav_home",
                    onClick = { onNavigate(AppRoute.Dashboard) },
                    modifier = Modifier.weight(1f)
                )
                BottomTaskItem(
                    label = strings.attendance,
                    icon = Icons.Outlined.EventAvailable,
                    selected = currentRouteKey == AppRoute.Attendance.key,
                    testTag = "bottom_nav_attendance",
                    onClick = { onNavigate(AppRoute.Attendance) },
                    modifier = Modifier.weight(1f)
                )

                Spacer(modifier = Modifier.weight(1f))

                BottomTaskItem(
                    label = fourthLabel,
                    icon = fourthIcon,
                    selected = currentRouteKey == fourthRoute.key,
                    testTag = fourthTag,
                    onClick = { onNavigate(fourthRoute) },
                    modifier = Modifier.weight(1f)
                )
                BottomTaskItem(
                    label = strings.menu,
                    icon = Icons.Outlined.Menu,
                    selected = false,
                    testTag = "bottom_nav_menu",
                    onClick = onMenuClick,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Elevated Glowing 3D Mic Orb with Gold-Ice Outer Halo Ring
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = (-6).dp)
                .size(66.dp)
                .shadow(
                    elevation = 14.dp,
                    shape = CircleShape,
                    ambientColor = IcyGlowShadow.copy(alpha = 0.65f),
                    spotColor = IcyGlowShadow.copy(alpha = 0.75f)
                )
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.White, IcyGoldRim, IcyGlossRim)
                    )
                )
                .padding(3.5.dp)
                .clip(CircleShape)
                .background(
                    Brush.verticalGradient(
                        listOf(IcyGlossTop, IcyGlossMid, IcyGlossBottom)
                    )
                )
                .border(1.2.dp, Color.White.copy(alpha = 0.9f), CircleShape)
                .clickable(onClick = onMicClick)
                .testTag("bottom_nav_mic"),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(CircleShape)
                    .background(
                        Brush.verticalGradient(
                            0.0f to Color.White.copy(alpha = 0.35f),
                            0.48f to Color.Transparent
                        )
                    )
            )
            Icon(
                imageVector = Icons.Filled.Mic,
                contentDescription = "Voice Assistant Command",
                tint = Color.White,
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun BottomTaskItem(
    label: String,
    icon: ImageVector,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val darkTheme = isSystemInDarkTheme()
    val color = when {
        selected -> if (darkTheme) IcyGlossTop else RoyalBlue
        darkTheme -> Color(0xFFA7BBE0)
        else -> DeepBlue
    }
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .testTag(testTag),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(21.dp)
        )
        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BrohiCard(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(22.dp)
    val cardBrush = if (darkTheme) {
        Brush.verticalGradient(listOf(Color(0xFF152B54), Color(0xFF0E2041)))
    } else {
        Brush.verticalGradient(listOf(Color.White, Color(0xFFF3F8FF)))
    }
    val borderBrush = if (darkTheme) {
        Brush.verticalGradient(listOf(Color(0xFF2B4B85), SoftBorderDark))
    } else {
        Brush.verticalGradient(listOf(Color.White, CrystalBorderLight))
    }
    val cardModifier = modifier
        .shadow(
            elevation = if (darkTheme) 6.dp else 8.dp,
            shape = shape,
            ambientColor = if (darkTheme) Color.Black else Color(0xFF6BA8FF).copy(alpha = 0.25f),
            spotColor = if (darkTheme) Color.Black else Color(0xFF569BFF).copy(alpha = 0.25f)
        )
        .clip(shape)
        .background(cardBrush)
        .border(width = 1.2.dp, brush = borderBrush, shape = shape)
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Box(modifier = cardModifier) {
        content()
    }
}

@Composable
fun SalesTrendBarChart(
    bars: List<DaySalesBar>,
    modifier: Modifier = Modifier
) {
    val maxVal = (bars.maxOfOrNull { it.amount } ?: 1.0).coerceAtLeast(1.0)
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
        ) {
            val count = bars.size.coerceAtLeast(1)
            val slotW = size.width / count
            val barW = (slotW * 0.46f).coerceIn(14f, 42f)
            val bottomY = size.height - 6f

            drawLine(
                color = labelColor.copy(alpha = 0.25f),
                start = Offset(0f, bottomY),
                end = Offset(size.width, bottomY),
                strokeWidth = 2f
            )

            bars.forEachIndexed { idx, bar ->
                val ratio = (bar.amount / maxVal).toFloat().coerceIn(0.08f, 1f)
                val barH = (bottomY - 8f) * ratio
                val left = idx * slotW + (slotW - barW) / 2f
                val top = bottomY - barH

                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(RoyalBlue, DeepBlue),
                        startY = top,
                        endY = bottomY
                    ),
                    topLeft = Offset(left, top),
                    size = Size(barW, barH),
                    cornerRadius = CornerRadius(10f, 10f)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            bars.forEach { bar ->
                Text(
                    text = bar.dayLabel,
                    fontSize = 11.sp,
                    color = labelColor,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun StockDonutChart(
    totalTons: Double,
    blocksPercent: Float,
    cubesPercent: Float,
    modifier: Modifier = Modifier
) {
    val primaryColor = RoyalBlue
    val secondaryColor = WarningOrange
    val tertiaryColor = AccentCyan

    Box(
        modifier = modifier.size(132.dp),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            val strokeWidth = 26f
            val sweepBlocks = (blocksPercent / 100f) * 360f
            val sweepCubes = (cubesPercent / 100f) * 360f
            val gap = 4f

            drawArc(
                color = primaryColor,
                startAngle = -90f,
                sweepAngle = (sweepBlocks - gap).coerceAtLeast(10f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            drawArc(
                color = secondaryColor,
                startAngle = -90f + sweepBlocks,
                sweepAngle = (sweepCubes * 0.65f - gap).coerceAtLeast(10f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
            drawArc(
                color = tertiaryColor,
                startAngle = -90f + sweepBlocks + sweepCubes * 0.65f,
                sweepAngle = (sweepCubes * 0.35f - gap).coerceAtLeast(8f),
                useCenter = false,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Butt)
            )
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = PakistaniFormatters.formatTons(totalTons),
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Total Stock",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private data class RoleOptionSpec(
    val key: String,
    val titleEn: String,
    val titleUr: String,
    val subtitle: String,
    val icon: ImageVector,
    val accent: Color
)

@Composable
fun ModernRoleSelectorCardGrid(
    selectedRole: String,
    roles: List<String>,
    onSelectRole: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val specs = roles.map { role ->
        when (UserRole.fromRequestedRole(role)) {
            UserRole.OWNER -> RoleOptionSpec(
                key = "Super Admin",
                titleEn = "Super Admin",
                titleUr = "سپر ایڈمن",
                subtitle = "Full control & approvals",
                icon = Icons.Outlined.AdminPanelSettings,
                accent = Color(0xFF1565C0)
            )
            UserRole.ADMIN -> RoleOptionSpec(
                key = "Admin",
                titleEn = "Admin",
                titleUr = "ایڈمن",
                subtitle = "Sales, expenses & staff",
                icon = Icons.Outlined.ManageAccounts,
                accent = Color(0xFF00796B)
            )
            UserRole.MANAGER -> RoleOptionSpec(
                key = "Manager",
                titleEn = "Manager",
                titleUr = "مینیجر",
                subtitle = "Plant, stock & vehicles",
                icon = Icons.Outlined.PrecisionManufacturing,
                accent = Color(0xFFEF6C00)
            )
            UserRole.STAFF -> RoleOptionSpec(
                key = "Staff",
                titleEn = "Staff",
                titleUr = "اسٹاف",
                subtitle = "Attendance, salary & notices",
                icon = Icons.Outlined.Badge,
                accent = Color(0xFF455A64)
            )
        }
    }
    val selectedKey = UserRole.fromRequestedRole(selectedRole).label
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        specs.chunked(2).forEach { rowSpecs ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowSpecs.forEach { spec ->
                    val isSelected = selectedKey.equals(spec.key, ignoreCase = true)
                    val container = if (isSelected) {
                        lerp(MaterialTheme.colorScheme.surface, spec.accent, 0.14f)
                    } else {
                        lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant, 0.55f)
                    }
                    val border = if (isSelected) {
                        BorderStroke(1.8.dp, spec.accent)
                    } else {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))
                    }
                    Surface(
                        onClick = { onSelectRole(spec.key) },
                        shape = RoundedCornerShape(16.dp),
                        color = container,
                        border = border,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("role_card_${spec.key.lowercase().replace(" ", "_")}")
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(spec.accent.copy(alpha = if (isSelected) 0.24f else 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = spec.icon,
                                        contentDescription = spec.titleEn,
                                        tint = spec.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = "Selected",
                                        tint = spec.accent,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = spec.titleEn,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = spec.titleUr,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = spec.accent,
                                maxLines = 1
                            )
                            Text(
                                text = spec.subtitle,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
                if (rowSpecs.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun ModernShiftSelector(
    selectedShift: String,
    shifts: List<String>,
    onSelectShift: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        shifts.forEachIndexed { index, shift ->
            val selected = selectedShift == shift
            val (icon, shortTitle, badgeText) = when {
                shift.contains("Morning", ignoreCase = true) -> Triple(Icons.Outlined.WbSunny, "Morning Shift (صبح)", "8AM - 4PM")
                shift.contains("Evening", ignoreCase = true) -> Triple(Icons.Outlined.Schedule, "Evening Shift (شام)", "4PM - 12AM")
                shift.contains("Night", ignoreCase = true) -> Triple(Icons.Outlined.NightsStay, "Night Shift (رات)", "12AM - 8AM")
                else -> Triple(Icons.Outlined.AllInclusive, "No Fixed Timing / 24h", "مقررہ وقت نہیں")
            }
            val accent = if (shift.contains("No Fixed", ignoreCase = true)) Color(0xFF00796B) else RoyalBlue
            Surface(
                onClick = { onSelectShift(shift) },
                shape = RoundedCornerShape(14.dp),
                color = if (selected) {
                    lerp(MaterialTheme.colorScheme.surface, accent, 0.13f)
                } else {
                    lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant, 0.45f)
                },
                border = BorderStroke(
                    width = if (selected) 1.6.dp else 1.dp,
                    color = if (selected) accent else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("shift_card_$index")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = shortTitle,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (selected) {
                            lerp(MaterialTheme.colorScheme.surface, accent, 0.20f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        }
                    ) {
                        Text(
                            text = badgeText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) accent else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                    if (selected) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Selected shift",
                            tint = accent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

