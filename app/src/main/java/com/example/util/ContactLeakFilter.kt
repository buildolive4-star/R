package com.example.util

/**
 * Validates chat messages sent by Companion Staff to ensure personal contact info
 * (phone numbers, emails, Instagram handles/IDs, WhatsApp/Telegram/Snapchat links)
 * cannot be shared outside the JustUs platform.
 */
object ContactLeakFilter {

    data class ValidationResult(
        val isAllowed: Boolean,
        val reason: String? = null,
    )

    private val emailRegex = Regex("""[A-Za-z0-9._%+\-]+@[A-Za-z0-9.\-]+\.[A-Za-z]{2,}""")
    private val handleRegex = Regex("""(?<!\w)@[A-Za-z0-9_.]{2,}""")
    private val phoneCandidateRegex = Regex("""(\+?\d[\d\s\-().]{5,}\d)""")
    private val obfuscatedDigitsRegex = Regex("""(?:zero|one|two|three|four|five|six|seven|eight|nine)(?:[\s,\-]+(?:zero|one|two|three|four|five|six|seven|eight|nine)){5,}""", RegexOption.IGNORE_CASE)
    private val urlRegex = Regex("""(https?://|www\.)\S+""", RegexOption.IGNORE_CASE)
    private val socialKeywordRegex = Regex(
        """\b(instagram|insta|ig\s*id|insta\s*id|whatsapp|wa\.me|telegram|t\.me|snapchat|snap\s*id|facebook|fb\s*id|gm@il|gmail|yahoo|outlook|call\s*me\s*on|my\s*number|phone\s*no|mobile\s*no)\b""",
        RegexOption.IGNORE_CASE
    )

    fun validateForStaff(rawText: String): ValidationResult {
        val text = rawText.trim()
        if (text.isEmpty()) {
            return ValidationResult(isAllowed = false, reason = "Message cannot be empty.")
        }

        if (emailRegex.containsMatchIn(text)) {
            return ValidationResult(
                isAllowed = false,
                reason = "Sharing email addresses is blocked for Staff safety."
            )
        }

        if (handleRegex.containsMatchIn(text)) {
            return ValidationResult(
                isAllowed = false,
                reason = "Sharing @handles or social IDs is blocked for Staff."
            )
        }

        val hasPhoneDigits = phoneCandidateRegex.findAll(text).any { match ->
            val digitsOnly = match.value.count { it.isDigit() }
            digitsOnly >= 7
        }
        val totalDigitsCount = text.count { it.isDigit() }
        if (hasPhoneDigits || totalDigitsCount >= 8 || obfuscatedDigitsRegex.containsMatchIn(text)) {
            return ValidationResult(
                isAllowed = false,
                reason = "Sharing phone or mobile numbers is not allowed in Staff chat."
            )
        }

        if (urlRegex.containsMatchIn(text)) {
            return ValidationResult(
                isAllowed = false,
                reason = "External links are blocked in Staff chat."
            )
        }

        if (socialKeywordRegex.containsMatchIn(text)) {
            return ValidationResult(
                isAllowed = false,
                reason = "Sharing Instagram, WhatsApp, Snapchat, Telegram, or personal contact details is blocked."
            )
        }

        return ValidationResult(isAllowed = true)
    }
}
