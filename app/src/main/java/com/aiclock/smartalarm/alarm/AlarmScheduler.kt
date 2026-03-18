package com.aiclock.smartalarm.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.aiclock.smartalarm.model.Alarm
import java.time.ZonedDateTime

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(alarm: Alarm) {
        if (alarm.enabled == false) {
            cancel(alarm.id)
            return
        }

        val triggerAtMillis = alarm.nextTriggerMillis()
        val pending = alarmPendingIntent(alarm.id, fromSnooze = false)
        scheduleExact(triggerAtMillis, pending)
    }

    fun scheduleSnooze(alarmId: Int, minutesFromNow: Long) {
        val trigger = ZonedDateTime.now().plusMinutes(minutesFromNow).toInstant().toEpochMilli()
        val pending = alarmPendingIntent(alarmId, fromSnooze = true)
        scheduleExact(trigger, pending)
    }

    fun cancel(alarmId: Int) {
        alarmManager.cancel(alarmPendingIntent(alarmId, fromSnooze = false))
        alarmManager.cancel(alarmPendingIntent(alarmId, fromSnooze = true))
    }

    fun canScheduleExactAlarms(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            return true
        }
        return alarmManager.canScheduleExactAlarms()
    }

    private fun scheduleExact(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() == false) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            return
        }
        alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
    }

    private fun alarmPendingIntent(alarmId: Int, fromSnooze: Boolean): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java)
            .setAction(AlarmConstants.ACTION_ALARM_TRIGGER)
            .putExtra(AlarmConstants.EXTRA_ALARM_ID, alarmId)
            .putExtra(AlarmConstants.EXTRA_FROM_SNOOZE, fromSnooze)

        val requestCode = if (fromSnooze) alarmId + 100_000 else alarmId
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
