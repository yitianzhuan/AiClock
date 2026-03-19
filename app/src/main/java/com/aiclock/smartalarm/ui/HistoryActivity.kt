package com.aiclock.smartalarm.ui

import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.aiclock.smartalarm.R
import com.aiclock.smartalarm.data.HistoryStore
import com.google.android.material.button.MaterialButton

class HistoryActivity : ComponentActivity() {
    private lateinit var historyStore: HistoryStore
    private lateinit var adapter: HistoryAdapter
    private lateinit var emptyState: View

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        historyStore = HistoryStore(this)
        emptyState = findViewById(R.id.emptyHistoryState)

        findViewById<MaterialButton>(R.id.backBtn).setOnClickListener {
            finish()
        }

        val list = findViewById<RecyclerView>(R.id.historyList)
        list.layoutManager = LinearLayoutManager(this)
        adapter = HistoryAdapter()
        list.adapter = adapter
    }

    override fun onResume() {
        super.onResume()
        val data = historyStore.getAll()
        adapter.submitList(data)
        emptyState.visibility = if (data.isEmpty()) View.VISIBLE else View.GONE
    }
}
