package com.aiclock.smartalarm.alarm

import android.app.KeyguardManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import com.aiclock.smartalarm.data.AlarmStore
import com.aiclock.smartalarm.data.HistoryStore
import com.aiclock.smartalarm.model.AlarmHistoryEntry

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == AlarmConstants.ACTION_ALARM_TRIGGER) {
            handleTrigger(context, intent)
        }
    }

    private fun handleTrigger(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(AlarmConstants.EXTRA_ALARM_ID, -1)
        if (alarmId < 0) {
            return
        }

        val fromSnooze = intent.getBooleanExtra(AlarmConstants.EXTRA_FROM_SNOOZE, false)
        val store = AlarmStore(context)
        val alarm = store.getById(alarmId) ?: return
        if (fromSnooze == false && alarm.enabled == false) {
            return
        }
        val historyStore = HistoryStore(context)

        val power = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        val keyguard = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

        val isScreenOn = power.isInteractive
        val isLocked = keyguard.isKeyguardLocked

        if (isScreenOn && isLocked == false) {
            NotificationHelper.showActiveAlarm(context, alarm)
            AlarmPlaybackManager.start(context, alarm)
            historyStore.add(
                AlarmHistoryEntry(
                    timestampMillis = System.currentTimeMillis(),
                    alarmId = alarm.id,
                    alarmTime = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    label = if (alarm.label.isBlank()) "提醒" else alarm.label,
                    status = "ACTIVE"
                )
            )
        } else {
            NotificationHelper.showSilentLog(context, alarm)
            historyStore.add(
                AlarmHistoryEntry(
                    timestampMillis = System.currentTimeMillis(),
                    alarmId = alarm.id,
                    alarmTime = String.format("%02d:%02d", alarm.hour, alarm.minute),
                    label = if (alarm.label.isBlank()) "提醒" else alarm.label,
                    status = "SILENT"
                )
            )
        }

        if (fromSnooze) {
            return
        }

        val scheduler = AlarmScheduler(context)
        if (alarm.repeatDays.isEmpty()) {
            store.upsert(alarm.copy(enabled = false))
            scheduler.cancel(alarm.id)
        } else {
            scheduler.schedule(alarm)
        }
    }
}
