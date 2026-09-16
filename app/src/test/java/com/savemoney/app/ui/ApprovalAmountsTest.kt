package com.savemoney.app.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovalAmountsTest {

    @Test
    fun allowsSameTotalWithHigherUnitPrice() {
        // 3×10=30 → 2×15=30
        assertTrue(approvedAmountsOk(10, 3, 15, 2))
    }

    @Test
    fun allowsHigherQuantityWhenTotalMatches() {
        assertTrue(approvedAmountsOk(10, 3, 5, 6))
    }

    @Test
    fun rejectsTotalOneCentOver() {
        assertFalse(approvedAmountsOk(10, 3, 31, 1))
        assertFalse(approvedAmountsOk(10, 3, 16, 2))
    }

    @Test
    fun allowsLowerOrOriginalTotal() {
        assertTrue(approvedAmountsOk(10, 3, 10, 3))
        assertTrue(approvedAmountsOk(10, 3, 10, 1))
        assertTrue(approvedAmountsOk(10, 3, 5, 3))
    }

    @Test
    fun rejectsNonPositiveOrMissingAmounts() {
        assertFalse(approvedAmountsOk(10, 3, 10, 0))
        assertFalse(approvedAmountsOk(10, 3, 0, 3))
        assertFalse(approvedAmountsOk(10, 3, 10, -1))
        assertFalse(approvedAmountsOk(10, 3, null, 2))
        assertFalse(approvedAmountsOk(10, 3, 15, null))
    }
}
