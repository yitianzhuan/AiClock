package com.aiclock.smartalarm.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val isBoot = action == Intent.ACTION_BOOT_COMPLETED
        val isLockedBoot = action == Intent.ACTION_LOCKED_BOOT_COMPLETED
        val isPackageReplaced = action == Intent.ACTION_MY_PACKAGE_REPLACED
        val isDateChanged = action == Intent.ACTION_DATE_CHANGED
        val isTimeChanged = action == Intent.ACTION_TIME_CHANGED
        val isTimeZoneChanged = action == Intent.ACTION_TIMEZONE_CHANGED
        if (
            isBoot == false &&
            isLockedBoot == false &&
            isPackageReplaced == false &&
            isDateChanged == false &&
            isTimeChanged == false &&
            isTimeZoneChanged == false
        ) {
            return
        }

        AlarmRestoreManager.restoreEnabledAlarms(context)
    }
}
