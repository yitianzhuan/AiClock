package com.aiclock.smartalarm.model

data class AlarmHistoryEntry(
    val timestampMillis: Long,
    val alarmId: Int,
    val alarmTime: String,
    val label: String,
    val status: String
)
