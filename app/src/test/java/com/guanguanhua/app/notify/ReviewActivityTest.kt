package com.guanguanhua.app.notify

import com.guanguanhua.app.data.PurchaseRequest
import com.guanguanhua.app.data.RequestStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewActivityTest {

    @Test
    fun pendingFromPartnerIsActionable() {
        val items = ReviewActivity.newItems(
            listOf(
                request(id = 3, mine = false, status = RequestStatus.PENDING, createdAt = 200, itemName = "耳机"),
                request(id = 4, mine = true, status = RequestStatus.PENDING, createdAt = 300, itemName = "自己的"),
            ),
            since = 100,
        )
        assertEquals(1, items.size)
        assertEquals(3L, items.single().requestId)
        assertTrue(items.single().title.contains("耳机"))
    }

    @Test
    fun reviewResultOnMyRequestIsActionable() {
        val items = ReviewActivity.newItems(
            listOf(
                request(
                    id = 8,
                    mine = true,
                    status = RequestStatus.APPROVED,
                    createdAt = 50,
                    reviewedAt = 180,
                    itemName = "键盘",
                    reviewerName = "小红",
                ),
            ),
            since = 100,
        )
        assertEquals(1, items.size)
        assertEquals(8L, items.single().requestId)
        assertTrue(items.single().title.contains("同意了"))
    }

    @Test
    fun alreadySeenItemsAreIgnored() {
        val items = ReviewActivity.newItems(
            listOf(
                request(id = 1, mine = false, status = RequestStatus.PENDING, createdAt = 90),
                request(id = 2, mine = true, status = RequestStatus.REJECTED, createdAt = 40, reviewedAt = 80),
            ),
            since = 100,
        )
        assertTrue(items.isEmpty())
    }

    @Test
    fun firstSyncWatermarkIsAtLeastOne() {
        assertEquals(1L, ReviewActivity.watermark(emptyList(), 0L))
        assertEquals(250L, ReviewActivity.watermark(
            listOf(request(createdAt = 200, reviewedAt = 250)),
            current = 0L,
        ))
    }

    private fun request(
        id: Long = 1,
        mine: Boolean = false,
        status: RequestStatus = RequestStatus.PENDING,
        createdAt: Long = 100,
        reviewedAt: Long? = null,
        itemName: String = "杯子",
        reviewerName: String? = null,
    ) = PurchaseRequest(
        id = id,
        requesterId = 1,
        mine = mine,
        itemName = itemName,
        category = "日用品",
        unitPriceCents = 1200,
        quantity = 1,
        reason = "用得着",
        requesterName = "小明",
        status = status,
        createdAt = createdAt,
        reviewedAt = reviewedAt,
        reviewerName = reviewerName,
    )
}
