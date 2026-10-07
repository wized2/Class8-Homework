package com.endroid.class8homework

import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon

class ChatAdapter(
    private val markwon: Markwon
) : RecyclerView.Adapter<ChatAdapter.VH>() {

    data class Item(val fromUser: Boolean, val text: String)

    private val items = mutableListOf<Item>()

    fun submit(list: List<Item>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_chat, parent, false)
        return VH(v, markwon)
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    class VH(itemView: View, private val markwon: Markwon) : RecyclerView.ViewHolder(itemView) {
        private val bubble: LinearLayout = itemView.findViewById(R.id.bubble)
        private val chatText: TextView = itemView.findViewById(R.id.chatText)
        private val structured: LinearLayout = itemView.findViewById(R.id.structured)
        private val serif: Typeface? = try {
            ResourcesCompat.getFont(itemView.context, R.font.anthropic_serif)
        } catch (_: Exception) {
            Typeface.SERIF
        }

        fun bind(item: Item) {
            val ctx = itemView.context
            val lp = bubble.layoutParams as FrameLayout.LayoutParams
            structured.removeAllViews()
            structured.visibility = View.GONE

            chatText.setSingleLine(false)
            chatText.maxLines = Integer.MAX_VALUE
            chatText.ellipsize = null

            if (item.fromUser) {
                lp.gravity = Gravity.END
                lp.width = ViewGroup.LayoutParams.WRAP_CONTENT
                bubble.setBackgroundResource(R.drawable.bg_chip)
                bubble.setPadding(dp(14), dp(10), dp(14), dp(10))
                chatText.visibility = View.VISIBLE
                chatText.setTextColor(ContextCompat.getColor(ctx, R.color.seed_dark))
                chatText.typeface = Typeface.DEFAULT
                chatText.text = item.text
            } else {
                lp.gravity = Gravity.START
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                bubble.background = null
                bubble.setPadding(dp(4), dp(8), dp(4), dp(8))
                chatText.setTextColor(ContextCompat.getColor(ctx, R.color.ink))
                chatText.typeface = serif ?: Typeface.SERIF

                val enhanced = MarkdownKit.prepare(item.text)
                val parsed = StructuredAi.parse(enhanced)

                // Always show full prose (never drop body)
                chatText.visibility = View.VISIBLE
                val body = when {
                    parsed.mcqs.isNotEmpty() || parsed.qas.isNotEmpty() ->
                        parsed.leadMarkdown.ifBlank { enhanced }
                    else -> enhanced
                }
                markwon.setMarkdown(chatText, body)

                if (parsed.mcqs.isNotEmpty()) {
                    structured.visibility = View.VISIBLE
                    parsed.mcqs.forEachIndexed { idx, mcq ->
                        structured.addView(buildMcqCard(mcq, idx + 1))
                    }
                } else if (parsed.qas.isNotEmpty()) {
                    structured.visibility = View.VISIBLE
                    parsed.qas.forEach { structured.addView(buildQaCard(it)) }
                }
            }
            bubble.layoutParams = lp
        }

        private fun buildMcqCard(mcq: StructuredAi.Mcq, num: Int): View {
            val ctx = itemView.context
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setBackgroundResource(R.drawable.bg_stat_card)
                val mlp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                mlp.topMargin = dp(10)
                layoutParams = mlp
            }
            card.addView(TextView(ctx).apply {
                text = "Q$num. ${mcq.question}"
                setTextColor(ContextCompat.getColor(ctx, R.color.ink))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
                typeface = Typeface.DEFAULT_BOLD
            })
            val inflater = LayoutInflater.from(ctx)
            for ((key, text) in mcq.options) {
                val row = inflater.inflate(R.layout.item_mcq_option, card, false)
                row.findViewById<TextView>(R.id.optKey).text = key
                row.findViewById<TextView>(R.id.optText).text = text
                val isAns = mcq.answer?.uppercase()?.let {
                    it.startsWith(key) || it.contains(key)
                } == true
                if (isAns) row.setBackgroundResource(R.drawable.bg_chip)
                card.addView(row)
            }
            if (!mcq.answer.isNullOrBlank()) {
                card.addView(TextView(ctx).apply {
                    text = "${ctx.getString(R.string.answer_label)}: ${mcq.answer}"
                    setTextColor(ContextCompat.getColor(ctx, R.color.ok))
                    setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
                    setPadding(0, dp(8), 0, 0)
                    typeface = Typeface.DEFAULT_BOLD
                })
            }
            return card
        }

        private fun buildQaCard(qa: StructuredAi.Qa): View {
            val ctx = itemView.context
            val card = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(12), dp(12), dp(12), dp(12))
                setBackgroundResource(R.drawable.bg_stat_card)
                val mlp = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                mlp.topMargin = dp(10)
                layoutParams = mlp
            }
            card.addView(TextView(ctx).apply {
                text = qa.question
                setTextColor(ContextCompat.getColor(ctx, R.color.ink))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                typeface = Typeface.DEFAULT_BOLD
            })
            card.addView(TextView(ctx).apply {
                text = qa.answer
                setTextColor(ContextCompat.getColor(ctx, R.color.muted))
                setTextSize(TypedValue.COMPLEX_UNIT_SP, 14f)
                setPadding(0, dp(6), 0, 0)
            })
            return card
        }

        private fun dp(v: Int): Int =
            (v * itemView.resources.displayMetrics.density).toInt()
    }
}
