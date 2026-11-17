package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.GalleryImage
import com.example.data.model.SavedBookmark
import com.example.data.preferences.NotifiedImagesStore
import com.example.data.preferences.StoredTestUpload
import com.example.notifications.PixoraNotificationManager
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
}

