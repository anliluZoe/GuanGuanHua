package com.guanguanhua.app.cycle

import com.guanguanhua.app.data.CycleRecord
import com.guanguanhua.app.data.CycleSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CycleMathTest {

    @Test
    fun seedHistoryPredictsOctober14AndIgnoresReference() {
        val prediction = CycleMath.predict(seed(), CycleSettings(referenceCycleDays = 30, periodDays = 5))
        assertEquals(PredictSource.DATA, prediction.source)
        assertEquals(26, prediction.cycleDays)
        assertEquals(26, prediction.averageDays)
        assertEquals(2, prediction.gapCount)
        assertEquals(LocalDate.of(2026, 10, 14), prediction.nextStart)
        assertEquals(LocalDate.of(2026, 9, 30), prediction.ovulation)
        assertTrue(CycleMath.predictMeta(prediction).contains("平均约 26 天"))
        assertTrue(CycleMath.predictMeta(prediction).contains("参考 30 天"))
        assertEquals("2026年10月14日", CycleMath.predictDateText(prediction))
    }

    @Test
    fun oneValidGapUsesReferenceOrDefaultInsteadOfThatGap() {
        val cycles = listOf(
            CycleRecord(1, "2026-01-01", "2026-01-05"),
            CycleRecord(2, "2026-01-29", "2026-02-02"),
        )
        val fallback = CycleMath.predict(cycles, CycleSettings())
        assertEquals(PredictSource.DEFAULT, fallback.source)
        assertEquals(28, fallback.cycleDays)
        assertEquals(LocalDate.of(2026, 2, 26), fallback.nextStart)

        val manual = CycleMath.predict(cycles, CycleSettings(referenceCycleDays = 30))
        assertEquals(PredictSource.MANUAL, manual.source)
        assertEquals(30, manual.cycleDays)
        assertEquals(LocalDate.of(2026, 2, 28), manual.nextStart)
    }

    @Test
    fun referenceOutsideAverageFilterStillUsedWhenHistoryIsThin() {
        val cycles = listOf(CycleRecord(1, "2026-09-18", "2026-09-22"))
        val short = CycleMath.predict(cycles, CycleSettings(referenceCycleDays = 18))
        assertEquals(PredictSource.MANUAL, short.source)
        assertEquals(LocalDate.of(2026, 10, 6), short.nextStart)
        val long = CycleMath.predict(cycles, CycleSettings(referenceCycleDays = 45))
        assertEquals(LocalDate.of(2026, 11, 2), long.nextStart)
        val ignored = CycleMath.predict(cycles, CycleSettings(referenceCycleDays = 10))
        assertEquals(PredictSource.DEFAULT, ignored.source)
        assertEquals(28, ignored.cycleDays)
    }

    @Test
    fun gapsOutside21To35AreDroppedAndHalfDaysRoundUp() {
        val weird = listOf(
            CycleRecord(1, "2026-01-01"),
            CycleRecord(2, "2026-01-21"),
            CycleRecord(3, "2026-02-26"),
        )
        val onlyOutliers = CycleMath.predict(weird, CycleSettings())
        assertEquals(PredictSource.DEFAULT, onlyOutliers.source)

        val edges = listOf(
            CycleRecord(1, "2026-01-01"),
            CycleRecord(2, "2026-01-22"),
            CycleRecord(3, "2026-02-26"),
        )
        val kept = CycleMath.predict(edges, CycleSettings(referenceCycleDays = 40))
        assertEquals(PredictSource.DATA, kept.source)
        assertEquals(listOf(21, 35), gapsOf(edges))
        assertEquals(28, kept.cycleDays)

        val half = listOf(
            CycleRecord(1, "2026-01-01"),
            CycleRecord(2, "2026-01-22"),
            CycleRecord(3, "2026-02-13"),
        )
        assertEquals(22, CycleMath.predict(half, CycleSettings()).cycleDays)
    }

    @Test
    fun prefillEndUsesPeriodLengthIncludingStartDay() {
        val start = LocalDate.of(2026, 9, 18)
        assertEquals(LocalDate.of(2026, 9, 22), CycleMath.prefillEnd(start, 5))
        assertEquals(start, CycleMath.prefillEnd(start, 1))
        assertEquals(start.plusDays(13), CycleMath.prefillEnd(start, 14))
        assertEquals(start, CycleMath.prefillEnd(start, 0))
        assertEquals(5, CycleMath.periodDays(CycleSettings(periodDays = 0)))
        assertEquals(5, CycleMath.durationDays("2026-09-18", "2026-09-22"))
        assertNull(CycleMath.durationDays("2026-09-18", null))
    }

    @Test
    fun periodMarksAreInclusiveAndOpenCycleIsStartOnly() {
        val dates = CycleMath.periodDates(
            listOf(
                CycleRecord(1, "2026-09-18", "2026-09-22"),
                CycleRecord(2, "2026-08-25", null),
            ),
        )
        assertTrue(LocalDate.of(2026, 9, 18) in dates)
        assertTrue(LocalDate.of(2026, 9, 22) in dates)
        assertTrue(LocalDate.of(2026, 9, 23) !in dates)
        assertEquals(setOf(LocalDate.of(2026, 8, 25)), dates.filter { it.monthValue == 8 }.toSet())
    }

    @Test
    fun csvIsUtf8BomNewestFirst() {
        val csv = CycleMath.toCsv(seed())
        assertTrue(csv.startsWith("\uFEFF"))
        assertEquals(
            """
            start,end,duration_days
            2026-09-18,2026-09-22,5
            2026-08-25,2026-08-30,6
            2026-07-28,2026-08-02,6
            """.trimIndent(),
            csv.removePrefix("\uFEFF"),
        )
        val open = CycleMath.toCsv(listOf(CycleRecord(1, "2026-09-01", null)))
        assertTrue(open.removePrefix("\uFEFF").contains("2026-09-01,,"))
    }

    @Test
    fun reminderFiresAtNineTwoDaysBeforeAndNotAgain() {
        val next = LocalDate.of(2026, 10, 14)
        val early = LocalDateTime.of(2026, 9, 23, 8, 0)
        assertEquals(
            LocalDateTime.of(2026, 10, 12, 9, 0),
            CycleMath.planReminder(next, remindEnabled = true, remindDays = 2, now = early, notifiedStart = null),
        )
        val late = LocalDateTime.of(2026, 10, 13, 10, 0)
        assertEquals(late, CycleMath.planReminder(next, true, 2, late, null))
        assertNull(CycleMath.planReminder(next, true, 2, late, next))
        assertNull(CycleMath.planReminder(next, false, 2, early, null))
        assertNull(CycleMath.planReminder(next, true, 2, LocalDateTime.of(2026, 10, 14, 9, 0), null))
        assertNull(CycleMath.planReminder(null, true, 2, early, null))
    }

    @Test
    fun emptyHistoryHasNoDateUntilAStartExists() {
        val prediction = CycleMath.predict(emptyList(), CycleSettings())
        assertNull(prediction.nextStart)
        assertNull(prediction.ovulation)
        assertEquals("再记一两回就有～", CycleMath.predictDateText(prediction))
        assertEquals(PredictSource.DEFAULT, prediction.source)
    }

    private fun seed() = listOf(
        CycleRecord(1, "2026-07-28", "2026-08-02"),
        CycleRecord(2, "2026-08-25", "2026-08-30"),
        CycleRecord(3, "2026-09-18", "2026-09-22"),
    )

    private fun gapsOf(cycles: List<CycleRecord>): List<Int> {
        val starts = cycles.map { LocalDate.parse(it.start) }.sorted()
        return starts.zipWithNext { a, b -> java.time.temporal.ChronoUnit.DAYS.between(a, b).toInt() }
            .filter { it in 21..35 }
    }
}
