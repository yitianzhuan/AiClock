package com.aiclock.smartalarm.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aiclock.smartalarm.R
import com.aiclock.smartalarm.model.AlarmHistoryEntry
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class HistoryAdapter : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {
    private val items = mutableListOf<AlarmHistoryEntry>()
    private val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    fun submitList(data: List<AlarmHistoryEntry>) {
        items.clear()
        items.addAll(data)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val titleText: TextView = itemView.findViewById(R.id.titleText)
        private val timeText: TextView = itemView.findViewById(R.id.timeText)
        private val statusText: TextView = itemView.findViewById(R.id.statusText)

        fun bind(entry: AlarmHistoryEntry) {
            titleText.text = if (entry.label.isBlank()) {
                "${entry.alarmTime} · 智能提醒"
            } else {
                "${entry.alarmTime} · ${entry.label}"
            }
            timeText.text = "触发时间：${formatter.format(Date(entry.timestampMillis))}"

            if (entry.status == "ACTIVE") {
                statusText.text = "已提醒（你正在使用手机）"
                statusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.history_active))
                statusText.setBackgroundResource(R.drawable.bg_history_badge_active)
            } else {
                statusText.text = "静默忽略（你未使用手机）"
                statusText.setTextColor(ContextCompat.getColor(itemView.context, R.color.history_muted))
                statusText.setBackgroundResource(R.drawable.bg_history_badge_muted)
            }
        }
    }
}
