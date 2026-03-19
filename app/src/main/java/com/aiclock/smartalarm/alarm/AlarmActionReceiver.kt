package com.aiclock.smartalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class AlarmActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getIntExtra(AlarmConstants.EXTRA_ALARM_ID, -1)
        if (alarmId < 0) {
            return
        }

        NotificationHelper.dismiss(context, alarmId)
        AlarmPlaybackManager.stop(context, alarmId)

        if (intent.action == AlarmConstants.ACTION_SNOOZE) {
            AlarmScheduler(context).scheduleSnooze(alarmId, AlarmConstants.SNOOZE_MINUTES)
        }
    }
}
