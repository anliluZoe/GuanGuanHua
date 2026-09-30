package com.guanguanhua.app.trip

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneOffset
import kotlin.math.hypot

class TripMathTest {

    @Test
    fun kindsAndRatingsStayInTheAgreedSet() {
        assertEquals(listOf("住宿", "美食", "风景", "博物馆", "杂物店"), TripMath.KINDS)
        assertTrue(TripMath.knownKind("杂物店"))
        assertNull(TripMath.ratingOrNull(0))
        assertEquals(5, TripMath.ratingOrNull(5))
        assertEquals(6, TripMath.MAX_PHOTOS)
        assertEquals(4, TripMath.MAP_MIN_ZOOM)
        assertEquals(18, TripMath.MAP_MAX_ZOOM)
    }

    @Test
    fun dayNumberCountsFromStartDate() {
        val start = LocalDate.of(2026, 10, 1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals(1, TripMath.dayNumber(start, LocalDate.of(2026, 10, 1), ZoneOffset.UTC))
        assertEquals(3, TripMath.dayNumber(start, LocalDate.of(2026, 10, 3), ZoneOffset.UTC))
        assertEquals("10月1日至今", TripMath.formatRange(start, null, ZoneOffset.UTC))
        val end = LocalDate.of(2026, 10, 7).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        assertEquals("10月1日–10月7日", TripMath.formatRange(start, end, ZoneOffset.UTC))
        assertEquals("第1天 · 10月1日", TripMath.formatDay(start, LocalDate.of(2026, 10, 1), ZoneOffset.UTC))
        assertEquals("第3天 · 10月3日", TripMath.formatDay(start, LocalDate.of(2026, 10, 3), ZoneOffset.UTC))
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
        val chengdu = 30.67 to 104.06
        val chengduFrame = TripMath.mapFrame(listOf(chengdu), 512, 260)
        val chengduPixel = TripMath.mapPixel(chengdu.first, chengdu.second, chengduFrame)
        assertTrue(chengduPixel.first in 0f..512f)
        assertTrue(chengduPixel.second in 0f..260f)
        assertTrue(chengduFrame.left != one.left || chengduFrame.top != one.top)
    }

    @Test(expected = IllegalStateException::class)
    fun emptyRouteDoesNotFallBackToGuilin() {
        TripMath.mapFrame(emptyList(), 400, 260)
    }

    @Test
    fun nearbyMarkersSpreadApartInsteadOfStacking() {
        val stacked = listOf(100f to 100f, 100f to 100f, 102f to 101f)
        val spread = TripMath.spreadOverlapping(stacked, 20f)
        val d01 = hypot(spread[0].first - spread[1].first, spread[0].second - spread[1].second)
        val d02 = hypot(spread[0].first - spread[2].first, spread[0].second - spread[2].second)
        val d12 = hypot(spread[1].first - spread[2].first, spread[1].second - spread[2].second)
        assertTrue(d01 >= 19.5f)
        assertTrue(d02 >= 19.5f)
        assertTrue(d12 >= 19.5f)
        val far = listOf(10f to 10f, 200f to 200f)
        assertEquals(far, TripMath.spreadOverlapping(far, 20f))
    }

    @Test
    fun zoomAndPanKeepTheFocusedStopOnScreen() {
        val chengdu = 30.67 to 104.06
        val frame = TripMath.mapFrame(listOf(chengdu), 512, 260)
        val pixel = TripMath.mapPixel(chengdu.first, chengdu.second, frame)
        val zoomed = TripMath.zoomFrame(frame, frame.zoom + 2, pixel.first, pixel.second)
        assertEquals(frame.zoom + 2, zoomed.zoom)
        val still = TripMath.mapPixel(chengdu.first, chengdu.second, zoomed)
        assertEquals(pixel.first, still.first, 1.5f)
        assertEquals(pixel.second, still.second, 1.5f)
        val maxed = TripMath.zoomFrame(zoomed, 99, 120f, 80f)
        assertEquals(TripMath.MAP_MAX_ZOOM, maxed.zoom)
        val mined = TripMath.zoomFrame(frame, 1, 120f, 80f)
        assertEquals(TripMath.MAP_MIN_ZOOM, mined.zoom)
        val panned = TripMath.panFrame(frame, 40f, -16f)
        val moved = TripMath.mapPixel(chengdu.first, chengdu.second, panned)
        assertEquals(pixel.first + 40f, moved.first, 1.5f)
        assertEquals(pixel.second - 16f, moved.second, 1.5f)
    }

    @Test
    fun routeProgressGoesFromEarlyLightToLateDark() {
        assertEquals(emptyList<Float>(), TripMath.routeProgress(emptyList()))
        assertEquals(listOf(0f), TripMath.routeProgress(listOf("2026-10-01")))
        val sameDay = TripMath.routeProgress(listOf("2026-10-01", "2026-10-01", "2026-10-01"))
        assertEquals(0f, sameDay[0], 0.01f)
        assertEquals(0.5f, sameDay[1], 0.01f)
        assertEquals(1f, sameDay[2], 0.01f)
        val twoDays = TripMath.routeProgress(listOf("2026-10-01", "2026-10-03"))
        assertEquals(0f, twoDays[0], 0.01f)
        assertEquals(1f, twoDays[1], 0.01f)
        val mixed = TripMath.routeProgress(listOf("2026-10-01", "2026-10-01", "2026-10-05"))
        assertTrue(mixed[0] < mixed[1])
        assertTrue(mixed[1] < mixed[2])
        assertTrue(mixed[1] < 0.4f)
        val broken = TripMath.routeProgress(listOf("bad", "also-bad", "nope"))
        assertEquals(0f, broken[0], 0.01f)
        assertEquals(1f, broken[2], 0.01f)
    }
}
