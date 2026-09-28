package com.guanguanhua.app.ui

object LeaveHouseholdPrompt {
    const val TITLE = "退出家庭"
    const val BODY = "这台手机会退出登录。家里的账本、申请和经期都还在，用家庭码可以再进来。"
    const val CONFIRM = "退出"
    const val CANCEL = "取消"

    fun afterChoice(confirmed: Boolean): LeaveHouseholdChoice =
        if (confirmed) LeaveHouseholdChoice.Leave else LeaveHouseholdChoice.Stay
}

enum class LeaveHouseholdChoice {
    Stay,
    Leave,
}
