package com.aiclock.smartalarm.model

import java.time.DayOfWeek
import java.time.ZonedDateTime

data class Alarm(
    val id: Int,
    val hour: Int,
    val minute: Int,
    val label: String,
    val repeatDays: Set<Int> = emptySet(),
    val enabled: Boolean = true,
    val ringtoneUri: String = "",
    val ringtoneName: String = "系统默认闹钟"
) {
    fun nextTriggerDateTime(now: ZonedDateTime = ZonedDateTime.now()): ZonedDateTime {
        val reference = now
        val todayAtAlarmTime = reference
            .withHour(hour)
            .withMinute(minute)
            .withSecond(0)
            .withNano(0)

        if (repeatDays.isEmpty()) {
            return if (todayAtAlarmTime.isAfter(reference)) {
                todayAtAlarmTime
            } else {
                todayAtAlarmTime.plusDays(1)
            }
        }

        for (offset in 0..7) {
            val candidate = todayAtAlarmTime.plusDays(offset.toLong())
            val dayValue = candidate.dayOfWeek.toMondayBasedInt()
            if (dayValue in repeatDays && candidate.isAfter(reference)) {
                return candidate
            }
        }

        return todayAtAlarmTime.plusDays(1)
    }

    fun nextTriggerMillis(now: ZonedDateTime = ZonedDateTime.now()): Long {
        return nextTriggerDateTime(now).toInstant().toEpochMilli()
    }
}

fun DayOfWeek.toMondayBasedInt(): Int = value
