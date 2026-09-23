package com.guanguanhua.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.edit
import com.guanguanhua.app.data.ApiConfig
import com.guanguanhua.app.data.HouseholdRepository
import com.guanguanhua.app.notify.CycleReminder
import com.guanguanhua.app.notify.ReviewActivityWorker
import com.guanguanhua.app.update.ApkDownloadCoordinator
import com.guanguanhua.app.update.AppUpdates
import com.guanguanhua.app.widget.WidgetRefreshWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class GuanGuanHuaApp : Application() {
    val repository: HouseholdRepository by lazy { HouseholdRepository(this) }

    /** 跟进程走，切换底部 Tab 或进入编辑页时下载不会被取消。 */
    val updateDownloads: ApkDownloadCoordinator by lazy {
        ApkDownloadCoordinator(
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
            download = { update, onProgress ->
                AppUpdates.downloadApk(update.apkUrl, AppUpdates.apkFile(this@GuanGuanHuaApp), onProgress)
            },
            install = { _ ->
                AppUpdates.installApk(this@GuanGuanHuaApp, AppUpdates.apkFile(this@GuanGuanHuaApp))
            },
        )
    }

    @Volatile
    var inForeground: Boolean = false

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("session", Context.MODE_PRIVATE)
        prefs.edit(commit = true) {
            putString("serverUrl", ApiConfig.resolvedServerUrl(prefs.getString("serverUrl", null)))
        }
        ensureReviewChannel()
        ensureCycleChannel()
        ReviewActivityWorker.schedule(this)
        CycleReminder.scheduleFromCache(this)
        WidgetRefreshWorker.schedule(this)
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

    fun ensureCycleChannel() {
        getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CycleReminder.CHANNEL_ID, "经期提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "在预测的下次经期之前轻轻提醒你"
                enableVibration(true)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PRIVATE
            },
        )
    }
}
