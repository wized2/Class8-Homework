package com.endroid.class8homework

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.textfield.TextInputEditText
import io.noties.markwon.Markwon
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var repo: HomeworkRepository
    private lateinit var aiRepo: AiRepository
    private lateinit var profile: UserProfile
    private lateinit var content: FrameLayout
    private lateinit var bottomNav: BottomNavigationView

    private lateinit var homeView: View
    private lateinit var homeworkView: View
    private lateinit var moreView: View
    private lateinit var aiView: View
    private lateinit var settingsView: View

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

    private lateinit var settingsName: TextInputEditText
    private lateinit var settingsFaith: Spinner
    private lateinit var settingsTheme: Spinner

    private var lastData: HomeworkData? = null
    private val chatHistory = mutableListOf<AiRepository.ChatMessage>()
    private val chatUi = mutableListOf<ChatAdapter.Item>()
    private var aiBusy = false

    /** home | homework | more | ai | settings */
    private var currentPanel = "home"

    private val tipPrefs by lazy { getSharedPreferences("tips", MODE_PRIVATE) }

    override fun onCreate(savedInstanceState: Bundle?) {
        profile = UserProfile(this)
        profile.applyTheme()
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        repo = HomeworkRepository(this)
        aiRepo = AiRepository()
        markwon = MarkdownKit.create(this)
        adapter = SubjectAdapter(markwon)
        chatAdapter = ChatAdapter(markwon)

        content = findViewById(R.id.content)
        bottomNav = findViewById(R.id.bottomNav)

        val inflater = LayoutInflater.from(this)
        homeView = inflater.inflate(R.layout.panel_home, content, false)
        homeworkView = inflater.inflate(R.layout.panel_homework, content, false)
        moreView = inflater.inflate(R.layout.panel_more, content, false)
        aiView = inflater.inflate(R.layout.panel_ai, content, false)
        settingsView = inflater.inflate(R.layout.panel_settings, content, false)

        bindHome(homeView)
        bindHomework(homeworkView)
        bindMore(moreView)
        bindAi(aiView)
        bindSettings(settingsView)

        content.addView(homeView)
        content.addView(homeworkView)
        content.addView(moreView)
        content.addView(aiView)
        content.addView(settingsView)
        showMainPanel("home")

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> { showMainPanel("home"); true }
                R.id.nav_homework -> { showMainPanel("homework"); true }
                R.id.nav_more -> { showMainPanel("more"); true }
                else -> false
            }
        }

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                when (currentPanel) {
                    "ai", "settings" -> {
                        showMainPanel("more")
                        bottomNav.selectedItemId = R.id.nav_more
                    }
                    "homework", "more" -> {
                        showMainPanel("home")
                        bottomNav.selectedItemId = R.id.nav_home
                    }
                    else -> {
                        isEnabled = false
                        onBackPressedDispatcher.onBackPressed()
                        isEnabled = true
                    }
                }
            }
        })

        applyGreeting()
        applyTip()
        updateSubjectStatsPlaceholder()

        if (!profile.isComplete) {
            showOnboarding()
        } else {
            seedWelcomeChat()
            repo.loadCache()?.let { renderLoaded(it, fromCache = true) }
            refresh()
        }
    }

    private fun showOnboarding() {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setCancelable(false)
        val view = layoutInflater.inflate(R.layout.dialog_onboarding, null)
        dialog.setContentView(view)
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.92).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )

        val inputName = view.findViewById<TextInputEditText>(R.id.inputName)
        val spinner = view.findViewById<Spinner>(R.id.spinnerFaith)
        val options = listOf(getString(R.string.faith_muslim), getString(R.string.faith_non_muslim))
        spinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, options)

        view.findViewById<MaterialButton>(R.id.btnSaveProfile).setOnClickListener {
            val name = inputName.text?.toString().orEmpty().trim()
            if (name.isBlank()) {
                Toast.makeText(this, R.string.name_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            profile.fullName = name
            profile.isMuslim = spinner.selectedItemPosition == 0
            dialog.dismiss()
            applyGreeting()
            seedWelcomeChat()
            repo.loadCache()?.let { renderLoaded(it, fromCache = true) }
            refresh()
        }
        dialog.show()
    }

    private fun showMainPanel(name: String) {
        currentPanel = name
        homeView.visibility = if (name == "home") View.VISIBLE else View.GONE
        homeworkView.visibility = if (name == "homework") View.VISIBLE else View.GONE
        moreView.visibility = if (name == "more") View.VISIBLE else View.GONE
        aiView.visibility = if (name == "ai") View.VISIBLE else View.GONE
        settingsView.visibility = if (name == "settings") View.VISIBLE else View.GONE
        bottomNav.visibility = if (name == "ai" || name == "settings") View.GONE else View.VISIBLE
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
        btnOpenHomework.setOnClickListener { bottomNav.selectedItemId = R.id.nav_homework }
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
        v.findViewById<MaterialCardView>(R.id.cardSettings).setOnClickListener {
            loadSettingsForm()
            showMainPanel("settings")
        }
    }

    private fun bindAi(v: View) {
        aiList = v.findViewById(R.id.aiList)
        aiInput = v.findViewById(R.id.aiInput)
        aiSend = v.findViewById(R.id.aiSend)
        aiBack = v.findViewById(R.id.aiBack)
        aiClear = v.findViewById(R.id.aiClear)
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

    private fun bindSettings(v: View) {
        settingsName = v.findViewById(R.id.settingsName)
        settingsFaith = v.findViewById(R.id.settingsFaith)
        settingsTheme = v.findViewById(R.id.settingsTheme)
        v.findViewById<ImageButton>(R.id.settingsBack).setOnClickListener {
            showMainPanel("more")
            bottomNav.selectedItemId = R.id.nav_more
        }
        val faithOpts = listOf(getString(R.string.faith_muslim), getString(R.string.faith_non_muslim))
        settingsFaith.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, faithOpts)
        val themeOpts = listOf(
            getString(R.string.theme_system),
            getString(R.string.theme_light),
            getString(R.string.theme_dark)
        )
        settingsTheme.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, themeOpts)

        v.findViewById<MaterialButton>(R.id.settingsSave).setOnClickListener {
            val name = settingsName.text?.toString().orEmpty().trim()
            if (name.isBlank()) {
                Toast.makeText(this, R.string.name_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            profile.fullName = name
            profile.isMuslim = settingsFaith.selectedItemPosition == 0
            profile.themeMode = when (settingsTheme.selectedItemPosition) {
                1 -> "light"
                2 -> "dark"
                else -> "system"
            }
            profile.applyTheme()
            applyGreeting()
            lastData?.let { renderLoaded(it, fromCache = true) }
            Toast.makeText(this, R.string.settings_saved, Toast.LENGTH_SHORT).show()
            showMainPanel("more")
            bottomNav.selectedItemId = R.id.nav_more
        }
    }

    private fun loadSettingsForm() {
        settingsName.setText(profile.fullName)
        settingsFaith.setSelection(if (profile.isMuslim != false) 0 else 1)
        settingsTheme.setSelection(
            when (profile.themeMode) {
                "light" -> 1
                "dark" -> 2
                else -> 0
            }
        )
    }

    private fun seedWelcomeChat() {
        val name = profile.firstName()
        val welcome = if (name.isBlank()) {
            getString(R.string.ai_welcome)
        } else {
            "Assalam-o-Alaikum, **$name**! I'm your Class 8 study helper. Ask in English or Urdu — math, science, grammar, or anything from your books."
        }
        chatUi.clear()
        chatUi.add(ChatAdapter.Item(fromUser = false, text = welcome))
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
            val hist = AiRepository.class8Primer(profile.firstName()) + chatHistory.dropLast(1)
            val result = aiRepo.ask(text, hist)
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
        val name = profile.firstName()
        greetingSub.text = if (name.isBlank()) {
            getString(R.string.greeting_sub)
        } else {
            getString(R.string.greeting_named, name)
        }
    }

    private fun applyTip() {
        val tips = (1..20).mapNotNull { i ->
            val id = resources.getIdentifier("tip_$i", "string", packageName)
            if (id != 0) getString(id) else null
        }
        if (tips.isEmpty()) return
        val now = System.currentTimeMillis()
        val lastAt = tipPrefs.getLong("tip_at", 0L)
        var idx = tipPrefs.getInt("tip_idx", 0)
        if (now - lastAt >= TimeUnit.HOURS.toMillis(2)) {
            idx = (idx + 1) % tips.size
            tipPrefs.edit().putInt("tip_idx", idx).putLong("tip_at", now).apply()
        }
        tipText.text = tips[idx % tips.size]
    }

    private fun updateSubjectStatsPlaceholder() {
        val total = profile.visibleSubjects().size.coerceAtLeast(1)
        statTotalCount.text = total.toString()
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

        val visible = profile.visibleSubjects()
        val rows = visible.map { info -> SubjectAdapter.Row(info, data.entries[info.key]) }
        adapter.submit(rows)

        val due = visible.count { data.entries.containsKey(it.key) }
        val total = visible.size
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
        val total = profile.visibleSubjects().size
        statDueCount.text = "0"
        statTotalCount.text = total.toString()
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
