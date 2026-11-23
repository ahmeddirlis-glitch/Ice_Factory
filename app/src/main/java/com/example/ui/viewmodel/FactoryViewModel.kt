package com.example.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.BuildConfig
import com.example.R
import com.example.data.model.AiAuditFinding
import com.example.data.model.AiAuditSerializer
import com.example.data.model.AppReleaseConfig
import com.example.data.model.AttendanceRecord
import com.example.data.model.AuditIssueStatus
import com.example.data.model.Customer
import com.example.data.model.DeliveryItem
import com.example.data.model.Employee
import com.example.data.model.Expense
import com.example.data.model.FaceEnrollmentGateState
import com.example.data.model.FactoryNotice
import com.example.data.model.FactorySettings
import com.example.data.model.FundingEntry
import com.example.data.model.InventoryMovement
import com.example.data.model.MachineItem
import com.example.data.model.OwnerAccountingSummary
import com.example.data.model.ParsedVoiceIntent
import com.example.data.model.PayrollRecord
import com.example.data.model.ProductionRecord
import com.example.data.model.ProfileUpdateRequest
import com.example.data.model.RegistrationRequest
import com.example.data.model.SafetyIncident
import com.example.data.model.Sale
import com.example.data.model.SupplierItem
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.example.data.model.UtilityReading
import com.example.data.model.canUserEnterMainWorkspace
import com.example.data.model.evaluateFaceEnrollmentGate
import com.example.data.model.isAppUpdateAvailable
import com.example.data.repository.AiUiAuditorRepository
import com.example.data.repository.FactoryRepository
import com.example.data.repository.VoiceAiRepository
import com.example.data.util.AppUpdateDownloadState
import com.example.data.util.AppUpdateManager
import com.example.data.util.FaceCryptoHelper
import com.example.data.util.PakistaniFormatters
import com.example.data.util.PreferencesManager
import com.example.data.util.RuntimeDiagnosticLogger
import com.example.ui.audit.AiAuditUiState
import com.example.ui.navigation.AppRoute
import kotlinx.coroutines.flow.first
import org.json.JSONObject
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private const val TAG = "FactoryViewModel"

data class DaySalesBar(
    val dayLabel: String,
    val isoDate: String,
    val amount: Double
)

data class DashboardMetrics(
    val todaySales: Double = 0.0,
    val todayExpenses: Double = 0.0,
    val todayProfit: Double = 0.0,
    val staffPresentCount: Int = 0,
    val totalStaffCount: Int = 0,
    val iceBlocksTons: Double = 0.0,
    val iceCubesTons: Double = 0.0,
    val totalStockTons: Double = 0.0,
    val blocksPercent: Float = 60f,
    val cubesPercent: Float = 40f,
    val salesTrend7Days: List<DaySalesBar> = emptyList(),
    val salesTrendPercentChange: Int = 12,
    val monthExpensesTotal: Double = 0.0,
    val todayProductionTons: Double = 0.0,
    val monthProductionTons: Double = 0.0
)

data class VoiceUiState(
    val isParsing: Boolean = false,
    val autoSaveEnabled: Boolean = false,
    val isSaving: Boolean = false,
    val lastNormalizedTranscript: String? = null,
    val pendingIntents: List<ParsedVoiceIntent> = emptyList(),
    val spokenAnswer: String? = null,
    val pendingNavigationKey: String? = null,
    val errorMessage: String? = null
)

fun canUserSwitchWorkspaceRole(profile: UserProfile?): Boolean {
    if (profile == null) return false
    return canUserEnterMainWorkspace(profile.approvalStatus) &&
        UserRole.fromRequestedRole(profile.requestedRole) == UserRole.OWNER
}

fun resolveEffectiveUserRole(
    ownerPreviewRole: UserRole?,
    profile: UserProfile?,
    hasPendingRegistration: Boolean
): UserRole {
    if (hasPendingRegistration || profile == null) return UserRole.STAFF
    if (!canUserEnterMainWorkspace(profile.approvalStatus)) return UserRole.STAFF
    val assignedRole = UserRole.fromRequestedRole(profile.requestedRole)
    return if (assignedRole == UserRole.OWNER) {
        ownerPreviewRole ?: UserRole.OWNER
    } else {
        assignedRole
    }
}

fun resolveEffectiveApprovalStatus(
    profile: UserProfile?,
    myRequest: RegistrationRequest?,
    isInitialized: Boolean,
    hasPendingRegistration: Boolean
): String {
    if (!isInitialized || hasPendingRegistration) return "LOADING"
    val profStatus = profile?.approvalStatus?.uppercase(Locale.US) ?: ""
    val reqStatus = myRequest?.status?.uppercase(Locale.US) ?: ""
    return when {
        profStatus == "APPROVED" || reqStatus == "APPROVED" -> "APPROVED"
        profStatus == "REJECTED" || reqStatus == "REJECTED" -> "REJECTED"
        else -> "PENDING"
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class FactoryViewModel(
    private val repository: FactoryRepository,
    private val voiceAiRepository: VoiceAiRepository,
    private val preferencesManager: PreferencesManager,
    val currentUserId: String,
    val currentUserName: String,
    val currentUserEmail: String?,
    val currentUserPhotoUrl: String?,
    private val aiUiAuditorRepository: AiUiAuditorRepository = AiUiAuditorRepository()
) : ViewModel() {

    private val _resolvedFactoryId = MutableStateFlow(currentUserId)
    private val _isProfileInitialized = MutableStateFlow(false)
    private val _isCheckingUpdate = MutableStateFlow(false)
    private val _lastUpdateCheckedMessage = MutableStateFlow<String?>(null)
    private val _updateDownloadState = MutableStateFlow<AppUpdateDownloadState>(AppUpdateDownloadState.Idle)
    private val _githubAppRelease = MutableStateFlow<AppReleaseConfig?>(null)

    val isCheckingUpdate: StateFlow<Boolean> = _isCheckingUpdate.asStateFlow()
    val lastUpdateCheckedMessage: StateFlow<String?> = _lastUpdateCheckedMessage.asStateFlow()
    val updateDownloadState: StateFlow<AppUpdateDownloadState> = _updateDownloadState.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    private val _voiceState = MutableStateFlow(VoiceUiState())
    val voiceState: StateFlow<VoiceUiState> = _voiceState.asStateFlow()

    private val _aiAuditState = MutableStateFlow(AiAuditUiState())
    private val autoWatchScreenTimestamps = mutableMapOf<String, Long>()

    val appPreferences = preferencesManager.preferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.example.data.util.AppPreferences())

    val aiAuditUiState: StateFlow<AiAuditUiState> = combine(_aiAuditState, appPreferences) { local, prefs ->
        val decodedFindings = AiAuditSerializer.decodeFindings(prefs.aiAuditFindingsJson)
        local.copy(
            isAutoWatchActive = prefs.aiAuditEnabled && prefs.aiAuditAutoWatch,
            findings = decodedFindings
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AiAuditUiState())

    private fun <T> safeListFlow(flow: kotlinx.coroutines.flow.Flow<List<T>>, label: String): StateFlow<List<T>> =
        flow.catch { error ->
            val fe = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fe == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing $label", fe ?: error)
            val errMsg = "Sync issue ($label): ${(fe ?: error).localizedMessage ?: "Offline mode active"}"
            RuntimeDiagnosticLogger.recordError("FirestoreSync", errMsg)
            _toastMessage.value = errMsg
            emit(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            if (error is CancellationException) throw error
            Log.w(TAG, "Error observing userProfile", error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeFactoryIdFlow: StateFlow<String> = combine(userProfile, _resolvedFactoryId) { profile, resolved ->
        profile?.linkedFactoryId?.takeIf { it.isNotBlank() } ?: resolved
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), currentUserId)

    val activeFactoryId: String
        get() = activeFactoryIdFlow.value

    val myRegistrationRequest: StateFlow<RegistrationRequest?> = activeFactoryIdFlow
        .flatMapLatest { fid -> repository.observeMyRegistrationRequest(fid, currentUserId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val approvalStatus: StateFlow<String> = combine(
        userProfile,
        myRegistrationRequest,
        _isProfileInitialized,
        appPreferences
    ) { profile, myReq, initialized, prefs ->
        resolveEffectiveApprovalStatus(
            profile = profile,
            myRequest = myReq,
            isInitialized = initialized,
            hasPendingRegistration = prefs.pendingRegistrationJson.isNotBlank()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "LOADING")

    val isApprovedUser: StateFlow<Boolean> = approvalStatus
        .map { canUserEnterMainWorkspace(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appRelease: StateFlow<AppReleaseConfig?> = combine(
        _githubAppRelease,
        repository.observeLatestAppRelease()
    ) { githubRel, firestoreRel ->
        AppUpdateManager.selectLatestRelease(githubRel, firestoreRel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isUpdateAvailable: StateFlow<Boolean> = appRelease
        .map { release -> isAppUpdateAvailable(BuildConfig.VERSION_CODE, release) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val activeRole: StateFlow<UserRole> = combine(appPreferences, userProfile, myRegistrationRequest) { prefs, profile, myReq ->
        val effectiveProfile = when {
            profile != null && myReq != null &&
                myReq.status.equals("APPROVED", ignoreCase = true) &&
                !profile.approvalStatus.equals("APPROVED", ignoreCase = true) -> {
                profile.copy(
                    approvalStatus = "APPROVED",
                    requestedRole = myReq.requestedRole.ifBlank { profile.requestedRole }
                )
            }
            else -> profile
        }
        resolveEffectiveUserRole(
            ownerPreviewRole = prefs.ownerPreviewRole,
            profile = effectiveProfile,
            hasPendingRegistration = prefs.pendingRegistrationJson.isNotBlank()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserRole.STAFF)

    val canSwitchWorkspaceRole: StateFlow<Boolean> = combine(userProfile, approvalStatus) { profile, status ->
        canUserSwitchWorkspaceRole(profile?.copy(approvalStatus = status))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val hasPendingCloudSync: StateFlow<Boolean> = combine(isApprovedUser, activeFactoryIdFlow) { approved, fid ->
        approved to fid
    }.flatMapLatest { (approved, fid) ->
        if (!approved) flowOf(false)
        else repository.observeHasPendingSync(fid)
    }.catch { error ->
        if (error is CancellationException) throw error
        emit(false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val settings: StateFlow<FactorySettings> = combine(isApprovedUser, activeFactoryIdFlow) { approved, fid ->
        approved to fid
    }.flatMapLatest { (approved, fid) ->
        if (approved) {
            repository.observeSettings(fid).map { it ?: FactorySettings(ownerId = fid) }
        } else {
            flowOf(FactorySettings(ownerId = fid))
        }
    }.catch { error ->
        if (error is CancellationException) throw error
        emit(FactorySettings(ownerId = activeFactoryId))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FactorySettings(ownerId = currentUserId))

    val employees: StateFlow<List<Employee>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (!approved) {
            flowOf(emptyList())
        } else if (role.canManageStaff) {
            repository.observeEmployees(fid, currentUserId, canManageAll = true)
        } else {
            repository.observeEmployees(fid, currentUserId, canManageAll = false)
        }
    }.let { safeListFlow(it, "employees") }

    val faceEnrollmentGate: StateFlow<FaceEnrollmentGateState> = combine(
        isApprovedUser,
        userProfile,
        employees,
        myRegistrationRequest
    ) { approved, profile, empList, myReq ->
        val myEmp = empList.firstOrNull { it.authUid == currentUserId }
            ?: empList.firstOrNull { it.id == currentUserId }
        val hasFace = !myEmp?.faceTemplateEncrypted.isNullOrBlank() ||
            !myReq?.faceTemplateEncrypted.isNullOrBlank() ||
            (profile?.faceEnrolled == true)
        val approvedMillis = profile?.approvedAtMillis
            ?: myReq?.updatedAt?.toDate()?.time
            ?: myEmp?.createdAt?.toDate()?.time
            ?: profile?.createdAt?.toDate()?.time
        evaluateFaceEnrollmentGate(
            isApproved = approved,
            hasEnrolledFace = hasFace,
            approvedAtMillis = approvedMillis
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FaceEnrollmentGateState())

    val registrationRequests: StateFlow<List<RegistrationRequest>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (approved && role.canManageStaff) {
            repository.observeRegistrationRequests(fid)
        } else {
            flowOf(emptyList())
        }
    }.let { safeListFlow(it, "registration_requests") }

    val profileUpdateRequests: StateFlow<List<ProfileUpdateRequest>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (approved) {
            repository.observeProfileUpdateRequests(
                factoryId = fid,
                callerUid = currentUserId,
                canManageAll = role.canManageStaff
            )
        } else {
            flowOf(emptyList())
        }
    }.let { safeListFlow(it, "profile_update_requests") }

    val myProfileUpdateRequest: StateFlow<ProfileUpdateRequest?> = profileUpdateRequests
        .map { list -> list.firstOrNull { it.applicantUid == currentUserId || it.id == currentUserId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val notices: StateFlow<List<FactoryNotice>> = combine(isApprovedUser, activeFactoryIdFlow) { approved, fid ->
        approved to fid
    }.flatMapLatest { (approved, fid) ->
        if (approved) repository.observeNotices(fid) else flowOf(emptyList())
    }.let { safeListFlow(it, "notices") }

    val attendance: StateFlow<List<AttendanceRecord>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (!approved) flowOf(emptyList())
        else repository.observeAttendance(fid, currentUserId, canReadAll = (role != UserRole.STAFF))
    }.let { safeListFlow(it, "attendance") }

    val production: StateFlow<List<ProductionRecord>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (!approved || role == UserRole.STAFF) flowOf(emptyList())
        else repository.observeProduction(fid, currentUserId, canReadAll = true)
    }.let { safeListFlow(it, "production") }

    val inventory: StateFlow<List<InventoryMovement>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (!approved || role == UserRole.STAFF) flowOf(emptyList())
        else repository.observeInventory(fid, currentUserId, canReadAll = true)
    }.let { safeListFlow(it, "inventory") }

    val customers: StateFlow<List<Customer>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (!approved || role == UserRole.STAFF) flowOf(emptyList())
        else repository.observeCustomers(fid, currentUserId, canReadAll = true)
    }.let { safeListFlow(it, "customers") }

    val sales: StateFlow<List<Sale>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (!approved || role == UserRole.STAFF) flowOf(emptyList())
        else repository.observeSales(fid, currentUserId, canReadAll = true)
    }.let { safeListFlow(it, "sales") }

    val expenses: StateFlow<List<Expense>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (!approved || role == UserRole.STAFF) {
            flowOf(emptyList())
        } else {
            repository.observeExpenses(fid, currentUserId, isOwner = role.isOwner, canReadOperatingExpenses = true)
                .map { list -> if (role.isOwner) list else list.filter { !it.capitalInvestment } }
        }
    }.let { safeListFlow(it, "expenses") }

    val funding: StateFlow<List<FundingEntry>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (approved && role.isOwner) repository.observeFunding(fid) else flowOf(emptyList())
    }.let { safeListFlow(it, "funding") }

    val payroll: StateFlow<List<PayrollRecord>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        when {
            !approved -> flowOf(emptyList())
            role.isOwner -> repository.observePayroll(fid, currentUserId, isOwner = true)
            else -> repository.observePayroll(fid, currentUserId, isOwner = false)
        }
    }.let { safeListFlow(it, "payroll") }

    val utilities: StateFlow<List<UtilityReading>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (!approved || role == UserRole.STAFF) flowOf(emptyList())
        else repository.observeUtilities(fid, currentUserId, canReadAll = true)
    }.let { safeListFlow(it, "utilities") }

    val deliveries: StateFlow<List<DeliveryItem>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (!approved || role == UserRole.STAFF) flowOf(emptyList())
        else repository.observeDeliveries(fid, currentUserId, canReadAll = true)
    }.let { safeListFlow(it, "deliveries") }

    val machines: StateFlow<List<MachineItem>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (!approved || role == UserRole.STAFF) flowOf(emptyList())
        else repository.observeMachines(fid, currentUserId, canReadAll = true)
    }.let { safeListFlow(it, "machines") }

    val suppliers: StateFlow<List<SupplierItem>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (!approved || role == UserRole.STAFF) flowOf(emptyList())
        else repository.observeSuppliers(fid, currentUserId, canReadAll = true)
    }.let { safeListFlow(it, "suppliers") }

    val incidents: StateFlow<List<SafetyIncident>> = combine(
        isApprovedUser,
        activeFactoryIdFlow,
        activeRole
    ) { approved, fid, role ->
        Triple(approved, fid, role)
    }.flatMapLatest { (approved, fid, role) ->
        if (!approved) flowOf(emptyList())
        else repository.observeIncidents(fid, currentUserId, canReadAll = (role != UserRole.STAFF))
    }.let { safeListFlow(it, "incidents") }

    val dashboardMetrics: StateFlow<DashboardMetrics> = combine(
        sales,
        expenses,
        attendance,
        employees,
        inventory
    ) { salesList, expenseList, attList, empList, invList ->
        computeBaseMetrics(salesList, expenseList, attList, empList, invList, emptyList())
    }.combine(production) { base, prodList ->
        val today = PakistaniFormatters.todayIso()
        val month = PakistaniFormatters.currentMonthIso()
        val todayProd = prodList.filter { it.date == today }.sumOf { it.tons }
        val monthProd = prodList.filter { it.date.startsWith(month) }.sumOf { it.tons }
        base.copy(todayProductionTons = todayProd, monthProductionTons = monthProd)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardMetrics())

    val ownerAccountingSummary: StateFlow<OwnerAccountingSummary> = combine(
        funding,
        expenses,
        sales,
        customers,
        settings
    ) { fundingList, expenseList, salesList, customerList, cfg ->
        computeOwnerAccounting(fundingList, expenseList, salesList, customerList, cfg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), OwnerAccountingSummary())

    init {
        viewModelScope.launch {
            try {
                val formattedUser = PakistaniFormatters.formatTitleCase(currentUserName.ifBlank { "Factory User" })
                val prefsNow = preferencesManager.preferencesFlow.first()
                val pendingJson = prefsNow.pendingRegistrationJson
                if (pendingJson.isNotBlank()) {
                    preferencesManager.setOwnerPreviewRole(null)
                    val obj = JSONObject(pendingJson)
                    val regName = PakistaniFormatters.formatTitleCase(obj.optString("name", formattedUser))
                    val rawReqRole = obj.optString("requestedRole", "Staff")
                    val normalizedRole = UserRole.fromRequestedRole(rawReqRole)
                    val safeRequestedRole = normalizedRole.label

                    val (factoryId, claimedPrimaryOwner) = repository.resolveOrClaimPrimaryFactory(
                        callerUid = currentUserId,
                        callerName = regName,
                        allowClaimIfMissing = (normalizedRole == UserRole.OWNER)
                    )
                    _resolvedFactoryId.value = factoryId

                    if (claimedPrimaryOwner && normalizedRole == UserRole.OWNER) {
                        val ownerFace = obj.optString("faceTemplateEncrypted").takeIf { it.isNotBlank() }
                        val ownerPhone = PakistaniFormatters.formatPhone(obj.optString("phone", ""))
                        val ownerCnic = PakistaniFormatters.formatCnic(obj.optString("cnic", ""))
                        repository.upsertUserProfile(
                            userId = currentUserId,
                            displayName = regName,
                            email = obj.optString("email", currentUserEmail ?: "").ifBlank { currentUserEmail },
                            phone = ownerPhone.ifBlank { null },
                            photoUrl = currentUserPhotoUrl,
                            preferredLanguage = prefsNow.language,
                            darkMode = prefsNow.darkMode,
                            approvalStatus = "APPROVED",
                            requestedRole = "Super Admin",
                            linkedFactoryId = factoryId,
                            faceEnrolled = !ownerFace.isNullOrBlank(),
                            approvedAtMillis = System.currentTimeMillis()
                        )
                        repository.saveEmployee(
                            factoryId = factoryId,
                            employee = Employee(
                                id = currentUserId,
                                ownerId = factoryId,
                                authUid = currentUserId,
                                name = regName,
                                phone = ownerPhone,
                                cnic = ownerCnic,
                                role = "Super Admin",
                                salary = 0.0,
                                faceTemplateEncrypted = ownerFace,
                                active = true
                            )
                        )
                        repository.ensureDemoSeeded(factoryId = factoryId, ownerName = regName)
                        runCatching {
                            repository.ensureLatestReleasePublished(
                                versionCode = BuildConfig.VERSION_CODE,
                                versionName = BuildConfig.VERSION_NAME,
                                apkUrl = AppUpdateManager.DEFAULT_APK_DOWNLOAD_URL,
                                notesEn = "New Biometric Camera Face Registration and Attendance with Liveness Protection, Two-Sided Digital CNIC Card, and Live Alerts.",
                                notesUr = "کیمرہ فیس رجسٹریشن اور اٹینڈنس تصدیق، ڈیجیٹل شناختی کارڈ، اور الرٹس۔",
                                publisherName = regName
                            )
                        }
                        _toastMessage.value = "Primary Super Admin workspace activated with Face Biometrics!"
                    } else {
                        val faceEnc = obj.optString("faceTemplateEncrypted").takeIf { it.isNotBlank() }
                        val req = RegistrationRequest(
                            ownerId = factoryId,
                            applicantUid = currentUserId,
                            name = regName,
                            fatherName = PakistaniFormatters.formatTitleCase(obj.optString("fatherName", "")),
                            cnic = PakistaniFormatters.formatCnic(obj.optString("cnic", "")),
                            phone = PakistaniFormatters.formatPhone(obj.optString("phone", "")),
                            whatsappNumber = PakistaniFormatters.formatPhone(obj.optString("whatsappNumber", "")),
                            email = obj.optString("email", currentUserEmail ?: ""),
                            emergencyContact = PakistaniFormatters.formatPhone(obj.optString("emergencyContact", "")),
                            requestedRole = safeRequestedRole,
                            shift = obj.optString("shift", "Morning (8AM - 4PM)"),
                            faceTemplateEncrypted = faceEnc
                        )
                        repository.upsertUserProfile(
                            userId = currentUserId,
                            displayName = regName,
                            email = req.email.ifBlank { currentUserEmail },
                            phone = req.phone.ifBlank { null },
                            photoUrl = currentUserPhotoUrl,
                            preferredLanguage = prefsNow.language,
                            darkMode = prefsNow.darkMode,
                            approvalStatus = "PENDING",
                            requestedRole = req.requestedRole,
                            linkedFactoryId = factoryId,
                            faceEnrolled = !faceEnc.isNullOrBlank()
                        )
                        repository.submitRegistrationRequest(factoryId, currentUserId, req)
                        preferencesManager.setPendingRegistrationJson("")
                        _toastMessage.value = "Registration sent! Waiting for Super Admin approval."
                    }
                } else {
                    val existingProfile = repository.getUserProfileServerOrLocal(currentUserId)
                    val (factoryId, isPrimaryOwner) = repository.resolveOrClaimPrimaryFactory(
                        callerUid = currentUserId,
                        callerName = formattedUser,
                        allowClaimIfMissing = (existingProfile == null || existingProfile.linkedFactoryId.isNullOrBlank())
                    )
                    val effectiveFactoryId = existingProfile?.linkedFactoryId?.takeIf { it.isNotBlank() } ?: factoryId
                    _resolvedFactoryId.value = effectiveFactoryId

                    if (existingProfile == null) {
                        val initialApproval = if (isPrimaryOwner) "APPROVED" else "PENDING"
                        val initialRole = if (isPrimaryOwner) "Super Admin" else "Staff"
                        repository.upsertUserProfile(
                            userId = currentUserId,
                            displayName = formattedUser,
                            email = currentUserEmail,
                            phone = null,
                            photoUrl = currentUserPhotoUrl,
                            preferredLanguage = prefsNow.language,
                            darkMode = prefsNow.darkMode,
                            approvalStatus = initialApproval,
                            requestedRole = initialRole,
                            linkedFactoryId = effectiveFactoryId,
                            faceEnrolled = false,
                            approvedAtMillis = if (isPrimaryOwner) System.currentTimeMillis() else null
                        )
                        if (isPrimaryOwner) {
                            repository.ensureDemoSeeded(factoryId = effectiveFactoryId, ownerName = formattedUser)
                        }
                    } else {
                        val serverReq = if (!canUserEnterMainWorkspace(existingProfile.approvalStatus)) {
                            repository.getMyRegistrationRequestServerOrLocal(effectiveFactoryId, currentUserId)
                        } else null
                        val syncedStatus = when {
                            isPrimaryOwner -> "APPROVED"
                            serverReq?.status.equals("APPROVED", ignoreCase = true) -> "APPROVED"
                            serverReq?.status.equals("REJECTED", ignoreCase = true) -> "REJECTED"
                            else -> existingProfile.approvalStatus.ifBlank { "PENDING" }
                        }
                        val syncedRole = when {
                            isPrimaryOwner -> "Super Admin"
                            serverReq?.status.equals("APPROVED", ignoreCase = true) && !serverReq?.requestedRole.isNullOrBlank() ->
                                serverReq!!.requestedRole
                            else -> existingProfile.requestedRole.ifBlank { "Staff" }
                        }
                        val resolvedApprovedAt = existingProfile.approvedAtMillis ?: if (syncedStatus == "APPROVED") {
                            serverReq?.updatedAt?.toDate()?.time
                                ?: existingProfile.createdAt?.toDate()?.time
                                ?: System.currentTimeMillis()
                        } else null
                        repository.upsertUserProfile(
                            userId = currentUserId,
                            displayName = existingProfile.displayName.ifBlank { formattedUser },
                            email = existingProfile.email ?: currentUserEmail,
                            phone = existingProfile.phone,
                            photoUrl = existingProfile.photoUrl ?: currentUserPhotoUrl,
                            preferredLanguage = prefsNow.language,
                            darkMode = prefsNow.darkMode,
                            approvalStatus = syncedStatus,
                            requestedRole = syncedRole,
                            linkedFactoryId = effectiveFactoryId,
                            approvedAtMillis = resolvedApprovedAt
                        )
                        if (isPrimaryOwner || syncedStatus == "APPROVED") {
                            if (effectiveFactoryId == currentUserId) {
                                repository.ensureDemoSeeded(factoryId = effectiveFactoryId, ownerName = formattedUser)
                            }
                            if (isPrimaryOwner || syncedRole == "Super Admin") {
                                runCatching {
                                    repository.ensureLatestReleasePublished(
                                        versionCode = BuildConfig.VERSION_CODE,
                                        versionName = BuildConfig.VERSION_NAME,
                                        apkUrl = AppUpdateManager.DEFAULT_APK_DOWNLOAD_URL,
                                        notesEn = "New Two-Sided Digital CNIC Card with QR Code, Exact Notification Badge Count, 5s Splash Screen with SHIPDAR & TECH branding, and Enhanced Security.",
                                        notesUr = "نیا ٹو سائیڈڈ ڈیجیٹل شناختی کارڈ مع کیو آر کوڈ، نوٹیفکیشن بیج کاؤنٹ، برانڈڈ سپلیش اسکرین اور سیکیورٹی اپڈیٹس۔",
                                        publisherName = formattedUser
                                    )
                                }
                            }
                        }
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Initial sync setup warning: ${e.message}")
            } finally {
                _isProfileInitialized.value = true
            }
        }
        refreshGitHubReleaseQuietly()

        // Enforce mandatory 2-day (48h) face enrollment auto-block & re-approval rule
        viewModelScope.launch {
            var autoBlockedOnce = false
            combine(
                _isProfileInitialized,
                isApprovedUser,
                faceEnrollmentGate,
                userProfile,
                employees
            ) { initialized, approved, gate, profile, empList ->
                listOf(initialized, approved, gate, profile, empList)
            }.collect { values ->
                val initialized = values[0] as Boolean
                val approved = values[1] as Boolean
                val gate = values[2] as FaceEnrollmentGateState
                val profile = values[3] as UserProfile?
                @Suppress("UNCHECKED_CAST")
                val empList = values[4] as List<Employee>
                if (!initialized || !approved || autoBlockedOnce) return@collect
                if (currentUserId == activeFactoryId) return@collect
                if (!gate.isEnrolled && gate.isOverdue && profile != null) {
                    val myEmp = empList.firstOrNull { it.authUid == currentUserId }
                        ?: empList.firstOrNull { it.id == currentUserId }
                    if (!myEmp?.faceTemplateEncrypted.isNullOrBlank()) return@collect
                    autoBlockedOnce = true
                    runCatching {
                        repository.autoBlockForMissingFaceEnrollment(
                            factoryId = activeFactoryId,
                            uid = currentUserId,
                            displayName = myEmp?.name?.ifBlank { profile.displayName } ?: profile.displayName.ifBlank { currentUserName },
                            fatherName = myEmp?.fatherName ?: "",
                            cnic = myEmp?.cnic ?: "",
                            phone = myEmp?.phone ?: (profile.phone ?: ""),
                            whatsappNumber = myEmp?.whatsappNumber ?: "",
                            email = myEmp?.email?.ifBlank { currentUserEmail ?: "" } ?: (profile.email ?: currentUserEmail ?: ""),
                            emergencyContact = myEmp?.emergencyContact ?: "",
                            requestedRole = myEmp?.role?.ifBlank { profile.requestedRole } ?: profile.requestedRole,
                            shift = myEmp?.shift ?: "Morning (8AM - 4PM)"
                        )
                    }.onSuccess {
                        _toastMessage.value = "Account auto-blocked: Face enrollment was not completed within 2 days. Sent to Super Admin & Admin for approval."
                    }.onFailure {
                        autoBlockedOnce = false
                    }
                }
            }
        }
    }

    fun refreshApprovalStatus() = viewModelScope.launch {
        runCatching {
            val profile = repository.getUserProfileServerOrLocal(currentUserId)
            val fid = profile?.linkedFactoryId?.takeIf { it.isNotBlank() } ?: activeFactoryId
            _resolvedFactoryId.value = fid
            val myReq = repository.getMyRegistrationRequestServerOrLocal(fid, currentUserId)
            if (profile != null && myReq != null && myReq.status.equals("APPROVED", ignoreCase = true) &&
                !profile.approvalStatus.equals("APPROVED", ignoreCase = true)
            ) {
                repository.upsertUserProfile(
                    userId = currentUserId,
                    displayName = profile.displayName.ifBlank { currentUserName },
                    email = profile.email ?: currentUserEmail,
                    phone = profile.phone,
                    photoUrl = currentUserPhotoUrl,
                    preferredLanguage = appPreferences.value.language,
                    darkMode = appPreferences.value.darkMode,
                    approvalStatus = "APPROVED",
                    requestedRole = myReq.requestedRole.ifBlank { profile.requestedRole },
                    linkedFactoryId = fid
                )
            }
        }.onSuccess {
            val st = approvalStatus.value
            _toastMessage.value = when (st) {
                "APPROVED" -> "Account approved! Entering Brohi Ice Factory..."
                "REJECTED" -> "Request declined by Super Admin."
                else -> "Still waiting for Super Admin approval."
            }
        }.onFailure {
            _toastMessage.value = "Could not check approval status: ${it.localizedMessage}"
        }
    }

    fun submitRegistrationFromGate(
        fullName: String,
        fatherName: String,
        cnic: String,
        phone: String,
        whatsappNumber: String,
        emergencyContact: String,
        requestedRole: String,
        shift: String,
        faceTemplateEncrypted: String? = null
    ) = viewModelScope.launch {
        runCatching {
            val cleanName = PakistaniFormatters.formatTitleCase(fullName.ifBlank { currentUserName })
            val normalizedRole = UserRole.fromRequestedRole(requestedRole)
            val safeRole = normalizedRole.label
            val (fid, claimedOwner) = repository.resolveOrClaimPrimaryFactory(
                callerUid = currentUserId,
                callerName = cleanName,
                allowClaimIfMissing = (normalizedRole == UserRole.OWNER)
            )
            _resolvedFactoryId.value = fid
            val existingFace = faceTemplateEncrypted?.takeIf { it.isNotBlank() }
                ?: myRegistrationRequest.value?.faceTemplateEncrypted
            if (claimedOwner && normalizedRole == UserRole.OWNER) {
                repository.upsertUserProfile(
                    userId = currentUserId,
                    displayName = cleanName,
                    email = currentUserEmail,
                    phone = PakistaniFormatters.formatPhone(phone).ifBlank { null },
                    photoUrl = currentUserPhotoUrl,
                    preferredLanguage = appPreferences.value.language,
                    darkMode = appPreferences.value.darkMode,
                    approvalStatus = "APPROVED",
                    requestedRole = "Super Admin",
                    linkedFactoryId = fid,
                    faceEnrolled = !existingFace.isNullOrBlank(),
                    approvedAtMillis = System.currentTimeMillis()
                )
                repository.ensureDemoSeeded(fid, cleanName)
            } else {
                val req = RegistrationRequest(
                    name = cleanName,
                    fatherName = PakistaniFormatters.formatTitleCase(fatherName),
                    cnic = PakistaniFormatters.formatCnic(cnic),
                    phone = PakistaniFormatters.formatPhone(phone),
                    whatsappNumber = PakistaniFormatters.formatPhone(whatsappNumber),
                    email = currentUserEmail ?: "",
                    emergencyContact = PakistaniFormatters.formatPhone(emergencyContact),
                    requestedRole = safeRole,
                    shift = shift,
                    reviewNote = if (!existingFace.isNullOrBlank()) "Face biometrics completed & awaiting approval" else null,
                    faceTemplateEncrypted = existingFace
                )
                repository.upsertUserProfile(
                    userId = currentUserId,
                    displayName = cleanName,
                    email = currentUserEmail,
                    phone = req.phone.ifBlank { null },
                    photoUrl = currentUserPhotoUrl,
                    preferredLanguage = appPreferences.value.language,
                    darkMode = appPreferences.value.darkMode,
                    approvalStatus = "PENDING",
                    requestedRole = safeRole,
                    linkedFactoryId = fid,
                    faceEnrolled = !existingFace.isNullOrBlank()
                )
                repository.submitRegistrationRequest(fid, currentUserId, req)
            }
        }.onSuccess {
            _toastMessage.value = "Registration request updated & sent for approval."
        }.onFailure {
            _toastMessage.value = "Registration submit failed: ${it.localizedMessage}"
        }
    }

    private fun computeBaseMetrics(
        salesList: List<Sale>,
        expenseList: List<Expense>,
        attList: List<AttendanceRecord>,
        empList: List<Employee>,
        invList: List<InventoryMovement>,
        prodList: List<ProductionRecord>
    ): DashboardMetrics {
        val today = PakistaniFormatters.todayIso()
        val month = PakistaniFormatters.currentMonthIso()

        val todaySales = salesList.filter { it.date == today }.sumOf { it.total }
        val operatingExpenses = expenseList.filter { !it.capitalInvestment }
        val todayExpenses = operatingExpenses.filter { it.date == today }.sumOf { it.amount }
        val todayProfit = todaySales - todayExpenses
        val monthExpenses = operatingExpenses.filter { it.date.startsWith(month) }.sumOf { it.amount }

        val todayAtt = attList.filter { it.date == today }
        val presentToday = todayAtt.filter { it.status == "Present" || it.status == "Checked Out" }
            .map { it.employeeId.ifBlank { it.employeeName } }
            .distinct()
            .size
        val totalStaff = empList.count { it.active }.coerceAtLeast(
            todayAtt.map { it.employeeId.ifBlank { it.employeeName } }.distinct().size
        )

        var blocks = 0.0
        var cubes = 0.0
        for (m in invList) {
            val delta = if (m.movementType.equals("OUT", ignoreCase = true)) -m.tons else m.tons
            if (m.product.contains("Cube", ignoreCase = true)) {
                cubes += delta
            } else {
                blocks += delta
            }
        }
        blocks = blocks.coerceAtLeast(0.0)
        cubes = cubes.coerceAtLeast(0.0)
        val totalStock = blocks + cubes
        val blocksPct = if (totalStock > 0) ((blocks / totalStock) * 100f).toFloat() else 60f
        val cubesPct = if (totalStock > 0) (100f - blocksPct) else 40f

        val trend = (6 downTo 0).map { offset ->
            val iso = PakistaniFormatters.daysAgoIso(offset)
            val total = salesList.filter { it.date == iso }.sumOf { it.total }
            DaySalesBar(
                dayLabel = PakistaniFormatters.dayShortLabel(iso),
                isoDate = iso,
                amount = total
            )
        }
        val prevAvg = trend.dropLast(1).map { it.amount }.average().takeIf { !it.isNaN() && it > 0.0 } ?: 1.0
        val pctChange = (((trend.lastOrNull()?.amount ?: 0.0) - prevAvg) / prevAvg * 100.0).roundToInt()

        return DashboardMetrics(
            todaySales = todaySales,
            todayExpenses = todayExpenses,
            todayProfit = todayProfit,
            staffPresentCount = presentToday,
            totalStaffCount = totalStaff,
            iceBlocksTons = blocks,
            iceCubesTons = cubes,
            totalStockTons = totalStock,
            blocksPercent = blocksPct,
            cubesPercent = cubesPct,
            salesTrend7Days = trend,
            salesTrendPercentChange = pctChange,
            monthExpensesTotal = monthExpenses
        )
    }

    private fun computeOwnerAccounting(
        fundingList: List<FundingEntry>,
        expenseList: List<Expense>,
        salesList: List<Sale>,
        customerList: List<Customer>,
        cfg: FactorySettings
    ): OwnerAccountingSummary {
        val totalFunding = fundingList.sumOf { it.amount }
        val totalRepaid = fundingList.sumOf { it.repaid }
        val stillToRepay = (totalFunding - totalRepaid).coerceAtLeast(0.0)
        val factoryInvestment = expenseList.filter { it.capitalInvestment }.sumOf { it.amount }
        val totalSales = salesList.sumOf { it.total }
        val creditPending = customerList.sumOf { it.balance }
        val operatingExpenses = expenseList.filter { !it.capitalInvestment }.sumOf { it.amount }
        val regularProfit = totalSales - operatingExpenses

        val stillToRecover = (factoryInvestment - regularProfit).coerceAtLeast(0.0)
        val recoveryPercent = if (factoryInvestment > 0.0) {
            ((regularProfit / factoryInvestment).toFloat().coerceIn(0f, 1f)) * 100f
        } else 100f

        val months = PakistaniFormatters.monthsSince(cfg.operationsStartDate)
        val avgMonthlyProfit = regularProfit / months
        val breakEvenReached = factoryInvestment > 0.0 && regularProfit >= factoryInvestment
        val isProfitable = avgMonthlyProfit > 0.0
        val estMonths = if (isProfitable && !breakEvenReached) stillToRecover / avgMonthlyProfit else 0.0
        val label = when {
            breakEvenReached -> "Break-even reached. Profit from here is regular profit"
            !isProfitable -> "Not profitable yet"
            else -> "~${String.format(Locale.US, "%.1f", estMonths)} months (${PakistaniFormatters.estimatedCompletionMonthYear(estMonths)})"
        }

        return OwnerAccountingSummary(
            totalFundingRaised = totalFunding,
            stillToRepay = stillToRepay,
            factoryInvestment = factoryInvestment,
            totalSalesSoFar = totalSales,
            creditPendingFromCustomers = creditPending,
            totalOperatingExpenses = operatingExpenses,
            regularProfitSoFar = regularProfit,
            stillToRecover = stillToRecover,
            recoveryPercent = recoveryPercent,
            averageMonthlyProfit = avgMonthlyProfit,
            estimatedMonthsToBreakEven = estMonths,
            estimatedCompletionLabel = label,
            isProfitable = isProfitable,
            isBreakEvenReached = breakEvenReached
        )
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun showToast(msg: String) {
        if (msg.contains("fail", ignoreCase = true) ||
            msg.contains("error", ignoreCase = true) ||
            msg.contains("restricted", ignoreCase = true) ||
            msg.contains("sync issue", ignoreCase = true)
        ) {
            RuntimeDiagnosticLogger.recordWarning("AppMessage", msg)
        }
        _toastMessage.value = msg
    }

    // --- Preferences ---
    fun setDarkMode(enabled: Boolean) = viewModelScope.launch {
        preferencesManager.setDarkMode(enabled)
    }

    fun setLanguage(lang: String) = viewModelScope.launch {
        preferencesManager.setLanguage(lang)
    }

    fun setActiveRole(role: UserRole) = viewModelScope.launch {
        if (!canUserSwitchWorkspaceRole(userProfile.value)) {
            _toastMessage.value = "Only approved Super Admin can switch workspace roles"
            return@launch
        }
        preferencesManager.setOwnerPreviewRole(if (role == UserRole.OWNER) null else role)
    }

    // --- Super Admin Only: AI Live Screen Audit & Prompt Generator ---
    private fun isTrueSuperAdmin(): Boolean = canUserSwitchWorkspaceRole(userProfile.value)

    fun setAiAuditEnabled(enabled: Boolean) = viewModelScope.launch {
        if (!isTrueSuperAdmin()) return@launch
        preferencesManager.setAiAuditEnabled(enabled)
        if (!enabled) {
            _aiAuditState.update { it.copy(isPanelOpen = false, isAuditing = false, isCapturingSnapshot = false) }
            _toastMessage.value = "Live AI Screen Audit turned OFF"
        } else {
            _toastMessage.value = "Live AI Screen Audit turned ON"
        }
    }

    fun setAiAuditAutoWatch(enabled: Boolean) = viewModelScope.launch {
        if (!isTrueSuperAdmin()) return@launch
        preferencesManager.setAiAuditAutoWatch(enabled)
    }

    fun setAiAuditExplainLanguage(lang: String) = viewModelScope.launch {
        if (!isTrueSuperAdmin()) return@launch
        preferencesManager.setAiAuditExplainLanguage(lang)
    }

    fun setAuditCapturingSnapshot(capturing: Boolean) {
        _aiAuditState.update { it.copy(isCapturingSnapshot = capturing) }
    }

    fun updateAuditActiveScreen(screenKey: String, screenTitle: String) {
        _aiAuditState.update { it.copy(activeScreenKey = screenKey, activeScreenTitle = screenTitle) }
    }

    fun openAuditPanel(screenKey: String? = null, screenTitle: String? = null) {
        if (!isTrueSuperAdmin()) return
        _aiAuditState.update { cur ->
            cur.copy(
                isPanelOpen = true,
                activeScreenKey = screenKey ?: cur.activeScreenKey,
                activeScreenTitle = screenTitle ?: cur.activeScreenTitle
            )
        }
    }

    fun dismissAuditPanel() {
        _aiAuditState.update { it.copy(isPanelOpen = false) }
    }

    fun runInstantScreenAudit(
        screenKey: String,
        screenTitle: String,
        activeRole: UserRole,
        screenshot: Bitmap?
    ) {
        if (!isTrueSuperAdmin() || !appPreferences.value.aiAuditEnabled) return
        executeScreenAudit(
            screenKey = screenKey,
            screenTitle = screenTitle,
            activeRole = activeRole,
            screenshot = screenshot,
            sourceMode = "INSTANT_CHECK"
        )
    }

    fun runAutoWatchAudit(
        screenKey: String,
        screenTitle: String,
        activeRole: UserRole,
        screenshot: Bitmap?
    ) {
        val prefs = appPreferences.value
        if (!isTrueSuperAdmin() || !prefs.aiAuditEnabled || !prefs.aiAuditAutoWatch) {
            screenshot?.recycle()
            return
        }
        if (_aiAuditState.value.isAuditing) {
            screenshot?.recycle()
            return
        }
        val now = System.currentTimeMillis()
        val lastScanned = autoWatchScreenTimestamps[screenKey] ?: 0L
        val hasFreshErrorSinceLastScan = RuntimeDiagnosticLogger.lastErrorEpochMillis > lastScanned
        val cooldownElapsed = (now - lastScanned) >= 5 * 60_000L
        if (lastScanned > 0L && !hasFreshErrorSinceLastScan && !cooldownElapsed) {
            screenshot?.recycle()
            return
        }
        autoWatchScreenTimestamps[screenKey] = now
        executeScreenAudit(
            screenKey = screenKey,
            screenTitle = screenTitle,
            activeRole = activeRole,
            screenshot = screenshot,
            sourceMode = "AUTO_WATCH"
        )
    }

    private fun executeScreenAudit(
        screenKey: String,
        screenTitle: String,
        activeRole: UserRole,
        screenshot: Bitmap?,
        sourceMode: String
    ) {
        viewModelScope.launch {
            _aiAuditState.update {
                it.copy(
                    isAuditing = true,
                    activeScreenKey = screenKey,
                    activeScreenTitle = screenTitle,
                    lastErrorMessage = null
                )
            }
            val prefs = appPreferences.value
            val diagnostics = RuntimeDiagnosticLogger.buildDiagnosticsSummary(screenKey)
            val result = try {
                aiUiAuditorRepository.auditScreen(
                    screenKey = screenKey,
                    screenTitle = screenTitle,
                    activeRole = activeRole,
                    isDarkMode = prefs.darkMode,
                    appLanguage = prefs.language,
                    diagnosticsSummary = diagnostics,
                    screenshot = screenshot,
                    sourceMode = sourceMode
                )
            } finally {
                screenshot?.recycle()
            }

            result.onSuccess { report ->
                val existing = AiAuditSerializer.decodeFindings(appPreferences.value.aiAuditFindingsJson)
                val merged = mergeAuditFindings(existing, report.findings)
                preferencesManager.setAiAuditFindingsJson(AiAuditSerializer.encodeFindings(merged))
                _aiAuditState.update {
                    it.copy(
                        isAuditing = false,
                        latestSummaryEn = report.summaryEn,
                        latestSummaryUr = report.summaryUr,
                        lastErrorMessage = null,
                        lastAuditedAtMillis = report.auditedAtMillis
                    )
                }
            }.onFailure { err ->
                if (err is CancellationException) throw err
                val errText = err.localizedMessage ?: "AI Screen Audit failed."
                _aiAuditState.update {
                    it.copy(
                        isAuditing = false,
                        lastErrorMessage = errText
                    )
                }
                if (sourceMode == "INSTANT_CHECK") {
                    _toastMessage.value = "Audit failed: $errText"
                }
            }
        }
    }

    fun updateAuditFindingStatus(findingId: String, status: AuditIssueStatus) = viewModelScope.launch {
        if (!isTrueSuperAdmin()) return@launch
        val existing = AiAuditSerializer.decodeFindings(appPreferences.value.aiAuditFindingsJson)
        val updated = existing.map { item ->
            if (item.id == findingId) item.copy(status = status) else item
        }
        preferencesManager.setAiAuditFindingsJson(AiAuditSerializer.encodeFindings(updated))
    }

    fun clearAuditInbox() = viewModelScope.launch {
        if (!isTrueSuperAdmin()) return@launch
        autoWatchScreenTimestamps.clear()
        preferencesManager.setAiAuditFindingsJson("[]")
        _aiAuditState.update { it.copy(latestSummaryEn = null, latestSummaryUr = null, lastErrorMessage = null) }
        _toastMessage.value = "Audit Inbox cleared"
    }

    // --- Profile ---
    fun updateProfile(
        name: String,
        phone: String,
        customPhotoDataUri: String? = null,
        clearPhoto: Boolean = false
    ) = viewModelScope.launch {
        val cleanName = PakistaniFormatters.formatTitleCase(name)
        val preservedPhoto = if (clearPhoto) {
            ""
        } else {
            customPhotoDataUri?.takeIf { it.isNotBlank() }
                ?: userProfile.value?.photoUrl
                ?: currentUserPhotoUrl
        }
        runCatching {
            repository.upsertUserProfile(
                userId = currentUserId,
                displayName = cleanName,
                email = currentUserEmail,
                phone = phone,
                photoUrl = preservedPhoto,
                preferredLanguage = appPreferences.value.language,
                darkMode = appPreferences.value.darkMode,
                clearPhoto = clearPhoto
            )
        }.onSuccess {
            _toastMessage.value = when {
                clearPhoto -> "Profile photo cleared"
                !customPhotoDataUri.isNullOrBlank() -> "Profile photo updated: $cleanName"
                else -> "Profile updated: $cleanName"
            }
        }.onFailure {
            _toastMessage.value = "Update failed: ${it.localizedMessage}"
        }
    }

    fun submitOrSaveFullProfile(
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
        customPhotoDataUri: String? = null,
        clearPhoto: Boolean = false
    ) = viewModelScope.launch {
        val cleanName = PakistaniFormatters.formatTitleCase(name.ifBlank { currentUserName })
        val preservedPhoto = if (clearPhoto) {
            ""
        } else {
            customPhotoDataUri?.takeIf { it.isNotBlank() }
                ?: userProfile.value?.photoUrl
                ?: currentUserPhotoUrl
        }
        val canDirectEdit = activeRole.value.canManageStaff
        if (canDirectEdit) {
            val safeRole = if (activeRole.value.isOwner) {
                UserRole.fromRequestedRole(requestedRole).label
            } else {
                val parsed = UserRole.fromRequestedRole(requestedRole)
                if (parsed == UserRole.OWNER) existingEmployee?.role ?: "Admin" else parsed.label
            }
            runCatching {
                repository.saveFullDirectProfile(
                    factoryId = activeFactoryId,
                    userId = currentUserId,
                    existingEmployee = existingEmployee,
                    name = cleanName,
                    fatherName = fatherName,
                    cnic = cnic,
                    phone = phone,
                    whatsappNumber = whatsappNumber,
                    email = email,
                    emergencyContact = emergencyContact,
                    role = safeRole,
                    shift = shift,
                    salary = salary,
                    joinDate = joinDate,
                    photoUrl = preservedPhoto,
                    clearPhoto = clearPhoto,
                    preferredLanguage = appPreferences.value.language,
                    darkMode = appPreferences.value.darkMode
                )
            }.onSuccess {
                _toastMessage.value = "Full profile updated: $cleanName"
            }.onFailure {
                _toastMessage.value = "Profile update failed: ${it.localizedMessage}"
            }
        } else {
            // Non-admin users submit full details (WITHOUT Role/Salary) for Admin / Super Admin approval
            runCatching {
                repository.submitProfileUpdateRequest(
                    factoryId = activeFactoryId,
                    applicantUid = currentUserId,
                    request = ProfileUpdateRequest(
                        id = currentUserId,
                        ownerId = activeFactoryId,
                        applicantUid = currentUserId,
                        employeeId = existingEmployee?.id ?: "",
                        name = cleanName,
                        fatherName = fatherName,
                        cnic = cnic,
                        phone = phone,
                        whatsappNumber = whatsappNumber,
                        email = email,
                        emergencyContact = emergencyContact,
                        shift = shift,
                        joinDate = joinDate,
                        photoUrl = preservedPhoto,
                        status = "PENDING"
                    )
                )
            }.onSuccess {
                _toastMessage.value = "Profile update sent for Super Admin / Admin approval (منظوری کے لیے بھیج دیا گیا)"
            }.onFailure {
                _toastMessage.value = "Request failed: ${it.localizedMessage}"
            }
        }
    }

    fun approveProfileUpdateRequest(
        request: ProfileUpdateRequest,
        reviewNote: String? = null
    ) = viewModelScope.launch {
        if (!activeRole.value.canManageStaff) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.approveProfileUpdateRequest(
                factoryId = activeFactoryId,
                request = request,
                reviewNote = reviewNote
            )
        }.onSuccess {
            _toastMessage.value = "Profile updated & approved for ${PakistaniFormatters.formatTitleCase(request.name)}"
        }.onFailure {
            _toastMessage.value = "Approval failed: ${it.localizedMessage}"
        }
    }

    fun rejectProfileUpdateRequest(
        request: ProfileUpdateRequest,
        reason: String? = null
    ) = viewModelScope.launch {
        if (!activeRole.value.canManageStaff) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.rejectProfileUpdateRequest(activeFactoryId, request, reason)
        }.onSuccess {
            _toastMessage.value = "Profile update declined: ${request.name}"
        }.onFailure {
            _toastMessage.value = "Reject failed: ${it.localizedMessage}"
        }
    }

    fun deleteProfileUpdateRequest(requestId: String) = viewModelScope.launch {
        if (!activeRole.value.canManageStaff) return@launch
        runCatching {
            repository.deleteDocument(activeFactoryId, "profile_update_requests", requestId)
        }.onSuccess {
            _toastMessage.value = "Profile update request deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    // --- Employees, Registration Approvals & Face ---
    fun saveEmployee(employee: Employee) = viewModelScope.launch {
        if (!activeRole.value.canManageStaff) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        val cleanEmp = employee.copy(
            ownerId = activeFactoryId,
            name = PakistaniFormatters.formatTitleCase(employee.name),
            fatherName = PakistaniFormatters.formatTitleCase(employee.fatherName),
            cnic = PakistaniFormatters.formatCnic(employee.cnic),
            phone = PakistaniFormatters.formatPhone(employee.phone),
            whatsappNumber = PakistaniFormatters.formatPhone(employee.whatsappNumber),
            emergencyContact = PakistaniFormatters.formatPhone(employee.emergencyContact)
        )
        runCatching {
            repository.saveEmployee(activeFactoryId, cleanEmp)
        }.onSuccess {
            _toastMessage.value = "Staff profile saved: ${cleanEmp.name}"
        }.onFailure {
            _toastMessage.value = "Save failed: ${it.localizedMessage}"
        }
    }

    fun deleteEmployee(employeeId: String, employeeName: String) = viewModelScope.launch {
        if (!activeRole.value.canManageStaff) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.deleteDocument(activeFactoryId, "employees", employeeId)
        }.onSuccess {
            _toastMessage.value = "Deleted staff: $employeeName"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    fun approveRegistrationRequest(
        request: RegistrationRequest,
        roleOverride: String,
        shiftOverride: String,
        startingSalary: Double,
        reviewNote: String? = null
    ) = viewModelScope.launch {
        if (!activeRole.value.canManageStaff) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        val normalizedRoleOverride = UserRole.fromRequestedRole(roleOverride).label
        if (normalizedRoleOverride == "Super Admin" && !activeRole.value.isOwner) {
            _toastMessage.value = "Only Super Admin can approve a Super Admin account"
            return@launch
        }
        runCatching {
            repository.approveRegistrationRequest(
                factoryId = activeFactoryId,
                request = request,
                roleOverride = normalizedRoleOverride,
                shiftOverride = shiftOverride,
                startingSalary = startingSalary,
                reviewNote = reviewNote
            )
        }.onSuccess {
            _toastMessage.value = "Approved & added ${PakistaniFormatters.formatTitleCase(request.name)} ($normalizedRoleOverride)"
        }.onFailure {
            _toastMessage.value = "Approval failed: ${it.localizedMessage}"
        }
    }

    fun rejectRegistrationRequest(request: RegistrationRequest, reason: String? = null) = viewModelScope.launch {
        if (!activeRole.value.canManageStaff) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.rejectRegistrationRequest(activeFactoryId, request, reason)
        }.onSuccess {
            _toastMessage.value = "Request declined: ${request.name}"
        }.onFailure {
            _toastMessage.value = "Reject failed: ${it.localizedMessage}"
        }
    }

    fun deleteRegistrationRequest(requestId: String) = viewModelScope.launch {
        if (!activeRole.value.canManageStaff) return@launch
        runCatching {
            repository.deleteDocument(activeFactoryId, "registration_requests", requestId)
        }.onSuccess {
            _toastMessage.value = "Registration request deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    fun saveNotice(notice: FactoryNotice) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.saveNotice(
                activeFactoryId,
                notice.copy(
                    ownerId = activeFactoryId,
                    createdByName = PakistaniFormatters.formatTitleCase(
                        notice.createdByName.ifBlank { currentUserName }
                    )
                )
            )
        }.onSuccess {
            _toastMessage.value = "Notice published: ${notice.title}"
        }.onFailure {
            _toastMessage.value = "Notice save failed: ${it.localizedMessage}"
        }
    }

    fun deleteNotice(noticeId: String) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) return@launch
        runCatching {
            repository.deleteDocument(activeFactoryId, "notices", noticeId)
        }.onSuccess {
            _toastMessage.value = "Notice deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    fun saveEncryptedFaceTemplate(employeeId: String, descriptorSamples: List<List<Float>>) = viewModelScope.launch {
        val avg = FaceCryptoHelper.averageDescriptors(descriptorSamples)
        if (avg.isEmpty()) {
            _toastMessage.value = "No valid face samples captured"
            return@launch
        }
        val encrypted = FaceCryptoHelper.encryptTemplate(activeFactoryId, avg)
        savePreEncryptedFaceTemplate(employeeId, encrypted)
    }

    fun savePreEncryptedFaceTemplate(
        employeeId: String,
        encryptedTemplate: String,
        attendanceFacePhoto: String? = null
    ) = viewModelScope.launch {
        if (encryptedTemplate.isBlank()) {
            _toastMessage.value = "No valid face template captured"
            return@launch
        }
        val targetEmp = employees.value.firstOrNull { it.id == employeeId || it.authUid == employeeId }
        val resolvedId = targetEmp?.id ?: employeeId
        val targetAuthUid = targetEmp?.authUid ?: if (employeeId == currentUserId) currentUserId else null
        runCatching {
            if (targetEmp == null && employeeId == currentUserId) {
                repository.saveEmployee(
                    activeFactoryId,
                    Employee(
                        id = currentUserId,
                        ownerId = activeFactoryId,
                        authUid = currentUserId,
                        name = currentUserName.ifBlank { "Super Admin" },
                        role = activeRole.value.label,
                        faceTemplateEncrypted = encryptedTemplate,
                        attendanceFacePhoto = attendanceFacePhoto,
                        active = true
                    )
                )
                repository.upsertUserProfile(
                    userId = currentUserId,
                    displayName = userProfile.value?.displayName?.ifBlank { currentUserName } ?: currentUserName,
                    email = currentUserEmail,
                    phone = userProfile.value?.phone,
                    photoUrl = userProfile.value?.photoUrl ?: currentUserPhotoUrl,
                    preferredLanguage = appPreferences.value.language,
                    darkMode = appPreferences.value.darkMode,
                    faceEnrolled = true
                )
            } else {
                repository.updateEmployeeFaceTemplate(
                    factoryId = activeFactoryId,
                    employeeId = resolvedId,
                    encryptedTemplate = encryptedTemplate,
                    employeeAuthUid = targetAuthUid,
                    attendanceFacePhoto = attendanceFacePhoto
                )
            }
        }.onSuccess {
            _toastMessage.value = "4-Pose Biometric Face Template encrypted & saved!"
        }.onFailure {
            _toastMessage.value = "Failed to save face template: ${it.localizedMessage}"
        }
    }

    fun updateEmployeeIdCardPhoto(
        employeeId: String,
        manualPhotoDataUri: String?
    ) = viewModelScope.launch {
        val targetEmp = employees.value.firstOrNull { it.id == employeeId || it.authUid == employeeId }
        val resolvedId = targetEmp?.id ?: employeeId
        runCatching {
            if (targetEmp == null && employeeId == currentUserId) {
                repository.saveEmployee(
                    activeFactoryId,
                    Employee(
                        id = currentUserId,
                        ownerId = activeFactoryId,
                        authUid = currentUserId,
                        name = currentUserName.ifBlank { "Super Admin" },
                        role = activeRole.value.label,
                        idCardPhoto = manualPhotoDataUri ?: "",
                        active = true
                    )
                )
            } else {
                repository.updateEmployeeIdCardPhoto(
                    factoryId = activeFactoryId,
                    employeeId = resolvedId,
                    idCardPhoto = manualPhotoDataUri
                )
            }
        }.onSuccess {
            _toastMessage.value = if (manualPhotoDataUri.isNullOrBlank()) {
                "Manual ID photo cleared — falling back to Profile / Attendance photo"
            } else {
                "Digital ID Card photo updated!"
            }
        }.onFailure {
            _toastMessage.value = "ID Card photo update failed: ${it.localizedMessage}"
        }
    }

    fun updateEmployeeAttendanceFacePhoto(
        employeeId: String,
        attendanceFacePhoto: String
    ) = viewModelScope.launch {
        if (attendanceFacePhoto.isBlank()) return@launch
        val targetEmp = employees.value.firstOrNull { it.id == employeeId || it.authUid == employeeId } ?: return@launch
        runCatching {
            repository.updateEmployeeAttendanceFacePhoto(
                factoryId = activeFactoryId,
                employeeId = targetEmp.id,
                attendanceFacePhoto = attendanceFacePhoto
            )
        }
    }

    fun matchFaceDescriptor(liveDescriptor: List<Float>): Pair<Employee, Float>? {
        if (liveDescriptor.size < FaceCryptoHelper.MULTIMODAL_DIM) return null
        var bestEmp: Employee? = null
        var bestScore = 0f
        for (emp in employees.value) {
            if (!emp.active) continue
            val enc = emp.faceTemplateEncrypted ?: continue
            if (FaceCryptoHelper.isLegacyTemplate(activeFactoryId, enc)) continue
            val frontPose = FaceCryptoHelper.decryptTemplate(activeFactoryId, enc)
            if (frontPose.size < FaceCryptoHelper.MULTIMODAL_DIM) continue
            val score = FaceCryptoHelper.strictBiometricMatchScore(liveDescriptor, frontPose) ?: continue
            if (score > bestScore) {
                bestScore = score
                bestEmp = emp
            }
        }
        return if (bestEmp != null && bestScore >= FaceCryptoHelper.MIN_MULTIMODAL_SCORE) bestEmp to bestScore else null
    }

    fun matchFaceForEmployee(liveDescriptor: List<Float>, employeeId: String): Pair<Employee, Float>? {
        if (liveDescriptor.size < FaceCryptoHelper.MULTIMODAL_DIM) return null
        val emp = employees.value.firstOrNull { it.id == employeeId || it.authUid == employeeId } ?: return null
        val enc = emp.faceTemplateEncrypted ?: return null
        if (FaceCryptoHelper.isLegacyTemplate(activeFactoryId, enc)) return null
        val frontPose = FaceCryptoHelper.decryptTemplate(activeFactoryId, enc)
        if (frontPose.size < FaceCryptoHelper.MULTIMODAL_DIM) return null
        val score = FaceCryptoHelper.strictBiometricMatchScore(liveDescriptor, frontPose) ?: return null
        return if (score >= FaceCryptoHelper.MIN_MULTIMODAL_SCORE) emp to score else null
    }

    fun toggleEmployeeSkipLiveness(employeeId: String, skip: Boolean) = viewModelScope.launch {
        runCatching {
            repository.toggleEmployeeSkipLiveness(activeFactoryId, employeeId, skip)
        }.onSuccess {
            _toastMessage.value = if (skip) "Liveness check skipped for employee" else "Liveness check enabled"
        }.onFailure {
            _toastMessage.value = "Update failed: ${it.localizedMessage}"
        }
    }

    // --- Attendance ---
    fun checkIn(employeeId: String, employeeName: String, bypassed: Boolean) = viewModelScope.launch {
        runCatching {
            repository.recordCheckIn(
                factoryId = activeFactoryId,
                employeeId = employeeId,
                employeeName = employeeName,
                authUid = currentUserId,
                bypassed = bypassed
            )
        }.onSuccess {
            _toastMessage.value = "Checked in: $employeeName"
        }.onFailure {
            _toastMessage.value = "Check-in failed: ${it.localizedMessage}"
        }
    }

    fun checkOut(record: AttendanceRecord, bypassed: Boolean = false) = viewModelScope.launch {
        if (bypassed && !activeRole.value.canManageStaff) {
            _toastMessage.value = "Admin permission required for manual bypass"
            return@launch
        }
        runCatching {
            repository.recordCheckOut(activeFactoryId, record, bypassed = bypassed)
        }.onSuccess {
            _toastMessage.value = "Checked out: ${record.employeeName}"
        }.onFailure {
            _toastMessage.value = "Check-out failed: ${it.localizedMessage}"
        }
    }

    fun saveAttendanceRecord(record: AttendanceRecord) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.saveAttendanceRecord(activeFactoryId, currentUserId, record)
        }.onSuccess {
            _toastMessage.value = "Attendance saved: ${record.employeeName} (${record.status})"
        }.onFailure {
            _toastMessage.value = "Attendance save failed: ${it.localizedMessage}"
        }
    }

    fun deleteAttendance(recordId: String) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.deleteDocument(activeFactoryId, "attendance", recordId)
        }.onSuccess {
            _toastMessage.value = "Attendance record deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    // --- Production & Inventory ---
    fun addProduction(tons: Double, productType: String, machineId: String, machineName: String, notes: String?) = viewModelScope.launch {
        runCatching {
            repository.addProductionAndStock(
                factoryId = activeFactoryId,
                callerUid = currentUserId,
                tons = tons,
                productType = productType,
                machineId = machineId,
                machineName = machineName,
                notes = notes
            )
        }.onSuccess {
            _toastMessage.value = "Added ${PakistaniFormatters.formatTons(tons)} production & updated stock"
        }.onFailure {
            _toastMessage.value = "Production save failed: ${it.localizedMessage}"
        }
    }

    fun updateProduction(record: ProductionRecord) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.updateProduction(activeFactoryId, record)
        }.onSuccess {
            _toastMessage.value = "Production record updated"
        }.onFailure {
            _toastMessage.value = "Update failed: ${it.localizedMessage}"
        }
    }

    fun deleteProduction(id: String) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.deleteDocument(activeFactoryId, "production", id)
        }.onSuccess {
            _toastMessage.value = "Production record deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    fun addInventoryMovement(product: String, tons: Double, location: String, type: String, notes: String?) = viewModelScope.launch {
        runCatching {
            repository.addInventoryMovement(activeFactoryId, currentUserId, product, tons, location, type, notes)
        }.onSuccess {
            _toastMessage.value = "Stock $type recorded ($product)"
        }.onFailure {
            _toastMessage.value = "Stock save failed: ${it.localizedMessage}"
        }
    }

    fun updateInventoryMovement(movement: InventoryMovement) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.updateInventoryMovement(activeFactoryId, movement)
        }.onSuccess {
            _toastMessage.value = "Stock movement updated"
        }.onFailure {
            _toastMessage.value = "Update failed: ${it.localizedMessage}"
        }
    }

    fun deleteInventoryMovement(id: String) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.deleteDocument(activeFactoryId, "inventory", id)
        }.onSuccess {
            _toastMessage.value = "Stock movement deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    // --- Sales & Customers ---
    fun createSale(
        customerId: String,
        customerName: String,
        product: String,
        quantity: Long,
        unitPrice: Double,
        total: Double,
        paid: Boolean,
        source: String = "manual"
    ) = viewModelScope.launch {
        runCatching {
            repository.createSale(
                factoryId = activeFactoryId,
                callerUid = currentUserId,
                customerId = customerId,
                customerName = customerName,
                product = product,
                quantity = quantity,
                unitPrice = unitPrice,
                total = total,
                paid = paid,
                source = source
            )
        }.onSuccess {
            _toastMessage.value = "Invoice created: ${PakistaniFormatters.formatPkr(total)}"
        }.onFailure {
            _toastMessage.value = "Sale failed: ${it.localizedMessage}"
        }
    }

    fun updateSale(sale: Sale) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.updateSale(activeFactoryId, sale)
        }.onSuccess {
            _toastMessage.value = "Invoice ${sale.invoiceNumber} updated"
        }.onFailure {
            _toastMessage.value = "Update failed: ${it.localizedMessage}"
        }
    }

    fun deleteSale(saleId: String) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.deleteDocument(activeFactoryId, "sales", saleId)
        }.onSuccess {
            _toastMessage.value = "Invoice deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    fun markSalePaid(sale: Sale) = viewModelScope.launch {
        runCatching {
            repository.markSalePaid(activeFactoryId, sale)
        }.onSuccess {
            _toastMessage.value = "Invoice ${sale.invoiceNumber} marked Paid"
        }.onFailure {
            _toastMessage.value = "Update failed: ${it.localizedMessage}"
        }
    }

    fun addCustomer(name: String, phone: String, address: String?, balance: Double) = viewModelScope.launch {
        runCatching {
            repository.addCustomer(activeFactoryId, currentUserId, name, phone, address, balance)
        }.onSuccess {
            _toastMessage.value = "Customer added: $name"
        }.onFailure {
            _toastMessage.value = "Failed: ${it.localizedMessage}"
        }
    }

    fun updateCustomer(customer: Customer) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.updateCustomer(activeFactoryId, customer)
        }.onSuccess {
            _toastMessage.value = "Customer updated: ${customer.name}"
        }.onFailure {
            _toastMessage.value = "Update failed: ${it.localizedMessage}"
        }
    }

    fun deleteCustomer(customerId: String) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) {
            _toastMessage.value = "Super Admin or Admin permission required"
            return@launch
        }
        runCatching {
            repository.deleteDocument(activeFactoryId, "customers", customerId)
        }.onSuccess {
            _toastMessage.value = "Customer deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    fun receiveCustomerPayment(customer: Customer, amount: Double) = viewModelScope.launch {
        runCatching {
            repository.receiveCustomerPayment(activeFactoryId, customer, amount)
        }.onSuccess {
            _toastMessage.value = "Received ${PakistaniFormatters.formatPkr(amount)} from ${customer.name}"
        }.onFailure {
            _toastMessage.value = "Payment failed: ${it.localizedMessage}"
        }
    }

    // --- Expenses & Funding ---
    fun addExpense(description: String, amount: Double, category: String, source: String = "manual") = viewModelScope.launch {
        if (category.equals("Factory Build", ignoreCase = true) && !activeRole.value.isOwner) {
            _toastMessage.value = "Only Super Admin can add Factory Build capital expenses"
            return@launch
        }
        runCatching {
            repository.addExpense(activeFactoryId, currentUserId, description, amount, category, source)
        }.onSuccess {
            _toastMessage.value = "Saved $category: ${PakistaniFormatters.formatPkr(amount)}"
        }.onFailure {
            _toastMessage.value = "Expense failed: ${it.localizedMessage}"
        }
    }

    fun updateExpense(expense: Expense) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete || (expense.capitalInvestment && !activeRole.value.isOwner)) {
            _toastMessage.value = "Insufficient permission to edit this expense"
            return@launch
        }
        runCatching {
            repository.updateExpense(activeFactoryId, expense)
        }.onSuccess {
            _toastMessage.value = "Expense updated"
        }.onFailure {
            _toastMessage.value = "Update failed: ${it.localizedMessage}"
        }
    }

    fun deleteExpense(expense: Expense) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete || (expense.capitalInvestment && !activeRole.value.isOwner)) {
            _toastMessage.value = "Insufficient permission to delete this expense"
            return@launch
        }
        runCatching {
            repository.deleteDocument(activeFactoryId, "expenses", expense.id)
        }.onSuccess {
            _toastMessage.value = "Expense deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    fun addFunding(source: String, name: String, type: String, amount: Double, interest: Double, dueDate: String?, notes: String?) = viewModelScope.launch {
        if (!activeRole.value.isOwner) {
            _toastMessage.value = "Only Super Admin can record funding"
            return@launch
        }
        runCatching {
            repository.addFunding(activeFactoryId, source, name, type, amount, 0.0, interest, dueDate, notes)
        }.onSuccess {
            _toastMessage.value = "Funding recorded: ${PakistaniFormatters.formatPkr(amount)}"
        }.onFailure {
            _toastMessage.value = "Funding failed: ${it.localizedMessage}"
        }
    }

    fun updateFunding(entry: FundingEntry) = viewModelScope.launch {
        if (!activeRole.value.isOwner) return@launch
        runCatching {
            repository.updateFunding(activeFactoryId, entry)
        }.onSuccess {
            _toastMessage.value = "Funding entry updated"
        }.onFailure {
            _toastMessage.value = "Update failed: ${it.localizedMessage}"
        }
    }

    fun deleteFunding(id: String) = viewModelScope.launch {
        if (!activeRole.value.isOwner) return@launch
        runCatching {
            repository.deleteDocument(activeFactoryId, "funding", id)
        }.onSuccess {
            _toastMessage.value = "Funding entry deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    fun repayFunding(entry: FundingEntry, repayAmount: Double) = viewModelScope.launch {
        if (!activeRole.value.isOwner) return@launch
        runCatching {
            repository.recordFundingRepayment(activeFactoryId, entry, repayAmount)
        }.onSuccess {
            _toastMessage.value = "Repayment recorded: ${PakistaniFormatters.formatPkr(repayAmount)}"
        }.onFailure {
            _toastMessage.value = "Repayment failed: ${it.localizedMessage}"
        }
    }

    // --- Payroll ---
    fun savePayroll(record: PayrollRecord) = viewModelScope.launch {
        if (!activeRole.value.isOwner) return@launch
        val matchedAuthUid = record.authUid.ifBlank {
            employees.value.firstOrNull { it.id == record.employeeId }?.authUid ?: ""
        }
        val enriched = record.copy(authUid = matchedAuthUid)
        runCatching {
            if (enriched.id.isNotBlank()) repository.updatePayroll(activeFactoryId, enriched)
            else repository.savePayroll(activeFactoryId, enriched)
        }.onSuccess {
            _toastMessage.value = "Salary record saved"
        }.onFailure {
            _toastMessage.value = "Failed: ${it.localizedMessage}"
        }
    }

    fun deletePayroll(id: String) = viewModelScope.launch {
        if (!activeRole.value.isOwner) return@launch
        runCatching {
            repository.deleteDocument(activeFactoryId, "payroll", id)
        }.onSuccess {
            _toastMessage.value = "Salary record deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    fun markPayrollPaid(record: PayrollRecord) = viewModelScope.launch {
        if (!activeRole.value.isOwner) return@launch
        runCatching {
            repository.markSalaryPaidAndCreateExpense(activeFactoryId, currentUserId, record)
        }.onSuccess {
            _toastMessage.value = "Salary paid & added to expenses"
        }.onFailure {
            _toastMessage.value = "Failed: ${it.localizedMessage}"
        }
    }

    // --- Utilities, Deliveries, Maintenance, Suppliers, Incidents, Settings ---
    fun saveUtility(type: String, meterReading: Double, unitsOrLiters: Double, amount: Double, notes: String?) = viewModelScope.launch {
        runCatching {
            repository.saveUtilityAndCreateExpense(activeFactoryId, currentUserId, type, meterReading, unitsOrLiters, amount, notes)
        }.onSuccess {
            _toastMessage.value = "$type reading saved"
        }.onFailure {
            _toastMessage.value = "Failed: ${it.localizedMessage}"
        }
    }

    fun updateUtility(reading: UtilityReading) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) return@launch
        runCatching {
            repository.updateUtility(activeFactoryId, reading)
        }.onSuccess {
            _toastMessage.value = "Utility reading updated"
        }.onFailure {
            _toastMessage.value = "Update failed: ${it.localizedMessage}"
        }
    }

    fun deleteUtility(id: String) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) return@launch
        runCatching {
            repository.deleteDocument(activeFactoryId, "utilities", id)
        }.onSuccess {
            _toastMessage.value = "Utility reading deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    fun saveDelivery(item: DeliveryItem) = viewModelScope.launch {
        runCatching {
            repository.saveDelivery(activeFactoryId, currentUserId, item)
        }.onSuccess {
            _toastMessage.value = "Delivery updated"
        }.onFailure {
            _toastMessage.value = "Failed: ${it.localizedMessage}"
        }
    }

    fun deleteDelivery(id: String) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) return@launch
        runCatching {
            repository.deleteDocument(activeFactoryId, "deliveries", id)
        }.onSuccess {
            _toastMessage.value = "Delivery deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    fun saveMachine(item: MachineItem) = viewModelScope.launch {
        runCatching {
            repository.saveMachine(activeFactoryId, currentUserId, item)
        }.onSuccess {
            _toastMessage.value = "Machine saved"
        }.onFailure {
            _toastMessage.value = "Failed: ${it.localizedMessage}"
        }
    }

    fun deleteMachine(id: String) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) return@launch
        runCatching {
            repository.deleteDocument(activeFactoryId, "machines", id)
        }.onSuccess {
            _toastMessage.value = "Machine deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    fun saveSupplier(item: SupplierItem) = viewModelScope.launch {
        runCatching {
            repository.saveSupplier(activeFactoryId, currentUserId, item)
        }.onSuccess {
            _toastMessage.value = "Supplier saved"
        }.onFailure {
            _toastMessage.value = "Failed: ${it.localizedMessage}"
        }
    }

    fun deleteSupplier(id: String) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) return@launch
        runCatching {
            repository.deleteDocument(activeFactoryId, "suppliers", id)
        }.onSuccess {
            _toastMessage.value = "Supplier deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    fun paySupplier(supplier: SupplierItem, amount: Double) = viewModelScope.launch {
        runCatching {
            repository.paySupplierAndCreateExpense(activeFactoryId, currentUserId, supplier, amount)
        }.onSuccess {
            _toastMessage.value = "Paid ${PakistaniFormatters.formatPkr(amount)} to ${supplier.name}"
        }.onFailure {
            _toastMessage.value = "Payment failed: ${it.localizedMessage}"
        }
    }

    fun saveIncident(incident: SafetyIncident) = viewModelScope.launch {
        runCatching {
            repository.saveIncident(activeFactoryId, currentUserId, incident)
        }.onSuccess {
            _toastMessage.value = "Incident updated"
        }.onFailure {
            _toastMessage.value = "Failed: ${it.localizedMessage}"
        }
    }

    fun deleteIncident(id: String) = viewModelScope.launch {
        if (!activeRole.value.canEditOrDelete) return@launch
        runCatching {
            repository.deleteDocument(activeFactoryId, "incidents", id)
        }.onSuccess {
            _toastMessage.value = "Incident deleted"
        }.onFailure {
            _toastMessage.value = "Delete failed: ${it.localizedMessage}"
        }
    }

    fun saveFactorySettings(newSettings: FactorySettings) = viewModelScope.launch {
        if (!activeRole.value.isOwner) {
            _toastMessage.value = "Only Super Admin can modify factory settings"
            return@launch
        }
        runCatching {
            repository.saveSettings(activeFactoryId, newSettings)
        }.onSuccess {
            _toastMessage.value = "Settings saved"
        }.onFailure {
            _toastMessage.value = "Failed: ${it.localizedMessage}"
        }
    }

    fun clearAllDemoData() = viewModelScope.launch {
        if (!activeRole.value.isOwner) {
            _toastMessage.value = "Only Super Admin can clear demo data"
            return@launch
        }
        runCatching {
            repository.clearDemoData(activeFactoryId)
        }.onSuccess {
            _toastMessage.value = "All [Demo] sample records cleared"
        }.onFailure {
            _toastMessage.value = "Clear demo data failed: ${it.localizedMessage}"
        }
    }

    // --- Live App Updates & GitHub Direct Release Channel ---
    fun refreshGitHubReleaseQuietly() = viewModelScope.launch {
        AppUpdateManager.fetchLatestReleaseFromGitHub()
            .onSuccess { rel ->
                if (rel != null) {
                    _githubAppRelease.value = rel
                }
            }
    }

    fun checkForUpdatesNow() = viewModelScope.launch {
        if (_isCheckingUpdate.value) return@launch
        _isCheckingUpdate.value = true
        val ghResult = AppUpdateManager.fetchLatestReleaseFromGitHub()
        val ghRelease = ghResult.getOrNull()
        if (ghRelease != null) {
            _githubAppRelease.value = ghRelease
        }
        val fsRelease = runCatching { repository.fetchLatestAppReleaseFromServer() }.getOrNull()
        val best = AppUpdateManager.selectLatestRelease(ghRelease ?: _githubAppRelease.value, fsRelease)

        val msg = when {
            isAppUpdateAvailable(BuildConfig.VERSION_CODE, best) -> {
                "Update v${best!!.latestVersionName} (Build ${best.latestVersionCode}) available on GitHub! (نئی اپڈیٹ دستیاب ہے)"
            }
            ghResult.isFailure && best == null -> {
                "Could not reach GitHub (${ghResult.exceptionOrNull()?.localizedMessage ?: "Check internet"})."
            }
            best == null -> {
                "No GitHub release published yet. Push code to GitHub (${AppUpdateManager.GITHUB_OWNER}/${AppUpdateManager.GITHUB_REPO}) to build your first auto-release."
            }
            else -> {
                "Latest Version v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) is active! (ایپ بالکل اپ ٹو ڈیٹ ہے)"
            }
        }
        _lastUpdateCheckedMessage.value = msg
        _toastMessage.value = msg
        _isCheckingUpdate.value = false
    }

    fun startApkUpdateDownload(context: Context) = viewModelScope.launch {
        val currentBest = appRelease.value ?: run {
            _toastMessage.value = "Checking GitHub releases..."
            val gh = AppUpdateManager.fetchLatestReleaseFromGitHub().getOrNull()
            if (gh != null) _githubAppRelease.value = gh
            val fs = runCatching { repository.fetchLatestAppReleaseFromServer() }.getOrNull()
            AppUpdateManager.selectLatestRelease(gh, fs)
        }
        if (!isAppUpdateAvailable(BuildConfig.VERSION_CODE, currentBest) || currentBest == null) {
            _toastMessage.value = "Already running latest build v${BuildConfig.VERSION_NAME}."
            return@launch
        }
        val targetUrl = currentBest.apkDownloadUrl.trim().ifBlank { AppUpdateManager.DEFAULT_APK_DOWNLOAD_URL }
        if (!AppUpdateManager.isValidApkUrl(targetUrl)) {
            val msg = "Invalid APK download URL."
            _updateDownloadState.value = AppUpdateDownloadState.Failed(msg)
            _toastMessage.value = msg
            return@launch
        }
        AppUpdateManager.downloadAndPrepareApk(
            context = context.applicationContext,
            downloadUrl = targetUrl,
            versionName = currentBest.latestVersionName
        ).collect { state ->
            _updateDownloadState.value = state
            when (state) {
                is AppUpdateDownloadState.ReadyToInstall -> {
                    AppUpdateManager.launchApkInstaller(context, state.apkFile)
                        .onFailure { _toastMessage.value = it.localizedMessage }
                }
                is AppUpdateDownloadState.Failed -> {
                    _toastMessage.value = state.message
                }
                else -> Unit
            }
        }
    }

    fun installOrGrantPermission(context: Context) {
        when (val st = _updateDownloadState.value) {
            is AppUpdateDownloadState.ReadyToInstall -> {
                AppUpdateManager.launchApkInstaller(context, st.apkFile)
                    .onFailure { _toastMessage.value = it.localizedMessage }
            }
            else -> startApkUpdateDownload(context)
        }
    }

    fun resetUpdateDownloadState() {
        _updateDownloadState.value = AppUpdateDownloadState.Idle
    }

    fun publishAppUpdate(
        versionCode: Int,
        versionName: String,
        apkUrl: String,
        releaseNotesEn: String,
        releaseNotesUr: String,
        mandatory: Boolean
    ) = viewModelScope.launch {
        if (!activeRole.value.isOwner) {
            _toastMessage.value = "Only Super Admin can publish app updates"
            return@launch
        }
        if (versionCode < 1 || versionName.isBlank()) {
            _toastMessage.value = "Enter a valid version code and version name"
            return@launch
        }
        if (!AppUpdateManager.isValidApkUrl(apkUrl)) {
            _toastMessage.value = "Enter a valid HTTPS APK download URL"
            return@launch
        }
        val config = AppReleaseConfig(
            id = "latest",
            latestVersionCode = versionCode,
            latestVersionName = versionName.trim(),
            releaseNotes = releaseNotesEn.trim(),
            releaseNotesUrdu = releaseNotesUr.trim(),
            apkDownloadUrl = apkUrl.trim(),
            mandatory = mandatory
        )
        runCatching {
            repository.publishAppRelease(config, currentUserName)
        }.onSuccess {
            _toastMessage.value = "Published v$versionName (Build $versionCode) to all users!"
        }.onFailure {
            _toastMessage.value = "Publish failed: ${it.localizedMessage}"
        }
    }

    // --- Voice Command Pipeline ---
    fun setVoiceAutoSave(enabled: Boolean) {
        _voiceState.value = _voiceState.value.copy(autoSaveEnabled = enabled)
    }

    fun parseVoiceTranscript(
        transcript: String,
        isOnline: Boolean = true,
        onSpeak: (String) -> Unit
    ) = viewModelScope.launch {
        val currentAutoSave = _voiceState.value.autoSaveEnabled
        val normalized = PakistaniFormatters.normalizeVoiceTranscript(transcript)
        _voiceState.value = VoiceUiState(
            isParsing = true,
            autoSaveEnabled = currentAutoSave,
            lastNormalizedTranscript = normalized
        )
        val role = activeRole.value
        val result = voiceAiRepository.parseTranscript(
            transcript = normalized,
            activeRole = role,
            allowRemoteAi = isOnline
        )
        result.onSuccess { intents ->
            val single = intents.firstOrNull()
            if (intents.size == 1 && single != null && single.intentType == "NAVIGATE") {
                val target = single.targetScreen.ifBlank { "dashboard" }
                if (!AppRoute.isAllowedFor(role, target)) {
                    val denied = "Access denied: That module is restricted for ${role.label}."
                    _voiceState.value = VoiceUiState(
                        autoSaveEnabled = currentAutoSave,
                        lastNormalizedTranscript = normalized,
                        spokenAnswer = denied
                    )
                    onSpeak(denied)
                } else {
                    val msg = single.spokenReply.ifBlank { "Opening $target." }
                    _voiceState.value = VoiceUiState(
                        autoSaveEnabled = currentAutoSave,
                        lastNormalizedTranscript = normalized,
                        spokenAnswer = msg,
                        pendingNavigationKey = target
                    )
                    onSpeak(msg)
                }
            } else if (intents.size == 1 && single != null && single.intentType == "QUERY") {
                val answer = answerVoiceQuery(single, role)
                val nav = single.targetScreen.takeIf { it.isNotBlank() && AppRoute.isAllowedFor(role, it) }
                _voiceState.value = VoiceUiState(
                    autoSaveEnabled = currentAutoSave,
                    lastNormalizedTranscript = normalized,
                    spokenAnswer = answer,
                    pendingNavigationKey = nav
                )
                onSpeak(answer)
            } else {
                val blockedByStaff = role == UserRole.STAFF
                val blockedByOwner = intents.any {
                    !role.isOwner && (it.intentType == "FUNDING" || it.intentType == "FACTORY_BUILD" ||
                        it.category.equals("Factory Build", ignoreCase = true))
                }
                if (blockedByStaff || blockedByOwner) {
                    val denied = if (blockedByStaff) {
                        "Restricted: Only Super Admin or Admin can record sales, expenses, or production. Staff can check Attendance, Salary, or Notices."
                    } else {
                        "Access restricted: Funding and Factory Build entries are available to Super Admin only."
                    }
                    _voiceState.value = VoiceUiState(
                        autoSaveEnabled = currentAutoSave,
                        lastNormalizedTranscript = normalized,
                        spokenAnswer = denied
                    )
                    onSpeak(denied)
                } else if (currentAutoSave) {
                    runCatching {
                        saveIntentsInternal(intents)
                    }.onSuccess { doneMsg ->
                        _voiceState.value = VoiceUiState(
                            autoSaveEnabled = true,
                            lastNormalizedTranscript = normalized,
                            spokenAnswer = doneMsg
                        )
                        _toastMessage.value = doneMsg
                        onSpeak(doneMsg)
                    }.onFailure { saveErr ->
                        val msg = "Detected command but save failed: ${saveErr.localizedMessage ?: "Retry"}"
                        _voiceState.value = VoiceUiState(
                            autoSaveEnabled = currentAutoSave,
                            lastNormalizedTranscript = normalized,
                            pendingIntents = intents,
                            errorMessage = msg
                        )
                    }
                } else {
                    val readBack = intents.joinToString(". ") { intent ->
                        val amtSpoken = if (intent.amount > 0) PakistaniFormatters.speakablePkr(intent.amount) else ""
                        when (intent.intentType) {
                            "SALE" -> "Cash ice sale, $amtSpoken"
                            "CREDIT_SALE" -> "Credit sale to ${intent.customerOrSourceName.ifBlank { "Customer" }}, $amtSpoken"
                            "CREDIT_PAYMENT" -> "Credit payment received from ${intent.customerOrSourceName.ifBlank { "Customer" }}, $amtSpoken"
                            "EXPENSE" -> "${intent.category.ifBlank { "Other" }} expense, $amtSpoken"
                            "FUNDING" -> "Funding from ${intent.customerOrSourceName.ifBlank { intent.category }}, $amtSpoken"
                            "FACTORY_BUILD" -> "Factory build capital expense, $amtSpoken"
                            "PRODUCTION" -> "Production ${PakistaniFormatters.formatTons(intent.tons)}"
                            else -> intent.description
                        }
                    } + ". Please confirm."
                    _voiceState.value = VoiceUiState(
                        autoSaveEnabled = false,
                        lastNormalizedTranscript = normalized,
                        pendingIntents = intents,
                        spokenAnswer = readBack
                    )
                    onSpeak(readBack)
                }
            }
        }.onFailure { err ->
            val msg = err.localizedMessage ?: "Could not process voice command"
            _voiceState.value = VoiceUiState(
                autoSaveEnabled = currentAutoSave,
                lastNormalizedTranscript = normalized,
                errorMessage = msg
            )
        }
    }

    private fun answerVoiceQuery(intent: ParsedVoiceIntent, role: UserRole): String {
        val metrics = dashboardMetrics.value
        val accounting = ownerAccountingSummary.value
        return when (intent.queryType) {
            "staff_denied" -> intent.spokenReply.ifBlank {
                "Restricted: Staff can check Attendance, Salary, or Notices only."
            }
            "notices_summary" -> {
                val activeNotices = notices.value.filter { it.active }
                if (activeNotices.isEmpty()) {
                    "There are no active factory holiday or shift notices right now."
                } else {
                    val top = activeNotices.first()
                    "${top.noticeType} Notice: ${top.title}. ${top.message}"
                }
            }
            "salary_summary" -> {
                val myEmp = employees.value.firstOrNull()
                val mySlips = payroll.value
                val due = mySlips.filter { !it.paid }.sumOf { it.netPay }
                val paid = mySlips.filter { it.paid }.sumOf { it.netPay }
                if (myEmp != null) {
                    "Current salary is ${PakistaniFormatters.speakablePkr(myEmp.salary)}. Total paid ${PakistaniFormatters.speakablePkr(paid)}, due ${PakistaniFormatters.speakablePkr(due)}."
                } else {
                    "Opening your salary and payroll view."
                }
            }
            "attendance_summary" -> {
                if (role.canManageStaff) {
                    "Today ${metrics.staffPresentCount} out of ${metrics.totalStaffCount} staff members are present."
                } else {
                    val mine = attendance.value.firstOrNull { it.date == PakistaniFormatters.todayIso() }
                    if (mine != null) "Your status today is ${mine.status}, check-in at ${mine.checkIn}."
                    else "You have not checked in today."
                }
            }
            "profit_report" -> {
                if (!role.isOwner) {
                    "Access restricted: Profit reports are available to Super Admin only."
                } else {
                    "Total sales so far are ${PakistaniFormatters.speakablePkr(accounting.totalSalesSoFar)}, " +
                        "regular profit so far is ${PakistaniFormatters.speakablePkr(accounting.regularProfitSoFar)}."
                }
            }
            "funding_breakeven" -> {
                if (!role.isOwner) {
                    "Access restricted: Funding and break-even details are available to Super Admin only."
                } else {
                    "Still to repay is ${PakistaniFormatters.speakablePkr(accounting.stillToRepay)}. " +
                        "Factory investment recovery is ${accounting.recoveryPercent.roundToInt()} percent. ${accounting.estimatedCompletionLabel}."
                }
            }
            else -> if (role == UserRole.STAFF) {
                "Staff voice mode: Say Attendance, Salary, or Notices."
            } else {
                intent.spokenReply.ifBlank { "Today sales are ${PakistaniFormatters.speakablePkr(metrics.todaySales)}." }
            }
        }
    }

    fun updatePendingIntent(updated: ParsedVoiceIntent) {
        val next = _voiceState.value.pendingIntents.map { if (it.id == updated.id) updated else it }
        _voiceState.value = _voiceState.value.copy(pendingIntents = next, errorMessage = null)
    }

    fun removePendingIntent(id: String) {
        val next = _voiceState.value.pendingIntents.filterNot { it.id == id }
        _voiceState.value = _voiceState.value.copy(pendingIntents = next, errorMessage = null)
    }

    fun dismissPendingIntents() {
        _voiceState.value = _voiceState.value.copy(
            pendingIntents = emptyList(),
            isSaving = false,
            errorMessage = null
        )
    }

    fun confirmAndSavePendingIntents(onSpeak: (String) -> Unit) = viewModelScope.launch {
        val state = _voiceState.value
        val pending = state.pendingIntents
        if (pending.isEmpty() || state.isSaving) return@launch
        val invalidItem = pending.firstOrNull {
            if (it.intentType == "PRODUCTION") it.tons <= 0.0 else it.amount <= 0.0
        }
        if (invalidItem != null) {
            _voiceState.value = state.copy(
                errorMessage = if (invalidItem.intentType == "PRODUCTION") {
                    "Please enter production quantity in tons greater than 0."
                } else {
                    "Please enter a valid amount greater than Rs. 0 before saving."
                }
            )
            return@launch
        }
        val currentAutoSave = state.autoSaveEnabled
        _voiceState.value = state.copy(isSaving = true, errorMessage = null)
        runCatching {
            saveIntentsInternal(pending)
        }.onSuccess { doneMsg ->
            _voiceState.value = VoiceUiState(
                autoSaveEnabled = currentAutoSave,
                isSaving = false,
                spokenAnswer = doneMsg
            )
            _toastMessage.value = doneMsg
            onSpeak(doneMsg)
        }.onFailure { err ->
            _voiceState.value = _voiceState.value.copy(
                isSaving = false,
                errorMessage = "Save failed: ${err.localizedMessage ?: "Retry"}"
            )
        }
    }

    private suspend fun saveIntentsInternal(intents: List<ParsedVoiceIntent>): String {
        val rate = settings.value.iceBlockRate.takeIf { it > 0 } ?: 1200.0
        val receiptLines = mutableListOf<String>()
        for (intent in intents) {
            val formattedAmt = PakistaniFormatters.formatPkr(intent.amount)
            when (intent.intentType) {
                "SALE" -> {
                    val qty = (intent.amount / rate).toLong().coerceAtLeast(1L)
                    repository.createSale(
                        factoryId = activeFactoryId,
                        callerUid = currentUserId,
                        customerId = "",
                        customerName = intent.customerOrSourceName.ifBlank { "Cash Customer" },
                        product = "Ice Blocks (10kg)",
                        quantity = qty,
                        unitPrice = rate,
                        total = intent.amount,
                        paid = true,
                        source = "voice"
                    )
                    receiptLines.add("✓ Added to Sales (Income): $formattedAmt")
                }
                "CREDIT_SALE" -> {
                    val custName = intent.customerOrSourceName.ifBlank { "Credit Customer" }
                    val existingCust = customers.value.firstOrNull {
                        it.name.contains(custName, ignoreCase = true)
                    }
                    val qty = (intent.amount / rate).toLong().coerceAtLeast(1L)
                    repository.createSale(
                        factoryId = activeFactoryId,
                        callerUid = currentUserId,
                        customerId = existingCust?.id ?: "",
                        customerName = existingCust?.name ?: custName,
                        product = "Ice Blocks (10kg)",
                        quantity = qty,
                        unitPrice = rate,
                        total = intent.amount,
                        paid = false,
                        source = "voice"
                    )
                    receiptLines.add("✓ Added Credit Sale (${existingCust?.name ?: custName}): $formattedAmt")
                }
                "CREDIT_PAYMENT" -> {
                    val custName = intent.customerOrSourceName.trim()
                    val matchedCust = if (custName.isNotBlank()) {
                        customers.value.firstOrNull { it.name.contains(custName, ignoreCase = true) }
                    } else {
                        customers.value.firstOrNull { it.balance > 0 }
                    }
                    if (matchedCust != null) {
                        repository.receiveCustomerPayment(activeFactoryId, matchedCust, intent.amount)
                        receiptLines.add("✓ Credit Payment Received (${matchedCust.name}): $formattedAmt")
                    } else {
                        val qty = (intent.amount / rate).toLong().coerceAtLeast(1L)
                        repository.createSale(
                            factoryId = activeFactoryId,
                            callerUid = currentUserId,
                            customerId = "",
                            customerName = custName.ifBlank { "Credit Recovery" },
                            product = "Credit Recovery",
                            quantity = qty,
                            unitPrice = rate,
                            total = intent.amount,
                            paid = true,
                            source = "voice"
                        )
                        receiptLines.add("✓ Recorded Credit Recovery: $formattedAmt")
                    }
                }
                "EXPENSE" -> {
                    val cat = intent.category.ifBlank { "Other" }
                    repository.addExpense(
                        factoryId = activeFactoryId,
                        callerUid = currentUserId,
                        description = intent.description.ifBlank { cat },
                        amount = intent.amount,
                        category = cat,
                        source = "voice"
                    )
                    receiptLines.add("✓ Added to Expenses ($cat): $formattedAmt")
                }
                "FACTORY_BUILD" -> {
                    repository.addExpense(
                        factoryId = activeFactoryId,
                        callerUid = currentUserId,
                        description = intent.description.ifBlank { "Factory Build Capital Expense" },
                        amount = intent.amount,
                        category = "Factory Build",
                        source = "voice"
                    )
                    receiptLines.add("✓ Added to Factory Build Capital: $formattedAmt")
                }
                "FUNDING" -> {
                    val source = intent.category.ifBlank { "Friend" }
                    repository.addFunding(
                        factoryId = activeFactoryId,
                        source = source,
                        name = intent.customerOrSourceName.ifBlank { "Friend / Lender" },
                        type = "Loan",
                        amount = intent.amount
                    )
                    receiptLines.add("✓ Added to Funding ($source): $formattedAmt")
                }
                "PRODUCTION" -> {
                    val tons = intent.tons.coerceAtLeast(1.0)
                    repository.addProductionAndStock(
                        factoryId = activeFactoryId,
                        callerUid = currentUserId,
                        tons = tons,
                        productType = "Ice Blocks (10kg)",
                        machineId = machines.value.firstOrNull()?.id ?: "mach_1",
                        machineName = machines.value.firstOrNull()?.name ?: "Plant #1",
                        notes = intent.description
                    )
                    receiptLines.add("✓ Added to Production: ${PakistaniFormatters.formatTons(tons)}")
                }
            }
        }
        return receiptLines.joinToString("\n")
    }

    fun consumePendingNavigation() {
        _voiceState.value = _voiceState.value.copy(pendingNavigationKey = null)
    }

    fun resetVoiceState() {
        _voiceState.value = VoiceUiState(autoSaveEnabled = _voiceState.value.autoSaveEnabled)
    }

    companion object {
        fun provideFactory(
            currentUserId: String,
            currentUserName: String,
            currentUserEmail: String?,
            currentUserPhotoUrl: String?
        ): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                FactoryViewModel(
                    repository = FactoryRepository(db),
                    voiceAiRepository = VoiceAiRepository(),
                    preferencesManager = PreferencesManager(app),
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    currentUserEmail = currentUserEmail,
                    currentUserPhotoUrl = currentUserPhotoUrl
                )
            }
        }
    }
}

/**
 * Pure anti-proxy Check-Out guard:
 * - Manual bypass is strictly restricted to Admin / Super Admin explicit bypass actions.
 * - Biometric Check-Out requires the live verified employee to match the attendance record's owner.
 *   In Kiosk mode, only exact `verifiedEmployeeId == record.employeeId` is permitted.
 */
fun canCheckoutAttendanceRecord(
    verifiedEmployeeId: String?,
    verifiedEmployeeAuthUid: String? = null,
    currentUserId: String = "",
    kioskMode: Boolean = false,
    record: AttendanceRecord?,
    isManualBypass: Boolean,
    role: UserRole
): Boolean {
    if (record == null || record.checkOut.isNotBlank()) return false
    if (isManualBypass) return role.canManageStaff
    if (verifiedEmployeeId.isNullOrBlank()) return false
    if (verifiedEmployeeId == record.employeeId) return true
    if (!kioskMode && currentUserId.isNotBlank() &&
        verifiedEmployeeAuthUid == currentUserId &&
        record.employeeId == currentUserId
    ) {
        return true
    }
    return false
}

fun mergeAuditFindings(
    existing: List<AiAuditFinding>,
    incoming: List<AiAuditFinding>,
    maxRetained: Int = 60
): List<AiAuditFinding> {
    if (incoming.isEmpty()) return existing.take(maxRetained)
    val existingKeys = existing.mapTo(HashSet()) {
        "${it.screenKey.lowercase(Locale.US)}|${it.issueTitle.trim().lowercase(Locale.US)}"
    }
    val freshUnique = incoming.filter { item ->
        val key = "${item.screenKey.lowercase(Locale.US)}|${item.issueTitle.trim().lowercase(Locale.US)}"
        key !in existingKeys
    }
    return (freshUnique + existing)
        .sortedByDescending { it.capturedAtMillis }
        .take(maxRetained)
}


