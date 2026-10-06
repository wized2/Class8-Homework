package com.endroid.class8homework

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.materialswitch.MaterialSwitch
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private lateinit var repo: HomeworkRepository
    private lateinit var aiRepo: AiRepository
    private lateinit var content: FrameLayout
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var homeView: View
    private lateinit var homeworkView: View
    private lateinit var moreView: View
    private lateinit var aiView: View

    private lateinit var greetingIcon: ImageView
    private lateinit var greetingTitle: TextView
    private lateinit var greetingDate: TextView
    private lateinit var greetingSub: TextView
    private lateinit var statDueCount: TextView
    private lateinit var statTotalCount: TextView
    private lateinit var summaryLine: TextView
    private lateinit var summaryHint: TextView
    private lateinit var homeStatus: TextView
    private lateinit var tipText: TextView
    private lateinit var btnOpenHomework: MaterialButton
    private lateinit var btnRefresh: MaterialButton

    private lateinit var swipe: SwipeRefreshLayout
    private lateinit var list: RecyclerView
    private lateinit var emptyState: View
    private lateinit var emptyTitle: TextView
    private lateinit var emptyMessage: TextView
    private lateinit var hwStatusChip: TextView
    private lateinit var retryBtn: MaterialButton

    private lateinit var adapter: SubjectAdapter
    private lateinit var markwon: Markwon
    private lateinit var chatAdapter: ChatAdapter

    private lateinit var aiList: RecyclerView
    private lateinit var aiInput: EditText
    private lateinit var aiSend: ImageButton
    private lateinit var aiBack: ImageButton
    private lateinit var aiClear: ImageButton
    private lateinit var aiWebSearch: MaterialSwitch

    private var lastData: HomeworkData? = null
    private val chatHistory = mutableListOf<AiRepository.ChatMessage>()
    private val chatUi = mutableListOf<ChatAdapter.Item>()
    private var aiBusy = false

    private val tips by lazy {
        (1..20).mapNotNull { i ->
            val id = resources.getIdentifier("tip_$i", "string", packageName)
            if (id != 0) getString(id) else null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = HomeworkRepository(this)
        aiRepo = AiRepository()
        markwon = Markwon.builder(this)
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .build()
        adapter = SubjectAdapter(markwon)
        chatAdapter = ChatAdapter(markwon)

        content = findViewById(R.id.content)
        bottomNav = findViewById(R.id.bottomNav)

        val inflater = LayoutInflater.from(this)
        homeView = inflater.inflate(R.layout.panel_home, content, false)
        homeworkView = inflater.inflate(R.layout.panel_homework, content, false)
        moreView = inflater.inflate(R.layout.panel_more, content, false)
        aiView = inflater.inflate(R.layout.panel_ai, content, false)

        bindHome(homeView)
        bindHomework(homeworkView)
        bindMore(moreView)
        bindAi(aiView)

        content.addView(homeView)
        content.addView(homeworkView)
        content.addView(moreView)
        content.addView(aiView)
        showMainPanel("home")

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    showMainPanel("home")
                    true
                }
                R.id.nav_homework -> {
                    showMainPanel("homework")
                    true
                }
                R.id.nav_more -> {
                    showMainPanel("more")
                    true
                }
                else -> false
            }
        }

        applyGreeting()
        if (tips.isNotEmpty()) {
            tipText.text = tips[LocalDate.now().dayOfYear % tips.size]
        }
        statTotalCount.text = Subjects.ALL.size.toString()

        seedWelcomeChat()

        repo.loadCache()?.let { renderLoaded(it, fromCache = true) }
        refresh()
    }

    private fun showMainPanel(name: String) {
        homeView.visibility = if (name == "home") View.VISIBLE else View.GONE
        homeworkView.visibility = if (name == "homework") View.VISIBLE else View.GONE
        moreView.visibility = if (name == "more") View.VISIBLE else View.GONE
        aiView.visibility = if (name == "ai") View.VISIBLE else View.GONE
        bottomNav.visibility = if (name == "ai") View.GONE else View.VISIBLE
    }

    private fun bindHome(v: View) {
        greetingIcon = v.findViewById(R.id.greetingIcon)
        greetingTitle = v.findViewById(R.id.greetingTitle)
        greetingDate = v.findViewById(R.id.greetingDate)
        greetingSub = v.findViewById(R.id.greetingSub)
        statDueCount = v.findViewById(R.id.statDueCount)
        statTotalCount = v.findViewById(R.id.statTotalCount)
        summaryLine = v.findViewById(R.id.summaryLine)
        summaryHint = v.findViewById(R.id.summaryHint)
        homeStatus = v.findViewById(R.id.homeStatus)
        tipText = v.findViewById(R.id.tipText)
        btnOpenHomework = v.findViewById(R.id.btnOpenHomework)
        btnRefresh = v.findViewById(R.id.btnRefresh)

        btnOpenHomework.setOnClickListener {
            bottomNav.selectedItemId = R.id.nav_homework
        }
        btnRefresh.setOnClickListener { refresh() }
    }

    private fun bindHomework(v: View) {
        swipe = v.findViewById(R.id.swipe)
        list = v.findViewById(R.id.list)
        emptyState = v.findViewById(R.id.emptyState)
        emptyTitle = v.findViewById(R.id.emptyTitle)
        emptyMessage = v.findViewById(R.id.emptyMessage)
        hwStatusChip = v.findViewById(R.id.hwStatusChip)
        retryBtn = v.findViewById(R.id.retryBtn)

        list.layoutManager = LinearLayoutManager(this)
        list.adapter = adapter
        swipe.setColorSchemeResources(R.color.seed)
        swipe.setOnRefreshListener { refresh() }
        retryBtn.setOnClickListener { refresh() }
    }

    private fun bindMore(v: View) {
        v.findViewById<MaterialCardView>(R.id.cardAiHelper).setOnClickListener {
            showMainPanel("ai")
        }
    }

    private fun bindAi(v: View) {
        aiList = v.findViewById(R.id.aiList)
        aiInput = v.findViewById(R.id.aiInput)
        aiSend = v.findViewById(R.id.aiSend)
        aiBack = v.findViewById(R.id.aiBack)
        aiClear = v.findViewById(R.id.aiClear)
        aiWebSearch = v.findViewById(R.id.aiWebSearch)

        aiList.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        aiList.adapter = chatAdapter

        aiBack.setOnClickListener {
            showMainPanel("more")
            bottomNav.selectedItemId = R.id.nav_more
        }
        aiClear.setOnClickListener {
            chatHistory.clear()
            chatUi.clear()
            seedWelcomeChat()
        }
        aiSend.setOnClickListener { sendAi() }
    }

    private fun seedWelcomeChat() {
        chatUi.clear()
        chatUi.add(ChatAdapter.Item(fromUser = false, text = getString(R.string.ai_welcome)))
        chatAdapter.submit(chatUi.toList())
    }

    private fun sendAi() {
        if (aiBusy) return
        val text = aiInput.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        aiInput.setText("")

        chatUi.add(ChatAdapter.Item(fromUser = true, text = text))
        chatHistory.add(AiRepository.ChatMessage("user", text))
        chatUi.add(ChatAdapter.Item(fromUser = false, text = getString(R.string.ai_thinking)))
        chatAdapter.submit(chatUi.toList())
        aiList.scrollToPosition(chatUi.size - 1)

        aiBusy = true
        aiSend.isEnabled = false
        lifecycleScope.launch {
            val hist = AiRepository.CLASS8_PRIMER + chatHistory.dropLast(1)
            val result = aiRepo.ask(text, hist, aiWebSearch.isChecked)
            // remove thinking
            if (chatUi.isNotEmpty() && chatUi.last().text == getString(R.string.ai_thinking)) {
                chatUi.removeAt(chatUi.lastIndex)
            }
            result.fold(
                onSuccess = { reply ->
                    chatUi.add(ChatAdapter.Item(fromUser = false, text = reply))
                    chatHistory.add(AiRepository.ChatMessage("model", reply))
                },
                onFailure = {
                    chatUi.add(
                        ChatAdapter.Item(
                            fromUser = false,
                            text = getString(R.string.ai_error) + "\n\n" + (it.message ?: "")
                        )
                    )
                    // roll back last user history on hard fail so retries are clean
                    if (chatHistory.isNotEmpty() && chatHistory.last().role == "user") {
                        chatHistory.removeAt(chatHistory.lastIndex)
                    }
                }
            )
            chatAdapter.submit(chatUi.toList())
            aiList.scrollToPosition(chatUi.size - 1)
            aiBusy = false
            aiSend.isEnabled = true
        }
    }

    private fun applyGreeting() {
        val hour = LocalTime.now().hour
        val (titleRes, iconRes) = when {
            hour in 5..11 -> R.string.greeting_morning to R.drawable.ic_sun
            hour in 12..16 -> R.string.greeting_afternoon to R.drawable.ic_cloud_sun
            hour in 17..20 -> R.string.greeting_evening to R.drawable.ic_cloud_sun
            else -> R.string.greeting_night to R.drawable.ic_moon
        }
        greetingTitle.setText(titleRes)
        greetingIcon.setImageResource(iconRes)
        greetingDate.text = LocalDate.now().format(
            DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale.getDefault())
        )
        greetingSub.setText(R.string.greeting_sub)
    }

    private fun refresh() {
        swipe.isRefreshing = true
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
                        setStatus(result.message)
                    } else {
                        renderError(result.message)
                    }
                }
            }
        }
    }

    private fun renderLoaded(data: HomeworkData, fromCache: Boolean) {
        lastData = data
        emptyState.visibility = View.GONE
        list.visibility = View.VISIBLE

        val rows = Subjects.ALL.map { info ->
            SubjectAdapter.Row(info, data.entries[info.key])
        }
        adapter.submit(rows)

        val due = data.entries.size
        val total = Subjects.ALL.size
        statDueCount.text = due.toString()
        statTotalCount.text = total.toString()

        if (due == 0) {
            summaryLine.text = getString(R.string.nothing_due)
            summaryHint.text = getString(R.string.stat_clear)
        } else {
            summaryLine.text = getString(R.string.subjects_with_work, due, total)
            summaryHint.text = getString(R.string.stat_done_hint)
        }

        val status = when {
            fromCache -> getString(R.string.cached_label)
            data.timestamp != null -> getString(R.string.updated, formatTs(data.timestamp))
            else -> "Up to date"
        }
        setStatus(status)
    }

    private fun renderEmpty(message: String) {
        lastData = HomeworkData(null, emptyMap())
        list.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        emptyTitle.text = getString(R.string.no_homework)
        emptyMessage.text = message

        statDueCount.text = "0"
        statTotalCount.text = Subjects.ALL.size.toString()
        summaryLine.text = getString(R.string.nothing_due)
        summaryHint.text = message
        setStatus(null)
        adapter.submit(emptyList())
    }

    private fun renderError(message: String) {
        list.visibility = View.GONE
        emptyState.visibility = View.VISIBLE
        emptyTitle.text = getString(R.string.service_unavailable)
        emptyMessage.text = message
        summaryLine.text = getString(R.string.service_unavailable)
        summaryHint.text = message
        setStatus(null)
    }

    private fun setStatus(text: String?) {
        if (text.isNullOrBlank()) {
            homeStatus.visibility = View.GONE
            hwStatusChip.visibility = View.GONE
        } else {
            homeStatus.visibility = View.VISIBLE
            homeStatus.text = text
            hwStatusChip.visibility = View.VISIBLE
            hwStatusChip.text = text
        }
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
