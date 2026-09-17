package com.guanguanhua.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MoneyFormatTest {

    @Test
    fun formatsCentsAsYuan() {
        assertEquals("¥0.00", 0L.toYuan())
        assertEquals("¥12.50", 1250L.toYuan())
        assertEquals("¥1000.05", 100005L.toYuan())
        assertEquals("¥-3.00", (-300L).toYuan())
    }

    @Test
    fun parsesValidYuanInput() {
        assertEquals(1250L, "12.5".yuanToCentsOrNull())
        assertEquals(1250L, " 12.50 ".yuanToCentsOrNull())
        assertEquals(100L, "1".yuanToCentsOrNull())
        assertEquals(5L, "0.05".yuanToCentsOrNull())
    }

    @Test
    fun rejectsInvalidYuanInput() {
        assertNull("".yuanToCentsOrNull())
        assertNull("abc".yuanToCentsOrNull())
        assertNull("0".yuanToCentsOrNull())
        assertNull("-5".yuanToCentsOrNull())
        assertNull("1.234".yuanToCentsOrNull())
    }
}
