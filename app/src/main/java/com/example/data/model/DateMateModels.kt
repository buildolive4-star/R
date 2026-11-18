package com.example.data.model

import com.example.R

enum class DateMateTab(val route: String, val labelRes: Int, val testTag: String) {
    HOME("home", R.string.nav_home, "nav_tab_home"),
    DISCOVER("discover", R.string.nav_discover, "nav_tab_discover"),
    BOOKINGS("bookings", R.string.nav_bookings, "nav_tab_bookings"),
    CHAT("chat", R.string.nav_chat, "nav_tab_chat"),
    PROFILE("profile", R.string.nav_profile, "nav_tab_profile"),
}

enum class MembershipTier(
    val title: String,
    val subtitle: String,
    val monthlyPriceInr: Int,
    val yearlyPriceInr: Int,
    val badge: String,
    val isRecommended: Boolean = false,
    val benefits: List<String>,
) {
    FREE(
        title = "Free",
        subtitle = "Essential discovery & browsing",
        monthlyPriceInr = 0,
        yearlyPriceInr = 0,
        badge = "STARTER",
        benefits = listOf(
            "Browse curated companion profiles",
            "10 daily Discover swipes",
            "Standard profile filters",
            "View companion availability & reviews",
        )
    ),
    SILVER(
        title = "Silver",
        subtitle = "Unlimited discovery & instant chat",
        monthlyPriceInr = 799,
        yearlyPriceInr = 6499,
        badge = "POPULAR",
        benefits = listOf(
            "Unlimited Discover swipes & rewinds",
            "Direct instant chat with companions",
            "5 Super Likes every week",
            "Advanced distance & interest filters",
            "Priority booking confirmation",
        )
    ),
    GOLD(
        title = "Gold",
        subtitle = "Full VIP experience & priority dates",
        monthlyPriceInr = 1499,
        yearlyPriceInr = 11999,
        badge = "RECOMMENDED",
        isRecommended = true,
        benefits = listOf(
            "Everything in Silver plus VIP access",
            "Unlimited audio & video calling in chat",
            "See everyone who liked your profile",
            "15 Super Likes every week",
            "Flexible free booking rescheduling",
            "Verified Gold Rose badge on your profile",
        )
    ),
    PLATINUM(
        title = "Platinum",
        subtitle = "Bespoke luxury & concierge privileges",
        monthlyPriceInr = 2999,
        yearlyPriceInr = 23999,
        badge = "LUXURY",
        benefits = listOf(
            "Everything in Gold with top-tier priority",
            "First-in-line placement in companion inboxes",
            "Complimentary date curation & table reservations",
            "Unlimited Super Likes & Profile Boosts",
            "Incognito private browsing mode",
            "24/7 dedicated DateMate relationship concierge",
        )
    ),
}

enum class BookingStatus(val label: String) {
    UPCOMING("Upcoming"),
    PENDING("Pending"),
    CONFIRMED("Confirmed"),
    COMPLETED("Completed"),
    CANCELLED("Cancelled"),
}

data class CompanionReview(
    val id: String,
    val reviewerName: String,
    val reviewerAvatar: String,
    val rating: Float,
    val comment: String,
    val dateLabel: String,
)

data class DateMateCompanionProfile(
    val id: String,
    val name: String,
    val age: Int,
    val city: String,
    val distanceKm: Int,
    val tagline: String,
    val aboutMe: String,
    val isVerified: Boolean = true,
    val isOnline: Boolean = true,
    val isPopular: Boolean = false,
    val rating: Float = 4.9f,
    val reviewCount: Int = 42,
    val pricePerHourInr: Int = 1800,
    val primaryPhotoUrl: String,
    val galleryPhotos: List<String>,
    val interests: List<String>,
    val hobbies: List<String>,
    val languages: List<String>,
    val personalityTraits: List<String>,
    val availabilityDays: List<String>,
    val availableTimeSlots: List<String>,
    val reviews: List<CompanionReview>,
)

data class DateBooking(
    val id: String,
    val companionId: String,
    val companionName: String,
    val companionPhotoUrl: String,
    val companionCity: String,
    val dateLabel: String,
    val timeSlot: String,
    val durationHours: Int,
    val venueExperience: String,
    val specialNote: String,
    val totalFeeInr: Int,
    val status: BookingStatus,
    val confirmationCode: String,
    val cancellationPolicy: String = "Free cancellation up to 6 hours before your scheduled meet.",
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

data class DateMateMessage(
    val id: String,
    val threadId: String,
    val senderIsMe: Boolean,
    val senderName: String,
    val text: String,
    val imageUrl: String = "",
    val voiceDurationSec: Int = 0,
    val replyToPreview: String = "",
    val reactions: List<String> = emptyList(),
    val timestampLabel: String,
    val isRead: Boolean = true,
    val createdAtEpochMs: Long = System.currentTimeMillis(),
)

data class DateMateChatThread(
    val id: String,
    val companionId: String,
    val companionName: String,
    val companionAge: Int,
    val companionCity: String,
    val companionPhotoUrl: String,
    val isOnline: Boolean,
    val isVerified: Boolean,
    val isTyping: Boolean = false,
    val lastMessage: String,
    val lastUpdatedLabel: String,
    val unreadCount: Int = 0,
)

enum class NotificationCategory(val label: String) {
    NEW_LIKE("New Likes"),
    NEW_MATCH("New Matches"),
    NEW_MESSAGE("New Messages"),
    BOOKING_REQUEST("Booking Requests"),
    BOOKING_CONFIRMATION("Booking Confirmations"),
    PROFILE_VERIFICATION("Profile Verification"),
    MEMBERSHIP_UPDATE("Membership Updates"),
}

data class DateMateNotification(
    val id: String,
    val category: NotificationCategory,
    val title: String,
    val body: String,
    val timestampLabel: String,
    val avatarUrl: String = "",
    val targetCompanionId: String = "",
    val targetThreadId: String = "",
    val isRead: Boolean = false,
)

data class CustomerUserProfile(
    val fullName: String = "Aarav Khanna",
    val email: String = "aarav.khanna@datemate.app",
    val mobileNumber: String = "+91 98204 51280",
    val dateOfBirth: String = "14 May 1998",
    val age: Int = 27,
    val city: String = "Mumbai",
    val bio: String = "Architect, specialty coffee enthusiast, and lover of rooftop jazz evenings & art galleries.",
    val avatarUrl: String = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=900&q=85",
    val galleryPhotos: List<String> = listOf(
        "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=900&q=85",
        "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=900&q=85",
        "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=900&q=85",
    ),
    val interests: List<String> = listOf("Fine Dining", "Art Galleries", "Coffee Dates", "Live Music", "Travel", "Sunset Walks"),
    val isAge18Verified: Boolean = true,
    val isIdentityVerified: Boolean = true,
    val membershipTier: MembershipTier = MembershipTier.GOLD,
    val isYearlyBilling: Boolean = true,
    val showOnlineStatus: Boolean = true,
    val showDistance: Boolean = true,
    val readReceiptsEnabled: Boolean = true,
    val incognitoMode: Boolean = false,
    val pushLikesAndMatches: Boolean = true,
    val pushMessages: Boolean = true,
    val pushBookingAlerts: Boolean = true,
    val pushMembershipOffers: Boolean = true,
)

data class DiscoverFilters(
    val maxDistanceKm: Int = 35,
    val ageRange: IntRange = 20..34,
    val selectedCity: String = "All Cities",
    val selectedInterest: String = "All",
    val onlineOnly: Boolean = false,
    val verifiedOnly: Boolean = true,
)

data class BookingDraft(
    val step: Int = 1,
    val companionId: String = "",
    val selectedDateLabel: String = "Sat, 24 May",
    val selectedTimeSlot: String = "07:30 PM",
    val durationHours: Int = 3,
    val venueExperience: String = "Fine Dining & Lounge",
    val specialNote: String = "Looking forward to a relaxed evening conversation over dinner.",
    val agreedToPolicy: Boolean = true,
)

object DateMateCatalog {
    val cities = listOf("All Cities", "Mumbai", "New Delhi", "Bengaluru", "Pune", "Hyderabad", "Goa")

    val allInterests = listOf(
        "All",
        "Fine Dining",
        "Coffee Dates",
        "Art Galleries",
        "Live Jazz",
        "Sunset Walks",
        "Wine Tasting",
        "Theatre & Cinema",
        "Bookstores",
        "Luxury Events",
        "Travel Stories",
        "Photography",
    )

    val dateOptions = listOf(
        "Today, Evening",
        "Tomorrow",
        "Sat, 24 May",
        "Sun, 25 May",
        "Wed, 28 May",
        "Fri, 30 May",
        "Sat, 31 May",
    )

    val timeSlotOptions = listOf(
        "12:30 PM",
        "03:00 PM",
        "05:30 PM",
        "07:30 PM",
        "08:30 PM",
        "09:30 PM",
    )

    val durationOptions = listOf(1, 2, 3, 4, 6)

    val venueExperiences = listOf(
        "Fine Dining & Lounge",
        "Artisanal Coffee & Gallery Walk",
        "Sunset Promenade & Dessert",
        "Cultural Event / Gala Companion",
        "Cozy Bookstore & Wine Bar",
    )

    val curatedCompanions: List<DateMateCompanionProfile> = listOf(
        DateMateCompanionProfile(
            id = "comp_aanya_01",
            name = "Aanya Sharma",
            age = 24,
            city = "Mumbai",
            distanceKm = 4,
            tagline = "Art curator & warm conversationalist for soulful evenings ✨",
            aboutMe = "Contemporary art curator in Bandra with a love for meaningful conversations, dimly lit jazz cafes, and coastal sunset walks. I enjoy making people feel effortlessly comfortable and heard.",
            isVerified = true,
            isOnline = true,
            isPopular = true,
            rating = 4.96f,
            reviewCount = 68,
            pricePerHourInr = 2200,
            primaryPhotoUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=1000&q=85",
            galleryPhotos = listOf(
                "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=1000&q=85",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=1000&q=85",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1000&q=85",
            ),
            interests = listOf("Fine Dining", "Art Galleries", "Coffee Dates", "Live Jazz"),
            hobbies = listOf("Oil Painting", "Specialty Pour-Over Coffee", "Film Photography", "Contemporary Literature"),
            languages = listOf("English", "Hindi", "French"),
            personalityTraits = listOf("Empathetic", "Witty", "Graceful", "Great Listener"),
            availabilityDays = listOf("Wed", "Thu", "Fri", "Sat", "Sun"),
            availableTimeSlots = listOf("05:30 PM", "07:30 PM", "08:30 PM"),
            reviews = listOf(
                CompanionReview(
                    id = "rev_1",
                    reviewerName = "Rohan M.",
                    reviewerAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=300&q=80",
                    rating = 5.0f,
                    comment = "Aanya is genuinely captivating. Three hours over dinner flew by like ten minutes.",
                    dateLabel = "3 days ago"
                ),
                CompanionReview(
                    id = "rev_2",
                    reviewerName = "Kabir S.",
                    reviewerAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=300&q=80",
                    rating = 4.9f,
                    comment = "Punctual, elegant, and wonderful company for our art exhibition evening.",
                    dateLabel = "1 week ago"
                )
            )
        ),
        DateMateCompanionProfile(
            id = "comp_kiara_02",
            name = "Kiara Oberoi",
            age = 25,
            city = "New Delhi",
            distanceKm = 6,
            tagline = "Fashion stylist & culinary explorer who loves laughter 🌹",
            aboutMe = "Editorial stylist based in South Delhi. Whether we are discovering a hidden speakeasy, attending a cultural gala, or sharing tiramisu at a cozy cafe, I bring warmth and effortless charm.",
            isVerified = true,
            isOnline = true,
            isPopular = true,
            rating = 4.94f,
            reviewCount = 54,
            pricePerHourInr = 2500,
            primaryPhotoUrl = "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&w=1000&q=85",
            galleryPhotos = listOf(
                "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&w=1000&q=85",
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=1000&q=85",
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=1000&q=85",
            ),
            interests = listOf("Fine Dining", "Luxury Events", "Wine Tasting", "Theatre & Cinema"),
            hobbies = listOf("Haute Couture", "Mediterranean Cooking", "Classical Piano", "Travel"),
            languages = listOf("English", "Hindi", "Punjabi"),
            personalityTraits = listOf("Charismatic", "Poised", "Playful", "Cultured"),
            availabilityDays = listOf("Tue", "Thu", "Fri", "Sat", "Sun"),
            availableTimeSlots = listOf("03:00 PM", "07:30 PM", "09:30 PM"),
            reviews = listOf(
                CompanionReview(
                    id = "rev_3",
                    reviewerName = "Vikramaditya",
                    reviewerAvatar = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&w=300&q=80",
                    rating = 5.0f,
                    comment = "Kiara made my business gala evening truly memorable. Impeccable etiquette and warmth.",
                    dateLabel = "5 days ago"
                )
            )
        ),
        DateMateCompanionProfile(
            id = "comp_meera_03",
            name = "Meera Nair",
            age = 23,
            city = "Bengaluru",
            distanceKm = 3,
            tagline = "Indie musician, bookstore hopper & sunset lover ☕",
            aboutMe = "UX designer by day and acoustic vocalist by evening in Indiranagar. I adore quiet garden cafes, deep conversations about life and cinema, and spontaneous dessert runs.",
            isVerified = true,
            isOnline = true,
            isPopular = true,
            rating = 4.91f,
            reviewCount = 39,
            pricePerHourInr = 1800,
            primaryPhotoUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1000&q=85",
            galleryPhotos = listOf(
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1000&q=85",
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=1000&q=85",
                "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=1000&q=85",
            ),
            interests = listOf("Coffee Dates", "Bookstores", "Live Jazz", "Sunset Walks"),
            hobbies = listOf("Acoustic Guitar", "Indie Vinyl Collecting", "Botanical Sketching"),
            languages = listOf("English", "Hindi", "Malayalam"),
            personalityTraits = listOf("Gentle", "Creative", "Authentic", "Thoughtful"),
            availabilityDays = listOf("Mon", "Wed", "Fri", "Sat", "Sun"),
            availableTimeSlots = listOf("12:30 PM", "05:30 PM", "07:30 PM"),
            reviews = listOf(
                CompanionReview(
                    id = "rev_4",
                    reviewerName = "Siddharth R.",
                    reviewerAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=300&q=80",
                    rating = 4.9f,
                    comment = "Such a refreshing and genuine soul. Our bookstore & coffee walk was the highlight of my week.",
                    dateLabel = "2 days ago"
                )
            )
        ),
        DateMateCompanionProfile(
            id = "comp_zoya_04",
            name = "Zoya Merchant",
            age = 26,
            city = "Mumbai",
            distanceKm = 7,
            tagline = "Sommelier & travel storyteller with a radiant smile 🥂",
            aboutMe = "Certified wine sommelier and travel writer in Colaba. If you appreciate old-world architecture, sea-breeze walks, and effortless laughter over candlelight, we will get along wonderfully.",
            isVerified = true,
            isOnline = false,
            isPopular = true,
            rating = 4.95f,
            reviewCount = 47,
            pricePerHourInr = 2400,
            primaryPhotoUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=1000&q=85",
            galleryPhotos = listOf(
                "https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=1000&q=85",
                "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&w=1000&q=85",
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=1000&q=85",
            ),
            interests = listOf("Wine Tasting", "Fine Dining", "Travel Stories", "Sunset Walks"),
            hobbies = listOf("Vineyard Tours", "Sailing", "Food Journalism"),
            languages = listOf("English", "Hindi", "Italian"),
            personalityTraits = listOf("Sophisticated", "Warm", "Adventurous", "Attentive"),
            availabilityDays = listOf("Thu", "Fri", "Sat", "Sun"),
            availableTimeSlots = listOf("07:30 PM", "08:30 PM", "09:30 PM"),
            reviews = listOf(
                CompanionReview(
                    id = "rev_5",
                    reviewerName = "Devansh K.",
                    reviewerAvatar = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=300&q=80",
                    rating = 5.0f,
                    comment = "Zoya knows every hidden gem in South Mumbai. Brilliant conversation and poise.",
                    dateLabel = "1 week ago"
                )
            )
        ),
        DateMateCompanionProfile(
            id = "comp_riya_05",
            name = "Riya Deshmukh",
            age = 24,
            city = "Pune",
            distanceKm = 9,
            tagline = "Classical dancer & psychology postgrad who loves deep talks 🌸",
            aboutMe = "Koregaon Park resident with a passion for mindfulness, performing arts, and cozy rooftop dinners. I love learning what inspires people and sharing serene, unhurried moments.",
            isVerified = true,
            isOnline = true,
            isPopular = false,
            rating = 4.89f,
            reviewCount = 31,
            pricePerHourInr = 1700,
            primaryPhotoUrl = "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=1000&q=85",
            galleryPhotos = listOf(
                "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=1000&q=85",
                "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=1000&q=85",
            ),
            interests = listOf("Coffee Dates", "Theatre & Cinema", "Art Galleries", "Bookstores"),
            hobbies = listOf("Kathak Dance", "Poetry", "Artisanal Tea"),
            languages = listOf("English", "Hindi", "Marathi"),
            personalityTraits = listOf("Calm", "Expressive", "Kind", "Intellectual"),
            availabilityDays = listOf("Wed", "Fri", "Sat", "Sun"),
            availableTimeSlots = listOf("03:00 PM", "05:30 PM", "07:30 PM"),
            reviews = listOf(
                CompanionReview(
                    id = "rev_6",
                    reviewerName = "Arjun P.",
                    reviewerAvatar = "https://images.unsplash.com/photo-1492562080023-ab3db95bfbce?auto=format&fit=crop&w=300&q=80",
                    rating = 4.9f,
                    comment = "Warm, graceful, and genuinely engaging throughout our evening.",
                    dateLabel = "4 days ago"
                )
            )
        ),
        DateMateCompanionProfile(
            id = "comp_tara_06",
            name = "Tara Sengupta",
            age = 27,
            city = "Hyderabad",
            distanceKm = 11,
            tagline = "Architect & photographer capturing golden hour stories 📸",
            aboutMe = "Design studio founder in Jubilee Hills. I love heritage walks, jazz lounges, and discovering new tasting menus with someone who enjoys good humor and elegance.",
            isVerified = true,
            isOnline = true,
            isPopular = true,
            rating = 4.93f,
            reviewCount = 44,
            pricePerHourInr = 2100,
            primaryPhotoUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=1000&q=85",
            galleryPhotos = listOf(
                "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=1000&q=85",
                "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1000&q=85",
            ),
            interests = listOf("Photography", "Fine Dining", "Luxury Events", "Travel Stories"),
            hobbies = listOf("Architectural Sketching", "Analog Cameras", "Jazz Records"),
            languages = listOf("English", "Hindi", "Bengali"),
            personalityTraits = listOf("Observant", "Elegant", "Witty", "Warm"),
            availabilityDays = listOf("Tue", "Thu", "Sat", "Sun"),
            availableTimeSlots = listOf("05:30 PM", "07:30 PM", "08:30 PM"),
            reviews = listOf(
                CompanionReview(
                    id = "rev_7",
                    reviewerName = "Nikhil V.",
                    reviewerAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=300&q=80",
                    rating = 5.0f,
                    comment = "Tara has an extraordinary eye for art and conversation. Highly recommended!",
                    dateLabel = "6 days ago"
                )
            )
        ),
    )

    val initialBookings: List<DateBooking> = listOf(
        DateBooking(
            id = "bk_1001",
            companionId = "comp_aanya_01",
            companionName = "Aanya Sharma",
            companionPhotoUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=1000&q=85",
            companionCity = "Mumbai",
            dateLabel = "Sat, 24 May",
            timeSlot = "07:30 PM",
            durationHours = 3,
            venueExperience = "Fine Dining & Lounge",
            specialNote = "Window table reserved at The Table, Colaba.",
            totalFeeInr = 6600,
            status = BookingStatus.CONFIRMED,
            confirmationCode = "DM-84920",
        ),
        DateBooking(
            id = "bk_1002",
            companionId = "comp_kiara_02",
            companionName = "Kiara Oberoi",
            companionPhotoUrl = "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&w=1000&q=85",
            companionCity = "New Delhi",
            dateLabel = "Wed, 28 May",
            timeSlot = "05:30 PM",
            durationHours = 2,
            venueExperience = "Artisanal Coffee & Gallery Walk",
            specialNote = "Afternoon gallery preview at Lodhi Art District.",
            totalFeeInr = 5000,
            status = BookingStatus.PENDING,
            confirmationCode = "DM-85104",
        ),
        DateBooking(
            id = "bk_1003",
            companionId = "comp_meera_03",
            companionName = "Meera Nair",
            companionPhotoUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1000&q=85",
            companionCity = "Bengaluru",
            dateLabel = "Last Sunday",
            timeSlot = "06:00 PM",
            durationHours = 2,
            venueExperience = "Cozy Bookstore & Wine Bar",
            specialNote = "Evening acoustic session and coffee.",
            totalFeeInr = 3600,
            status = BookingStatus.COMPLETED,
            confirmationCode = "DM-79311",
        ),
    )

    val initialThreads: List<DateMateChatThread> = listOf(
        DateMateChatThread(
            id = "th_aanya",
            companionId = "comp_aanya_01",
            companionName = "Aanya Sharma",
            companionAge = 24,
            companionCity = "Mumbai",
            companionPhotoUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=1000&q=85",
            isOnline = true,
            isVerified = true,
            lastMessage = "I would love to visit that new jazz café in Bandra this Saturday! ❤️",
            lastUpdatedLabel = "2m ago",
            unreadCount = 1,
        ),
        DateMateChatThread(
            id = "th_kiara",
            companionId = "comp_kiara_02",
            companionName = "Kiara Oberoi",
            companionAge = 25,
            companionCity = "New Delhi",
            companionPhotoUrl = "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&w=1000&q=85",
            isOnline = true,
            isVerified = true,
            lastMessage = "Thank you for the booking request! Looking forward to Wednesday ✨",
            lastUpdatedLabel = "18m ago",
            unreadCount = 0,
        ),
        DateMateChatThread(
            id = "th_meera",
            companionId = "comp_meera_03",
            companionName = "Meera Nair",
            companionAge = 23,
            companionCity = "Bengaluru",
            companionPhotoUrl = "https://images.unsplash.com/photo-1517841905240-472988babdf9?auto=format&fit=crop&w=1000&q=85",
            isOnline = true,
            isVerified = true,
            lastMessage = "That bookstore recommendation was wonderful, thank you!",
            lastUpdatedLabel = "Yesterday",
            unreadCount = 0,
        ),
    )

    val initialMessagesByThread: Map<String, List<DateMateMessage>> = mapOf(
        "th_aanya" to listOf(
            DateMateMessage(
                id = "m_1",
                threadId = "th_aanya",
                senderIsMe = false,
                senderName = "Aanya Sharma",
                text = "Hi Aarav! So lovely to match with you on DateMate 🌸",
                timestampLabel = "06:40 PM",
                reactions = listOf("❤️"),
            ),
            DateMateMessage(
                id = "m_2",
                threadId = "th_aanya",
                senderIsMe = true,
                senderName = "You",
                text = "Hi Aanya! Your art curation work sounds fascinating. Are you free this Saturday evening?",
                timestampLabel = "06:42 PM",
                isRead = true,
            ),
            DateMateMessage(
                id = "m_3",
                threadId = "th_aanya",
                senderIsMe = false,
                senderName = "Aanya Sharma",
                text = "I would love to visit that new jazz café in Bandra this Saturday! ❤️",
                timestampLabel = "06:44 PM",
                reactions = listOf("😍"),
            ),
        ),
        "th_kiara" to listOf(
            DateMateMessage(
                id = "m_4",
                threadId = "th_kiara",
                senderIsMe = true,
                senderName = "You",
                text = "Hello Kiara, I just scheduled our gallery walk for Wednesday afternoon.",
                timestampLabel = "04:15 PM",
                isRead = true,
            ),
            DateMateMessage(
                id = "m_5",
                threadId = "th_kiara",
                senderIsMe = false,
                senderName = "Kiara Oberoi",
                text = "Thank you for the booking request! Looking forward to Wednesday ✨",
                timestampLabel = "04:18 PM",
            ),
        ),
        "th_meera" to listOf(
            DateMateMessage(
                id = "m_6",
                threadId = "th_meera",
                senderIsMe = false,
                senderName = "Meera Nair",
                text = "That bookstore recommendation was wonderful, thank you!",
                timestampLabel = "Yesterday",
                reactions = listOf("🌹"),
            ),
        ),
    )

    val initialNotifications: List<DateMateNotification> = listOf(
        DateMateNotification(
            id = "notif_1",
            category = NotificationCategory.NEW_MATCH,
            title = "It's a Romantic Match! ❤️",
            body = "You and Aanya Sharma both liked each other. Start a conversation now!",
            timestampLabel = "5m ago",
            avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=400&q=80",
            targetCompanionId = "comp_aanya_01",
            targetThreadId = "th_aanya",
            isRead = false,
        ),
        DateMateNotification(
            id = "notif_2",
            category = NotificationCategory.BOOKING_CONFIRMATION,
            title = "Date Booking Confirmed ✨",
            body = "Your Fine Dining date with Aanya Sharma on Sat, 24 May at 07:30 PM is confirmed (Code: DM-84920).",
            timestampLabel = "25m ago",
            avatarUrl = "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=400&q=80",
            targetCompanionId = "comp_aanya_01",
            isRead = false,
        ),
        DateMateNotification(
            id = "notif_3",
            category = NotificationCategory.NEW_MESSAGE,
            title = "New Message from Kiara Oberoi",
            body = "\"Thank you for the booking request! Looking forward to Wednesday ✨\"",
            timestampLabel = "1h ago",
            avatarUrl = "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&w=400&q=80",
            targetCompanionId = "comp_kiara_02",
            targetThreadId = "th_kiara",
            isRead = false,
        ),
        DateMateNotification(
            id = "notif_4",
            category = NotificationCategory.NEW_LIKE,
            title = "Tara Sengupta Liked Your Profile",
            body = "Tara Sengupta (27, Hyderabad) sent you a Super Rose Like.",
            timestampLabel = "3h ago",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?auto=format&fit=crop&w=400&q=80",
            targetCompanionId = "comp_tara_06",
            isRead = true,
        ),
        DateMateNotification(
            id = "notif_5",
            category = NotificationCategory.BOOKING_REQUEST,
            title = "Booking Request Sent",
            body = "Your gallery date request with Kiara Oberoi (DM-85104) is awaiting final venue confirmation.",
            timestampLabel = "5h ago",
            avatarUrl = "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&w=400&q=80",
            targetCompanionId = "comp_kiara_02",
            isRead = true,
        ),
        DateMateNotification(
            id = "notif_6",
            category = NotificationCategory.PROFILE_VERIFICATION,
            title = "18+ & Identity Badge Verified",
            body = "Your DateMate Trust & Verification badge is active on your profile.",
            timestampLabel = "1d ago",
            isRead = true,
        ),
        DateMateNotification(
            id = "notif_7",
            category = NotificationCategory.MEMBERSHIP_UPDATE,
            title = "DateMate Gold Privileges Active",
            body = "Enjoy unlimited priority chats, Super Likes, and complimentary date rescheduling.",
            timestampLabel = "2d ago",
            isRead = true,
        ),
    )
}
