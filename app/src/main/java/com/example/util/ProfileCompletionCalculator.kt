package com.example.util

object ProfileCompletionCalculator {

    data class CompletionSummary(
        val percent: Int,
        val missingFields: List<String>,
        val isReadyForChatOnboarding: Boolean,
        val isReadyForStaffApplication: Boolean,
    )

    fun calculate(
        name: String,
        photoUrl: String,
        phone: String,
        age: Int,
        address: String,
        city: String,
        bio: String,
        interests: String,
        genderPreference: String,
    ): CompletionSummary {
        val checks = listOf(
            "Full Name" to (name.trim().length >= 2),
            "Profile Photo" to photoUrl.trim().isNotEmpty(),
            "Mobile Number" to (phone.count { it.isDigit() } >= 8),
            "Age (18+)" to (age in 18..99),
            "Full Address" to (address.trim().length >= 5),
            "City" to (city.trim().length >= 2),
            "Bio" to (bio.trim().length >= 10),
            "Interests" to (interests.trim().length >= 3),
            "Role / Preference" to genderPreference.trim().isNotEmpty(),
        )

        val completed = checks.count { it.second }
        val percent = ((completed.toFloat() / checks.size.toFloat()) * 100f).toInt().coerceIn(0, 100)
        val missing = checks.filterNot { it.second }.map { it.first }

        val chatReady = name.trim().length >= 2 &&
            phone.count { it.isDigit() } >= 8 &&
            age in 18..99 &&
            address.trim().length >= 4

        return CompletionSummary(
            percent = percent,
            missingFields = missing,
            isReadyForChatOnboarding = chatReady,
            isReadyForStaffApplication = percent == 100,
        )
    }
}
