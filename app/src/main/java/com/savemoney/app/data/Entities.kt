package com.savemoney.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/** 申请状态 */
enum class RequestStatus(val label: String) {
    PENDING("待审核"),
    APPROVED("已通过"),
    REJECTED("已拒绝"),
}

/** 购买申请。金额以“分”为单位存储，避免浮点误差。 */
@Entity(tableName = "purchase_requests")
data class PurchaseRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val itemName: String,
    val category: String,
    val unitPriceCents: Long,
    val quantity: Int,
    val reason: String,
    val requesterName: String,
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long,
    val reviewedAt: Long? = null,
    val reviewerName: String? = null,
    val reviewComment: String? = null,
    val imagePath: String? = null,
) {
    val totalCents: Long get() = unitPriceCents * quantity
}

/** 消费记录。申请审核通过时自动生成一条记录。 */
@Entity(tableName = "expense_records")
data class ExpenseRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val requestId: Long,
    val itemName: String,
    val category: String,
    val amountCents: Long,
    val spentAt: Long,
    val requesterName: String,
    val reviewerName: String,
)

/** 月度预算，yearMonth 形如 "2026-09"。 */
@Entity(tableName = "monthly_budgets")
data class MonthlyBudget(
    @PrimaryKey val yearMonth: String,
    val amountCents: Long,
)
