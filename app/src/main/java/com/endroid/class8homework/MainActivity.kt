package com.endroid.class8homework

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

class MainActivity : AppCompatActivity() {
    private lateinit var repo: HomeworkRepository
    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var list: RecyclerView
    private lateinit var emptyState: View
    private lateinit var emptyTitle: TextView
    private lateinit var emptyMessage: TextView
    private lateinit var statusChip: TextView
    private lateinit var retryBtn: MaterialButton
    private val adapter = SubjectAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = HomeworkRepository(this)

        swipe = findViewById(R.id.swipe)
        list = findViewById(R.id.list)
        emptyState = findViewById(R.id.emptyState)
        emptyTitle = findViewById(R.id.emptyTitle)
        emptyMessage = findViewById(R.id.emptyMessage)
        statusChip = findViewById(R.id.statusChip)
        retryBtn = findViewById(R.id.retryBtn)

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter

        swipe.setColorSchemeResources(R.color.seed)
        swipe.setOnRefreshListener { refresh(showCachedFirst = false) }
        retryBtn.setOnClickListener { refresh(showCachedFirst = false) }

        // Instant cache, then network
        repo.loadCache()?.let { renderLoaded(it, fromCache = true) }
        refresh(showCachedFirst = true)
    }

    private fun refresh(showCachedFirst: Boolean) {
        if (!showCachedFirst) swipe.isRefreshing = true
        lifecycleScope.launch {
            val result = repo.fetch()
            swipe.isRefreshing = false
            when (result) {
                is HomeworkResult.Loaded -> renderLoaded(result.data, fromCache = false)
                is HomeworkResult.Empty -> renderEmpty(result.message)
                is HomeworkResult.ApiError -> renderError(result.message)
                is HomeworkResult.NetworkError -> {
                    val cache = repo.loadCache()
                    if (cache != null) {
                        renderLoaded(cache, fromCache = true)
                        statusChip.visibility = View.VISIBLE
                        statusChip.text = result.message
                    } else {
                        renderError(result.message)
                    }
                }
            }
        }
    }

    private fun renderLoaded(data: HomeworkData, fromCache: Boolean) {
        emptyState.visibility = View.GONE
        list.visibility = View.VISIBLE
        val rows = Subjects.ALL.map { info ->
            SubjectAdapter.Row(info, data.entries[info.key])
        }
        adapter.submit(rows)
        statusChip.visibility = View.VISIBLE
        statusChip.text = when {
            fromCache -> getString(R.string.cached_label)
            data.timestamp != null -> getString(R.string.updated, formatTs(data.timestamp))
            else -> "Up to date"
        }
    }

    private fun renderEmpty(message: String) {
        list.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        emptyTitle.text = getString(R.string.no_homework)
        emptyMessage.text = message
        statusChip.visibility = View.GONE
        adapter.submit(emptyList())
    }

    private fun renderError(message: String) {
        list.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        emptyTitle.text = getString(R.string.service_unavailable)
        emptyMessage.text = message
        statusChip.visibility = View.GONE
    }

    private fun formatTs(iso: String): String = try {
        val inst = Instant.parse(iso)
        DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM)
            .withZone(ZoneId.systemDefault())
            .format(inst)
    } catch (_: Exception) {
        iso
    }
}
