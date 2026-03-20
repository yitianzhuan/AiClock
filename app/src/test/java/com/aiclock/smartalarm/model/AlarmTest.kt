package com.aiclock.smartalarm.model

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class AlarmTest {
    private val zone = ZoneId.of("Asia/Shanghai")

    @Test
    fun nextTriggerDateTime_dailyAlarmAtTriggerTime_movesToNextDay() {
        val alarm = Alarm(
            id = 1,
            hour = 7,
            minute = 30,
            label = "Daily",
            repeatDays = (1..7).toSet()
        )

        val now = ZonedDateTime.of(2026, 3, 21, 7, 30, 0, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 3, 22, 7, 30, 0, 0, zone),
            alarm.nextTriggerDateTime(now)
        )
    }

    @Test
    fun nextTriggerDateTime_weekdayAlarmAfterToday_movesToNextValidDay() {
        val alarm = Alarm(
            id = 2,
            hour = 8,
            minute = 0,
            label = "Weekday",
            repeatDays = setOf(1, 2, 3, 4, 5)
        )

        val now = ZonedDateTime.of(2026, 3, 23, 8, 0, 1, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 3, 24, 8, 0, 0, 0, zone),
            alarm.nextTriggerDateTime(now)
        )
    }

    @Test
    fun nextTriggerDateTime_oneShotAlarmBeforeTime_staysOnSameDay() {
        val alarm = Alarm(
            id = 3,
            hour = 22,
            minute = 15,
            label = "One shot"
        )

        val now = ZonedDateTime.of(2026, 3, 21, 22, 14, 59, 0, zone)

        assertEquals(
            ZonedDateTime.of(2026, 3, 21, 22, 15, 0, 0, zone),
            alarm.nextTriggerDateTime(now)
        )
    }
}
