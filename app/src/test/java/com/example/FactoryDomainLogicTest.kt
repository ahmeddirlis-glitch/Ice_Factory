package com.example

import androidx.compose.ui.text.AnnotatedString
import com.example.data.model.Employee
import com.example.data.model.PayrollRecord
import com.example.data.model.UserRole
import com.example.data.repository.VoiceAiRepository
import com.example.data.util.FaceCryptoHelper
import com.example.data.util.PakistaniFormatters
import com.example.ui.components.CnicVisualTransformation
import com.example.ui.components.PhoneVisualTransformation
import com.example.ui.navigation.AppRoute
import com.example.ui.screens.computeStartingSalary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FactoryDomainLogicTest {

    @Test
    fun `pakistani numbering formats lakhs and crores accurately`() {
        assertEquals("Rs. 1,200", PakistaniFormatters.formatPkr(1200.0))
        assertEquals("Rs. 1,25,000", PakistaniFormatters.formatPkr(125000.0))
        assertEquals("Rs. 50,00,000", PakistaniFormatters.formatPkr(5000000.0))
        assertEquals("Rs. 2,00,00,000", PakistaniFormatters.formatPkr(20000000.0))
    }

    @Test
    fun `voice transcript normalization converts urdu digits and hazar lakh to Rs format`() {
        assertEquals(
            "Rs. 75,000 بجلی کا بل",
            PakistaniFormatters.normalizeVoiceTranscript("75 ہزار بجلی کا بل")
        )
        assertEquals(
            "Rs. 75,000 بجلی کا بل",
            PakistaniFormatters.normalizeVoiceTranscript("۷۵ ہزار بجلی کا بل")
        )
        assertEquals(
            "record Rs. 75,000 electricity bill",
            PakistaniFormatters.normalizeVoiceTranscript("record 75000 electricity bill")
        )
        assertEquals(
            "Rs. 2,00,000",
            PakistaniFormatters.normalizeVoiceTranscript("2 لاکھ")
        )
        assertEquals(
            "12 ton",
            PakistaniFormatters.normalizeVoiceTranscript("12 ton")
        )
        assertEquals(
            "2 chai Rs. 260 aur pani Rs. 800",
            PakistaniFormatters.normalizeVoiceTranscript("2 chai 260 aur pani 800")
        )
    }

    @Test
    fun `local hybrid voice parser classifies expense sale credit payment and multi item`() {
        val parser = VoiceAiRepository()

        val exp = parser.parseTranscriptLocally("record Rs. 75,000 electricity bill", UserRole.OWNER)
        assertEquals(1, exp.size)
        assertEquals("EXPENSE", exp.first().intentType)
        assertEquals("Electricity", exp.first().category)
        assertEquals(75000.0, exp.first().amount, 0.01)

        val sale = parser.parseTranscriptLocally("Rs. 15,000 ki baraf sale hui", UserRole.OWNER)
        assertEquals(1, sale.size)
        assertEquals("SALE", sale.first().intentType)
        assertEquals(15000.0, sale.first().amount, 0.01)

        val creditRecovery = parser.parseTranscriptLocally("Al-Madina udhar wapas aaya Rs. 10,000", UserRole.OWNER)
        assertEquals(1, creditRecovery.size)
        assertEquals("CREDIT_PAYMENT", creditRecovery.first().intentType)
        assertEquals(10000.0, creditRecovery.first().amount, 0.01)

        val multi = parser.parseTranscriptLocally("2 chai 260 aur pani 800", UserRole.OWNER)
        assertEquals(2, multi.size)
        assertEquals("EXPENSE", multi[0].intentType)
        assertEquals("Refreshment", multi[0].category)
        assertEquals(260.0, multi[0].amount, 0.01)
        assertEquals("EXPENSE", multi[1].intentType)
        assertEquals("Refreshment", multi[1].category)
        assertEquals(800.0, multi[1].amount, 0.01)
    }

    @Test
    fun `computeStartingSalary uses explicit initialSalary or earliest slip`() {
        val empExplicit = Employee(id = "e1", name = "Bilal", salary = 110000.0, initialSalary = 25000.0)
        assertEquals(25000.0, computeStartingSalary(empExplicit, emptyList()), 0.01)

        val empFromSlips = Employee(id = "e2", name = "Tariq", salary = 75000.0, initialSalary = 0.0)
        val slips = listOf(
            PayrollRecord(id = "p2", employeeId = "e2", month = "2026-11", baseSalary = 75000.0),
            PayrollRecord(id = "p1", employeeId = "e2", month = "2022-01", baseSalary = 25000.0)
        )
        assertEquals(25000.0, computeStartingSalary(empFromSlips, slips), 0.01)
    }

    @Test
    fun `face crypto round trip encrypts and matches identical descriptor`() {
        val sample = listOf(0.12f, -0.34f, 0.56f, 0.78f, -0.21f, 0.44f, 0.65f, -0.11f)
        val encrypted = FaceCryptoHelper.encryptTemplate("owner_uid_123", sample)
        val decrypted = FaceCryptoHelper.decryptTemplate("owner_uid_123", encrypted)
        assertEquals(sample.size, decrypted.size)
        val similarity = FaceCryptoHelper.cosineSimilarity(sample, decrypted)
        assertTrue("Expected high similarity, got $similarity", similarity > 0.99f)
    }

    @Test
    fun `owner only routes exclude non owner roles and admin can manage staff`() {
        assertTrue(UserRole.OWNER.isOwner)
        assertTrue(UserRole.OWNER.canManageStaff)
        assertTrue(UserRole.ADMIN.canManageStaff)
        assertFalse(UserRole.ADMIN.isOwner)
        assertFalse(UserRole.MANAGER.isOwner)
        assertFalse(UserRole.MANAGER.canManageStaff)
        assertFalse(UserRole.STAFF.isOwner)
        assertEquals(UserRole.OWNER, UserRole.fromString("Owner"))
        assertEquals(UserRole.OWNER, UserRole.fromString("Super Admin"))
        assertTrue("users" in AppRoute.staffManagerKeys)
        assertTrue("salary" in AppRoute.ownerOnlyKeys)
        assertTrue("reports" in AppRoute.ownerOnlyKeys)
    }

    @Test
    fun `title case formatter normalizes lowercase and uppercase names properly`() {
        assertEquals("Ahmed Dirlis", PakistaniFormatters.formatTitleCase("ahmed dirlis"))
        assertEquals("Muhammad Tariq Khan", PakistaniFormatters.formatTitleCase("  MUHAMMAD   TARIQ KHAN "))
        assertEquals("Sara Ali [Demo]", PakistaniFormatters.formatTitleCase("sara ali [Demo]"))
        assertEquals("", PakistaniFormatters.formatTitleCase("   "))
    }

    @Test
    fun `staff role is strictly limited to self service routes`() {
        assertTrue(AppRoute.isAllowedFor(UserRole.STAFF, AppRoute.Dashboard.key))
        assertTrue(AppRoute.isAllowedFor(UserRole.STAFF, AppRoute.Attendance.key))
        assertTrue(AppRoute.isAllowedFor(UserRole.STAFF, AppRoute.Salary.key))
        assertTrue(AppRoute.isAllowedFor(UserRole.STAFF, AppRoute.Notices.key))
        assertTrue(AppRoute.isAllowedFor(UserRole.STAFF, AppRoute.Profile.key))
        assertTrue(AppRoute.isAllowedFor(UserRole.STAFF, AppRoute.Incidents.key))

        assertFalse(AppRoute.isAllowedFor(UserRole.STAFF, AppRoute.Sales.key))
        assertFalse(AppRoute.isAllowedFor(UserRole.STAFF, AppRoute.Expenses.key))
        assertFalse(AppRoute.isAllowedFor(UserRole.STAFF, AppRoute.Production.key))
        assertFalse(AppRoute.isAllowedFor(UserRole.STAFF, AppRoute.UsersRoles.key))
        assertFalse(AppRoute.isAllowedFor(UserRole.STAFF, AppRoute.Reports.key))
    }

    @Test
    fun `voice parser restricts staff to self service queries and blocks financial writes`() {
        val parser = VoiceAiRepository()

        val noticeQ = parser.parseTranscriptLocally("kal chutti hai?", UserRole.STAFF)
        assertEquals(1, noticeQ.size)
        assertEquals("QUERY", noticeQ.first().intentType)
        assertEquals("notices_summary", noticeQ.first().queryType)

        val salaryQ = parser.parseTranscriptLocally("meri tankhwah batao", UserRole.STAFF)
        assertEquals(1, salaryQ.size)
        assertEquals("QUERY", salaryQ.first().intentType)
        assertEquals("salary_summary", salaryQ.first().queryType)

        val blockedSale = parser.parseTranscriptLocally("15000 ki baraf sale hui", UserRole.STAFF)
        assertEquals(1, blockedSale.size)
        assertEquals("QUERY", blockedSale.first().intentType)
        assertEquals("staff_denied", blockedSale.first().queryType)
    }

    @Test
    fun `cnic formatter auto inserts dashes strips non digits and caps at 13 digits`() {
        assertEquals("4420412538355", PakistaniFormatters.normalizeCnicDigits("44204-1253835-5"))
        assertEquals("4420412538355", PakistaniFormatters.normalizeCnicDigits("44204-1253835-59999abc"))
        assertEquals("4420412538355", PakistaniFormatters.normalizeCnicDigits("۴۴۲۰۴-۱۲۵۳۸۳۵-۵۸۸"))

        assertEquals("", PakistaniFormatters.formatCnic(""))
        assertEquals("44204", PakistaniFormatters.formatCnic("44204"))
        assertEquals("44204-1", PakistaniFormatters.formatCnic("442041"))
        assertEquals("44204-1253835", PakistaniFormatters.formatCnic("442041253835"))
        assertEquals("44204-1253835-5", PakistaniFormatters.formatCnic("4420412538355"))
        assertEquals("44204-1253835-5", PakistaniFormatters.formatCnic("4420412538355999"))

        assertTrue(PakistaniFormatters.isValidCnic("44204-1253835-5"))
        assertTrue(PakistaniFormatters.isValidCnic("4420412538355"))
        assertFalse(PakistaniFormatters.isValidCnic("44204-125383"))
    }

    @Test
    fun `cnic visual transformation inserts dashes and maps offsets cleanly`() {
        val vt = CnicVisualTransformation()
        val transformed = vt.filter(AnnotatedString("4420412538355"))
        assertEquals("44204-1253835-5", transformed.text.text)

        val map = transformed.offsetMapping
        assertEquals(0, map.originalToTransformed(0))
        assertEquals(5, map.originalToTransformed(5))
        assertEquals(7, map.originalToTransformed(6))
        assertEquals(13, map.originalToTransformed(12))
        assertEquals(15, map.originalToTransformed(13))

        assertEquals(5, map.transformedToOriginal(5))
        assertEquals(5, map.transformedToOriginal(6))
        assertEquals(6, map.transformedToOriginal(7))
        assertEquals(12, map.transformedToOriginal(13))
        assertEquals(12, map.transformedToOriginal(14))
        assertEquals(13, map.transformedToOriginal(15))
    }

    @Test
    fun `mobile phone formatter auto inserts dash strips non digits and caps at 11 digits`() {
        assertEquals("03332828285", PakistaniFormatters.normalizePhoneDigits("0333-2828285"))
        assertEquals("03332828285", PakistaniFormatters.normalizePhoneDigits("0333-28282859999abc"))
        assertEquals("03332828285", PakistaniFormatters.normalizePhoneDigits("۰۳۳۳-۲۸۲۸۲۸۵۹۹"))

        assertEquals("", PakistaniFormatters.formatPhone(""))
        assertEquals("0333", PakistaniFormatters.formatPhone("0333"))
        assertEquals("0333-2", PakistaniFormatters.formatPhone("03332"))
        assertEquals("0333-2828285", PakistaniFormatters.formatPhone("03332828285"))
        assertEquals("0333-2828285", PakistaniFormatters.formatPhone("03332828285999"))

        assertTrue(PakistaniFormatters.isValidPhone("0333-2828285"))
        assertTrue(PakistaniFormatters.isValidPhone("03332828285"))
        assertFalse(PakistaniFormatters.isValidPhone("0333-282828"))
    }

    @Test
    fun `phone visual transformation inserts dash after 4 digits and maps offsets cleanly`() {
        val vt = PhoneVisualTransformation()
        val transformed = vt.filter(AnnotatedString("03332828285"))
        assertEquals("0333-2828285", transformed.text.text)

        val map = transformed.offsetMapping
        assertEquals(0, map.originalToTransformed(0))
        assertEquals(4, map.originalToTransformed(4))
        assertEquals(6, map.originalToTransformed(5))
        assertEquals(12, map.originalToTransformed(11))

        assertEquals(4, map.transformedToOriginal(4))
        assertEquals(4, map.transformedToOriginal(5))
        assertEquals(5, map.transformedToOriginal(6))
        assertEquals(11, map.transformedToOriginal(12))
    }

    @Test
    fun `emergency contact must differ from mobile and whatsapp while whatsapp may match mobile`() {
        assertTrue(
            PakistaniFormatters.isEmergencySameAsPrimary(
                emergencyRaw = "0333-2828285",
                mobileRaw = "03332828285",
                whatsappRaw = ""
            )
        )
        assertTrue(
            PakistaniFormatters.isEmergencySameAsPrimary(
                emergencyRaw = "0300-1112233",
                mobileRaw = "0333-2828285",
                whatsappRaw = "03001112233"
            )
        )
        assertFalse(
            PakistaniFormatters.isEmergencySameAsPrimary(
                emergencyRaw = "0321-9988776",
                mobileRaw = "0333-2828285",
                whatsappRaw = "0333-2828285"
            )
        )
        assertFalse(
            PakistaniFormatters.isEmergencySameAsPrimary(
                emergencyRaw = "",
                mobileRaw = "0333-2828285",
                whatsappRaw = "0333-2828285"
            )
        )
    }

    @Test
    fun `requested role parser never defaults unknown or staff to super admin`() {
        assertEquals(UserRole.STAFF, UserRole.fromRequestedRole("Staff"))
        assertEquals(UserRole.STAFF, UserRole.fromRequestedRole(""))
        assertEquals(UserRole.STAFF, UserRole.fromRequestedRole(null))
        assertEquals(UserRole.STAFF, UserRole.fromRequestedRole("Helper"))
        assertEquals(UserRole.MANAGER, UserRole.fromRequestedRole("Manager"))
        assertEquals(UserRole.ADMIN, UserRole.fromRequestedRole("Admin"))
        assertEquals(UserRole.OWNER, UserRole.fromRequestedRole("Super Admin"))
        assertEquals(UserRole.OWNER, UserRole.fromRequestedRole("Owner"))
        assertEquals(UserRole.STAFF, UserRole.fromString(null))
    }

    @Test
    fun `new registrant and non owner profiles can never enter as super admin`() {
        val pendingStaff = com.example.data.model.UserProfile(
            userId = "u1",
            approvalStatus = "PENDING",
            requestedRole = "Staff"
        )
        assertEquals(
            UserRole.STAFF,
            com.example.ui.viewmodel.resolveEffectiveUserRole(
                ownerPreviewRole = UserRole.OWNER,
                profile = pendingStaff,
                hasPendingRegistration = false
            )
        )
        val pendingAdmin = com.example.data.model.UserProfile(
            userId = "u2",
            approvalStatus = "PENDING",
            requestedRole = "Admin"
        )
        assertEquals(
            UserRole.STAFF,
            com.example.ui.viewmodel.resolveEffectiveUserRole(
                ownerPreviewRole = null,
                profile = pendingAdmin,
                hasPendingRegistration = true
            )
        )
        val approvedStaff = pendingStaff.copy(approvalStatus = "APPROVED")
        assertEquals(
            UserRole.STAFF,
            com.example.ui.viewmodel.resolveEffectiveUserRole(
                ownerPreviewRole = UserRole.OWNER,
                profile = approvedStaff,
                hasPendingRegistration = false
            )
        )
        assertFalse(com.example.ui.viewmodel.canUserSwitchWorkspaceRole(approvedStaff))

        val approvedAdmin = pendingAdmin.copy(approvalStatus = "APPROVED")
        assertEquals(
            UserRole.ADMIN,
            com.example.ui.viewmodel.resolveEffectiveUserRole(
                ownerPreviewRole = UserRole.OWNER,
                profile = approvedAdmin,
                hasPendingRegistration = false
            )
        )
        assertFalse(com.example.ui.viewmodel.canUserSwitchWorkspaceRole(approvedAdmin))

        val approvedOwner = com.example.data.model.UserProfile(
            userId = "owner",
            approvalStatus = "APPROVED",
            requestedRole = "Super Admin"
        )
        assertTrue(com.example.ui.viewmodel.canUserSwitchWorkspaceRole(approvedOwner))
        assertEquals(
            UserRole.OWNER,
            com.example.ui.viewmodel.resolveEffectiveUserRole(
                ownerPreviewRole = null,
                profile = approvedOwner,
                hasPendingRegistration = false
            )
        )
        assertEquals(
            UserRole.STAFF,
            com.example.ui.viewmodel.resolveEffectiveUserRole(
                ownerPreviewRole = UserRole.STAFF,
                profile = approvedOwner,
                hasPendingRegistration = false
            )
        )
    }

    @Test
    fun `work shift options include unscheduled 24h option within firestore length rules`() {
        assertTrue(com.example.data.model.WORK_SHIFT_OPTIONS.size == 4)
        val unscheduled = com.example.data.model.WORK_SHIFT_OPTIONS.last()
        assertTrue(unscheduled.contains("No Fixed Timing", ignoreCase = true))
        assertTrue(unscheduled.length in 1..60)
    }

    @Test
    fun `splash screen footer is branded Powered by SHIPDAR and TECH in both languages`() {
        assertEquals("Powered by SHIPDAR & TECH", com.example.data.util.getAppStrings("en").poweredByFirebase)
        assertEquals("Powered by SHIPDAR & TECH", com.example.data.util.getAppStrings("ur").poweredByFirebase)
    }

    @Test
    fun `voice confirmation popup mode is enabled by default and parses urdu labor and sales cleanly`() {
        val defaultVoiceState = com.example.ui.viewmodel.VoiceUiState()
        assertFalse(defaultVoiceState.autoSaveEnabled)

        val parser = VoiceAiRepository()
        val laborExp = parser.parseTranscriptLocally("پندرہ ہزار مزدوری دی", UserRole.OWNER)
        assertEquals(1, laborExp.size)
        assertEquals("EXPENSE", laborExp.first().intentType)
        assertEquals("Salary", laborExp.first().category)
        assertEquals(15000.0, laborExp.first().amount, 0.01)

        val saleUrdu = parser.parseTranscriptLocally("پچاس ہزار برف بکری", UserRole.ADMIN)
        assertEquals(1, saleUrdu.size)
        assertEquals("SALE", saleUrdu.first().intentType)
        assertEquals(50000.0, saleUrdu.first().amount, 0.01)
    }

    @Test
    fun `approval gate blocks dashboard until server or profile confirms APPROVED`() {
        assertEquals(
            "LOADING",
            com.example.ui.viewmodel.resolveEffectiveApprovalStatus(
                profile = null,
                myRequest = null,
                isInitialized = false,
                hasPendingRegistration = false
            )
        )
        assertEquals(
            "PENDING",
            com.example.ui.viewmodel.resolveEffectiveApprovalStatus(
                profile = com.example.data.model.UserProfile(userId = "u1", approvalStatus = "PENDING"),
                myRequest = null,
                isInitialized = true,
                hasPendingRegistration = false
            )
        )
        assertEquals(
            "REJECTED",
            com.example.ui.viewmodel.resolveEffectiveApprovalStatus(
                profile = com.example.data.model.UserProfile(userId = "u1", approvalStatus = "PENDING"),
                myRequest = com.example.data.model.RegistrationRequest(id = "u1", status = "REJECTED"),
                isInitialized = true,
                hasPendingRegistration = false
            )
        )
        assertEquals(
            "APPROVED",
            com.example.ui.viewmodel.resolveEffectiveApprovalStatus(
                profile = com.example.data.model.UserProfile(userId = "u1", approvalStatus = "PENDING"),
                myRequest = com.example.data.model.RegistrationRequest(id = "u1", status = "APPROVED"),
                isInitialized = true,
                hasPendingRegistration = false
            )
        )

        assertFalse(com.example.data.model.canUserEnterMainWorkspace("LOADING"))
        assertFalse(com.example.data.model.canUserEnterMainWorkspace("PENDING"))
        assertFalse(com.example.data.model.canUserEnterMainWorkspace("REJECTED"))
        assertTrue(com.example.data.model.canUserEnterMainWorkspace("APPROVED"))
    }

    @Test
    fun `registration role options include Super Admin and all roles can access settings for updates`() {
        assertTrue("Super Admin" in com.example.data.model.REGISTRATION_ROLE_OPTIONS)
        assertTrue("Admin" in com.example.data.model.REGISTRATION_ROLE_OPTIONS)
        assertTrue("Manager" in com.example.data.model.REGISTRATION_ROLE_OPTIONS)
        assertTrue("Staff" in com.example.data.model.REGISTRATION_ROLE_OPTIONS)

        assertTrue(AppRoute.isAllowedFor(UserRole.OWNER, AppRoute.Settings.key))
        assertTrue(AppRoute.isAllowedFor(UserRole.ADMIN, AppRoute.Settings.key))
        assertTrue(AppRoute.isAllowedFor(UserRole.MANAGER, AppRoute.Settings.key))
        assertTrue(AppRoute.isAllowedFor(UserRole.STAFF, AppRoute.Settings.key))
        assertFalse(AppRoute.isAllowedFor(UserRole.MANAGER, AppRoute.Salary.key))
        assertFalse(AppRoute.isAllowedFor(UserRole.MANAGER, AppRoute.Reports.key))
    }

    @Test
    fun `app release comparison and https apk url validation work reliably`() {
        assertFalse(com.example.data.model.isAppUpdateAvailable(1, null))
        assertFalse(
            com.example.data.model.isAppUpdateAvailable(
                2,
                com.example.data.model.AppReleaseConfig(latestVersionCode = 2, latestVersionName = "1.1")
            )
        )
        assertTrue(
            com.example.data.model.isAppUpdateAvailable(
                1,
                com.example.data.model.AppReleaseConfig(latestVersionCode = 2, latestVersionName = "1.1")
            )
        )

        assertTrue(com.example.data.util.AppUpdateManager.isValidApkUrl("https://example.com/releases/brohi-v2.apk"))
        assertFalse(com.example.data.util.AppUpdateManager.isValidApkUrl("http://example.com/app.apk"))
        assertFalse(com.example.data.util.AppUpdateManager.isValidApkUrl(""))
        assertFalse(com.example.data.util.AppUpdateManager.isValidApkUrl("ftp://example.com/app.apk"))
    }

    @Test
    fun `face crypto helper encrypts and roundtrip decrypts descriptors with global and owner fallback`() {
        val sampleDescriptor = List(26) { idx -> (idx + 1) * 0.05f }
        val encryptedGlobal = FaceCryptoHelper.encryptTemplate("global", sampleDescriptor)
        assertTrue(encryptedGlobal.isNotBlank())

        val recovered = FaceCryptoHelper.decryptTemplate("owner-factory-123", encryptedGlobal)
        assertEquals(26, recovered.size)
        sampleDescriptor.forEachIndexed { i, expected ->
            assertEquals(expected, recovered[i], 0.0001f)
        }

        val encryptedOwner = FaceCryptoHelper.encryptTemplate("owner-factory-123", sampleDescriptor)
        val recoveredOwner = FaceCryptoHelper.decryptTemplate("owner-factory-123", encryptedOwner)
        assertEquals(26, recoveredOwner.size)
        sampleDescriptor.forEachIndexed { i, expected ->
            assertEquals(expected, recoveredOwner[i], 0.0001f)
        }
    }

    @Test
    fun `cosine similarity accurately identifies same face and rejects impostor faces`() {
        val originalFace = List(26) { 0.5f }
        val slightAngleChange = List(26) { idx -> 0.5f + if (idx % 2 == 0) 0.02f else -0.02f }
        val completelyDifferentImpostor = List(26) { idx -> if (idx < 13) 0.9f else -0.8f }

        val selfScore = FaceCryptoHelper.cosineSimilarity(originalFace, originalFace)
        val samePersonScore = FaceCryptoHelper.cosineSimilarity(originalFace, slightAngleChange)
        val impostorScore = FaceCryptoHelper.cosineSimilarity(originalFace, completelyDifferentImpostor)

        assertEquals(1.0f, selfScore, 0.001f)
        assertTrue("Same person score should be >= 0.85f: $samePersonScore", samePersonScore >= 0.85f)
        assertTrue("Impostor score should be < 0.80f: $impostorScore", impostorScore < 0.80f)
    }

    @Test
    fun `registration request correctly retains encrypted face template`() {
        val req = com.example.data.model.RegistrationRequest(
            name = "Muhammad Ali",
            cnic = "42101-1234567-1",
            phone = "0333-1234567",
            faceTemplateEncrypted = "encrypted-sample-token-abc-123"
        )
        assertEquals("encrypted-sample-token-abc-123", req.faceTemplateEncrypted)
    }

    @Test
    fun `multi pose face template encrypts and decrypts all 4 poses cleanly`() {
        val front = List(44) { idx -> (idx - 22) * 0.04f }
        val left = List(44) { idx -> (idx - 22) * 0.04f + 0.03f }
        val right = List(44) { idx -> (idx - 22) * 0.04f - 0.03f }
        val up = List(44) { idx -> (idx - 20) * 0.045f }

        val encoded = FaceCryptoHelper.encryptMultiPoseTemplate(
            ownerUid = "owner-xyz",
            poseDescriptors = listOf(front, left, right, up)
        )
        assertTrue(encoded.startsWith("MP1|"))

        val allDecrypted = FaceCryptoHelper.decryptMultiPoseTemplates("owner-xyz", encoded)
        assertEquals(4, allDecrypted.size)
        front.forEachIndexed { idx, expected ->
            assertEquals(expected, allDecrypted[0][idx], 0.0001f)
        }
        up.forEachIndexed { idx, expected ->
            assertEquals(expected, allDecrypted[3][idx], 0.0001f)
        }

        val singleFront = FaceCryptoHelper.decryptTemplate("owner-xyz", encoded)
        assertEquals(44, singleFront.size)
        front.forEachIndexed { idx, expected ->
            assertEquals(expected, singleFront[idx], 0.0001f)
        }
    }

    @Test
    fun `strict biometric match score accepts live variation of enrolled user and rejects impostor`() {
        val enrolled = List(44) { idx ->
            val wave = kotlin.math.sin(idx * 0.45).toFloat() * 0.65f
            1.1f + wave
        }
        val samePersonLive = enrolled.mapIndexed { idx, v ->
            v + if (idx % 3 == 0) 0.025f else -0.02f
        }
        val impostorLive = List(44) { idx ->
            val wave = kotlin.math.cos(idx * 0.92).toFloat() * 0.65f
            1.1f + wave
        }

        val sameScore = FaceCryptoHelper.strictBiometricMatchScore(samePersonLive, enrolled)
        val impostorScore = FaceCryptoHelper.strictBiometricMatchScore(impostorLive, enrolled)

        assertTrue("Expected genuine person score >= 0.74f, got $sameScore", sameScore != null && sameScore >= 0.74f)
        assertEquals(null, impostorScore)
    }

    @Test
    fun `two day face enrollment gate tracks countdown and auto blocks after 48 hours unless enrolled`() {
        val approvedAt = 1_700_000_000_000L
        val grace = com.example.data.model.FACE_ENROLLMENT_GRACE_MILLIS

        val withinGrace = com.example.data.model.evaluateFaceEnrollmentGate(
            isApproved = true,
            hasEnrolledFace = false,
            approvedAtMillis = approvedAt,
            nowMillis = approvedAt + (12L * 60L * 60L * 1000L)
        )
        assertFalse(withinGrace.isEnrolled)
        assertFalse(withinGrace.isOverdue)
        assertEquals(36L * 60L * 60L * 1000L, withinGrace.remainingMillis)
        assertEquals("1d 12h", com.example.data.model.formatRemainingFaceDeadline(withinGrace.remainingMillis))

        val expiredGate = com.example.data.model.evaluateFaceEnrollmentGate(
            isApproved = true,
            hasEnrolledFace = false,
            approvedAtMillis = approvedAt,
            nowMillis = approvedAt + grace + 1000L
        )
        assertFalse(expiredGate.isEnrolled)
        assertTrue(expiredGate.isOverdue)
        assertEquals(0L, expiredGate.remainingMillis)
        assertEquals("Expired", com.example.data.model.formatRemainingFaceDeadline(expiredGate.remainingMillis))

        val enrolledGate = com.example.data.model.evaluateFaceEnrollmentGate(
            isApproved = true,
            hasEnrolledFace = true,
            approvedAtMillis = approvedAt,
            nowMillis = approvedAt + (grace * 10L)
        )
        assertTrue(enrolledGate.isEnrolled)
        assertFalse(enrolledGate.isOverdue)
    }

    @Test
    fun `digital id card photo resolution strictly follows manual then profile then attendance priority`() {
        val manualActive = com.example.data.model.resolveIdCardPhoto(
            manualIdPhoto = "data:image/jpeg;base64,MANUAL",
            profilePhoto = "https://example.com/profile.jpg",
            attendanceFacePhoto = "data:image/jpeg;base64,ATTENDANCE"
        )
        assertEquals(com.example.data.model.IdCardPhotoSource.MANUAL, manualActive.source)
        assertEquals("data:image/jpeg;base64,MANUAL", manualActive.photoData)

        val profileFallback = com.example.data.model.resolveIdCardPhoto(
            manualIdPhoto = "   ",
            profilePhoto = "https://example.com/profile.jpg",
            attendanceFacePhoto = "data:image/jpeg;base64,ATTENDANCE"
        )
        assertEquals(com.example.data.model.IdCardPhotoSource.PROFILE, profileFallback.source)
        assertEquals("https://example.com/profile.jpg", profileFallback.photoData)

        val attendanceFallback = com.example.data.model.resolveIdCardPhoto(
            manualIdPhoto = null,
            profilePhoto = "",
            attendanceFacePhoto = "data:image/jpeg;base64,ATTENDANCE"
        )
        assertEquals(com.example.data.model.IdCardPhotoSource.ATTENDANCE, attendanceFallback.source)
        assertEquals("data:image/jpeg;base64,ATTENDANCE", attendanceFallback.photoData)

        val defaultFallback = com.example.data.model.resolveIdCardPhoto(
            manualIdPhoto = null,
            profilePhoto = null,
            attendanceFacePhoto = null
        )
        assertEquals(com.example.data.model.IdCardPhotoSource.DEFAULT_PLACEHOLDER, defaultFallback.source)
        assertEquals(null, defaultFallback.photoData)
    }

    @Test
    fun `digital id card generates cnic qr bitmap and one year expiry accurately`() {
        val (joining, expiry) = com.example.data.util.ImageAndQrUtils.computeJoiningAndOneYearExpiry("2025-03-15")
        assertEquals("15-03-2025", joining)
        assertEquals("15-03-2026", expiry)

        val empCode = com.example.data.util.ImageAndQrUtils.formatEmployeeIdCode(
            employeeId = "emp_017",
            joinDate = "2025-03-15",
            cnic = "44204-1253835-5"
        )
        assertEquals("BIF-2025-017", empCode)

        val qrPayload = com.example.data.util.ImageAndQrUtils.buildEmployeeQrPayload(
            cnic = "4420412538355",
            employeeCode = empCode,
            name = "muhammad usman",
            role = "Staff"
        )
        assertTrue(qrPayload.contains("44204-1253835-5"))
        assertTrue(qrPayload.contains("BIF-2025-017"))

        val bitmap = com.example.data.util.ImageAndQrUtils.generateQrCodeBitmap(qrPayload, sizePx = 128)
        assertTrue(bitmap != null && bitmap.width == 128 && bitmap.height == 128)
    }

    @Test
    fun `multimodal MP2 biometric matching accepts lighting variation of enrolled user and rejects colleague with identical landmarks`() {
        val patchW = FaceCryptoHelper.PATCH_SIZE
        val patchH = FaceCryptoHelper.PATCH_SIZE

        fun makeSyntheticFacePatch(
            browDrop: Float,
            noseShade: Float,
            hasBeard: Boolean,
            brightnessGain: Float,
            sideLampBias: Float
        ): FloatArray {
            val px = FloatArray(patchW * patchH)
            for (y in 0 until patchH) {
                for (x in 0 until patchW) {
                    val nx = x / patchW.toFloat()
                    var valLuma = 165f
                    // Eyebrows
                    if (y in (8 + browDrop.toInt())..(11 + browDrop.toInt()) && (x in 6..19 || x in 28..41)) {
                        valLuma -= 65f
                    }
                    // Eye sockets
                    if (y in 13..18 && (x in 9..17 || x in 31..39)) {
                        valLuma -= 55f
                    }
                    // Nose bridge & highlight
                    if (y in 18..31 && x in 21..27) {
                        valLuma += noseShade
                    }
                    // Mouth / lips
                    if (y in 36..39 && x in 17..31) {
                        valLuma -= 42f
                    }
                    // Optional beard / mustache texture
                    if (hasBeard && ((y in 32..35 && x in 15..33) || (y in 40..47 && x in 10..38))) {
                        valLuma -= if ((x + y) % 2 == 0) 60f else 45f
                    }
                    // Apply lighting gain & side-lamp gradient
                    val lamp = (nx - 0.5f) * sideLampBias
                    px[y * patchW + x] = (valLuma * brightnessGain + lamp).coerceIn(5f, 250f)
                }
            }
            return px
        }

        val sharedGeometry = List(FaceCryptoHelper.GEOMETRY_DIM) { idx ->
            1.0f + kotlin.math.sin(idx * 0.45).toFloat() * 0.65f
        }

        val enrolledPatch = makeSyntheticFacePatch(
            browDrop = 0f,
            noseShade = 22f,
            hasBeard = true,
            brightnessGain = 1.0f,
            sideLampBias = 0f
        )
        val samePersonDiffLightingPatch = makeSyntheticFacePatch(
            browDrop = 0f,
            noseShade = 22f,
            hasBeard = true,
            brightnessGain = 0.82f,
            sideLampBias = 18f
        )
        val colleaguePatch = makeSyntheticFacePatch(
            browDrop = 3f,
            noseShade = -18f,
            hasBeard = false,
            brightnessGain = 1.0f,
            sideLampBias = 0f
        )

        val enrolledDescriptor = sharedGeometry + FaceCryptoHelper.extractPatchAppearanceAndTexture(enrolledPatch)
        val samePersonLive = sharedGeometry + FaceCryptoHelper.extractPatchAppearanceAndTexture(samePersonDiffLightingPatch)
        // Colleague shares 100% identical skull/landmark geometry!
        val colleagueLive = sharedGeometry + FaceCryptoHelper.extractPatchAppearanceAndTexture(colleaguePatch)

        assertEquals(FaceCryptoHelper.MULTIMODAL_DIM, enrolledDescriptor.size)

        val encodedMp2 = FaceCryptoHelper.encryptMultiPoseTemplate("owner_uid", listOf(enrolledDescriptor))
        assertTrue(encodedMp2.startsWith("MP2|"))
        assertFalse(FaceCryptoHelper.isLegacyTemplate("owner_uid", encodedMp2))

        val encodedLegacy = FaceCryptoHelper.encryptMultiPoseTemplate("owner_uid", listOf(sharedGeometry))
        assertTrue(encodedLegacy.startsWith("MP1|"))
        assertTrue(FaceCryptoHelper.isLegacyTemplate("owner_uid", encodedLegacy))

        val decryptedFront = FaceCryptoHelper.decryptTemplate("owner_uid", encodedMp2)
        val genuineScore = FaceCryptoHelper.strictBiometricMatchScore(samePersonLive, decryptedFront)
        val colleagueScore = FaceCryptoHelper.strictBiometricMatchScore(colleagueLive, decryptedFront)

        assertTrue(
            "Same person across lighting change should pass (>= 0.76f), got $genuineScore",
            genuineScore != null && genuineScore >= FaceCryptoHelper.MIN_MULTIMODAL_SCORE
        )
        assertEquals(
            "Colleague with identical landmarks but different facial appearance/texture MUST be rejected",
            null,
            colleagueScore
        )
    }

    @Test
    fun `checkout authorization blocks colleague face mismatch and enforces strict owner binding`() {
        val myOpenCheckIn = com.example.data.model.AttendanceRecord(
            id = "att_1",
            employeeId = "emp_user_1",
            employeeName = "Usman",
            authUid = "uid_user_1",
            date = "2026-11-18",
            checkIn = "09:00 AM",
            checkOut = "",
            status = "Present"
        )

        // Colleague verified face in kiosk or mismatch state CANNOT check out Usman's record
        assertFalse(
            com.example.ui.viewmodel.canCheckoutAttendanceRecord(
                verifiedEmployeeId = "emp_colleague_2",
                verifiedEmployeeAuthUid = "uid_colleague_2",
                currentUserId = "uid_user_1",
                kioskMode = true,
                record = myOpenCheckIn,
                isManualBypass = false,
                role = UserRole.OWNER
            )
        )
        assertFalse(
            com.example.ui.viewmodel.canCheckoutAttendanceRecord(
                verifiedEmployeeId = null,
                currentUserId = "uid_user_1",
                kioskMode = false,
                record = myOpenCheckIn,
                isManualBypass = false,
                role = UserRole.OWNER
            )
        )

        // Only original owner face passes biometric checkout
        assertTrue(
            com.example.ui.viewmodel.canCheckoutAttendanceRecord(
                verifiedEmployeeId = "emp_user_1",
                verifiedEmployeeAuthUid = "uid_user_1",
                currentUserId = "uid_user_1",
                kioskMode = false,
                record = myOpenCheckIn,
                isManualBypass = false,
                role = UserRole.STAFF
            )
        )

        // Staff/Manager cannot manually bypass checkout; only Admin/Super Admin can when explicit bypass is requested
        assertFalse(
            com.example.ui.viewmodel.canCheckoutAttendanceRecord(
                verifiedEmployeeId = null,
                record = myOpenCheckIn,
                isManualBypass = true,
                role = UserRole.STAFF
            )
        )
        assertTrue(
            com.example.ui.viewmodel.canCheckoutAttendanceRecord(
                verifiedEmployeeId = null,
                record = myOpenCheckIn,
                isManualBypass = true,
                role = UserRole.ADMIN
            )
        )
    }

    @Test
    fun `amount in words converts zero, thousands, lakhs, and crores cleanly`() {
        assertEquals("Rupees Zero Only", PakistaniFormatters.amountInWords(0.0))
        assertEquals("Rupees Two Thousand Four Hundred Only", PakistaniFormatters.amountInWords(2400.0))
        assertEquals("Rupees One Lakh Twenty Five Thousand Only", PakistaniFormatters.amountInWords(125000.0))
        assertEquals("Rupees Two Crore Only", PakistaniFormatters.amountInWords(20_000_000.0))
        assertEquals("Rupees Minus Five Hundred Only", PakistaniFormatters.amountInWords(-500.0))
    }

    @Test
    fun `statement builder constructs customer ledger reconciling opening credit or recovery`() {
        val settings = com.example.data.model.FactorySettings(
            companyName = "Brohi Ice Factory",
            phone = "0300-1234567",
            address = "SITE Industrial Area"
        )
        val customer = com.example.data.model.Customer(
            id = "cust_99",
            name = "al madina traders",
            phone = "0333-1112233",
            balance = 5000.0
        )
        val sales = listOf(
            com.example.data.model.Sale(
                id = "s1",
                invoiceNumber = "BIF-5608",
                customerId = "cust_99",
                customerName = "Al Madina Traders",
                product = "Ice Blocks (10kg)",
                quantity = 2,
                unitPrice = 1200.0,
                total = 2400.0,
                paid = true,
                paidAmount = 2400.0,
                date = "2026-11-19"
            ),
            com.example.data.model.Sale(
                id = "s2",
                invoiceNumber = "BIF-5609",
                customerId = "cust_99",
                customerName = "Al Madina Traders",
                product = "Ice Blocks (10kg)",
                quantity = 3,
                unitPrice = 1200.0,
                total = 3600.0,
                paid = false,
                paidAmount = 0.0,
                date = "2026-11-20"
            )
        )

        val stmt = com.example.data.util.StatementBuilder.buildCustomerStatement(customer, sales, settings)
        assertEquals("CUSTOMER STATEMENT", stmt.title)
        assertEquals("CREDIT DUE", stmt.stampLabel)
        assertFalse(stmt.stampPositive)
        assertEquals("Al Madina Traders", stmt.partyName)
        assertEquals(5000.0, stmt.netAmount, 0.01)
        // Net from sales is 3600 due, customer balance is 5000 -> +1400 Opening Balance row reconciled
        assertEquals(3, stmt.rows.size)
        assertTrue(stmt.rows.any { it.referenceOrCategory == "OPEN-BAL" && it.primaryAmount == 1400.0 })
        assertEquals("Rupees Five Thousand Only", stmt.amountInWords)
    }

    @Test
    fun `statement builder constructs comprehensive financial statement with category filters`() {
        val settings = com.example.data.model.FactorySettings(companyName = "Brohi Ice Factory")
        val sales = listOf(
            com.example.data.model.Sale(
                id = "s1",
                invoiceNumber = "BIF-101",
                customerName = "Walk-in",
                product = "Ice Blocks",
                quantity = 5,
                unitPrice = 1000.0,
                total = 5000.0,
                paid = true,
                paidAmount = 5000.0,
                date = "2026-11-19"
            )
        )
        val expenses = listOf(
            com.example.data.model.Expense(
                id = "e1",
                description = "Diesel generator",
                amount = 1500.0,
                category = "Diesel",
                date = "2026-11-19",
                capitalInvestment = false
            )
        )
        val funding = listOf(
            com.example.data.model.FundingEntry(
                id = "f1",
                source = "Friend",
                name = "Haji Tariq",
                type = "Loan",
                amount = 100000.0,
                repaid = 20000.0,
                date = "2026-11-10"
            )
        )
        val utilities = listOf(
            com.example.data.model.UtilityReading(
                id = "u1",
                type = "Electricity",
                meterReading = 1200.0,
                unitsOrLiters = 50.0,
                amount = 1800.0,
                date = "2026-11-18"
            )
        )
        val payroll = listOf(
            PayrollRecord(
                id = "p1",
                employeeId = "emp1",
                employeeName = "Bilal",
                month = "2026-11",
                baseSalary = 25000.0,
                netPay = 25000.0,
                paid = true,
                paidDate = "2026-11-15"
            )
        )
        val suppliers = listOf(
            com.example.data.model.SupplierItem(
                id = "sup1",
                name = "K-Electric",
                itemSupplied = "Power",
                totalPurchases = 4000.0,
                dues = 500.0,
                lastPaymentDate = "2026-11-12"
            )
        )
        val summary = com.example.data.model.OwnerAccountingSummary(
            totalFundingRaised = 100000.0,
            stillToRepay = 80000.0,
            factoryInvestment = 0.0,
            totalSalesSoFar = 5000.0,
            creditPendingFromCustomers = 0.0,
            regularProfitSoFar = 3500.0
        )

        val allStmt = com.example.data.util.StatementBuilder.buildFinancialStatement(
            sales = sales,
            expenses = expenses,
            fundingList = funding,
            utilities = utilities,
            payroll = payroll,
            suppliers = suppliers,
            summary = summary,
            settings = settings,
            filterGroup = com.example.data.util.StatementFilterGroup.ALL
        )
        assertEquals("FINANCIAL STATEMENT", allStmt.title)
        assertEquals("PROFIT", allStmt.stampLabel)
        assertTrue(allStmt.stampPositive)
        assertEquals(6, allStmt.rows.size)
        assertEquals(5000.0, allStmt.summaryPrimaryValue, 0.01)
        assertEquals(1500.0, allStmt.summarySecondaryValue, 0.01)
        assertEquals(3500.0, allStmt.netAmount, 0.01)

        val revOnly = com.example.data.util.StatementBuilder.buildFinancialStatement(
            sales = sales,
            expenses = expenses,
            fundingList = funding,
            utilities = utilities,
            payroll = payroll,
            suppliers = suppliers,
            summary = summary,
            settings = settings,
            filterGroup = com.example.data.util.StatementFilterGroup.REVENUE
        )
        assertEquals(1, revOnly.rows.size)
        assertEquals(5000.0, revOnly.netAmount, 0.01)

        val fundPayOnly = com.example.data.util.StatementBuilder.buildFinancialStatement(
            sales = sales,
            expenses = expenses,
            fundingList = funding,
            utilities = utilities,
            payroll = payroll,
            suppliers = suppliers,
            summary = summary,
            settings = settings,
            filterGroup = com.example.data.util.StatementFilterGroup.FUNDING_PAYROLL
        )
        assertEquals(2, fundPayOnly.rows.size)
    }

    @Test
    fun `runtime diagnostic logger records errors warnings visits and caps buffer at 50`() {
        val logger = com.example.data.util.RuntimeDiagnosticLogger
        logger.clear()

        logger.recordScreenVisit("sales", "Sales & Invoicing", UserRole.OWNER)
        assertEquals("sales", logger.currentScreenKey)
        assertEquals("Sales & Invoicing", logger.currentScreenTitle)
        assertEquals(0, logger.getRecentErrorCount())

        logger.recordWarning("VoiceAI", "Timeout parsing voice command", screenKey = "sales")
        logger.recordError("FirestoreSync", "PERMISSION_DENIED on customers", screenKey = "customers")
        assertEquals(2, logger.getRecentErrorCount())
        assertTrue(logger.lastErrorEpochMillis > 0L)

        val summary = logger.buildDiagnosticsSummary("sales")
        assertTrue(summary.contains("VoiceAI"))
        assertTrue(summary.contains("PERMISSION_DENIED"))

        repeat(60) { idx ->
            logger.recordError("StressTest", "Error #$idx", screenKey = "attendance")
        }
        assertEquals(50, logger.getRecentEvents(100).size)
        logger.clear()
        assertEquals(0, logger.getRecentErrorCount())
    }

    @Test
    fun `ai audit serializer round trips findings with status severity and bilingual explanations`() {
        val sample = listOf(
            com.example.data.model.AiAuditFinding(
                id = "f1",
                screenKey = "attendance",
                screenTitle = "Attendance",
                issueTitle = "Check-In Button Clipped",
                severity = com.example.data.model.AuditSeverity.HIGH,
                category = com.example.data.model.AuditCategory.UI_LAYOUT,
                explanationEn = "The Check-In button label wraps awkwardly on narrow screens.",
                explanationUr = "چیک ان بٹن کا متن چھوٹی اسکرین پر کٹ رہا ہے۔",
                correctionPromptEn = "Update AttendanceScreen Check-In Button to use heightIn(min = 48.dp) and maxLines = 1.",
                status = com.example.data.model.AuditIssueStatus.PENDING,
                capturedAtMillis = 1700000100000L,
                sourceMode = "INSTANT_CHECK"
            ),
            com.example.data.model.AiAuditFinding(
                id = "f2",
                screenKey = "sales",
                screenTitle = "Sales & Invoicing",
                issueTitle = "Invoice Save Timeout Warning",
                severity = com.example.data.model.AuditSeverity.MEDIUM,
                category = com.example.data.model.AuditCategory.BROKEN_FEATURE,
                explanationEn = "Cloud write took over 15s during poor connectivity.",
                explanationUr = "کمزور انٹرنیٹ کی وجہ سے انوائس محفوظ کرنے میں تاخیر ہوئی۔",
                correctionPromptEn = "Add offline optimistic feedback banner in SalesInvoicingScreen when write queue is pending.",
                status = com.example.data.model.AuditIssueStatus.APPLIED,
                capturedAtMillis = 1700000200000L,
                sourceMode = "AUTO_WATCH"
            )
        )

        val encoded = com.example.data.model.AiAuditSerializer.encodeFindings(sample)
        val decoded = com.example.data.model.AiAuditSerializer.decodeFindings(encoded)

        assertEquals(2, decoded.size)
        assertEquals("f1", decoded[0].id)
        assertEquals(com.example.data.model.AuditSeverity.HIGH, decoded[0].severity)
        assertEquals(com.example.data.model.AuditIssueStatus.PENDING, decoded[0].status)
        assertEquals("چیک ان بٹن کا متن چھوٹی اسکرین پر کٹ رہا ہے۔", decoded[0].explanationUr)
        assertEquals(com.example.data.model.AuditIssueStatus.APPLIED, decoded[1].status)
        assertEquals(com.example.data.model.AuditCategory.BROKEN_FEATURE, decoded[1].category)
        assertTrue(com.example.data.model.AiAuditSerializer.decodeFindings("not-valid-json").isEmpty())
    }

    @Test
    fun `mergeAuditFindings deduplicates by screen and title and enforces max retained limit`() {
        val existing = listOf(
            com.example.data.model.AiAuditFinding(
                id = "old1",
                screenKey = "dashboard",
                screenTitle = "Brohi Ice Factory",
                issueTitle = "Low Contrast Subtitle",
                severity = com.example.data.model.AuditSeverity.LOW,
                category = com.example.data.model.AuditCategory.UI_LAYOUT,
                explanationEn = "Old",
                explanationUr = "پرانا",
                correctionPromptEn = "Fix contrast",
                status = com.example.data.model.AuditIssueStatus.IGNORED,
                capturedAtMillis = 1000L
            )
        )
        val incoming = listOf(
            com.example.data.model.AiAuditFinding(
                id = "dup1",
                screenKey = "DASHBOARD",
                screenTitle = "Brohi Ice Factory",
                issueTitle = " low contrast subtitle ",
                severity = com.example.data.model.AuditSeverity.MEDIUM,
                category = com.example.data.model.AuditCategory.UI_LAYOUT,
                explanationEn = "Duplicate",
                explanationUr = "نقل",
                correctionPromptEn = "Fix contrast",
                capturedAtMillis = 5000L
            ),
            com.example.data.model.AiAuditFinding(
                id = "new1",
                screenKey = "settings",
                screenTitle = "Settings",
                issueTitle = "Button Padding Tight",
                severity = com.example.data.model.AuditSeverity.MEDIUM,
                category = com.example.data.model.AuditCategory.UX_WORKFLOW,
                explanationEn = "Tight padding",
                explanationUr = "کم فاصلہ",
                correctionPromptEn = "Increase vertical spacing to 12.dp",
                capturedAtMillis = 9000L
            )
        )

        val merged = com.example.ui.viewmodel.mergeAuditFindings(existing, incoming, maxRetained = 2)
        assertEquals(2, merged.size)
        // Newest unique finding comes first; duplicate ignored so IGNORED status of old1 is preserved
        assertEquals("new1", merged[0].id)
        assertEquals("old1", merged[1].id)
        assertEquals(com.example.data.model.AuditIssueStatus.IGNORED, merged[1].status)
    }

    @Test
    fun `ai live watcher access is strictly restricted to approved super admin only`() {
        val approvedAdmin = com.example.data.model.UserProfile(
            userId = "a1",
            approvalStatus = "APPROVED",
            requestedRole = "Admin"
        )
        val approvedManager = com.example.data.model.UserProfile(
            userId = "m1",
            approvalStatus = "APPROVED",
            requestedRole = "Manager"
        )
        val approvedStaff = com.example.data.model.UserProfile(
            userId = "s1",
            approvalStatus = "APPROVED",
            requestedRole = "Staff"
        )
        val pendingSuperAdmin = com.example.data.model.UserProfile(
            userId = "o_pending",
            approvalStatus = "PENDING",
            requestedRole = "Super Admin"
        )
        val approvedSuperAdmin = com.example.data.model.UserProfile(
            userId = "o_ok",
            approvalStatus = "APPROVED",
            requestedRole = "Super Admin"
        )

        assertFalse(com.example.ui.viewmodel.canUserSwitchWorkspaceRole(null))
        assertFalse(com.example.ui.viewmodel.canUserSwitchWorkspaceRole(approvedAdmin))
        assertFalse(com.example.ui.viewmodel.canUserSwitchWorkspaceRole(approvedManager))
        assertFalse(com.example.ui.viewmodel.canUserSwitchWorkspaceRole(approvedStaff))
        assertFalse(com.example.ui.viewmodel.canUserSwitchWorkspaceRole(pendingSuperAdmin))
        assertTrue(com.example.ui.viewmodel.canUserSwitchWorkspaceRole(approvedSuperAdmin))
    }

    @Test
    fun `github version json and release api parsers extract build codes and apk url accurately`() {
        val versionJson = """
            {
              "latestVersionCode": 14,
              "latestVersionName": "1.1.12",
              "releaseNotes": "Added GitHub Auto-Update support",
              "releaseNotesUrdu": "گٹ ہب آٹو اپڈیٹ شامل کر دیا گیا",
              "apkDownloadUrl": "https://github.com/ahmeddirlis-glitch/Ice_Factory/releases/download/v1.1.12-b14/brohi-ice-factory-latest.apk",
              "mandatory": true,
              "publishedBy": "GitHub Auto-Release",
              "publishedAtIso": "2026-11-23"
            }
        """.trimIndent()
        val parsedVersion = com.example.data.util.AppUpdateManager.parseVersionJsonToConfig(versionJson)
        assertTrue(parsedVersion != null)
        assertEquals(14, parsedVersion!!.latestVersionCode)
        assertEquals("1.1.12", parsedVersion.latestVersionName)
        assertTrue(parsedVersion.mandatory)
        assertTrue(parsedVersion.apkDownloadUrl.contains("ahmeddirlis-glitch/Ice_Factory"))

        val ghApiJson = """
            {
              "tag_name": "v1.2.0-b25",
              "name": "Brohi Ice Factory v1.2.0 (Build 25)",
              "draft": false,
              "body": "Bug fixes and faster printing [mandatory]",
              "published_at": "2026-11-23T08:00:00Z",
              "assets": [
                {
                  "name": "brohi-ice-factory-latest.apk",
                  "browser_download_url": "https://github.com/ahmeddirlis-glitch/Ice_Factory/releases/download/v1.2.0-b25/brohi-ice-factory-latest.apk"
                }
              ]
            }
        """.trimIndent()
        val parsedApi = com.example.data.util.AppUpdateManager.parseGitHubApiReleaseToConfig(ghApiJson)
        assertTrue(parsedApi != null)
        assertEquals(25, parsedApi!!.latestVersionCode)
        assertEquals("1.2.0", parsedApi.latestVersionName)
        assertTrue(parsedApi.mandatory)
        assertEquals(
            "https://github.com/ahmeddirlis-glitch/Ice_Factory/releases/download/v1.2.0-b25/brohi-ice-factory-latest.apk",
            parsedApi.apkDownloadUrl
        )

        assertEquals(25, com.example.data.util.AppUpdateManager.extractBuildCodeFromTag("v1.2.0-b25"))
        assertEquals(10203, com.example.data.util.AppUpdateManager.extractBuildCodeFromTag("v1.2.3"))

        val selected = com.example.data.util.AppUpdateManager.selectLatestRelease(parsedApi, parsedVersion)
        assertEquals(25, selected?.latestVersionCode)
    }

    @Test
    fun `app updates route is accessible to all approved user roles`() {
        assertTrue(AppRoute.isAllowedFor(UserRole.OWNER, AppRoute.Updates.key))
        assertTrue(AppRoute.isAllowedFor(UserRole.ADMIN, AppRoute.Updates.key))
        assertTrue(AppRoute.isAllowedFor(UserRole.MANAGER, AppRoute.Updates.key))
        assertTrue(AppRoute.isAllowedFor(UserRole.STAFF, AppRoute.Updates.key))
        assertEquals(AppRoute.Updates, AppRoute.fromKey("updates"))
    }
}

