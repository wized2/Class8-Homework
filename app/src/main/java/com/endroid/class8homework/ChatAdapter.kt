package com.endroid.class8homework

import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
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

    fun add(item: Item) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun removeLast() {
        if (items.isEmpty()) return
        val i = items.size - 1
        items.removeAt(i)
        notifyItemRemoved(i)
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

        fun bind(item: Item) {
            val lp = bubble.layoutParams as FrameLayout.LayoutParams
            if (item.fromUser) {
                lp.gravity = Gravity.END
                bubble.setBackgroundResource(R.drawable.bg_chip)
                chatText.setTextColor(Color.parseColor("#3730A3"))
                chatText.text = item.text
            } else {
                lp.gravity = Gravity.START
                bubble.setBackgroundResource(R.drawable.bg_stat_card)
                chatText.setTextColor(Color.parseColor("#0F172A"))
                markwon.setMarkdown(chatText, item.text)
            }
            bubble.layoutParams = lp
        }
    }
}
