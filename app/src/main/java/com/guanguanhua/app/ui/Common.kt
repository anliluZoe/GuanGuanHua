package com.guanguanhua.app.ui

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

/**
 * 审核通过金额：数量和单价须为正，且批准总额不得超过申请总额。
 * 允许提高数量或单价，只要总价不超（例如 3×10=30 允许 2×15=30，不允许总额 31）。
 */
fun approvedAmountsOk(
    requestedUnitPriceCents: Long,
    requestedQuantity: Int,
    approvedUnitPriceCents: Long?,
    approvedQuantity: Int?,
): Boolean {
    if (approvedUnitPriceCents == null || approvedQuantity == null) return false
    if (approvedUnitPriceCents < 1L || approvedQuantity < 1) return false
    return approvedUnitPriceCents * approvedQuantity <= requestedUnitPriceCents * requestedQuantity
}
