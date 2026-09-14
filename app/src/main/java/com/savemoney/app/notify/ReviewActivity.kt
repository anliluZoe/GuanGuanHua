package com.savemoney.app.notify

import com.savemoney.app.UserRole
import com.savemoney.app.data.PurchaseRequest
import com.savemoney.app.data.RequestStatus
import com.savemoney.app.ui.toYuan

data class ActivityItem(
    val requestId: Long,
    val at: Long,
    val title: String,
    val text: String,
)

/**
 * 申请列表里"对方做了什么"的增量：申请人关心审核结果，审核人关心新申请。
 * 水位线记录的是已经看过的最新时间戳（服务器时间），避免和手机时钟比较。
 */
object ReviewActivity {
    const val PREF_SINCE = "activitySince"

    fun newItems(requests: List<PurchaseRequest>, role: UserRole, since: Long): List<ActivityItem> {
        val items = when (role) {
            UserRole.REQUESTER -> requests
                .filter { it.status != RequestStatus.PENDING && (it.reviewedAt ?: 0L) > since }
                .map { r ->
                    val verdict = if (r.status == RequestStatus.APPROVED) "同意了" else "没同意"
                    val comment = r.reviewComment?.takeIf { it.isNotBlank() }?.let { " · 留言：$it" } ?: ""
                    ActivityItem(
                        requestId = r.id,
                        at = r.reviewedAt ?: 0L,
                        title = "${r.reviewerName ?: "审核人"}$verdict「${r.itemName}」",
                        text = "${r.totalCents.toYuan()}$comment",
                    )
                }
            UserRole.APPROVER -> requests
                .filter { it.status == RequestStatus.PENDING && it.createdAt > since }
                .map { r ->
                    val reason = r.reason.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                    ActivityItem(
                        requestId = r.id,
                        at = r.createdAt,
                        title = "${r.requesterName}想买「${r.itemName}」",
                        text = "${r.totalCents.toYuan()} · 等你审核$reason",
                    )
                }
        }
        return items.sortedBy { it.at }
    }

    /** 至少为 1，用来区分"从未同步过"（0）和"同步过但还没有任何申请"。 */
    fun watermark(requests: List<PurchaseRequest>, current: Long): Long =
        requests.fold(maxOf(current, 1L)) { acc, r -> maxOf(acc, r.createdAt, r.reviewedAt ?: 0L) }
}
