package com.aiclock.smartalarm.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.aiclock.smartalarm.R
import com.aiclock.smartalarm.model.Alarm
import com.google.android.material.materialswitch.MaterialSwitch

class AlarmAdapter(
    private val onToggle: (Alarm, Boolean) -> Unit,
    private val onLongPressDelete: (Alarm) -> Unit,
    private val onEdit: (Alarm) -> Unit
) : RecyclerView.Adapter<AlarmAdapter.AlarmViewHolder>() {

    private val items = mutableListOf<Alarm>()

    fun submitList(data: List<Alarm>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AlarmViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_alarm, parent, false)
        return AlarmViewHolder(view)
    }

    override fun onBindViewHolder(holder: AlarmViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class AlarmViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val labelText: TextView = itemView.findViewById(R.id.labelText)
        private val repeatText: TextView = itemView.findViewById(R.id.repeatText)
        private val ringtoneText: TextView = itemView.findViewById(R.id.ringtoneText)
        private val enableSwitch: MaterialSwitch = itemView.findViewById(R.id.enableSwitch)

        fun bind(alarm: Alarm) {
            timeText.text = String.format("%02d:%02d", alarm.hour, alarm.minute)
            labelText.text = if (alarm.label.isBlank()) "智能提醒" else alarm.label
            repeatText.text = if (alarm.repeatDays.isEmpty()) "单次提醒" else alarm.repeatDays.toDisplayText()
            ringtoneText.text = alarm.ringtoneName

            enableSwitch.setOnCheckedChangeListener(null)
            enableSwitch.isChecked = alarm.enabled
            enableSwitch.setOnCheckedChangeListener { _, checked -> onToggle(alarm, checked) }

            itemView.alpha = if (alarm.enabled) 1f else 0.72f

            itemView.setOnLongClickListener {
                onLongPressDelete(alarm)
                true
            }

            itemView.setOnClickListener {
                onEdit(alarm)
            }
        }
    }
}

private fun Set<Int>.toDisplayText(): String {
    if (this.size == 7) {
        return "每天"
    }
    val names = listOf("一", "二", "三", "四", "五", "六", "日")
    val selected = this.sorted().mapNotNull { day ->
        val idx = day - 1
        names.getOrNull(idx)?.let { "周$it" }
    }
    return selected.joinToString(" ")
}
