package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.HourglassTop
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.ContactPhone
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.EventAvailable
import androidx.compose.material.icons.outlined.FaceRetouchingNatural
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LocalShipping
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Phone
import androidx.compose.material.icons.outlined.PrecisionManufacturing
import androidx.compose.material.icons.outlined.QrCode2
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.outlined.SupervisorAccount
import androidx.compose.material.icons.outlined.SystemUpdate
import androidx.compose.material.icons.outlined.TrendingUp
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AppReleaseConfig
import com.example.data.model.AttendanceRecord
import com.example.data.model.Employee
import com.example.data.model.FactoryNotice
import com.example.data.model.FactorySettings
import com.example.data.model.OwnerAccountingSummary
import com.example.data.model.PayrollRecord
import com.example.data.model.ProfileUpdateRequest
import com.example.data.model.REGISTRATION_ROLE_OPTIONS
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.WORK_SHIFT_OPTIONS
import com.example.data.util.AppStrings
import com.example.data.util.ImageAndQrUtils
import com.example.data.util.PakistaniFormatters
import com.example.ui.components.BrohiCard
import com.example.ui.components.CnicCrystalTextField
import com.example.ui.components.CrystalFormDialog
import com.example.ui.components.CrystalFullPhotoViewerDialog
import com.example.ui.components.CrystalTextField
import com.example.ui.components.IcyGlossyButton
import com.example.ui.components.ModernRoleSelectorCardGrid
import com.example.ui.components.ModernShiftSelector
import com.example.ui.components.PhoneCrystalTextField
import com.example.ui.components.PrimaryBlueGradient
import com.example.ui.components.SalesTrendBarChart
import com.example.ui.components.UserAvatarImage
import com.example.ui.components.ValidationPopupDialog
import com.example.ui.components.rememberCameraPhotoLauncher
import com.example.ui.navigation.AppRoute
import com.example.ui.theme.AccentPurple
import com.example.ui.theme.CrystalBorderLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.WarningOrange
import com.example.ui.viewmodel.DashboardMetrics
import kotlin.math.roundToInt

private data class BilingualTile(
    val english: String,
    val urdu: String,
    val icon: ImageVector,
    val route: AppRoute? = null,
    val onSpecialClick: (() -> Unit)? = null
)

@Composable
fun DashboardScreen(
    userName: String,
    photoUrl: String?,
    activeRole: UserRole,
    userProfile: UserProfile?,
    primaryEmployee: Employee?,
    metrics: DashboardMetrics,
    accountingSummary: OwnerAccountingSummary = OwnerAccountingSummary(),
    notices: List<FactoryNotice>,
    myAttendance: List<AttendanceRecord>,
    myPayroll: List<PayrollRecord>,
    pendingApprovalsCount: Int,
    isUpdateAvailable: Boolean = false,
    appRelease: AppReleaseConfig? = null,
    factorySettings: FactorySettings = FactorySettings(),
    strings: AppStrings,
    onNavigate: (AppRoute) -> Unit,
    onProfileClick: () -> Unit,
    onOpenVoice: () -> Unit = {},
    onOpenUpdateDialog: () -> Unit = {},
    onUpdateIdCardPhoto: (employeeId: String, manualPhotoDataUri: String?) -> Unit = { _, _ -> },
    onOpenFaceEnrollClick: () -> Unit = {}
) {
    var showDigitalIdModal by remember { mutableStateOf(false) }
    val cleanName = PakistaniFormatters.formatTitleCase(userName.ifBlank { "Ahmed Khan" })
    val activeNotices = remember(notices) { notices.filter { it.active } }
    val latestNotice = activeNotices.firstOrNull()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Role badge & update notification
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${strings.welcomeBack}\n$cleanName",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 25.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val roleSubtitle = when (activeRole) {
                        UserRole.OWNER -> "Super Admin • Full Control"
                        UserRole.ADMIN -> "Admin • Operations & Billing"
                        UserRole.MANAGER -> "Manager • Plant & Logistics"
                        UserRole.STAFF -> "Staff • Self-Service Portal"
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = roleSubtitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }
            UserAvatarImage(
                photoUrl = photoUrl,
                contentDescription = "Profile photo",
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                    .clickable(onClick = onProfileClick)
            )
        }

        // Cross-Role App Update Notification Banner (Visible to Super Admin, Admin, Manager & Staff)
        if (isUpdateAvailable && appRelease != null) {
            Surface(
                onClick = onOpenUpdateDialog,
                shape = RoundedCornerShape(16.dp),
                color = lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primaryContainer, 0.72f),
                border = BorderStroke(1.2.dp, RoyalBlue.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_update_banner")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoyalBlue,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Outlined.SystemUpdate,
                                contentDescription = "Updates Available",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Updates Available • v${appRelease.latestVersionName}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = appRelease.releaseNotesUrdu.ifBlank {
                                appRelease.releaseNotes.ifBlank { "نئی اپڈیٹ دستیاب ہے — انسٹال کرنے کے لیے ٹیپ کریں" }
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SuccessGreen
                    ) {
                        Text(
                            text = "Update",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }

        // Pak-ID Inspired Digital Factory ID Banner
        Surface(
            onClick = { showDigitalIdModal = true },
            shape = RoundedCornerShape(16.dp),
            color = lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.primary, 0.11f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("digital_id_banner")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = DeepBlue,
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.QrCode2,
                            contentDescription = "Digital ID Card",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Digital ID Card",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap to view your verified Factory Employee ID",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "ڈیجیٹل آئی ڈی",
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Pending Staff Registrations Banner (Super Admin / Admin)
        if (activeRole.canManageStaff && pendingApprovalsCount > 0) {
            Surface(
                onClick = { onNavigate(AppRoute.UsersRoles) },
                shape = RoundedCornerShape(14.dp),
                color = lerp(MaterialTheme.colorScheme.surface, WarningOrange, 0.13f),
                border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("pending_approvals_banner")
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(Icons.Outlined.Group, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "$pendingApprovalsCount Pending Registration Request(s)",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = WarningOrange
                        )
                        Text(
                            text = "نئی درخواستیں منظوری کے لیے موجود ہیں • Tap to Review",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }

        // Active Broadcast Holiday / Shift Notice Banner
        if (latestNotice != null) {
            val isHoliday = latestNotice.noticeType.equals("Holiday", ignoreCase = true) ||
                latestNotice.noticeType.equals("Urgent", ignoreCase = true)
            val noticeAccent = if (isHoliday) WarningOrange else RoyalBlue
            Surface(
                onClick = { onNavigate(AppRoute.Notices) },
                shape = RoundedCornerShape(14.dp),
                color = lerp(MaterialTheme.colorScheme.surface, noticeAccent, 0.11f),
                border = BorderStroke(1.dp, noticeAccent.copy(alpha = 0.35f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_notice_banner")
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Campaign,
                        contentDescription = "Factory Notice",
                        tint = noticeAccent,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${latestNotice.noticeType}: ${latestNotice.title}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "اعلانات",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = noticeAccent
                            )
                        }
                        Text(
                            text = latestNotice.messageUrdu.ifBlank { latestNotice.message },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // STRICT ROLE-SPECIFIC INTERFACES (Distinct Dashboard per Role)
        when (activeRole) {
            UserRole.STAFF -> {
                val todayIso = PakistaniFormatters.todayIso()
                val todayMine = myAttendance.firstOrNull { it.date == todayIso }
                val dueSalary = myPayroll.filter { !it.paid }.sumOf { it.netPay }
                val mySalText = if (myPayroll.isNotEmpty()) {
                    PakistaniFormatters.formatPkr(if (dueSalary > 0) dueSalary else (primaryEmployee?.salary ?: 0.0))
                } else {
                    PakistaniFormatters.formatPkr(primaryEmployee?.salary ?: 35000.0)
                }

                // Assigned Duty Shift Card
                BrohiCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(RoyalBlue.copy(alpha = 0.14f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Schedule, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(22.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("My Assigned Shift (میری ڈیوٹی شفٹ)", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                text = primaryEmployee?.shift?.ifBlank { "Morning (8AM - 4PM)" } ?: "Morning (8AM - 4PM)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardKpiCard(
                        title = "Today Attendance (آج حاضری)",
                        value = todayMine?.status ?: "Not Checked In",
                        icon = Icons.Outlined.EventAvailable,
                        iconBg = RoyalBlue,
                        softBg = RoyalBlue.copy(alpha = 0.14f),
                        onClick = { onNavigate(AppRoute.Attendance) },
                        modifier = Modifier.weight(1f).testTag("staff_kpi_attendance")
                    )
                    DashboardKpiCard(
                        title = if (dueSalary > 0) "Due Salary (واجب الادا)" else "My Salary (تنخواہ)",
                        value = mySalText,
                        icon = Icons.Outlined.Payments,
                        iconBg = SuccessGreen,
                        softBg = SuccessGreen.copy(alpha = 0.14f),
                        onClick = { onNavigate(AppRoute.Salary) },
                        modifier = Modifier.weight(1f).testTag("staff_kpi_salary")
                    )
                }

                BrohiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Staff Self-Service", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("اسٹاف پورٹل", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

                        val staffTiles = listOf(
                            BilingualTile("My Attendance", "میری حاضری", Icons.Outlined.EventAvailable, AppRoute.Attendance),
                            BilingualTile("My Salary", "میری تنخواہ", Icons.Outlined.Payments, AppRoute.Salary),
                            BilingualTile("Notices", "اعلانات و چھٹیاں", Icons.Outlined.Campaign, AppRoute.Notices),
                            BilingualTile("My Profile", "میرا پروفائل", Icons.Outlined.Person, AppRoute.Profile),
                            BilingualTile("Safety Report", "شکایت درج کریں", Icons.Outlined.WarningAmber, AppRoute.Incidents),
                            BilingualTile("App Updates", "ایپ اپڈیٹس", Icons.Outlined.Settings, AppRoute.Settings)
                        )
                        QuickActionGridRows(tiles = staffTiles, onNavigate = onNavigate, tagPrefix = "staff_tile")
                    }
                }
            }

            UserRole.MANAGER -> {
                // Plant & Logistics Focused Dashboard (No Revenue/Profit/Salary/Approvals)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardKpiCard(
                        title = "Today Production (آج پیداوار)",
                        value = PakistaniFormatters.formatTons(metrics.todayProductionTons),
                        icon = Icons.Outlined.PrecisionManufacturing,
                        iconBg = RoyalBlue,
                        softBg = RoyalBlue.copy(alpha = 0.14f),
                        onClick = { onNavigate(AppRoute.Production) },
                        modifier = Modifier.weight(1f).testTag("manager_kpi_production")
                    )
                    DashboardKpiCard(
                        title = strings.stock,
                        value = PakistaniFormatters.formatTons(metrics.totalStockTons),
                        icon = Icons.Outlined.Inventory2,
                        iconBg = WarningOrange,
                        softBg = WarningOrange.copy(alpha = 0.14f),
                        onClick = { onNavigate(AppRoute.Inventory) },
                        modifier = Modifier.weight(1f).testTag("kpi_stock")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardKpiCard(
                        title = strings.staffInside,
                        value = "${metrics.staffPresentCount} / ${metrics.totalStaffCount}",
                        icon = Icons.Outlined.Group,
                        iconBg = AccentPurple,
                        softBg = AccentPurple.copy(alpha = 0.14f),
                        onClick = { onNavigate(AppRoute.Attendance) },
                        modifier = Modifier.weight(1f).testTag("kpi_staff_inside")
                    )
                    DashboardKpiCard(
                        title = "Blocks / Cubes Mix",
                        value = "${metrics.blocksPercent.roundToInt()}% / ${metrics.cubesPercent.roundToInt()}%",
                        icon = Icons.Outlined.LocalShipping,
                        iconBg = SuccessGreen,
                        softBg = SuccessGreen.copy(alpha = 0.14f),
                        onClick = { onNavigate(AppRoute.Deliveries) },
                        modifier = Modifier.weight(1f).testTag("manager_kpi_mix")
                    )
                }

                BrohiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Plant & Fleet Console", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("پلانٹ آپریشنز", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

                        val managerTiles = listOf(
                            BilingualTile("Production", "پیداوار", Icons.Outlined.PrecisionManufacturing, AppRoute.Production),
                            BilingualTile("Inventory", "کولڈ اسٹوریج", Icons.Outlined.Inventory2, AppRoute.Inventory),
                            BilingualTile("Deliveries", "ڈلیوری", Icons.Outlined.LocalShipping, AppRoute.Deliveries),
                            BilingualTile("Utilities", "بجلی و ڈیزل", Icons.Outlined.TrendingUp, AppRoute.Utilities),
                            BilingualTile("Maintenance", "مشین مرمت", Icons.Outlined.Build, AppRoute.Maintenance),
                            BilingualTile("Attendance", "حاضری", Icons.Outlined.Schedule, AppRoute.Attendance),
                            BilingualTile("Notices", "اعلانات", Icons.Outlined.Campaign, AppRoute.Notices),
                            BilingualTile("Voice Helper", "وائس مدد", Icons.Outlined.Mic, onSpecialClick = onOpenVoice),
                            BilingualTile("App Updates", "ایپ اپڈیٹس", Icons.Outlined.Settings, AppRoute.Settings)
                        )
                        QuickActionGridRows(tiles = managerTiles, onNavigate = onNavigate, tagPrefix = "quick_action")
                    }
                }
            }

            UserRole.ADMIN -> {
                // Admin Daily Sales, Expenses, Customers & Approvals Dashboard (No Owner Profit/Break-Even/Salary/Reports)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardKpiCard(
                        title = strings.todaySales,
                        value = PakistaniFormatters.formatPkr(metrics.todaySales),
                        icon = Icons.Outlined.ShoppingCart,
                        iconBg = RoyalBlue,
                        softBg = RoyalBlue.copy(alpha = 0.14f),
                        onClick = { onNavigate(AppRoute.Sales) },
                        modifier = Modifier.weight(1f).testTag("kpi_today_sales")
                    )
                    DashboardKpiCard(
                        title = strings.todayExpenses,
                        value = PakistaniFormatters.formatPkr(metrics.todayExpenses),
                        icon = Icons.Outlined.ReceiptLong,
                        iconBg = DangerRed,
                        softBg = DangerRed.copy(alpha = 0.14f),
                        onClick = { onNavigate(AppRoute.Expenses) },
                        modifier = Modifier.weight(1f).testTag("kpi_today_expenses")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardKpiCard(
                        title = strings.staffInside,
                        value = "${metrics.staffPresentCount} / ${metrics.totalStaffCount}",
                        icon = Icons.Outlined.Group,
                        iconBg = AccentPurple,
                        softBg = AccentPurple.copy(alpha = 0.14f),
                        onClick = { onNavigate(AppRoute.Attendance) },
                        modifier = Modifier.weight(1f).testTag("kpi_staff_inside")
                    )
                    DashboardKpiCard(
                        title = strings.stock,
                        value = PakistaniFormatters.formatTons(metrics.totalStockTons),
                        icon = Icons.Outlined.Inventory2,
                        iconBg = WarningOrange,
                        softBg = WarningOrange.copy(alpha = 0.14f),
                        onClick = { onNavigate(AppRoute.Inventory) },
                        modifier = Modifier.weight(1f).testTag("kpi_stock")
                    )
                }

                BrohiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(strings.salesTrend7Days, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                            val pctText = if (metrics.salesTrendPercentChange >= 0) "↑ ${metrics.salesTrendPercentChange}%" else "↓ ${kotlin.math.abs(metrics.salesTrendPercentChange)}%"
                            Text(pctText, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = SuccessGreen)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        SalesTrendBarChart(bars = metrics.salesTrend7Days)
                    }
                }

                BrohiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Admin Operations", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurface)
                            Text("ایڈمن پینل", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary)
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

                        val adminTiles = listOf(
                            BilingualTile("Sales", "فروخت", Icons.Outlined.ShoppingCart, AppRoute.Sales),
                            BilingualTile("Expenses", "اخراجات", Icons.Outlined.ReceiptLong, AppRoute.Expenses),
                            BilingualTile("Customers", "گاہک", Icons.Outlined.Storefront, AppRoute.Customers),
                            BilingualTile("Inventory", "اسٹاک", Icons.Outlined.Inventory2, AppRoute.Inventory),
                            BilingualTile("Production", "پیداوار", Icons.Outlined.PrecisionManufacturing, AppRoute.Production),
                            BilingualTile("Attendance", "حاضری", Icons.Outlined.Schedule, AppRoute.Attendance),
                            BilingualTile("Approvals", "منظوری", Icons.Outlined.Group, AppRoute.UsersRoles),
                            BilingualTile("Notices", "اعلانات", Icons.Outlined.Campaign, AppRoute.Notices),
                            BilingualTile("App Updates", "ایپ اپڈیٹس", Icons.Outlined.Settings, AppRoute.Settings)
                        )
                        QuickActionGridRows(tiles = adminTiles, onNavigate = onNavigate, tagPrefix = "quick_action")
                    }
                }
            }

            UserRole.OWNER -> {
                // Super Admin Executive Command Center
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardKpiCard(
                        title = strings.todaySales,
                        value = PakistaniFormatters.formatPkr(metrics.todaySales),
                        icon = Icons.Outlined.ShoppingCart,
                        iconBg = RoyalBlue,
                        softBg = RoyalBlue.copy(alpha = 0.14f),
                        onClick = { onNavigate(AppRoute.Sales) },
                        modifier = Modifier.weight(1f).testTag("kpi_today_sales")
                    )
                    DashboardKpiCard(
                        title = strings.todayProfit,
                        value = PakistaniFormatters.formatPkr(metrics.todayProfit),
                        icon = Icons.Outlined.TrendingUp,
                        iconBg = SuccessGreen,
                        softBg = SuccessGreen.copy(alpha = 0.14f),
                        onClick = { onNavigate(AppRoute.Reports) },
                        modifier = Modifier.weight(1f).testTag("kpi_today_profit")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    DashboardKpiCard(
                        title = strings.staffInside,
                        value = "${metrics.staffPresentCount} / ${metrics.totalStaffCount}",
                        icon = Icons.Outlined.Group,
                        iconBg = AccentPurple,
                        softBg = AccentPurple.copy(alpha = 0.14f),
                        onClick = { onNavigate(AppRoute.Attendance) },
                        modifier = Modifier.weight(1f).testTag("kpi_staff_inside")
                    )
                    DashboardKpiCard(
                        title = strings.stock,
                        value = PakistaniFormatters.formatTons(metrics.totalStockTons),
                        icon = Icons.Outlined.Inventory2,
                        iconBg = WarningOrange,
                        softBg = WarningOrange.copy(alpha = 0.14f),
                        onClick = { onNavigate(AppRoute.Inventory) },
                        modifier = Modifier.weight(1f).testTag("kpi_stock")
                    )
                }

                // Owner-Only Executive Break-Even & Loan Recovery Strip
                BrohiCard(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { onNavigate(AppRoute.Reports) }
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Super Admin Financial Health",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${accountingSummary.recoveryPercent.roundToInt()}% Recovered",
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = SuccessGreen
                            )
                        }
                        LinearProgressIndicator(
                            progress = { (accountingSummary.recoveryPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(4.dp)),
                            color = SuccessGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Profit: ${PakistaniFormatters.formatPkr(accountingSummary.regularProfitSoFar)}",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Loan Due: ${PakistaniFormatters.formatPkr(accountingSummary.stillToRepay)}",
                                fontSize = 11.sp,
                                color = if (accountingSummary.stillToRepay > 0) WarningOrange else SuccessGreen,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                BrohiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.salesTrend7Days,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val pctText = if (metrics.salesTrendPercentChange >= 0) {
                                "↑ ${metrics.salesTrendPercentChange}%"
                            } else {
                                "↓ ${kotlin.math.abs(metrics.salesTrendPercentChange)}%"
                            }
                            Text(
                                text = pctText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = SuccessGreen
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        SalesTrendBarChart(bars = metrics.salesTrend7Days)
                    }
                }

                BrohiCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = strings.quickActions,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "مکمل کنٹرول",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.45f))

                        val ownerTiles = listOf(
                            BilingualTile("Sales", "فروخت", Icons.Outlined.ShoppingCart, AppRoute.Sales),
                            BilingualTile("Expenses", "اخراجات", Icons.Outlined.ReceiptLong, AppRoute.Expenses),
                            BilingualTile("Inventory", "اسٹاک", Icons.Outlined.Inventory2, AppRoute.Inventory),
                            BilingualTile("Production", "پیداوار", Icons.Outlined.PrecisionManufacturing, AppRoute.Production),
                            BilingualTile("Attendance", "حاضری", Icons.Outlined.Schedule, AppRoute.Attendance),
                            BilingualTile("Approvals", "منظوری", Icons.Outlined.Group, AppRoute.UsersRoles),
                            BilingualTile("Salary", "تنخواہیں", Icons.Outlined.Payments, AppRoute.Salary),
                            BilingualTile("Reports", "رپورٹس", Icons.Outlined.BarChart, AppRoute.Reports),
                            BilingualTile("Settings", "اپڈیٹس و سیٹنگز", Icons.Outlined.Settings, AppRoute.Settings)
                        )
                        QuickActionGridRows(tiles = ownerTiles, onNavigate = onNavigate, tagPrefix = "quick_action")
                    }
                }
            }
        }
    }

    if (showDigitalIdModal) {
        val targetEmployeeId = primaryEmployee?.id ?: userProfile?.userId ?: "001"
        DigitalEmployeeIdDialog(
            employeeId = targetEmployeeId,
            name = cleanName,
            role = activeRole.label,
            shift = primaryEmployee?.shift?.ifBlank { null }
                ?: if (activeRole.isOwner) com.example.data.model.WORK_SHIFT_OPTIONS.last() else "Production",
            cnic = PakistaniFormatters.formatCnic(primaryEmployee?.cnic.orEmpty()).ifBlank { "42201-1234567-1" },
            joinDateRaw = primaryEmployee?.joinDate.orEmpty(),
            manualIdPhoto = primaryEmployee?.idCardPhoto,
            profilePhoto = photoUrl,
            attendanceFacePhoto = primaryEmployee?.attendanceFacePhoto,
            factorySettings = factorySettings,
            onUpdateManualIdPhoto = { dataUri ->
                onUpdateIdCardPhoto(targetEmployeeId, dataUri)
            },
            onOpenProfileClick = onProfileClick,
            onOpenFaceEnrollClick = onOpenFaceEnrollClick,
            onDismiss = { showDigitalIdModal = false }
        )
    }
}

@Composable
private fun DashboardKpiCard(
    title: String,
    value: String,
    icon: ImageVector,
    iconBg: Color,
    softBg: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    BrohiCard(modifier = modifier, onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(softBg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = iconBg,
                    modifier = Modifier.size(20.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = value,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun QuickActionGridRows(
    tiles: List<BilingualTile>,
    onNavigate: (AppRoute) -> Unit,
    tagPrefix: String
) {
    tiles.chunked(3).forEach { rowItems ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            rowItems.forEach { tile ->
                QuickActionTile(
                    english = tile.english,
                    urdu = tile.urdu,
                    icon = tile.icon,
                    onClick = {
                        tile.onSpecialClick?.invoke() ?: tile.route?.let(onNavigate)
                    },
                    modifier = Modifier
                        .weight(1f)
                        .testTag("${tagPrefix}_${tile.route?.key ?: "special"}")
                )
            }
            repeat(3 - rowItems.size) {
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun QuickActionTile(
    english: String,
    urdu: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = lerp(MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.surfaceVariant, 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 11.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = english,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = english,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = urdu,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun UserProfileScreen(
    userProfile: UserProfile?,
    fallbackName: String,
    fallbackEmail: String?,
    fallbackPhoto: String?,
    activeRole: UserRole,
    primaryEmployee: Employee?,
    myProfileUpdateRequest: ProfileUpdateRequest? = null,
    onUpdateProfile: (name: String, phone: String, customPhotoDataUri: String?, clearPhoto: Boolean) -> Unit,
    onSubmitFullProfile: (
        existingEmployee: Employee?,
        name: String,
        fatherName: String,
        cnic: String,
        phone: String,
        whatsappNumber: String,
        email: String,
        emergencyContact: String,
        requestedRole: String,
        shift: String,
        salary: Double,
        joinDate: String,
        customPhotoDataUri: String?,
        clearPhoto: Boolean
    ) -> Unit = { _, n, _, _, p, _, _, _, _, _, _, _, ph, cl -> onUpdateProfile(n, p, ph, cl) }
) {
    val context = LocalContext.current
    var showEditDialog by remember { mutableStateOf(false) }
    var showPhotoViewer by remember { mutableStateOf(false) }

    val displayName = PakistaniFormatters.formatTitleCase(
        userProfile?.displayName?.ifBlank { primaryEmployee?.name }?.ifBlank { fallbackName }
            ?: fallbackName.ifBlank { "Ahmed Khan" }
    )
    val fatherName = PakistaniFormatters.formatTitleCase(primaryEmployee?.fatherName.orEmpty())
    val phone = PakistaniFormatters.formatPhone(userProfile?.phone?.ifBlank { primaryEmployee?.phone } ?: primaryEmployee?.phone ?: "").ifBlank { "0300-1234567" }
    val whatsapp = PakistaniFormatters.formatPhone(primaryEmployee?.whatsappNumber.orEmpty())
    val cnic = PakistaniFormatters.formatCnic(primaryEmployee?.cnic.orEmpty()).ifBlank { "42201-1234567-1" }
    val emailDisplay = primaryEmployee?.email?.ifBlank { userProfile?.email } ?: userProfile?.email ?: fallbackEmail.orEmpty()
    val salary = if (activeRole.isOwner || activeRole == UserRole.STAFF || activeRole == UserRole.ADMIN) {
        PakistaniFormatters.formatPkr(primaryEmployee?.salary ?: 0.0)
    } else {
        "Protected"
    }
    val shift = primaryEmployee?.shift?.ifBlank { null }
        ?: if (activeRole.isOwner) WORK_SHIFT_OPTIONS.last() else "Not Assigned"
    val joinDate = primaryEmployee?.joinDate?.ifBlank { PakistaniFormatters.todayIso() } ?: PakistaniFormatters.todayIso()
    val emergency = PakistaniFormatters.formatPhone(primaryEmployee?.emergencyContact.orEmpty()).ifBlank { "0300-9876543" }

    val activeProfilePhoto = if (userProfile?.photoUrl != null) {
        userProfile.photoUrl.takeIf { it.isNotBlank() }
    } else {
        fallbackPhoto
    }

    val profilePhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val encoded = ImageAndQrUtils.encodeUriToJpegDataUri(context, uri, maxDimension = 320, quality = 74)
            if (!encoded.isNullOrBlank()) {
                onUpdateProfile(displayName, phone, encoded, false)
            } else {
                Toast.makeText(context, "Could not process selected photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    val launchProfileCamera = rememberCameraPhotoLauncher(maxDimension = 340, quality = 74) { encoded ->
        onUpdateProfile(displayName, phone, encoded, false)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Box(contentAlignment = Alignment.BottomEnd) {
            UserAvatarImage(
                photoUrl = activeProfilePhoto,
                contentDescription = "User profile picture",
                modifier = Modifier
                    .size(108.dp)
                    .clip(CircleShape)
                    .border(3.dp, Color.White, CircleShape)
                    .clickable { showPhotoViewer = true }
                    .testTag("profile_avatar_preview_button")
            )
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(RoyalBlue)
                    .border(2.dp, Color.White, CircleShape)
                    .clickable { launchProfileCamera() }
                    .testTag("change_profile_photo_button"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.CameraAlt,
                    contentDescription = "Take profile photo with camera",
                    tint = Color.White,
                    modifier = Modifier.size(17.dp)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(displayName, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = DeepBlue)
            if (emailDisplay.isNotBlank()) {
                Text(
                    text = emailDisplay,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                TextButton(
                    onClick = { showPhotoViewer = true },
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 2.dp)
                ) {
                    Text("Change Photo (تصویر بدلیں)", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
                if (!activeProfilePhoto.isNullOrBlank()) {
                    TextButton(
                        onClick = { onUpdateProfile(displayName, phone, null, true) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Remove", fontSize = 12.sp, color = DangerRed)
                    }
                }
            }
        }

        // Pending or Declined Profile Update Request Status Banner
        if (myProfileUpdateRequest != null && myProfileUpdateRequest.status.uppercase() in listOf("PENDING", "REJECTED")) {
            val isPendingReq = myProfileUpdateRequest.status.equals("PENDING", ignoreCase = true)
            val statusColor = if (isPendingReq) WarningOrange else DangerRed
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isPendingReq) Color(0xFFFFF7ED) else Color(0xFFFEF2F2),
                border = BorderStroke(1.dp, statusColor.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = if (isPendingReq) Icons.Filled.HourglassTop else Icons.Filled.Block,
                        contentDescription = null,
                        tint = statusColor,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isPendingReq) {
                                "Profile Update Pending Approval (منظوری کا انتظار)"
                            } else {
                                "Profile Update Declined (درخواست مسترد)"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.5.sp,
                            color = statusColor
                        )
                        Text(
                            text = if (isPendingReq) {
                                "Your updated details (${myProfileUpdateRequest.name}) were sent to Admin / Super Admin for review."
                            } else {
                                myProfileUpdateRequest.reviewNote?.takeIf { it.isNotBlank() }
                                    ?: "Your profile update request was declined. Tap Edit Profile to resubmit."
                            },
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        }

        BrohiCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileFieldRow(Icons.Outlined.Person, "Full Name (پورا نام)", displayName)
                HorizontalDivider(color = CrystalBorderLight.copy(alpha = 0.6f))
                ProfileFieldRow(
                    Icons.Outlined.SupervisorAccount,
                    "Father's Name (والد کا نام)",
                    fatherName.ifBlank { "Not specified" }
                )
                HorizontalDivider(color = CrystalBorderLight.copy(alpha = 0.6f))
                ProfileFieldRow(Icons.Outlined.Phone, "Mobile (موبائل)", phone)
                if (whatsapp.isNotBlank()) {
                    HorizontalDivider(color = CrystalBorderLight.copy(alpha = 0.6f))
                    ProfileFieldRow(Icons.Outlined.Chat, "WhatsApp (واٹس ایپ)", whatsapp)
                }
                HorizontalDivider(color = CrystalBorderLight.copy(alpha = 0.6f))
                ProfileFieldRow(Icons.Outlined.Badge, "CNIC (شناختی کارڈ)", cnic)
                if (emailDisplay.isNotBlank()) {
                    HorizontalDivider(color = CrystalBorderLight.copy(alpha = 0.6f))
                    ProfileFieldRow(Icons.Outlined.Email, "Email (ای میل)", emailDisplay)
                }
                HorizontalDivider(color = CrystalBorderLight.copy(alpha = 0.6f))
                ProfileFieldRow(Icons.Outlined.WorkspacePremium, "Role (عہدہ)", activeRole.label)
                HorizontalDivider(color = CrystalBorderLight.copy(alpha = 0.6f))
                ProfileFieldRow(Icons.Outlined.Payments, "Salary (تنخواہ)", salary)
                HorizontalDivider(color = CrystalBorderLight.copy(alpha = 0.6f))
                ProfileFieldRow(Icons.Outlined.Schedule, "Shift (شفٹ)", shift)
                HorizontalDivider(color = CrystalBorderLight.copy(alpha = 0.6f))
                ProfileFieldRow(Icons.Outlined.CalendarMonth, "Joining Date (شمولیت)", joinDate)
                HorizontalDivider(color = CrystalBorderLight.copy(alpha = 0.6f))
                ProfileFieldRow(Icons.Outlined.ContactPhone, "Emergency Contact (ہنگامی نمبر)", emergency)
            }
        }

        IcyGlossyButton(
            onClick = { showEditDialog = true },
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 50.dp)
                .testTag("edit_profile_button")
        ) {
            Icon(Icons.Filled.Edit, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Edit Profile (پروفائل ترمیم)", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }

    if (showPhotoViewer) {
        CrystalFullPhotoViewerDialog(
            title = "Profile Photo (پروفائل تصویر)",
            subtitle = displayName,
            photoUrl = activeProfilePhoto,
            onDismiss = { showPhotoViewer = false },
            onTakeCameraPhoto = { launchProfileCamera() },
            onPickFromGallery = {
                profilePhotoPicker.launch(
                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                )
            },
            removeLabel = if (!activeProfilePhoto.isNullOrBlank()) "Remove Photo (تصویر ہٹائیں)" else null,
            onRemovePhoto = if (!activeProfilePhoto.isNullOrBlank()) {
                { onUpdateProfile(displayName, phone, null, true) }
            } else {
                null
            }
        )
    }

    if (showEditDialog) {
        val canEditRoleAndDirect = activeRole.canManageStaff
        val seedReq = myProfileUpdateRequest?.takeIf { !canEditRoleAndDirect && it.status.equals("PENDING", ignoreCase = true) }

        var nameDraft by remember { mutableStateOf(seedReq?.name ?: displayName) }
        var fatherDraft by remember { mutableStateOf(seedReq?.fatherName ?: primaryEmployee?.fatherName.orEmpty()) }
        var cnicDigits by remember { mutableStateOf(PakistaniFormatters.normalizeCnicDigits(seedReq?.cnic ?: primaryEmployee?.cnic.orEmpty())) }
        var phoneDigits by remember { mutableStateOf(PakistaniFormatters.normalizePhoneDigits(seedReq?.phone ?: phone)) }
        var whatsappDigits by remember { mutableStateOf(PakistaniFormatters.normalizePhoneDigits(seedReq?.whatsappNumber ?: primaryEmployee?.whatsappNumber.orEmpty())) }
        var emergencyDigits by remember { mutableStateOf(PakistaniFormatters.normalizePhoneDigits(seedReq?.emergencyContact ?: primaryEmployee?.emergencyContact.orEmpty())) }
        var emailDraft by remember { mutableStateOf(seedReq?.email ?: emailDisplay) }
        var roleDraft by remember { mutableStateOf(primaryEmployee?.role?.ifBlank { activeRole.label } ?: activeRole.label) }
        var shiftDraft by remember { mutableStateOf(seedReq?.shift ?: shift.takeIf { it != "Not Assigned" } ?: WORK_SHIFT_OPTIONS.first()) }
        var salaryDraft by remember { mutableStateOf((primaryEmployee?.salary ?: 0.0).toLong().toString()) }
        var joinDateDraft by remember { mutableStateOf(seedReq?.joinDate ?: joinDate) }
        var pendingPhotoDataUri by remember { mutableStateOf<String?>(null) }
        var triedSave by remember { mutableStateOf(false) }
        var popupTitle by remember { mutableStateOf<String?>(null) }
        var popupMessage by remember { mutableStateOf<String?>(null) }

        val emergencySame = PakistaniFormatters.isEmergencySameAsPrimary(
            emergencyRaw = emergencyDigits,
            mobileRaw = phoneDigits,
            whatsappRaw = whatsappDigits
        )

        val dialogPhotoCamera = rememberCameraPhotoLauncher(maxDimension = 340, quality = 74) { encoded ->
            pendingPhotoDataUri = encoded
        }
        val dialogPhotoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                val encoded = ImageAndQrUtils.encodeUriToJpegDataUri(context, uri, maxDimension = 320, quality = 74)
                if (!encoded.isNullOrBlank()) {
                    pendingPhotoDataUri = encoded
                } else {
                    Toast.makeText(context, "Could not process selected photo", Toast.LENGTH_SHORT).show()
                }
            }
        }

        CrystalFormDialog(
            onDismissRequest = { showEditDialog = false },
            title = "Edit Profile (ترمیم)",
            subtitle = if (canEditRoleAndDirect) {
                "Full profile & role control (Super Admin / Admin)"
            } else {
                "Update profile details (Approval required)"
            },
            icon = Icons.Filled.Edit,
            confirmText = if (canEditRoleAndDirect) "Save (محفوظ)" else "Send for Approval (بھیجیں)",
            dismissText = "Cancel",
            onConfirm = {
                triedSave = true
                if (nameDraft.isBlank()) {
                    popupTitle = "Full Name Required (نام ضروری ہے)"
                    popupMessage = "Please enter your full name."
                    return@CrystalFormDialog
                }
                if (cnicDigits.isNotEmpty() && !PakistaniFormatters.isValidCnic(cnicDigits)) {
                    popupTitle = "CNIC Number Not Complete"
                    popupMessage = "CNIC number is not complete. Please enter all 13 digits (e.g. 44204-1253835-5).\n\nشناختی کارڈ نمبر مکمل نہیں ہے، براہ کرم 13 ہندسے درج کریں۔"
                    return@CrystalFormDialog
                }
                if (!PakistaniFormatters.isValidPhone(phoneDigits)) {
                    popupTitle = "Number Is Not Complete (نمبر مکمل نہیں ہے)"
                    popupMessage = "Mobile number is not complete. Please enter all 11 digits (e.g. 0333-2828285).\n\nموبائل نمبر مکمل نہیں ہے، براہ کرم پورے 11 ہندسے درج کریں۔"
                    return@CrystalFormDialog
                }
                if (whatsappDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(whatsappDigits)) {
                    popupTitle = "Number Is Not Complete (نمبر مکمل نہیں ہے)"
                    popupMessage = "WhatsApp number is not complete. Please enter all 11 digits.\n\nواٹس ایپ نمبر مکمل نہیں ہے، براہ کرم پورے 11 ہندسے درج کریں۔"
                    return@CrystalFormDialog
                }
                if (emergencyDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(emergencyDigits)) {
                    popupTitle = "Number Is Not Complete (نمبر مکمل نہیں ہے)"
                    popupMessage = "Emergency contact number is not complete. Please enter all 11 digits.\n\nہنگامی نمبر مکمل نہیں ہے، براہ کرم پورے 11 ہندسے درج کریں۔"
                    return@CrystalFormDialog
                }
                if (emergencySame) {
                    popupTitle = "Different Emergency Number Required"
                    popupMessage = "Emergency contact cannot be same as Mobile or WhatsApp number.\n\nہنگامی رابطہ نمبر موبائل یا واٹس ایپ جیسا نہیں ہو سکتا۔"
                    return@CrystalFormDialog
                }
                onSubmitFullProfile(
                    primaryEmployee,
                    PakistaniFormatters.formatTitleCase(nameDraft),
                    PakistaniFormatters.formatTitleCase(fatherDraft),
                    PakistaniFormatters.formatCnic(cnicDigits),
                    PakistaniFormatters.formatPhone(phoneDigits),
                    PakistaniFormatters.formatPhone(whatsappDigits),
                    emailDraft.trim(),
                    PakistaniFormatters.formatPhone(emergencyDigits),
                    roleDraft,
                    shiftDraft,
                    salaryDraft.toDoubleOrNull() ?: (primaryEmployee?.salary ?: 0.0),
                    joinDateDraft.trim().ifBlank { PakistaniFormatters.todayIso() },
                    pendingPhotoDataUri,
                    false
                )
                showEditDialog = false
            }
        ) {
            if (!canEditRoleAndDirect) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFFFF7ED),
                    border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(18.dp))
                        Text(
                            text = "Profile changes will be sent to Admin / Super Admin for approval before updating.\nتبدیلیاں ایڈمن / سپر ایڈمن کی منظوری کے بعد اپ ڈیٹ ہوں گی۔",
                            fontSize = 11.sp,
                            color = Color(0xFF9A3412),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = { dialogPhotoCamera() },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = DeepBlue),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                ) {
                    Icon(Icons.Filled.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (pendingPhotoDataUri != null) "Photo Ready ✓" else "Camera",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }
                OutlinedButton(
                    onClick = {
                        dialogPhotoPicker.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, RoyalBlue.copy(alpha = 0.4f)),
                    colors = ButtonDefaults.outlinedButtonColors(containerColor = Color.White, contentColor = DeepBlue),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 44.dp)
                ) {
                    Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Gallery", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                }
            }

            CrystalTextField(
                value = nameDraft,
                onValueChange = { nameDraft = it },
                englishLabel = "Full Name *",
                urduLabel = "پورا نام",
                leadingIcon = Icons.Outlined.Person
            )
            CrystalTextField(
                value = fatherDraft,
                onValueChange = { fatherDraft = it },
                englishLabel = "Father's Name",
                urduLabel = "والد کا نام",
                leadingIcon = Icons.Outlined.SupervisorAccount
            )
            CnicCrystalTextField(
                digitsValue = cnicDigits,
                onDigitsChange = { cnicDigits = it },
                leadingIcon = Icons.Outlined.Badge,
                isError = triedSave && cnicDigits.isNotEmpty() && !PakistaniFormatters.isValidCnic(cnicDigits)
            )
            PhoneCrystalTextField(
                digitsValue = phoneDigits,
                onDigitsChange = { phoneDigits = it },
                englishLabel = "Mobile Number *",
                urduLabel = "موبائل نمبر",
                leadingIcon = Icons.Outlined.Phone,
                isError = triedSave && !PakistaniFormatters.isValidPhone(phoneDigits)
            )
            PhoneCrystalTextField(
                digitsValue = whatsappDigits,
                onDigitsChange = { whatsappDigits = it },
                englishLabel = "WhatsApp Number",
                urduLabel = "واٹس ایپ نمبر",
                leadingIcon = Icons.Outlined.Chat,
                helperText = "Can match Mobile or enter different",
                isError = triedSave && whatsappDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(whatsappDigits),
                actionChip = {
                    if (phoneDigits.isNotEmpty() && whatsappDigits != phoneDigits) {
                        TextButton(
                            onClick = { whatsappDigits = phoneDigits },
                            contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                        ) {
                            Text("Same", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            )
            PhoneCrystalTextField(
                digitsValue = emergencyDigits,
                onDigitsChange = { emergencyDigits = it },
                englishLabel = "Emergency Contact",
                urduLabel = "ہنگامی نمبر",
                leadingIcon = Icons.Outlined.ContactPhone,
                helperText = "Must be different from Mobile / WhatsApp",
                errorMessage = if (emergencySame) {
                    "Emergency contact cannot match Mobile / WhatsApp"
                } else {
                    "Number is not complete: enter 11 digits"
                },
                isError = emergencySame ||
                    (triedSave && emergencyDigits.isNotEmpty() && !PakistaniFormatters.isValidPhone(emergencyDigits))
            )
            CrystalTextField(
                value = emailDraft,
                onValueChange = { emailDraft = it },
                englishLabel = "Email Address",
                urduLabel = "ای میل",
                leadingIcon = Icons.Outlined.Email,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
            )

            // Role & Salary controls are ONLY visible and editable for Super Admin and Admin
            if (canEditRoleAndDirect) {
                Text(
                    text = "Access Role • عہدہ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = DeepBlue
                )
                val roleChoices = if (activeRole.isOwner) {
                    REGISTRATION_ROLE_OPTIONS
                } else {
                    listOf("Admin", "Manager", "Staff")
                }
                ModernRoleSelectorCardGrid(
                    selectedRole = roleDraft,
                    roles = roleChoices,
                    onSelectRole = { roleDraft = it }
                )
                CrystalTextField(
                    value = salaryDraft,
                    onValueChange = { salaryDraft = it },
                    englishLabel = "Monthly Salary (Rs.)",
                    urduLabel = "ماہانہ تنخواہ",
                    leadingIcon = Icons.Outlined.Payments,
                    keyboardType = KeyboardType.Number
                )
            }

            Text(
                text = "Work Shift • شفٹ",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = DeepBlue
            )
            ModernShiftSelector(
                selectedShift = shiftDraft,
                shifts = WORK_SHIFT_OPTIONS,
                onSelectShift = { shiftDraft = it }
            )
            CrystalTextField(
                value = joinDateDraft,
                onValueChange = { joinDateDraft = it },
                englishLabel = "Joining Date",
                urduLabel = "شمولیت کی تاریخ",
                leadingIcon = Icons.Outlined.CalendarMonth,
                placeholder = "YYYY-MM-DD"
            )
        }
        ValidationPopupDialog(
            title = popupTitle ?: "Check Profile Details (تفصیلات چیک کریں)",
            message = popupMessage,
            onDismiss = { popupMessage = null }
        )
    }
}

@Composable
private fun ProfileFieldRow(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(19.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}
