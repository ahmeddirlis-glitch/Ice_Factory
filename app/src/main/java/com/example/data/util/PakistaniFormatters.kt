package com.example.data.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object PakistaniFormatters {
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val displayDateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    private val timeFormat = SimpleDateFormat("hh:mm a", Locale.US)
    private val monthFormat = SimpleDateFormat("yyyy-MM", Locale.US)

    fun formatPkr(amount: Double, includeSymbol: Boolean = true): String {
        val negative = amount < 0
        val rounded = abs(amount).roundToLong()
        val formattedDigits = formatIndianNumbering(rounded)
        val sign = if (negative) "-" else ""
        return if (includeSymbol) "${sign}Rs. $formattedDigits" else "$sign$formattedDigits"
    }

    fun formatIndianNumbering(value: Long): String {
        val str = abs(value).toString()
        if (str.length <= 3) return str
        val lastThree = str.takeLast(3)
        val remaining = str.dropLast(3)
        val sb = StringBuilder()
        var i = remaining.length
        var count = 0
        while (i > 0) {
            sb.append(remaining[i - 1])
            count++
            if (count == 2 && i > 1) {
                sb.append(',')
                count = 0
            }
            i--
        }
        return "${sb.reverse()},$lastThree"
    }

    fun formatTons(tons: Double): String {
        return if (tons % 1.0 == 0.0) {
            "${tons.toLong()} Ton"
        } else {
            String.format(Locale.US, "%.1f Ton", tons)
        }
    }

    fun todayIso(): String = dateFormat.format(Date())

    fun currentMonthIso(): String = monthFormat.format(Date())

    fun currentTimeDisplay(): String = timeFormat.format(Date())

    fun todayDisplay(): String = SimpleDateFormat("EEE, MMM dd, yyyy", Locale.US).format(Date())

    fun formatDisplayDate(isoDate: String): String {
        return try {
            val parsed = dateFormat.parse(isoDate)
            if (parsed != null) displayDateFormat.format(parsed) else isoDate
        } catch (_: Exception) {
            isoDate
        }
    }

    fun daysAgoIso(daysAgo: Int): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -daysAgo)
        return dateFormat.format(cal.time)
    }

    fun dayShortLabel(isoDate: String): String {
        return try {
            val parsed = dateFormat.parse(isoDate)
            if (parsed != null) SimpleDateFormat("EEE", Locale.US).format(parsed) else isoDate
        } catch (_: Exception) {
            isoDate
        }
    }

    fun monthsSince(startIso: String): Double {
        return try {
            val start = dateFormat.parse(startIso) ?: return 1.0
            val diffMillis = (Date().time - start.time).coerceAtLeast(0L)
            val days = diffMillis / (1000.0 * 60.0 * 60.0 * 24.0)
            (days / 30.4375).coerceAtLeast(1.0)
        } catch (_: Exception) {
            1.0
        }
    }

    fun estimatedCompletionMonthYear(monthsNeeded: Double): String {
        val cal = Calendar.getInstance()
        val wholeMonths = monthsNeeded.roundToLong().toInt().coerceIn(0, 600)
        cal.add(Calendar.MONTH, wholeMonths)
        return SimpleDateFormat("MMM yyyy", Locale.US).format(cal.time)
    }

    fun calculateHoursBetween(checkIn: String, checkOut: String): Double {
        return try {
            val t1 = timeFormat.parse(checkIn)?.time ?: return 0.0
            val t2 = timeFormat.parse(checkOut)?.time ?: return 0.0
            val diff = (t2 - t1).coerceAtLeast(0L)
            ((diff / (1000.0 * 60.0 * 60.0)) * 100.0).roundToLong() / 100.0
        } catch (_: Exception) {
            8.0
        }
    }

    fun speakablePkr(amount: Double): String {
        val rounded = abs(amount).roundToLong()
        return when {
            rounded >= 10_000_000L -> {
                val crore = rounded / 10_000_000.0
                String.format(Locale.US, "%.2f crore rupees", crore).replace(".00 ", " ")
            }
            rounded >= 100_000L -> {
                val lakh = rounded / 100_000.0
                String.format(Locale.US, "%.2f lakh rupees", lakh).replace(".00 ", " ")
            }
            else -> "$rounded rupees"
        }
    }

    private val URDU_DIGIT_MAP = mapOf(
        '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
        '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9',
        '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
        '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
        '०' to '0', '१' to '1', '२' to '2', '३' to '3', '४' to '4',
        '५' to '5', '६' to '6', '७' to '7', '८' to '8', '९' to '9',
        '٬' to ',', '،' to ' '
    )

    private val URDU_NUMBER_WORDS = mapOf(
        "aik" to "1", "ek" to "1", "ایک" to "1", "एक" to "1",
        "do" to "2", "دو" to "2", "दो" to "2",
        "teen" to "3", "تین" to "3", "तीन" to "3",
        "chaar" to "4", "char" to "4", "چار" to "4", "चार" to "4",
        "paanch" to "5", "panch" to "5", "پانچ" to "5", "पांच" to "5", "पाँच" to "5",
        "chay" to "6", "cheh" to "6", "چھ" to "6", "छह" to "6",
        "saat" to "7", "sat" to "7", "سات" to "7", "सात" to "7",
        "aath" to "8", "ath" to "8", "آٹھ" to "8", "आठ" to "8",
        "nau" to "9", "نو" to "9", "नौ" to "9",
        "das" to "10", "دس" to "10", "दस" to "10",
        "gyarah" to "11", "گیارہ" to "11", "ग्यारह" to "11",
        "barah" to "12", "بارہ" to "12", "बारह" to "12",
        "terah" to "13", "تیرہ" to "13", "तेरह" to "13",
        "chaudah" to "14", "چودہ" to "14", "चौदह" to "14",
        "pandrah" to "15", "پندرہ" to "15", "पंद्रह" to "15",
        "solah" to "16", "سولہ" to "16", "सोलह" to "16",
        "satrah" to "17", "سترہ" to "17", "सत्रह" to "17",
        "atharah" to "18", "اٹھارہ" to "18", "अठारह" to "18",
        "unnees" to "19", "انیس" to "19", "उन्नीस" to "19",
        "bees" to "20", "بیس" to "20", "बीस" to "20",
        "pachis" to "25", "pachees" to "25", "پچیس" to "25", "पच्चीस" to "25",
        "tees" to "30", "تیس" to "30", "तीस" to "30",
        "paintees" to "35", "پینتیس" to "35", "पैंतीस" to "35",
        "chalis" to "40", "چالیس" to "40", "चालीस" to "40",
        "pachaas" to "50", "pachas" to "50", "پچاس" to "50", "पचास" to "50",
        "pachpan" to "55", "پچپن" to "55", "पचपन" to "55",
        "saath" to "60", "sath" to "60", "ساٹھ" to "60", "साठ" to "60",
        "painsath" to "65", "پینسٹھ" to "65", "पैंसठ" to "65",
        "sattar" to "70", "ستر" to "70", "सत्तर" to "70",
        "pachattar" to "75", "پچھتر" to "75", "पचहत्तर" to "75",
        "assi" to "80", "اسی" to "80", "अस्सी" to "80",
        "nabbe" to "90", "نوے" to "90", "नब्बे" to "90",
        "sau" to "100", "سو" to "100", "सौ" to "100",
        "dedh" to "1.5", "ڈیڑھ" to "1.5", "डेढ़" to "1.5",
        "dhai" to "2.5", "ڈھائی" to "2.5", "ढाई" to "2.5"
    )

    fun convertUrduDigits(input: String): String {
        if (input.isBlank()) return input
        val sb = StringBuilder(input.length)
        for (ch in input) {
            sb.append(URDU_DIGIT_MAP[ch] ?: ch)
        }
        return sb.toString()
    }

    /**
     * Converts Urdu/Hindi/Arabic digits, Urdu/Hindi number words + multipliers (ہزار / हजार / hazar / لاکھ / लाख / lakh / کروڑ / करोड़ / crore),
     * and raw currency amounts like "75000" or "75 ہزار" into normalized "Rs. 75,000" display format,
     * while preserving unit counts like "15 blocks" or "50 بلاکس".
     */
    fun normalizeVoiceTranscript(raw: String): String {
        if (raw.isBlank()) return ""
        var text = convertUrduDigits(raw).trim()

        // Replace standalone Urdu/Hindi number words followed by a multiplier (e.g., "پچھتر ہزار" -> "75 ہزار")
        URDU_NUMBER_WORDS.forEach { (word, digit) ->
            val pattern = Regex("(?<=^|\\s)${Regex.escape(word)}(?=\\s*(?:ہزار|لاکھ|کروڑ|हज़ार|हजार|लाख|करोड़|hazar|hazaar|thousand|lakh|lac|crore|karor))", RegexOption.IGNORE_CASE)
            text = pattern.replace(text, digit)
        }

        // Replace standalone Urdu/Hindi number words followed by blocks/tons
        URDU_NUMBER_WORDS.forEach { (word, digit) ->
            val pattern = Regex("(?<=^|\\s)${Regex.escape(word)}(?=\\s*(?:block|blocks|بلاک|بلاکس|ब्लॉक|ब्लॉक्स|ton|tons|ٹن|टन))", RegexOption.IGNORE_CASE)
            text = pattern.replace(text, digit)
        }

        // Handle "Rs." / "Rs" already before a multiplied phrase like "Rs 75 ہزار" -> "75 ہزار" first so we don't duplicate "Rs."
        text = text.replace(Regex("(?i)(?:rs\\.?|pkr|₹)\\s*(?=[0-9])"), "RS_MARKER ")

        // Replace number + multiplier (e.g., "75 ہزار", "75ہزار", "35 हजार", "2 لاکھ", "1.5 crore", "75 hazar")
        val multiplierRegex = Regex(
            "(?:RS_MARKER\\s*)?(\\d+(?:[.,]\\d+)?)\\s*(ہزار|हज़ार|हजार|hazaar|hazar|thousand|k|لاکھ|लाख|lakh|lac|کروڑ|करोड़|crore|karor)(?=$|[\\s.,!?])",
            RegexOption.IGNORE_CASE
        )
        text = multiplierRegex.replace(text) { m ->
            val rawNum = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@replace m.value
            val unit = m.groupValues[2].lowercase(Locale.US)
            val multiplier = when (unit) {
                "ہزار", "हज़ार", "हजार", "hazar", "hazaar", "thousand", "k" -> 1_000.0
                "لاکھ", "लाख", "lakh", "lac" -> 100_000.0
                "کروڑ", "करोड़", "crore", "karor" -> 10_000_000.0
                else -> 1.0
            }
            val total = rawNum * multiplier
            formatPkr(total)
        }

        // Format remaining explicit RS_MARKER numbers or standalone large currency numbers (>= 50 unless followed by unit keywords)
        val markerNumRegex = Regex("RS_MARKER\\s*(\\d+(?:,\\d+)*(?:\\.\\d+)?)")
        text = markerNumRegex.replace(text) { m ->
            val v = m.groupValues[1].replace(",", "").toDoubleOrNull() ?: return@replace m.value
            formatPkr(v)
        }

        val standaloneCurrencyRegex = Regex(
            "(?<!Rs\\.\\s)(?<!Rs\\.)(?<![,\\d.])(\\b\\d{1,3}(?:,\\d{2,3})+\\b|\\b\\d{3,}\\b|\\b[5-9]\\d\\b)(?![,\\d.])(?!\\s*(?:ton|tons|ٹن|टन|block|blocks|بلاک|بلاکس|ब्लॉक|ब्लॉक्स|pcs|piece|pieces|kg|liter|liters|litre|litres|unit|units|یونٹ|میٹر|meter|%|percent|saal|sal|year|years|month|months|chai|bottle|bottles|برف|बर्फ))",
            RegexOption.IGNORE_CASE
        )
        text = standaloneCurrencyRegex.replace(text) { m ->
            val rawNum = m.groupValues[1].replace(",", "")
            val v = rawNum.toDoubleOrNull() ?: return@replace m.value
            // Avoid formatting phone numbers or dates like 2026
            if (rawNum.length >= 10 || (rawNum.length == 4 && v in 2020.0..2035.0)) {
                m.value
            } else {
                formatPkr(v)
            }
        }

        // Strip trailing currency words left orphaned after Rs. X formatting (e.g., "Rs. 75,000 روپے")
        text = text.replace(Regex("(?<=\\d)\\s*(?:روپے|रुपये|रुपए|rupees|rupay|rs|pkr)(?=$|[\\s.,!?])", RegexOption.IGNORE_CASE), "")
        return text.replace(Regex("\\s+"), " ").trim()
    }

    /**
     * Converts names like "ahmed dirlis" or "MUHAMMAD ali" into Title Case ("Ahmed Dirlis", "Muhammad Ali")
     * while preserving bracket tags like "[Demo]".
     */
    fun formatTitleCase(raw: String): String {
        val trimmed = raw.trim().replace(Regex("\\s+"), " ")
        if (trimmed.isBlank()) return ""
        return trimmed.split(" ").joinToString(" ") { word ->
            if (word.startsWith("[") && word.endsWith("]")) {
                word
            } else if (word.length <= 1) {
                word.uppercase(Locale.US)
            } else {
                word.lowercase(Locale.US).replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase(Locale.US) else ch.toString()
                }
            }
        }
    }

    const val CNIC_DIGIT_COUNT = 13

    /**
     * Extracts digits from English or Urdu/Arabic numeric input and strictly caps length at 13 digits.
     */
    fun normalizeCnicDigits(raw: String): String {
        if (raw.isBlank()) return ""
        return convertUrduDigits(raw).filter { it.isDigit() }.take(CNIC_DIGIT_COUNT)
    }

    /**
     * Formats up to 13 CNIC digits into standard Pakistani NADRA format: XXXXX-XXXXXXX-X (e.g. 44204-1253835-5).
     */
    fun formatCnic(raw: String): String {
        val digits = normalizeCnicDigits(raw)
        if (digits.isEmpty()) return ""
        return buildString(digits.length + 2) {
            for (i in digits.indices) {
                append(digits[i])
                if (i == 4 && i + 1 < digits.length) append('-')
                if (i == 11 && i + 1 < digits.length) append('-')
            }
        }
    }

    fun isValidCnic(raw: String): Boolean = normalizeCnicDigits(raw).length == CNIC_DIGIT_COUNT

    const val PHONE_DIGIT_COUNT = 11

    /**
     * Extracts digits from English or Urdu/Arabic numeric input and strictly caps length at 11 digits.
     */
    fun normalizePhoneDigits(raw: String): String {
        if (raw.isBlank()) return ""
        return convertUrduDigits(raw).filter { it.isDigit() }.take(PHONE_DIGIT_COUNT)
    }

    /**
     * Formats up to 11 mobile digits into standard Pakistani format: XXXX-XXXXXXX (e.g. 0333-2828285).
     */
    fun formatPhone(raw: String): String {
        val digits = normalizePhoneDigits(raw)
        if (digits.isEmpty()) return ""
        return buildString(digits.length + 1) {
            for (i in digits.indices) {
                append(digits[i])
                if (i == 3 && i + 1 < digits.length) append('-')
            }
        }
    }

    fun isValidPhone(raw: String): Boolean = normalizePhoneDigits(raw).length == PHONE_DIGIT_COUNT

    /**
     * Returns true when an entered emergency contact matches either the primary mobile or WhatsApp number.
     */
    fun isEmergencySameAsPrimary(
        emergencyRaw: String,
        mobileRaw: String,
        whatsappRaw: String = ""
    ): Boolean {
        val emergencyDigits = normalizePhoneDigits(emergencyRaw)
        if (emergencyDigits.isEmpty()) return false
        val mobileDigits = normalizePhoneDigits(mobileRaw)
        val whatsappDigits = normalizePhoneDigits(whatsappRaw)
        return (mobileDigits.isNotEmpty() && emergencyDigits == mobileDigits) ||
            (whatsappDigits.isNotEmpty() && emergencyDigits == whatsappDigits)
    }

    private val ONES_WORDS = arrayOf(
        "", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
        "Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
        "Seventeen", "Eighteen", "Nineteen"
    )
    private val TENS_WORDS = arrayOf(
        "", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"
    )

    private fun twoDigitWords(n: Int): String = when {
        n <= 0 -> ""
        n < 20 -> ONES_WORDS[n]
        else -> {
            val tens = TENS_WORDS[n / 10]
            val ones = ONES_WORDS[n % 10]
            if (ones.isEmpty()) tens else "$tens $ones"
        }
    }

    private fun threeDigitWords(n: Int): String {
        val h = n / 100
        val rem = n % 100
        val parts = mutableListOf<String>()
        if (h > 0) parts += "${ONES_WORDS[h]} Hundred"
        if (rem > 0) parts += twoDigitWords(rem)
        return parts.joinToString(" ")
    }

    /**
     * Converts a PKR numeric value into formal invoice words, e.g.:
     * 2400.0 -> "Rupees Two Thousand Four Hundred Only"
     */
    fun amountInWords(amount: Double): String {
        val isNegative = amount < 0
        val value = abs(amount).roundToLong()
        if (value == 0L) return "Rupees Zero Only"

        val parts = mutableListOf<String>()
        var remaining = value

        val arab = (remaining / 1_000_000_000L).toInt()
        if (arab > 0) {
            parts += "${threeDigitWords(arab)} Arab"
            remaining %= 1_000_000_000L
        }
        val crore = (remaining / 10_000_000L).toInt()
        if (crore > 0) {
            parts += "${twoDigitWords(crore)} Crore"
            remaining %= 10_000_000L
        }
        val lakh = (remaining / 100_000L).toInt()
        if (lakh > 0) {
            parts += "${twoDigitWords(lakh)} Lakh"
            remaining %= 100_000L
        }
        val thousand = (remaining / 1_000L).toInt()
        if (thousand > 0) {
            parts += "${twoDigitWords(thousand)} Thousand"
            remaining %= 1_000L
        }
        val rest = remaining.toInt()
        if (rest > 0) {
            parts += threeDigitWords(rest)
        }

        val prefix = if (isNegative) "Minus " else ""
        return "Rupees $prefix${parts.joinToString(" ")} Only"
    }
}



