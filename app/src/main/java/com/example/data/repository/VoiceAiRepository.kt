package com.example.data.repository

import android.util.Log
import com.example.data.model.ParsedVoiceIntent
import com.example.data.model.UserRole
import com.example.data.model.VoiceAssistantResult
import com.example.data.model.VoiceConversationTurn
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
import kotlin.math.roundToLong

private const val TAG = "VoiceAiRepository"

data class WakePhraseMatch(
    val matched: Boolean,
    val trailingCommand: String = ""
)

class VoiceAiRepository {

    private val intentItemSchema = Schema.obj(
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
                description = "For EXPENSE: Electricity, Rent, Diesel, Water, Salary, Refreshment, Maintenance, Supplier, Other, Factory Build. For FUNDING: Friend, Bank, Investor, Customer."
            ),
            "description" to Schema.string(
                description = "Short clear description of the item"
            ),
            "amount" to Schema.double(
                description = "Total numeric PKR amount (never multiply blocks * rate on top of total when total is already stated)"
            ),
            "quantity" to Schema.integer(
                description = "Number of ice blocks sold (for SALE or CREDIT_SALE), or 0 if not stated"
            ),
            "unitPrice" to Schema.double(
                description = "Calculated price per ice block (amount / quantity) when quantity > 0"
            ),
            "tons" to Schema.double(
                description = "Production tons if PRODUCTION intent"
            ),
            "customerOrSourceName" to Schema.string(
                description = "Customer name for CREDIT_SALE/CREDIT_PAYMENT/SALE or lender/investor name for FUNDING"
            ),
            "targetScreen" to Schema.string(
                description = "For NAVIGATE or QUERY: one of dashboard, users, attendance, face_verification, notices, production, inventory, sales, customers, deliveries, expenses, salary, utilities, maintenance, suppliers, incidents, reports, profile, settings, about, updates"
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
            "quantity",
            "unitPrice",
            "tons",
            "customerOrSourceName",
            "targetScreen",
            "queryType"
        )
    )

    private val voiceSchema = Schema.obj(
        properties = mapOf(
            "intents" to Schema.array(
                items = intentItemSchema,
                description = "One or more intents parsed from the user's voice transcript"
            )
        )
    )

    private val assistantConversationSchema = Schema.obj(
        properties = mapOf(
            "mode" to Schema.enumeration(
                values = listOf("CLARIFY", "ANSWER", "NAVIGATE", "EXECUTE", "DENIED"),
                description = "CLARIFY if required info like block count or amount is missing; ANSWER for Q&A or chat; NAVIGATE to open a screen; EXECUTE when transaction intents are complete and ready; DENIED if role lacks permission."
            ),
            "replyLanguage" to Schema.enumeration(
                values = listOf("en", "hi", "ur"),
                description = "Reply language matching the user's spoken language: en (English), hi (Hindi), or ur (Urdu)."
            ),
            "spokenReply" to Schema.string(
                description = "Natural, concise response to speak aloud in replyLanguage (Urdu script for ur, Hindi Devanagari for hi, English for en)."
            ),
            "missingField" to Schema.string(
                description = "If mode is CLARIFY: 'blocks', 'amount', 'category', or 'customer'; otherwise empty string."
            ),
            "intents" to Schema.array(
                items = intentItemSchema,
                description = "Draft or completed intents. Populate partial known fields during CLARIFY so the app retains amount/type across turns."
            )
        ),
        optionalProperties = listOf("missingField")
    )

    private val systemPrompt = content {
        text(
            """
            You are the voice parser for Brohi Ice Factory in Pakistan (currency PKR / Rs.).
            Users may speak mixed Urdu, Hindi, and English in Roman or native scripts, and Indian/Pakistani number formats:
            - Rs. 75,000 or 75000 or 75 hazar or 75 ہزار or 75 हजार = 75000
            - 2,00,000 or 2 lakh = 200000
            - 50 lakh = 5000000
            - 2 crore = 20000000
            Return ONLY JSON matching the schema. Classify automatically without asking follow-up questions.
            Rules:
            1. Cash ice sale / income (e.g. "Aaj 35,000 ki baraf sale hui aur 15 blocks sale hue") -> intentType=SALE, amount=35000, quantity=15, unitPrice=2333.33, description="Ice sale (15 blocks)".
            2. Credit/udhar ice sale -> intentType=CREDIT_SALE, amount=..., quantity=..., customerOrSourceName.
            3. Credit recovery / wasooli -> intentType=CREDIT_PAYMENT, amount=..., customerOrSourceName.
            4. Expenses: classify into Electricity, Rent, Diesel, Water, Salary, Refreshment, Maintenance, Supplier, Other.
            5. Funding/loan -> intentType=FUNDING.
            6. Factory build -> intentType=FACTORY_BUILD.
            7. Production -> intentType=PRODUCTION, tons=...
            8. Strict Staff Access Control: If Active role is Staff, NEVER output SALE, CREDIT_SALE, CREDIT_PAYMENT, EXPENSE, FUNDING, FACTORY_BUILD, or PRODUCTION.
            """.trimIndent()
        )
    }

    private val conversationalSystemPrompt = content {
        text(
            """
            You are Brohi Ice AI, an intelligent, friendly, trilingual voice assistant for Brohi Ice Factory (Pakistan, PKR currency).
            You understand and speak English ('en'), Hindi ('hi' in Devanagari script), and Urdu ('ur' in Urdu script) fluently.

            CORE CONVERSATION & CLARIFICATION RULES:
            1. Match the user's language naturally:
               - If user speaks English -> replyLanguage="en" and write spokenReply in clear English.
               - If user speaks Hindi -> replyLanguage="hi" and write spokenReply in clear Hindi (Devanagari).
               - If user speaks Urdu or Roman Urdu -> replyLanguage="ur" and write spokenReply in clear Urdu script.
               - Speak naturally and concisely (1-2 sentences) so Android Text-to-Speech reads it aloud smoothly.
            2. Smart Ice Sale Total vs. Blocks Rule (CRITICAL):
               - If user gives BOTH total revenue AND block count (e.g., "Today 35,000 sale happened and 15 blocks were sold"):
                 * Do NOT multiply blocks by default rate and do NOT add them together.
                 * Keep total amount = 35000, quantity = 15, unitPrice = 35000 / 15 = 2333.33.
                 * Set mode="EXECUTE".
               - If user mentions an ice SALE or CREDIT_SALE with total amount > 0 (e.g., "Aaj 35 hazar ki ice sale hui" / "Today 35,000 ice was sold") WITHOUT mentioning how many blocks were sold (and no pending draft already has blocks):
                 * Set mode="CLARIFY", missingField="blocks".
                 * Include the partial draft intent in intents (amount=35000, quantity=0, intentType="SALE").
                 * Ask clearly in the user's language how many ice blocks were sold (e.g., "35,000 روپے کی برف کی سیل نوٹ کر لی ہے۔ کتنے بلاکس سیل ہوئے؟" or "Noted Rs. 35,000 ice sale. How many blocks were sold?").
               - If user mentions block count (e.g., "15 blocks ice sold") WITHOUT mentioning the amount:
                 * Set mode="CLARIFY", missingField="amount".
                 * Include partial draft (quantity=15, amount=0, intentType="SALE") and ask for the total PKR amount.
               - If there IS a pending draft awaiting clarification (provided in context) and the user replies with a number or detail (e.g., "15", "15 blocks", "10 بلاکس", or "default rate"):
                 * Merge it with the pending draft! Compute unitPrice = amount / quantity (or if amount was missing and user says "default rate", use quantity * defaultIceBlockRate), and return mode="EXECUTE" with the completed intent!
            3. Vague Expense Rule:
               - If user gives an expense amount with no item/category (e.g., "35,000 kharch hue"), set mode="CLARIFY", missingField="category", keep partial EXPENSE draft (amount=35000), and ask what it was spent on.
               - If user specifies the item (e.g., "75,000 bijli ka bill", "2 chai 260 aur 2 pani 800"), return mode="EXECUTE" immediately.
            4. Live Q&A & Greeting:
               - Use the provided Live Factory Context to answer business/attendance/salary/notice/profit/stock/credit questions in mode="ANSWER".
               - Greet the user by name warmly when greeted (e.g. "Salam" / "Hello" / "Hey Ice").
            5. Strict Role Access Control:
               - If activeRole is Staff, NEVER allow SALE, CREDIT_SALE, CREDIT_PAYMENT, EXPENSE, FUNDING, FACTORY_BUILD, PRODUCTION, or profit/funding answers. Return mode="DENIED" politely explaining Staff can check Attendance, Salary, Notices, Safety Complaints, or Profile.
               - Only Super Admin (Owner) can record FUNDING or FACTORY_BUILD or view profit/break-even reports.
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

    private val conversationalModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = assistantConversationSchema
            },
            systemInstruction = conversationalSystemPrompt
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
                val response = withTimeout(30_000L) {
                    model.generateContent(
                        "Active role: ${activeRole.label}. User transcript: \"$cleaned\""
                    )
                }
                val jsonText = response.text ?: throw IllegalStateException("Empty AI response")
                val root = JSONObject(jsonText)
                parseIntentArray(root.optJSONArray("intents"), cleaned)
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
                IllegalStateException("Could not recognize an amount or command. Try e.g. 'Rs. 75,000 electricity bill' or 'Rs. 35,000 sale 15 blocks'.")
            )
        }
    }

    /**
     * Full trilingual conversational assistant entry point with multi-turn clarification,
     * Gemini 3.8 Flash online reasoning, and seamless offline conversational fallback.
     */
    suspend fun converseWithAssistant(
        transcript: String,
        activeRole: UserRole,
        userName: String,
        preferredLocaleTag: String,
        defaultBlockRate: Double,
        factoryContextSummary: String,
        history: List<VoiceConversationTurn>,
        pendingDraft: ParsedVoiceIntent?,
        awaitingField: String,
        allowRemoteAi: Boolean = true
    ): VoiceAssistantResult {
        val cleaned = PakistaniFormatters.normalizeVoiceTranscript(transcript)
        val detectedLang = detectLanguage(transcript, preferredLocaleTag)
        if (cleaned.isBlank()) {
            return VoiceAssistantResult(
                mode = "CLARIFY",
                replyLanguage = detectedLang,
                spokenReply = localizedPrompt(detectedLang, "empty"),
                shouldReopenMic = true
            )
        }

        // Handle explicit cancel during clarification
        val lower = cleaned.lowercase(Locale.US)
        if (pendingDraft != null && containsAny(lower, "cancel", "chhodo", "band", "mat karo", "رہنے دو", "منسوخ", "नहीं", "रद्द")) {
            return VoiceAssistantResult(
                mode = "ANSWER",
                replyLanguage = detectedLang,
                spokenReply = when (detectedLang) {
                    "ur" -> "ٹھیک ہے، یہ انٹری منسوخ کر دی گئی ہے۔ کوئی اور حکم بتائیں؟"
                    "hi" -> "ठीक है, यह एंट्री रद्द कर दी गई है। कोई और काम बताएं?"
                    else -> "Okay, cancelled that entry. How else can I help?"
                },
                pendingDraft = null,
                missingField = "",
                shouldReopenMic = false
            )
        }

        if (allowRemoteAi) {
            val aiResult = runCatching {
                val historyText = history.takeLast(8).joinToString("\n") { turn ->
                    val speaker = if (turn.isUser) "User" else "Assistant"
                    "$speaker: ${turn.text}"
                }
                val draftText = if (pendingDraft != null) {
                    "Pending Draft awaiting '$awaitingField': intentType=${pendingDraft.intentType}, amount=${pendingDraft.amount}, quantity=${pendingDraft.quantity}, category='${pendingDraft.category}', customer='${pendingDraft.customerOrSourceName}', description='${pendingDraft.description}'"
                } else {
                    "Pending Draft: None"
                }
                val prompt = """
                    User Name: $userName
                    Active Role: ${activeRole.label}
                    UI Selected Locale: $preferredLocaleTag (detected input language hint: $detectedLang)
                    Default Ice Block Rate: Rs. ${defaultBlockRate.toLong()}
                    Live Factory Context:
                    $factoryContextSummary

                    Recent Conversation:
                    $historyText

                    $draftText

                    Latest User Utterance (normalized): "$cleaned"
                    Latest User Utterance (raw): "$transcript"
                """.trimIndent()

                val response = withTimeout(30_000L) {
                    conversationalModel.generateContent(prompt)
                }
                val jsonText = response.text ?: throw IllegalStateException("Empty Gemini conversational response")
                val root = JSONObject(jsonText)
                val mode = root.optString("mode", "ANSWER")
                val replyLang = root.optString("replyLanguage", detectedLang).ifBlank { detectedLang }
                val spokenReply = root.optString("spokenReply", "")
                val missing = root.optString("missingField", "")
                val intents = parseIntentArray(root.optJSONArray("intents"), cleaned, replyLang)
                    .map { normalizeSaleQuantities(it) }

                val enforced = enforceRoleSecurity(
                    mode = mode,
                    replyLanguage = replyLang,
                    spokenReply = spokenReply,
                    missingField = missing,
                    intents = intents,
                    activeRole = activeRole
                )
                enforced
            }

            aiResult.onSuccess { res ->
                return res
            }.onFailure { err ->
                if (err is CancellationException) throw err
                Log.w(TAG, "Gemini conversational assistant fallback: ${err.message}")
                RuntimeDiagnosticLogger.recordWarning(
                    source = "VoiceAI",
                    message = "Conversational AI fallback: ${err.localizedMessage ?: err.message ?: "error"}"
                )
            }
        }

        return evaluateConversationLocally(
            rawTranscript = transcript,
            normalizedTranscript = cleaned,
            activeRole = activeRole,
            userName = userName,
            preferredLocaleTag = preferredLocaleTag,
            defaultBlockRate = defaultBlockRate,
            pendingDraft = pendingDraft,
            awaitingField = awaitingField
        )
    }

    internal fun evaluateConversationLocally(
        rawTranscript: String,
        normalizedTranscript: String = PakistaniFormatters.normalizeVoiceTranscript(rawTranscript),
        activeRole: UserRole,
        userName: String = "",
        preferredLocaleTag: String = "ur-PK",
        defaultBlockRate: Double = 1200.0,
        pendingDraft: ParsedVoiceIntent? = null,
        awaitingField: String = ""
    ): VoiceAssistantResult {
        val lang = detectLanguage(rawTranscript, preferredLocaleTag)
        val lower = normalizedTranscript.lowercase(Locale.US)

        // 1. If we are waiting for clarification on a pending draft (e.g., how many blocks?)
        if (pendingDraft != null && awaitingField.isNotBlank()) {
            when (awaitingField) {
                "blocks" -> {
                    val useDefault = containsAny(lower, "default", "rate", "auto", "skip", "نارمل", "عام", "सामान्य")
                    val blocks = if (useDefault && pendingDraft.amount > 0) {
                        (pendingDraft.amount / defaultBlockRate.coerceAtLeast(1.0)).roundToLong().coerceAtLeast(1L)
                    } else {
                        extractBlockCount(normalizedTranscript, allowBareNumber = true)
                    }
                    if (blocks > 0L) {
                        val completed = normalizeSaleQuantities(
                            pendingDraft.copy(
                                quantity = blocks,
                                description = "${pendingDraft.description.ifBlank { "Ice sale" }} ($blocks blocks)",
                                missingField = "",
                                detectedLang = lang
                            )
                        )
                        val unitRounded = completed.unitPrice.roundToLong()
                        val amtStr = PakistaniFormatters.formatPkr(completed.amount)
                        val reply = when (lang) {
                            "ur" -> "شکریہ! $blocks بلاکس کی کل $amtStr سیل (تقریباً $unitRounded روپے فی بلاک) تیار ہے۔ براہِ کرم تصدیق کریں۔"
                            "hi" -> "धन्यवाद! $blocks ब्लॉक की कुल $amtStr सेल (लगभग $unitRounded रुपये प्रति ब्लॉक) तैयार है। कृपया पुष्टि करें।"
                            else -> "Got it! $blocks blocks for $amtStr (about Rs. $unitRounded per block). Ready to confirm."
                        }
                        return VoiceAssistantResult(
                            mode = "EXECUTE",
                            replyLanguage = lang,
                            spokenReply = reply,
                            intents = listOf(completed),
                            pendingDraft = null,
                            missingField = "",
                            shouldReopenMic = false
                        )
                    } else {
                        val reask = when (lang) {
                            "ur" -> "معاف کیجیے، بلاکس کی تعداد سمجھ نہیں آئی۔ براہِ کرم بتائیں کتنے بلاکس سیل ہوئے؟ جیسے 15 بلاکس۔"
                            "hi" -> "माफ़ कीजिए, ब्लॉक की संख्या समझ नहीं आई। कृपया बताएं कितने ब्लॉक सेल हुए? जैसे 15 ब्लॉक।"
                            else -> "Sorry, I didn't catch the block count. How many ice blocks were sold? For example, 15 blocks."
                        }
                        return VoiceAssistantResult(
                            mode = "CLARIFY",
                            replyLanguage = lang,
                            spokenReply = reask,
                            pendingDraft = pendingDraft,
                            missingField = "blocks",
                            shouldReopenMic = true
                        )
                    }
                }
                "amount" -> {
                    val amt = extractAmount(normalizedTranscript)
                    if (amt > 0.0) {
                        val completed = normalizeSaleQuantities(
                            pendingDraft.copy(
                                amount = amt,
                                missingField = "",
                                detectedLang = lang
                            )
                        )
                        val amtStr = PakistaniFormatters.formatPkr(completed.amount)
                        val qty = completed.quantity
                        val reply = when (lang) {
                            "ur" -> "درست ہے! $qty بلاکس کی کل رقم $amtStr درج کی جا رہی ہے۔"
                            "hi" -> "ठीक है! $qty ब्लॉक की कुल राशि $amtStr दर्ज की जा रही है।"
                            else -> "Noted! $qty blocks totaling $amtStr. Ready to confirm."
                        }
                        return VoiceAssistantResult(
                            mode = "EXECUTE",
                            replyLanguage = lang,
                            spokenReply = reply,
                            intents = listOf(completed),
                            pendingDraft = null,
                            missingField = "",
                            shouldReopenMic = false
                        )
                    } else {
                        return VoiceAssistantResult(
                            mode = "CLARIFY",
                            replyLanguage = lang,
                            spokenReply = localizedPrompt(lang, "ask_amount"),
                            pendingDraft = pendingDraft,
                            missingField = "amount",
                            shouldReopenMic = true
                        )
                    }
                }
                "category" -> {
                    val detectedCat = detectExpenseCategory(lower)
                        ?: normalizedTranscript.trim().take(28).replaceFirstChar { it.uppercase() }
                    val completed = pendingDraft.copy(
                        category = detectedCat,
                        description = "${pendingDraft.description} - $detectedCat",
                        missingField = "",
                        detectedLang = lang
                    )
                    val amtStr = PakistaniFormatters.formatPkr(completed.amount)
                    val reply = when (lang) {
                        "ur" -> "ٹھیک ہے، $detectedCat کا $amtStr خرچہ تیار ہے۔ تصدیق کریں۔"
                        "hi" -> "ठीक है, $detectedCat का $amtStr खर्च तैयार है। कृपया पुष्टि करें।"
                        else -> "Got it, $detectedCat expense of $amtStr ready to confirm."
                    }
                    return VoiceAssistantResult(
                        mode = "EXECUTE",
                        replyLanguage = lang,
                        spokenReply = reply,
                        intents = listOf(completed),
                        pendingDraft = null,
                        missingField = "",
                        shouldReopenMic = false
                    )
                }
            }
        }

        // 2. Greeting check
        if (containsAny(lower, "salam", "assalam", "hello", "hi ", "hey", "namaste", "سلام", "नमस्ते") &&
            extractAmount(normalizedTranscript) <= 0.0 &&
            extractBlockCount(normalizedTranscript) <= 0L
        ) {
            val firstName = userName.trim().substringBefore(" ").ifBlank { userName.ifBlank { "Friend" } }
            val greeting = when (lang) {
                "ur" -> "وعلیکم السلام $firstName! بتائیں آج کیا مدد کروں؟ سیل، خرچہ یا کوئی بھی سوال پوچھ سکتے ہیں۔"
                "hi" -> "नमस्ते $firstName! बताइए आज क्या मदद करूँ? आप सेल, खर्च या कोई भी सवाल पूछ सकते हैं।"
                else -> "Hello $firstName! How can I help you today? You can tell me a sale, expense, or ask any question."
            }
            return VoiceAssistantResult(
                mode = "ANSWER",
                replyLanguage = lang,
                spokenReply = greeting,
                shouldReopenMic = true
            )
        }

        // 3. Parse raw intents locally
        val parsed = parseTranscriptLocally(normalizedTranscript, activeRole)

        // Handle block-only sale (user said "15 blocks ice sold" without Rs amount)
        val blocksWithoutAmount = extractBlockCount(normalizedTranscript)
        if (parsed.isEmpty() && blocksWithoutAmount > 0L &&
            containsAny(lower, "sale", "sold", "bech", "baraf", "ice", "سیل", "برف", "बर्फ", "बिक", "बेच")
        ) {
            if (activeRole == UserRole.STAFF) {
                return staffRestrictedResult(lang)
            }
            val isCredit = containsAny(lower, "udhar", "udhaar", "credit", "ادھار", "उधार")
            val draft = ParsedVoiceIntent(
                intentType = if (isCredit) "CREDIT_SALE" else "SALE",
                category = if (isCredit) "Credit Sale" else "Ice Sale",
                description = normalizedTranscript,
                amount = 0.0,
                quantity = blocksWithoutAmount,
                customerOrSourceName = extractPersonOrCustomerName(normalizedTranscript),
                missingField = "amount",
                detectedLang = lang
            )
            val ask = when (lang) {
                "ur" -> "$blocksWithoutAmount برف کے بلاکس نوٹ کر لیے ہیں۔ کل کتنے روپے کی سیل ہوئی؟"
                "hi" -> "$blocksWithoutAmount बर्फ के ब्लॉक नोट कर लिए हैं। कुल कितने रुपये की सेल हुई?"
                else -> "Noted $blocksWithoutAmount ice blocks sold. What was the total sale amount in rupees?"
            }
            return VoiceAssistantResult(
                mode = "CLARIFY",
                replyLanguage = lang,
                spokenReply = ask,
                intents = listOf(draft),
                pendingDraft = draft,
                missingField = "amount",
                shouldReopenMic = true
            )
        }

        if (parsed.isEmpty()) {
            val clarifyMsg = when (lang) {
                "ur" -> "مجھے پوری بات واضح نہیں ہوئی۔ مثلاً بولیں: آج 35 ہزار کی برف سیل ہوئی اور 15 بلاکس بکے، یا بجلی کا بل بتائیں۔"
                "hi" -> "मुझे पूरी बात स्पष्ट नहीं हुई। जैसे बोलें: आज 35 हजार की बर्फ सेल हुई और 15 ब्लॉक बिके, या बिजली का बिल बताएं।"
                else -> "I didn't quite catch that. Try saying e.g. 'Today 35,000 sale and 15 blocks sold' or '75,000 electricity bill'."
            }
            return VoiceAssistantResult(
                mode = "CLARIFY",
                replyLanguage = lang,
                spokenReply = clarifyMsg,
                shouldReopenMic = true
            )
        }

        val first = parsed.first()
        if (parsed.size == 1 && first.intentType == "NAVIGATE") {
            val target = first.targetScreen.ifBlank { "dashboard" }
            if (!com.example.ui.navigation.AppRoute.isAllowedFor(activeRole, target)) {
                return VoiceAssistantResult(
                    mode = "DENIED",
                    replyLanguage = lang,
                    spokenReply = when (lang) {
                        "ur" -> "معاف کیجیے، آپ کے رول کو یہ سیکشن کھولنے کی اجازت نہیں ہے۔"
                        "hi" -> "माफ़ कीजिए, आपके रोल को यह सेक्शन खोलने की अनुमति नहीं है।"
                        else -> "Access denied: That module is restricted for ${activeRole.label}."
                    }
                )
            }
            val openMsg = when (lang) {
                "ur" -> "${target.replace('_', ' ')} کھول رہا ہوں۔"
                "hi" -> "${target.replace('_', ' ')} खोल रहा हूँ।"
                else -> first.spokenReply.ifBlank { "Opening ${target.replace('_', ' ')}." }
            }
            return VoiceAssistantResult(
                mode = "NAVIGATE",
                replyLanguage = lang,
                spokenReply = openMsg,
                intents = listOf(first.copy(detectedLang = lang))
            )
        }

        if (parsed.size == 1 && first.intentType == "QUERY") {
            if (first.queryType == "staff_denied") {
                return staffRestrictedResult(lang)
            }
            return VoiceAssistantResult(
                mode = "ANSWER",
                replyLanguage = lang,
                spokenReply = first.spokenReply,
                intents = listOf(first.copy(detectedLang = lang))
            )
        }

        // Security gate for financial writes
        val blockedByOwner = parsed.any {
            !activeRole.isOwner && (it.intentType == "FUNDING" || it.intentType == "FACTORY_BUILD" ||
                it.category.equals("Factory Build", ignoreCase = true))
        }
        if (activeRole == UserRole.STAFF) return staffRestrictedResult(lang)
        if (blockedByOwner) {
            return VoiceAssistantResult(
                mode = "DENIED",
                replyLanguage = lang,
                spokenReply = when (lang) {
                    "ur" -> "معاف کیجیے، فنڈنگ اور فیکٹری تعمیر کا اندراج صرف سپر ایڈمن کر سکتے ہیں۔"
                    "hi" -> "माफ़ कीजिए, फंडिंग और फैक्ट्री निर्माण केवल सुपर एडमिन दर्ज कर सकते हैं।"
                    else -> "Access restricted: Funding and Factory Build entries are available to Super Admin only."
                }
            )
        }

        // Check if single ice SALE / CREDIT_SALE has amount > 0 but NO blocks mentioned -> ask for block count!
        if (parsed.size == 1 && (first.intentType == "SALE" || first.intentType == "CREDIT_SALE") && first.amount > 0.0 && first.quantity <= 0L) {
            val amtStr = PakistaniFormatters.formatPkr(first.amount)
            val draft = first.copy(missingField = "blocks", detectedLang = lang)
            val ask = when (lang) {
                "ur" -> "$amtStr کی برف کی سیل نوٹ کر لی ہے۔ کتنے بلاکس سیل ہوئے ہیں؟"
                "hi" -> "$amtStr की बर्फ की सेल नोट कर ली है। कितने ब्लॉक सेल हुए हैं?"
                else -> "Noted $amtStr ice sale. How many ice blocks were sold?"
            }
            return VoiceAssistantResult(
                mode = "CLARIFY",
                replyLanguage = lang,
                spokenReply = ask,
                intents = listOf(draft),
                pendingDraft = draft,
                missingField = "blocks",
                shouldReopenMic = true
            )
        }

        // Check if single vague expense has no specific category -> clarify
        if (parsed.size == 1 && first.intentType == "EXPENSE" && first.category == "Other" &&
            !containsAny(lower, "other", "misc", "bill", "خرچ", "खर्च")
        ) {
            val amtStr = PakistaniFormatters.formatPkr(first.amount)
            val draft = first.copy(missingField = "category", detectedLang = lang)
            val ask = when (lang) {
                "ur" -> "$amtStr کا خرچہ نوٹ کر لیا ہے۔ یہ کس کام پر خرچ ہوا؟ مثلاً بجلی، ڈیزل یا مرمت؟"
                "hi" -> "$amtStr का खर्च नोट कर लिया है। यह किस काम पर खर्च हुआ? जैसे बिजली, डीजल या मरम्मत?"
                else -> "Noted $amtStr expense. What was it for? For example Electricity, Diesel, or Maintenance."
            }
            return VoiceAssistantResult(
                mode = "CLARIFY",
                replyLanguage = lang,
                spokenReply = ask,
                intents = listOf(draft),
                pendingDraft = draft,
                missingField = "category",
                shouldReopenMic = true
            )
        }

        val finalized = parsed.map { normalizeSaleQuantities(it.copy(detectedLang = lang)) }
        val spokenConfirmation = buildLocalizedExecutionReply(finalized, lang)
        return VoiceAssistantResult(
            mode = "EXECUTE",
            replyLanguage = lang,
            spokenReply = spokenConfirmation,
            intents = finalized,
            shouldReopenMic = false
        )
    }

    private fun enforceRoleSecurity(
        mode: String,
        replyLanguage: String,
        spokenReply: String,
        missingField: String,
        intents: List<ParsedVoiceIntent>,
        activeRole: UserRole
    ): VoiceAssistantResult {
        if (activeRole == UserRole.STAFF) {
            val hasForbiddenWrite = intents.any {
                it.intentType in listOf("SALE", "CREDIT_SALE", "CREDIT_PAYMENT", "EXPENSE", "FUNDING", "FACTORY_BUILD", "PRODUCTION")
            }
            val hasForbiddenScreen = intents.any {
                it.targetScreen.isNotBlank() && !com.example.ui.navigation.AppRoute.isAllowedFor(activeRole, it.targetScreen)
            }
            if (hasForbiddenWrite || hasForbiddenScreen) {
                return staffRestrictedResult(replyLanguage)
            }
        } else if (!activeRole.isOwner) {
            val ownerOnly = intents.any {
                it.intentType == "FUNDING" || it.intentType == "FACTORY_BUILD" ||
                    it.category.equals("Factory Build", ignoreCase = true) ||
                    it.queryType in listOf("profit_report", "funding_breakeven")
            }
            if (ownerOnly) {
                return VoiceAssistantResult(
                    mode = "DENIED",
                    replyLanguage = replyLanguage,
                    spokenReply = when (replyLanguage) {
                        "ur" -> "معاف کیجیے، یہ معلومات صرف سپر ایڈمن کے لیے دستیاب ہیں۔"
                        "hi" -> "माफ़ कीजिए, यह जानकारी केवल सुपर एडमिन के लिए उपलब्ध है।"
                        else -> "Access restricted: This action is available to Super Admin only."
                    }
                )
            }
        }

        return VoiceAssistantResult(
            mode = mode,
            replyLanguage = replyLanguage,
            spokenReply = spokenReply.ifBlank { buildLocalizedExecutionReply(intents, replyLanguage) },
            intents = intents,
            missingField = missingField,
            pendingDraft = if (mode == "CLARIFY") intents.firstOrNull() else null,
            shouldReopenMic = (mode == "CLARIFY")
        )
    }

    private fun staffRestrictedResult(lang: String): VoiceAssistantResult = VoiceAssistantResult(
        mode = "DENIED",
        replyLanguage = lang,
        spokenReply = when (lang) {
            "ur" -> "معاف کیجیے، اسٹاف سیل، خرچ یا پیداوار درج نہیں کر سکتا۔ آپ حاضری، تنخواہ یا نوٹس چیک کر سکتے ہیں۔"
            "hi" -> "माफ़ कीजिए, स्टाफ सेल, खर्च या प्रोडक्शन दर्ज नहीं कर सकता। आप हाज़िरी, सैलरी या नोटिस देख सकते हैं।"
            else -> "Restricted: Only Super Admin or Admin can record sales, expenses, or production. Staff can check Attendance, Salary, or Notices."
        }
    )

    private fun buildLocalizedExecutionReply(intents: List<ParsedVoiceIntent>, lang: String): String {
        val summary = intents.joinToString(". ") { intent ->
            val amt = if (intent.amount > 0) PakistaniFormatters.formatPkr(intent.amount) else ""
            when (intent.intentType) {
                "SALE" -> when (lang) {
                    "ur" -> if (intent.quantity > 0) "${intent.quantity} بلاکس کی نقد سیل $amt" else "نقد برف سیل $amt"
                    "hi" -> if (intent.quantity > 0) "${intent.quantity} ब्लॉक की नकद सेल $amt" else "नकद बर्फ सेल $amt"
                    else -> if (intent.quantity > 0) "Cash sale of ${intent.quantity} blocks for $amt" else "Cash ice sale of $amt"
                }
                "CREDIT_SALE" -> when (lang) {
                    "ur" -> if (intent.quantity > 0) "${intent.customerOrSourceName} کو ${intent.quantity} بلاکس ادھار سیل $amt" else "ادھار سیل $amt"
                    "hi" -> if (intent.quantity > 0) "${intent.customerOrSourceName} को ${intent.quantity} ब्लॉक उधार सेल $amt" else "उधार सेल $amt"
                    else -> if (intent.quantity > 0) "Credit sale of ${intent.quantity} blocks to ${intent.customerOrSourceName} for $amt" else "Credit sale of $amt"
                }
                "CREDIT_PAYMENT" -> when (lang) {
                    "ur" -> "${intent.customerOrSourceName.ifBlank { "کسٹمر" }} سے $amt وصولی"
                    "hi" -> "${intent.customerOrSourceName.ifBlank { "ग्राहक" }} से $amt वसूली"
                    else -> "Credit recovery of $amt from ${intent.customerOrSourceName.ifBlank { "Customer" }}"
                }
                "EXPENSE" -> when (lang) {
                    "ur" -> "${intent.category.ifBlank { "دیگر" }} خرچہ $amt"
                    "hi" -> "${intent.category.ifBlank { "अन्य" }} खर्च $amt"
                    else -> "${intent.category.ifBlank { "Other" }} expense of $amt"
                }
                "PRODUCTION" -> when (lang) {
                    "ur" -> "پیداوار ${PakistaniFormatters.formatTons(intent.tons)}"
                    "hi" -> "प्रोडक्शन ${PakistaniFormatters.formatTons(intent.tons)}"
                    else -> "Production ${PakistaniFormatters.formatTons(intent.tons)}"
                }
                else -> intent.description
            }
        }
        return when (lang) {
            "ur" -> "$summary۔ براہِ کرم تصدیق کریں۔"
            "hi" -> "$summary। कृपया पुष्टि करें।"
            else -> "$summary. Please confirm."
        }
    }

    private fun localizedPrompt(lang: String, key: String): String = when (key) {
        "ask_amount" -> when (lang) {
            "ur" -> "براہِ کرم کل رقم روپے میں بتائیں؟ مثلاً 35 ہزار۔"
            "hi" -> "कृपया कुल राशि रुपये में बताएं? जैसे 35 हजार।"
            else -> "Please tell me the total amount in rupees, for example 35,000."
        }
        else -> when (lang) {
            "ur" -> "جی میں سن رہا ہوں، براہِ کرم بولیں۔"
            "hi" -> "जी मैं सुन रहा हूँ, कृपया बोलें।"
            else -> "I'm listening, please speak."
        }
    }

    private fun parseIntentArray(
        intentsArr: org.json.JSONArray?,
        fallbackDescription: String,
        detectedLang: String = "en"
    ): List<ParsedVoiceIntent> {
        if (intentsArr == null) return emptyList()
        val results = mutableListOf<ParsedVoiceIntent>()
        for (i in 0 until intentsArr.length()) {
            val obj = intentsArr.getJSONObject(i)
            val parsedAmt = obj.optDouble("amount", 0.0).takeIf { !it.isNaN() && it > 0.0 } ?: 0.0
            val parsedQty = obj.optLong("quantity", 0L).coerceAtLeast(0L)
            val parsedUnit = obj.optDouble("unitPrice", 0.0).takeIf { !it.isNaN() && it > 0.0 } ?: 0.0
            val parsedTons = obj.optDouble("tons", 0.0).takeIf { !it.isNaN() && it > 0.0 } ?: 0.0
            results.add(
                normalizeSaleQuantities(
                    ParsedVoiceIntent(
                        intentType = obj.optString("intentType", "EXPENSE"),
                        category = obj.optString("category", ""),
                        description = obj.optString("description", fallbackDescription),
                        amount = parsedAmt,
                        quantity = parsedQty,
                        unitPrice = parsedUnit,
                        tons = parsedTons,
                        customerOrSourceName = obj.optString("customerOrSourceName", ""),
                        targetScreen = obj.optString("targetScreen", ""),
                        queryType = obj.optString("queryType", ""),
                        spokenReply = obj.optString("spokenReply", ""),
                        detectedLang = detectedLang
                    )
                )
            )
        }
        return results
    }

    private fun normalizeSaleQuantities(intent: ParsedVoiceIntent): ParsedVoiceIntent {
        if ((intent.intentType == "SALE" || intent.intentType == "CREDIT_SALE") &&
            intent.quantity > 0L && intent.amount > 0.0
        ) {
            val computedUnit = intent.amount / intent.quantity.toDouble()
            return intent.copy(unitPrice = computedUnit)
        }
        return intent
    }

    internal fun parseTranscriptLocally(
        transcript: String,
        activeRole: UserRole
    ): List<ParsedVoiceIntent> {
        val normalized = PakistaniFormatters.normalizeVoiceTranscript(transcript)
        if (normalized.isBlank()) return emptyList()

        val rawClauses = normalized
            .split(Regex("\\s+(?:aur|and|&|اور|और)\\s+|\\n|؛", RegexOption.IGNORE_CASE))
            .map { it.trim() }
            .filter { it.isNotBlank() }

        // Only split into separate clauses when multiple clauses each represent independent financial items (not "35,000 sale aur 15 blocks")
        val independentItemClauses = rawClauses.count { c ->
            val hasAmt = extractAmount(c) > 0
            val hasTons = extractTons(c) > 0
            hasAmt || hasTons
        }
        val clauses = if (rawClauses.size > 1 && independentItemClauses >= 2) {
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
        val blocks = extractBlockCount(clause)

        // Strict Staff Self-Service Scope
        if (activeRole == UserRole.STAFF) {
            when {
                containsAny(lower, "notice", "chutti", "holiday", "announc", "اعلان", "چھٹی", "छुट्टी", "नोटिस") -> {
                    return ParsedVoiceIntent(
                        intentType = "QUERY",
                        queryType = "notices_summary",
                        targetScreen = "notices",
                        description = clause
                    )
                }
                containsAny(lower, "attendance", "hazri", "حاضری", "हाजिरी", "हाज़िरी") -> {
                    return ParsedVoiceIntent(
                        intentType = "QUERY",
                        queryType = "attendance_summary",
                        targetScreen = "attendance",
                        description = clause
                    )
                }
                containsAny(lower, "salary", "tankhwah", "pay", "slip", "تنخواہ", "सैलरी", "तनख्वाह") && amount <= 0.0 -> {
                    return ParsedVoiceIntent(
                        intentType = "QUERY",
                        queryType = "salary_summary",
                        targetScreen = "salary",
                        description = clause
                    )
                }
                containsAny(lower, "incident", "complaint", "shikayat", "safety", "شکایت", "शिकायत") -> {
                    return ParsedVoiceIntent(
                        intentType = "NAVIGATE",
                        targetScreen = "incidents",
                        description = clause,
                        spokenReply = "Opening safety complaints."
                    )
                }
                containsAny(lower, "profile", "id card", "پروفائل", "प्रोफाइल") -> {
                    return ParsedVoiceIntent(
                        intentType = "NAVIGATE",
                        targetScreen = "profile",
                        description = clause,
                        spokenReply = "Opening your profile."
                    )
                }
            }
            val navTarget = detectNavigationTarget(lower)
            if (navTarget != null && containsAny(lower, "open", "show", "go to", "kholo", "dikhao", "کھولو", "دکھاؤ", "खोलो", "दिखाओ")) {
                return ParsedVoiceIntent(
                    intentType = "NAVIGATE",
                    targetScreen = navTarget,
                    description = clause,
                    spokenReply = "Opening ${navTarget.replace('_', ' ')}."
                )
            }
            if (amount > 0.0 || tons > 0.0 || blocks > 0L || containsAny(lower, "sale", "expense", "kharch", "production", "udhar", "profit")) {
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
                containsAny(lower, "notice", "chutti", "holiday", "announc", "اعلان", "چھٹی", "छुट्टी", "नोटिस") -> {
                    return ParsedVoiceIntent(
                        intentType = "QUERY",
                        queryType = "notices_summary",
                        targetScreen = "notices",
                        description = clause
                    )
                }
                containsAny(lower, "attendance", "hazri", "حاضری", "हाजिरी", "हाज़िरी") -> {
                    return ParsedVoiceIntent(
                        intentType = "QUERY",
                        queryType = "attendance_summary",
                        targetScreen = "attendance",
                        description = clause
                    )
                }
                containsAny(lower, "salary", "tankhwah", "تنخواہ", "तनख्वाह", "सैलरी") && !containsAny(lower, "open", "kholo", "खोलो") -> {
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
                containsAny(lower, "profit", "report", "munafa", "منافع", "मुनाफा", "रिपोर्ट") -> {
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
            if (navTarget != null && containsAny(lower, "open", "show", "go to", "kholo", "dikhao", "کھولو", "دکھاؤ", "खोलो", "दिखाओ")) {
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
        if (tons > 0.0 && containsAny(lower, "production", "produced", "ton", "tons", "ٹن", "بنی", "تیار", "टन")) {
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
        if (containsAny(lower, "qarz", "loan", "funding", "investment", "investor", "قرض", "سرمایہ", "कर्ज")) {
            val sourceCat = when {
                containsAny(lower, "bank", "بینک", "बैंक") -> "Bank"
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
        val mentionsCredit = containsAny(lower, "udhar", "udhaar", "credit", "bakaya", "baqaya", "ادھار", "بقایا", "उधार", "बकाया")
        val mentionsReturn = containsAny(
            lower,
            "wapas", "wapis", "aaya", "aya", "received", "receive", "recover", "recovery",
            "wasool", "wasooli", "mila", "mili", "diya", "diye", "paid back", "wapsi",
            "وصول", "واپس", "ملا", "آیا", "دیا", "वापस", "मिला", "वसूल"
        )
        val hasSaleWord = containsAny(lower, "bechi", "becha", "sale", "sold", "frokht", "فروخت", "بیچی", "सेल", "बेची")
        if ((mentionsCredit && mentionsReturn && !hasSaleWord) ||
            containsAny(lower, "wasooli", "credit recovery", "credit payment", "وصولی", "वसूली")
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
            return normalizeSaleQuantities(
                ParsedVoiceIntent(
                    intentType = "CREDIT_SALE",
                    category = "Credit Sale",
                    description = clause,
                    amount = amount,
                    quantity = blocks,
                    customerOrSourceName = extractPersonOrCustomerName(clause).ifBlank { "Credit Customer" }
                )
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

        // 8. Cash Sale / Income (supports blocks + total amount together cleanly!)
        if (containsAny(
                lower,
                "sale", "sold", "income", "amdani", "bechi", "becha", "bik", "bikri", "bika", "bike", "revenue",
                "baraf", "ice", "block", "blocks", "cube", "سیل", "آمدنی", "برف", "فروخت", "بکری", "بلاکس", "بلاک",
                "सेल", "बर्फ", "ब्लॉक", "बिके", "बिकी"
            )
        ) {
            return normalizeSaleQuantities(
                ParsedVoiceIntent(
                    intentType = "SALE",
                    category = "Ice Sale",
                    description = clause,
                    amount = amount,
                    quantity = blocks,
                    customerOrSourceName = extractPersonOrCustomerName(clause).ifBlank { "Cash Customer" }
                )
            )
        }

        // 9. Explicit Expense keywords without specific category -> Expense (Other)
        if (containsAny(lower, "expense", "kharch", "kharcha", "bill", "paid", "buy", "kharid", "خرچ", "خرچہ", "بل", "खर्च", "बिल")) {
            return ParsedVoiceIntent(
                intentType = "EXPENSE",
                category = "Other",
                description = clause,
                amount = amount
            )
        }

        // Default fallback when an amount is spoken -> Expense (Other)
        return ParsedVoiceIntent(
            intentType = "EXPENSE",
            category = "Other",
            description = clause,
            amount = amount
        )
    }

    private fun detectExpenseCategory(lower: String): String? = when {
        containsAny(lower, "electricity", "bijli", "wapda", "lesco", "ke ", "بجلی", "बिजली") -> "Electricity"
        containsAny(lower, "diesel", "petrol", "fuel", "generator", "ڈیزل", "پٹرول", "डीजल", "पेट्रोल") -> "Diesel"
        containsAny(lower, "water", "tanker", "pani ka bill", "پانی کا بل", "पानी का बिल") -> "Water"
        containsAny(lower, "rent", "kiraya", "کرایہ", "किराया") -> "Rent"
        containsAny(lower, "chai", "tea", "nashta", "khana", "food", "refreshment", "pani", "bottle", "چائے", "کھانا", "پانی", "चाय", "नाश्ता", "पानी") -> "Refreshment"
        containsAny(lower, "maintenance", "repair", "compressor", "motor", "oil", "gas", "service", "مرمت", "मरम्मत") -> "Maintenance"
        containsAny(lower, "salary", "tankhwah", "pay", "advance", "mazdoori", "mazdoor", "labour", "labor", "تنخواہ", "مزدوری", "तनख्वाह", "मजदूरी") -> "Salary"
        containsAny(lower, "supplier", "vendor") -> "Supplier"
        else -> null
    }

    private fun detectNavigationTarget(lower: String): String? = when {
        containsAny(lower, "dashboard", "home") -> "dashboard"
        containsAny(lower, "user", "staff", "role", "approval") -> "users"
        containsAny(lower, "attendance", "hazri", "حاضری", "हाजिरी") -> "attendance"
        containsAny(lower, "face") -> "face_verification"
        containsAny(lower, "notice", "holiday", "chutti", "announc", "اعلان", "چھٹی", "नोटिस") -> "notices"
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
        containsAny(lower, "incident", "safety", "complaint", "شکایت", "शिकायत") -> "incidents"
        containsAny(lower, "report", "analytic") -> "reports"
        containsAny(lower, "profile", "id card", "پروفائل", "प्रोफाइल") -> "profile"
        containsAny(lower, "update", "version") -> "updates"
        containsAny(lower, "setting") -> "settings"
        containsAny(lower, "about") -> "about"
        else -> null
    }

    private fun extractAmount(clause: String): Double {
        val rsMatches = Regex("Rs\\.?\\s*([0-9][0-9,]*)", RegexOption.IGNORE_CASE)
            .findAll(clause)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .filter { it > 0 }
            .toList()
        if (rsMatches.isNotEmpty()) {
            return rsMatches.last()
        }

        val multMatch = Regex("(\\d+(?:\\.\\d+)?)\\s*(hazar|hazaar|thousand|lakh|lac|crore|karor|ہزار|لاکھ|کروڑ|हज़ार|हजार|लाख|करोड़)", RegexOption.IGNORE_CASE)
            .find(clause)
        if (multMatch != null) {
            val base = multMatch.groupValues[1].toDoubleOrNull() ?: 0.0
            val mult = when (multMatch.groupValues[2].lowercase(Locale.US)) {
                "hazar", "hazaar", "thousand", "ہزار", "हज़ार", "हजार" -> 1_000.0
                "lakh", "lac", "لاکھ", "लाख" -> 100_000.0
                "crore", "karor", "کروڑ", "करोड़" -> 10_000_000.0
                else -> 1.0
            }
            if (base * mult > 0) return base * mult
        }

        // Ignore block count numbers when scanning fallback plain numbers
        val withoutBlocks = clause.replace(
            Regex("\\b\\d+\\s*(?:block|blocks|بلاک|بلاکس|ब्लॉक|ब्लॉक्स|pcs|piece|pieces)\\b", RegexOption.IGNORE_CASE),
            " "
        )
        val nums = Regex("\\b([0-9][0-9,]*)\\b")
            .findAll(withoutBlocks)
            .mapNotNull { it.groupValues[1].replace(",", "").toDoubleOrNull() }
            .filter { it > 0 }
            .toList()
        return nums.maxOrNull() ?: 0.0
    }

    internal fun extractBlockCount(clause: String, allowBareNumber: Boolean = false): Long {
        val normalizedDigits = PakistaniFormatters.convertUrduDigits(clause)
        val blockMatch = Regex(
            "(\\d+)\\s*(?:ice\\s+)?(?:block|blocks|بلاک|بلاکس|ब्लॉक|ब्लॉक्स|pcs|piece|pieces)\\b",
            RegexOption.IGNORE_CASE
        ).find(normalizedDigits)
        if (blockMatch != null) {
            return blockMatch.groupValues[1].toLongOrNull()?.coerceAtLeast(0L) ?: 0L
        }
        if (allowBareNumber) {
            val wordMatch = Regex(
                "\\b(aik|ek|do|teen|chaar|char|paanch|panch|chay|cheh|saat|sat|aath|ath|nau|das|gyarah|barah|terah|chaudah|pandrah|solah|satrah|atharah|unnees|bees|pachis|pachees|tees|paintees|chalis|pachaas|pachas|saath|sattar|assi|nabbe|sau|ایک|دو|تین|چار|پانچ|چھ|سات|آٹھ|نو|دس|گیارہ|بارہ|تیرہ|چودہ|پندرہ|سولہ|سترہ|اٹھارہ|انیس|بیس|پچیس|تیس|پینتیس|چالیس|پچاس|ساٹھ|ستر|اسی|نوے|سو|एक|दो|तीन|चार|पांच|पाँच|छह|सात|आठ|नौ|दस|ग्यारह|बारह|तेरह|चौदह|पंद्रह|सोलह|सत्रह|अठारह|उन्नीस|बीस|पच्चीस|तीस|पैंतीस|चालीस|पचास|साठ|सत्तर|अस्सी|नब्बे|सौ)\\b",
                RegexOption.IGNORE_CASE
            ).find(normalizedDigits)
            if (wordMatch != null) {
                val mapped = mapNumberWordToLong(wordMatch.groupValues[1].lowercase(Locale.US))
                if (mapped > 0L) return mapped
            }
            val bare = Regex("\\b(\\d{1,5})\\b").find(normalizedDigits)
            val valNum = bare?.groupValues?.get(1)?.toLongOrNull() ?: 0L
            if (valNum in 1L..50_000L) return valNum
        }
        return 0L
    }

    private fun mapNumberWordToLong(word: String): Long = when (word) {
        "aik", "ek", "ایک", "एक" -> 1L
        "do", "دو", "दो" -> 2L
        "teen", "تین", "तीन" -> 3L
        "chaar", "char", "چار", "चार" -> 4L
        "paanch", "panch", "پانچ", "पांच", "पाँच" -> 5L
        "chay", "cheh", "چھ", "छह" -> 6L
        "saat", "sat", "سات", "सात" -> 7L
        "aath", "ath", "آٹھ", "आठ" -> 8L
        "nau", "نو", "नौ" -> 9L
        "das", "دس", "दस" -> 10L
        "gyarah", "گیارہ", "ग्यारह" -> 11L
        "barah", "بارہ", "बारह" -> 12L
        "terah", "تیرہ", "तेरह" -> 13L
        "chaudah", "چودہ", "चौदह" -> 14L
        "pandrah", "پندرہ", "पंद्रह" -> 15L
        "solah", "سولہ", "सोलह" -> 16L
        "satrah", "سترہ", "सत्रह" -> 17L
        "atharah", "اٹھارہ", "अठारह" -> 18L
        "unnees", "انیس", "उन्नीस" -> 19L
        "bees", "بیس", "बीस" -> 20L
        "pachis", "pachees", "پچیس", "पच्चीस" -> 25L
        "tees", "تیس", "तीस" -> 30L
        "paintees", "پینتیس", "पैंतीस" -> 35L
        "chalis", "چالیس", "चालीस" -> 40L
        "pachaas", "pachas", "پچاس", "पचास" -> 50L
        "saath", "ساٹھ", "साठ" -> 60L
        "sattar", "ستر", "सत्तर" -> 70L
        "assi", "اسی", "अस्सी" -> 80L
        "nabbe", "نوے", "नब्बे" -> 90L
        "sau", "سو", "सौ" -> 100L
        else -> 0L
    }

    private fun extractTons(clause: String): Double {
        val match = Regex("(\\d+(?:\\.\\d+)?)\\s*(?:ton|tons|ٹن|टन)", RegexOption.IGNORE_CASE).find(clause)
        return match?.groupValues?.get(1)?.toDoubleOrNull() ?: 0.0
    }

    private fun extractPersonOrCustomerName(clause: String): String {
        val named = Regex("([A-Za-z][A-Za-z0-9\\s-]{1,24}?)\\s+(?:ne|ko|ka|ki|from|to)\\b", RegexOption.IGNORE_CASE)
            .find(clause)
            ?.groupValues?.get(1)
            ?.trim()
        val ignoreWords = setOf("aaj", "today", "mein", "rs", "record", "add", "credit", "udhar", "cash", "baraf", "ice")
        return if (!named.isNullOrBlank() && named.lowercase(Locale.US) !in ignoreWords) named else ""
    }

    private fun containsAny(text: String, vararg keywords: String): Boolean {
        return keywords.any { text.contains(it, ignoreCase = true) }
    }

    companion object {
        private val WAKE_PATTERNS = listOf(
            Regex("\\b(?:hey|hay|hi|hello|ok|okay|aye|ey|a)\\s+(?:ice|eyes|ayes|is|ais)\\b", RegexOption.IGNORE_CASE),
            Regex("(?:اے|ہے|ای|اوئے|ہیلو|اوکے)\\s*(?:آئس|ائس|آئیس|آیس)"),
            Regex("(?:हे|ए|है|हाय|हेलो|ओके)\\s*(?:आइस|आईस|आयस)")
        )

        fun matchWakePhrase(rawTranscript: String): WakePhraseMatch {
            val trimmed = rawTranscript.trim()
            if (trimmed.isBlank()) return WakePhraseMatch(false)
            for (pattern in WAKE_PATTERNS) {
                val match = pattern.find(trimmed)
                if (match != null) {
                    val after = trimmed.substring(match.range.last + 1)
                        .trimStart(',', '.', '!', '?', ' ', '،')
                        .trim()
                    return WakePhraseMatch(matched = true, trailingCommand = after)
                }
            }
            return WakePhraseMatch(false)
        }

        fun detectLanguage(text: String, preferredLocaleTag: String = "ur-PK"): String {
            if (text.any { it in '\u0900'..'\u097F' }) return "hi"
            if (text.any { it in '\u0600'..'\u06FF' }) return "ur"
            val lower = text.lowercase(Locale.US)
            val romanDesiHints = listOf(
                "aaj", "baraf", "kitne", "kitni", "hua", "hui", "hue", "bechi", "becha",
                "udhar", "wasooli", "kharch", "kharcha", "bijli", "tankhwah", "hazri",
                "chutti", "batao", "kholo", "dikhao", "munafa", "rupay", "hazar", "lakh",
                "salam", "assalam", "bhai", "haan", "ji ", "nahi", "kya", "kaise", "hai"
            )
            if (romanDesiHints.any { lower.contains(it) }) {
                return if (preferredLocaleTag.startsWith("hi", ignoreCase = true)) "hi" else "ur"
            }
            return when {
                preferredLocaleTag.startsWith("hi", ignoreCase = true) -> "hi"
                preferredLocaleTag.startsWith("ur", ignoreCase = true) -> "en" // English input stays English unless Urdu/Desi spoken
                else -> "en"
            }
        }
    }
}
