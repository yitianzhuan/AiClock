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
    fun nextTriggerMillis(now: ZonedDateTime = ZonedDateTime.now()): Long {
        val base = now.withSecond(0).withNano(0)

        if (repeatDays.isEmpty()) {
            var target = base.withHour(hour).withMinute(minute)
            if (target <= base) {
                target = target.plusDays(1)
            }
            return target.toInstant().toEpochMilli()
        }

        for (offset in 0..7) {
            val candidate = base.plusDays(offset.toLong()).withHour(hour).withMinute(minute)
            val dayValue = candidate.dayOfWeek.toMondayBasedInt()
            if (dayValue in repeatDays && candidate > base) {
                return candidate.toInstant().toEpochMilli()
            }
        }

        return base.plusDays(1).withHour(hour).withMinute(minute).toInstant().toEpochMilli()
    }
}

fun DayOfWeek.toMondayBasedInt(): Int = value
