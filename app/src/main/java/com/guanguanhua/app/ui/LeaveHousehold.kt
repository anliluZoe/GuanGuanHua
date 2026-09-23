package com.guanguanhua.app.ui

object LeaveHouseholdPrompt {
    const val TITLE = "退出家庭"
    const val BODY = "退出后你会离开这个家庭，并可能空出一个名额。"
    const val CONFIRM = "退出"
    const val CANCEL = "取消"

    fun afterChoice(confirmed: Boolean): LeaveHouseholdChoice =
        if (confirmed) LeaveHouseholdChoice.Leave else LeaveHouseholdChoice.Stay
}

enum class LeaveHouseholdChoice {
    Stay,
    Leave,
}
