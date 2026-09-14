package com.savemoney.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.savemoney.app.data.RequestStatus
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 可选的物品分类，用于申请与消费统计。 */
val CATEGORIES = listOf("餐饮", "日用品", "服饰", "数码", "交通", "娱乐", "学习", "医疗", "其他")

private val DATE_TIME_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MM-dd HH:mm")

/** 分 -> "¥12.50" */
fun Long.toYuan(): String = "¥" + BigDecimal(this).movePointLeft(2).setScale(2, RoundingMode.HALF_UP).toPlainString()

/** 毫秒时间戳 -> "09-14 10:30" */
fun Long.toDateTimeText(): String =
    DATE_TIME_FORMAT.format(Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()))

/** 用户输入的元（如 "12.5"）-> 分；非法或非正数返回 null。 */
fun String.yuanToCentsOrNull(): Long? {
    val value = trim().toBigDecimalOrNull() ?: return null
    if (value.signum() <= 0 || value.scale() > 2) return null
    return value.movePointRight(2).longValueExact()
}

@Composable
fun StatusBadge(status: RequestStatus, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val (bg, fg) = when (status) {
        RequestStatus.PENDING -> scheme.tertiaryContainer to scheme.onTertiaryContainer
        RequestStatus.APPROVED -> scheme.primaryContainer to scheme.onPrimaryContainer
        RequestStatus.REJECTED -> scheme.errorContainer to scheme.onErrorContainer
    }
    Text(
        text = status.label,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(bg)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    )
}
