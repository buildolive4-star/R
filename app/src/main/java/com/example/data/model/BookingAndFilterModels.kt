package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

object BookingLifecycleStatus {
    const val AWAITING_PAYMENT = "awaiting_payment"
    const val CONFIRMED = "confirmed"
    const val AWAITING_RATING = "awaiting_rating"
    const val COMPLETED = "completed"
    const val CANCELLED = "cancelled"
}

object PayoutStatus {
    const val PENDING = "pending"
    const val PAID = "paid"
    const val REJECTED = "rejected"
}

/**
 * Admin-defined per-profile date rate plan (e.g., Dinner -> ₹1,999, Night -> ₹3,499).
 * Stored cleanly in Firestore as encoded "Label|₹Price" strings inside `bookingPlans`.
 */
@Keep
data class BookingRatePlan(
    val label: String,
    val priceText: String,
    val numericPrice: Int,
) {
    fun encode(): String = "${label.trim().replace("|", "-")}|${priceText.trim().replace("|", "")}"

    companion object {
        fun fromInputs(label: String, rawPrice: String): BookingRatePlan {
            val cleanLabel = label.trim().ifBlank { "Standard Date" }.take(32)
            val cleanPrice = rawPrice.trim().ifBlank { "₹499" }.take(24)
            val formattedPrice = if (cleanPrice.startsWith("₹") || cleanPrice.equals("Free", true)) {
                cleanPrice
            } else {
                "₹$cleanPrice"
            }
            val numeric = formattedPrice.filter { it.isDigit() }.toIntOrNull() ?: 499
            return BookingRatePlan(
                label = cleanLabel,
                priceText = formattedPrice,
                numericPrice = numeric,
            )
        }

        fun parse(encoded: String): BookingRatePlan? {
            val parts = encoded.split("|", limit = 2)
            if (parts.isEmpty() || parts[0].isBlank()) return null
            val label = parts[0].trim()
            val rawPrice = parts.getOrNull(1)?.trim().orEmpty().ifBlank { "₹499" }
            return fromInputs(label, rawPrice)
        }

        fun parseList(encodedList: List<String>, fallbackPrice: String): List<BookingRatePlan> {
            val parsed = encodedList.mapNotNull { parse(it) }
            if (parsed.isNotEmpty()) return parsed
            val baseNum = fallbackPrice.filter { it.isDigit() }.toIntOrNull() ?: 499
            return listOf(
                fromInputs("Coffee / Meet", fallbackPrice.ifBlank { "₹$baseNum" }),
                fromInputs("Dinner Date", "₹${(baseNum * 2).coerceAtLeast(999)}"),
                fromInputs("Night Out", "₹${(baseNum * 4).coerceAtLeast(1999)}"),
            )
        }

        val PRESET_PLAN_NAMES = listOf("Coffee", "Dinner", "Night", "Movie", "Day Out", "VIP Event")
    }
}

/**
 * Admin-defined Filter Category with manual sub-options (e.g., Experience -> Dinner, Night, Coffee).
 * Stored in Firestore `app_config/filter_taxonomy` as `groups: List<String>` ("Category::Opt1,Opt2").
 */
@Keep
data class FilterCategoryGroup(
    val category: String = "",
    val options: List<String> = emptyList(),
) {
    fun encode(): String {
        val cleanCat = category.trim().replace("::", " ").take(32)
        val cleanOpts = options.map { it.trim().replace(",", " ").replace("::", " ").take(32) }
            .filter { it.isNotBlank() }
            .distinctBy { it.lowercase() }
        return "$cleanCat::${cleanOpts.joinToString(",")}"
    }

    companion object {
        fun parse(encoded: String): FilterCategoryGroup? {
            val parts = encoded.split("::", limit = 2)
            val cat = parts.firstOrNull()?.trim().orEmpty()
            if (cat.isBlank()) return null
            val opts = parts.getOrNull(1).orEmpty()
                .split(",")
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinctBy { it.lowercase() }
            return FilterCategoryGroup(category = cat, options = opts)
        }

        fun parseList(raw: List<String>): List<FilterCategoryGroup> =
            raw.mapNotNull { parse(it) }

        fun defaultTaxonomy(): List<FilterCategoryGroup> = listOf(
            FilterCategoryGroup("Occasion", listOf("Dinner", "Night", "Coffee", "Movie", "Travel")),
            FilterCategoryGroup("Companion Type", listOf("GF", "BF", "VIP", "Verified")),
            FilterCategoryGroup("Budget", listOf("Under ₹299", "Under ₹499", "Premium")),
            FilterCategoryGroup("City", listOf("Mumbai", "Delhi NCR", "Bengaluru", "Pune", "Jaipur")),
        )
    }
}

@Keep
@IgnoreExtraProperties
data class FilterTaxonomyConfig(
    @DocumentId val id: String = "",
    val groups: List<String> = emptyList(),
    val updatedByEmail: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null,
)

@Keep
@IgnoreExtraProperties
data class PaymentServerConfig(
    @DocumentId val id: String = "",
    val serverBaseUrl: String = DEFAULT_PAYMENT_SERVER_URL,
    val defaultAdvanceInr: Int = 299,
    val updatedByEmail: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val cleanBaseUrl: String
        get() = serverBaseUrl.trim().trimEnd('/').ifBlank { DEFAULT_PAYMENT_SERVER_URL }

    companion object {
        const val DEFAULT_PAYMENT_SERVER_URL = "https://jaiambey.shop"
    }
}

@Keep
@IgnoreExtraProperties
data class DateMateBooking(
    @DocumentId val id: String = "",
    val clientUid: String = "",
    val clientName: String = "",
    val clientPhone: String = "",
    val clientPhotoUrl: String = "",
    val profileId: String = "",
    val profileName: String = "",
    val profilePhotoUrl: String = "",
    val staffOwnerUid: String = "",
    val planLabel: String = "",
    val planPrice: String = "",
    val planNumericAmount: Int = 0,
    val advanceAmountInr: Int = 0,
    val advancePaid: Boolean = false,
    val paymentId: String = "",
    val orderId: String = "",
    val dateLabel: String = "",
    val timeSlot: String = "",
    val meetingLocation: String = "",
    val notes: String = "",
    val bookingStatus: String = BookingLifecycleStatus.AWAITING_PAYMENT,
    val staffAttended: Boolean = false,
    val staffAttendedAtEpochMs: Long = 0L,
    val userRating: Int = 0,
    val userFeedback: String = "",
    val ratedAtEpochMs: Long = 0L,
    val virtualEarningInr: Int = 0,
    val payoutStatus: String = PayoutStatus.PENDING,
    val payoutNote: String = "",
    val payoutUpdatedByEmail: String = "",
    val payoutUpdatedAtEpochMs: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
) {
    val isPaymentConfirmed: Boolean
        get() = advancePaid && bookingStatus != BookingLifecycleStatus.AWAITING_PAYMENT

    val isWaitingForRating: Boolean
        get() = bookingStatus == BookingLifecycleStatus.AWAITING_RATING && staffAttended && userRating <= 0

    val isCompleted: Boolean
        get() = bookingStatus == BookingLifecycleStatus.COMPLETED

    val effectiveVirtualEarningInr: Int
        get() = if (virtualEarningInr > 0) virtualEarningInr else planNumericAmount.coerceAtLeast(advanceAmountInr)
}

@Keep
@IgnoreExtraProperties
data class UserComplaint(
    @DocumentId val id: String = "",
    val userUid: String = "",
    val userName: String = "",
    val userEmail: String = "",
    val userPhone: String = "",
    val subject: String = "",
    val message: String = "",
    val status: String = "open", // "open" | "resolved"
    val adminReply: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
)
