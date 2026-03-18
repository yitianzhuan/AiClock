package com.aiclock.smartalarm.data

import android.content.Context
import com.aiclock.smartalarm.model.Alarm
import org.json.JSONArray
import org.json.JSONObject

class AlarmStore(context: Context) {
    private val prefs = context.getSharedPreferences("alarm_store", Context.MODE_PRIVATE)

    fun getAll(): List<Alarm> {
        val raw = prefs.getString(KEY_ALARMS, "[]") ?: "[]"
        val array = JSONArray(raw)
        return buildList {
            for (i in 0 until array.length()) {
                add(fromJson(array.getJSONObject(i)))
            }
        }.sortedWith(compareBy({ it.hour }, { it.minute }))
    }

    fun getById(id: Int): Alarm? = getAll().firstOrNull { it.id == id }

    fun upsert(alarm: Alarm) {
        val list = getAll().toMutableList()
        val index = list.indexOfFirst { it.id == alarm.id }
        if (index >= 0) {
            list[index] = alarm
        } else {
            list.add(alarm)
        }
        saveAll(list)
    }

    fun delete(alarmId: Int) {
        val list = getAll().filterNot { it.id == alarmId }
        saveAll(list)
    }

    fun nextId(): Int = (getAll().maxOfOrNull { it.id } ?: 0) + 1

    private fun saveAll(alarms: List<Alarm>) {
        val array = JSONArray()
        alarms.forEach { array.put(toJson(it)) }
        prefs.edit().putString(KEY_ALARMS, array.toString()).apply()
    }

    private fun toJson(alarm: Alarm): JSONObject = JSONObject()
        .put("id", alarm.id)
        .put("hour", alarm.hour)
        .put("minute", alarm.minute)
        .put("label", alarm.label)
        .put("enabled", alarm.enabled)
        .put("repeatDays", JSONArray(alarm.repeatDays.sorted()))

    private fun fromJson(json: JSONObject): Alarm {
        val repeatArray = json.optJSONArray("repeatDays") ?: JSONArray()
        val repeatDays = mutableSetOf<Int>()
        for (i in 0 until repeatArray.length()) {
            repeatDays += repeatArray.getInt(i)
        }

        return Alarm(
            id = json.getInt("id"),
            hour = json.getInt("hour"),
            minute = json.getInt("minute"),
            label = json.optString("label", "提醒"),
            repeatDays = repeatDays,
            enabled = json.optBoolean("enabled", true)
        )
    }

    companion object {
        private const val KEY_ALARMS = "alarms"
    }
}
