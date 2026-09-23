package com.guanguanhua.app.cycle

import com.guanguanhua.app.data.CycleRecord
import com.guanguanhua.app.data.CycleSettings
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import kotlin.math.floor

enum class PredictSource {
    DATA,
    MANUAL,
    DEFAULT,
}

data class CyclePrediction(
    val source: PredictSource,
    val cycleDays: Int,
    val averageDays: Int,
    val gapCount: Int,
    val recordedCount: Int,
    val periodDays: Int,
    val referenceDays: Int?,
    val nextStart: LocalDate?,
    val ovulation: LocalDate?,
)

/**
 * 周期 = 这次月经第一天到下次第一天。
 * 只把 21–35 天的间隔拿来平均；至少 2 段才用历史。不够时用手填参考天数（18–45），再退回 28。
 * 排卵示意 = 预测的下次经期首日 − 14。
 */
object CycleMath {
    const val GAP_MIN = 21
    const val GAP_MAX = 35
    const val MIN_GAPS_FOR_DATA = 2
    const val DEFAULT_CYCLE_DAYS = 28
    const val REF_MIN = 18
    const val REF_MAX = 45
    const val PERIOD_MIN = 1
    const val PERIOD_MAX = 14
    const val DEFAULT_PERIOD_DAYS = 5
    const val REMIND_HOUR = 9

    fun predict(cycles: List<CycleRecord>, settings: CycleSettings): CyclePrediction {
        val starts = cycles.mapNotNull { parse(it.start) }.sorted()
        val gaps = if (starts.size < 2) {
            emptyList()
        } else {
            starts.zipWithNext { previous, next -> ChronoUnit.DAYS.between(previous, next).toInt() }
                .filter { it in GAP_MIN..GAP_MAX }
        }
        val averageDays = if (gaps.isEmpty()) DEFAULT_CYCLE_DAYS else floor(gaps.average() + 0.5).toInt()
        val reference = referenceDays(settings)
        val periodDays = periodDays(settings)
        val enough = gaps.size >= MIN_GAPS_FOR_DATA
        val source = when {
            enough -> PredictSource.DATA
            reference != null -> PredictSource.MANUAL
            else -> PredictSource.DEFAULT
        }
        val cycleDays = when (source) {
            PredictSource.DATA -> averageDays
            PredictSource.MANUAL -> reference ?: DEFAULT_CYCLE_DAYS
            PredictSource.DEFAULT -> DEFAULT_CYCLE_DAYS
        }
        val nextStart = starts.maxOrNull()?.plusDays(cycleDays.toLong())
        return CyclePrediction(
            source = source,
            cycleDays = cycleDays,
            averageDays = averageDays,
            gapCount = gaps.size,
            recordedCount = starts.size,
            periodDays = periodDays,
            referenceDays = reference,
            nextStart = nextStart,
            ovulation = nextStart?.minusDays(14),
        )
    }

    fun periodDays(settings: CycleSettings): Int =
        settings.periodDays.takeIf { it in PERIOD_MIN..PERIOD_MAX } ?: DEFAULT_PERIOD_DAYS

    fun referenceDays(settings: CycleSettings): Int? =
        settings.referenceCycleDays?.takeIf { it in REF_MIN..REF_MAX }

    fun suggestedReference(prediction: CyclePrediction): Int =
        prediction.referenceDays ?: prediction.averageDays.coerceIn(REF_MIN, REF_MAX)

    /** 记开始日时按「每次大概几天」预填结束日（含首日）。 */
    fun prefillEnd(start: LocalDate, periodDays: Int): LocalDate =
        start.plusDays((periodDays.coerceIn(PERIOD_MIN, PERIOD_MAX) - 1).toLong())

    fun periodDates(cycles: List<CycleRecord>): Set<LocalDate> {
        val dates = mutableSetOf<LocalDate>()
        cycles.forEach { cycle ->
            val start = parse(cycle.start) ?: return@forEach
            val end = parse(cycle.end) ?: start
            if (end.isBefore(start)) return@forEach
            var cursor = start
            var guard = 0
            while (!cursor.isAfter(end) && guard < 60) {
                dates.add(cursor)
                cursor = cursor.plusDays(1)
                guard++
            }
        }
        return dates
    }

    fun durationDays(start: String, end: String?): Int? {
        val from = parse(start) ?: return null
        val to = parse(end) ?: return null
        if (to.isBefore(from)) return null
        return ChronoUnit.DAYS.between(from, to).toInt() + 1
    }

    fun toCsv(cycles: List<CycleRecord>): String {
        val lines = mutableListOf("start,end,duration_days")
        cycles.sortedWith(compareByDescending<CycleRecord> { it.start }.thenByDescending { it.id }).forEach { cycle ->
            val end = cycle.end?.takeIf { it.isNotBlank() }.orEmpty()
            val duration = durationDays(cycle.start, end.takeIf { it.isNotEmpty() })?.toString().orEmpty()
            lines += "${cycle.start},$end,$duration"
        }
        return "\uFEFF" + lines.joinToString("\n")
    }

    fun formatCn(date: LocalDate): String = "${date.monthValue}月${date.dayOfMonth}日"

    fun formatCnFull(date: LocalDate): String = "${date.year}年${date.monthValue}月${date.dayOfMonth}日"

    fun predictDateText(prediction: CyclePrediction): String =
        prediction.nextStart?.let(::formatCnFull) ?: "再记一两回就有～"

    fun predictMeta(prediction: CyclePrediction): String {
        if (prediction.nextStart == null) {
            return if (prediction.source == PredictSource.MANUAL) {
                "样本还少，先按你设的 ${prediction.cycleDays} 天估"
            } else {
                "默认按约 $DEFAULT_CYCLE_DAYS 天估 · 多记几次更准"
            }
        }
        val periodBit = " · 经期约 ${prediction.periodDays} 天"
        val recorded = prediction.recordedCount
        return when (prediction.source) {
            PredictSource.DATA -> {
                val ref = prediction.referenceDays?.let { "（你设的参考 $it 天）" }.orEmpty()
                "按你的记录平均约 ${prediction.averageDays} 天（首日→下次首日 · ${prediction.gapCount} 段）$ref · 已记 $recorded 次$periodBit"
            }
            PredictSource.MANUAL ->
                "记录还少，先按你设的 ${prediction.cycleDays} 天估 · 已记 $recorded 次 · 再记几回会改用你的平均$periodBit"
            PredictSource.DEFAULT ->
                "按约 ${prediction.cycleDays} 天估 · 已记 $recorded 次 · 再记几回会按你的平均$periodBit"
        }
    }

    fun predictOvuText(prediction: CyclePrediction): String {
        val ovulation = prediction.ovulation ?: return "排卵示意要等有下次大概之后～"
        return "排卵大概 ${formatCnFull(ovulation)}（下次首日 −14 · 仅规律时较准）"
    }

    fun settingsHint(prediction: CyclePrediction): String = when (prediction.source) {
        PredictSource.DATA -> "有足够记录，预测按历史平均约 ${prediction.averageDays} 天。参考天数只作对照。"
        PredictSource.MANUAL -> "记录还少，先按参考 ${prediction.cycleDays} 天估。再记几回会改用你的平均。"
        PredictSource.DEFAULT -> "有足够记录用历史平均；不够才用参考天数或默认 28。"
    }

    /**
     * 提醒落在预测首日往前 remindDays 的当天早上 9 点。
     * 已经过了这个点、但还没到预测首日，并且这次还没提醒过，就立刻发。
     */
    fun planReminder(
        nextStart: LocalDate?,
        remindEnabled: Boolean,
        remindDays: Int,
        now: LocalDateTime,
        notifiedStart: LocalDate?,
    ): LocalDateTime? {
        if (!remindEnabled || nextStart == null) return null
        if (!now.toLocalDate().isBefore(nextStart)) return null
        if (notifiedStart == nextStart) return null
        val days = remindDays.coerceIn(1, 7)
        val fireAt = nextStart.minusDays(days.toLong()).atTime(REMIND_HOUR, 0)
        return if (fireAt.isAfter(now)) fireAt else now
    }

    fun parse(iso: String?): LocalDate? =
        iso?.takeIf { it.isNotBlank() }?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
}
