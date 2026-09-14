package com.savemoney.app

import android.app.Application
import com.savemoney.app.data.HouseholdRepository

class SaveMoneyApp : Application() {
    val repository: HouseholdRepository by lazy { HouseholdRepository(this) }
}
