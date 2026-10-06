package com.endroid.class8homework

import android.graphics.Color
import android.graphics.Typeface
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
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
        private val serif: Typeface? = try {
            ResourcesCompat.getFont(itemView.context, R.font.anthropic_serif)
        } catch (_: Exception) {
            Typeface.SERIF
        }

        fun bind(item: Item) {
            val lp = bubble.layoutParams as FrameLayout.LayoutParams
            if (item.fromUser) {
                lp.gravity = Gravity.END
                bubble.setBackgroundResource(R.drawable.bg_chip)
                bubble.setPadding(dp(14), dp(10), dp(14), dp(10))
                chatText.setTextColor(Color.parseColor("#3730A3"))
                chatText.typeface = Typeface.DEFAULT
                chatText.text = item.text
            } else {
                // Full-width, no chat bubble — clean reading surface
                lp.gravity = Gravity.START
                lp.width = ViewGroup.LayoutParams.MATCH_PARENT
                bubble.setBackgroundColor(Color.TRANSPARENT)
                bubble.setPadding(dp(4), dp(8), dp(4), dp(8))
                chatText.setTextColor(Color.parseColor("#0F172A"))
                chatText.typeface = serif ?: Typeface.SERIF
                val enhanced = MathMarkdown.enhance(item.text)
                markwon.setMarkdown(chatText, enhanced)
            }
            bubble.layoutParams = lp
        }

        private fun dp(v: Int): Int =
            (v * itemView.resources.displayMetrics.density).toInt()
    }
}
