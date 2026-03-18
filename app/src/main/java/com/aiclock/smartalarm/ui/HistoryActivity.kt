package com.aiclock.smartalarm.ui

import android.os.Bundle
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aiclock.smartalarm.R
import com.aiclock.smartalarm.data.HistoryStore

class HistoryActivity : ComponentActivity() {
    private lateinit var historyStore: HistoryStore
    private lateinit var adapter: HistoryAdapter
    private lateinit var emptyText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyStore = HistoryStore(this)
        emptyText = findViewById(R.id.emptyHistoryText)

        val list = findViewById<RecyclerView>(R.id.historyList)
        list.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter()
        list.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        val data = historyStore.getAll()
        adapter.submitList(data)
        emptyText.visibility = if (data.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE
    }
}
