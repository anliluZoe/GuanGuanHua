package com.guanguanhua.app.notify

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.guanguanhua.app.GuanGuanHuaApp
import com.guanguanhua.app.trip.TripCache
import com.guanguanhua.app.trip.TripMath
import java.time.LocalDate
import java.util.concurrent.TimeUnit

class TripEndReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? GuanGuanHuaApp ?: return Result.retry()
        val cached = TripCache.read(app)?.active ?: return Result.success()
        if (!cached.active) return Result.success()
        val planned = cached.plannedEnd?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val today = LocalDate.now()
        val fireOn = TripMath.nextEndReminder(planned, ended = false, today = today, lastReminded = TripCache.lastReminded(app))
            ?: return Result.success()
        TripOngoing.remindEnd(app, cached.name)
        TripCache.setLastReminded(app, fireOn)
        return Result.success()
    }

    companion object {
        const val WORK_NAME = "trip-end-remind"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<TripEndReminderWorker>(12, TimeUnit.HOURS)
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
