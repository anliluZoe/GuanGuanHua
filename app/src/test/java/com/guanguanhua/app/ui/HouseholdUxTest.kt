package com.guanguanhua.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HouseholdUxTest {

    @Test
    fun leaveHouseholdCallsApiOnlyAfterConfirm() {
        assertEquals(LeaveHouseholdChoice.Stay, LeaveHouseholdPrompt.afterChoice(confirmed = false))
        assertEquals(LeaveHouseholdChoice.Leave, LeaveHouseholdPrompt.afterChoice(confirmed = true))
        assertEquals("退出家庭", LeaveHouseholdPrompt.TITLE)
        assertEquals("取消", LeaveHouseholdPrompt.CANCEL)
        assertEquals("退出", LeaveHouseholdPrompt.CONFIRM)
        assertTrue(LeaveHouseholdPrompt.BODY.contains("离开这个家庭"))
        assertTrue(LeaveHouseholdPrompt.BODY.contains("名额"))
    }

    @Test
    fun householdPairTitleFallsBackWhenNamesMissing() {
        assertEquals("我 × 另一半", householdPairTitle("", null))
        assertEquals("我 × 另一半", householdPairTitle("   ", null))
        assertEquals("小明 × 小红", householdPairTitle("小明", "小红"))
        assertEquals("小明 × 另一半", householdPairTitle("小明", null))
    }

    @Test
    fun householdCodeCopiesTrimmedTextAndSkipsBlank() {
        assertEquals("AB12CD", HouseholdCodeCopy.clipText(" AB12CD "))
        assertNull(HouseholdCodeCopy.clipText("  "))
        assertNull(HouseholdCodeCopy.clipText(""))
        assertEquals("已复制家庭码", HouseholdCodeCopy.SNACKBAR)
    }
}
