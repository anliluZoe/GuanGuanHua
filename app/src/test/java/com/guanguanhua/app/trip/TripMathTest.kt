package com.guanguanhua.app.trip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset

class TripMathTest {

    @Test
    fun kindsAndRatingsStayInTheAgreedSet() {
        assertEquals(listOf("住宿", "美食", "风景", "博物馆", "杂物店"), TripMath.KINDS)
        assertTrue(TripMath.knownKind("杂物店"))
        assertNull(TripMath.ratingOrNull(0))
        assertEquals(5, TripMath.ratingOrNull(5))
    }

    @Test
    fun dayNumberCountsFromStartDate() {
        val start = LocalDate.of(2026, 10, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(1, TripMath.dayNumber(start, LocalDate.of(2026, 10, 1), ZoneOffset.UTC))
        assertEquals(3, TripMath.dayNumber(start, LocalDate.of(2026, 10, 3), ZoneOffset.UTC))
        assertEquals("10月1日至今", TripMath.formatRange(start, null, ZoneOffset.UTC))
        val end = LocalDate.of(2026, 10, 7).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("10月1日–10月7日", TripMath.formatRange(start, end, ZoneOffset.UTC))
    }

    @Test
    fun osmTagsPickAKind() {
        assertEquals("美食", TripMath.kindFromTags("restaurant", null, null))
        assertEquals("住宿", TripMath.kindFromTags(null, "hotel", null))
        assertEquals("博物馆", TripMath.kindFromTags(null, "museum", null))
        assertEquals("杂物店", TripMath.kindFromTags(null, null, "convenience"))
        assertEquals("风景", TripMath.kindFromTags(null, "viewpoint", null))
        assertNull(TripMath.kindFromTags("bank", null, null))
    }

    @Test
    fun endReminderFiresOnPlannedDayThenOnceMoreAfterTwoDays() {
        val planned = LocalDate.of(2026, 10, 7)
        assertNull(TripMath.nextEndReminder(planned, ended = false, today = LocalDate.of(2026, 10, 6), lastReminded = null))
        assertEquals(planned, TripMath.nextEndReminder(planned, false, planned, null))
        assertNull(TripMath.nextEndReminder(planned, false, planned.plusDays(1), planned))
        assertEquals(planned.plusDays(2), TripMath.nextEndReminder(planned, false, planned.plusDays(2), planned))
        assertNull(TripMath.nextEndReminder(planned, false, planned.plusDays(5), planned.plusDays(2)))
        assertNull(TripMath.nextEndReminder(planned, ended = true, today = planned, lastReminded = null))
        assertNull(TripMath.nextEndReminder(null, false, planned, null))
    }

    @Test
    fun routeMapFitsGuilinStopsInsideTheView() {
        val guilin = 25.273 to 110.290
        val yangshuo = 24.778 to 110.496
        val one = TripMath.mapFrame(listOf(guilin), 512, 260)
        val pixel = TripMath.mapPixel(guilin.first, guilin.second, one)
        assertTrue(pixel.first in 0f..512f)
        assertTrue(pixel.second in 0f..260f)
        val tiles = TripMath.mapTiles(one)
        assertTrue(tiles.isNotEmpty())
        assertTrue(tiles.first().url.contains("autonavi.com"))
        val two = TripMath.mapFrame(listOf(guilin, yangshuo), 800, 400)
        val start = TripMath.mapPixel(guilin.first, guilin.second, two)
        val end = TripMath.mapPixel(yangshuo.first, yangshuo.second, two)
        assertTrue(start.first in 0f..800f)
        assertTrue(end.first in 0f..800f)
        assertTrue(start.second in 0f..400f)
        assertTrue(end.second in 0f..400f)
        assertTrue(TripMath.mapTiles(TripMath.mapFrame(emptyList(), 400, 260)).isNotEmpty())
    }
}
