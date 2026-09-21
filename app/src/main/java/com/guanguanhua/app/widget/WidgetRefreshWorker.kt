package com.guanguanhua.app.widget

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.guanguanhua.app.GuanGuanHuaApp
import java.util.concurrent.TimeUnit

/** 拉家庭共享组件状态，刷新桌面上的照片和说明。 */
class WidgetRefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? GuanGuanHuaApp ?: return Result.retry()
        val prefs = app.getSharedPreferences("session", Context.MODE_PRIVATE)
        if (prefs.getString("token", "").isNullOrBlank()) {
            WidgetCache.clear(app)
            WidgetCache.publish(app)
            return Result.success()
        }
        val remote = runCatching { app.repository.getWidget() }.getOrElse { return Result.retry() }
        WidgetCache.applyRemote(app, remote)
        WidgetCache.publish(app)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "household-widget"
        const val ONCE_NAME = "household-widget-once"

        fun schedule(context: Context) {
            if (!hasSession(context)) {
                cancel(context)
                return
            }
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES, 5, TimeUnit.MINUTES)
                .setConstraints(networkConstraints())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun enqueueSoon(context: Context, delay: Long = 0L, unit: TimeUnit = TimeUnit.SECONDS) {
            if (!hasSession(context)) return
            val work = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
                .setConstraints(networkConstraints())
                .setInitialDelay(delay, unit)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniqueWork(ONCE_NAME, ExistingWorkPolicy.REPLACE, work)
        }

        fun cancel(context: Context) {
            val wm = WorkManager.getInstance(context)
            wm.cancelUniqueWork(WORK_NAME)
            wm.cancelUniqueWork(ONCE_NAME)
        }

        private fun networkConstraints(): Constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        private fun hasSession(context: Context): Boolean =
            !context.getSharedPreferences("session", Context.MODE_PRIVATE).getString("token", "").isNullOrBlank()
    }
}
