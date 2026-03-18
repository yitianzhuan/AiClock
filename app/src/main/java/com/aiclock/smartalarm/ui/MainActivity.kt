package com.aiclock.smartalarm.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.LayoutInflater
import android.widget.CheckBox
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aiclock.smartalarm.R
import com.aiclock.smartalarm.alarm.AlarmScheduler
import com.aiclock.smartalarm.alarm.NotificationHelper
import com.aiclock.smartalarm.data.AlarmStore
import com.aiclock.smartalarm.model.Alarm
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar

class MainActivity : ComponentActivity() {
    private lateinit var alarmStore: AlarmStore
    private lateinit var scheduler: AlarmScheduler
    private lateinit var adapter: AlarmAdapter
    private lateinit var emptyText: TextView

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        alarmStore = AlarmStore(this)
        scheduler = AlarmScheduler(this)
        NotificationHelper.ensureChannels(this)

        emptyText = findViewById(R.id.emptyText)
        setupList()
        setupActions()

        ensureRuntimePermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshList()
    }

    private fun setupList() {
        val recycler = findViewById<RecyclerView>(R.id.alarmList)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = AlarmAdapter(
            onToggle = { alarm, checked ->
                val updated = alarm.copy(enabled = checked)
                alarmStore.upsert(updated)
                if (checked) scheduler.schedule(updated) else scheduler.cancel(alarm.id)
                refreshList()
            },
            onLongPressDelete = { alarm ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("删除闹钟")
                    .setMessage("确定删除 ${String.format("%02d:%02d", alarm.hour, alarm.minute)} 吗？")
                    .setPositiveButton("删除") { _, _ ->
                        scheduler.cancel(alarm.id)
                        alarmStore.delete(alarm.id)
                        refreshList()
                    }
                    .setNegativeButton("取消", null)
                    .show()
            },
            onEdit = { alarm ->
                openEditFlow(alarm)
            },
        )
        recycler.adapter = adapter
    }

    private fun setupActions() {
        findViewById<FloatingActionButton>(R.id.addAlarmFab).setOnClickListener {
            openCreateFlow()
        }
        findViewById<MaterialButton>(R.id.historyBtn).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
    }

    private fun openCreateFlow() {
        openAlarmFlow(existing = null)
    }

    private fun openEditFlow(alarm: Alarm) {
        openAlarmFlow(existing = alarm)
    }

    private fun openAlarmFlow(existing: Alarm?) {
        val now = java.util.Calendar.getInstance()
        val initialHour = existing?.hour ?: now.get(java.util.Calendar.HOUR_OF_DAY)
        val initialMinute = existing?.minute ?: now.get(java.util.Calendar.MINUTE)

        TimePickerDialog(
            this,
            { _, hour, minute ->
                showAlarmFormDialog(hour, minute, existing)
            },
            initialHour,
            initialMinute,
            true
        ).show()
    }

    private fun showAlarmFormDialog(hour: Int, minute: Int, existing: Alarm?) {
        val formView = LayoutInflater.from(this).inflate(R.layout.dialog_alarm_form, null)
        val labelInput = formView.findViewById<TextView>(R.id.labelInput)
        labelInput.text = existing?.label.orEmpty()

        setDayChecked(formView, R.id.dayMon, existing?.repeatDays?.contains(1) == true)
        setDayChecked(formView, R.id.dayTue, existing?.repeatDays?.contains(2) == true)
        setDayChecked(formView, R.id.dayWed, existing?.repeatDays?.contains(3) == true)
        setDayChecked(formView, R.id.dayThu, existing?.repeatDays?.contains(4) == true)
        setDayChecked(formView, R.id.dayFri, existing?.repeatDays?.contains(5) == true)
        setDayChecked(formView, R.id.daySat, existing?.repeatDays?.contains(6) == true)
        setDayChecked(formView, R.id.daySun, existing?.repeatDays?.contains(7) == true)

        val title = if (existing == null) "新建闹钟" else "编辑闹钟"
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setView(formView)
            .setPositiveButton("保存") { _, _ ->
                val repeatDays = collectRepeatDays(formView)
                val alarm = Alarm(
                    id = existing?.id ?: alarmStore.nextId(),
                    hour = hour,
                    minute = minute,
                    label = labelInput.text?.toString()?.trim().orEmpty(),
                    repeatDays = repeatDays,
                    enabled = existing?.enabled ?: true
                )
                alarmStore.upsert(alarm)
                if (alarm.enabled) {
                    scheduler.schedule(alarm)
                } else {
                    scheduler.cancel(alarm.id)
                }
                refreshList()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun setDayChecked(view: android.view.View, id: Int, checked: Boolean) {
        view.findViewById<CheckBox>(id).isChecked = checked
    }

    private fun collectRepeatDays(view: android.view.View): Set<Int> {
        val result = mutableSetOf<Int>()
        if (view.findViewById<CheckBox>(R.id.dayMon).isChecked) result += 1
        if (view.findViewById<CheckBox>(R.id.dayTue).isChecked) result += 2
        if (view.findViewById<CheckBox>(R.id.dayWed).isChecked) result += 3
        if (view.findViewById<CheckBox>(R.id.dayThu).isChecked) result += 4
        if (view.findViewById<CheckBox>(R.id.dayFri).isChecked) result += 5
        if (view.findViewById<CheckBox>(R.id.daySat).isChecked) result += 6
        if (view.findViewById<CheckBox>(R.id.daySun).isChecked) result += 7
        return result
    }

    private fun refreshList() {
        val alarms = alarmStore.getAll()
        adapter.submitList(alarms)
        emptyText.visibility = if (alarms.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }

    private fun ensureRuntimePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (granted == false) {
                requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && scheduler.canScheduleExactAlarms() == false) {
            Snackbar.make(
                findViewById(android.R.id.content),
                "建议开启精准闹钟权限，确保提醒准时",
                Snackbar.LENGTH_LONG
            ).setAction("去开启") {
                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            }.show()
        }
    }
}
