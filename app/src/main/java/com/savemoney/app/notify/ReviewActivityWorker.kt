package com.savemoney.app.notify

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.savemoney.app.MainActivity
import com.savemoney.app.R
import com.savemoney.app.SaveMoneyApp
import java.util.concurrent.TimeUnit

/** 后台拉一次申请列表，把对方的新动作变成系统通知。 */
class ReviewActivityWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? SaveMoneyApp ?: return Result.retry()
        val prefs = app.getSharedPreferences("session", Context.MODE_PRIVATE)
        if (prefs.getString("token", "").isNullOrBlank()) return Result.success()

        val since = prefs.getLong(ReviewActivity.PREF_SINCE, 0L)
        val requests = runCatching { app.repository.listRequests() }.getOrElse { return Result.retry() }
        val items = if (since > 0L) ReviewActivity.newItems(requests, since) else emptyList()

        val consume = when {
            since <= 0L || items.isEmpty() || app.inForeground -> true
            else -> notifyItems(app, items)
        }
        if (consume) {
            prefs.edit(commit = true) { putLong(ReviewActivity.PREF_SINCE, ReviewActivity.watermark(requests, since)) }
        }
        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "review_activity"
        const val WORK_NAME = "review-activity"
        const val ONCE_NAME = "review-activity-once"

        fun schedule(context: Context) {
            if (!hasSession(context)) {
                cancel(context)
                return
            }
            val request = PeriodicWorkRequestBuilder<ReviewActivityWorker>(15, TimeUnit.MINUTES, 5, TimeUnit.MINUTES)
                .setConstraints(networkConstraints())
                .build()
            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
        }

        fun enqueueSoon(context: Context, delay: Long = 0L, unit: TimeUnit = TimeUnit.SECONDS) {
            if (!hasSession(context)) return
            val work = OneTimeWorkRequestBuilder<ReviewActivityWorker>()
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

        fun notificationsAllowed(context: Context): Boolean {
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
            val channel = context.getSystemService(NotificationManager::class.java).getNotificationChannel(CHANNEL_ID)
            return channel == null || channel.importance != NotificationManager.IMPORTANCE_NONE
        }

        fun notifyItems(context: Context, items: List<ActivityItem>): Boolean {
            if (items.isEmpty()) return true
            val app = context.applicationContext
            if (app is SaveMoneyApp) app.ensureReviewChannel()
            if (!notificationsAllowed(app)) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(app, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            ) {
                return false
            }
            val nm = app.getSystemService(NotificationManager::class.java)
            return try {
                items.forEach { item ->
                    val open = Intent(app, MainActivity::class.java)
                        .putExtra(MainActivity.EXTRA_REQUEST_ID, item.requestId)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    val pending = PendingIntent.getActivity(
                        app,
                        item.requestId.toInt(),
                        open,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    )
                    nm.notify(
                        item.requestId.toInt(),
                        NotificationCompat.Builder(app, CHANNEL_ID)
                            .setSmallIcon(R.drawable.ic_notification)
                            .setContentTitle(item.title)
                            .setContentText(item.text)
                            .setStyle(NotificationCompat.BigTextStyle().bigText(item.text))
                            .setContentIntent(pending)
                            .setAutoCancel(true)
                            .setOnlyAlertOnce(true)
                            .setCategory(NotificationCompat.CATEGORY_REMINDER)
                            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                            .setPriority(NotificationCompat.PRIORITY_HIGH)
                            .setDefaults(NotificationCompat.DEFAULT_ALL)
                            .build(),
                    )
                }
                true
            } catch (_: SecurityException) {
                false
            }
        }

        private fun networkConstraints(): Constraints =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        private fun hasSession(context: Context): Boolean =
            !context.getSharedPreferences("session", Context.MODE_PRIVATE).getString("token", "").isNullOrBlank()
    }
}
