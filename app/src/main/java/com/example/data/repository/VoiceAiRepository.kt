package com.example.data.repository

import android.util.Log
import com.example.data.model.ParsedVoiceIntent
import com.example.data.model.UserRole
import com.example.data.util.PakistaniFormatters
import com.example.data.util.RuntimeDiagnosticLogger
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.withTimeout
import org.json.JSONObject
import java.util.Locale
import kotlin.coroutines.cancellation.CancellationException

private const val TAG = "VoiceAiRepository"

class VoiceAiRepository {

    private val voiceSchema = Schema.obj(
        properties = mapOf(
            "intents" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "intentType" to Schema.enumeration(
                            values = listOf(
                                "SALE",
                                "CREDIT_SALE",
                                "CREDIT_PAYMENT",
                                "EXPENSE",
                                "FUNDING",
                                "FACTORY_BUILD",
                                "PRODUCTION",
                                "NAVIGATE",
                                "QUERY"
                            ),
                            description = "Classified intent type"
                        ),
                        "category" to Schema.string(
                            description = "For EXPENSE: one of Electricity, Rent, Diesel, Water, Salary, Refreshment, Maintenance, Supplier, Other, Factory Build. For FUNDING: Friend, Bank, Investor, or Customer."
                        ),
                        "description" to Schema.string(
                            description = "Short clear description of the item"
                        ),
                        "amount" to Schema.double(
                            description = "Numeric PKR amount parsed from digits or Urdu/Hindi words (e.g. 2,00,000 -> 200000, 15 hazar -> 15000, 75,000 -> 75000, 50 lakh -> 5000000, 2 crore -> 20000000)"
                        ),
                        "tons" to Schema.double(
                            description = "Production tons if PRODUCTION intent"
                        ),
                        "customerOrSourceName" to Schema.string(
                            description = "Customer name for CREDIT_SALE/CREDIT_PAYMENT/SALE or lender/investor name for FUNDING"
                        ),
                        "targetScreen" to Schema.string(
                            description = "For NAVIGATE or QUERY: one of dashboard, users, attendance, face_verification, notices, production, inventory, sales, customers, deliveries, expenses, salary, utilities, maintenance, suppliers, incidents, reports, profile, settings, about"
                        ),
                        "queryType" to Schema.string(
                            description = "For QUERY: attendance_summary, salary_summary, notices_summary, profit_report, funding_breakeven, or general"
                        ),
                        "spokenReply" to Schema.string(
                            description = "Short natural confirmation or answer sentence to read aloud"
                        )
                    ),
                    optionalProperties = listOf(
                        "category",
                        "tons",
                        "customerOrSourceName",
                        "targetScreen",
                        "queryType"
                    )
                ),
                description = "One or more intents parsed from the user's voice transcript"
            )
        )
    )

    private val systemPrompt = content {
        text(
            """
            You are the voice parser for Brohi Ice Factory in Pakistan (currency PKR / Rs.).
            Users may speak mixed Urdu, Hindi, and English in Roman or native scripts, and Indian/Pakistani number formats:
            - Rs. 75,000 or 75000 or 75 hazar or 75 ہزار = 75000
            - 2,00,000 or 2 lakh = 200000
            - 50 lakh = 5000000
            - 2 crore = 20000000
            Return ONLY JSON matching the schema. Classify automatically without asking follow-up questions.
            Rules:
            1. Cash ice sale / income (e.g. "Aaj 15,000 ki baraf sale hui", "Rs. 25,000 cash sale") -> intentType=SALE, amount=15000, description="Ice sale".
            2. Credit/udhar ice sale (e.g. "20,000 ki baraf udhar bechi" or "Ali ko 20 hazar ki udhar baraf") -> intentType=CREDIT_SALE, amount=20000, customerOrSourceName (default "Credit Customer" if unnamed).
            3. Credit recovery / payment received on pending udhar (e.g. "Al-Madina ne 10,000 udhar wapas diya", "credit payment received 15000", "jo credit pe tha uska 12000 aaya", "wasooli 20,000") -> intentType=CREDIT_PAYMENT, amount=..., customerOrSourceName.
            4. Expenses: classify into Electricity (bijli, electricity, meter, wapda), Rent (kiraya, rent), Diesel (diesel, fuel), Water (pani bill, tanker), Salary (tankhwah, salary, advance), Refreshment (chai, pani ki bottle, nashta, food), Maintenance (repair, compressor, machine), Supplier, Other.
               Split multiple items in one sentence into separate intents (e.g. "2 chai 260 aur 2 pani ki bottle 800" -> TWO EXPENSE intents: Refreshment 260, Refreshment 800).
            5. Funding/loan (e.g. "Dost se 2 crore qarz liya") -> intentType=FUNDING, category="Friend" (or Bank/Investor/Customer), amount=20000000, customerOrSourceName="Friend".
            6. Factory build/setup capital (e.g. "Factory ki tameer par 50 lakh kharch hue") -> intentType=FACTORY_BUILD, category="Factory Build", amount=5000000.
            7. Production (e.g. "Production 12 ton") -> intentType=PRODUCTION, tons=12.0.
            8. Never create a profit entry; profit is calculated from sales and expenses.
            9. Navigation commands (e.g. "Open Salary", "Inventory kholo", "Sales dikhao") -> intentType=NAVIGATE with targetScreen.
            10. Questions:
               - "Show attendance" / "attendance dikhao" -> intentType=QUERY, queryType="attendance_summary", targetScreen="attendance"
               - "Show my salary" / "meri tankhwah" -> intentType=QUERY, queryType="salary_summary", targetScreen="salary"
               - "Any holiday or notice?" / "chutti hai?" / "notice dikhao" -> intentType=QUERY, queryType="notices_summary", targetScreen="notices"
               - "Report" / "This month's profit" / "is mahine ka profit" -> intentType=QUERY, queryType="profit_report", targetScreen="reports"
               - "Funding" / "When will I break even" / "break even kab hoga" -> intentType=QUERY, queryType="funding_breakeven", targetScreen="expenses"
            11. Strict Staff Access Control: If Active role is Staff, NEVER output SALE, CREDIT_SALE, CREDIT_PAYMENT, EXPENSE, FUNDING, FACTORY_BUILD, or PRODUCTION; only output NAVIGATE (to dashboard, attendance, face_verification, salary, notices, incidents, profile, about) or QUERY (attendance_summary, salary_summary, notices_summary).
            """.trimIndent()
        )
    }

    private val model by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = voiceSchema
            },
            systemInstruction = systemPrompt
        )
    }

    suspend fun parseTranscript(
        transcript: String,
        activeRole: UserRole,
        allowRemoteAi: Boolean = true
    ): Result<List<ParsedVoiceIntent>> {
        val cleaned = PakistaniFormatters.normalizeVoiceTranscript(transcript)
        if (cleaned.isBlank()) {
            return Result.failure(IllegalArgumentException("Please speak or type a command first."))
        }

        if (allowRemoteAi) {
            val aiResult = runCatching {
                val response = withTimeout(4_500L) {
                    model.generateContent(
                        "Active role: ${activeRole.label}. User transcript: \"$cleaned\""
                    )
                }
                val jsonText = response.text ?: throw IllegalStateException("Empty AI response")
                val root = JSONObject(jsonText)
                val intentsArr = root.getJSONArray("intents")
                val results = mutableListOf<ParsedVoiceIntent>()
                for (i in 0 until intentsArr.length()) {
                    val obj = intentsArr.getJSONObject(i)
                    val parsedAmt = obj.optDouble("amount", 0.0).takeIf { !it.isNaN() && it > 0.0 } ?: 0.0
                    val parsedTons = obj.optDouble("tons", 0.0).takeIf { !it.isNaN() && it > 0.0 } ?: 0.0
                    results.add(
                        ParsedVoiceIntent(
                            intentType = obj.optString("intentType", "EXPENSE"),
                            category = obj.optString("category", ""),
                            description = obj.optString("description", cleaned),
                            amount = parsedAmt,
                            tons = parsedTons,
                            customerOrSourceName = obj.optString("customerOrSourceName", ""),
                            targetScreen = obj.optString("targetScreen", ""),
                            queryType = obj.optString("queryType", ""),
                            spokenReply = obj.optString("spokenReply", "")
                        )
                    )
                }
                results
            }

            aiResult.onSuccess { list ->
                if (list.isNotEmpty()) {
                    return Result.success(list)
                }
            }.onFailure { err ->
                if (err is CancellationException) throw err
                Log.w(TAG, "Gemini voice parser unavailable or timed out, using instant local classifier: ${err.message}")
                RuntimeDiagnosticLogger.recordWarning(
                    source = "VoiceAI",
                    message = "Remote Gemini voice parser fallback: ${err.localizedMessage ?: err.message ?: "timeout"}"
                )
            }
        }

        val localIntents = parseTranscriptLocally(cleaned, activeRole)
        return if (localIntents.isNotEmpty()) {
            Result.success(localIntents)
        } else {
            Result.failure(
                IllegalStateException("Could not recognize an amount or command. Try e.g. 'Rs. 75,000 electricity bill' or 'Rs. 15,000 baraf sale'.")
            )
        }
    }

    internal fun parseTranscriptLocally(
        transcript: String,
        activeRole: UserRole
    ): List<ParsedVoiceIntent> {
        val normalized = PakistaniFormatters.normalizeVoiceTranscript(transcript)
        if (normalized.isBlank()) return emptyList()

        val rawClauses = normalized
            .split(Regex("\\s+(?:aur|and|&|اور)\\s+|\\n|؛", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        val clauses = if (rawClauses.size > 1 && rawClauses.count { extractAmount(it) > 0 || extractTons(it) > 0 } >= 2) {
            rawClauses
        } else {
            listOf(normalized)
        }

        val intents = mutableListOf<ParsedVoiceIntent>()
        for (clause in clauses) {
            parseSingleClause(clause, activeRole)?.let { intents.add(it) }
        }
        return intents
    }

    private fun parseSingleClause(clause: String, activeRole: UserRole): ParsedVoiceIntent? {
        val lower = clause.lowercase(Locale.US)
        val amount = extractAmount(clause)
        val tons = extractTons(clause)

        // Strict Staff Self-Service Scope
        if (activeRole == UserRole.STAFF) {
            when {
                containsAny(lower, "notice", "chutti", "holiday", "announc", "اعلان", "چھٹی") -> {
                    return ParsedVoiceIntent(
                        intentType = "QUERY",
                        queryType = "notices_summary",
                        targetScreen = "notices",
                        description = clause
                    )
                }
                containsAny(lower, "attendance", "hazri", "حاضری") -> {
                    return ParsedVoiceIntent(
                        intentType = "QUERY",
                        queryType = "attendance_summary",
                        targetScreen = "attendance",
                        description = clause
                    )
                }
                containsAny(lower, "salary", "tankhwah", "pay", "slip", "تنخواہ") && amount <= 0.0 -> {
                    return ParsedVoiceIntent(
                        intentType = "QUERY",
                        queryType = "salary_summary",
                        targetScreen = "salary",
                        description = clause
                    )
                }
                containsAny(lower, "incident", "complaint", "shikayat", "safety", "شکایت") -> {
                    return ParsedVoiceIntent(
                        intentType = "NAVIGATE",
                        targetScreen = "incidents",
                        description = clause,
                        spokenReply = "Opening safety complaints."
                    )
                }
                containsAny(lower, "profile", "id card", "پروفائل") -> {
                    return ParsedVoiceIntent(
                        intentType = "NAVIGATE",
                        targetScreen = "profile",
                        description = clause,
                        spokenReply = "Opening your profile."
                    )
                }
            }
            val navTarget = detectNavigationTarget(lower)
            if (navTarget != null && containsAny(lower, "open", "show", "go to", "kholo", "dikhao", "کھولو", "دکھاؤ")) {
                return ParsedVoiceIntent(
                    intentType = "NAVIGATE",
                    targetScreen = navTarget,
                    description = clause,
                    spokenReply = "Opening ${navTarget.replace('_', ' ')}."
                )
            }
            if (amount > 0.0 || tons > 0.0 || containsAny(lower, "sale", "expense", "kharch", "production", "udhar", "profit")) {
                return ParsedVoiceIntent(
                    intentType = "QUERY",
                    queryType = "staff_denied",
                    description = clause,
                    spokenReply = "Restricted: Only Super Admin or Admin can record sales, expenses, or production. Staff can say Attendance, Salary, or Notices."
                )
            }
            return null
        }

        // 1. Queries without explicit transaction amounts
        if (amount <= 0.0 && tons <= 0.0) {
            when {
                containsAny(lower, "notice", "chutti", "holiday", "announc", "اعلان", "چھٹی") -> {
                    return ParsedVoiceIntent(
                        intentType = "QUERY",
                        queryType = "notices_summary",
                        targetScreen = "notices",
                        description = clause
                    )
                }
                containsAny(lower, "attendance", "hazri", "حاضری") -> {
                    return ParsedVoiceIntent(
                        intentType = "QUERY",
                        queryType = "attendance_summary",
                        targetScreen = "attendance",
                        description = clause
                    )
                }
                containsAny(lower, "salary", "tankhwah", "تنخواہ") && !containsAny(lower, "open", "kholo") -> {
                    return ParsedVoiceIntent(
                        intentType = "QUERY",
                        queryType = "salary_summary",
                        targetScreen = "salary",
                        description = clause
                    )
                }
                containsAny(lower, "break even", "breakeven", "funding", "recovery") -> {
                    return ParsedVoiceIntent(
                        intentType = "QUERY",
                        queryType = "funding_breakeven",
                        targetScreen = "expenses",
                        description = clause
                    )
                }
                containsAny(lower, "profit", "report", "munafa", "منافع") -> {
                    return ParsedVoiceIntent(
                        intentType = "QUERY",
                        queryType = "profit_report",
                        targetScreen = "reports",
                        description = clause
                    )
                }
            }

            // Navigation intents
            val navTarget = detectNavigationTarget(lower)
            if (navTarget != null && containsAny(lower, "open", "show", "go to", "kholo", "dikhao", "کھولو", "دکھاؤ")) {
                return ParsedVoiceIntent(
                    intentType = "NAVIGATE",
                    targetScreen = navTarget,
                    description = clause,
                    spokenReply = "Opening ${navTarget.replace('_', ' ')}."
                )
            }
            return null
        }

        // 2. Production in Tons
        if (tons > 0.0 && containsAny(lower, "production", "produced", "ton", "tons", "ٹن", "بنی", "تیار")) {
            return ParsedVoiceIntent(
                intentType = "PRODUCTION",
                category = "Production",
                description = clause,
                tons = tons
            )
        }

        if (amount <= 0.0) return null

        // 3. Factory Build / Construction Capital
        if (containsAny(lower, "tameer", "construction", "factory build", "plant setup", "تعمیر")) {
            return ParsedVoiceIntent(
                intentType = "FACTORY_BUILD",
                category = "Factory Build",
                description = clause,
                amount = amount
            )
        }

        // 4. Funding / Loan
        if (containsAny(lower, "qarz", "loan", "funding", "investment", "investor", "قرض", "سرمایہ")) {
            val sourceCat = when {
                containsAny(lower, "bank", "بینک") -> "Bank"
                containsAny(lower, "investor", "partner") -> "Investor"
                else -> "Friend"
            }
            return ParsedVoiceIntent(
                intentType = "FUNDING",
                category = sourceCat,
                description = clause,
                amount = amount,
                customerOrSourceName = sourceCat
            )
        }

        // 5. Credit Payment / Recovery (udhar wapas aaya / credit received / wasooli)
        val mentionsCredit = containsAny(lower, "udhar", "udhaar", "credit", "bakaya", "baqaya", "ادھار", "بقایا")
        val mentionsReturn = containsAny(
            lower,
            "wapas", "wapis", "aaya", "aya", "received", "receive", "recover", "recovery",
            "wasool", "wasooli", "mila", "mili", "diya", "diye", "paid back", "wapsi",
            "وصول", "واپس", "ملا", "آیا", "دیا"
        )
        val hasSaleWord = containsAny(lower, "bechi", "becha", "sale", "sold", "frokht", "فروخت", "بیچی")
        if ((mentionsCredit && mentionsReturn && !hasSaleWord) ||
            containsAny(lower, "wasooli", "credit recovery", "credit payment", "وصولی")
        ) {
            return ParsedVoiceIntent(
                intentType = "CREDIT_PAYMENT",
                category = "Credit Recovery",
                description = clause,
                amount = amount,
                customerOrSourceName = extractPersonOrCustomerName(clause)
            )
        }

        // 6. Credit Sale (udhar sale)
        if (mentionsCredit) {
            return ParsedVoiceIntent(
                intentType = "CREDIT_SALE",
                category = "Credit Sale",
                description = clause,
                amount = amount,
                customerOrSourceName = extractPersonOrCustomerName(clause).ifBlank { "Credit Customer" }
            )
        }

        // 7. Explicit Expense Category Detection first (so e.g. "electricity bill" never gets confused)
        val expenseCategory = detectExpenseCategory(lower)
        if (expenseCategory != null) {
            return ParsedVoiceIntent(
                intentType = "EXPENSE",
                category = expenseCategory,
                description = clause,
                amount = amount
            )
        }

        // 8. Cash Sale / Income
        if (containsAny(
                lower,
                "sale", "sold", "income", "amdani", "bechi", "becha", "bik", "bikri", "revenue",
                "baraf", "ice", "block", "blocks", "cube", "سیل", "آمدنی", "برف", "فروخت", "بکری"
            )
        ) {
            return ParsedVoiceIntent(
                intentType = "SALE",
                category = "Ice Sale",
                description = clause,
                amount = amount,
                customerOrSourceName = extractPersonOrCustomerName(clause).ifBlank { "Cash Customer" }
            )
        }

        // 9. Explicit Expense keywords without specific category -> Expense (Other)
        if (containsAny(lower, "expense", "kharch", "kharcha", "bill", "paid", "buy", "kharid", "خرچ", "خرچہ", "بل")) {
            return ParsedVoiceIntent(
                intentType = "EXPENSE",
                category = "Other",
                description = clause,
                amount = amount
            )
        }

        // Default fallback when an amount is spoken -> Expense (Other) unless sale keyword present
        return ParsedVoiceIntent(
            intentType = "EXPENSE",
            category = "Other",
            description = clause,
            amount = amount
        )
    }

    private fun detectExpenseCategory(lower: String): String? = when {
        containsAny(lower, "electricity", "bijli", "wapda", "lesco", "ke ", "بجلی") -> "Electricity"
        containsAny(lower, "diesel", "petrol", "fuel", "generator", "ڈیزل", "پٹرول") -> "Diesel"
        containsAny(lower, "water", "tanker", "pani ka bill", "پانی کا بل") -> "Water"
        containsAny(lower, "rent", "kiraya", "کرایہ") -> "Rent"
        containsAny(lower, "chai", "tea", "nashta", "khana", "food", "refreshment", "pani", "bottle", "چائے", "کھانا", "پانی") -> "Refreshment"
        containsAny(lower, "maintenance", "repair", "compressor", "motor", "oil", "gas", "service", "مرمت") -> "Maintenance"
        containsAny(lower, "salary", "tankhwah", "pay", "advance", "mazdoori", "mazdoor", "labour", "labor", "تنخواہ", "مزدوری") -> "Salary"
        containsAny(lower, "supplier", "vendor") -> "Supplier"
        else -> null
    }

    private fun detectNavigationTarget(lower: String): String? = when {
        containsAny(lower, "dashboard", "home") -> "dashboard"
        containsAny(lower, "user", "staff", "role", "approval") -> "users"
        containsAny(lower, "attendance", "hazri", "حاضری") -> "attendance"
        containsAny(lower, "face") -> "face_verification"
        containsAny(lower, "notice", "holiday", "chutti", "announc", "اعلان", "چھٹی") -> "notices"
        containsAny(lower, "production") -> "production"
        containsAny(lower, "inventory", "stock", "cold storage") -> "inventory"
        containsAny(lower, "sale", "invoice") -> "sales"
        containsAny(lower, "customer", "credit", "udhar") -> "customers"
        containsAny(lower, "deliver", "vehicle") -> "deliveries"
        containsAny(lower, "expense", "finance", "kharch") -> "expenses"
        containsAny(lower, "salary", "payroll", "tankhwah", "تنخواہ") -> "salary"
        containsAny(lower, "utilit", "electricity", "diesel") -> "utilities"
        containsAny(lower, "maintenance", "machine") -> "maintenance"
        containsAny(lower, "supplier") -> "suppliers"
        containsAny(lower, "incident", "safety", "complaint", "شکایت") -> "incidents"
        containsAny(lower, "report", "analytic") -> "reports"
        containsAny(lower, "profile", "id card", "پروفائل") -> "profile"
        containsAny(lower, "setting") -> "settings"
        containsAny(lower, "about") -> "about"
        else -> null
    }

    private fun extractAmount(clause: String): Double {
        // Explicit "Rs. X" matches highest priority
        val rsMatches = Regex("Rs\\.?\\s*([0-9][0-9,]*)", RegexOption.IGNORE_CASE)
            .findAll(clause)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .filter { it > 0 }
            .toList()
        if (rsMatches.isNotEmpty()) {
            return rsMatches.last()
        }

        // Multiplier matches if any weren't already normalized
        val multMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(hazar|hazaar|thousand|lakh|lac|crore|karor|ہزار|لاکھ|کروڑ)", RegexOption.IGNORE_CASE)
            .find(clause)
        if (multMatch != null) {
            val base = multMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val mult = when (multMatch.groupValues[2].lowercase(Locale.US)) {
                "hazar", "hazaar", "thousand", "ہزار" -> 1_000.0
                "lakh", "lac", "لاکھ" -> 100_000.0
                "crore", "karor", "کروڑ" -> 10_000_000.0
                else -> 1.0
            }
            if (base * mult > 0) return base * mult
        }

        // Fallback: take the largest plain number that isn't a small item count before a product like "2 chai"
        val nums = Regex("\\b([0-9][0-9,]*)\\b")
            .findAll(clause)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .filter { it > 0 }
            .toList()
        return nums.maxOrNull() ?: 0.0
    }

    private fun extractTons(clause: String): Double {
        val match = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:ton|tons|ٹن)", RegexOption.IGNORE_CASE).find(clause)
        return match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    private fun extractPersonOrCustomerName(clause: String): String {
        val named = Regex("([A-Za-z][A-Za-z0-9\\s-]{1,24}?)\\s+(?:ne|ko|ka|ki|from|to)\\b", RegexOption.IGNORE_CASE)
            .find(clause)
            ?.groupValues?.get(1)
            ?.trim()
        val ignoreWords = setOf("aaj", "today", "mein", "rs", "record", "add", "credit", "udhar", "cash")
        return if (!named.isNullOrBlank() && named.lowercase(Locale.US) !in ignoreWords) named else ""
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }
}
