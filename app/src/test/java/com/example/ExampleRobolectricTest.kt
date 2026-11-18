package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AdminUserFilter
import com.example.data.model.GalleryImage
import com.example.data.model.LeadTag
import com.example.data.model.ProfileTier
import com.example.data.model.SavedBookmark
import com.example.data.preferences.LocalTestUploadsRepository
import com.example.data.preferences.NotifiedImagesStore
import com.example.data.preferences.StoredTestUpload
import com.example.notifications.PixoraNotificationManager
import com.example.util.ContactLeakFilter
import com.example.util.ProfileCompletionCalculator
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    fun `notification manager registers high importance channel`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        PixoraNotificationManager.ensureChannel(context)
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = manager.getNotificationChannel(PixoraNotificationManager.CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertTrue(channel.shouldVibrate())
    }

    @Test
    fun `notifiedImagesStore tracks known IDs and background sync flag`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val store = NotifiedImagesStore(context)
        assertTrue(store.isBackgroundSyncEnabled())

        store.markIdsKnown(listOf("prof_a", "prof_b"))
        val known = store.getKnownIds()
        assertTrue(known.contains("prof_a"))
        assertTrue(known.contains("prof_b"))

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
    fun `hidden photo starts 40s countdown on reveal and turns into soft blurred preview on expiry`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LocalTestUploadsRepository(context)
        repo.clearAll()

        val hiddenUpload = StoredTestUpload(
            id = "hidden_40",
            title = "Secret Companion",
            category = "Date Night",
            imageUrl = "https://i.ibb.co/sec/secret.jpg",
            thumbUrl = "https://i.ibb.co/sec/secret_t.jpg",
            aspectRatio = 9.0 / 16.0,
            createdAtEpochMs = 1000L,
            price = "₹499",
            age = 22,
            role = "GF",
            tier = ProfileTier.GRID_9_16,
            isHidden = true,
            revealExpiresAtEpochMs = 0L,
        )
        repo.addUpload(hiddenUpload)

        val beforeReveal = repo.testUploads.first().first()
        assertTrue(beforeReveal.isHidden)
        assertFalse(beforeReveal.isRevealStarted)
        assertFalse(beforeReveal.isDeletedBlurred)
        assertEquals(40, beforeReveal.remainingSeconds())

        val expiry1 = repo.startHiddenRevealCountdown("hidden_40", ttlMs = 40_000L)
        assertTrue(expiry1 > System.currentTimeMillis())

        val afterReveal = repo.testUploads.first().first()
        assertTrue(afterReveal.isRevealStarted)
        assertFalse(afterReveal.isDeletedBlurred)
        assertTrue(afterReveal.remainingSeconds() in 1..40)

        val expiredEarly = repo.markExpiredUploads(nowMs = expiry1 - 1000L)
        assertTrue(expiredEarly.isEmpty())

        val expiredAfter = repo.markExpiredUploads(nowMs = expiry1 + 100L)
        assertEquals(listOf("hidden_40"), expiredAfter)

        val blurredItem = repo.testUploads.first().first()
        assertTrue(blurredItem.isExpired)
        assertTrue(blurredItem.isDeletedBlurred)
        assertEquals(0, blurredItem.remainingSeconds())

        val refreshed = repo.refreshUpload("hidden_40")
        assertNotNull(refreshed)
        val afterRefresh = repo.testUploads.first().first()
        assertFalse(afterRefresh.isExpired)
        assertFalse(afterRefresh.isDeletedBlurred)
        assertFalse(afterRefresh.isRevealStarted)
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
}
