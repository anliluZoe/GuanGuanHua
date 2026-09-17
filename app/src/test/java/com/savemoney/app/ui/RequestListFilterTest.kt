package com.savemoney.app.ui

import com.savemoney.app.data.PurchaseRequest
import com.savemoney.app.data.RequestStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class RequestListFilterTest {

    private val minePending = request(1, mine = true, RequestStatus.PENDING)
    private val theirsPending = request(2, mine = false, RequestStatus.PENDING)
    private val mineApproved = request(3, mine = true, RequestStatus.APPROVED)
    private val theirsRejected = request(4, mine = false, RequestStatus.REJECTED)
    private val all = listOf(minePending, theirsPending, mineApproved, theirsRejected)

    @Test
    fun pendingForMeIsOnlyOthersWaitingOnMe() {
        assertEquals("待审核", RequestListFilter.PENDING_FOR_ME.label)
        assertEquals(
            listOf(theirsPending),
            all.filter { RequestListFilter.PENDING_FOR_ME.matches(it) },
        )
    }

    @Test
    fun waitingForPartnerIsOwnPendingOnly() {
        assertEquals(
            listOf(minePending),
            all.filter { RequestListFilter.WAITING_FOR_PARTNER.matches(it) },
        )
    }

    @Test
    fun allKeepsOwnPendingSubmissions() {
        assertEquals(all, all.filter { RequestListFilter.ALL.matches(it) })
    }

    @Test
    fun approvedAndRejectedStayStatusOnly() {
        assertEquals(listOf(mineApproved), all.filter { RequestListFilter.APPROVED.matches(it) })
        assertEquals(listOf(theirsRejected), all.filter { RequestListFilter.REJECTED.matches(it) })
    }

    private fun request(id: Long, mine: Boolean, status: RequestStatus) = PurchaseRequest(
        id = id,
        mine = mine,
        itemName = "item$id",
        category = "其他",
        unitPriceCents = 100,
        quantity = 1,
        reason = "r",
        requesterName = if (mine) "me" else "partner",
        status = status,
        createdAt = id,
    )
}
