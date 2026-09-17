package com.guanguanhua.app.notify

import com.guanguanhua.app.data.PurchaseRequest
import com.guanguanhua.app.data.RequestStatus
import com.guanguanhua.app.ui.toYuan

data class ActivityItem(
    val requestId: Long,
    val at: Long,
    val title: String,
    val text: String,
)

/**
 * 申请列表里"对方做了什么"的增量：对方的新申请等我审，我的申请被对方审了。
 * 水位线记录的是已经看过的最新时间戳（服务器时间），避免和手机时钟比较。
 */
object ReviewActivity {
    const val PREF_SINCE = "activitySince"

    fun newItems(requests: List<PurchaseRequest>, since: Long): List<ActivityItem> {
        val waitingForMe = requests
            .filter { !it.mine && it.status == RequestStatus.PENDING && it.createdAt > since }
            .map { r ->
                val reason = r.reason.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""
                ActivityItem(
                    requestId = r.id,
                    at = r.createdAt,
                    title = "${r.requesterName}想买「${r.itemName}」",
                    text = "${r.totalCents.toYuan()} · 等你审核$reason",
                )
            }
        val myResults = requests
            .filter { it.mine && it.status != RequestStatus.PENDING && (it.reviewedAt ?: 0L) > since }
            .map { r ->
                val verdict = when {
                    r.status == RequestStatus.REJECTED -> "没同意"
                    r.partial -> "部分同意了"
                    else -> "同意了"
                }
                val comment = r.reviewComment?.takeIf { it.isNotBlank() }?.let { " · 留言：$it" } ?: ""
                val cut = if (r.partial) "（申请 ${r.askedCents.toYuan()}）" else ""
                ActivityItem(
                    requestId = r.id,
                    at = r.reviewedAt ?: 0L,
                    title = "${r.reviewerName ?: "对方"}$verdict「${r.itemName}」",
                    text = "${r.totalCents.toYuan()}$cut$comment",
                )
            }
        return (waitingForMe + myResults).sortedBy { it.at }
    }

    /** 至少为 1，用来区分"从未同步过"（0）和"同步过但还没有任何申请"。 */
    fun watermark(requests: List<PurchaseRequest>, current: Long): Long =
        requests.fold(maxOf(current, 1L)) { acc, r -> maxOf(acc, r.createdAt, r.reviewedAt ?: 0L) }
}
