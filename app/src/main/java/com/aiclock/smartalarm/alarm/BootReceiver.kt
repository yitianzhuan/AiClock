package com.aiclock.smartalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aiclock.smartalarm.data.AlarmStore

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isBoot = action == Intent.ACTION_BOOT_COMPLETED
        val isLockedBoot = action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        if (isBoot == false && isLockedBoot == false) {
            return
        }

        NotificationHelper.ensureChannels(context)

        val scheduler = AlarmScheduler(context)
        val store = AlarmStore(context)
        store.getAll().forEach { alarm ->
            if (alarm.enabled) {
                scheduler.schedule(alarm)
            }
        }
    }
}
