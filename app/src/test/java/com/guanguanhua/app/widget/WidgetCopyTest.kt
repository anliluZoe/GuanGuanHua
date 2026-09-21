package com.guanguanhua.app.widget

import org.junit.Assert.assertEquals
import org.junit.Test

class WidgetCopyTest {

    @Test
    fun clampsCaptionToFortyTrimmedCharacters() {
        assertEquals("", WidgetCopy.clampCaption("   "))
        assertEquals("周末去看海", WidgetCopy.clampCaption("  周末去看海  "))
        assertEquals("啊".repeat(40), WidgetCopy.clampCaption("啊".repeat(48)))
    }

    @Test
    fun samplesBitmapDownToMaxEdge() {
        assertEquals(1, WidgetCopy.bitmapSampleSize(720, 720))
        assertEquals(1, WidgetCopy.bitmapSampleSize(400, 300))
        assertEquals(2, WidgetCopy.bitmapSampleSize(1440, 900))
        assertEquals(8, WidgetCopy.bitmapSampleSize(3000, 2000))
        assertEquals(1, WidgetCopy.bitmapSampleSize(0, 100))
    }

    @Test
    fun emptyPhotoCopyIsTheApprovedPlaceholder() {
        assertEquals("还没有照片", WidgetCopy.EMPTY_PHOTO)
        assertEquals("管管花", WidgetCopy.BRAND)
    }
}
