package com.guanguanhua.app

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.edit
import androidx.work.WorkManager
import com.guanguanhua.app.data.ApiConfig
import com.guanguanhua.app.data.HouseholdRepository
import com.guanguanhua.app.notify.CycleReminder
import com.guanguanhua.app.notify.TripEndReminderWorker
import com.guanguanhua.app.notify.TripOngoing
import com.guanguanhua.app.trip.TripCache
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

    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("session", Context.MODE_PRIVATE)
        prefs.edit(commit = true) {
            putString("serverUrl", ApiConfig.resolvedServerUrl(prefs.getString("serverUrl", null)))
        }
        clearOldReviewActivity()
        ensureCycleChannel()
        TripOngoing.ensureChannels(this)
        CycleReminder.scheduleFromCache(this)
        TripOngoing.refresh(this, TripCache.read(this)?.active)
        TripEndReminderWorker.schedule(this)
        WidgetRefreshWorker.schedule(this)
    }

    /** 申请/审核功能已移除；升级后清掉旧版本留下的后台任务和通知通道。 */
    private fun clearOldReviewActivity() {
        val work = WorkManager.getInstance(this)
        work.cancelUniqueWork("review-activity")
        work.cancelUniqueWork("review-activity-once")
        getSystemService(NotificationManager::class.java).deleteNotificationChannel("review_activity")
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
