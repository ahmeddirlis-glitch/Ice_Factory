package com.example

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.HowToReg
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.auth.GoogleAuthHelper
import com.example.data.auth.GoogleAuthResult
import com.google.firebase.auth.FirebaseAuth
import com.example.data.model.UserRole
import com.example.data.model.canUserEnterMainWorkspace
import com.example.data.model.formatRemainingFaceDeadline
import com.example.data.util.AppPreferences
import com.example.data.util.AppUpdateDownloadState
import com.example.data.util.LocalStorageBootstrap
import com.example.data.util.PreferencesManager
import com.example.data.util.RuntimeDiagnosticLogger
import com.example.data.util.ScreenSnapshotHelper
import com.example.data.util.getAppStrings
import com.example.data.voice.VoiceSpeechManager
import com.example.data.voice.VoiceWakeService
import com.example.security.configureAppCheck
import com.example.ui.audit.AiAuditOverlayController
import com.example.ui.audit.AiLiveAuditControlDialog
import com.example.ui.audit.FloatingAiAuditButton
import com.example.ui.audit.LocalAiAuditController
import com.example.ui.components.BrohiBottomTaskbar
import com.example.ui.components.BrohiDashboardTopBar
import com.example.ui.components.BrohiDrawerContent
import com.example.ui.components.BrohiSubScreenTopBar
import com.example.ui.components.Crystal3DNavigationDrawer
import com.example.ui.components.CrystalBackground
import com.example.ui.components.CrystalFormDialog
import com.example.ui.components.IcyGlossyButton
import com.example.ui.components.IcyGlossyTone
import com.example.ui.components.IcySplashProgressBar
import com.example.ui.components.VoiceCommandBottomSheet
import com.example.ui.navigation.AppRoute
import com.example.ui.screens.AboutScreen
import com.example.ui.screens.AppUpdatesScreen
import com.example.ui.screens.AttendanceScreen
import com.example.ui.screens.CustomersCreditScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.DeliveriesVehiclesScreen
import com.example.ui.screens.ExpensesFinanceScreen
import com.example.ui.screens.FaceEnrollmentScreen
import com.example.ui.screens.GuidedFaceEnrollmentDialog
import com.example.ui.screens.InventoryColdStorageScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.MaintenanceScreen
import com.example.ui.screens.NoticesScreen
import com.example.ui.screens.ProductionScreen
import com.example.ui.screens.ReportsAnalyticsScreen
import com.example.ui.screens.SafetyIncidentsScreen
import com.example.ui.screens.SalaryPayrollScreen
import com.example.ui.screens.SalesInvoicingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.SuppliersScreen
import com.example.ui.screens.UserProfileScreen
import com.example.ui.screens.UsersRolesScreen
import com.example.ui.screens.UtilitiesScreen
import com.example.ui.screens.WaitingForApprovalGateScreen
import com.example.ui.theme.BrohiIceTheme
import com.example.ui.theme.CrystalBorderLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DangerRedSoft
import com.example.ui.theme.DeepBlue
import com.example.ui.theme.RoyalBlue
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenSoft
import com.example.ui.theme.WarningOrange
import com.example.ui.theme.WarningOrangeSoft
import com.example.ui.viewmodel.FactoryViewModel
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val wakeTriggerFlow = MutableStateFlow<Pair<Long, String>?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        LocalStorageBootstrap.ensureImageCacheConfigured(applicationContext)
        // CRITICAL: Initialize Firebase App Check with Launch Intent BEFORE any Firebase/ViewModel calls
        configureAppCheck(this, intent)
        handleWakeIntent(intent)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT
            )
        )

        setContent {
            BrohiIceFactoryRoot(
                activity = this,
                wakeTriggerState = wakeTriggerFlow,
                onConsumeWakeTrigger = { wakeTriggerFlow.value = null }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWakeIntent(intent)
    }

    private fun handleWakeIntent(intent: Intent?) {
        if (intent == null) return
        val isWake = intent.action == VoiceWakeService.ACTION_WAKE_TRIGGERED ||
            intent.getBooleanExtra(VoiceWakeService.EXTRA_WAKE_TRIGGERED, false)
        if (isWake) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true)
                setTurnScreenOn(true)
            } else {
                @Suppress("DEPRECATION")
                window.addFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                )
            }
            val trailingCmd = intent.getStringExtra(VoiceWakeService.EXTRA_WAKE_COMMAND).orEmpty()
            wakeTriggerFlow.value = System.currentTimeMillis() to trailingCmd
        }
    }
}

@Composable
private fun BrohiIceFactoryRoot(
    activity: ComponentActivity,
    wakeTriggerState: StateFlow<Pair<Long, String>?>,
    onConsumeWakeTrigger: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferencesManager = remember { PreferencesManager(context.applicationContext) }
    val googleAuthHelper = remember { GoogleAuthHelper(context.applicationContext) }

    val prefs by preferencesManager.preferencesFlow.collectAsStateWithLifecycle(initialValue = AppPreferences())
    var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
    DisposableEffect(Unit) {
        val auth = Firebase.auth
        currentUser = auth.currentUser
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUser = firebaseAuth.currentUser
        }
        auth.addAuthStateListener(listener)
        onDispose {
            auth.removeAuthStateListener(listener)
        }
    }

    val strings = remember(prefs.language) { getAppStrings(prefs.language) }
    val wakeEvent by wakeTriggerState.collectAsStateWithLifecycle()
    var showSplash by rememberSaveable { mutableStateOf(true) }
    var hasLoggedInThisLaunch by rememberSaveable { mutableStateOf(false) }
    var isSigningIn by remember { mutableStateOf(false) }
    var authError by remember { mutableStateOf<String?>(null) }
    var authCancelNotice by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(wakeEvent, currentUser) {
        if (wakeEvent != null && currentUser != null) {
            showSplash = false
            hasLoggedInThisLaunch = true
        }
    }

    BrohiIceTheme(
        darkTheme = prefs.darkMode,
        isUrduRtl = prefs.language == "ur"
    ) {
        val activeUser = currentUser
        if (showSplash) {
            SplashScreen(
                strings = strings,
                onTimeout = { showSplash = false }
            )
        } else if (!hasLoggedInThisLaunch || activeUser == null) {
            val launchGoogleAuth: (hint: String, pendingJson: String?) -> Unit = { hint, pendingJson ->
                if (!isSigningIn) {
                    activity.lifecycleScope.launch {
                        isSigningIn = true
                        authError = null
                        authCancelNotice = null
                        try {
                            preferencesManager.setOwnerPreviewRole(null)
                            preferencesManager.setPendingRegistrationJson(pendingJson ?: "")
                            if (prefs.rememberMe && hint.isNotBlank()) {
                                preferencesManager.setSavedEmailHint(hint)
                            }
                            val online = LocalStorageBootstrap.isNetworkAvailable(context)
                            if (!online && pendingJson == null && currentUser != null) {
                                hasLoggedInThisLaunch = true
                                Toast.makeText(
                                    context,
                                    "Offline login — using local database (will sync when online)",
                                    Toast.LENGTH_SHORT
                                ).show()
                                return@launch
                            }
                            when (val result = googleAuthHelper.signInWithGoogleInteractive(activity, isOnline = online)) {
                                is GoogleAuthResult.Success -> {
                                    currentUser = result.user
                                    authError = null
                                    authCancelNotice = null
                                    hasLoggedInThisLaunch = true
                                }
                                is GoogleAuthResult.Cancelled -> {
                                    if (pendingJson != null) {
                                        preferencesManager.setPendingRegistrationJson("")
                                    }
                                    val reason = result.reason?.trim().orEmpty()
                                    val looksLikeUserDismissed = reason.isBlank() ||
                                        reason.contains("cancelled by the user", ignoreCase = true) ||
                                        reason.contains("canceled by the user", ignoreCase = true)
                                    if (looksLikeUserDismissed) {
                                        authCancelNotice = "Google Sign-In did not complete. Tap Continue with Google to retry."
                                    } else {
                                        authError = "Google Sign-In did not complete ($reason). Please tap Continue with Google to try again."
                                    }
                                }
                                is GoogleAuthResult.NoSavedCredentials -> {
                                    if (pendingJson != null) {
                                        preferencesManager.setPendingRegistrationJson("")
                                    }
                                    authError = if (!online) {
                                        "Internet connection required for first-time Google Sign-In or Registration."
                                    } else {
                                        "No Google account found on device. Please add a Google account in device Settings and try again."
                                    }
                                }
                                is GoogleAuthResult.Error -> {
                                    if (pendingJson != null) {
                                        preferencesManager.setPendingRegistrationJson("")
                                    }
                                    authError = result.message
                                }
                            }
                        } finally {
                            isSigningIn = false
                        }
                    }
                }
            }
            // Unauthenticated or fresh-launch state: ALWAYS show LoginScreen after Splash
            LoginScreen(
                strings = strings,
                initialEmailHint = prefs.savedEmailHint,
                rememberMe = prefs.rememberMe,
                isSigningIn = isSigningIn,
                errorMessage = authError,
                cancelNotice = authCancelNotice,
                existingSignedInEmail = activeUser?.email?.takeIf { it.isNotBlank() }
                    ?: activeUser?.displayName?.takeIf { it.isNotBlank() },
                onContinueExistingSession = {
                    if (activeUser != null) {
                        activity.lifecycleScope.launch {
                            preferencesManager.setOwnerPreviewRole(null)
                            preferencesManager.setPendingRegistrationJson("")
                            authError = null
                            authCancelNotice = null
                            hasLoggedInThisLaunch = true
                        }
                    }
                },
                onDismissError = { authError = null },
                onRememberMeChange = { checked ->
                    scope.launch { preferencesManager.setRememberMe(checked) }
                },
                onGoogleSignInClick = { hint ->
                    launchGoogleAuth(hint, null)
                },
                onSubmitRegistrationAndSignIn = { json, hint ->
                    launchGoogleAuth(hint, json)
                }
            )
        } else {
            key(activeUser.uid) {
                AuthenticatedFactoryShell(
                    activity = activity,
                    user = activeUser,
                    prefs = prefs,
                    wakeEvent = wakeEvent,
                    onConsumeWakeTrigger = onConsumeWakeTrigger,
                    onSignOut = {
                        hasLoggedInThisLaunch = false
                        authError = null
                        authCancelNotice = null
                        VoiceWakeService.stop(context)
                        activity.lifecycleScope.launch {
                            preferencesManager.setPendingRegistrationJson("")
                            preferencesManager.setOwnerPreviewRole(null)
                            googleAuthHelper.signOut()
                            currentUser = null
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedFactoryShell(
    activity: ComponentActivity,
    user: FirebaseUser,
    prefs: AppPreferences,
    wakeEvent: Pair<Long, String>?,
    onConsumeWakeTrigger: () -> Unit,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDrawerOpen by remember { mutableStateOf(false) }
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val viewModel: FactoryViewModel = viewModel(
        key = user.uid,
        factory = FactoryViewModel.provideFactory(
            currentUserId = user.uid,
            currentUserName = user.displayName ?: "Ahmed Khan",
            currentUserEmail = user.email,
            currentUserPhotoUrl = user.photoUrl?.toString()
        )
    )

    val speechManager = remember { VoiceSpeechManager(context) }
    DisposableEffect(Unit) {
        onDispose { speechManager.release() }
    }

    val approvalStatus by viewModel.approvalStatus.collectAsStateWithLifecycle()
    val myRegistrationRequest by viewModel.myRegistrationRequest.collectAsStateWithLifecycle()
    val appRelease by viewModel.appRelease.collectAsStateWithLifecycle()
    val isUpdateAvailable by viewModel.isUpdateAvailable.collectAsStateWithLifecycle()
    val isCheckingUpdate by viewModel.isCheckingUpdate.collectAsStateWithLifecycle()
    val lastUpdateCheckedMessage by viewModel.lastUpdateCheckedMessage.collectAsStateWithLifecycle()
    val updateDownloadState by viewModel.updateDownloadState.collectAsStateWithLifecycle()

    val activeRole by viewModel.activeRole.collectAsStateWithLifecycle()
    val canSwitchWorkspaceRole by viewModel.canSwitchWorkspaceRole.collectAsStateWithLifecycle()
    val hasPendingCloudSync by viewModel.hasPendingCloudSync.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val employees by viewModel.employees.collectAsStateWithLifecycle()
    val registrationRequests by viewModel.registrationRequests.collectAsStateWithLifecycle()
    val profileUpdateRequests by viewModel.profileUpdateRequests.collectAsStateWithLifecycle()
    val myProfileUpdateRequest by viewModel.myProfileUpdateRequest.collectAsStateWithLifecycle()
    val notices by viewModel.notices.collectAsStateWithLifecycle()
    val attendance by viewModel.attendance.collectAsStateWithLifecycle()
    val production by viewModel.production.collectAsStateWithLifecycle()
    val inventory by viewModel.inventory.collectAsStateWithLifecycle()
    val customers by viewModel.customers.collectAsStateWithLifecycle()
    val sales by viewModel.sales.collectAsStateWithLifecycle()
    val expenses by viewModel.expenses.collectAsStateWithLifecycle()
    val funding by viewModel.funding.collectAsStateWithLifecycle()
    val payroll by viewModel.payroll.collectAsStateWithLifecycle()
    val utilities by viewModel.utilities.collectAsStateWithLifecycle()
    val deliveries by viewModel.deliveries.collectAsStateWithLifecycle()
    val machines by viewModel.machines.collectAsStateWithLifecycle()
    val suppliers by viewModel.suppliers.collectAsStateWithLifecycle()
    val incidents by viewModel.incidents.collectAsStateWithLifecycle()
    val metrics by viewModel.dashboardMetrics.collectAsStateWithLifecycle()
    val accountingSummary by viewModel.ownerAccountingSummary.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()
    val voiceUiState by viewModel.voiceState.collectAsStateWithLifecycle()
    val speechUiState by speechManager.state.collectAsStateWithLifecycle()
    val faceGate by viewModel.faceEnrollmentGate.collectAsStateWithLifecycle()
    val aiAuditUiState by viewModel.aiAuditUiState.collectAsStateWithLifecycle()

    val strings = remember(prefs.language) { getAppStrings(prefs.language) }
    var showVoiceSheet by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showAppUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var showMandatoryFaceEnrollDialog by remember { mutableStateOf(false) }
    var promptedReleaseVersion by rememberSaveable { mutableIntStateOf(0) }
    var auditPanelInitialTab by remember { mutableIntStateOf(0) }

    LaunchedEffect(toastMsg) {
        val msg = toastMsg
        if (!msg.isNullOrBlank()) {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
            viewModel.clearToast()
        }
    }

    LaunchedEffect(approvalStatus, isUpdateAvailable, appRelease?.latestVersionCode) {
        val rel = appRelease
        if (canUserEnterMainWorkspace(approvalStatus) &&
            isUpdateAvailable &&
            rel != null &&
            rel.latestVersionCode > promptedReleaseVersion
        ) {
            promptedReleaseVersion = rel.latestVersionCode
            showAppUpdateDialog = true
        }
    }

    val displayName = userProfile?.displayName?.ifBlank { user.displayName ?: "Ahmed Khan" }
        ?: user.displayName
        ?: "Ahmed Khan"

    LaunchedEffect(prefs.voiceWakeLang) {
        if (prefs.voiceWakeLang.isNotBlank()) {
            speechManager.setLocaleTag(prefs.voiceWakeLang)
        }
    }

    LaunchedEffect(approvalStatus, prefs.heyIceWakeEnabled, displayName, prefs.voiceWakeLang) {
        if (canUserEnterMainWorkspace(approvalStatus)) {
            val micGranted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.RECORD_AUDIO
            ) == PackageManager.PERMISSION_GRANTED
            if (prefs.heyIceWakeEnabled && micGranted) {
                VoiceWakeService.startOrUpdate(
                    context = context,
                    userName = displayName,
                    localeTag = prefs.voiceWakeLang.ifBlank { "ur-PK" }
                )
            } else {
                VoiceWakeService.stop(context)
            }
        }
    }

    LaunchedEffect(wakeEvent, approvalStatus, displayName, speechUiState.selectedLocaleTag) {
        val ev = wakeEvent
        if (ev != null && canUserEnterMainWorkspace(approvalStatus)) {
            onConsumeWakeTrigger()
            speechManager.clearResult()
            showVoiceSheet = true
            val trailingCmd = ev.second.trim()
            if (trailingCmd.isNotBlank()) {
                val isOnline = LocalStorageBootstrap.isNetworkAvailable(context)
                viewModel.parseVoiceTranscript(
                    transcript = trailingCmd,
                    isOnline = isOnline,
                    preferredLocaleTag = speechUiState.selectedLocaleTag,
                    userName = displayName
                ) { reply, lang, reopenMic ->
                    speechManager.speak(reply, lang) {
                        if (reopenMic) speechManager.startListening()
                    }
                }
            } else {
                viewModel.startWakeGreeting(
                    userName = displayName,
                    preferredLocaleTag = speechUiState.selectedLocaleTag
                ) { greeting, lang, reopenMic ->
                    speechManager.speak(greeting, lang) {
                        if (reopenMic) speechManager.startListening()
                    }
                }
            }
        }
    }

    // STRICT APPROVAL GATE: Unapproved users NEVER enter Dashboard / Main Workspace
    if (approvalStatus == "LOADING") {
        CrystalBackground(darkTheme = prefs.darkMode) {
            Column(
                modifier = Modifier.fillMaxSize().padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator(color = RoyalBlue)
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Checking Account Approval...\nاکاؤنٹ کی منظوری چیک ہو رہی ہے",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(14.dp))
                TextButton(onClick = onSignOut) {
                    Text("Sign Out (لاگ آؤٹ)", color = DangerRed, fontWeight = FontWeight.Bold)
                }
            }
        }
        return
    }

    if (!canUserEnterMainWorkspace(approvalStatus)) {
        WaitingForApprovalGateScreen(
            darkTheme = prefs.darkMode,
            approvalStatus = approvalStatus,
            userName = displayName,
            userEmail = user.email,
            profile = userProfile,
            myRequest = myRegistrationRequest,
            onRefreshStatus = viewModel::refreshApprovalStatus,
            onSubmitUpdatedRegistration = { fullName, fatherName, cnic, phone, whatsapp, emergency, requestedRole, shift, faceTemplateEncrypted ->
                viewModel.submitRegistrationFromGate(
                    fullName = fullName,
                    fatherName = fatherName,
                    cnic = cnic,
                    phone = phone,
                    whatsappNumber = whatsapp,
                    emergencyContact = emergency,
                    requestedRole = requestedRole,
                    shift = shift,
                    faceTemplateEncrypted = faceTemplateEncrypted
                )
            },
            onLogoutClick = onSignOut
        )
        return
    }

    // Handle voice navigation commands with strict role check
    LaunchedEffect(voiceUiState.pendingNavigationKey) {
        val target = voiceUiState.pendingNavigationKey
        if (!target.isNullOrBlank()) {
            val route = AppRoute.fromKey(target)
            if (AppRoute.isAllowedFor(activeRole, route.key)) {
                navController.navigate(route) {
                    launchSingleTop = true
                }
            }
            viewModel.consumePendingNavigation()
            showVoiceSheet = false
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRouteString = navBackStackEntry?.destination?.route ?: ""
    val currentRouteKey = remember(currentRouteString) {
        when {
            currentRouteString.contains("UsersRoles") -> AppRoute.UsersRoles.key
            currentRouteString.contains("Attendance") -> AppRoute.Attendance.key
            currentRouteString.contains("FaceVerification") -> AppRoute.FaceVerification.key
            currentRouteString.contains("Notices") -> AppRoute.Notices.key
            currentRouteString.contains("Production") -> AppRoute.Production.key
            currentRouteString.contains("Inventory") -> AppRoute.Inventory.key
            currentRouteString.contains("Sales") -> AppRoute.Sales.key
            currentRouteString.contains("Customers") -> AppRoute.Customers.key
            currentRouteString.contains("Deliveries") -> AppRoute.Deliveries.key
            currentRouteString.contains("Expenses") -> AppRoute.Expenses.key
            currentRouteString.contains("Salary") -> AppRoute.Salary.key
            currentRouteString.contains("Utilities") -> AppRoute.Utilities.key
            currentRouteString.contains("Maintenance") -> AppRoute.Maintenance.key
            currentRouteString.contains("Suppliers") -> AppRoute.Suppliers.key
            currentRouteString.contains("Incidents") -> AppRoute.Incidents.key
            currentRouteString.contains("Reports") -> AppRoute.Reports.key
            currentRouteString.contains("Settings") -> AppRoute.Settings.key
            currentRouteString.contains("About") -> AppRoute.About.key
            currentRouteString.contains("Updates") -> AppRoute.Updates.key
            currentRouteString.contains("Profile") -> AppRoute.Profile.key
            else -> AppRoute.Dashboard.key
        }
    }

    val screenTitle = when (currentRouteKey) {
        AppRoute.UsersRoles.key -> "Users & Approvals"
        AppRoute.Attendance.key -> if (activeRole == UserRole.STAFF) "My Attendance" else "Attendance"
        AppRoute.FaceVerification.key -> "Face Enrollment"
        AppRoute.Notices.key -> "Notices & Holidays"
        AppRoute.Production.key -> "Production"
        AppRoute.Inventory.key -> "Inventory & Finance"
        AppRoute.Sales.key -> "Sales & Invoicing"
        AppRoute.Customers.key -> "Customers & Credit"
        AppRoute.Deliveries.key -> "Deliveries & Vehicles"
        AppRoute.Expenses.key -> "Expenses & Finance"
        AppRoute.Salary.key -> if (activeRole == UserRole.STAFF) "My Salary & Slips" else "Salary & Payroll"
        AppRoute.Utilities.key -> "Electricity / Diesel / Water"
        AppRoute.Maintenance.key -> "Machine Maintenance"
        AppRoute.Suppliers.key -> "Suppliers"
        AppRoute.Incidents.key -> "Safety Incidents"
        AppRoute.Reports.key -> "Reports & Analytics"
        AppRoute.Settings.key -> "Settings"
        AppRoute.About.key -> "About"
        AppRoute.Updates.key -> "App Updates"
        AppRoute.Profile.key -> "User Profile"
        else -> "Brohi Ice Factory"
    }

    val pendingRegApprovals = registrationRequests.count { it.status.equals("PENDING", ignoreCase = true) }
    val pendingProfileApprovals = profileUpdateRequests.count { it.status.equals("PENDING", ignoreCase = true) }
    val pendingApprovalsCount = pendingRegApprovals + pendingProfileApprovals
    val alerts = buildList {
        notices.firstOrNull { it.active }?.let { add("Notice: ${it.title}") }
        if (activeRole.canManageStaff && pendingRegApprovals > 0) {
            add("$pendingRegApprovals new staff registration(s) awaiting approval")
        }
        if (activeRole.canManageStaff && pendingProfileApprovals > 0) {
            add("$pendingProfileApprovals profile update request(s) awaiting approval")
        }
        val openIncidents = incidents.count { it.status != "Resolved" }
        if (openIncidents > 0) add("$openIncidents open safety incident(s)")
        if (activeRole != UserRole.STAFF) {
            val dueMachines = machines.count { it.status != "Operational" }
            if (dueMachines > 0) add("$dueMachines machine(s) need maintenance/repair")
            if (metrics.totalStockTons < 2.0) add("Low ice stock (${metrics.totalStockTons} Ton)")
        }
        if (activeRole.isOwner && accountingSummary.stillToRepay > 0) {
            add("Loan balance to repay: ${com.example.data.util.PakistaniFormatters.formatPkr(accountingSummary.stillToRepay)}")
        }
    }

    val navigateToRoute: (AppRoute) -> Unit = { route ->
        if (!AppRoute.isAllowedFor(activeRole, route.key)) {
            viewModel.showToast("Access restricted for ${activeRole.label} role")
        } else {
            navController.navigate(route) {
                launchSingleTop = true
                if (route == AppRoute.Dashboard) {
                    popUpTo(AppRoute.Dashboard) { inclusive = false }
                }
            }
        }
    }

    val photoUrl = if (userProfile?.photoUrl != null) {
        userProfile?.photoUrl?.takeIf { it.isNotBlank() }
    } else {
        user.photoUrl?.toString()
    }

    LaunchedEffect(
        currentRouteKey,
        screenTitle,
        activeRole,
        canSwitchWorkspaceRole,
        prefs.aiAuditEnabled,
        prefs.aiAuditAutoWatch,
        aiAuditUiState.isPanelOpen
    ) {
        RuntimeDiagnosticLogger.recordScreenVisit(currentRouteKey, screenTitle, activeRole)
        viewModel.updateAuditActiveScreen(currentRouteKey, screenTitle)
        if (canSwitchWorkspaceRole && prefs.aiAuditEnabled && prefs.aiAuditAutoWatch && !aiAuditUiState.isPanelOpen) {
            delay(1300L)
            viewModel.setAuditCapturingSnapshot(true)
            delay(90L)
            val bmp = ScreenSnapshotHelper.captureActivityWindowBitmap(activity)
            viewModel.setAuditCapturingSnapshot(false)
            viewModel.runAutoWatchAudit(currentRouteKey, screenTitle, activeRole, bmp)
        }
    }

    val scanCurrentScreenNow: () -> Unit = {
        if (canSwitchWorkspaceRole && prefs.aiAuditEnabled) {
            scope.launch {
                auditPanelInitialTab = 0
                viewModel.dismissAuditPanel()
                delay(160L)
                viewModel.setAuditCapturingSnapshot(true)
                delay(90L)
                val bmp = ScreenSnapshotHelper.captureActivityWindowBitmap(activity)
                viewModel.setAuditCapturingSnapshot(false)
                viewModel.openAuditPanel(currentRouteKey, screenTitle)
                viewModel.runInstantScreenAudit(currentRouteKey, screenTitle, activeRole, bmp)
            }
        }
    }

    val aiAuditController = remember(
        canSwitchWorkspaceRole,
        prefs.aiAuditEnabled,
        aiAuditUiState.pendingCount,
        aiAuditUiState.isAuditing,
        activeRole
    ) {
        AiAuditOverlayController(
            isEnabledForSuperAdmin = canSwitchWorkspaceRole && prefs.aiAuditEnabled,
            pendingCount = aiAuditUiState.pendingCount,
            isScanning = aiAuditUiState.isAuditing,
            onAuditDialogSnapshot = { dialogTitle, bmp ->
                if (canSwitchWorkspaceRole && prefs.aiAuditEnabled) {
                    val slug = dialogTitle.lowercase().replace(Regex("[^a-z0-9]+"), "_").trim('_').take(24)
                    val dlgKey = "dialog_${slug.ifBlank { "modal" }}"
                    val dlgTitle = "Dialog: $dialogTitle"
                    auditPanelInitialTab = 0
                    viewModel.openAuditPanel(dlgKey, dlgTitle)
                    viewModel.runInstantScreenAudit(dlgKey, dlgTitle, activeRole, bmp)
                } else {
                    bmp?.recycle()
                }
            }
        )
    }

    CompositionLocalProvider(LocalAiAuditController provides aiAuditController) {
        Box(modifier = Modifier.fillMaxSize()) {
            Crystal3DNavigationDrawer(
        isOpen = isDrawerOpen,
        onOpenChange = { isDrawerOpen = it },
        drawerContent = {
            BrohiDrawerContent(
                userName = displayName,
                photoUrl = photoUrl,
                activeRole = activeRole,
                currentRouteKey = currentRouteKey,
                strings = strings,
                isUpdateAvailable = isUpdateAvailable,
                onSelectRoute = { route ->
                    isDrawerOpen = false
                    navigateToRoute(route)
                },
                onProfileClick = {
                    isDrawerOpen = false
                    navigateToRoute(AppRoute.Profile)
                },
                onLogoutClick = {
                    isDrawerOpen = false
                    onSignOut()
                }
            )
        }
    ) {
        CrystalBackground {
            Scaffold(
                containerColor = Color.Transparent,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = { SnackbarHost(snackbarHostState) },
                topBar = {
                    when (currentRouteKey) {
                        AppRoute.Dashboard.key -> {
                            BrohiDashboardTopBar(
                                photoUrl = photoUrl,
                                notificationCount = alerts.size + (if (activeRole.canManageStaff) pendingApprovalsCount else 0),
                                onMenuClick = { isDrawerOpen = true },
                                onNotificationClick = { showNotificationsDialog = true },
                                onProfileClick = { navigateToRoute(AppRoute.Profile) }
                            )
                        }
                        AppRoute.Updates.key -> Unit
                        else -> {
                            BrohiSubScreenTopBar(
                                title = screenTitle,
                                onBackClick = {
                                    if (!navController.popBackStack()) {
                                        navigateToRoute(AppRoute.Dashboard)
                                    }
                                }
                            )
                        }
                    }
                },
            bottomBar = {
                BrohiBottomTaskbar(
                    currentRouteKey = currentRouteKey,
                    activeRole = activeRole,
                    strings = strings,
                    onNavigate = navigateToRoute,
                    onMicClick = {
                        viewModel.resetVoiceState()
                        speechManager.clearResult()
                        showVoiceSheet = true
                    },
                    onMenuClick = { isDrawerOpen = true }
                )
            }
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (!faceGate.isEnrolled && currentRouteKey != AppRoute.Updates.key) {
                    val remainingLabel = formatRemainingFaceDeadline(faceGate.remainingMillis)
                    Surface(
                        color = Color(0xFFFFF7ED),
                        border = BorderStroke(1.dp, WarningOrange.copy(alpha = 0.55f)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = WarningOrange,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Face Enrollment Mandatory ($remainingLabel left)",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = Color(0xFF9A3412)
                                )
                                Text(
                                    text = "چہرہ رجسٹر نہ کرنے پر 2 دن میں اکاؤنٹ دوبارہ بلاک ہو جائے گا",
                                    fontSize = 10.sp,
                                    color = Color(0xFF7C2D12)
                                )
                            }
                            IcyGlossyButton(
                                onClick = { showMandatoryFaceEnrollDialog = true },
                                tone = IcyGlossyTone.ORANGE,
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                                modifier = Modifier
                                    .heightIn(min = 42.dp)
                                    .testTag("banner_enroll_face_button")
                            ) {
                                Icon(Icons.Filled.CameraAlt, contentDescription = null, tint = Color.White, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Scan", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                NavHost(
                    navController = navController,
                    startDestination = AppRoute.Dashboard
                ) {
                    composable<AppRoute.Dashboard> {
                        val currentPrimaryEmployee = employees.firstOrNull { it.authUid == user.uid }
                            ?: employees.firstOrNull { it.id == user.uid }
                            ?: employees.firstOrNull()
                        DashboardScreen(
                            userName = displayName,
                            photoUrl = photoUrl,
                            activeRole = activeRole,
                            userProfile = userProfile,
                            primaryEmployee = currentPrimaryEmployee,
                            metrics = metrics,
                            accountingSummary = accountingSummary,
                            notices = notices,
                            myAttendance = attendance,
                            myPayroll = payroll,
                            pendingApprovalsCount = pendingApprovalsCount,
                            isUpdateAvailable = isUpdateAvailable,
                            appRelease = appRelease,
                            factorySettings = settings,
                            strings = strings,
                            onNavigate = navigateToRoute,
                            onProfileClick = { navigateToRoute(AppRoute.Profile) },
                            onOpenVoice = {
                                viewModel.resetVoiceState()
                                speechManager.clearResult()
                                showVoiceSheet = true
                            },
                            onOpenUpdateDialog = { showAppUpdateDialog = true },
                            onUpdateIdCardPhoto = viewModel::updateEmployeeIdCardPhoto,
                            onOpenFaceEnrollClick = { showMandatoryFaceEnrollDialog = true }
                        )
                    }
                    composable<AppRoute.Profile> {
                        val currentPrimaryEmployee = employees.firstOrNull { it.authUid == user.uid }
                            ?: employees.firstOrNull { it.id == user.uid }
                            ?: employees.firstOrNull()
                        UserProfileScreen(
                            userProfile = userProfile,
                            fallbackName = displayName,
                            fallbackEmail = user.email,
                            fallbackPhoto = photoUrl,
                            activeRole = activeRole,
                            primaryEmployee = currentPrimaryEmployee,
                            myProfileUpdateRequest = myProfileUpdateRequest,
                            onUpdateProfile = { name, phone, customPhoto, clearPhoto ->
                                viewModel.updateProfile(name, phone, customPhoto, clearPhoto)
                            },
                            onSubmitFullProfile = viewModel::submitOrSaveFullProfile
                        )
                    }
                    composable<AppRoute.UsersRoles> {
                        UsersRolesScreen(
                            activeRole = activeRole,
                            employees = employees,
                            registrationRequests = registrationRequests,
                            profileUpdateRequests = profileUpdateRequests,
                            onSaveEmployee = viewModel::saveEmployee,
                            onDeleteEmployee = viewModel::deleteEmployee,
                            onApproveRequest = viewModel::approveRegistrationRequest,
                            onRejectRequest = viewModel::rejectRegistrationRequest,
                            onDeleteRequest = viewModel::deleteRegistrationRequest,
                            onApproveProfileUpdate = viewModel::approveProfileUpdateRequest,
                            onRejectProfileUpdate = viewModel::rejectProfileUpdateRequest,
                            onDeleteProfileUpdate = viewModel::deleteProfileUpdateRequest,
                            onClearDemoData = viewModel::clearAllDemoData,
                            onGoToFaceEnroll = { navigateToRoute(AppRoute.FaceVerification) }
                        )
                    }
                    composable<AppRoute.Notices> {
                        NoticesScreen(
                            activeRole = activeRole,
                            notices = notices,
                            reporterName = displayName,
                            onSaveNotice = viewModel::saveNotice,
                            onDeleteNotice = viewModel::deleteNotice
                        )
                    }
                    composable<AppRoute.FaceVerification> {
                        FaceEnrollmentScreen(
                            employees = employees,
                            onSavePreEncryptedFaceTemplate = viewModel::savePreEncryptedFaceTemplate,
                            onSaveAttendanceFacePhoto = viewModel::updateEmployeeAttendanceFacePhoto,
                            onToggleSkipLiveness = viewModel::toggleEmployeeSkipLiveness
                        )
                    }
                    composable<AppRoute.Attendance> {
                        AttendanceScreen(
                            activeRole = activeRole,
                            currentUserName = displayName,
                            currentUserId = user.uid,
                            employees = employees,
                            attendanceRecords = attendance,
                            strings = strings,
                            onMatchFace = viewModel::matchFaceDescriptor,
                            onCheckIn = viewModel::checkIn,
                            onCheckOut = { rec, bypassed -> viewModel.checkOut(rec, bypassed) },
                            onSaveAttendanceRecord = viewModel::saveAttendanceRecord,
                            onDeleteAttendance = viewModel::deleteAttendance,
                            onSavePreEncryptedFaceTemplate = viewModel::savePreEncryptedFaceTemplate,
                            onSaveAttendanceFacePhoto = viewModel::updateEmployeeAttendanceFacePhoto
                        )
                    }
                    composable<AppRoute.Production> {
                        ProductionScreen(
                            activeRole = activeRole,
                            metrics = metrics,
                            productionRecords = production,
                            machines = machines,
                            onAddProduction = viewModel::addProduction,
                            onUpdateProduction = viewModel::updateProduction,
                            onDeleteProduction = viewModel::deleteProduction
                        )
                    }
                    composable<AppRoute.Inventory> {
                        InventoryColdStorageScreen(
                            activeRole = activeRole,
                            metrics = metrics,
                            movements = inventory,
                            onAddMovement = viewModel::addInventoryMovement,
                            onUpdateMovement = viewModel::updateInventoryMovement,
                            onDeleteMovement = viewModel::deleteInventoryMovement,
                            onViewExpensesFinance = { navigateToRoute(AppRoute.Expenses) }
                        )
                    }
                    composable<AppRoute.Sales> {
                        SalesInvoicingScreen(
                            activeRole = activeRole,
                            customers = customers,
                            sales = sales,
                            settings = settings,
                            onCreateSale = { cid, cname, prod, qty, price, tot, paid ->
                                viewModel.createSale(cid, cname, prod, qty, price, tot, paid)
                            },
                            onUpdateSale = viewModel::updateSale,
                            onDeleteSale = viewModel::deleteSale,
                            onMarkSalePaid = viewModel::markSalePaid
                        )
                    }
                    composable<AppRoute.Customers> {
                        CustomersCreditScreen(
                            activeRole = activeRole,
                            customers = customers,
                            sales = sales,
                            settings = settings,
                            onAddCustomer = viewModel::addCustomer,
                            onUpdateCustomer = viewModel::updateCustomer,
                            onDeleteCustomer = viewModel::deleteCustomer,
                            onReceivePayment = viewModel::receiveCustomerPayment
                        )
                    }
                    composable<AppRoute.Deliveries> {
                        DeliveriesVehiclesScreen(
                            activeRole = activeRole,
                            deliveries = deliveries,
                            onSaveDelivery = viewModel::saveDelivery,
                            onDeleteDelivery = viewModel::deleteDelivery
                        )
                    }
                    composable<AppRoute.Expenses> {
                        ExpensesFinanceScreen(
                            activeRole = activeRole,
                            expenses = expenses,
                            fundingList = funding,
                            summary = accountingSummary,
                            onAddExpense = { desc, amt, cat -> viewModel.addExpense(desc, amt, cat) },
                            onUpdateExpense = viewModel::updateExpense,
                            onDeleteExpense = viewModel::deleteExpense,
                            onAddFunding = viewModel::addFunding,
                            onUpdateFunding = viewModel::updateFunding,
                            onDeleteFunding = viewModel::deleteFunding,
                            onRepayFunding = viewModel::repayFunding,
                            sales = sales,
                            utilities = utilities,
                            payroll = payroll,
                            suppliers = suppliers,
                            settings = settings
                        )
                    }
                    composable<AppRoute.Salary> {
                        SalaryPayrollScreen(
                            activeRole = activeRole,
                            employees = employees,
                            payrollRecords = payroll,
                            onSavePayroll = viewModel::savePayroll,
                            onDeletePayroll = viewModel::deletePayroll,
                            onMarkPaid = viewModel::markPayrollPaid,
                            onSaveEmployee = viewModel::saveEmployee
                        )
                    }
                    composable<AppRoute.Utilities> {
                        UtilitiesScreen(
                            activeRole = activeRole,
                            readings = utilities,
                            onSaveUtility = viewModel::saveUtility,
                            onUpdateUtility = viewModel::updateUtility,
                            onDeleteUtility = viewModel::deleteUtility
                        )
                    }
                    composable<AppRoute.Maintenance> {
                        MaintenanceScreen(
                            activeRole = activeRole,
                            machines = machines,
                            onSaveMachine = viewModel::saveMachine,
                            onDeleteMachine = viewModel::deleteMachine
                        )
                    }
                    composable<AppRoute.Suppliers> {
                        SuppliersScreen(
                            activeRole = activeRole,
                            suppliers = suppliers,
                            onSaveSupplier = viewModel::saveSupplier,
                            onDeleteSupplier = viewModel::deleteSupplier,
                            onPaySupplier = viewModel::paySupplier
                        )
                    }
                    composable<AppRoute.Incidents> {
                        SafetyIncidentsScreen(
                            activeRole = activeRole,
                            incidents = incidents,
                            reporterName = displayName,
                            onSaveIncident = viewModel::saveIncident,
                            onDeleteIncident = viewModel::deleteIncident
                        )
                    }
                    composable<AppRoute.Reports> {
                        ReportsAnalyticsScreen(
                            sales = sales,
                            expenses = expenses,
                            summary = accountingSummary,
                            strings = strings,
                            fundingList = funding,
                            utilities = utilities,
                            payroll = payroll,
                            suppliers = suppliers,
                            settings = settings
                        )
                    }
                    composable<AppRoute.Settings> {
                        SettingsScreen(
                            settings = settings,
                            darkMode = prefs.darkMode,
                            language = prefs.language,
                            activeRole = activeRole,
                            canSwitchWorkspaceRole = canSwitchWorkspaceRole,
                            hasPendingCloudSync = hasPendingCloudSync,
                            appRelease = appRelease,
                            isUpdateAvailable = isUpdateAvailable,
                            isCheckingUpdate = isCheckingUpdate,
                            lastUpdateCheckedMessage = lastUpdateCheckedMessage,
                            updateDownloadState = updateDownloadState,
                            onToggleDarkMode = viewModel::setDarkMode,
                            onChangeLanguage = viewModel::setLanguage,
                            onChangeRole = viewModel::setActiveRole,
                            onSaveSettings = viewModel::saveFactorySettings,
                            onClearDemoData = viewModel::clearAllDemoData,
                            onCheckForUpdates = viewModel::checkForUpdatesNow,
                            onStartUpdateDownload = { viewModel.startApkUpdateDownload(context) },
                            onInstallOrGrantPermission = { viewModel.installOrGrantPermission(context) },
                            onPublishAppUpdate = viewModel::publishAppUpdate,
                            aiAuditEnabled = prefs.aiAuditEnabled,
                            aiAuditAutoWatch = prefs.aiAuditAutoWatch,
                            aiAuditPendingCount = aiAuditUiState.pendingCount,
                            aiAuditTotalCount = aiAuditUiState.findings.size,
                            onToggleAiAuditEnabled = viewModel::setAiAuditEnabled,
                            onToggleAiAuditAutoWatch = viewModel::setAiAuditAutoWatch,
                            onOpenAiAuditInbox = {
                                auditPanelInitialTab = 1
                                viewModel.openAuditPanel(currentRouteKey, screenTitle)
                            },
                            onClearAiAuditInbox = viewModel::clearAuditInbox,
                            heyIceWakeEnabled = prefs.heyIceWakeEnabled,
                            voiceWakeLang = prefs.voiceWakeLang,
                            onToggleHeyIceWake = viewModel::setHeyIceWakeEnabled,
                            onChangeVoiceWakeLang = { tag ->
                                speechManager.setLocaleTag(tag)
                                viewModel.setVoiceWakeLang(tag)
                            }
                        )
                    }
                    composable<AppRoute.About> {
                        AboutScreen(
                            appRelease = appRelease,
                            isUpdateAvailable = isUpdateAvailable,
                            isCheckingUpdate = isCheckingUpdate,
                            lastUpdateCheckedMessage = lastUpdateCheckedMessage,
                            updateDownloadState = updateDownloadState,
                            onCheckForUpdates = viewModel::checkForUpdatesNow,
                            onStartUpdateDownload = { viewModel.startApkUpdateDownload(context) },
                            onInstallOrGrantPermission = { viewModel.installOrGrantPermission(context) },
                            onOpenUpdatesScreen = { navigateToRoute(AppRoute.Updates) }
                        )
                    }
                    composable<AppRoute.Updates> {
                        AppUpdatesScreen(
                            appRelease = appRelease,
                            isUpdateAvailable = isUpdateAvailable,
                            isCheckingUpdate = isCheckingUpdate,
                            lastUpdateCheckedMessage = lastUpdateCheckedMessage,
                            updateDownloadState = updateDownloadState,
                            notificationCount = alerts.size + (if (activeRole.canManageStaff) pendingApprovalsCount else 0),
                            onBackClick = {
                                if (!navController.popBackStack()) {
                                    navigateToRoute(AppRoute.Dashboard)
                                }
                            },
                            onNotificationClick = { showNotificationsDialog = true },
                            onCheckForUpdates = viewModel::checkForUpdatesNow,
                            onStartUpdateDownload = { viewModel.startApkUpdateDownload(context) },
                            onInstallOrGrantPermission = { viewModel.installOrGrantPermission(context) }
                        )
                    }
                }
                }
            }
        }
        }
            }

            if (canSwitchWorkspaceRole && prefs.aiAuditEnabled && !aiAuditUiState.isPanelOpen && !isDrawerOpen) {
                FloatingAiAuditButton(
                    pendingCount = aiAuditUiState.pendingCount,
                    isScanning = aiAuditUiState.isAuditing,
                    autoWatchEnabled = prefs.aiAuditAutoWatch,
                    hiddenForCapture = aiAuditUiState.isCapturingSnapshot,
                    onClick = {
                        scope.launch {
                            auditPanelInitialTab = 0
                            val hasExistingForScreen = aiAuditUiState.findings.any { it.screenKey == currentRouteKey }
                            if (!hasExistingForScreen) {
                                viewModel.setAuditCapturingSnapshot(true)
                                delay(90L)
                                val bmp = ScreenSnapshotHelper.captureActivityWindowBitmap(activity)
                                viewModel.setAuditCapturingSnapshot(false)
                                viewModel.openAuditPanel(currentRouteKey, screenTitle)
                                viewModel.runInstantScreenAudit(currentRouteKey, screenTitle, activeRole, bmp)
                            } else {
                                viewModel.openAuditPanel(currentRouteKey, screenTitle)
                            }
                        }
                    }
                )
            }
        }

    if (showMandatoryFaceEnrollDialog) {
        val myEmp = employees.firstOrNull { it.authUid == user.uid }
            ?: employees.firstOrNull { it.id == user.uid }
        GuidedFaceEnrollmentDialog(
            ownerKey = viewModel.activeFactoryId,
            onDismiss = { showMandatoryFaceEnrollDialog = false },
            onPortraitCaptured = { portraitUri ->
                if (!portraitUri.isNullOrBlank()) {
                    viewModel.updateEmployeeAttendanceFacePhoto(myEmp?.id ?: user.uid, portraitUri)
                }
            },
            onCompleted = { encrypted ->
                showMandatoryFaceEnrollDialog = false
                viewModel.savePreEncryptedFaceTemplate(myEmp?.id ?: user.uid, encrypted)
            }
        )
    }

    if (showVoiceSheet) {
        VoiceCommandBottomSheet(
            speechState = speechUiState,
            voiceUiState = voiceUiState,
            userName = displayName,
            heyIceWakeEnabled = prefs.heyIceWakeEnabled,
            strings = strings,
            onSelectLocale = { tag ->
                speechManager.setLocaleTag(tag)
                viewModel.setVoiceWakeLang(tag)
            },
            onToggleAutoSave = viewModel::setVoiceAutoSave,
            onToggleHeyIceWake = viewModel::setHeyIceWakeEnabled,
            onStartListening = speechManager::startListening,
            onStopListening = speechManager::stopListening,
            onStopSpeaking = speechManager::stopSpeaking,
            onSubmitTranscript = { text ->
                val isOnline = LocalStorageBootstrap.isNetworkAvailable(context)
                viewModel.parseVoiceTranscript(
                    transcript = text,
                    isOnline = isOnline,
                    preferredLocaleTag = speechUiState.selectedLocaleTag,
                    userName = displayName
                ) { reply, lang, reopenMic ->
                    speechManager.speak(reply, lang) {
                        if (reopenMic) speechManager.startListening()
                    }
                }
            },
            onUpdateIntent = viewModel::updatePendingIntent,
            onRemoveIntent = viewModel::removePendingIntent,
            onConfirmAndSave = {
                viewModel.confirmAndSavePendingIntents { doneMsg, lang ->
                    speechManager.speak(doneMsg, lang)
                }
            },
            onCancelPending = viewModel::dismissPendingIntents,
            onDismiss = {
                speechManager.clearResult()
                showVoiceSheet = false
            }
        )
    }

    if (showNotificationsDialog) {
        CrystalFormDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = "Factory Alerts • فیکٹری الرٹس",
            subtitle = "Live operational & approval alerts",
            icon = Icons.Filled.Notifications,
            confirmText = "Got It",
            showDismissButton = false,
            onConfirm = { showNotificationsDialog = false }
        ) {
            if (isUpdateAvailable && appRelease != null) {
                Surface(
                    onClick = {
                        showNotificationsDialog = false
                        showAppUpdateDialog = true
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White,
                    border = BorderStroke(1.2.dp, RoyalBlue.copy(alpha = 0.45f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(RoyalBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Filled.SystemUpdate, contentDescription = null, tint = RoyalBlue, modifier = Modifier.size(19.dp))
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "App Update v${appRelease!!.latestVersionName} Available",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = DeepBlue
                            )
                            Text(
                                text = "نئی اپڈیٹ دستیاب ہے • Tap to install",
                                fontSize = 11.sp,
                                color = RoyalBlue
                            )
                        }
                    }
                }
            }

            if (alerts.isEmpty() && !isUpdateAvailable) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SuccessGreenSoft,
                    border = BorderStroke(1.dp, SuccessGreen.copy(alpha = 0.4f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "All Systems Healthy",
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                color = Color(0xFF0B5D29)
                            )
                            Text(
                                text = "All factory operations, machines, and stock levels look healthy!",
                                fontSize = 12.sp,
                                color = Color(0xFF166534)
                            )
                        }
                    }
                }
            } else {
                alerts.forEach { alert ->
                    val (alertIcon, tint, bgSoft) = when {
                        alert.startsWith("Notice:") -> Triple(Icons.Filled.Campaign, RoyalBlue, Color(0xFFEAF2FF))
                        alert.contains("approval", ignoreCase = true) -> Triple(Icons.Filled.HowToReg, WarningOrange, WarningOrangeSoft)
                        alert.contains("incident", ignoreCase = true) -> Triple(Icons.Filled.Warning, DangerRed, DangerRedSoft)
                        alert.contains("machine", ignoreCase = true) -> Triple(Icons.Filled.PrecisionManufacturing, WarningOrange, WarningOrangeSoft)
                        alert.contains("ice stock", ignoreCase = true) -> Triple(Icons.Filled.AcUnit, RoyalBlue, Color(0xFFE0F2FE))
                        else -> Triple(Icons.Filled.Warning, WarningOrange, WarningOrangeSoft)
                    }
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, tint.copy(alpha = 0.28f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(bgSoft),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(alertIcon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                            }
                            Text(
                                text = alert,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = DeepBlue,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAppUpdateDialog && appRelease != null) {
        val rel = appRelease!!
        val btnLabel = when (updateDownloadState) {
            is AppUpdateDownloadState.ReadyToInstall -> "Install APK"
            is AppUpdateDownloadState.Downloading -> "Downloading..."
            else -> "Update Now"
        }
        CrystalFormDialog(
            onDismissRequest = { if (!rel.mandatory) showAppUpdateDialog = false },
            title = "Updates Available • v${rel.latestVersionName}",
            subtitle = "Installed v${BuildConfig.VERSION_NAME} → Build ${rel.latestVersionCode}",
            icon = Icons.Filled.SystemUpdate,
            confirmText = btnLabel,
            confirmEnabled = updateDownloadState !is AppUpdateDownloadState.Downloading,
            showDismissButton = !rel.mandatory,
            dismissText = "Later",
            accentColor = SuccessGreen,
            confirmTestTag = "dialog_update_now_button",
            onConfirm = { viewModel.installOrGrantPermission(context) }
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CrystalBorderLight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Installed: v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE})  →  New: v${rel.latestVersionName} (Build ${rel.latestVersionCode})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = DeepBlue
                    )
                    if (rel.releaseNotes.isNotBlank()) {
                        Text(rel.releaseNotes, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
                    }
                    if (rel.releaseNotesUrdu.isNotBlank()) {
                        Text(rel.releaseNotesUrdu, fontSize = 12.5.sp, fontWeight = FontWeight.Medium, color = RoyalBlue)
                    }
                }
            }
            when (val st = updateDownloadState) {
                is AppUpdateDownloadState.Downloading -> {
                    IcySplashProgressBar(
                        progressPercent = st.progressPercent,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text("Downloading APK... ${st.progressPercent}%", fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold, color = DeepBlue)
                }
                is AppUpdateDownloadState.ReadyToInstall -> {
                    Text("APK ready! Tap Install APK below to apply update.", fontSize = 12.sp, color = SuccessGreen, fontWeight = FontWeight.Bold)
                }
                is AppUpdateDownloadState.Failed -> {
                    Text(st.message, fontSize = 12.sp, color = DangerRed, fontWeight = FontWeight.SemiBold)
                }
                AppUpdateDownloadState.Idle -> Unit
            }
        }
    }

    if (canSwitchWorkspaceRole && aiAuditUiState.isPanelOpen) {
        AiLiveAuditControlDialog(
            uiState = aiAuditUiState,
            autoWatchEnabled = prefs.aiAuditAutoWatch,
            explainLanguage = prefs.aiAuditExplainLanguage,
            initialTab = auditPanelInitialTab,
            onToggleAuditEnabled = viewModel::setAiAuditEnabled,
            onToggleAutoWatch = viewModel::setAiAuditAutoWatch,
            onChangeExplainLanguage = viewModel::setAiAuditExplainLanguage,
            onScanCurrentScreen = scanCurrentScreenNow,
            onUpdateFindingStatus = viewModel::updateAuditFindingStatus,
            onClearAllFindings = viewModel::clearAuditInbox,
            onDismiss = viewModel::dismissAuditPanel
        )
    }
    }
}
