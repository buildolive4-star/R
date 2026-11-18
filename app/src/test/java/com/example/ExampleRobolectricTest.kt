package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.firebase.GoogleAuthHelper
import com.example.data.model.AdminUserFilter
import com.example.data.model.CallStatus
import com.example.data.model.ChatMessage
import com.example.data.model.ChatThread
import com.example.data.model.GalleryImage
import com.example.data.model.LaunchBannerConfig
import com.example.data.model.LeadTag
import com.example.data.model.MessageTickState
import com.example.data.model.PresenceStatus
import com.example.data.model.ProfileTier
import com.example.data.model.SavedBookmark
import com.example.data.preferences.LocalTestUploadsRepository
import com.example.data.preferences.NotifiedImagesStore
import com.example.data.preferences.StoredTestUpload
import com.example.data.repository.UserCrmRepository
import com.example.notifications.OneSignalPushManager
import com.example.notifications.PixoraNotificationManager
import com.example.util.CallRingtonePlayer
import com.example.util.ContactLeakFilter
import com.example.util.ProfileCompletionCalculator
import com.google.firebase.Timestamp
import java.util.Date
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app name from context returns JustUs`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("JustUs", appName)
    }

    @Test
    fun `savedBookmark converts back to galleryImage with role and tier`() {
        val bookmark = SavedBookmark(
            imageId = "prof_1",
            title = "Aanya",
            category = "VIP",
            imageUrl = "https://i.ibb.co/test/aanya.jpg",
            thumbUrl = "https://i.ibb.co/test/aanya-thumb.jpg",
            aspectRatio = 16.0 / 9.0,
            price = "₹999",
            age = 23,
            role = "GF",
            tier = ProfileTier.VIP_16_9,
        )
        val gallery = bookmark.toGalleryImage()
        assertEquals("prof_1", gallery.id)
        assertEquals("Aanya", gallery.title)
        assertEquals(23, gallery.age)
        assertEquals("GF", gallery.role)
        assertTrue(gallery.isVip)
    }

    @Test
    fun `galleryImage clamps aspect ratio safely`() {
        val extreme = GalleryImage(aspectRatio = 10.0, imageUrl = "https://i.ibb.co/a/b.jpg")
        assertEquals(2.2f, extreme.safeAspectRatio)
    }

    @Test
    fun `storedTestUpload maps to published companion profile`() {
        val upload = StoredTestUpload(
            id = "prof_123",
            title = "Riya",
            category = "Companion",
            imageUrl = "https://i.ibb.co/abc/riya.jpg",
            thumbUrl = "https://i.ibb.co/abc/riya_thumb.jpg",
            aspectRatio = 9.0 / 16.0,
            createdAtEpochMs = 1_700_000_000_000L,
            price = "₹499",
            age = 22,
            role = "GF",
            tier = ProfileTier.GRID_9_16,
            featured = false,
        )
        val mapped = upload.toGalleryImage()
        assertEquals("prof_123", mapped.id)
        assertEquals("Riya", mapped.title)
        assertEquals(22, mapped.age)
        assertEquals("₹499", mapped.price)
        assertEquals("published", mapped.status)
        assertFalse(mapped.isVip)
    }

    @Test
    fun `notification manager registers profile, chat message, and voice call high importance channels`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PixoraNotificationManager.ensureChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val profileChannel = manager.getNotificationChannel(PixoraNotificationManager.CHANNEL_ID)
        assertNotNull(profileChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, profileChannel.importance)
        assertTrue(profileChannel.shouldVibrate())

        val messageChannel = manager.getNotificationChannel(PixoraNotificationManager.CHANNEL_MESSAGES_ID)
        assertNotNull(messageChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, messageChannel.importance)
        assertTrue(messageChannel.shouldVibrate())

        val callChannel = manager.getNotificationChannel(PixoraNotificationManager.CHANNEL_CALLS_ID)
        assertNotNull(callChannel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, callChannel.importance)
    }

    @Test
    fun `notifiedImagesStore tracks refresh-aware tokens and deduplicates chat epochs`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = NotifiedImagesStore(context)
        assertTrue(store.isBackgroundSyncEnabled())

        val token1 = NotifiedImagesStore.buildProfileAlertToken("prof_a", 100L)
        val token2 = NotifiedImagesStore.buildProfileAlertToken("prof_a", 200L)
        assertNotEquals(token1, token2)

        store.markIdsKnown(listOf(token1))
        val known = store.getKnownIds()
        assertTrue(known.contains(token1))
        assertFalse(known.contains(token2))

        store.markChatNotified("chat_1_client", 5000L)
        assertEquals(5000L, store.getLastNotifiedChatEpoch("chat_1_client"))
        store.markChatNotified("chat_1_client", 4000L)
        assertEquals(5000L, store.getLastNotifiedChatEpoch("chat_1_client"))

        store.setBackgroundSyncEnabled(false)
        assertFalse(store.isBackgroundSyncEnabled())
    }

    @Test
    fun `localTestUploadsRepository does not seed prototype images by default`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LocalTestUploadsRepository(context)
        repo.clearAll()
        assertTrue(repo.testUploads.first().isEmpty())
    }

    @Test
    fun `multi-photo hidden profile reveals and expires each photo independently`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LocalTestUploadsRepository(context)
        repo.clearAll()

        val multiHidden = StoredTestUpload(
            id = "hidden_multi",
            title = "Maya Multi",
            category = "VIP",
            imageUrl = "https://i.ibb.co/m/1.jpg",
            thumbUrl = "https://i.ibb.co/m/1.jpg",
            photoUrls = listOf(
                "https://i.ibb.co/m/1.jpg",
                "https://i.ibb.co/m/2.jpg",
            ),
            aspectRatio = 9.0 / 16.0,
            createdAtEpochMs = 1000L,
            price = "₹799",
            age = 22,
            role = "GF",
            tier = ProfileTier.GRID_9_16,
            isHidden = true,
        )
        repo.addUpload(multiHidden)

        val initial = repo.testUploads.first().first()
        assertEquals(2, initial.unopenedPhotoCount())
        assertEquals(0, initial.nextOpenablePhotoIndex())
        assertFalse(initial.isDeletedBlurred)

        // Reveal & expire photo 0 only -> photo 1 MUST still be openable on Home and Profile stays unblurred!
        val exp0 = repo.startHiddenRevealCountdown("hidden_multi", photoIndex = 0, ttlMs = 40_000L)
        repo.markExpiredUploads(nowMs = exp0 + 50L)

        val afterFirstExpired = repo.testUploads.first().first()
        assertTrue(afterFirstExpired.isPhotoExpired(0))
        assertFalse(afterFirstExpired.isPhotoExpired(1))
        assertFalse(afterFirstExpired.allPhotosExpired())
        assertFalse(afterFirstExpired.isDeletedBlurred)
        assertEquals(1, afterFirstExpired.unopenedPhotoCount())
        assertEquals(1, afterFirstExpired.nextOpenablePhotoIndex())

        // Now reveal & expire photo 1 -> NOW Home card turns into soft blurred memory!
        val exp1 = repo.startHiddenRevealCountdown("hidden_multi", photoIndex = 1, ttlMs = 40_000L)
        val expiredList = repo.markExpiredUploads(nowMs = exp1 + 50L)
        assertEquals(listOf("hidden_multi"), expiredList)

        val afterAllExpired = repo.testUploads.first().first()
        assertTrue(afterAllExpired.isPhotoExpired(0))
        assertTrue(afterAllExpired.isPhotoExpired(1))
        assertTrue(afterAllExpired.allPhotosExpired())
        assertTrue(afterAllExpired.isDeletedBlurred)

        // Refreshing resets ALL photos cleanly
        val refreshed = repo.refreshUpload("hidden_multi")
        assertNotNull(refreshed)
        val afterRefresh = repo.testUploads.first().first()
        assertFalse(afterRefresh.isDeletedBlurred)
        assertEquals(2, afterRefresh.unopenedPhotoCount())
        assertFalse(afterRefresh.isPhotoRevealStarted(0))
        assertFalse(afterRefresh.isPhotoRevealStarted(1))
    }

    @Test
    fun `admin can manually update profile price and metadata`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LocalTestUploadsRepository(context)
        repo.clearAll()

        repo.addUpload(
            StoredTestUpload(
                id = "edit_prof",
                title = "Ananya",
                category = "Companion",
                imageUrl = "https://i.ibb.co/a/1.jpg",
                thumbUrl = "https://i.ibb.co/a/1.jpg",
                aspectRatio = 9.0 / 16.0,
                createdAtEpochMs = 2000L,
                price = "₹499",
                age = 21,
            )
        )

        repo.updateUpload(
            imageId = "edit_prof",
            title = "Ananya VIP",
            description = "Evening concierge",
            category = "VIP",
            price = "₹2,499",
            tags = emptyList(),
            featured = true,
            isHidden = false,
            age = 23,
            role = "GF",
            tier = ProfileTier.VIP_16_9,
        )

        val updated = repo.testUploads.first().first()
        assertEquals("Ananya VIP", updated.title)
        assertEquals("₹2,499", updated.price)
        assertEquals(23, updated.age)
        assertTrue(updated.isVip)
    }

    @Test
    fun `webrtc admin callee sentinel matches rules expectations`() {
        assertEquals("__ADMIN__", CallStatus.ADMIN_CALLEE_UID)
        assertEquals("ringing", CallStatus.RINGING)
    }

    @Test
    fun `contactLeakFilter blocks phone email instagram handles and urls for staff`() {
        assertFalse(ContactLeakFilter.validateForStaff("Call me at 9876543210").isAllowed)
        assertFalse(ContactLeakFilter.validateForStaff("My email is girl@gmail.com").isAllowed)
        assertFalse(ContactLeakFilter.validateForStaff("Follow @riya_official on insta").isAllowed)
        assertFalse(ContactLeakFilter.validateForStaff("Ping me on Instagram please").isAllowed)
        assertFalse(ContactLeakFilter.validateForStaff("Check https://example.com").isAllowed)
        assertTrue(ContactLeakFilter.validateForStaff("Hi! Let us plan dinner at 8 PM.").isAllowed)
    }

    @Test
    fun `profileCompletionCalculator reaches 100 percent only when all fields complete`() {
        val partial = ProfileCompletionCalculator.calculate(
            name = "Rahul",
            photoUrl = "",
            phone = "9876543210",
            age = 24,
            address = "MG Road, Bengaluru",
            city = "",
            bio = "",
            interests = "",
            genderPreference = "",
        )
        assertTrue(partial.isReadyForChatOnboarding)
        assertFalse(partial.isReadyForStaffApplication)
        assertTrue(partial.percent in 40..60)

        val complete = ProfileCompletionCalculator.calculate(
            name = "Rahul Sharma",
            photoUrl = "https://i.ibb.co/abc/p.jpg",
            phone = "9876543210",
            age = 24,
            address = "MG Road, Bengaluru",
            city = "Bengaluru",
            bio = "Love coffee dates and weekend drives.",
            interests = "Coffee, Music",
            genderPreference = "Looking for GF",
        )
        assertEquals(100, complete.percent)
        assertTrue(complete.isReadyForStaffApplication)
    }

    @Test
    fun `crm filters include online notified old users and lead tags`() {
        val labels = AdminUserFilter.entries.map { it.label }
        assertTrue(labels.contains("Online"))
        assertTrue(labels.contains("Notified"))
        assertTrue(labels.contains("Old Users"))
        assertTrue(labels.contains("Hot Lead"))
        assertEquals("hot", LeadTag.HOT)
    }

    @Test
    fun `access code normalization strips invalid chars and uppercases`() {
        assertEquals("JU-4829", UserCrmRepository.normalizeAccessCode(" ju-4829 "))
        assertEquals("PARTNER_24", UserCrmRepository.normalizeAccessCode("partner_24!@#"))
    }

    @Test
    fun `auth diagnostic info reads web client id from google services resources`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val diag = GoogleAuthHelper.getAuthDiagnosticInfo(context)
        assertTrue(diag.hasWebClientId)
        assertEquals(GoogleAuthHelper.EXPECTED_DEBUG_SHA1_COLON, diag.expectedSha1Colon)
    }

    @Test
    fun `oneSignalPushManager safely skips initialization in robolectric without crashing`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        OneSignalPushManager.init(context)
        OneSignalPushManager.syncUserIdentity("uid_test", isAdmin = true, isApprovedStaff = false)
        assertFalse(OneSignalPushManager.isInitialized)
        assertEquals(
            OneSignalPushManager.isAppIdConfigured() && OneSignalPushManager.isRestKeyConfigured(),
            OneSignalPushManager.isFullyConfigured()
        )
        OneSignalPushManager.consumeDeepLinkChat()
        OneSignalPushManager.consumeDeepLinkImage()
    }

    @Test
    fun `chatThread tickStateForOutgoingMessage transitions cleanly across SENT, DELIVERED, and READ`() {
        val now = System.currentTimeMillis()
        val msg = ChatMessage(
            id = "m1",
            senderRole = "client",
            text = "Hello",
            createdAt = Timestamp(Date(now - 5_000L)),
        )
        val sentThread = ChatThread(id = "c1", clientUid = "u1")
        assertEquals(
            MessageTickState.SENT,
            sentThread.tickStateForOutgoingMessage(msg, viewerRole = "client", peerOnline = false)
        )
        assertEquals(
            MessageTickState.DELIVERED,
            sentThread.tickStateForOutgoingMessage(msg, viewerRole = "client", peerOnline = true)
        )

        val deliveredThread = sentThread.copy(agentLastDeliveredEpochMs = now - 1_000L)
        assertEquals(
            MessageTickState.DELIVERED,
            deliveredThread.tickStateForOutgoingMessage(msg, viewerRole = "client", peerOnline = false)
        )

        val readThread = deliveredThread.copy(agentLastReadEpochMs = now)
        assertEquals(
            MessageTickState.READ,
            readThread.tickStateForOutgoingMessage(msg, viewerRole = "client", peerOnline = false)
        )
    }

    @Test
    fun `presenceStatus detects stale heartbeats and formats WhatsApp status label`() {
        val now = 1_750_000_000_000L
        val onlineFresh = PresenceStatus(online = true, lastSeenEpochMs = now - 10_000L)
        assertTrue(onlineFresh.isEffectivelyOnline(now))
        assertEquals("Online", onlineFresh.formatPresenceLabel(now))

        val onlineStale = PresenceStatus(online = true, lastSeenEpochMs = now - 300_000L)
        assertFalse(onlineStale.isEffectivelyOnline(now))
        assertEquals("last seen 5m ago", onlineStale.formatPresenceLabel(now))

        val offlineJustNow = PresenceStatus(online = false, lastSeenEpochMs = now - 15_000L)
        assertFalse(offlineJustNow.isEffectivelyOnline(now))
        assertEquals("last seen just now", offlineJustNow.formatPresenceLabel(now))
    }

    @Test
    fun `launchBannerConfig isReadyToDisplay requires active flag and valid https url`() {
        assertFalse(LaunchBannerConfig(active = false, imageUrl = "https://i.ibb.co/x.jpg").isReadyToDisplay)
        assertFalse(LaunchBannerConfig(active = true, imageUrl = "").isReadyToDisplay)
        assertFalse(LaunchBannerConfig(active = true, imageUrl = "http://insecure.jpg").isReadyToDisplay)
        assertTrue(LaunchBannerConfig(active = true, imageUrl = "https://i.ibb.co/x.jpg").isReadyToDisplay)
    }

    @Test
    fun `callRingtonePlayer start and stop executes safely without throwing in Robolectric`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val player = CallRingtonePlayer(context)
        player.startIncomingRingtone()
        player.startOutgoingRingback()
        player.stopAll()
    }
}
