package com.guanguanhua.app.data

/** 一条经期。结束日可空。日期是 YYYY-MM-DD，跟服务器一致。 */
data class CycleRecord(
    val id: Long = 0,
    val start: String = "",
    val end: String? = null,
)

/** 跟当前成员走的周期设置。referenceCycleDays 为空表示先看历史平均，不够再退回 28。 */
data class CycleSettings(
    val referenceCycleDays: Int? = null,
    val periodDays: Int = 5,
    val remindEnabled: Boolean = true,
    val remindDays: Int = 2,
)

data class CycleState(
    val memberId: Long = 0,
    val cycles: List<CycleRecord> = emptyList(),
    val settings: CycleSettings = CycleSettings(),
    val loaded: Boolean = false,
)
