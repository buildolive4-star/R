package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.GalleryImage
import com.example.data.model.SavedBookmark
import com.example.data.preferences.LocalTestUploadsRepository
import com.example.data.preferences.NotifiedImagesStore
import com.example.data.preferences.StoredTestUpload
import com.example.notifications.PixoraNotificationManager
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
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Pixora", appName)
    }

    @Test
    fun `savedBookmark converts back to galleryImage`() {
        val bookmark = SavedBookmark(
            imageId = "img_1",
            title = "Sunset",
            category = "Nature",
            imageUrl = "https://i.ibb.co/test/sunset.jpg",
            thumbUrl = "https://i.ibb.co/test/sunset-thumb.jpg",
            aspectRatio = 1.5,
        )
        val gallery = bookmark.toGalleryImage()
        assertEquals("img_1", gallery.id)
        assertEquals("Sunset", gallery.title)
        assertEquals(1.5f, gallery.safeAspectRatio)
    }

    @Test
    fun `galleryImage clamps aspect ratio safely`() {
        val extreme = GalleryImage(aspectRatio = 10.0, imageUrl = "https://i.ibb.co/a/b.jpg")
        assertEquals(2.2f, extreme.safeAspectRatio)
    }

    @Test
    fun `storedTestUpload maps to published GalleryImage`() {
        val upload = StoredTestUpload(
            id = "test_123",
            title = "Neon Skyline",
            category = "Photography",
            imageUrl = "https://i.ibb.co/abc/neon.jpg",
            thumbUrl = "https://i.ibb.co/abc/neon_thumb.jpg",
            aspectRatio = 0.75,
            createdAtEpochMs = 1_700_000_000_000L,
        )
        val mapped = upload.toGalleryImage()
        assertEquals("test_123", mapped.id)
        assertEquals("Neon Skyline", mapped.title)
        assertEquals("Photography", mapped.category)
        assertEquals("published", mapped.status)
        assertTrue(mapped.featured)
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

        store.markIdsKnown(listOf("img_a", "img_b"))
        val known = store.getKnownIds()
        assertTrue(known.contains("img_a"))
        assertTrue(known.contains("img_b"))

        store.setBackgroundSyncEnabled(false)
        assertFalse(store.isBackgroundSyncEnabled())
    }

    @Test
    fun `localTestUploadsRepository adds and removes individual uploads`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LocalTestUploadsRepository(context)
        repo.clearAll()

        val upload1 = StoredTestUpload(
            id = "imgbb_1",
            title = "Photo 1",
            category = "Nature",
            imageUrl = "https://i.ibb.co/1/p1.jpg",
            thumbUrl = "https://i.ibb.co/1/p1_t.jpg",
            aspectRatio = 1.0,
            createdAtEpochMs = 1000L,
        )
        val upload2 = StoredTestUpload(
            id = "imgbb_2",
            title = "Photo 2",
            category = "Abstract",
            imageUrl = "https://i.ibb.co/2/p2.jpg",
            thumbUrl = "https://i.ibb.co/2/p2_t.jpg",
            aspectRatio = 1.2,
            createdAtEpochMs = 2000L,
        )

        repo.addUpload(upload1)
        repo.addUpload(upload2)
        assertEquals(2, repo.testUploads.first().size)

        repo.removeUpload("imgbb_1")
        val remaining = repo.testUploads.first()
        assertEquals(1, remaining.size)
        assertEquals("imgbb_2", remaining.first().id)

        repo.clearAll()
        assertEquals(0, repo.testUploads.first().size)
    }

    @Test
    fun `hidden photo starts 20s countdown on reveal and turns into blurred preview on expiry`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LocalTestUploadsRepository(context)
        repo.clearAll()

        val hiddenUpload = StoredTestUpload(
            id = "hidden_1",
            title = "Secret Shot",
            category = "Date Night",
            imageUrl = "https://i.ibb.co/sec/secret.jpg",
            thumbUrl = "https://i.ibb.co/sec/secret_t.jpg",
            aspectRatio = 1.0,
            createdAtEpochMs = 1000L,
            isHidden = true,
            revealExpiresAtEpochMs = 0L,
        )
        repo.addUpload(hiddenUpload)

        val beforeReveal = repo.testUploads.first().first()
        assertTrue(beforeReveal.isHidden)
        assertFalse(beforeReveal.isRevealStarted)
        assertFalse(beforeReveal.isDeletedBlurred)
        assertFalse(beforeReveal.featured)

        val expiry1 = repo.startHiddenRevealCountdown("hidden_1", ttlMs = 20_000L)
        assertTrue(expiry1 > System.currentTimeMillis())

        val expiry2 = repo.startHiddenRevealCountdown("hidden_1", ttlMs = 20_000L)
        assertEquals(expiry1, expiry2)

        val afterReveal = repo.testUploads.first().first()
        assertTrue(afterReveal.isRevealStarted)
        assertFalse(afterReveal.isDeletedBlurred)
        assertTrue(afterReveal.remainingSeconds() in 1..20)

        val expiredEarly = repo.markExpiredUploads(nowMs = expiry1 - 1000L)
        assertTrue(expiredEarly.isEmpty())

        val expiredAfter = repo.markExpiredUploads(nowMs = expiry1 + 100L)
        assertEquals(listOf("hidden_1"), expiredAfter)

        // Blurred residual card remains in the list with isExpired = true
        val blurredItem = repo.testUploads.first().first()
        assertTrue(blurredItem.isExpired)
        assertTrue(blurredItem.isDeletedBlurred)
        assertEquals(0, blurredItem.remainingSeconds())
        assertFalse(blurredItem.featured)

        // Admin clicks Refresh -> photo returns to unlockable state
        val refreshed = repo.refreshUpload("hidden_1")
        assertNotNull(refreshed)
        val afterRefresh = repo.testUploads.first().first()
        assertTrue(afterRefresh.isHidden)
        assertFalse(afterRefresh.isExpired)
        assertFalse(afterRefresh.isDeletedBlurred)
        assertFalse(afterRefresh.isRevealStarted)
        assertTrue(afterRefresh.refreshEpochMs > 0L)
    }

    @Test
    fun `starter moments seed at least 4 9_16 cards with prices and refreshAll resets expired`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = LocalTestUploadsRepository(context)
        repo.clearAll()

        repo.ensureStarterMomentsSeeded(context.packageName)
        val seeded = repo.testUploads.first()
        assertTrue(seeded.size >= 4)
        assertTrue(seeded.all { it.price.isNotBlank() })

        // Simulate expiry of the hidden starter item, then refreshAllExpiredUploads
        val hiddenStarter = seeded.first { it.isHidden }
        val exp = repo.startHiddenRevealCountdown(hiddenStarter.id, ttlMs = 1L)
        repo.markExpiredUploads(nowMs = exp + 100L)
        assertTrue(repo.testUploads.first().first { it.id == hiddenStarter.id }.isDeletedBlurred)

        val refreshedAll = repo.refreshAllExpiredUploads()
        assertEquals(1, refreshedAll.size)
        val resetItem = repo.testUploads.first().first { it.id == hiddenStarter.id }
        assertFalse(resetItem.isDeletedBlurred)
        assertFalse(resetItem.isRevealStarted)
    }
}

