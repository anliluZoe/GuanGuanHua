package com.savemoney.app.ui

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

val CATEGORIES = listOf("餐饮", "日用品", "服饰", "数码", "交通", "娱乐", "学习", "医疗", "其他")

private val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

fun Long.toYuan(): String =
    "¥" + BigDecimal(this).movePointLeft(2).setScale(2, RoundingMode.HALF_UP).toPlainString()

fun Long.toDateTimeText(): String =
    DATE_TIME_FORMAT.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

fun String.yuanToCentsOrNull(): Long? {
    val value = trim().toBigDecimalOrNull() ?: return null
    if (value.signum() <= 0 || value.scale() > 2) return null
    return value.movePointRight(2).longValueExact()
}
