package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

val WORK_SHIFT_OPTIONS = listOf(
    "Morning (8AM - 4PM)",
    "Evening (4PM - 12AM)",
    "Night (12AM - 8AM)",
    "No Fixed Timing / 24h (مقررہ وقت نہیں)"
)

val REGISTRATION_ROLE_OPTIONS = listOf(
    "Super Admin",
    "Admin",
    "Manager",
    "Staff"
)

fun isAppUpdateAvailable(currentVersionCode: Int, release: AppReleaseConfig?): Boolean {
    return release != null && release.latestVersionCode > currentVersionCode
}

fun canUserEnterMainWorkspace(approvalStatus: String?): Boolean {
    return approvalStatus.equals("APPROVED", ignoreCase = true)
}

const val FACE_ENROLLMENT_GRACE_MILLIS: Long = 2L * 24L * 60L * 60L * 1000L

data class FaceEnrollmentGateState(
    val isEnrolled: Boolean = true,
    val deadlineMillis: Long? = null,
    val remainingMillis: Long? = null,
    val isOverdue: Boolean = false
)

fun evaluateFaceEnrollmentGate(
    isApproved: Boolean,
    hasEnrolledFace: Boolean,
    approvedAtMillis: Long?,
    nowMillis: Long = System.currentTimeMillis()
): FaceEnrollmentGateState {
    if (!isApproved) {
        return FaceEnrollmentGateState(
            isEnrolled = hasEnrolledFace,
            deadlineMillis = null,
            remainingMillis = null,
            isOverdue = false
        )
    }
    if (hasEnrolledFace) {
        return FaceEnrollmentGateState(
            isEnrolled = true,
            deadlineMillis = null,
            remainingMillis = null,
            isOverdue = false
        )
    }
    if (approvedAtMillis != null && approvedAtMillis > 0L) {
        val deadline = approvedAtMillis + FACE_ENROLLMENT_GRACE_MILLIS
        val remaining = (deadline - nowMillis).coerceAtLeast(0L)
        return FaceEnrollmentGateState(
            isEnrolled = false,
            deadlineMillis = deadline,
            remainingMillis = remaining,
            isOverdue = nowMillis >= deadline
        )
    }
    return FaceEnrollmentGateState(
        isEnrolled = false,
        deadlineMillis = null,
        remainingMillis = FACE_ENROLLMENT_GRACE_MILLIS,
        isOverdue = false
    )
}

fun formatRemainingFaceDeadline(remainingMillis: Long?): String {
    if (remainingMillis == null || remainingMillis <= 0L) return "Expired"
    val totalMinutes = remainingMillis / 60_000L
    val days = totalMinutes / (24L * 60L)
    val hours = (totalMinutes % (24L * 60L)) / 60L
    val mins = totalMinutes % 60L
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${mins}m"
        else -> "${mins}m"
    }
}

enum class IdCardPhotoSource(val labelEn: String, val labelUr: String, val priorityOrder: Int) {
    MANUAL("Manual ID Photo", "مینوئل تصویر", 1),
    PROFILE("Profile Photo", "پروفائل تصویر", 2),
    ATTENDANCE("Attendance Face", "اٹینڈنس تصویر", 3),
    DEFAULT_PLACEHOLDER("Default Avatar", "ڈیفالٹ تصویر", 4)
}

data class ResolvedIdCardPhoto(
    val photoData: String?,
    val source: IdCardPhotoSource
)

fun resolveIdCardPhoto(
    manualIdPhoto: String?,
    profilePhoto: String?,
    attendanceFacePhoto: String?
): ResolvedIdCardPhoto {
    val manual = manualIdPhoto?.trim()?.takeIf { it.isNotBlank() }
    if (manual != null) return ResolvedIdCardPhoto(manual, IdCardPhotoSource.MANUAL)

    val profile = profilePhoto?.trim()?.takeIf { it.isNotBlank() }
    if (profile != null) return ResolvedIdCardPhoto(profile, IdCardPhotoSource.PROFILE)

    val attendance = attendanceFacePhoto?.trim()?.takeIf { it.isNotBlank() }
    if (attendance != null) return ResolvedIdCardPhoto(attendance, IdCardPhotoSource.ATTENDANCE)

    return ResolvedIdCardPhoto(null, IdCardPhotoSource.DEFAULT_PLACEHOLDER)
}

fun resolveDepartmentForRole(role: String, shift: String = ""): String = when {
    role.equals("Super Admin", ignoreCase = true) || role.equals("Owner", ignoreCase = true) -> "Executive Management"
    role.equals("Admin", ignoreCase = true) -> "Administration"
    role.equals("Manager", ignoreCase = true) -> "Plant Operations"
    shift.contains("Night", ignoreCase = true) -> "Night Production"
    else -> "Production"
}

enum class UserRole(val label: String) {
    OWNER("Super Admin"),
    ADMIN("Admin"),
    MANAGER("Manager"),
    STAFF("Staff");

    val isOwner: Boolean get() = this == OWNER
    val canManageStaff: Boolean get() = this == OWNER || this == ADMIN
    val canEditOrDelete: Boolean get() = this == OWNER || this == ADMIN

    companion object {
        fun fromString(raw: String?, fallback: UserRole = STAFF): UserRole = when {
            raw.equals("Owner", ignoreCase = true) -> OWNER
            raw.equals("Super Admin", ignoreCase = true) -> OWNER
            else -> entries.firstOrNull { it.label.equals(raw, ignoreCase = true) } ?: fallback
        }

        fun fromStringOrNull(raw: String?): UserRole? = when {
            raw.isNullOrBlank() -> null
            raw.equals("Owner", ignoreCase = true) -> OWNER
            raw.equals("Super Admin", ignoreCase = true) -> OWNER
            else -> entries.firstOrNull { it.label.equals(raw, ignoreCase = true) }
        }

        fun fromRequestedRole(raw: String?): UserRole = when {
            raw.equals("Owner", ignoreCase = true) -> OWNER
            raw.equals("Super Admin", ignoreCase = true) -> OWNER
            raw.equals("Admin", ignoreCase = true) -> ADMIN
            raw.equals("Manager", ignoreCase = true) -> MANAGER
            else -> STAFF
        }
    }
}

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String? = null,
    val phone: String? = null,
    val photoUrl: String? = null,
    val preferredLanguage: String = "en",
    val darkMode: Boolean = false,
    val approvalStatus: String = "PENDING",
    val requestedRole: String = "Staff",
    val linkedFactoryId: String? = null,
    val faceEnrolled: Boolean = false,
    val approvedAtMillis: Long? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Employee(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val authUid: String? = null,
    val name: String = "",
    val fatherName: String = "",
    val phone: String = "",
    val whatsappNumber: String = "",
    val email: String = "",
    val cnic: String = "",
    val role: String = "Staff",
    val salary: Double = 0.0,
    val initialSalary: Double = 0.0,
    val shift: String = "Morning (8AM - 4PM)",
    val joinDate: String = "",
    val emergencyContact: String = "",
    val skipLiveness: Boolean = false,
    val faceTemplateEncrypted: String? = null,
    val idCardPhoto: String? = null,
    val attendanceFacePhoto: String? = null,
    val active: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AttendanceRecord(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val employeeId: String = "",
    val employeeName: String = "",
    val authUid: String = "",
    val date: String = "",
    val checkIn: String = "",
    val checkOut: String = "",
    val status: String = "Present",
    val bypassed: Boolean = false,
    val hours: Double = 0.0,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProductionRecord(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val date: String = "",
    val tons: Double = 0.0,
    val productType: String = "Ice Blocks (10kg)",
    val machineId: String = "",
    val machineName: String = "Plant #1",
    val notes: String? = null,
    val createdBy: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class InventoryMovement(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val product: String = "Ice Blocks",
    val tons: Double = 0.0,
    val location: String = "Cold Room A",
    val movementType: String = "IN",
    val notes: String? = null,
    val createdBy: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Customer(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val phone: String = "",
    val address: String? = null,
    val balance: Double = 0.0,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Sale(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val invoiceNumber: String = "",
    val customerId: String = "",
    val customerName: String = "",
    val product: String = "Ice Blocks (10kg)",
    val quantity: Long = 1L,
    val unitPrice: Double = 1200.0,
    val total: Double = 1200.0,
    val paid: Boolean = true,
    val paidAmount: Double = 1200.0,
    val date: String = "",
    val createdBy: String = "",
    val source: String = "manual",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class Expense(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val category: String = "Other",
    val capitalInvestment: Boolean = false,
    val date: String = "",
    val createdBy: String = "",
    val source: String = "manual",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FundingEntry(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val source: String = "Friend",
    val name: String = "",
    val type: String = "Loan",
    val amount: Double = 0.0,
    val repaid: Double = 0.0,
    val interest: Double = 0.0,
    val dueDate: String? = null,
    val date: String = "",
    val notes: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PayrollRecord(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val employeeId: String = "",
    val authUid: String = "",
    val employeeName: String = "",
    val month: String = "",
    val baseSalary: Double = 0.0,
    val advances: Double = 0.0,
    val deductions: Double = 0.0,
    val netPay: Double = 0.0,
    val paid: Boolean = false,
    val paidDate: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UtilityReading(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val type: String = "Electricity",
    val meterReading: Double = 0.0,
    val unitsOrLiters: Double = 0.0,
    val amount: Double = 0.0,
    val date: String = "",
    val notes: String? = null,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class DeliveryItem(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val vehicleNumber: String = "",
    val driverName: String = "",
    val route: String = "",
    val customerName: String = "",
    val tons: Double = 0.0,
    val status: String = "Idle",
    val date: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class MachineItem(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val model: String = "",
    val capacityTonsPerDay: Double = 0.0,
    val lastServicedDate: String = "",
    val nextServiceDate: String = "",
    val status: String = "Operational",
    val breakdownNote: String? = null,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SupplierItem(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val name: String = "",
    val phone: String = "",
    val itemSupplied: String = "",
    val totalPurchases: Double = 0.0,
    val dues: Double = 0.0,
    val lastPaymentDate: String? = null,
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SafetyIncident(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val description: String = "",
    val severity: String = "Low",
    val status: String = "Open",
    val followUp: String? = null,
    val reportedBy: String = "",
    val date: String = "",
    val memberUids: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FactorySettings(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val companyName: String = "Brohi Ice Factory",
    val phone: String = "0333-2862498",
    val address: String = "SITE Industrial Area, Karachi",
    val iceBlockRate: Double = 1200.0,
    val iceCubeRate: Double = 850.0,
    val operationsStartDate: String = "2026-01-01",
    val notificationsEnabled: Boolean = true,
    val seededDemo: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

data class OwnerAccountingSummary(
    val totalFundingRaised: Double = 0.0,
    val stillToRepay: Double = 0.0,
    val factoryInvestment: Double = 0.0,
    val totalSalesSoFar: Double = 0.0,
    val creditPendingFromCustomers: Double = 0.0,
    val totalOperatingExpenses: Double = 0.0,
    val regularProfitSoFar: Double = 0.0,
    val stillToRecover: Double = 0.0,
    val recoveryPercent: Float = 0f,
    val averageMonthlyProfit: Double = 0.0,
    val estimatedMonthsToBreakEven: Double = 0.0,
    val estimatedCompletionLabel: String = "",
    val isProfitable: Boolean = false,
    val isBreakEvenReached: Boolean = false
)

data class ParsedVoiceIntent(
    val id: String = java.util.UUID.randomUUID().toString(),
    val intentType: String, // SALE, CREDIT_SALE, CREDIT_PAYMENT, EXPENSE, FUNDING, FACTORY_BUILD, PRODUCTION, NAVIGATE, QUERY
    val category: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val tons: Double = 0.0,
    val customerOrSourceName: String = "",
    val targetScreen: String = "",
    val queryType: String = "",
    val spokenReply: String = ""
)

@IgnoreExtraProperties
data class RegistrationRequest(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val applicantUid: String = "",
    val name: String = "",
    val fatherName: String = "",
    val cnic: String = "",
    val phone: String = "",
    val whatsappNumber: String = "",
    val email: String = "",
    val emergencyContact: String = "",
    val requestedRole: String = "Staff",
    val shift: String = "Morning (8AM - 4PM)",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val startingSalary: Double = 35000.0,
    val reviewNote: String? = null,
    val faceTemplateEncrypted: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProfileUpdateRequest(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val applicantUid: String = "",
    val employeeId: String = "",
    val name: String = "",
    val fatherName: String = "",
    val cnic: String = "",
    val phone: String = "",
    val whatsappNumber: String = "",
    val email: String = "",
    val emergencyContact: String = "",
    val shift: String = "Morning (8AM - 4PM)",
    val joinDate: String = "",
    val photoUrl: String? = null,
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val reviewNote: String? = null,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FactoryNotice(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val title: String = "",
    val titleUrdu: String = "",
    val message: String = "",
    val messageUrdu: String = "",
    val noticeType: String = "General", // Holiday, Urgent, Salary, General
    val date: String = "",
    val active: Boolean = true,
    val createdByName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class FactoryDirectoryEntry(
    @DocumentId val id: String = "primary",
    val ownerId: String = "",
    val companyName: String = "Brohi Ice Factory",
    val ownerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AppReleaseConfig(
    @DocumentId val id: String = "latest",
    val latestVersionCode: Int = 1,
    val latestVersionName: String = "1.0",
    val releaseNotes: String = "",
    val releaseNotesUrdu: String = "",
    val apkDownloadUrl: String = "",
    val mandatory: Boolean = false,
    val publishedBy: String = "",
    val publishedAtIso: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
