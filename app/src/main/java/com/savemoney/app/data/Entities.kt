package com.savemoney.app.data

enum class RequestStatus(val label: String) {
    PENDING("待审核"),
    APPROVED("已通过"),
    REJECTED("已拒绝"),
}

data class PurchaseRequest(
    val id: Long = 0,
    val requesterId: Long = 0,
    /** 是不是当前这部手机的人提的；服务器按令牌算好返回。 */
    val mine: Boolean = false,
    val itemName: String,
    val category: String,
    val unitPriceCents: Long,
    val quantity: Int,
    val approvedUnitPriceCents: Long? = null,
    val approvedQuantity: Int? = null,
    val partial: Boolean = false,
    val reason: String,
    val requesterName: String,
    val status: RequestStatus = RequestStatus.PENDING,
    val createdAt: Long,
    val reviewedAt: Long? = null,
    val reviewerName: String? = null,
    val reviewComment: String? = null,
    val imagePath: String? = null,
) {
    val askedCents: Long get() = unitPriceCents * quantity
    val totalCents: Long
        get() = (approvedUnitPriceCents ?: unitPriceCents) * (approvedQuantity ?: quantity)
}

data class ExpenseRecord(
    val id: Long = 0,
    val requestId: Long,
    val itemName: String,
    val category: String,
    val amountCents: Long,
    val spentAt: Long,
    val requesterName: String,
    val reviewerName: String,
)

data class MonthlyBudget(
    val yearMonth: String,
    val amountCents: Long,
)
