package com.savemoney.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import com.savemoney.app.data.HouseholdRepository
import com.savemoney.app.notify.ReviewActivityWorker

class SaveMoneyApp : Application() {
    val repository: HouseholdRepository by lazy { HouseholdRepository(this) }

    @Volatile
    var inForeground: Boolean = false

    override fun onCreate() {
        super.onCreate()
        ensureReviewChannel()
        ReviewActivityWorker.schedule(this)
    }

    fun ensureReviewChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(ReviewActivityWorker.CHANNEL_ID, "审核动态", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "对方提交申请或给出审核结果时提醒你"
                enableVibration(true)
                enableLights(true)
                setShowBadge(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            },
        )
    }
}
