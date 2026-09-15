package com.savemoney.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.savemoney.app.data.HouseholdRepository
import com.savemoney.app.notify.ReviewActivityWorker

class SaveMoneyApp : Application() {
    val repository: HouseholdRepository by lazy { HouseholdRepository(this) }

    override fun onCreate() {
        super.onCreate()
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(ReviewActivityWorker.CHANNEL_ID, "审核动态", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "对方提交申请或给出审核结果时提醒你"
            }
        )
        if (!getSharedPreferences("session", MODE_PRIVATE).getString("token", "").isNullOrBlank()) {
            ReviewActivityWorker.schedule(this)
        }
    }
}
