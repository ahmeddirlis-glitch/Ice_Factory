package com.example.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.model.REGISTRATION_ROLE_OPTIONS
import com.example.data.model.RegistrationRequest
import com.example.data.model.UserProfile
import com.example.data.model.WORK_SHIFT_OPTIONS
import com.example.data.util.AppStrings
import com.example.data.util.PakistaniFormatters
import com.example.ui.components.BilingualPillLabel
import com.example.ui.components.BrohiCard
import com.example.ui.components.CnicCrystalTextField
import com.example.ui.components.CrystalBackground
import com.example.ui.components.CrystalFormDialog
import com.example.ui.components.CrystalTextField
import com.example.ui.components.IcyGlossyButton
import com.example.ui.components.IcyGlossyTone
import com.example.ui.components.ModernRoleSelectorCardGrid
import com.example.ui.components.ModernShiftSelector
import com.example.ui.components.PhoneCrystalTextField
import com.example.ui.components.PrimaryBlueGradient
import com.example.ui.components.ValidationPopupDialog
import com.example.ui.theme.AccentCyan
import com.example.ui.theme.CrystalBorderLight
import com.example.ui.theme.CrystalIceBgBottom
import com.example.ui.theme.CrystalIceBgTop
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkNavy
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import kotlinx.coroutines.delay
import org.json.JSONObject
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun SplashScreen(
    strings: AppStrings,
    onTimeout: () -> Unit
) {
    val density = LocalDensity.current
    val spiralIn = remember { Animatable(0f) }
    val spiralOut = remember { Animatable(0f) }
    val holdPulse = remember { Animatable(1f) }
    val screenAlpha = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        // Phase 1: Brohi Ice Factory spirals smoothly into the center
        spiralIn.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1550, easing = FastOutSlowInEasing)
        )
        // Phase 2: Crisp centered crystal display with subtle pulse
        holdPulse.animateTo(
            targetValue = 1.05f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )
        holdPulse.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
        )
        delay(850L)
        // Phase 3: Brohi Ice Factory spirals away to end the splash screen cleanly
        spiralOut.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 950, easing = FastOutLinearInEasing)
        )
        screenAlpha.animateTo(
            targetValue = 0f,
            animationSpec = tween(durationMillis = 220, easing = LinearEasing)
        )
        onTimeout()
    }

    val infinite = rememberInfiniteTransition(label = "spinner")
    val angle by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "spinAngle"
    )
    val haloSweep by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
        label = "haloSweep"
    )

    val inProgress = spiralIn.value
    val outProgress = spiralOut.value
    val spiralTranslationX: Float
    val spiralTranslationY: Float
    val spiralRotationZ: Float
    val spiralScale: Float
    val spiralAlpha: Float

    if (outProgress > 0f) {
        val thetaOut = outProgress * (4.0 * PI).toFloat()
        val radiusOutPx = with(density) { (outProgress * 78f).dp.toPx() }
        spiralTranslationX = cos(thetaOut) * radiusOutPx
        spiralTranslationY = sin(thetaOut) * radiusOutPx
        spiralRotationZ = outProgress * 720f
        spiralScale = (1f - 0.88f * outProgress).coerceAtLeast(0.06f)
        spiralAlpha = (1f - outProgress).coerceIn(0f, 1f)
    } else {
        val thetaIn = (1f - inProgress) * (4.0 * PI).toFloat()
        val radiusInPx = with(density) { ((1f - inProgress) * 78f).dp.toPx() }
        spiralTranslationX = cos(thetaIn) * radiusInPx
        spiralTranslationY = sin(thetaIn) * radiusInPx
        spiralRotationZ = (1f - inProgress) * -720f
        spiralScale = (0.14f + 0.86f * inProgress) * holdPulse.value
        spiralAlpha = (inProgress / 0.32f).coerceIn(0f, 1f)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = screenAlpha.value }
            .background(
                Brush.verticalGradient(
                    colors = listOf(CrystalIceBgTop, Color(0xFFF6FAFF), CrystalIceBgBottom)
                )
            )
    ) {
        // Soft, pale Nawabshah heritage clock tower background illustration under icy crystal veil
        Image(
            painter = painterResource(R.drawable.img_nawabshah_heritage),
            contentDescription = "Nawabshah Heritage",
            contentScale = ContentScale.Crop,
            alpha = 0.28f,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0xD8F2F7FF),
                            Color(0x99FFFFFF),
                            Color(0xCCDAE9FF)
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .padding(horizontal = 24.dp)
                    .graphicsLayer {
                        translationX = spiralTranslationX
                        translationY = spiralTranslationY
                        rotationZ = spiralRotationZ
                        scaleX = spiralScale
                        scaleY = spiralScale
                        alpha = spiralAlpha
                    }
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(124.dp)
                ) {
                    // Icy spiral vortex arcs framing the Brohi emblem
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 2.6.dp.toPx()
                        val outerPad = strokeWidth
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(Color.Transparent, AccentCyan.copy(alpha = 0.7f), RoyalBlue)
                            ),
                            startAngle = haloSweep,
                            sweepAngle = 210f,
                            useCenter = false,
                            topLeft = Offset(outerPad, outerPad),
                            size = Size(size.width - outerPad * 2f, size.height - outerPad * 2f),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                        val innerPad = 10.dp.toPx()
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(Color.Transparent, RoyalBlue.copy(alpha = 0.65f), DeepBlue)
                            ),
                            startAngle = -haloSweep * 1.3f,
                            sweepAngle = 190f,
                            useCenter = false,
                            topLeft = Offset(innerPad, innerPad),
                            size = Size(size.width - innerPad * 2f, size.height - innerPad * 2f),
                            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                        )
                    }
                    Surface(
                        shape = CircleShape,
                        color = Color(0xFFFAFDFF),
                        border = BorderStroke(1.5.dp, CrystalBorderLight),
                        shadowElevation = 6.dp,
                        modifier = Modifier.size(90.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Filled.AcUnit,
                                contentDescription = "Brohi Ice Factory Logo",
                                tint = DeepBlue,
                                modifier = Modifier.size(52.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "BROHI",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepBlue,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "ICE FACTORY",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "بروہی آئس فیکٹری • نوابشاہ",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RoyalBlue
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = strings.tagline,
                    fontSize = 13.sp,
                    fontStyle = FontStyle.Italic,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF1E3A70),
                    textAlign = TextAlign.Center
                )
            }

            // Frosted Crystal Ice Banner Preview
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFFF4F9FF),
                border = BorderStroke(1.dp, CrystalBorderLight),
                shadowElevation = 4.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
            ) {
                Image(
                    painter = painterResource(R.drawable.img_ice_cubes_banner),
                    contentDescription = "Crystal ice cubes",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(22.dp))
                )
            }

            // Bottom Navy Wave Footer with "Powered by Firebase"
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val path = Path().apply {
                        moveTo(0f, size.height * 0.25f)
                        quadraticTo(size.width * 0.35f, -size.height * 0.1f, size.width, size.height * 0.28f)
                        lineTo(size.width, size.height)
                        lineTo(0f, size.height)
                        close()
                    }
                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(listOf(DeepBlue, DarkNavy))
                    )
                }
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(top = 34.dp, bottom = 14.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Powered by SHIPDAR & TECH • شیپدار اینڈ ٹیک",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Canvas(
                        modifier = Modifier
                            .size(32.dp)
                            .rotate(angle)
                    ) {
                        val dots = 8
                        val radius = size.minDimension / 2.5f
                        for (i in 0 until dots) {
                            val rad = Math.toRadians((i * 360.0 / dots))
                            val cx = center.x + (radius * cos(rad)).toFloat()
                            val cy = center.y + (radius * sin(rad)).toFloat()
                            drawCircle(
                                color = Color.White.copy(alpha = 0.25f + 0.75f * (i / dots.toFloat())),
                                radius = 3.4.dp.toPx(),
                                center = Offset(cx, cy)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    strings: AppStrings,
    initialEmailHint: String,
    rememberMe: Boolean,
    isSigningIn: Boolean,
    errorMessage: String?,
    onRememberMeChange: (Boolean) -> Unit,
    onGoogleSignInClick: (emailHint: String) -> Unit,
    onSubmitRegistrationAndSignIn: (registrationJson: String, emailHint: String) -> Unit
) {
    var emailOrPhone by remember(initialEmailHint) { mutableStateOf(initialEmailHint) }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var infoHint by remember { mutableStateOf<String?>(null) }
    var showRegisterSheet by remember { mutableStateOf(false) }
    var footerDialog by remember { mutableStateOf<Pair<String, String>?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(CrystalIceBgTop, Color(0xFFF6FAFF), CrystalIceBgBottom)
                )
            )
    ) {
        // Light Nawabshah heritage silhouette behind the header
        Image(
            painter = painterResource(R.drawable.img_nawabshah_heritage),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alpha = 0.14f,
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Brand Emblem + Bilingual Welcome Header (Pak ID style)
                Surface(
                    shape = CircleShape,
                    color = Color(0xFFFAFDFF),
                    border = BorderStroke(1.5.dp, CrystalBorderLight),
                    shadowElevation = 4.dp,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.AcUnit,
                            contentDescription = "Brohi Ice Factory",
                            tint = DeepBlue,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Welcome to Brohi Ice Factory",
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = DeepBlue,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "بروہی آئس فیکٹری میں خوش آمدید",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = RoyalBlue,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))

                // Dual Pill CTA Row inspired by Pak ID: Register (اکاؤنٹ بنائیں) | Login (لاگ ان)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { showRegisterSheet = true },
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.2.dp, CrystalBorderLight),
                        colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White.copy(alpha = 0.94f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .testTag("open_register_sheet_button")
                    ) {
                        BilingualPillLabel(
                            english = "Register",
                            urdu = "اکاؤنٹ",
                            textColor = DeepBlue,
                            urduColor = RoyalBlue
                        )
                    }

                    IcyGlossyButton(
                        onClick = { onGoogleSignInClick(emailOrPhone.trim()) },
                        enabled = !isSigningIn,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                            .testTag("login_primary_button")
                    ) {
                        if (isSigningIn) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            BilingualPillLabel(
                                english = "Login",
                                urdu = "لاگ ان",
                                textColor = Color.White,
                                urduColor = Color(0xFFDCE8FF)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Crystal Glass Login Card
                BrohiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Factory Account Sign In (فیکٹری لاگ ان)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DeepBlue
                        )

                        CrystalTextField(
                            value = emailOrPhone,
                            onValueChange = { emailOrPhone = it },
                            englishLabel = "Email / Phone",
                            urduLabel = "ای میل یا موبائل",
                            leadingIcon = Icons.Outlined.Person,
                            modifier = Modifier.testTag("login_email_input")
                        )

                        CrystalTextField(
                            value = password,
                            onValueChange = { password = it },
                            englishLabel = "Password",
                            urduLabel = "پاس ورڈ",
                            leadingIcon = Icons.Outlined.Lock,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                                        contentDescription = "Toggle password visibility",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            modifier = Modifier.testTag("login_password_input")
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .weight(1f, fill = false)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onRememberMeChange(!rememberMe) }
                                    .padding(end = 6.dp)
                            ) {
                                Checkbox(
                                    checked = rememberMe,
                                    onCheckedChange = onRememberMeChange,
                                    colors = CheckboxDefaults.colors(checkedColor = RoyalBlue)
                                )
                                Text(
                                    text = strings.rememberMe,
                                    fontSize = 12.sp,
                                    color = Color(0xFF1A2F5A),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            TextButton(
                                onClick = {
                                    infoHint = "Passwordless security is managed via Google Sign-In. Approved staff & admins are verified by Super Admin."
                                }
                            ) {
                                Text("Help / مدد", fontSize = 12.sp, color = RoyalBlue, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            }
                        }

                        OutlinedButton(
                            onClick = { onGoogleSignInClick(emailOrPhone.trim()) },
                            enabled = !isSigningIn,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.2.dp, CrystalBorderLight),
                            colors = ButtonDefaults.outlinedButtonColors(containerColor = Color(0xFFFAFDFF)),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .testTag("login_google_button")
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFFF1F6FF),
                                    border = BorderStroke(0.8.dp, CrystalBorderLight.copy(alpha = 0.85f)),
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Canvas(modifier = Modifier.size(16.dp)) {
                                            val stroke = 2.7.dp.toPx()
                                            val arcSize = Size(size.width - stroke, size.height - stroke)
                                            val arcTopLeft = Offset(stroke / 2f, stroke / 2f)
                                            // Red top-left/top
                                            drawArc(
                                                color = Color(0xFFEA4335),
                                                startAngle = -155f,
                                                sweepAngle = 110f,
                                                useCenter = false,
                                                topLeft = arcTopLeft,
                                                size = arcSize,
                                                style = Stroke(width = stroke, cap = StrokeCap.Butt)
                                            )
                                            // Yellow left
                                            drawArc(
                                                color = Color(0xFFFBBC05),
                                                startAngle = 145f,
                                                sweepAngle = 65f,
                                                useCenter = false,
                                                topLeft = arcTopLeft,
                                                size = arcSize,
                                                style = Stroke(width = stroke, cap = StrokeCap.Butt)
                                            )
                                            // Green bottom
                                            drawArc(
                                                color = Color(0xFF34A853),
                                                startAngle = 35f,
                                                sweepAngle = 115f,
                                                useCenter = false,
                                                topLeft = arcTopLeft,
                                                size = arcSize,
                                                style = Stroke(width = stroke, cap = StrokeCap.Butt)
                                            )
                                            // Blue right
                                            drawArc(
                                                color = Color(0xFF4285F4),
                                                startAngle = -15f,
                                                sweepAngle = 55f,
                                                useCenter = false,
                                                topLeft = arcTopLeft,
                                                size = arcSize,
                                                style = Stroke(width = stroke, cap = StrokeCap.Butt)
                                            )
                                            // Horizontal crossbar
                                            drawLine(
                                                color = Color(0xFF4285F4),
                                                start = Offset(center.x, center.y),
                                                end = Offset(size.width - stroke * 0.25f, center.y),
                                                strokeWidth = stroke,
                                                cap = StrokeCap.Round
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "Continue with Google",
                                        color = DeepBlue,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "گوگل لاگ ان",
                                        color = RoyalBlue,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 11.5.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (!errorMessage.isNullOrBlank()) {
                            Text(
                                text = errorMessage,
                                color = DangerRed,
                                fontSize = 12.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (!infoHint.isNullOrBlank()) {
                            Text(
                                text = infoHint!!,
                                color = DeepBlue,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Bottom 4 Pak-ID-inspired small outlined utility icons
            Surface(
                color = Color.White.copy(alpha = 0.85f),
                border = BorderStroke(1.dp, CrystalBorderLight.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WelcomeUtilityItem(
                        icon = Icons.Outlined.MenuBook,
                        english = "Guidelines",
                        urdu = "رہنمائی",
                        onClick = {
                            footerDialog = "Guidelines (رہنمائی)" to
                                "1. New staff tap Register (اکاؤنٹ بنائیں) and fill full details (CNIC, Mobile, WhatsApp, Emergency).\n" +
                                "2. Super Admin or Admin approves your request and assigns role & salary.\n" +
                                "3. Staff members can view My Attendance, My Salary slips, Digital ID Card, and Holiday Notices."
                        },
                        modifier = Modifier.weight(1f)
                    )
                    WelcomeUtilityItem(
                        icon = Icons.Outlined.Info,
                        english = "Factory Info",
                        urdu = "فیکٹری",
                        onClick = {
                            footerDialog = "Brohi Ice Factory (بروہی آئس فیکٹری)" to
                                "Nawabshah / Sindh Industrial Ice Production Plant.\nIce Blocks (10kg) • Crystal Ice Cubes • Cold Storage & Fleet Delivery.\n\nPrepared by SHIPDAR & TECH • 2026"
                        },
                        modifier = Modifier.weight(1f)
                    )
                    WelcomeUtilityItem(
                        icon = Icons.Outlined.EventAvailable,
                        english = "Holidays",
                        urdu = "چھٹیاں",
                        onClick = {
                            footerDialog = "Holiday & Shift Notices (چھٹیاں اور اعلانات)" to
                                "Sign in with your approved account to view real-time factory holiday broadcasts, shift changes, and safety alerts from management."
                        },
                        modifier = Modifier.weight(1f)
                    )
                    WelcomeUtilityItem(
                        icon = Icons.Outlined.SupportAgent,
                        english = "Contact",
                        urdu = "رابطہ",
                        onClick = {
                            footerDialog = "Contact & Support (رابطہ)" to
                                "Factory Support & System Developer:\nSHIPDAR & TECH\nPhone / WhatsApp: 0333-2862498"
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // Registration ModalBottomSheet (Bilingual, Title Case auto-formatted)
    if (showRegisterSheet) {
        var name by remember { mutableStateOf("") }
        var fatherName by remember { mutableStateOf("") }
        var cnicDigits by remember { mutableStateOf("") }
        var phoneDigits by remember { mutableStateOf("") }
        var whatsappDigits by remember { mutableStateOf("") }
        var email by remember { mutableStateOf(emailOrPhone) }
        var emergencyDigits by remember { mutableStateOf("") }
        var requestedRole by remember { mutableStateOf("Staff") }
        var shift by remember { mutableStateOf("Morning (8AM - 4PM)") }
        var showGuidedFaceDialog by remember { mutableStateOf(false) }
        var capturedFaceEncrypted by remember { mutableStateOf<String?>(null) }
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
            onDismissRequest = { showRegisterSheet = false },
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
                        .padding(horizontal = 18.dp, vertical = 14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Staff & Admin Registration",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = "نیا اکاؤنٹ بنائیں • Admin Approval Required",
                                color = Color(0xFFDCE8FF),
                                fontSize = 12.sp
                            )
                        }
                        IconButton(onClick = { showRegisterSheet = false }) {
                            Icon(Icons.Filled.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Personal Identity (ذاتی معلومات)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            CrystalTextField(
                                value = name,
                                onValueChange = { name = it; formError = null },
                                englishLabel = "Full Name *",
                                urduLabel = "پورا نام",
                                leadingIcon = Icons.Outlined.Person,
                                placeholder = "e.g. Ahmed Dirlis"
                            )
                            CrystalTextField(
                                value = fatherName,
                                onValueChange = { fatherName = it },
                                englishLabel = "Father's Name",
                                urduLabel = "والد کا نام",
                                leadingIcon = Icons.Outlined.SupervisorAccount
                            )
                            CnicCrystalTextField(
                                digitsValue = cnicDigits,
                                onDigitsChange = {
                                    cnicDigits = it
                                    formError = null
                                },
                                leadingIcon = Icons.Outlined.Badge,
                                isError = triedSubmit && cnicDigits.isNotEmpty() && !PakistaniFormatters.isValidCnic(cnicDigits)
                            )
                            CrystalTextField(
                                value = email,
                                onValueChange = { email = it },
                                englishLabel = "Email Address",
                                urduLabel = "ای میل",
                                leadingIcon = Icons.Outlined.Email,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Contact Numbers (رابطہ نمبرز)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            PhoneCrystalTextField(
                                digitsValue = phoneDigits,
                                onDigitsChange = { phoneDigits = it; formError = null },
                                englishLabel = "Mobile Number *",
                                urduLabel = "موبائل نمبر",
                                leadingIcon = Icons.Outlined.Phone,
                                isError = triedSubmit && !PakistaniFormatters.isValidPhone(phoneDigits)
                            )
                            PhoneCrystalTextField(
                                digitsValue = whatsappDigits,
                                onDigitsChange = { whatsappDigits = it; formError = null },
                                englishLabel = "WhatsApp Number",
                                urduLabel = "واٹس ایپ",
                                leadingIcon = Icons.Outlined.Chat,
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
                                leadingIcon = Icons.Outlined.ContactPhone,
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
                    }

                    // Biometric Camera Face Registration Section (Mandatory so only original employee can mark attendance)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
                        border = BorderStroke(
                            1.dp,
                            if (capturedFaceEncrypted != null) SuccessGreen.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = if (capturedFaceEncrypted != null) Icons.Filled.CheckCircle else Icons.Filled.Face,
                                        contentDescription = null,
                                        tint = if (capturedFaceEncrypted != null) SuccessGreen else MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Column {
                                        Text(
                                            "Face Biometrics (چہرہ رجسٹریشن) *",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (capturedFaceEncrypted != null) SuccessGreen else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            "حاضری کے لیے لازمی • صرف اصل بندہ ہی حاضری لگا سکے گا",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                if (capturedFaceEncrypted != null) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = SuccessGreen.copy(alpha = 0.15f)
                                    ) {
                                        Text(
                                            text = "REGISTERED",
                                            color = SuccessGreen,
                                            fontWeight = FontWeight.ExtraBold,
                                            fontSize = 10.sp,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                        )
                                    }
                                }
                            }

                            if (capturedFaceEncrypted != null) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = SuccessGreen.copy(alpha = 0.08f),
                                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.35f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                "✓ Face Encrypted & Saved (چہرہ محفوظ ہو گیا)",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                color = Color(0xFF0B5D29)
                                            )
                                            Text(
                                                "Only you will be able to mark attendance. Proxy check-in blocked.",
                                                fontSize = 11.sp,
                                                color = Color(0xFF166534),
                                                lineHeight = 14.sp
                                            )
                                        }
                                        TextButton(
                                            onClick = {
                                                capturedFaceEncrypted = null
                                                showGuidedFaceDialog = true
                                            }
                                        ) {
                                            Text("Re-scan", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            } else {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFFEAF2FF),
                                    border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.25f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = "4-Angle Face Scan (Front • Left • Right • Tilt up): کیمرہ سے چہرہ اسکین کروائیں تاکہ بعد میں صرف اصل بندہ ہی حاضری لگا سکے۔ اگر 2 دن میں چہرہ اسکین نہ ہو تو اکاؤنٹ دوبارہ بلاک ہو جائے گا۔",
                                            fontSize = 11.sp,
                                            color = DeepBlue,
                                            lineHeight = 16.sp
                                        )
                                        IcyGlossyButton(
                                            onClick = { showGuidedFaceDialog = true },
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(48.dp)
                                                .testTag("open_registration_camera_button")
                                        ) {
                                            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(17.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Start 4-Pose Face Scan (چہرہ اسکین کریں)",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.34f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Select Account Role (عہدہ منتخب کریں)", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                            ModernRoleSelectorCardGrid(
                                selectedRole = requestedRole,
                                roles = REGISTRATION_ROLE_OPTIONS,
                                onSelectRole = { requestedRole = it }
                            )
                            if (requestedRole == "Super Admin") {
                                Text(
                                    text = "First factory registration activates Primary Super Admin. Additional Super Admin requests wait for existing Super Admin approval.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary,
                                    lineHeight = 15.sp
                                )
                            }
                            Text(
                                text = "Duty Shift / Timing (شفٹ یا بغیر وقت کے 24 گھنٹے)",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            ModernShiftSelector(
                                selectedShift = shift,
                                shifts = WORK_SHIFT_OPTIONS,
                                onSelectShift = { shift = it }
                            )
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showRegisterSheet = false },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(0.35f).height(48.dp)
                        ) {
                            Text("Cancel", maxLines = 1, softWrap = false)
                        }
                        IcyGlossyButton(
                            onClick = {
                                triedSubmit = true
                                if (name.isBlank()) {
                                    formError = "Please enter Full Name (پورا نام لکھیں)"
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
                                if (capturedFaceEncrypted.isNullOrBlank()) {
                                    formError = "Please scan your face with camera first. (براہ کرم پہلے کیمرہ سے چہرہ اسکین کریں تاکہ بعد میں صرف آپ ہی حاضری لگا سکیں۔)"
                                    popupTitle = "Face Registration Required (چہرہ اسکین لازمی ہے)"
                                    popupMessage = "Biometric 4-pose face registration is mandatory so that proxy attendance is strictly prevented and only the original person can mark their attendance.\n\nرجسٹریشن کے لیے 4 زاویوں سے چہرہ اسکین کرنا لازمی ہے تاکہ صرف اصل ملازم ہی حاضری لگا سکے۔"
                                    showGuidedFaceDialog = true
                                    return@IcyGlossyButton
                                }
                                val payload = JSONObject().apply {
                                    put("name", PakistaniFormatters.formatTitleCase(name))
                                    put("fatherName", PakistaniFormatters.formatTitleCase(fatherName))
                                    put("cnic", PakistaniFormatters.formatCnic(cnicDigits))
                                    put("phone", PakistaniFormatters.formatPhone(phoneDigits))
                                    put("whatsappNumber", PakistaniFormatters.formatPhone(whatsappDigits))
                                    put("email", email.trim())
                                    put("emergencyContact", PakistaniFormatters.formatPhone(emergencyDigits))
                                    put("requestedRole", requestedRole)
                                    put("shift", shift)
                                    put("faceTemplateEncrypted", capturedFaceEncrypted)
                                }.toString()
                                showRegisterSheet = false
                                onSubmitRegistrationAndSignIn(payload, email.trim())
                            },
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                            modifier = Modifier
                                .weight(0.65f)
                                .height(48.dp)
                                .testTag("submit_registration_button")
                        ) {
                            Icon(Icons.Filled.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Submit & Verify Google",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                softWrap = false
                            )
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

        if (showGuidedFaceDialog) {
            GuidedFaceEnrollmentDialog(
                ownerKey = "global",
                onDismiss = { showGuidedFaceDialog = false },
                onCompleted = { encrypted ->
                    capturedFaceEncrypted = encrypted
                    showGuidedFaceDialog = false
                    formError = null
                }
            )
        }
    }

    footerDialog?.let { (title, body) ->
        CrystalFormDialog(
            onDismissRequest = { footerDialog = null },
            title = title,
            subtitle = "فیکٹری معلومات و رہنمائی",
            icon = Icons.Outlined.Info,
            confirmText = "Close (بند کریں)",
            onConfirm = { footerDialog = null },
            showDismissButton = false
        ) {
            Text(
                text = body,
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun WelcomeUtilityItem(
    icon: ImageVector,
    english: String,
    urdu: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 6.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = english,
            tint = DeepBlue,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text = english,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = DeepBlue,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = urdu,
            fontSize = 10.sp,
            color = RoyalBlue,
            maxLines = 1,
            softWrap = false
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WaitingForApprovalGateScreen(
    darkTheme: Boolean,
    approvalStatus: String,
    userName: String,
    userEmail: String?,
    profile: UserProfile?,
    myRequest: RegistrationRequest?,
    onRefreshStatus: () -> Unit,
    onSubmitUpdatedRegistration: (
        fullName: String,
        fatherName: String,
        cnic: String,
        phone: String,
        whatsapp: String,
        emergency: String,
        requestedRole: String,
        shift: String,
        faceTemplateEncrypted: String?
    ) -> Unit,
    onLogoutClick: () -> Unit
) {
    val isRejected = approvalStatus.equals("REJECTED", ignoreCase = true)
    val isAutoBlocked = myRequest?.reviewNote?.contains("Auto-blocked", ignoreCase = true) == true
    val roleLabel = myRequest?.requestedRole?.ifBlank { profile?.requestedRole }
        ?: profile?.requestedRole?.ifBlank { "Staff" }
        ?: "Staff"
    val accentColor = if (isRejected || isAutoBlocked) DangerRed else WarningOrange
    var showWaitingPopup by remember(approvalStatus) { mutableStateOf(true) }
    var showEditRegSheet by remember { mutableStateOf(false) }

    CrystalBackground(darkTheme = darkTheme) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BrohiCard(
                darkTheme = darkTheme,
                modifier = Modifier.widthIn(max = 520.dp).fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(accentColor.copy(alpha = 0.14f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRejected || isAutoBlocked) Icons.Filled.Block else Icons.Filled.HourglassTop,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(38.dp)
                        )
                    }

                    Text(
                        text = when {
                            isAutoBlocked -> "Account Auto-Blocked (Face Required)"
                            isRejected -> "Registration Declined"
                            else -> "Waiting for Approval"
                        },
                        fontSize = 21.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = when {
                            isAutoBlocked -> "2 دن میں چہرہ اسکین نہ ہونے کی وجہ سے اکاؤنٹ بلاک کر دیا گیا ہے"
                            isRejected -> "آپ کی درخواست مسترد کر دی گئی ہے"
                            else -> "سپر ایڈمن کی منظوری کا انتظار کریں"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentColor,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = when {
                            isAutoBlocked -> "Face enrollment was not completed within 2 days. Please scan your 4-angle face below and resubmit to Super Admin & Admin for approval."
                            isRejected -> myRequest?.reviewNote?.takeIf { it.isNotBlank() }
                                ?: "Your account request was declined. You may update your registration details and resubmit for approval."
                            else -> "Your account has been registered. For security, only Super Admin can approve access before the dashboard opens. You will enter automatically once approved."
                        },
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val displayName = PakistaniFormatters.formatTitleCase(
                                myRequest?.name?.ifBlank { profile?.displayName }
                                    ?: profile?.displayName?.ifBlank { userName }
                                    ?: userName
                            )
                            Text("Applicant: $displayName", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text("Requested Role: $roleLabel", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                            if (!userEmail.isNullOrBlank()) {
                                Text("Account: $userEmail", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Text(
                                text = "Status: ${if (isRejected) "REJECTED" else "PENDING APPROVAL"}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentColor
                            )
                        }
                    }

                    IcyGlossyButton(
                        onClick = onRefreshStatus,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("gate_refresh_approval_button")
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Check Approval Status (منظوری چیک کریں)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    OutlinedButton(
                        onClick = { showEditRegSheet = true },
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("gate_complete_reg_button")
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (myRequest == null) "Complete Registration Form (فارم مکمل کریں)" else "Update Registration Details (تفصیلات تبدیل کریں)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TextButton(
                        onClick = onLogoutClick,
                        modifier = Modifier.testTag("gate_logout_button")
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null, tint = DangerRed, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sign Out / Switch Account (لاگ آؤٹ)", color = DangerRed, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }
                }
            }
        }
    }

    if (showWaitingPopup) {
        CrystalFormDialog(
            onDismissRequest = { showWaitingPopup = false },
            title = if (isRejected) "Approval Declined" else "Waiting for Approval",
            subtitle = if (isRejected) "منظوری مسترد" else "منظوری کا انتظار",
            icon = if (isRejected) Icons.Filled.Block else Icons.Filled.HourglassTop,
            accentColor = accentColor,
            isDestructive = isRejected,
            confirmText = "Check Now (چیک کریں)",
            onConfirm = {
                showWaitingPopup = false
                onRefreshStatus()
            },
            dismissText = "Close (بند کریں)",
            confirmTestTag = "waiting_popup_refresh"
        ) {
            Text(
                text = if (isRejected) {
                    "Your account request for $roleLabel was declined.\n\nآپ کی درخواست مسترد کی گئی ہے۔ تفصیلات درست کر کے دوبارہ جمع کروائیں۔"
                } else {
                    "Your account ($roleLabel) is pending approval by Super Admin. Once Super Admin approves your account, you will automatically enter your dashboard.\n\nآپ کا اکاؤنٹ سپر ایڈمن کی منظوری کے لیے بھیج دیا گیا ہے۔ منظوری ملنے کے بعد آپ ایپ استعمال کر سکیں گے۔"
                },
                fontSize = 13.5.sp,
                lineHeight = 20.sp,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }

    if (showEditRegSheet) {
        var name by remember { mutableStateOf(myRequest?.name ?: profile?.displayName ?: userName) }
        var fatherName by remember { mutableStateOf(myRequest?.fatherName ?: "") }
        var cnicDigits by remember { mutableStateOf(PakistaniFormatters.normalizeCnicDigits(myRequest?.cnic ?: "")) }
        var phoneDigits by remember { mutableStateOf(PakistaniFormatters.normalizePhoneDigits(myRequest?.phone ?: profile?.phone ?: "")) }
        var whatsappDigits by remember { mutableStateOf(PakistaniFormatters.normalizePhoneDigits(myRequest?.whatsappNumber ?: "")) }
        var emergencyDigits by remember { mutableStateOf(PakistaniFormatters.normalizePhoneDigits(myRequest?.emergencyContact ?: "")) }
        var roleChoice by remember { mutableStateOf(roleLabel) }
        var shiftChoice by remember { mutableStateOf(myRequest?.shift ?: WORK_SHIFT_OPTIONS.first()) }
        var gateFaceEncrypted by remember { mutableStateOf(myRequest?.faceTemplateEncrypted) }
        var showGateFaceDialog by remember { mutableStateOf(false) }
        var gateFormError by remember { mutableStateOf<String?>(null) }
        var triedSubmit by remember { mutableStateOf(false) }

        val emergencySame = PakistaniFormatters.isEmergencySameAsPrimary(
            emergencyRaw = emergencyDigits,
            mobileRaw = phoneDigits,
            whatsappRaw = whatsappDigits
        )

        ModalBottomSheet(
            onDismissRequest = { showEditRegSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Registration Details (رجسٹریشن تفصیلات)",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                CrystalTextField(
                    value = name,
                    onValueChange = { name = it; gateFormError = null },
                    englishLabel = "Full Name *",
                    urduLabel = "پورا نام",
                    leadingIcon = Icons.Outlined.Person
                )
                CrystalTextField(
                    value = fatherName,
                    onValueChange = { fatherName = it },
                    englishLabel = "Father's Name",
                    urduLabel = "والد کا نام",
                    leadingIcon = Icons.Outlined.SupervisorAccount
                )
                CnicCrystalTextField(
                    digitsValue = cnicDigits,
                    onDigitsChange = { cnicDigits = it; gateFormError = null },
                    isError = triedSubmit && cnicDigits.isNotEmpty() && !PakistaniFormatters.isValidCnic(cnicDigits)
                )
                PhoneCrystalTextField(
                    digitsValue = phoneDigits,
                    onDigitsChange = { phoneDigits = it; gateFormError = null },
                    englishLabel = "Mobile Number *",
                    urduLabel = "موبائل نمبر",
                    isError = triedSubmit && !PakistaniFormatters.isValidPhone(phoneDigits)
                )
                PhoneCrystalTextField(
                    digitsValue = whatsappDigits,
                    onDigitsChange = { whatsappDigits = it; gateFormError = null },
                    englishLabel = "WhatsApp Number",
                    urduLabel = "واٹس ایپ",
                    leadingIcon = Icons.Outlined.Chat,
                    isError = triedSubmit && whatsappDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(whatsappDigits),
                    actionChip = {
                        if (phoneDigits.isNotEmpty() && whatsappDigits != phoneDigits) {
                            TextButton(onClick = { whatsappDigits = phoneDigits }) {
                                Text("Same", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                )
                PhoneCrystalTextField(
                    digitsValue = emergencyDigits,
                    onDigitsChange = { emergencyDigits = it; gateFormError = null },
                    englishLabel = "Emergency Contact",
                    urduLabel = "ہنگامی نمبر",
                    leadingIcon = Icons.Outlined.ContactPhone,
                    isError = emergencySame || (triedSubmit && emergencyDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(emergencyDigits))
                )
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (!gateFaceEncrypted.isNullOrBlank()) SuccessGreen.copy(alpha = 0.08f) else Color(0xFFEAF2FF),
                    border = BorderStroke(
                        1.dp,
                        if (!gateFaceEncrypted.isNullOrBlank()) SuccessGreen.copy(alpha = 0.4f) else RoyalBlue.copy(alpha = 0.3f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (!gateFaceEncrypted.isNullOrBlank()) {
                                    "✓ 4-Pose Face Biometrics Saved"
                                } else {
                                    "Face Biometrics Required *"
                                },
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = if (!gateFaceEncrypted.isNullOrBlank()) Color(0xFF065F46) else DeepBlue
                            )
                            Text(
                                text = "Mandatory so only you can mark attendance",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IcyGlossyButton(
                            onClick = { showGateFaceDialog = true },
                            tone = if (!gateFaceEncrypted.isNullOrBlank()) IcyGlossyTone.GREEN else IcyGlossyTone.BLUE,
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (!gateFaceEncrypted.isNullOrBlank()) "Re-scan" else "Scan Face",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Text("Select Role (عہدہ منتخب کریں)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ModernRoleSelectorCardGrid(
                    selectedRole = roleChoice,
                    roles = REGISTRATION_ROLE_OPTIONS,
                    onSelectRole = { roleChoice = it }
                )
                Text("Duty Shift / Timing (شفٹ منتخب کریں)", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                ModernShiftSelector(
                    selectedShift = shiftChoice,
                    shifts = WORK_SHIFT_OPTIONS,
                    onSelectShift = { shiftChoice = it }
                )
                if (!gateFormError.isNullOrBlank()) {
                    Text(gateFormError!!, color = DangerRed, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                IcyGlossyButton(
                    onClick = {
                        triedSubmit = true
                        when {
                            name.isBlank() -> gateFormError = "Full name is required."
                            cnicDigits.isNotEmpty() && !PakistaniFormatters.isValidCnic(cnicDigits) ->
                                gateFormError = "CNIC number must be 13 digits."
                            !PakistaniFormatters.isValidPhone(phoneDigits) ->
                                gateFormError = "Mobile number must be complete (11 digits)."
                            whatsappDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(whatsappDigits) ->
                                gateFormError = "WhatsApp number must be complete (11 digits)."
                            emergencyDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(emergencyDigits) ->
                                gateFormError = "Emergency contact must be complete (11 digits)."
                            emergencySame ->
                                gateFormError = "Emergency contact must be different from Mobile / WhatsApp."
                            gateFaceEncrypted.isNullOrBlank() -> {
                                gateFormError = "Please scan your 4-angle face first."
                                showGateFaceDialog = true
                            }
                            else -> {
                                showEditRegSheet = false
                                showWaitingPopup = true
                                onSubmitUpdatedRegistration(
                                    name,
                                    fatherName,
                                    cnicDigits,
                                    phoneDigits,
                                    whatsappDigits,
                                    emergencyDigits,
                                    roleChoice,
                                    shiftChoice,
                                    gateFaceEncrypted
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Submit for Approval (منظوری کے لیے بھیجیں)", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }

        if (showGateFaceDialog) {
            GuidedFaceEnrollmentDialog(
                ownerKey = "global",
                onDismiss = { showGateFaceDialog = false },
                onCompleted = { encrypted ->
                    gateFaceEncrypted = encrypted
                    showGateFaceDialog = false
                    gateFormError = null
                }
            )
        }
    }
}

