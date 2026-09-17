package com.guanguanhua.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.edit
import com.guanguanhua.app.data.ApiConfig
import com.guanguanhua.app.data.HouseholdRepository
import com.guanguanhua.app.notify.ReviewActivityWorker

class GuanGuanHuaApp : Application() {
    val repository: HouseholdRepository by lazy { HouseholdRepository(this) }

    @Volatile
    var inForeground: Boolean = false

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("session", Context.MODE_PRIVATE)
        prefs.edit(commit = true) {
            putString("serverUrl", ApiConfig.resolvedServerUrl(prefs.getString("serverUrl", null)))
        }
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
