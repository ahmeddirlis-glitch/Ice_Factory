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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
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
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SoftBorderDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.DaySalesBar

val PrimaryBlueGradient = Brush.horizontalGradient(
    colors = listOf(DeepBlue, RoyalBlue)
)

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
    val fourthRoute = if (activeRole == UserRole.STAFF) AppRoute.Salary else AppRoute.Sales
    val fourthLabel = if (activeRole == UserRole.STAFF) "Salary" else strings.sales
    val fourthIcon = if (activeRole == UserRole.STAFF) Icons.Outlined.Payments else Icons.Outlined.ShoppingCart
    val fourthTag = if (activeRole == UserRole.STAFF) "bottom_nav_salary" else "bottom_nav_sales"

    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        shadowElevation = 10.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .height(66.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
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

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .offset(y = (-6).dp)
                            .size(54.dp)
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(PrimaryBlueGradient)
                            .border(2.dp, Color.White, CircleShape)
                            .clickable(onClick = onMicClick)
                            .testTag("bottom_nav_mic"),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Mic,
                            contentDescription = "Voice Assistant Command",
                            tint = Color.White,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                }

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
    val color = if (selected) RoyalBlue else MaterialTheme.colorScheme.onSurfaceVariant
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(vertical = 4.dp, horizontal = 2.dp)
            .clip(RoundedCornerShape(14.dp))
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
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
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
    val shape = RoundedCornerShape(18.dp)
    val surfaceColor = if (darkTheme) CrystalSurfaceDark else CrystalSurfaceLight
    val borderColor = if (darkTheme) SoftBorderDark else CrystalBorderLight.copy(alpha = 0.85f)
    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier,
            shape = shape,
            colors = CardDefaults.cardColors(containerColor = surfaceColor),
            border = BorderStroke(1.dp, borderColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            content()
        }
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

