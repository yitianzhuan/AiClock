package com.aiclock.smartalarm.alarm

import com.aiclock.smartalarm.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Test

class AlarmRestoreManagerTest {
    @Test
    fun restoreEnabledAlarms_schedulesOnlyEnabledAlarms() {
        val alarms = listOf(
            Alarm(id = 1, hour = 7, minute = 30, label = "Weekday", enabled = true),
            Alarm(id = 2, hour = 8, minute = 0, label = "Disabled", enabled = false),
            Alarm(id = 3, hour = 9, minute = 15, label = "Daily", enabled = true)
        )
        val scheduledIds = mutableListOf<Int>()

        AlarmRestoreManager.restoreEnabledAlarms(alarms) { alarm ->
            scheduledIds += alarm.id
        }

        assertEquals(listOf(1, 3), scheduledIds)
    }
}
