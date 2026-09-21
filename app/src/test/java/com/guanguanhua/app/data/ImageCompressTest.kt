package com.guanguanhua.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageCompressTest {

    @Test
    fun photoAndAvatarMaxEdgesMatchHandoff() {
        assertEquals(1280, ImageCompress.PHOTO_MAX_EDGE)
        assertEquals(1024, ImageCompress.AVATAR_MAX_EDGE)
        assertEquals("image/jpeg", ImageCompress.MIME_JPEG)
        assertTrue(ImageCompress.JPEG_QUALITY in 82..85)
    }

    @Test
    fun scaledSizeKeepsAspectAndCapsLongestEdge() {
        assertEquals(800 to 600, ImageCompress.scaledSize(800, 600, 1280))
        assertEquals(1280 to 640, ImageCompress.scaledSize(2000, 1000, 1280))
        assertEquals(1024 to 512, ImageCompress.scaledSize(2048, 1024, 1024))
        assertEquals(960 to 1280, ImageCompress.scaledSize(1920, 2560, 1280))
        assertEquals(1 to 1, ImageCompress.scaledSize(0, 10, 1280))
    }

    @Test
    fun decodeSampleSizeStopsBeforeDroppingBelowTarget() {
        assertEquals(1, ImageCompress.decodeSampleSize(1280, 720, 1280))
        assertEquals(1, ImageCompress.decodeSampleSize(2000, 1000, 1280))
        assertEquals(2, ImageCompress.decodeSampleSize(4000, 3000, 1280))
        assertEquals(1, ImageCompress.decodeSampleSize(0, 100, 1280))
    }
}
