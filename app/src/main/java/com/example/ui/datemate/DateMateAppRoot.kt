package com.example.ui.datemate

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.BookingStatus
import com.example.data.model.DateMateTab
import com.example.ui.theme.DateMateBackground

private enum class SecondaryDestination {
    NONE,
    COMPANION_DETAIL,
    CHAT_ROOM,
    MEMBERSHIP,
    NOTIFICATIONS,
}

@Composable
fun DateMateAppRoot(
    viewModel: DateMateViewModel = viewModel(),
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    val hasCompletedSplash by viewModel.hasCompletedSplash.collectAsStateWithLifecycle()
    val hasCompletedOnboarding by viewModel.hasCompletedOnboarding.collectAsStateWithLifecycle()
    val isAuthenticated by viewModel.isAuthenticated.collectAsStateWithLifecycle()

    val selectedCity by viewModel.selectedHomeCity.collectAsStateWithLifecycle()
    val homeSearchQuery by viewModel.homeSearchQuery.collectAsStateWithLifecycle()
    val companions by viewModel.companions.collectAsStateWithLifecycle()
    val favoriteIds by viewModel.favoriteIds.collectAsStateWithLifecycle()
    val blockedIds by viewModel.blockedCompanionIds.collectAsStateWithLifecycle()
    val discoverFilters by viewModel.discoverFilters.collectAsStateWithLifecycle()
    val likedIdsInSession by viewModel.likedIdsInSession.collectAsStateWithLifecycle()
    val heartExplosionTrigger by viewModel.heartExplosionTrigger.collectAsStateWithLifecycle()
    val lastMatchedCompanion by viewModel.lastMatchedCompanion.collectAsStateWithLifecycle()

    val bookings by viewModel.bookings.collectAsStateWithLifecycle()
    val bookingDraft by viewModel.bookingDraft.collectAsStateWithLifecycle()
    val showBookingWizard by viewModel.showBookingWizard.collectAsStateWithLifecycle()

    val chatThreads by viewModel.chatThreads.collectAsStateWithLifecycle()
    val messagesByThread by viewModel.messagesByThread.collectAsStateWithLifecycle()
    val notifications by viewModel.notifications.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val activeCall by viewModel.activeCall.collectAsStateWithLifecycle()

    var selectedTab by rememberSaveable { mutableStateOf(DateMateTab.HOME) }
    var secondaryDestination by rememberSaveable { mutableStateOf(SecondaryDestination.NONE) }
    var selectedCompanionId by rememberSaveable { mutableStateOf("") }
    var selectedChatThreadId by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val visibleCompanions = remember(companions, blockedIds) {
        companions.filterNot { it.id in blockedIds }
    }
    val discoverDeck = remember(
        visibleCompanions,
        discoverFilters,
        likedIdsInSession,
        blockedIds
    ) {
        viewModel.getDiscoverDeck()
    }
    val unreadChatCount = remember(chatThreads) {
        chatThreads.sumOf { it.unreadCount }
    }
    val unreadNotificationCount = remember(notifications) {
        notifications.count { !it.isRead }
    }
    val pendingBookingCount = remember(bookings) {
        bookings.count { it.status == BookingStatus.PENDING || it.status == BookingStatus.CONFIRMED }
    }

    // 1. Animated 3D Splash Screen
    if (!hasCompletedSplash) {
        DateMateSplashScreen(
            onSplashFinished = viewModel::completeSplash
        )
        return
    }

    // 2. 3-Screen Romantic Onboarding Flow
    if (!hasCompletedOnboarding) {
        DateMateOnboardingScreen(
            onFinishOnboarding = viewModel::completeOnboarding
        )
        return
    }

    // 3. Authentication Screen
    if (!isAuthenticated) {
        Box(modifier = Modifier.fillMaxSize()) {
            DateMateAuthenticationScreen(
                onAuthenticated = { fullName, identifier, dob ->
                    viewModel.completeAuthentication(
                        fullName = fullName,
                        emailOrMobile = identifier,
                        dob = dob
                    )
                },
                onGoogleSignIn = { viewModel.signInWithGoogle(context) },
                onShowMessage = viewModel::emitMessage,
            )
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }
        return
    }

    // 4. Main Customer Application Shell
    val isTopLevelScreen = secondaryDestination == SecondaryDestination.NONE

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = DateMateBackground,
            contentWindowInsets = if (isTopLevelScreen) WindowInsets.safeDrawing else WindowInsets(0, 0, 0, 0),
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (secondaryDestination) {
                    SecondaryDestination.COMPANION_DETAIL -> {
                        val companion = viewModel.findCompanionById(selectedCompanionId)
                            ?: visibleCompanions.firstOrNull()
                        if (companion != null) {
                            DateMateProfileDetailScreen(
                                profile = companion,
                                isFavorite = companion.id in favoriteIds,
                                onBack = { secondaryDestination = SecondaryDestination.NONE },
                                onToggleFavorite = { viewModel.toggleFavorite(companion.id) },
                                onChatNow = {
                                    val threadId = viewModel.ensureChatThreadForCompanion(companion)
                                    viewModel.markThreadRead(threadId)
                                    selectedChatThreadId = threadId
                                    secondaryDestination = SecondaryDestination.CHAT_ROOM
                                },
                                onBookNow = {
                                    viewModel.openBookingWizard(preselectedCompanionId = companion.id)
                                },
                                onReportProfile = { reason ->
                                    viewModel.reportCompanion(companion.id, reason)
                                },
                                onBlockProfile = {
                                    viewModel.blockCompanion(companion.id)
                                    secondaryDestination = SecondaryDestination.NONE
                                }
                            )
                        } else {
                            secondaryDestination = SecondaryDestination.NONE
                        }
                    }

                    SecondaryDestination.CHAT_ROOM -> {
                        val activeThread = chatThreads.firstOrNull { it.id == selectedChatThreadId }
                            ?: chatThreads.firstOrNull()
                        if (activeThread != null) {
                            val messages = messagesByThread[activeThread.id].orEmpty()
                            DateMateChatRoomScreen(
                                thread = activeThread,
                                messages = messages,
                                onBack = { secondaryDestination = SecondaryDestination.NONE },
                                onSendMessage = { text, imageUrl, voiceSec, replyTo ->
                                    viewModel.sendChatMessage(
                                        threadId = activeThread.id,
                                        text = text,
                                        imageUrl = imageUrl,
                                        voiceDurationSec = voiceSec,
                                        replyToPreview = replyTo,
                                    )
                                },
                                onToggleReaction = { msgId, emoji ->
                                    viewModel.toggleMessageReaction(activeThread.id, msgId, emoji)
                                },
                                onDeleteMessage = { msgId ->
                                    viewModel.deleteMessage(activeThread.id, msgId)
                                },
                                onStartAudioCall = {
                                    viewModel.startCompanionCall(
                                        companionId = activeThread.companionId,
                                        callType = ActiveCallType.AUDIO
                                    )
                                },
                                onStartVideoCall = {
                                    viewModel.startCompanionCall(
                                        companionId = activeThread.companionId,
                                        callType = ActiveCallType.VIDEO
                                    )
                                },
                                onBookDateWithCompanion = {
                                    viewModel.openBookingWizard(preselectedCompanionId = activeThread.companionId)
                                },
                                onBlockCompanion = {
                                    viewModel.blockCompanion(activeThread.companionId)
                                    secondaryDestination = SecondaryDestination.NONE
                                },
                                onReportCompanion = {
                                    viewModel.reportCompanion(
                                        activeThread.companionId,
                                        "Reported from Chat"
                                    )
                                }
                            )
                        } else {
                            secondaryDestination = SecondaryDestination.NONE
                        }
                    }

                    SecondaryDestination.MEMBERSHIP -> {
                        DateMateMembershipScreen(
                            currentTier = userProfile.membershipTier,
                            currentIsYearly = userProfile.isYearlyBilling,
                            onSelectPlan = { tier, yearly ->
                                viewModel.selectMembershipPlan(tier, yearly)
                            },
                            onBack = { secondaryDestination = SecondaryDestination.NONE }
                        )
                    }

                    SecondaryDestination.NOTIFICATIONS -> {
                        DateMateNotificationsScreen(
                            notifications = notifications,
                            onMarkAllRead = viewModel::markAllNotificationsRead,
                            onNotificationClick = { notif ->
                                viewModel.markNotificationRead(notif.id)
                                when {
                                    notif.targetThreadId.isNotBlank() -> {
                                        selectedChatThreadId = notif.targetThreadId
                                        secondaryDestination = SecondaryDestination.CHAT_ROOM
                                    }
                                    notif.targetCompanionId.isNotBlank() -> {
                                        selectedCompanionId = notif.targetCompanionId
                                        secondaryDestination = SecondaryDestination.COMPANION_DETAIL
                                    }
                                    else -> {
                                        secondaryDestination = SecondaryDestination.NONE
                                    }
                                }
                            },
                            onBack = { secondaryDestination = SecondaryDestination.NONE }
                        )
                    }

                    SecondaryDestination.NONE -> {
                        when (selectedTab) {
                            DateMateTab.HOME -> {
                                DateMateHomeScreen(
                                    userProfile = userProfile,
                                    selectedCity = selectedCity,
                                    searchQuery = homeSearchQuery,
                                    companions = visibleCompanions,
                                    favoriteIds = favoriteIds,
                                    unreadNotificationCount = unreadNotificationCount,
                                    onSelectCity = viewModel::selectHomeCity,
                                    onSearchQueryChange = viewModel::updateHomeSearchQuery,
                                    onOpenNotifications = {
                                        secondaryDestination = SecondaryDestination.NOTIFICATIONS
                                    },
                                    onOpenMyProfile = {
                                        selectedTab = DateMateTab.PROFILE
                                    },
                                    onBrowseProfilesClick = {
                                        selectedTab = DateMateTab.DISCOVER
                                    },
                                    onBookDateClick = { preselected ->
                                        viewModel.openBookingWizard(preselected?.id)
                                    },
                                    onChatNowClick = { companion ->
                                        if (companion != null) {
                                            val threadId = viewModel.ensureChatThreadForCompanion(companion)
                                            viewModel.markThreadRead(threadId)
                                            selectedChatThreadId = threadId
                                            secondaryDestination = SecondaryDestination.CHAT_ROOM
                                        } else {
                                            selectedTab = DateMateTab.CHAT
                                        }
                                    },
                                    onOpenMembership = {
                                        secondaryDestination = SecondaryDestination.MEMBERSHIP
                                    },
                                    onOpenCompanionDetail = { companion ->
                                        selectedCompanionId = companion.id
                                        secondaryDestination = SecondaryDestination.COMPANION_DETAIL
                                    },
                                    onToggleFavorite = viewModel::toggleFavorite,
                                )
                            }

                            DateMateTab.DISCOVER -> {
                                DateMateDiscoverScreen(
                                    deck = discoverDeck,
                                    filters = discoverFilters,
                                    onUpdateFilters = viewModel::updateDiscoverFilters,
                                    onResetFilters = viewModel::resetDiscoverFilters,
                                    onPassProfile = viewModel::passCompanionOnDiscover,
                                    onLikeProfile = { companionId, isSuperLike ->
                                        viewModel.likeCompanionOnDiscover(companionId, isSuperLike)
                                    },
                                    onOpenProfileDetail = { companion ->
                                        selectedCompanionId = companion.id
                                        secondaryDestination = SecondaryDestination.COMPANION_DETAIL
                                    }
                                )
                            }

                            DateMateTab.BOOKINGS -> {
                                DateMateBookingsScreen(
                                    bookings = bookings,
                                    companions = visibleCompanions,
                                    onOpenNewBookingWizard = {
                                        viewModel.openBookingWizard(null)
                                    },
                                    onCancelBooking = viewModel::cancelBooking,
                                    onOpenChatForCompanion = { companionId ->
                                        val companion = viewModel.findCompanionById(companionId)
                                        if (companion != null) {
                                            val threadId = viewModel.ensureChatThreadForCompanion(companion)
                                            viewModel.markThreadRead(threadId)
                                            selectedChatThreadId = threadId
                                            secondaryDestination = SecondaryDestination.CHAT_ROOM
                                        }
                                    },
                                    onOpenCompanionProfile = { companionId ->
                                        selectedCompanionId = companionId
                                        secondaryDestination = SecondaryDestination.COMPANION_DETAIL
                                    }
                                )
                            }

                            DateMateTab.CHAT -> {
                                DateMateChatListScreen(
                                    threads = chatThreads,
                                    companions = visibleCompanions,
                                    onOpenThread = { thread ->
                                        viewModel.markThreadRead(thread.id)
                                        selectedChatThreadId = thread.id
                                        secondaryDestination = SecondaryDestination.CHAT_ROOM
                                    },
                                    onStartChatWithCompanion = { companion ->
                                        val threadId = viewModel.ensureChatThreadForCompanion(companion)
                                        viewModel.markThreadRead(threadId)
                                        selectedChatThreadId = threadId
                                        secondaryDestination = SecondaryDestination.CHAT_ROOM
                                    },
                                    onDeleteThread = viewModel::deleteChatThread,
                                )
                            }

                            DateMateTab.PROFILE -> {
                                val favoriteCompanions = remember(companions, favoriteIds) {
                                    companions.filter { it.id in favoriteIds }
                                }
                                val blockedCompanions = remember(companions, blockedIds) {
                                    companions.filter { it.id in blockedIds }
                                }
                                DateMateMyProfileScreen(
                                    userProfile = userProfile,
                                    favoriteCompanions = favoriteCompanions,
                                    blockedCompanions = blockedCompanions,
                                    onUpdateProfile = viewModel::updateUserProfile,
                                    onAddProfilePhoto = viewModel::addUploadedProfilePhoto,
                                    onOpenMembership = {
                                        secondaryDestination = SecondaryDestination.MEMBERSHIP
                                    },
                                    onOpenNotifications = {
                                        secondaryDestination = SecondaryDestination.NOTIFICATIONS
                                    },
                                    onOpenCompanionDetail = { companion ->
                                        selectedCompanionId = companion.id
                                        secondaryDestination = SecondaryDestination.COMPANION_DETAIL
                                    },
                                    onUnblockCompanion = viewModel::unblockCompanion,
                                    onLogout = viewModel::logoutUser,
                                    onDeleteAccount = viewModel::deleteUserAccount,
                                    onShowMessage = viewModel::emitMessage,
                                )
                            }
                        }
                    }
                }
            }
        }

        // Floating 5-Tab Bottom Navigation Bar on Top-Level Tabs
        if (isTopLevelScreen) {
            DateMateFloatingBottomBar(
                selectedTab = selectedTab,
                onSelectTab = { tab ->
                    selectedTab = tab
                    secondaryDestination = SecondaryDestination.NONE
                },
                unreadChatCount = unreadChatCount,
                pendingBookingCount = pendingBookingCount,
                modifier = Modifier.align(Alignment.BottomCenter)
            )
        }

        // 3D Heart Explosion Particle Overlay
        HeartExplosionOverlay(
            triggerTimestamp = heartExplosionTrigger,
            modifier = Modifier.fillMaxSize()
        )

        // Match Celebration Modal
        MatchCelebrationDialog(
            companion = lastMatchedCompanion,
            onDismiss = viewModel::dismissMatchCelebration,
            onStartChat = { companion ->
                viewModel.dismissMatchCelebration()
                val threadId = viewModel.ensureChatThreadForCompanion(companion)
                viewModel.markThreadRead(threadId)
                selectedChatThreadId = threadId
                secondaryDestination = SecondaryDestination.CHAT_ROOM
            },
            onBookDate = { companion ->
                viewModel.dismissMatchCelebration()
                viewModel.openBookingWizard(preselectedCompanionId = companion.id)
            }
        )

        // 6-Step Booking Wizard Bottom Sheet
        if (showBookingWizard) {
            SixStepBookingWizardSheet(
                draft = bookingDraft,
                companions = visibleCompanions,
                onUpdateDraft = viewModel::updateBookingDraft,
                onConfirmBooking = {
                    viewModel.confirmBookingFromDraft()
                    selectedTab = DateMateTab.BOOKINGS
                    secondaryDestination = SecondaryDestination.NONE
                },
                onDismiss = viewModel::closeBookingWizard,
            )
        }

        // Private Audio / Video Call Overlay
        CompanionCallOverlay(
            call = activeCall,
            onToggleMute = viewModel::toggleCallMute,
            onToggleSpeaker = viewModel::toggleCallSpeaker,
            onEndCall = viewModel::endCompanionCall,
        )
    }
}
