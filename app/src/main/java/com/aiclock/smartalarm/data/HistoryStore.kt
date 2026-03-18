package com.aiclock.smartalarm.data

import android.content.Context
import com.aiclock.smartalarm.model.AlarmHistoryEntry
import org.json.JSONArray
import org.json.JSONObject

class HistoryStore(context: Context) {
    private val prefs = context.getSharedPreferences("alarm_history_store", Context.MODE_PRIVATE)

    fun add(entry: AlarmHistoryEntry) {
        val all = getAll().toMutableList()
        all.add(0, entry)
        val trimmed = all.take(MAX_RECORDS)

        val array = JSONArray()
        trimmed.forEach { array.put(toJson(it)) }
        prefs.edit().putString(KEY_HISTORY, array.toString()).apply()
    }

    fun getAll(): List<AlarmHistoryEntry> {
        val raw = prefs.getString(KEY_HISTORY, "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                add(fromJson(array.getJSONObject(i)))
            }
        }
    }

    private fun toJson(entry: AlarmHistoryEntry): JSONObject = JSONObject()
        .put("timestampMillis", entry.timestampMillis)
        .put("alarmId", entry.alarmId)
        .put("alarmTime", entry.alarmTime)
        .put("label", entry.label)
        .put("status", entry.status)

    private fun fromJson(json: JSONObject): AlarmHistoryEntry = AlarmHistoryEntry(
        timestampMillis = json.optLong("timestampMillis", 0L),
        alarmId = json.optInt("alarmId", -1),
        alarmTime = json.optString("alarmTime", "--:--"),
        label = json.optString("label", "提醒"),
        status = json.optString("status", "ACTIVE")
    )

    companion object {
        private const val KEY_HISTORY = "history"
        private const val MAX_RECORDS = 200
    }
}
