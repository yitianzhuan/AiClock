package com.aiclock.smartalarm.alarm

import android.content.Context
import com.aiclock.smartalarm.data.AlarmStore
import com.aiclock.smartalarm.model.Alarm

object AlarmRestoreManager {
    fun restoreEnabledAlarms(context: Context) {
        NotificationHelper.ensureChannels(context)

        val scheduler = AlarmScheduler(context)
        restoreEnabledAlarms(AlarmStore(context).getAll(), scheduler::schedule)
    }

    internal fun restoreEnabledAlarms(alarms: List<Alarm>, scheduleAlarm: (Alarm) -> Unit) {
        alarms.forEach { alarm ->
            if (alarm.enabled) {
                scheduleAlarm(alarm)
            }
        }
    }
}
