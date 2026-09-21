package com.guanguanhua.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class JoinFailureTest {
    @Test
    fun householdFullReturnsMembersForPicker() {
        val error = apiFailure(
            409,
            """{"detail":"这个家庭已经有两个人了，请选择其中一个身份进入","code":"household_full","members":[{"id":1,"name":"Alice","avatarPreset":"mascot_cat","avatarUrl":null},{"id":2,"name":"Bob","avatarPreset":"mascot_dog","avatarUrl":"http://example/b.jpg"}]}""",
            "加入失败",
        )
        assertTrue(error is HouseholdFullException)
        val full = error as HouseholdFullException
        assertEquals("这个家庭已经有两个人了，请选择其中一个身份进入", full.message)
        assertEquals(2, full.members.size)
        assertEquals("Alice", full.members[0].name)
        assertEquals("mascot_cat", full.members[0].avatarPreset)
        assertEquals("Bob", full.members[1].name)
        assertEquals("http://example/b.jpg", full.members[1].avatarUrl)
    }

    @Test
    fun otherErrorsUseServerDetail() {
        val missing = apiFailure(404, """{"detail":"找不到这个家庭码"}""", "加入失败")
        assertTrue(missing is IOException)
        assertEquals("找不到这个家庭码", missing.message)

        val leave = apiFailure(500, "{}", "退出失败")
        assertEquals("退出失败", leave.message)
    }
}
