package com.guanguanhua.app.notify

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import com.guanguanhua.app.MainActivity
import com.guanguanhua.app.R
import com.guanguanhua.app.data.TripDetail

object TripOngoing {
    const val CHANNEL_ID = "trip_ongoing"
    const val REMIND_CHANNEL_ID = "trip_end_remind"
    private const val ONGOING_ID = 920_001
    private const val REMIND_ID = 920_002

    fun ensureChannels(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "旅行中", NotificationManager.IMPORTANCE_LOW).apply {
                description = "开始旅程后一直挂着，方便记一站"
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(REMIND_CHANNEL_ID, "旅程结束提醒", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "到了预计结束日时问问还要不要继续"
            },
        )
    }

    fun refresh(context: Context, active: TripDetail?) {
        ensureChannels(context)
        if (active == null) {
            NotificationManagerCompat.from(context).cancel(ONGOING_ID)
            return
        }
        if (!canNotify(context)) return
        val record = pending(context, MainActivity.EXTRA_OPEN_RECORD_STOP, 31)
        val end = pending(context, MainActivity.EXTRA_CONFIRM_END_TRIP, 32)
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("旅行中 · ${active.name}")
            .setContentText("到店了就记一站")
            .setOngoing(true)
            .setContentIntent(record)
            .addAction(0, "记一站", record)
            .addAction(0, "结束旅程", end)
            .setSilent(true)
            .build()
        NotificationManagerCompat.from(context).notify(ONGOING_ID, notification)
    }

    fun remindEnd(context: Context, name: String) {
        ensureChannels(context)
        if (!canNotify(context)) return
        val end = pending(context, MainActivity.EXTRA_CONFIRM_END_TRIP, 33)
        val keep = pending(context, MainActivity.EXTRA_TRIP_KEEP_GOING, 34)
        val notification = NotificationCompat.Builder(context, REMIND_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("「$name」预计今天结束")
            .setContentText("要结束这段旅程吗？还可以继续。")
            .setContentIntent(end)
            .addAction(0, "结束旅程", end)
            .addAction(0, "还在继续", keep)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(REMIND_ID, notification)
    }

    fun cancelRemind(context: Context) {
        NotificationManagerCompat.from(context).cancel(REMIND_ID)
    }

    private fun pending(context: Context, extra: String, requestCode: Int): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            .putExtra(extra, true)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun canNotify(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }
}
