package com.aiclock.smartalarm.ui

import android.Manifest
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePaddingRelative
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aiclock.smartalarm.R
import com.aiclock.smartalarm.alarm.AlarmPlaybackManager
import com.aiclock.smartalarm.alarm.AlarmRestoreManager
import com.aiclock.smartalarm.alarm.AlarmScheduler
import com.aiclock.smartalarm.alarm.NotificationHelper
import com.aiclock.smartalarm.data.AlarmStore
import com.aiclock.smartalarm.model.Alarm
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText

class MainActivity : ComponentActivity() {
    private lateinit var alarmStore: AlarmStore
    private lateinit var scheduler: AlarmScheduler
    private lateinit var adapter: AlarmAdapter
    private lateinit var mainContent: View
    private lateinit var emptyStateCard: View
    private lateinit var alarmCountText: TextView
    private lateinit var alarmSummaryText: TextView
    private lateinit var addAlarmFab: FloatingActionButton

    private var selectedRingtoneUri: String = defaultRingtoneUri()
    private var selectedRingtoneName: String = "系统默认闹钟"
    private var pendingRingtoneNameView: TextView? = null
    private var dayChipSyncing = false

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    private val pickSystemAudio = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode != RESULT_OK) {
            return@registerForActivityResult
        }

        val data = result.data ?: return@registerForActivityResult
        val uri = data.data ?: return@registerForActivityResult

        val takeFlags = data.flags and
            (Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        runCatching {
            contentResolver.takePersistableUriPermission(uri, takeFlags)
        }

        selectedRingtoneUri = uri.toString()
        selectedRingtoneName = resolveRingtoneTitle(uri)
        pendingRingtoneNameView?.text = selectedRingtoneName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        alarmStore = AlarmStore(this)
        scheduler = AlarmScheduler(this)
        NotificationHelper.ensureChannels(this)

        mainContent = findViewById(R.id.mainContent)
        emptyStateCard = findViewById(R.id.emptyStateCard)
        alarmCountText = findViewById(R.id.alarmCountText)
        alarmSummaryText = findViewById(R.id.alarmSummaryText)
        addAlarmFab = findViewById(R.id.addAlarmFab)
        applyWindowInsets()
        setupList()
        setupActions()

        ensureRuntimePermissions()
    }

    override fun onResume() {
        super.onResume()
        AlarmRestoreManager.restoreEnabledAlarms(this)
        refreshList()
    }

    private fun setupList() {
        val recycler = findViewById<RecyclerView>(R.id.alarmList)
        recycler.layoutManager = LinearLayoutManager(this)
        adapter = AlarmAdapter(
            onToggle = { alarm, checked ->
                val updated = alarm.copy(enabled = checked)
                alarmStore.upsert(updated)
                if (checked) {
                    scheduler.schedule(updated)
                } else {
                    scheduler.cancel(alarm.id)
                    AlarmPlaybackManager.stop(this, alarm.id)
                }
                refreshList()
            },
            onLongPressDelete = { alarm ->
                MaterialAlertDialogBuilder(this)
                    .setTitle("删除闹钟")
                    .setMessage("确定删除 ${String.format("%02d:%02d", alarm.hour, alarm.minute)} 吗？")
                    .setPositiveButton("删除") { _, _ ->
                        scheduler.cancel(alarm.id)
                        AlarmPlaybackManager.stop(this, alarm.id)
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
        addAlarmFab.setOnClickListener {
            openCreateFlow()
        }
        findViewById<MaterialButton>(R.id.historyBtn).setOnClickListener {
            startActivity(Intent(this, HistoryActivity::class.java))
        }
        findViewById<View>(R.id.appInfoIconButton).setOnClickListener {
            showVersionInfo()
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
        val labelInput = formView.findViewById<TextInputEditText>(R.id.labelInput)
        labelInput.setText(existing?.label.orEmpty())

        selectedRingtoneUri = existing?.ringtoneUri?.takeIf { it.isNotBlank() } ?: defaultRingtoneUri()
        selectedRingtoneName = existing?.ringtoneName?.takeIf { it.isNotBlank() } ?: "系统默认闹钟"

        val formTimeText = formView.findViewById<TextView>(R.id.formTimeText)
        formTimeText.text = String.format("%02d:%02d", hour, minute)

        val ringtoneNameText = formView.findViewById<TextView>(R.id.ringtoneNameText)
        ringtoneNameText.text = selectedRingtoneName
        pendingRingtoneNameView = ringtoneNameText

        setupRepeatDayChips(formView, existing?.repeatDays ?: emptySet())
        setupRingtoneActions(formView)

        val title = if (existing == null) "新建闹钟" else "编辑闹钟"
        val dialog = MaterialAlertDialogBuilder(this)
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
                    enabled = existing?.enabled ?: true,
                    ringtoneUri = selectedRingtoneUri,
                    ringtoneName = selectedRingtoneName
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
        dialog.setOnDismissListener {
            pendingRingtoneNameView = null
        }
    }

    private fun setupRingtoneActions(formView: View) {
        formView.findViewById<MaterialButton>(R.id.choosePresetBtn).setOnClickListener {
            val presets = listOf(
                RingtonePreset("系统默认闹钟", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()),
                RingtonePreset("系统默认来电", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE).toString()),
                RingtonePreset("系统默认通知", RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION).toString())
            )

            val labels = presets.map { it.name }.toTypedArray()
            MaterialAlertDialogBuilder(this)
                .setTitle("选择预置铃声")
                .setItems(labels) { _, index ->
                    selectedRingtoneUri = presets[index].uri
                    selectedRingtoneName = presets[index].name
                    pendingRingtoneNameView?.text = selectedRingtoneName
                }
                .show()
        }

        formView.findViewById<MaterialButton>(R.id.chooseSystemFileBtn).setOnClickListener {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                .addCategory(Intent.CATEGORY_OPENABLE)
                .setType("audio/*")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                .addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            pickSystemAudio.launch(intent)
        }
    }

    private fun setupRepeatDayChips(formView: View, existingDays: Set<Int>) {
        val everydayChip = formView.findViewById<Chip>(R.id.dayEveryday)
        val dayIds = listOf(
            R.id.dayMon to 1,
            R.id.dayTue to 2,
            R.id.dayWed to 3,
            R.id.dayThu to 4,
            R.id.dayFri to 5,
            R.id.daySat to 6,
            R.id.daySun to 7
        )

        dayChipSyncing = true
        dayIds.forEach { (id, day) ->
            formView.findViewById<Chip>(id).isChecked = day in existingDays
        }
        everydayChip.isChecked = existingDays.size == 7
        dayChipSyncing = false

        everydayChip.setOnCheckedChangeListener { _, checked ->
            if (dayChipSyncing) return@setOnCheckedChangeListener
            dayChipSyncing = true
            dayIds.forEach { (id, _) -> formView.findViewById<Chip>(id).isChecked = checked }
            dayChipSyncing = false
        }

        dayIds.forEach { (id, _) ->
            formView.findViewById<Chip>(id).setOnCheckedChangeListener { _, _ ->
                if (dayChipSyncing) return@setOnCheckedChangeListener
                val allChecked = dayIds.all { (chipId, _) -> formView.findViewById<Chip>(chipId).isChecked }
                dayChipSyncing = true
                everydayChip.isChecked = allChecked
                dayChipSyncing = false
            }
        }
    }

    private fun collectRepeatDays(view: View): Set<Int> {
        val result = mutableSetOf<Int>()
        if (view.findViewById<Chip>(R.id.dayMon).isChecked) result += 1
        if (view.findViewById<Chip>(R.id.dayTue).isChecked) result += 2
        if (view.findViewById<Chip>(R.id.dayWed).isChecked) result += 3
        if (view.findViewById<Chip>(R.id.dayThu).isChecked) result += 4
        if (view.findViewById<Chip>(R.id.dayFri).isChecked) result += 5
        if (view.findViewById<Chip>(R.id.daySat).isChecked) result += 6
        if (view.findViewById<Chip>(R.id.daySun).isChecked) result += 7
        return result
    }

    private fun resolveRingtoneTitle(uri: Uri): String {
        val title = runCatching { RingtoneManager.getRingtone(this, uri)?.getTitle(this) }.getOrNull()
        val hasProviderStylePrefix = title?.matches(Regex("^[a-z]{2,10}:[^\\s]+$")) == true
        if (
            title.isNullOrBlank() == false &&
            title.contains('/') == false &&
            title.startsWith("raw:") == false &&
            hasProviderStylePrefix == false
        ) {
            return title
        }

        val fileName = contentResolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME),
            null,
            null,
            null
        )?.use { cursor ->
            val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && cursor.moveToFirst()) {
                cursor.getString(index)
            } else {
                null
            }
        }

        return when {
            fileName.isNullOrBlank() == false -> fileName
            title.isNullOrBlank() == false -> title.substringAfterLast('/').removePrefix("raw:")
            uri.lastPathSegment.isNullOrBlank() == false -> uri.lastPathSegment!!.substringAfterLast('/')
            else -> "本地音频"
        }
    }

    private fun defaultRingtoneUri(): String {
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
    }

    private fun applyWindowInsets() {
        val root = findViewById<View>(android.R.id.content)
        val initialStart = mainContent.paddingStart
        val initialTop = mainContent.paddingTop
        val initialEnd = mainContent.paddingEnd
        val fabLayoutParams = addAlarmFab.layoutParams as ViewGroup.MarginLayoutParams
        val initialFabBottom = fabLayoutParams.bottomMargin
        val initialFabEnd = fabLayoutParams.marginEnd

        ViewCompat.setOnApplyWindowInsetsListener(root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            mainContent.updatePaddingRelative(
                start = initialStart + systemBars.left,
                top = initialTop + systemBars.top,
                end = initialEnd + systemBars.right
            )
            addAlarmFab.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                bottomMargin = initialFabBottom + systemBars.bottom
                marginEnd = initialFabEnd + systemBars.right
            }
            insets
        }
        ViewCompat.requestApplyInsets(root)
    }

    private fun showVersionInfo() {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            packageManager.getPackageInfo(packageName, 0)
        }
        val versionName = packageInfo.versionName?.takeIf { it.isNotBlank() } ?: "unknown"
        val versionCode = packageInfo.longVersionCode

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.version_info_title)
            .setMessage(
                getString(
                    R.string.version_info_message,
                    getString(R.string.app_name),
                    versionName,
                    versionCode
                )
            )
            .setPositiveButton(R.string.version_info_confirm, null)
            .show()
    }

    private fun refreshList() {
        val alarms = alarmStore.getAll()
        val enabledCount = alarms.count { it.enabled }
        adapter.submitList(alarms)
        alarmCountText.text = String.format("%02d", alarms.size)
        alarmSummaryText.text = when {
            alarms.isEmpty() -> "点击右下角创建第一条提醒"
            enabledCount == 0 -> "已创建 ${alarms.size} 个提醒，当前全部暂停"
            enabledCount == alarms.size -> "${enabledCount} 个提醒已全部启用"
            else -> "${enabledCount} 个提醒正在运行，其余已暂停"
        }
        emptyStateCard.visibility = if (alarms.isEmpty()) View.VISIBLE else View.GONE
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

private data class RingtonePreset(
    val name: String,
    val uri: String
)
