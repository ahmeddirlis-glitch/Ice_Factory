package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppReleaseConfig
import com.example.data.model.AttendanceRecord
import com.example.data.model.Customer
import com.example.data.model.DeliveryItem
import com.example.data.model.Employee
import com.example.data.model.Expense
import com.example.data.model.FactoryDirectoryEntry
import com.example.data.model.FactoryNotice
import com.example.data.model.FactorySettings
import com.example.data.model.FundingEntry
import com.example.data.model.InventoryMovement
import com.example.data.model.MachineItem
import com.example.data.model.PayrollRecord
import com.example.data.model.ProductionRecord
import com.example.data.model.ProfileUpdateRequest
import com.example.data.model.RegistrationRequest
import com.example.data.model.SafetyIncident
import com.example.data.model.Sale
import com.example.data.model.SupplierItem
import com.example.data.model.UserProfile
import com.example.data.model.UtilityReading
import com.example.data.model.WORK_SHIFT_OPTIONS
import com.example.data.util.LocalStorageBootstrap
import com.example.data.util.OperationType
import com.example.data.util.PakistaniFormatters
import com.example.data.util.handleFirestoreError
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.MetadataChanges
import com.google.firebase.firestore.Source
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import java.util.UUID
import kotlin.coroutines.cancellation.CancellationException

class FactoryRepository(private val db: FirebaseFirestore) {

    init {
        LocalStorageBootstrap.configureFirestoreOfflinePersistence(db)
    }

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun factoryDoc(factoryId: String) = db.collection("factories").document(factoryId)

    private suspend fun getLocalFirstOrNull(docRef: DocumentReference): DocumentSnapshot? {
        val cached = runCatching { docRef.get(Source.CACHE).await() }.getOrNull()
        if (cached != null && cached.exists()) return cached
        return runCatching {
            withTimeoutOrNull(1200L) { docRef.get(Source.DEFAULT).await() }
        }.getOrNull() ?: cached
    }

    private suspend fun Task<Void>.awaitLocalFirst(op: OperationType, path: String) {
        try {
            withTimeoutOrNull(1200L) { await() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, op, path)
            throw e
        }
    }

    fun observeHasPendingSync(factoryId: String): Flow<Boolean> {
        val cols = listOf("employees", "attendance", "production", "sales", "expenses")
        val flows = cols.map { col ->
            factoryDoc(factoryId).collection(col)
                .snapshots(MetadataChanges.INCLUDE)
                .map { it.metadata.hasPendingWrites() }
                .catch { emit(false) }
        }
        return combine(flows) { flags -> flags.any { it } }
    }

    private fun <T> Flow<List<T>>.withFirestoreErrorLog(
        op: OperationType,
        path: String
    ): Flow<List<T>> = catch { error ->
        val firestoreError = generateSequence(error) { it.cause }
            .filterIsInstance<FirebaseFirestoreException>()
            .firstOrNull()
        if (firestoreError == null && error is CancellationException) throw error
        handleFirestoreError(firestoreError ?: error, op, path)
        throw error
    }

    suspend fun getPrimaryFactoryEntry(): FactoryDirectoryEntry? {
        val docRef = db.collection("factory_directory").document("primary")
        val snap = runCatching {
            withTimeoutOrNull(2500L) { docRef.get(Source.SERVER).await() }
        }.getOrNull() ?: getLocalFirstOrNull(docRef)
        return snap?.takeIf { it.exists() }?.toObject(FactoryDirectoryEntry::class.java)
    }

    /**
     * Resolves the shared factory workspace ID (`factory_directory/primary`).
     * If no primary factory exists yet and [allowClaimIfMissing] is true, the caller claims primary Super Admin ownership.
     * Returns Pair(factoryId, isPrimaryOwner).
     */
    suspend fun resolveOrClaimPrimaryFactory(
        callerUid: String,
        callerName: String,
        allowClaimIfMissing: Boolean
    ): Pair<String, Boolean> {
        val existing = getPrimaryFactoryEntry()
        if (existing != null && existing.ownerId.isNotBlank()) {
            return existing.ownerId to (existing.ownerId == callerUid)
        }
        if (!allowClaimIfMissing) {
            return callerUid to false
        }
        val docRef = db.collection("factory_directory").document("primary")
        val cleanOwner = PakistaniFormatters.formatTitleCase(callerName.ifBlank { "Super Admin" })
        val payload = mapOf(
            "ownerId" to callerUid,
            "companyName" to "Brohi Ice Factory",
            "ownerName" to cleanOwner,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        return try {
            docRef.set(payload).awaitLocalFirst(OperationType.CREATE, docRef.path)
            callerUid to true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val fallback = getPrimaryFactoryEntry()
            if (fallback != null && fallback.ownerId.isNotBlank()) {
                fallback.ownerId to (fallback.ownerId == callerUid)
            } else {
                callerUid to true
            }
        }
    }

    suspend fun getUserProfileServerOrLocal(userId: String): UserProfile? {
        val docRef = db.collection("users").document(userId)
        val snap = runCatching {
            withTimeoutOrNull(2500L) { docRef.get(Source.SERVER).await() }
        }.getOrNull() ?: getLocalFirstOrNull(docRef)
        return snap?.takeIf { it.exists() }?.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
    }

    // --- User Profile ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> {
        val path = "users/$userId"
        return db.collection("users").document(userId)
            .snapshots()
            .map { snap -> snap.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                handleFirestoreError(fe ?: error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun upsertUserProfile(
        userId: String,
        displayName: String,
        email: String?,
        phone: String?,
        photoUrl: String?,
        preferredLanguage: String = "en",
        darkMode: Boolean = false,
        approvalStatus: String? = null,
        requestedRole: String? = null,
        linkedFactoryId: String? = null,
        faceEnrolled: Boolean? = null,
        approvedAtMillis: Long? = null,
        clearPhoto: Boolean = false
    ) {
        val docRef = db.collection("users").document(userId)
        val normalizedName = PakistaniFormatters.formatTitleCase(displayName.ifBlank { "Factory User" })
        val normalizedPhone = phone?.takeIf { it.isNotBlank() }?.let { PakistaniFormatters.formatPhone(it) }
        try {
            val existing = getLocalFirstOrNull(docRef)
            if (existing != null && existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "displayName" to normalizedName,
                    "preferredLanguage" to preferredLanguage,
                    "darkMode" to darkMode,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!email.isNullOrBlank()) updates["email"] = email
                if (!normalizedPhone.isNullOrBlank()) updates["phone"] = normalizedPhone
                if (clearPhoto) {
                    updates["photoUrl"] = ""
                } else if (!photoUrl.isNullOrBlank()) {
                    updates["photoUrl"] = photoUrl
                }
                if (!approvalStatus.isNullOrBlank()) updates["approvalStatus"] = approvalStatus
                if (!requestedRole.isNullOrBlank()) updates["requestedRole"] = requestedRole
                if (!linkedFactoryId.isNullOrBlank()) updates["linkedFactoryId"] = linkedFactoryId
                if (faceEnrolled != null) updates["faceEnrolled"] = faceEnrolled
                if (approvedAtMillis != null) updates["approvedAtMillis"] = approvedAtMillis
                docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
            } else {
                val payload = mapOf(
                    "userId" to userId,
                    "displayName" to normalizedName,
                    "email" to email,
                    "phone" to normalizedPhone,
                    "photoUrl" to photoUrl,
                    "preferredLanguage" to preferredLanguage,
                    "darkMode" to darkMode,
                    "approvalStatus" to (approvalStatus ?: "PENDING"),
                    "requestedRole" to (requestedRole ?: "Staff"),
                    "linkedFactoryId" to linkedFactoryId,
                    "faceEnrolled" to (faceEnrolled ?: false),
                    "approvedAtMillis" to approvedAtMillis,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).awaitLocalFirst(OperationType.CREATE, docRef.path)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Settings ---
    fun observeSettings(factoryId: String): Flow<FactorySettings?> {
        val path = "factories/$factoryId/settings/config"
        return factoryDoc(factoryId).collection("settings").document("config")
            .snapshots()
            .map { snap -> snap.toObject(FactorySettings::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                handleFirestoreError(fe ?: error, OperationType.GET, path)
                throw error
            }
    }

    suspend fun saveSettings(factoryId: String, settings: FactorySettings) {
        val docRef = factoryDoc(factoryId).collection("settings").document("config")
        try {
            val existing = getLocalFirstOrNull(docRef)
            if (existing != null && existing.exists()) {
                docRef.update(
                    mapOf(
                        "companyName" to settings.companyName,
                        "phone" to PakistaniFormatters.formatPhone(settings.phone),
                        "address" to settings.address,
                        "iceBlockRate" to settings.iceBlockRate,
                        "iceCubeRate" to settings.iceCubeRate,
                        "operationsStartDate" to settings.operationsStartDate,
                        "notificationsEnabled" to settings.notificationsEnabled,
                        "seededDemo" to settings.seededDemo,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitLocalFirst(OperationType.UPDATE, docRef.path)
            } else {
                docRef.set(
                    mapOf(
                        "ownerId" to factoryId,
                        "companyName" to settings.companyName,
                        "phone" to PakistaniFormatters.formatPhone(settings.phone),
                        "address" to settings.address,
                        "iceBlockRate" to settings.iceBlockRate,
                        "iceCubeRate" to settings.iceCubeRate,
                        "operationsStartDate" to settings.operationsStartDate,
                        "notificationsEnabled" to settings.notificationsEnabled,
                        "seededDemo" to settings.seededDemo,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitLocalFirst(OperationType.CREATE, docRef.path)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Employees (Admin/Super Admin see all; Staff sees own profile via authUid) ---
    fun observeEmployees(
        factoryId: String,
        callerUid: String,
        canManageAll: Boolean
    ): Flow<List<Employee>> {
        val path = "factories/$factoryId/employees"
        val query = if (canManageAll || callerUid == factoryId) {
            factoryDoc(factoryId).collection("employees")
        } else {
            factoryDoc(factoryId).collection("employees").whereEqualTo("authUid", callerUid)
        }
        return query.snapshots()
            .map { it.toObjects(Employee::class.java, ServerTimestampBehavior.ESTIMATE) }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun saveEmployee(factoryId: String, employee: Employee): String {
        val docId = employee.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = factoryDoc(factoryId).collection("employees").document(docId)
        val cleanName = PakistaniFormatters.formatTitleCase(employee.name)
        val cleanFather = PakistaniFormatters.formatTitleCase(employee.fatherName)
        val cleanCnic = PakistaniFormatters.formatCnic(employee.cnic)
        val cleanPhone = PakistaniFormatters.formatPhone(employee.phone)
        val cleanWhatsapp = PakistaniFormatters.formatPhone(employee.whatsappNumber)
        val cleanEmergency = PakistaniFormatters.formatPhone(employee.emergencyContact)
        try {
            val existing = if (employee.id.isNotBlank()) getLocalFirstOrNull(docRef) else null
            val resolvedInitial = if (employee.initialSalary > 0.0) employee.initialSalary else employee.salary
            if (existing != null && existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "name" to cleanName,
                    "fatherName" to cleanFather,
                    "phone" to cleanPhone,
                    "whatsappNumber" to cleanWhatsapp,
                    "email" to employee.email,
                    "cnic" to cleanCnic,
                    "role" to employee.role,
                    "salary" to employee.salary,
                    "initialSalary" to resolvedInitial,
                    "shift" to employee.shift,
                    "joinDate" to employee.joinDate.ifBlank { PakistaniFormatters.todayIso() },
                    "emergencyContact" to cleanEmergency,
                    "skipLiveness" to employee.skipLiveness,
                    "active" to employee.active,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (!employee.authUid.isNullOrBlank()) updates["authUid"] = employee.authUid
                if (!employee.faceTemplateEncrypted.isNullOrBlank()) {
                    updates["faceTemplateEncrypted"] = employee.faceTemplateEncrypted
                }
                if (employee.idCardPhoto != null) {
                    updates["idCardPhoto"] = employee.idCardPhoto
                }
                if (!employee.attendanceFacePhoto.isNullOrBlank()) {
                    updates["attendanceFacePhoto"] = employee.attendanceFacePhoto
                }
                docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
            } else {
                val payload = mapOf(
                    "ownerId" to factoryId,
                    "authUid" to employee.authUid,
                    "name" to cleanName,
                    "fatherName" to cleanFather,
                    "phone" to cleanPhone,
                    "whatsappNumber" to cleanWhatsapp,
                    "email" to employee.email,
                    "cnic" to cleanCnic,
                    "role" to employee.role,
                    "salary" to employee.salary,
                    "initialSalary" to resolvedInitial,
                    "shift" to employee.shift,
                    "joinDate" to employee.joinDate.ifBlank { PakistaniFormatters.todayIso() },
                    "emergencyContact" to cleanEmergency,
                    "skipLiveness" to employee.skipLiveness,
                    "faceTemplateEncrypted" to employee.faceTemplateEncrypted,
                    "idCardPhoto" to employee.idCardPhoto,
                    "attendanceFacePhoto" to employee.attendanceFacePhoto,
                    "active" to employee.active,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).awaitLocalFirst(OperationType.CREATE, docRef.path)
            }
            val linkedAuthUid = employee.authUid?.takeIf { it.isNotBlank() }
            if (linkedAuthUid != null) {
                runCatching {
                    val userRef = db.collection("users").document(linkedAuthUid)
                    val userSnap = getLocalFirstOrNull(userRef)
                    if (userSnap != null && userSnap.exists()) {
                        val userUpdates = mutableMapOf<String, Any>(
                            "displayName" to cleanName,
                            "phone" to cleanPhone,
                            "requestedRole" to employee.role,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                        if (employee.email.isNotBlank()) userUpdates["email"] = employee.email.trim()
                        userRef.update(userUpdates).awaitLocalFirst(OperationType.UPDATE, userRef.path)
                    }
                }
            }
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun saveFullDirectProfile(
        factoryId: String,
        userId: String,
        existingEmployee: Employee?,
        name: String,
        fatherName: String,
        cnic: String,
        phone: String,
        whatsappNumber: String,
        email: String,
        emergencyContact: String,
        role: String,
        shift: String,
        salary: Double,
        joinDate: String,
        photoUrl: String?,
        clearPhoto: Boolean = false,
        preferredLanguage: String = "en",
        darkMode: Boolean = false
    ) {
        val cleanName = PakistaniFormatters.formatTitleCase(name.ifBlank { "Factory User" })
        val cleanPhone = PakistaniFormatters.formatPhone(phone)
        upsertUserProfile(
            userId = userId,
            displayName = cleanName,
            email = email.trim().ifBlank { null },
            phone = cleanPhone,
            photoUrl = photoUrl,
            preferredLanguage = preferredLanguage,
            darkMode = darkMode,
            requestedRole = role.ifBlank { null },
            clearPhoto = clearPhoto
        )
        val empToSave = (existingEmployee ?: Employee(
            id = userId,
            ownerId = factoryId,
            authUid = userId,
            active = true
        )).copy(
            ownerId = factoryId,
            authUid = existingEmployee?.authUid?.takeIf { it.isNotBlank() } ?: userId,
            name = cleanName,
            fatherName = PakistaniFormatters.formatTitleCase(fatherName),
            cnic = PakistaniFormatters.formatCnic(cnic),
            phone = cleanPhone,
            whatsappNumber = PakistaniFormatters.formatPhone(whatsappNumber),
            email = email.trim(),
            emergencyContact = PakistaniFormatters.formatPhone(emergencyContact),
            role = role.ifBlank { existingEmployee?.role ?: "Staff" },
            shift = shift.ifBlank { existingEmployee?.shift ?: WORK_SHIFT_OPTIONS.first() },
            salary = salary.coerceAtLeast(0.0),
            initialSalary = existingEmployee?.initialSalary?.takeIf { it > 0.0 } ?: salary.coerceAtLeast(0.0),
            joinDate = joinDate.trim().ifBlank { PakistaniFormatters.todayIso() }
        )
        saveEmployee(factoryId, empToSave)
    }

    suspend fun updateEmployeeFaceTemplate(
        factoryId: String,
        employeeId: String,
        encryptedTemplate: String,
        employeeAuthUid: String? = null,
        attendanceFacePhoto: String? = null
    ) {
        val docRef = factoryDoc(factoryId).collection("employees").document(employeeId)
        try {
            val updates = mutableMapOf<String, Any>(
                "faceTemplateEncrypted" to encryptedTemplate,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!attendanceFacePhoto.isNullOrBlank()) {
                updates["attendanceFacePhoto"] = attendanceFacePhoto
            }
            docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
            val targetAuthUid = employeeAuthUid?.takeIf { it.isNotBlank() }
                ?: getLocalFirstOrNull(docRef)?.getString("authUid")?.takeIf { !it.isNullOrBlank() }
            if (!targetAuthUid.isNullOrBlank()) {
                val userRef = db.collection("users").document(targetAuthUid)
                runCatching {
                    val userSnap = getLocalFirstOrNull(userRef)
                    if (userSnap != null && userSnap.exists()) {
                        userRef.update(
                            mapOf(
                                "faceEnrolled" to true,
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        ).awaitLocalFirst(OperationType.UPDATE, userRef.path)
                    }
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateEmployeeIdCardPhoto(
        factoryId: String,
        employeeId: String,
        idCardPhoto: String?
    ) {
        val docRef = factoryDoc(factoryId).collection("employees").document(employeeId)
        try {
            docRef.update(
                mapOf(
                    "idCardPhoto" to (idCardPhoto ?: ""),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitLocalFirst(OperationType.UPDATE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateEmployeeAttendanceFacePhoto(
        factoryId: String,
        employeeId: String,
        attendanceFacePhoto: String
    ) {
        if (attendanceFacePhoto.isBlank()) return
        val docRef = factoryDoc(factoryId).collection("employees").document(employeeId)
        try {
            docRef.update(
                mapOf(
                    "attendanceFacePhoto" to attendanceFacePhoto,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitLocalFirst(OperationType.UPDATE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun toggleEmployeeSkipLiveness(
        factoryId: String,
        employeeId: String,
        skipLiveness: Boolean
    ) {
        val docRef = factoryDoc(factoryId).collection("employees").document(employeeId)
        try {
            docRef.update(
                mapOf(
                    "skipLiveness" to skipLiveness,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitLocalFirst(OperationType.UPDATE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Attendance ---
    fun observeAttendance(factoryId: String, callerUid: String, canReadAll: Boolean): Flow<List<AttendanceRecord>> {
        val path = "factories/$factoryId/attendance"
        val query = if (canReadAll || callerUid == factoryId) {
            factoryDoc(factoryId).collection("attendance")
        } else {
            factoryDoc(factoryId).collection("attendance").whereEqualTo("authUid", callerUid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(AttendanceRecord::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun recordCheckIn(
        factoryId: String,
        employeeId: String,
        employeeName: String,
        authUid: String,
        bypassed: Boolean
    ): String {
        val docId = UUID.randomUUID().toString()
        val docRef = factoryDoc(factoryId).collection("attendance").document(docId)
        val nowTime = PakistaniFormatters.currentTimeDisplay()
        val today = PakistaniFormatters.todayIso()
        val payload = mapOf(
            "ownerId" to factoryId,
            "employeeId" to employeeId,
            "employeeName" to employeeName,
            "authUid" to authUid,
            "date" to today,
            "checkIn" to nowTime,
            "checkOut" to "",
            "status" to "Present",
            "bypassed" to bypassed,
            "hours" to 0.0,
            "memberUids" to listOf(factoryId, authUid).distinct(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).awaitLocalFirst(OperationType.CREATE, docRef.path)
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun recordCheckOut(
        factoryId: String,
        attendance: AttendanceRecord,
        bypassed: Boolean = false
    ) {
        require(attendance.id.isNotBlank())
        val docRef = factoryDoc(factoryId).collection("attendance").document(attendance.id)
        val nowTime = PakistaniFormatters.currentTimeDisplay()
        val hours = PakistaniFormatters.calculateHoursBetween(attendance.checkIn, nowTime)
        val updates = mutableMapOf<String, Any>(
            "checkOut" to nowTime,
            "status" to "Checked Out",
            "hours" to hours,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (bypassed) {
            updates["bypassed"] = true
        }
        try {
            docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun saveAttendanceRecord(factoryId: String, callerUid: String, record: AttendanceRecord): String {
        val docId = record.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = factoryDoc(factoryId).collection("attendance").document(docId)
        val hours = if (record.checkIn.isNotBlank() && record.checkOut.isNotBlank()) {
            PakistaniFormatters.calculateHoursBetween(record.checkIn, record.checkOut)
        } else {
            record.hours.coerceAtLeast(0.0)
        }
        try {
            val existing = if (record.id.isNotBlank()) getLocalFirstOrNull(docRef) else null
            if (existing != null && existing.exists()) {
                docRef.update(
                    mapOf(
                        "employeeId" to record.employeeId,
                        "employeeName" to record.employeeName,
                        "date" to record.date.ifBlank { PakistaniFormatters.todayIso() },
                        "checkIn" to record.checkIn,
                        "checkOut" to record.checkOut,
                        "status" to record.status,
                        "bypassed" to record.bypassed,
                        "hours" to hours,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitLocalFirst(OperationType.UPDATE, docRef.path)
            } else {
                val authUid = record.authUid.ifBlank { callerUid }
                docRef.set(
                    mapOf(
                        "ownerId" to factoryId,
                        "employeeId" to record.employeeId,
                        "employeeName" to record.employeeName,
                        "authUid" to authUid,
                        "date" to record.date.ifBlank { PakistaniFormatters.todayIso() },
                        "checkIn" to record.checkIn,
                        "checkOut" to record.checkOut,
                        "status" to record.status,
                        "bypassed" to record.bypassed,
                        "hours" to hours,
                        "memberUids" to listOf(factoryId, authUid).distinct(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitLocalFirst(OperationType.CREATE, docRef.path)
            }
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Production ---
    fun observeProduction(factoryId: String, callerUid: String, canReadAll: Boolean = true): Flow<List<ProductionRecord>> {
        val path = "factories/$factoryId/production"
        val query = if (canReadAll || callerUid == factoryId) {
            factoryDoc(factoryId).collection("production")
        } else {
            factoryDoc(factoryId).collection("production").whereArrayContains("memberUids", callerUid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(ProductionRecord::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun addProductionAndStock(
        factoryId: String,
        callerUid: String,
        tons: Double,
        productType: String,
        machineId: String,
        machineName: String,
        notes: String? = null,
        date: String = PakistaniFormatters.todayIso()
    ): String {
        val prodId = UUID.randomUUID().toString()
        val invId = UUID.randomUUID().toString()
        val prodRef = factoryDoc(factoryId).collection("production").document(prodId)
        val invRef = factoryDoc(factoryId).collection("inventory").document(invId)
        val members = listOf(factoryId, callerUid).distinct()

        val prodPayload = mapOf(
            "ownerId" to factoryId,
            "date" to date,
            "tons" to tons,
            "productType" to productType,
            "machineId" to machineId,
            "machineName" to machineName,
            "notes" to notes,
            "createdBy" to callerUid,
            "memberUids" to members,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }

        val stockProduct = if (productType.contains("Cube", ignoreCase = true)) "Ice Cubes" else "Ice Blocks"
        val invPayload = mapOf(
            "ownerId" to factoryId,
            "product" to stockProduct,
            "tons" to tons,
            "location" to "Cold Room A",
            "movementType" to "IN",
            "notes" to "Auto-added from production ($machineName)",
            "createdBy" to callerUid,
            "memberUids" to members,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )

        try {
            val batch = db.batch()
            batch.set(prodRef, prodPayload)
            batch.set(invRef, invPayload)
            batch.commit().awaitLocalFirst(OperationType.CREATE, prodRef.path)
            return prodId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, prodRef.path)
            throw e
        }
    }

    // --- Inventory ---
    fun observeInventory(factoryId: String, callerUid: String, canReadAll: Boolean = true): Flow<List<InventoryMovement>> {
        val path = "factories/$factoryId/inventory"
        val query = if (canReadAll || callerUid == factoryId) {
            factoryDoc(factoryId).collection("inventory")
        } else {
            factoryDoc(factoryId).collection("inventory").whereArrayContains("memberUids", callerUid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(InventoryMovement::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun addInventoryMovement(
        factoryId: String,
        callerUid: String,
        product: String,
        tons: Double,
        location: String,
        movementType: String,
        notes: String?
    ): String {
        val docId = UUID.randomUUID().toString()
        val docRef = factoryDoc(factoryId).collection("inventory").document(docId)
        val payload = mapOf(
            "ownerId" to factoryId,
            "product" to product,
            "tons" to tons,
            "location" to location,
            "movementType" to movementType,
            "notes" to notes,
            "createdBy" to callerUid,
            "memberUids" to listOf(factoryId, callerUid).distinct(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        try {
            docRef.set(payload).awaitLocalFirst(OperationType.CREATE, docRef.path)
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Customers & Credit ---
    fun observeCustomers(factoryId: String, callerUid: String, canReadAll: Boolean = true): Flow<List<Customer>> {
        val path = "factories/$factoryId/customers"
        val query = if (canReadAll || callerUid == factoryId) {
            factoryDoc(factoryId).collection("customers")
        } else {
            factoryDoc(factoryId).collection("customers").whereArrayContains("memberUids", callerUid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(Customer::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.balance }
            }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun addCustomer(
        factoryId: String,
        callerUid: String,
        name: String,
        phone: String,
        address: String?,
        initialBalance: Double = 0.0
    ): String {
        val docId = UUID.randomUUID().toString()
        val docRef = factoryDoc(factoryId).collection("customers").document(docId)
        val payload = mapOf(
            "ownerId" to factoryId,
            "name" to PakistaniFormatters.formatTitleCase(name),
            "phone" to PakistaniFormatters.formatPhone(phone),
            "address" to address,
            "balance" to initialBalance.coerceAtLeast(0.0),
            "memberUids" to listOf(factoryId, callerUid).distinct(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        try {
            docRef.set(payload).awaitLocalFirst(OperationType.CREATE, docRef.path)
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun receiveCustomerPayment(factoryId: String, customer: Customer, paymentAmount: Double) {
        require(customer.id.isNotBlank())
        val docRef = factoryDoc(factoryId).collection("customers").document(customer.id)
        val newBalance = (customer.balance - paymentAmount).coerceAtLeast(0.0)
        try {
            docRef.update(
                mapOf(
                    "balance" to newBalance,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitLocalFirst(OperationType.UPDATE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Sales & Invoicing ---
    fun observeSales(factoryId: String, callerUid: String, canReadAll: Boolean = true): Flow<List<Sale>> {
        val path = "factories/$factoryId/sales"
        val query = if (canReadAll || callerUid == factoryId) {
            factoryDoc(factoryId).collection("sales")
        } else {
            factoryDoc(factoryId).collection("sales").whereArrayContains("memberUids", callerUid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(Sale::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun createSale(
        factoryId: String,
        callerUid: String,
        customerId: String,
        customerName: String,
        product: String,
        quantity: Long,
        unitPrice: Double,
        total: Double,
        paid: Boolean,
        source: String = "manual",
        date: String = PakistaniFormatters.todayIso()
    ): String {
        val saleId = UUID.randomUUID().toString()
        val saleRef = factoryDoc(factoryId).collection("sales").document(saleId)
        val invoiceNum = "BIF-${(1000..9999).random()}"
        val members = listOf(factoryId, callerUid).distinct()
        val resolvedCustomerName = customerName.ifBlank { "Walk-in Customer" }

        var resolvedCustomerId = customerId
        val batch = db.batch()

        if (!paid) {
            if (resolvedCustomerId.isBlank()) {
                // Create or attach a customer ledger doc for credit sale
                val newCustId = UUID.randomUUID().toString()
                resolvedCustomerId = newCustId
                val custRef = factoryDoc(factoryId).collection("customers").document(newCustId)
                batch.set(
                    custRef,
                    mapOf(
                        "ownerId" to factoryId,
                        "name" to resolvedCustomerName,
                        "phone" to "",
                        "balance" to total,
                        "memberUids" to members,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            } else {
                val custRef = factoryDoc(factoryId).collection("customers").document(resolvedCustomerId)
                batch.update(
                    custRef,
                    mapOf(
                        "balance" to FieldValue.increment(total),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }

        val salePayload = mapOf(
            "ownerId" to factoryId,
            "invoiceNumber" to invoiceNum,
            "customerId" to resolvedCustomerId,
            "customerName" to resolvedCustomerName,
            "product" to product,
            "quantity" to quantity.coerceAtLeast(1L),
            "unitPrice" to unitPrice,
            "total" to total,
            "paid" to paid,
            "paidAmount" to (if (paid) total else 0.0),
            "date" to date,
            "createdBy" to callerUid,
            "source" to source,
            "memberUids" to members,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        batch.set(saleRef, salePayload)

        try {
            batch.commit().awaitLocalFirst(OperationType.CREATE, saleRef.path)
            return saleId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, saleRef.path)
            throw e
        }
    }

    suspend fun markSalePaid(factoryId: String, sale: Sale) {
        val saleRef = factoryDoc(factoryId).collection("sales").document(sale.id)
        try {
            saleRef.update(
                mapOf(
                    "paid" to true,
                    "paidAmount" to sale.total,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitLocalFirst(OperationType.UPDATE, saleRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, saleRef.path)
            throw e
        }
    }

    // --- Expenses (Factory Build filtered out for non-Super-Admins) ---
    fun observeExpenses(
        factoryId: String,
        callerUid: String,
        isOwner: Boolean,
        canReadOperatingExpenses: Boolean = true
    ): Flow<List<Expense>> {
        val path = "factories/$factoryId/expenses"
        val query = when {
            isOwner -> factoryDoc(factoryId).collection("expenses")
            canReadOperatingExpenses -> factoryDoc(factoryId).collection("expenses")
                .whereEqualTo("capitalInvestment", false)
            else -> factoryDoc(factoryId).collection("expenses")
                .whereArrayContains("memberUids", callerUid)
                .whereEqualTo("capitalInvestment", false)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(Expense::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun addExpense(
        factoryId: String,
        callerUid: String,
        description: String,
        amount: Double,
        category: String,
        source: String = "manual",
        date: String = PakistaniFormatters.todayIso()
    ): String {
        val docId = UUID.randomUUID().toString()
        val docRef = factoryDoc(factoryId).collection("expenses").document(docId)
        val isCapital = category.equals("Factory Build", ignoreCase = true)
        val normalizedCategory = if (isCapital) "Factory Build" else category
        val payload = mapOf(
            "ownerId" to factoryId,
            "description" to description,
            "amount" to amount,
            "category" to normalizedCategory,
            "capitalInvestment" to isCapital,
            "date" to date,
            "createdBy" to callerUid,
            "source" to source,
            "memberUids" to listOf(factoryId, callerUid).distinct(),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).awaitLocalFirst(OperationType.CREATE, docRef.path)
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    // --- Funding (Owner only) ---
    fun observeFunding(factoryId: String): Flow<List<FundingEntry>> {
        val path = "factories/$factoryId/funding"
        return factoryDoc(factoryId).collection("funding")
            .snapshots()
            .map { snap ->
                snap.toObjects(FundingEntry::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun addFunding(
        factoryId: String,
        source: String,
        name: String,
        type: String,
        amount: Double,
        repaid: Double = 0.0,
        interest: Double = 0.0,
        dueDate: String? = null,
        notes: String? = null,
        date: String = PakistaniFormatters.todayIso()
    ): String {
        val docId = UUID.randomUUID().toString()
        val docRef = factoryDoc(factoryId).collection("funding").document(docId)
        val payload = mapOf(
            "ownerId" to factoryId,
            "source" to source,
            "name" to name,
            "type" to type,
            "amount" to amount,
            "repaid" to repaid,
            "interest" to interest,
            "dueDate" to dueDate,
            "date" to date,
            "notes" to notes,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        try {
            docRef.set(payload).awaitLocalFirst(OperationType.CREATE, docRef.path)
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun recordFundingRepayment(factoryId: String, funding: FundingEntry, repayAmount: Double) {
        val docRef = factoryDoc(factoryId).collection("funding").document(funding.id)
        val newRepaid = (funding.repaid + repayAmount).coerceAtMost(funding.amount)
        try {
            docRef.update(
                mapOf(
                    "repaid" to newRepaid,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitLocalFirst(OperationType.UPDATE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    // --- Payroll (Super Admin sees all; Staff sees own slips by authUid) ---
    fun observePayroll(factoryId: String, callerUid: String, isOwner: Boolean): Flow<List<PayrollRecord>> {
        val path = "factories/$factoryId/payroll"
        val query = if (isOwner) {
            factoryDoc(factoryId).collection("payroll")
        } else {
            factoryDoc(factoryId).collection("payroll").whereEqualTo("authUid", callerUid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(PayrollRecord::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun savePayroll(factoryId: String, record: PayrollRecord): String {
        val docId = record.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = factoryDoc(factoryId).collection("payroll").document(docId)
        val netPay = (record.baseSalary - record.advances - record.deductions).coerceAtLeast(0.0)
        val payload = mapOf(
            "ownerId" to factoryId,
            "employeeId" to record.employeeId,
            "authUid" to record.authUid.ifBlank { null },
            "employeeName" to record.employeeName,
            "month" to record.month,
            "baseSalary" to record.baseSalary,
            "advances" to record.advances,
            "deductions" to record.deductions,
            "netPay" to netPay,
            "paid" to record.paid,
            "paidDate" to record.paidDate,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        try {
            docRef.set(payload).awaitLocalFirst(OperationType.CREATE, docRef.path)
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
    }

    suspend fun markSalaryPaidAndCreateExpense(factoryId: String, callerUid: String, payroll: PayrollRecord) {
        val payRef = factoryDoc(factoryId).collection("payroll").document(payroll.id)
        val expId = UUID.randomUUID().toString()
        val expRef = factoryDoc(factoryId).collection("expenses").document(expId)
        val today = PakistaniFormatters.todayIso()
        val batch = db.batch()
        batch.update(
            payRef,
            mapOf(
                "paid" to true,
                "paidDate" to today,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            expRef,
            mapOf(
                "ownerId" to factoryId,
                "description" to "Salary paid to ${payroll.employeeName} (${payroll.month})",
                "amount" to payroll.netPay,
                "category" to "Salary",
                "capitalInvestment" to false,
                "date" to today,
                "createdBy" to callerUid,
                "source" to "manual",
                "memberUids" to listOf(factoryId),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        try {
            batch.commit().awaitLocalFirst(OperationType.UPDATE, payRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, payRef.path)
            throw e
        }
    }

    // --- Utilities (Electricity / Diesel / Water) ---
    fun observeUtilities(factoryId: String, callerUid: String, canReadAll: Boolean = true): Flow<List<UtilityReading>> {
        val path = "factories/$factoryId/utilities"
        val query = if (canReadAll || callerUid == factoryId) {
            factoryDoc(factoryId).collection("utilities")
        } else {
            factoryDoc(factoryId).collection("utilities").whereArrayContains("memberUids", callerUid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(UtilityReading::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun saveUtilityAndCreateExpense(
        factoryId: String,
        callerUid: String,
        type: String,
        meterReading: Double,
        unitsOrLiters: Double,
        amount: Double,
        notes: String?
    ) {
        val utilId = UUID.randomUUID().toString()
        val expId = UUID.randomUUID().toString()
        val utilRef = factoryDoc(factoryId).collection("utilities").document(utilId)
        val expRef = factoryDoc(factoryId).collection("expenses").document(expId)
        val today = PakistaniFormatters.todayIso()
        val members = listOf(factoryId, callerUid).distinct()

        val batch = db.batch()
        batch.set(
            utilRef,
            mapOf(
                "ownerId" to factoryId,
                "type" to type,
                "meterReading" to meterReading,
                "unitsOrLiters" to unitsOrLiters,
                "amount" to amount,
                "date" to today,
                "notes" to notes,
                "memberUids" to members,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
        )
        if (amount > 0) {
            batch.set(
                expRef,
                mapOf(
                    "ownerId" to factoryId,
                    "description" to "$type bill/usage ($unitsOrLiters units)",
                    "amount" to amount,
                    "category" to type,
                    "capitalInvestment" to false,
                    "date" to today,
                    "createdBy" to callerUid,
                    "source" to "manual",
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        try {
            batch.commit().awaitLocalFirst(OperationType.CREATE, utilRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, utilRef.path)
            throw e
        }
    }

    // --- Deliveries & Vehicles ---
    fun observeDeliveries(factoryId: String, callerUid: String, canReadAll: Boolean = true): Flow<List<DeliveryItem>> {
        val path = "factories/$factoryId/deliveries"
        val query = if (canReadAll || callerUid == factoryId) {
            factoryDoc(factoryId).collection("deliveries")
        } else {
            factoryDoc(factoryId).collection("deliveries").whereArrayContains("memberUids", callerUid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(DeliveryItem::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun saveDelivery(factoryId: String, callerUid: String, delivery: DeliveryItem) {
        val docId = delivery.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = factoryDoc(factoryId).collection("deliveries").document(docId)
        try {
            val existing = if (delivery.id.isNotBlank()) getLocalFirstOrNull(docRef) else null
            if (existing != null && existing.exists()) {
                docRef.update(
                    mapOf(
                        "vehicleNumber" to delivery.vehicleNumber,
                        "driverName" to delivery.driverName,
                        "route" to delivery.route,
                        "customerName" to delivery.customerName,
                        "tons" to delivery.tons,
                        "status" to delivery.status,
                        "date" to delivery.date.ifBlank { PakistaniFormatters.todayIso() },
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitLocalFirst(OperationType.UPDATE, docRef.path)
            } else {
                docRef.set(
                    mapOf(
                        "ownerId" to factoryId,
                        "vehicleNumber" to delivery.vehicleNumber,
                        "driverName" to delivery.driverName,
                        "route" to delivery.route,
                        "customerName" to delivery.customerName,
                        "tons" to delivery.tons,
                        "status" to delivery.status,
                        "date" to delivery.date.ifBlank { PakistaniFormatters.todayIso() },
                        "memberUids" to listOf(factoryId, callerUid).distinct(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).awaitLocalFirst(OperationType.CREATE, docRef.path)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Machines & Maintenance ---
    fun observeMachines(factoryId: String, callerUid: String, canReadAll: Boolean = true): Flow<List<MachineItem>> {
        val path = "factories/$factoryId/machines"
        val query = if (canReadAll || callerUid == factoryId) {
            factoryDoc(factoryId).collection("machines")
        } else {
            factoryDoc(factoryId).collection("machines").whereArrayContains("memberUids", callerUid)
        }
        return query.snapshots()
            .map { snap -> snap.toObjects(MachineItem::class.java, ServerTimestampBehavior.ESTIMATE) }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun saveMachine(factoryId: String, callerUid: String, machine: MachineItem) {
        val docId = machine.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = factoryDoc(factoryId).collection("machines").document(docId)
        try {
            val existing = if (machine.id.isNotBlank()) getLocalFirstOrNull(docRef) else null
            if (existing != null && existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "name" to machine.name,
                    "model" to machine.model,
                    "capacityTonsPerDay" to machine.capacityTonsPerDay,
                    "lastServicedDate" to machine.lastServicedDate,
                    "nextServiceDate" to machine.nextServiceDate,
                    "status" to machine.status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (machine.breakdownNote != null) updates["breakdownNote"] = machine.breakdownNote
                docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
            } else {
                docRef.set(
                    mapOf(
                        "ownerId" to factoryId,
                        "name" to machine.name,
                        "model" to machine.model,
                        "capacityTonsPerDay" to machine.capacityTonsPerDay,
                        "lastServicedDate" to machine.lastServicedDate,
                        "nextServiceDate" to machine.nextServiceDate,
                        "status" to machine.status,
                        "breakdownNote" to machine.breakdownNote,
                        "memberUids" to listOf(factoryId, callerUid).distinct(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                ).awaitLocalFirst(OperationType.CREATE, docRef.path)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Suppliers ---
    fun observeSuppliers(factoryId: String, callerUid: String, canReadAll: Boolean = true): Flow<List<SupplierItem>> {
        val path = "factories/$factoryId/suppliers"
        val query = if (canReadAll || callerUid == factoryId) {
            factoryDoc(factoryId).collection("suppliers")
        } else {
            factoryDoc(factoryId).collection("suppliers").whereArrayContains("memberUids", callerUid)
        }
        return query.snapshots()
            .map { snap -> snap.toObjects(SupplierItem::class.java, ServerTimestampBehavior.ESTIMATE) }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun saveSupplier(factoryId: String, callerUid: String, supplier: SupplierItem) {
        val docId = supplier.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = factoryDoc(factoryId).collection("suppliers").document(docId)
        try {
            val existing = if (supplier.id.isNotBlank()) getLocalFirstOrNull(docRef) else null
            if (existing != null && existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "name" to supplier.name,
                    "phone" to PakistaniFormatters.formatPhone(supplier.phone),
                    "itemSupplied" to supplier.itemSupplied,
                    "totalPurchases" to supplier.totalPurchases,
                    "dues" to supplier.dues,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (supplier.lastPaymentDate != null) updates["lastPaymentDate"] = supplier.lastPaymentDate
                docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
            } else {
                docRef.set(
                    mapOf(
                        "ownerId" to factoryId,
                        "name" to supplier.name,
                        "phone" to PakistaniFormatters.formatPhone(supplier.phone),
                        "itemSupplied" to supplier.itemSupplied,
                        "totalPurchases" to supplier.totalPurchases,
                        "dues" to supplier.dues,
                        "lastPaymentDate" to supplier.lastPaymentDate,
                        "memberUids" to listOf(factoryId, callerUid).distinct(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                ).awaitLocalFirst(OperationType.CREATE, docRef.path)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun paySupplierAndCreateExpense(
        factoryId: String,
        callerUid: String,
        supplier: SupplierItem,
        paymentAmount: Double
    ) {
        val supRef = factoryDoc(factoryId).collection("suppliers").document(supplier.id)
        val expId = UUID.randomUUID().toString()
        val expRef = factoryDoc(factoryId).collection("expenses").document(expId)
        val today = PakistaniFormatters.todayIso()
        val newDues = (supplier.dues - paymentAmount).coerceAtLeast(0.0)
        val members = listOf(factoryId, callerUid).distinct()

        val batch = db.batch()
        batch.update(
            supRef,
            mapOf(
                "dues" to newDues,
                "lastPaymentDate" to today,
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        batch.set(
            expRef,
            mapOf(
                "ownerId" to factoryId,
                "description" to "Supplier payment to ${supplier.name} (${supplier.itemSupplied})",
                "amount" to paymentAmount,
                "category" to "Supplier",
                "capitalInvestment" to false,
                "date" to today,
                "createdBy" to callerUid,
                "source" to "manual",
                "memberUids" to members,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        try {
            batch.commit().awaitLocalFirst(OperationType.UPDATE, supRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, supRef.path)
            throw e
        }
    }

    // --- Safety Incidents ---
    fun observeIncidents(factoryId: String, callerUid: String, canReadAll: Boolean = true): Flow<List<SafetyIncident>> {
        val path = "factories/$factoryId/incidents"
        val query = if (canReadAll || callerUid == factoryId) {
            factoryDoc(factoryId).collection("incidents")
        } else {
            factoryDoc(factoryId).collection("incidents").whereArrayContains("memberUids", callerUid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(SafetyIncident::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun saveIncident(factoryId: String, callerUid: String, incident: SafetyIncident) {
        val docId = incident.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = factoryDoc(factoryId).collection("incidents").document(docId)
        try {
            val existing = if (incident.id.isNotBlank()) getLocalFirstOrNull(docRef) else null
            if (existing != null && existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "title" to incident.title,
                    "description" to incident.description,
                    "severity" to incident.severity,
                    "status" to incident.status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (incident.followUp != null) updates["followUp"] = incident.followUp
                docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
            } else {
                docRef.set(
                    mapOf(
                        "ownerId" to factoryId,
                        "title" to incident.title,
                        "description" to incident.description,
                        "severity" to incident.severity,
                        "status" to incident.status,
                        "followUp" to incident.followUp,
                        "reportedBy" to incident.reportedBy,
                        "date" to incident.date.ifBlank { PakistaniFormatters.todayIso() },
                        "memberUids" to listOf(factoryId, callerUid).distinct(),
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    ).filterValues { it != null }
                ).awaitLocalFirst(OperationType.CREATE, docRef.path)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Generic Delete & Entity Updates ---
    suspend fun deleteDocument(factoryId: String, collection: String, docId: String) {
        require(docId.isNotBlank())
        val docRef = factoryDoc(factoryId).collection(collection).document(docId)
        try {
            docRef.delete().awaitLocalFirst(OperationType.DELETE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            throw e
        }
    }

    suspend fun updateProduction(factoryId: String, record: ProductionRecord) {
        require(record.id.isNotBlank())
        val docRef = factoryDoc(factoryId).collection("production").document(record.id)
        try {
            val updates = mutableMapOf<String, Any>(
                "date" to record.date,
                "tons" to record.tons.coerceAtLeast(0.0),
                "productType" to record.productType,
                "machineId" to record.machineId,
                "machineName" to record.machineName,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (record.notes != null) updates["notes"] = record.notes
            docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateInventoryMovement(factoryId: String, movement: InventoryMovement) {
        require(movement.id.isNotBlank())
        val docRef = factoryDoc(factoryId).collection("inventory").document(movement.id)
        try {
            val updates = mutableMapOf<String, Any>(
                "product" to movement.product,
                "tons" to movement.tons.coerceAtLeast(0.0),
                "location" to movement.location,
                "movementType" to movement.movementType,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (movement.notes != null) updates["notes"] = movement.notes
            docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateCustomer(factoryId: String, customer: Customer) {
        require(customer.id.isNotBlank())
        val docRef = factoryDoc(factoryId).collection("customers").document(customer.id)
        try {
            val updates = mutableMapOf<String, Any>(
                "name" to PakistaniFormatters.formatTitleCase(customer.name),
                "phone" to customer.phone,
                "balance" to customer.balance.coerceAtLeast(0.0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (customer.address != null) updates["address"] = customer.address
            docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateSale(factoryId: String, sale: Sale) {
        require(sale.id.isNotBlank())
        val docRef = factoryDoc(factoryId).collection("sales").document(sale.id)
        try {
            docRef.update(
                mapOf(
                    "customerId" to sale.customerId,
                    "customerName" to sale.customerName,
                    "product" to sale.product,
                    "quantity" to sale.quantity.coerceAtLeast(1L),
                    "unitPrice" to sale.unitPrice.coerceAtLeast(0.0),
                    "total" to sale.total.coerceAtLeast(0.0),
                    "paid" to sale.paid,
                    "paidAmount" to (if (sale.paid) sale.total else sale.paidAmount).coerceAtLeast(0.0),
                    "date" to sale.date,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitLocalFirst(OperationType.UPDATE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateExpense(factoryId: String, expense: Expense) {
        require(expense.id.isNotBlank())
        val docRef = factoryDoc(factoryId).collection("expenses").document(expense.id)
        val isCapital = expense.category.equals("Factory Build", ignoreCase = true)
        val normalizedCat = if (isCapital) "Factory Build" else expense.category
        try {
            docRef.update(
                mapOf(
                    "description" to expense.description,
                    "amount" to expense.amount.coerceAtLeast(0.0),
                    "category" to normalizedCat,
                    "capitalInvestment" to isCapital,
                    "date" to expense.date,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitLocalFirst(OperationType.UPDATE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateFunding(factoryId: String, entry: FundingEntry) {
        require(entry.id.isNotBlank())
        val docRef = factoryDoc(factoryId).collection("funding").document(entry.id)
        try {
            val updates = mutableMapOf<String, Any>(
                "source" to entry.source,
                "name" to entry.name,
                "type" to entry.type,
                "amount" to entry.amount.coerceAtLeast(0.0),
                "repaid" to entry.repaid.coerceIn(0.0, entry.amount.coerceAtLeast(0.0)),
                "interest" to entry.interest.coerceAtLeast(0.0),
                "date" to entry.date,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (entry.dueDate != null) updates["dueDate"] = entry.dueDate
            if (entry.notes != null) updates["notes"] = entry.notes
            docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updatePayroll(factoryId: String, record: PayrollRecord) {
        require(record.id.isNotBlank())
        val docRef = factoryDoc(factoryId).collection("payroll").document(record.id)
        val netPay = (record.baseSalary - record.advances - record.deductions).coerceAtLeast(0.0)
        try {
            val updates = mutableMapOf<String, Any>(
                "baseSalary" to record.baseSalary.coerceAtLeast(0.0),
                "advances" to record.advances.coerceAtLeast(0.0),
                "deductions" to record.deductions.coerceAtLeast(0.0),
                "netPay" to netPay,
                "paid" to record.paid,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (record.authUid.isNotBlank()) updates["authUid"] = record.authUid
            if (record.paidDate != null) updates["paidDate"] = record.paidDate
            docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun updateUtility(factoryId: String, reading: UtilityReading) {
        require(reading.id.isNotBlank())
        val docRef = factoryDoc(factoryId).collection("utilities").document(reading.id)
        try {
            val updates = mutableMapOf<String, Any>(
                "meterReading" to reading.meterReading.coerceAtLeast(0.0),
                "unitsOrLiters" to reading.unitsOrLiters.coerceAtLeast(0.0),
                "amount" to reading.amount.coerceAtLeast(0.0),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (reading.notes != null) updates["notes"] = reading.notes
            docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
    }

    suspend fun clearDemoData(factoryId: String): Int {
        val demoDocRefs = listOf(
            "employees" to listOf("emp_1", "emp_2", "emp_3", "emp_4", "emp_5"),
            "attendance" to listOf("att_demo_0", "att_demo_1", "att_demo_2", "att_demo_3", "att_demo_4"),
            "production" to listOf("prod_demo_1"),
            "inventory" to listOf("inv_demo_blocks", "inv_demo_cubes"),
            "customers" to listOf("cust_demo_1", "cust_demo_2"),
            "sales" to (0..6).map { "sale_demo_$it" },
            "expenses" to listOf("exp_demo_elec", "exp_demo_diesel", "exp_demo_build"),
            "funding" to listOf("fund_demo_1"),
            "payroll" to listOf("pay_demo_1", "pay_demo_2", "pay_demo_3"),
            "machines" to listOf("mach_1"),
            "deliveries" to listOf("del_demo_1"),
            "suppliers" to listOf("sup_demo_1"),
            "incidents" to listOf("inc_demo_1"),
            "utilities" to listOf("util_demo_1"),
            "notices" to listOf("notice_demo_1")
        )
        var count = 0
        try {
            val batch = db.batch()
            demoDocRefs.forEach { (col, ids) ->
                ids.forEach { id ->
                    batch.delete(factoryDoc(factoryId).collection(col).document(id))
                    count++
                }
            }
            val cfgRef = factoryDoc(factoryId).collection("settings").document("config")
            val cfgSnap = getLocalFirstOrNull(cfgRef)
            if (cfgSnap != null && cfgSnap.exists()) {
                batch.update(
                    cfgRef,
                    mapOf(
                        "seededDemo" to false,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
            batch.commit().awaitLocalFirst(OperationType.DELETE, "factories/$factoryId/demo")
            return count
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, "factories/$factoryId/demo")
            throw e
        }
    }

    // --- Initial Demo Data Seeding (clearly labeled [Demo], idempotent) ---
    suspend fun ensureDemoSeeded(factoryId: String, ownerName: String) {
        val configRef = factoryDoc(factoryId).collection("settings").document("config")
        try {
            val configSnap = getLocalFirstOrNull(configRef)
            if (configSnap != null && configSnap.exists()) return

            val today = PakistaniFormatters.todayIso()
            val month = PakistaniFormatters.currentMonthIso()
            val members = listOf(factoryId)
            val batch = db.batch()

            // Settings
            batch.set(
                configRef,
                mapOf(
                    "ownerId" to factoryId,
                    "companyName" to "Brohi Ice Factory",
                    "phone" to "0333-2862498",
                    "address" to "SITE Industrial Area, Karachi",
                    "iceBlockRate" to 1200.0,
                    "iceCubeRate" to 850.0,
                    "operationsStartDate" to "2026-08-01",
                    "notificationsEnabled" to true,
                    "seededDemo" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Demo Staff
            val empIds = listOf("emp_1", "emp_2", "emp_3", "emp_4", "emp_5")
            val demoEmps = listOf(
                listOf("Ahmed Khan [Demo]", "Manager", 75000.0, 25000.0, "2021-01-01"),
                listOf("Sara Ali [Demo]", "Admin", 55000.0, 30000.0, "2022-06-01"),
                listOf("Usman Tariq [Demo]", "Staff", 38000.0, 22000.0, "2023-03-01"),
                listOf("Zainab Fatima [Demo]", "Staff", 35000.0, 25000.0, "2024-01-15"),
                listOf("Rizwan Ahmed [Demo]", "Staff", 36000.0, 28000.0, "2024-09-01")
            )
            demoEmps.forEachIndexed { idx, row ->
                val eRef = factoryDoc(factoryId).collection("employees").document(empIds[idx])
                batch.set(
                    eRef,
                    mapOf(
                        "ownerId" to factoryId,
                        "authUid" to factoryId,
                        "name" to row[0] as String,
                        "phone" to "0300-123456$idx",
                        "cnic" to "42201-123456$idx-1",
                        "role" to row[1] as String,
                        "salary" to row[2] as Double,
                        "initialSalary" to row[3] as Double,
                        "shift" to "Morning (8AM - 4PM)",
                        "joinDate" to row[4] as String,
                        "emergencyContact" to "0300-987654$idx",
                        "skipLiveness" to (idx == 4),
                        "active" to true,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // Demo Attendance
            val demoAtt = listOf(
                listOf("emp_1", "Ahmed Khan [Demo]", "Present", "08:15 AM", "05:50 PM", 9.5, false),
                listOf("emp_2", "Sara Ali [Demo]", "Present", "08:20 AM", "05:45 PM", 9.4, false),
                listOf("emp_3", "Usman Tariq [Demo]", "Present", "08:25 AM", "", 0.0, false),
                listOf("emp_4", "Zainab Fatima [Demo]", "Absent", "", "", 0.0, false),
                listOf("emp_5", "Rizwan Ahmed [Demo]", "Present", "08:18 AM", "05:52 PM", 9.5, true)
            )
            demoAtt.forEachIndexed { idx, row ->
                val aRef = factoryDoc(factoryId).collection("attendance").document("att_demo_$idx")
                batch.set(
                    aRef,
                    mapOf(
                        "ownerId" to factoryId,
                        "employeeId" to row[0] as String,
                        "employeeName" to row[1] as String,
                        "authUid" to factoryId,
                        "date" to today,
                        "checkIn" to row[3] as String,
                        "checkOut" to row[4] as String,
                        "status" to row[2] as String,
                        "bypassed" to row[6] as Boolean,
                        "hours" to row[5] as Double,
                        "memberUids" to members,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // Demo Production & Stock
            batch.set(
                factoryDoc(factoryId).collection("production").document("prod_demo_1"),
                mapOf(
                    "ownerId" to factoryId,
                    "date" to today,
                    "tons" to 12.5,
                    "productType" to "Ice Blocks (10kg)",
                    "machineId" to "mach_1",
                    "machineName" to "Plant #1 [Demo]",
                    "notes" to "Morning shift production",
                    "createdBy" to ownerName,
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                factoryDoc(factoryId).collection("inventory").document("inv_demo_blocks"),
                mapOf(
                    "ownerId" to factoryId,
                    "product" to "Ice Blocks",
                    "tons" to 5.1,
                    "location" to "Cold Room A",
                    "movementType" to "IN",
                    "notes" to "Initial demo stock",
                    "createdBy" to ownerName,
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                factoryDoc(factoryId).collection("inventory").document("inv_demo_cubes"),
                mapOf(
                    "ownerId" to factoryId,
                    "product" to "Ice Cubes",
                    "tons" to 3.4,
                    "location" to "Cold Room B",
                    "movementType" to "IN",
                    "notes" to "Initial demo stock",
                    "createdBy" to ownerName,
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Demo Customers
            batch.set(
                factoryDoc(factoryId).collection("customers").document("cust_demo_1"),
                mapOf(
                    "ownerId" to factoryId,
                    "name" to "Al-Madina Cold Drinks [Demo]",
                    "phone" to "0321-4567890",
                    "address" to "Saddar Market",
                    "balance" to 25000.0,
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                factoryDoc(factoryId).collection("customers").document("cust_demo_2"),
                mapOf(
                    "ownerId" to factoryId,
                    "name" to "Karachi Fish Harbor [Demo]",
                    "phone" to "0301-8899776",
                    "address" to "West Wharf",
                    "balance" to 0.0,
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Demo Sales (last 7 days for chart + today)
            val dailySales = listOf(42000.0, 58000.0, 64000.0, 81000.0, 74000.0, 95000.0, 125000.0)
            dailySales.forEachIndexed { idx, amt ->
                val dayIso = PakistaniFormatters.daysAgoIso(6 - idx)
                batch.set(
                    factoryDoc(factoryId).collection("sales").document("sale_demo_$idx"),
                    mapOf(
                        "ownerId" to factoryId,
                        "invoiceNumber" to "BIF-100$idx",
                        "customerId" to "cust_demo_2",
                        "customerName" to "Karachi Fish Harbor [Demo]",
                        "product" to "Ice Blocks (10kg)",
                        "quantity" to (amt / 1200.0).toLong().coerceAtLeast(1L),
                        "unitPrice" to 1200.0,
                        "total" to amt,
                        "paid" to true,
                        "paidAmount" to amt,
                        "date" to dayIso,
                        "createdBy" to ownerName,
                        "source" to "manual",
                        "memberUids" to members,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }

            // Demo Expenses (Operating + Factory Build)
            batch.set(
                factoryDoc(factoryId).collection("expenses").document("exp_demo_elec"),
                mapOf(
                    "ownerId" to factoryId,
                    "description" to "K-Electric Industrial Bill [Demo]",
                    "amount" to 52000.0,
                    "category" to "Electricity",
                    "capitalInvestment" to false,
                    "date" to today,
                    "createdBy" to ownerName,
                    "source" to "manual",
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                factoryDoc(factoryId).collection("expenses").document("exp_demo_diesel"),
                mapOf(
                    "ownerId" to factoryId,
                    "description" to "Backup Generator Diesel [Demo]",
                    "amount" to 30500.0,
                    "category" to "Diesel",
                    "capitalInvestment" to false,
                    "date" to today,
                    "createdBy" to ownerName,
                    "source" to "manual",
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                factoryDoc(factoryId).collection("expenses").document("exp_demo_build"),
                mapOf(
                    "ownerId" to factoryId,
                    "description" to "Factory Setup & Compressor Plant [Demo]",
                    "amount" to 1800000.0,
                    "category" to "Factory Build",
                    "capitalInvestment" to true,
                    "date" to "2026-08-01",
                    "createdBy" to ownerName,
                    "source" to "manual",
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Demo Funding
            batch.set(
                factoryDoc(factoryId).collection("funding").document("fund_demo_1"),
                mapOf(
                    "ownerId" to factoryId,
                    "source" to "Friend",
                    "name" to "Tariq Mehmood [Demo]",
                    "type" to "Loan",
                    "amount" to 1000000.0,
                    "repaid" to 250000.0,
                    "interest" to 0.0,
                    "dueDate" to "2027-06-01",
                    "date" to "2026-08-01",
                    "notes" to "Initial setup support",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Demo Payroll
            batch.set(
                factoryDoc(factoryId).collection("payroll").document("pay_demo_1"),
                mapOf(
                    "ownerId" to factoryId,
                    "employeeId" to "emp_1",
                    "employeeName" to "Ahmed Khan [Demo]",
                    "month" to month,
                    "baseSalary" to 75000.0,
                    "advances" to 5000.0,
                    "deductions" to 0.0,
                    "netPay" to 70000.0,
                    "paid" to false,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                factoryDoc(factoryId).collection("payroll").document("pay_demo_2"),
                mapOf(
                    "ownerId" to factoryId,
                    "employeeId" to "emp_1",
                    "employeeName" to "Ahmed Khan [Demo]",
                    "month" to "2026-10",
                    "baseSalary" to 75000.0,
                    "advances" to 10000.0,
                    "deductions" to 0.0,
                    "netPay" to 65000.0,
                    "paid" to true,
                    "paidDate" to "2026-11-01",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                factoryDoc(factoryId).collection("payroll").document("pay_demo_3"),
                mapOf(
                    "ownerId" to factoryId,
                    "employeeId" to "emp_2",
                    "employeeName" to "Sara Ali [Demo]",
                    "month" to month,
                    "baseSalary" to 55000.0,
                    "advances" to 0.0,
                    "deductions" to 0.0,
                    "netPay" to 55000.0,
                    "paid" to true,
                    "paidDate" to today,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            // Demo Machine, Delivery, Supplier, Incident, Utility
            batch.set(
                factoryDoc(factoryId).collection("machines").document("mach_1"),
                mapOf(
                    "ownerId" to factoryId,
                    "name" to "Compressor Plant #1 [Demo]",
                    "model" to "Mycom N6WB",
                    "capacityTonsPerDay" to 20.0,
                    "lastServicedDate" to "2026-10-15",
                    "nextServiceDate" to "2026-12-15",
                    "status" to "Operational",
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                factoryDoc(factoryId).collection("deliveries").document("del_demo_1"),
                mapOf(
                    "ownerId" to factoryId,
                    "vehicleNumber" to "KHI-4821",
                    "driverName" to "Sajid Ali [Demo]",
                    "route" to "SITE to Fish Harbor",
                    "customerName" to "Karachi Fish Harbor [Demo]",
                    "tons" to 4.5,
                    "status" to "On route",
                    "date" to today,
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                factoryDoc(factoryId).collection("suppliers").document("sup_demo_1"),
                mapOf(
                    "ownerId" to factoryId,
                    "name" to "Sindh Salt & Ammonia Co. [Demo]",
                    "phone" to "0312-2233445",
                    "itemSupplied" to "Industrial Brine Salt",
                    "totalPurchases" to 145000.0,
                    "dues" to 22000.0,
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                factoryDoc(factoryId).collection("incidents").document("inc_demo_1"),
                mapOf(
                    "ownerId" to factoryId,
                    "title" to "Cold Room Floor Wet [Demo]",
                    "description" to "Minor water leakage near Cold Room B door seal; anti-slip mat placed.",
                    "severity" to "Low",
                    "status" to "Open",
                    "followUp" to "Replace rubber gasket during weekend check",
                    "reportedBy" to "Usman Tariq [Demo]",
                    "date" to today,
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                factoryDoc(factoryId).collection("utilities").document("util_demo_1"),
                mapOf(
                    "ownerId" to factoryId,
                    "type" to "Electricity",
                    "meterReading" to 48920.0,
                    "unitsOrLiters" to 1240.0,
                    "amount" to 52000.0,
                    "date" to today,
                    "notes" to "Monthly industrial meter reading",
                    "memberUids" to members,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                factoryDoc(factoryId).collection("notices").document("notice_demo_1"),
                mapOf(
                    "ownerId" to factoryId,
                    "title" to "Scheduled Maintenance & Shift Alert [Demo]",
                    "titleUrdu" to "شیڈولڈ مینٹیننس اور شفٹ کا اعلان",
                    "message" to "Morning shift starts 8:00 AM sharp. Compressor servicing at 2:00 PM — all staff please wear safety boots.",
                    "messageUrdu" to "صبح کی شفٹ ٹھیک 8 بجے شروع ہوگی۔ دوپہر 2 بجے کمپریسر سروس ہوگی، تمام عملہ حفاظتی جوتے پہنے۔",
                    "noticeType" to "General",
                    "date" to today,
                    "active" to true,
                    "createdByName" to PakistaniFormatters.formatTitleCase(ownerName.ifBlank { "Super Admin" }),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit().awaitLocalFirst(OperationType.WRITE, configRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, configRef.path)
        }
    }

    // --- Registration Requests & Approvals ---
    fun observeRegistrationRequests(factoryId: String): Flow<List<RegistrationRequest>> {
        val path = "factories/$factoryId/registration_requests"
        return factoryDoc(factoryId).collection("registration_requests")
            .snapshots()
            .map { snap ->
                snap.toObjects(RegistrationRequest::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    fun observeMyRegistrationRequest(factoryId: String, applicantUid: String): Flow<RegistrationRequest?> {
        val path = "factories/$factoryId/registration_requests/$applicantUid"
        return factoryDoc(factoryId).collection("registration_requests").document(applicantUid)
            .snapshots()
            .map { snap ->
                snap.takeIf { it.exists() }?.toObject(RegistrationRequest::class.java, ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                handleFirestoreError(fe ?: error, OperationType.GET, path)
                emit(null)
            }
    }

    suspend fun getMyRegistrationRequestServerOrLocal(factoryId: String, applicantUid: String): RegistrationRequest? {
        val docRef = factoryDoc(factoryId).collection("registration_requests").document(applicantUid)
        val snap = runCatching {
            withTimeoutOrNull(2500L) { docRef.get(Source.SERVER).await() }
        }.getOrNull() ?: getLocalFirstOrNull(docRef)
        return snap?.takeIf { it.exists() }?.toObject(RegistrationRequest::class.java, ServerTimestampBehavior.ESTIMATE)
    }

    suspend fun submitRegistrationRequest(
        factoryId: String,
        applicantUid: String,
        request: RegistrationRequest
    ): String {
        val docId = request.id.ifBlank { applicantUid }
        val docRef = factoryDoc(factoryId).collection("registration_requests").document(docId)
        val cleanName = PakistaniFormatters.formatTitleCase(request.name)
        val cleanFather = PakistaniFormatters.formatTitleCase(request.fatherName)
        val cleanCnic = PakistaniFormatters.formatCnic(request.cnic)
        val cleanPhone = PakistaniFormatters.formatPhone(request.phone)
        val cleanWhatsapp = PakistaniFormatters.formatPhone(request.whatsappNumber)
        val cleanEmergency = PakistaniFormatters.formatPhone(request.emergencyContact)
        try {
            val existing = getLocalFirstOrNull(docRef)
            if (existing != null && existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "name" to cleanName,
                    "cnic" to cleanCnic,
                    "phone" to cleanPhone,
                    "emergencyContact" to cleanEmergency,
                    "requestedRole" to request.requestedRole.ifBlank { "Staff" },
                    "shift" to request.shift.ifBlank { "Morning (8AM - 4PM)" },
                    "status" to "PENDING",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (cleanFather.isNotBlank()) updates["fatherName"] = cleanFather
                if (cleanWhatsapp.isNotBlank()) updates["whatsappNumber"] = cleanWhatsapp
                if (request.email.isNotBlank()) updates["email"] = request.email
                if (!request.reviewNote.isNullOrBlank()) updates["reviewNote"] = request.reviewNote
                if (!request.faceTemplateEncrypted.isNullOrBlank()) {
                    updates["faceTemplateEncrypted"] = request.faceTemplateEncrypted
                }
                docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
            } else {
                val payload = mapOf(
                    "ownerId" to factoryId,
                    "applicantUid" to applicantUid,
                    "name" to cleanName,
                    "fatherName" to cleanFather.ifBlank { null },
                    "cnic" to cleanCnic,
                    "phone" to cleanPhone,
                    "whatsappNumber" to cleanWhatsapp.ifBlank { null },
                    "email" to request.email.ifBlank { null },
                    "emergencyContact" to cleanEmergency,
                    "requestedRole" to request.requestedRole.ifBlank { "Staff" },
                    "shift" to request.shift.ifBlank { "Morning (8AM - 4PM)" },
                    "status" to "PENDING",
                    "startingSalary" to request.startingSalary.coerceAtLeast(0.0),
                    "reviewNote" to request.reviewNote?.ifBlank { null },
                    "faceTemplateEncrypted" to request.faceTemplateEncrypted?.ifBlank { null },
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).awaitLocalFirst(OperationType.CREATE, docRef.path)
            }
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun autoBlockForMissingFaceEnrollment(
        factoryId: String,
        uid: String,
        displayName: String,
        fatherName: String = "",
        cnic: String = "",
        phone: String = "",
        whatsappNumber: String = "",
        email: String = "",
        emergencyContact: String = "",
        requestedRole: String = "Staff",
        shift: String = "Morning (8AM - 4PM)"
    ) {
        val note = "Auto-blocked: Face enrollment not completed within 2 days. Needs Super Admin / Admin approval."
        val reqRef = factoryDoc(factoryId).collection("registration_requests").document(uid)
        val userRef = db.collection("users").document(uid)
        val batch = db.batch()
        val userSnap = getLocalFirstOrNull(userRef)
        if (userSnap != null && userSnap.exists()) {
            batch.update(
                userRef,
                mapOf(
                    "approvalStatus" to "PENDING",
                    "faceEnrolled" to false,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        }
        val reqSnap = getLocalFirstOrNull(reqRef)
        if (reqSnap != null && reqSnap.exists()) {
            batch.update(
                reqRef,
                mapOf(
                    "status" to "PENDING",
                    "reviewNote" to note,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
        } else {
            val payload = mapOf(
                "ownerId" to factoryId,
                "applicantUid" to uid,
                "name" to PakistaniFormatters.formatTitleCase(displayName.ifBlank { "Employee" }),
                "fatherName" to fatherName.ifBlank { null },
                "cnic" to PakistaniFormatters.formatCnic(cnic),
                "phone" to PakistaniFormatters.formatPhone(phone),
                "whatsappNumber" to whatsappNumber.ifBlank { null },
                "email" to email.ifBlank { null },
                "emergencyContact" to PakistaniFormatters.formatPhone(emergencyContact),
                "requestedRole" to requestedRole.ifBlank { "Staff" },
                "shift" to shift.ifBlank { "Morning (8AM - 4PM)" },
                "status" to "PENDING",
                "startingSalary" to 35000.0,
                "reviewNote" to note,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }
            batch.set(reqRef, payload)
        }
        try {
            batch.commit().awaitLocalFirst(OperationType.WRITE, reqRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, reqRef.path)
            throw e
        }
    }

    suspend fun approveRegistrationRequest(
        factoryId: String,
        request: RegistrationRequest,
        roleOverride: String,
        shiftOverride: String,
        startingSalary: Double,
        reviewNote: String? = null
    ): String {
        require(request.id.isNotBlank())
        val reqRef = factoryDoc(factoryId).collection("registration_requests").document(request.id)
        val empId = UUID.randomUUID().toString()
        val empRef = factoryDoc(factoryId).collection("employees").document(empId)
        val cleanName = PakistaniFormatters.formatTitleCase(request.name)
        val cleanFather = PakistaniFormatters.formatTitleCase(request.fatherName)
        val cleanCnic = PakistaniFormatters.formatCnic(request.cnic)
        val cleanPhone = PakistaniFormatters.formatPhone(request.phone)
        val cleanWhatsapp = PakistaniFormatters.formatPhone(request.whatsappNumber)
        val cleanEmergency = PakistaniFormatters.formatPhone(request.emergencyContact)
        val resolvedSalary = startingSalary.coerceAtLeast(0.0)
        val applicantUid = request.applicantUid.ifBlank { request.id }
        val batch = db.batch()
        batch.update(
            reqRef,
            mapOf(
                "name" to cleanName,
                "fatherName" to cleanFather,
                "requestedRole" to roleOverride,
                "shift" to shiftOverride,
                "status" to "APPROVED",
                "startingSalary" to resolvedSalary,
                "reviewNote" to (reviewNote?.trim()?.ifBlank { "Approved by Super Admin" } ?: "Approved by Super Admin"),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        val empPayload = mapOf(
            "ownerId" to factoryId,
            "authUid" to applicantUid,
            "name" to cleanName,
            "fatherName" to cleanFather,
            "phone" to cleanPhone,
            "whatsappNumber" to cleanWhatsapp,
            "email" to request.email,
            "cnic" to cleanCnic,
            "role" to roleOverride,
            "salary" to resolvedSalary,
            "initialSalary" to resolvedSalary,
            "shift" to shiftOverride,
            "joinDate" to PakistaniFormatters.todayIso(),
            "emergencyContact" to cleanEmergency,
            "skipLiveness" to false,
            "faceTemplateEncrypted" to request.faceTemplateEncrypted?.ifBlank { null },
            "active" to true,
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        ).filterValues { it != null }
        batch.set(empRef, empPayload)
        if (applicantUid.isNotBlank()) {
            val userRef = db.collection("users").document(applicantUid)
            val userSnap = getLocalFirstOrNull(userRef)
            if (userSnap != null && userSnap.exists()) {
                batch.update(
                    userRef,
                    mapOf(
                        "approvalStatus" to "APPROVED",
                        "requestedRole" to roleOverride,
                        "linkedFactoryId" to factoryId,
                        "faceEnrolled" to !request.faceTemplateEncrypted.isNullOrBlank(),
                        "approvedAtMillis" to System.currentTimeMillis(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }
        try {
            batch.commit().awaitLocalFirst(OperationType.WRITE, reqRef.path)
            return empId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, reqRef.path)
            throw e
        }
    }

    suspend fun rejectRegistrationRequest(
        factoryId: String,
        request: RegistrationRequest,
        reviewNote: String? = null
    ) {
        require(request.id.isNotBlank())
        val reqRef = factoryDoc(factoryId).collection("registration_requests").document(request.id)
        val applicantUid = request.applicantUid.ifBlank { request.id }
        val batch = db.batch()
        batch.update(
            reqRef,
            mapOf(
                "status" to "REJECTED",
                "reviewNote" to (reviewNote?.trim()?.ifBlank { "Declined by Super Admin" } ?: "Declined by Super Admin"),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (applicantUid.isNotBlank()) {
            val userRef = db.collection("users").document(applicantUid)
            val userSnap = getLocalFirstOrNull(userRef)
            if (userSnap != null && userSnap.exists()) {
                batch.update(
                    userRef,
                    mapOf(
                        "approvalStatus" to "REJECTED",
                        "linkedFactoryId" to factoryId,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                )
            }
        }
        try {
            batch.commit().awaitLocalFirst(OperationType.UPDATE, reqRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            throw e
        }
    }

    // --- Profile Update Requests & Approvals ---
    fun observeProfileUpdateRequests(
        factoryId: String,
        callerUid: String,
        canManageAll: Boolean
    ): Flow<List<ProfileUpdateRequest>> {
        val path = "factories/$factoryId/profile_update_requests"
        val query = if (canManageAll || callerUid == factoryId) {
            factoryDoc(factoryId).collection("profile_update_requests")
        } else {
            factoryDoc(factoryId).collection("profile_update_requests").whereEqualTo("applicantUid", callerUid)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(ProfileUpdateRequest::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
            }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun submitProfileUpdateRequest(
        factoryId: String,
        applicantUid: String,
        request: ProfileUpdateRequest
    ): String {
        val docId = request.id.ifBlank { applicantUid }
        val docRef = factoryDoc(factoryId).collection("profile_update_requests").document(docId)
        val cleanName = PakistaniFormatters.formatTitleCase(request.name.ifBlank { "Employee" })
        val cleanFather = PakistaniFormatters.formatTitleCase(request.fatherName)
        val cleanCnic = PakistaniFormatters.formatCnic(request.cnic)
        val cleanPhone = PakistaniFormatters.formatPhone(request.phone)
        val cleanWhatsapp = PakistaniFormatters.formatPhone(request.whatsappNumber)
        val cleanEmergency = PakistaniFormatters.formatPhone(request.emergencyContact)
        val cleanShift = request.shift.ifBlank { WORK_SHIFT_OPTIONS.first() }
        val cleanJoinDate = request.joinDate.trim().ifBlank { PakistaniFormatters.todayIso() }
        try {
            val existing = getLocalFirstOrNull(docRef)
            if (existing != null && existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "name" to cleanName,
                    "fatherName" to cleanFather,
                    "cnic" to cleanCnic,
                    "phone" to cleanPhone,
                    "whatsappNumber" to cleanWhatsapp,
                    "email" to request.email.trim(),
                    "emergencyContact" to cleanEmergency,
                    "shift" to cleanShift,
                    "joinDate" to cleanJoinDate,
                    "status" to "PENDING",
                    "reviewNote" to "",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (request.employeeId.isNotBlank()) updates["employeeId"] = request.employeeId
                if (request.photoUrl != null) updates["photoUrl"] = request.photoUrl
                docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
            } else {
                val payload = mapOf(
                    "ownerId" to factoryId,
                    "applicantUid" to applicantUid,
                    "employeeId" to request.employeeId.ifBlank { null },
                    "name" to cleanName,
                    "fatherName" to cleanFather.ifBlank { null },
                    "cnic" to cleanCnic,
                    "phone" to cleanPhone,
                    "whatsappNumber" to cleanWhatsapp.ifBlank { null },
                    "email" to request.email.trim().ifBlank { null },
                    "emergencyContact" to cleanEmergency,
                    "shift" to cleanShift,
                    "joinDate" to cleanJoinDate,
                    "photoUrl" to request.photoUrl?.takeIf { it.isNotBlank() },
                    "status" to "PENDING",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).awaitLocalFirst(OperationType.CREATE, docRef.path)
            }
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun approveProfileUpdateRequest(
        factoryId: String,
        request: ProfileUpdateRequest,
        reviewNote: String? = null
    ) {
        require(request.id.isNotBlank())
        val reqRef = factoryDoc(factoryId).collection("profile_update_requests").document(request.id)
        val cleanName = PakistaniFormatters.formatTitleCase(request.name)
        val cleanFather = PakistaniFormatters.formatTitleCase(request.fatherName)
        val cleanCnic = PakistaniFormatters.formatCnic(request.cnic)
        val cleanPhone = PakistaniFormatters.formatPhone(request.phone)
        val cleanWhatsapp = PakistaniFormatters.formatPhone(request.whatsappNumber)
        val cleanEmergency = PakistaniFormatters.formatPhone(request.emergencyContact)
        val cleanShift = request.shift.ifBlank { WORK_SHIFT_OPTIONS.first() }
        val cleanJoinDate = request.joinDate.trim().ifBlank { PakistaniFormatters.todayIso() }
        val applicantUid = request.applicantUid.ifBlank { request.id }

        // Resolve linked Employee document (by employeeId or authUid query)
        val empCollection = factoryDoc(factoryId).collection("employees")
        val targetEmpDoc = if (request.employeeId.isNotBlank()) {
            getLocalFirstOrNull(empCollection.document(request.employeeId))
        } else {
            null
        } ?: runCatching {
            empCollection.whereEqualTo("authUid", applicantUid).get().await().documents.firstOrNull()
        }.getOrNull()

        val userRef = db.collection("users").document(applicantUid)
        val userSnap = getLocalFirstOrNull(userRef)

        val batch = db.batch()
        batch.update(
            reqRef,
            mapOf(
                "status" to "APPROVED",
                "reviewNote" to (reviewNote?.trim()?.ifBlank { "Approved by Admin" } ?: "Approved by Admin"),
                "updatedAt" to FieldValue.serverTimestamp()
            )
        )
        if (targetEmpDoc != null && targetEmpDoc.exists()) {
            val empUpdates = mutableMapOf<String, Any>(
                "name" to cleanName,
                "fatherName" to cleanFather,
                "cnic" to cleanCnic,
                "phone" to cleanPhone,
                "whatsappNumber" to cleanWhatsapp,
                "email" to request.email.trim(),
                "emergencyContact" to cleanEmergency,
                "shift" to cleanShift,
                "joinDate" to cleanJoinDate,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.update(targetEmpDoc.reference, empUpdates)
        } else {
            val newEmpRef = empCollection.document(request.employeeId.ifBlank { applicantUid })
            val userRole = userSnap?.getString("requestedRole") ?: "Staff"
            val empPayload = mapOf(
                "ownerId" to factoryId,
                "authUid" to applicantUid,
                "name" to cleanName,
                "fatherName" to cleanFather,
                "phone" to cleanPhone,
                "whatsappNumber" to cleanWhatsapp,
                "email" to request.email.trim(),
                "cnic" to cleanCnic,
                "role" to userRole,
                "salary" to 0.0,
                "initialSalary" to 0.0,
                "shift" to cleanShift,
                "joinDate" to cleanJoinDate,
                "emergencyContact" to cleanEmergency,
                "skipLiveness" to false,
                "active" to true,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            batch.set(newEmpRef, empPayload)
        }
        if (userSnap != null && userSnap.exists()) {
            val userUpdates = mutableMapOf<String, Any>(
                "displayName" to cleanName,
                "phone" to cleanPhone,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (request.email.isNotBlank()) userUpdates["email"] = request.email.trim()
            if (request.photoUrl != null) userUpdates["photoUrl"] = request.photoUrl
            batch.update(userRef, userUpdates)
        }
        try {
            batch.commit().awaitLocalFirst(OperationType.UPDATE, reqRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            throw e
        }
    }

    suspend fun rejectProfileUpdateRequest(
        factoryId: String,
        request: ProfileUpdateRequest,
        reviewNote: String? = null
    ) {
        require(request.id.isNotBlank())
        val reqRef = factoryDoc(factoryId).collection("profile_update_requests").document(request.id)
        try {
            reqRef.update(
                mapOf(
                    "status" to "REJECTED",
                    "reviewNote" to (reviewNote?.trim()?.ifBlank { "Declined by Admin" } ?: "Declined by Admin"),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).awaitLocalFirst(OperationType.UPDATE, reqRef.path)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, reqRef.path)
            throw e
        }
    }

    // --- Factory Notices & Holiday Broadcasts ---
    fun observeNotices(factoryId: String): Flow<List<FactoryNotice>> {
        val path = "factories/$factoryId/notices"
        return factoryDoc(factoryId).collection("notices")
            .snapshots()
            .map { snap ->
                snap.toObjects(FactoryNotice::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt?.seconds ?: 0L }
            }
            .withFirestoreErrorLog(OperationType.LIST, path)
    }

    suspend fun saveNotice(factoryId: String, notice: FactoryNotice): String {
        val docId = notice.id.ifBlank { UUID.randomUUID().toString() }
        val docRef = factoryDoc(factoryId).collection("notices").document(docId)
        try {
            val existing = if (notice.id.isNotBlank()) getLocalFirstOrNull(docRef) else null
            if (existing != null && existing.exists()) {
                val updates = mutableMapOf<String, Any>(
                    "title" to notice.title,
                    "titleUrdu" to notice.titleUrdu,
                    "message" to notice.message,
                    "messageUrdu" to notice.messageUrdu,
                    "noticeType" to notice.noticeType,
                    "date" to notice.date.ifBlank { PakistaniFormatters.todayIso() },
                    "active" to notice.active,
                    "createdByName" to PakistaniFormatters.formatTitleCase(notice.createdByName.ifBlank { "Admin" }),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
            } else {
                val payload = mapOf(
                    "ownerId" to factoryId,
                    "title" to notice.title,
                    "titleUrdu" to notice.titleUrdu.ifBlank { null },
                    "message" to notice.message,
                    "messageUrdu" to notice.messageUrdu.ifBlank { null },
                    "noticeType" to notice.noticeType,
                    "date" to notice.date.ifBlank { PakistaniFormatters.todayIso() },
                    "active" to notice.active,
                    "createdByName" to PakistaniFormatters.formatTitleCase(notice.createdByName.ifBlank { "Admin" }),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                docRef.set(payload).awaitLocalFirst(OperationType.CREATE, docRef.path)
            }
            return docId
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    // --- Live App Updates & Release Channel (/app_releases/latest) ---
    fun observeLatestAppRelease(): Flow<AppReleaseConfig?> {
        val path = "app_releases/latest"
        return db.collection("app_releases").document("latest")
            .snapshots()
            .map { snap ->
                snap.takeIf { it.exists() }?.toObject(AppReleaseConfig::class.java, ServerTimestampBehavior.ESTIMATE)
            }
            .catch { error ->
                val fe = generateSequence(error) { it.cause }
                    .filterIsInstance<FirebaseFirestoreException>().firstOrNull()
                if (fe == null && error is CancellationException) throw error
                handleFirestoreError(fe ?: error, OperationType.GET, path)
                emit(null)
            }
    }

    suspend fun fetchLatestAppReleaseFromServer(): AppReleaseConfig? {
        val docRef = db.collection("app_releases").document("latest")
        val snap = runCatching {
            withTimeoutOrNull(4500L) { docRef.get(Source.SERVER).await() }
        }.getOrNull() ?: getLocalFirstOrNull(docRef)
        return snap?.takeIf { it.exists() }?.toObject(AppReleaseConfig::class.java, ServerTimestampBehavior.ESTIMATE)
    }

    suspend fun publishAppRelease(release: AppReleaseConfig, publisherName: String) {
        val docRef = db.collection("app_releases").document("latest")
        val cleanUrl = release.apkDownloadUrl.trim()
        val cleanVersionName = release.latestVersionName.trim().ifBlank { "1.0" }
        val cleanNotes = release.releaseNotes.trim()
        val cleanNotesUrdu = release.releaseNotesUrdu.trim()
        val cleanPublisher = PakistaniFormatters.formatTitleCase(publisherName.ifBlank { "Super Admin" })
        try {
            val existing = getLocalFirstOrNull(docRef)
            if (existing != null && existing.exists()) {
                val updates = mapOf(
                    "latestVersionCode" to release.latestVersionCode.coerceAtLeast(1),
                    "latestVersionName" to cleanVersionName,
                    "releaseNotes" to cleanNotes,
                    "releaseNotesUrdu" to cleanNotesUrdu,
                    "apkDownloadUrl" to cleanUrl,
                    "mandatory" to release.mandatory,
                    "publishedBy" to cleanPublisher,
                    "publishedAtIso" to PakistaniFormatters.todayIso(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updates).awaitLocalFirst(OperationType.UPDATE, docRef.path)
            } else {
                val payload = mapOf(
                    "latestVersionCode" to release.latestVersionCode.coerceAtLeast(1),
                    "latestVersionName" to cleanVersionName,
                    "releaseNotes" to cleanNotes,
                    "releaseNotesUrdu" to cleanNotesUrdu,
                    "apkDownloadUrl" to cleanUrl,
                    "mandatory" to release.mandatory,
                    "publishedBy" to cleanPublisher,
                    "publishedAtIso" to PakistaniFormatters.todayIso(),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).awaitLocalFirst(OperationType.CREATE, docRef.path)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
    }

    suspend fun ensureLatestReleasePublished(
        versionCode: Int,
        versionName: String,
        apkUrl: String,
        notesEn: String,
        notesUr: String,
        publisherName: String
    ) {
        val existing = fetchLatestAppReleaseFromServer()
        if (existing == null || existing.latestVersionCode < versionCode) {
            publishAppRelease(
                AppReleaseConfig(
                    id = "latest",
                    latestVersionCode = versionCode,
                    latestVersionName = versionName,
                    releaseNotes = notesEn,
                    releaseNotesUrdu = notesUr,
                    apkDownloadUrl = apkUrl,
                    mandatory = false,
                    publishedBy = publisherName,
                    publishedAtIso = PakistaniFormatters.todayIso()
                ),
                publisherName = publisherName
            )
        }
    }
}
