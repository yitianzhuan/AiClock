package com.aiclock.smartalarm.alarm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.aiclock.smartalarm.R
import com.aiclock.smartalarm.model.Alarm
import com.aiclock.smartalarm.ui.MainActivity

object NotificationHelper {
    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val active = NotificationChannel(
            AlarmConstants.CHANNEL_ACTIVE,
            "Active Alarm",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Triggers when phone is in active use"
            enableVibration(false)
            setSound(null, null)
            lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
        }

        val silent = NotificationChannel(
            AlarmConstants.CHANNEL_SILENT,
            "Silent Alarm Log",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Records ignored alarms when phone not in use"
            setSound(null, null)
            enableVibration(false)
        }

        manager.createNotificationChannel(active)
        manager.createNotificationChannel(silent)
    }

    fun showActiveAlarm(context: Context, alarm: Alarm) {
        ensureChannels(context)

        val openIntent = PendingIntent.getActivity(
            context,
            alarm.id,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dismissIntent = actionIntent(context, AlarmConstants.ACTION_DISMISS, alarm.id)
        val snoozeIntent = actionIntent(context, AlarmConstants.ACTION_SNOOZE, alarm.id)

        val title = String.format("%02d:%02d", alarm.hour, alarm.minute)
        val body = if (alarm.label.isBlank()) "提醒时间到了" else alarm.label

        val notification = NotificationCompat.Builder(context, AlarmConstants.CHANNEL_ACTIVE)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(openIntent)
            .setAutoCancel(false)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(true)
            .addAction(0, context.getString(R.string.dismiss), dismissIntent)
            .addAction(0, context.getString(R.string.snooze_10m), snoozeIntent)
            .build()

        NotificationManagerCompat.from(context).notify(alarm.id, notification)
    }

    fun showSilentLog(context: Context, alarm: Alarm) {
        ensureChannels(context)
        val text = if (alarm.label.isBlank()) "该提醒触发时你未使用手机，已静默忽略" else "${alarm.label}：触发时未使用手机，已静默忽略"

        val notification = NotificationCompat.Builder(context, AlarmConstants.CHANNEL_SILENT)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(String.format("%02d:%02d 静默记录", alarm.hour, alarm.minute))
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setSilent(true)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(alarm.id + 50_000, notification)
    }

    fun dismiss(context: Context, alarmId: Int) {
        NotificationManagerCompat.from(context).cancel(alarmId)
    }

    private fun actionIntent(context: Context, action: String, alarmId: Int): PendingIntent {
        val intent = Intent(context, AlarmActionReceiver::class.java)
            .setAction(action)
            .putExtra(AlarmConstants.EXTRA_ALARM_ID, alarmId)

        val requestCode = when (action) {
            AlarmConstants.ACTION_DISMISS -> alarmId + 200_000
            else -> alarmId + 300_000
        }

        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
