package com.guanguanhua.app.trip

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

object TripMath {
    val KINDS = listOf("住宿", "美食", "风景", "博物馆", "杂物店")

    fun knownKind(kind: String): Boolean = kind in KINDS

    fun ratingOrNull(rating: Int?): Int? = rating?.takeIf { it in 1..5 }

    fun startedDate(startedAt: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(startedAt).atZone(zone).toLocalDate()

    fun dayNumber(startedAt: Long, visitedOn: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int =
        ChronoUnit.DAYS.between(startedDate(startedAt, zone), visitedOn).toInt() + 1

    fun formatRange(startedAt: Long, endedAt: Long?, zone: ZoneId = ZoneId.systemDefault()): String {
        val start = startedDate(startedAt, zone)
        val end = endedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        return if (end == null) "${start.monthValue}月${start.dayOfMonth}日至今" else
            "${start.monthValue}月${start.dayOfMonth}日–${end.monthValue}月${end.dayOfMonth}日"
    }

    fun kindFromTags(amenity: String?, tourism: String?, shop: String?): String? {
        val a = amenity.orEmpty()
        val t = tourism.orEmpty()
        val s = shop.orEmpty()
        return when {
            t in setOf("hotel", "guest_house", "hostel", "motel", "apartment") || a == "hotel" -> "住宿"
            a in setOf("restaurant", "cafe", "fast_food", "bar", "food_court", "ice_cream") -> "美食"
            t in setOf("museum", "gallery") || a == "arts_centre" -> "博物馆"
            s in setOf("convenience", "supermarket", "general", "gift") -> "杂物店"
            t.isNotBlank() || a == "attraction" -> "风景"
            else -> null
        }
    }

    /**
     * 预计结束日到了提醒一次；若还没结束，满 2 天再提醒一次。
     * 返回这次提醒对应的日期（写入 lastReminded）；不该提醒则 null。
     */
    fun nextEndReminder(plannedEnd: LocalDate?, ended: Boolean, today: LocalDate, lastReminded: LocalDate?): LocalDate? {
        if (ended || plannedEnd == null || today.isBefore(plannedEnd)) return null
        if (lastReminded == null) return plannedEnd
        if (lastReminded == plannedEnd && !today.isBefore(plannedEnd.plusDays(2))) return today
        return null
    }
}
