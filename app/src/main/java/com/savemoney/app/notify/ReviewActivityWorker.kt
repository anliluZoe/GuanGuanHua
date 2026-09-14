package com.savemoney.app.notify

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.savemoney.app.MainActivity
import com.savemoney.app.R
import com.savemoney.app.SaveMoneyApp
import com.savemoney.app.UserRole
import java.util.concurrent.TimeUnit

/** 后台定时拉一次申请列表，把对方的新动作变成系统通知。 */
class ReviewActivityWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    @SuppressLint("MissingPermission")
    override suspend fun doWork(): Result {
        val app = applicationContext as SaveMoneyApp
        val prefs = app.getSharedPreferences("session", Context.MODE_PRIVATE)
        if (prefs.getString("token", "").isNullOrBlank()) return Result.success()

        val role = runCatching { UserRole.valueOf(prefs.getString("role", UserRole.REQUESTER.name)!!) }
            .getOrDefault(UserRole.REQUESTER)
        val since = prefs.getLong(ReviewActivity.PREF_SINCE, 0L)
        val requests = runCatching { app.repository.listRequests() }.getOrElse { return Result.retry() }

        val manager = NotificationManagerCompat.from(app)
        if (since > 0L && manager.areNotificationsEnabled()) {
            ReviewActivity.newItems(requests, role, since).forEach { item ->
                val open = Intent(app, MainActivity::class.java)
                    .putExtra(MainActivity.EXTRA_REQUEST_ID, item.requestId)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                val pending = PendingIntent.getActivity(
                    app,
                    item.requestId.toInt(),
                    open,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                val notification = NotificationCompat.Builder(app, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle(item.title)
                    .setContentText(item.text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(item.text))
                    .setContentIntent(pending)
                    .setAutoCancel(true)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .build()
                manager.notify(item.requestId.toInt(), notification)
            }
        }
        prefs.edit(commit = true) { putLong(ReviewActivity.PREF_SINCE, ReviewActivity.watermark(requests, since)) }
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "review_activity"
        const val WORK_NAME = "review-activity"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReviewActivityWorker>(15, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}
