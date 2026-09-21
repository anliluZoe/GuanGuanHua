package com.guanguanhua.app.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals("上传一张放上桌面吧", WidgetCopy.EMPTY_PHOTO_HINT)
    }

    @Test
    fun captionColorDefaultsAndNormalizesHex() {
        assertEquals("#FFFFFF", WidgetCopy.DEFAULT_CAPTION_COLOR)
        assertEquals("#FFFFFF", WidgetCopy.normalizeCaptionColor(null))
        assertEquals("#FFFFFF", WidgetCopy.normalizeCaptionColor("  "))
        assertEquals("#FFFFFF", WidgetCopy.normalizeCaptionColor("#fff"))
        assertEquals("#F07A5C", WidgetCopy.normalizeCaptionColor("f07a5c"))
        assertEquals("#FF0077", WidgetCopy.normalizeCaptionColor("#f07"))
        assertEquals("#7EB8D8", WidgetCopy.normalizeCaptionColor("#7EB8D8"))
        assertNull(WidgetCopy.parseCaptionColorRgb("not-a-color"))
        assertNull(WidgetCopy.parseCaptionColorRgb("#GG0000"))
        assertEquals(0xFFF07A5C.toInt(), WidgetCopy.captionColorArgb("#F07A5C"))
        assertEquals(0xFFFFFFFF.toInt(), WidgetCopy.captionColorArgb("nope"))
    }
}
