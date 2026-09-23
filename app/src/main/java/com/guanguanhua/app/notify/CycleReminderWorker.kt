package com.guanguanhua.app.notify

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
import androidx.work.BackoffPolicy
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.guanguanhua.app.AppViewModel
import com.guanguanhua.app.GuanGuanHuaApp
import com.guanguanhua.app.MainActivity
import com.guanguanhua.app.R
import com.guanguanhua.app.cycle.CycleCache
import com.guanguanhua.app.cycle.CycleMath
import com.guanguanhua.app.data.CycleState
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/** 在预测的下次经期首日之前，用 WorkManager 排一次本地提醒。 */
object CycleReminder {
    const val WORK_NAME = "cycle-reminder"
    const val CHANNEL_ID = "cycle_reminder"
    private const val NOTIFICATION_ID = 910_001
    private const val KEY_SCHEDULED = "scheduledKey"

    fun schedule(context: Context, state: CycleState, force: Boolean = false) {
        val app = context.applicationContext
        val prediction = CycleMath.predict(state.cycles, state.settings)
        val now = LocalDateTime.now()
        val fireAt = CycleMath.planReminder(
            nextStart = prediction.nextStart,
            remindEnabled = state.settings.remindEnabled,
            remindDays = state.settings.remindDays,
            now = now,
            notifiedStart = CycleCache.notifiedStart(app),
        )
        val immediate = fireAt != null && !fireAt.isAfter(now.plusSeconds(5))
        val key = when {
            fireAt == null -> "off"
            immediate -> "due:${prediction.nextStart}"
            else -> fireAt.toString()
        }
        val prefs = app.getSharedPreferences("cycle", Context.MODE_PRIVATE)
        if (!force && prefs.getString(KEY_SCHEDULED, null) == key) return
        prefs.edit { putString(KEY_SCHEDULED, key) }
        val wm = WorkManager.getInstance(app)
        if (fireAt == null) {
            wm.cancelUniqueWork(WORK_NAME)
            return
        }
        val delay = Duration.between(LocalDateTime.now(), fireAt).coerceAtLeast(Duration.ZERO)
        val request = OneTimeWorkRequestBuilder<CycleReminderWorker>()
            .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.MINUTES)
            .build()
        wm.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
    }

    fun scheduleFromCache(context: Context) {
        val app = context.applicationContext
        val prefs = app.getSharedPreferences("session", Context.MODE_PRIVATE)
        if (prefs.getString("token", "").isNullOrBlank()) {
            cancel(app)
            return
        }
        val memberId = prefs.getLong(AppViewModel.PREF_MEMBER_ID, 0L)
        val state = CycleCache.read(app)?.takeIf { it.memberId == memberId } ?: return
        schedule(app, state, force = true)
    }

    fun cancel(context: Context) {
        val app = context.applicationContext
        WorkManager.getInstance(app).cancelUniqueWork(WORK_NAME)
        app.getSharedPreferences("cycle", Context.MODE_PRIVATE).edit { remove(KEY_SCHEDULED) }
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

    fun notifyUpcoming(context: Context, nextStart: LocalDate): Boolean {
        val app = context.applicationContext
        if (app is GuanGuanHuaApp) app.ensureCycleChannel()
        if (!notificationsAllowed(app)) return false
        val daysLeft = ChronoUnit.DAYS.between(LocalDate.now(), nextStart).toInt().coerceAtLeast(0)
        val text = if (daysLeft <= 1) {
            "明天左右可能来，轻轻记一下就好～"
        } else {
            "大概还有 $daysLeft 天，下次可能在 ${CycleMath.formatCn(nextStart)} 左右。"
        }
        val open = Intent(app, MainActivity::class.java)
            .putExtra(MainActivity.EXTRA_OPEN_CYCLE, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        val pending = PendingIntent.getActivity(
            app,
            NOTIFICATION_ID,
            open,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return try {
            NotificationManagerCompat.from(app).notify(
                NOTIFICATION_ID,
                NotificationCompat.Builder(app, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_notification)
                    .setContentTitle("经期提醒")
                    .setContentText(text)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                    .setContentIntent(pending)
                    .setAutoCancel(true)
                    .setCategory(NotificationCompat.CATEGORY_REMINDER)
                    .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .build(),
            )
            true
        } catch (_: SecurityException) {
            false
        }
    }
}

class CycleReminderWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val app = applicationContext as? GuanGuanHuaApp ?: return Result.success()
        val prefs = app.getSharedPreferences("session", Context.MODE_PRIVATE)
        if (prefs.getString("token", "").isNullOrBlank()) return Result.success()
        val memberId = prefs.getLong(AppViewModel.PREF_MEMBER_ID, 0L)
        var state = CycleCache.read(app)?.takeIf { it.memberId == memberId } ?: return Result.success()
        runCatching {
            val cycles = app.repository.listCycles()
            val settings = app.repository.getCycleSettings()
            state = CycleState(memberId, cycles, settings, loaded = true)
            CycleCache.write(app, state)
        }
        val prediction = CycleMath.predict(state.cycles, state.settings)
        val fireAt = CycleMath.planReminder(
            nextStart = prediction.nextStart,
            remindEnabled = state.settings.remindEnabled,
            remindDays = state.settings.remindDays,
            now = LocalDateTime.now(),
            notifiedStart = CycleCache.notifiedStart(app),
        ) ?: return Result.success()
        if (fireAt.isAfter(LocalDateTime.now().plusMinutes(2))) {
            CycleReminder.schedule(app, state, force = true)
            return Result.success()
        }
        val nextStart = prediction.nextStart ?: return Result.success()
        if (!CycleReminder.notificationsAllowed(app)) return Result.success()
        if (!CycleReminder.notifyUpcoming(app, nextStart)) return Result.retry()
        CycleCache.setNotifiedStart(app, nextStart)
        return Result.success()
    }
}
