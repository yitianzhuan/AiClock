package com.aiclock.smartalarm.alarm

object AlarmConstants {
    const val ACTION_ALARM_TRIGGER = "com.aiclock.smartalarm.ACTION_ALARM_TRIGGER"
    const val ACTION_DISMISS = "com.aiclock.smartalarm.ACTION_DISMISS"
    const val ACTION_SNOOZE = "com.aiclock.smartalarm.ACTION_SNOOZE"

    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_FROM_SNOOZE = "extra_from_snooze"

    const val CHANNEL_ACTIVE = "active_alarm"
    const val CHANNEL_SILENT = "silent_alarm_log"

    const val SNOOZE_MINUTES = 10L
}
