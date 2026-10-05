package com.endroid.class8homework

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SubjectAdapter : RecyclerView.Adapter<SubjectAdapter.VH>() {
    data class Row(
        val info: Subjects.Info,
        val entry: HomeworkEntry?
    )

    private val items = mutableListOf<Row>()

    fun submit(rows: List<Row>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_subject, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    class VH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val icon: TextView = itemView.findViewById(R.id.icon)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val source: TextView = itemView.findViewById(R.id.source)
        private val body: TextView = itemView.findViewById(R.id.body)
        private val meta: TextView = itemView.findViewById(R.id.meta)

        fun bind(row: Row) {
            icon.text = row.info.emoji
            title.text = row.info.label
            val e = row.entry
            if (e == null) {
                source.visibility = View.GONE
                body.text = itemView.context.getString(R.string.no_subject)
                body.alpha = 0.55f
                meta.visibility = View.GONE
            } else {
                source.visibility = View.VISIBLE
                source.text = e.source.replaceFirstChar { it.uppercase() }
                body.text = e.description
                body.alpha = 1f
                val parts = mutableListOf<String>()
                if (e.page.isNotBlank()) parts += "Page ${e.page}"
                if (e.notes.isNotBlank()) parts += e.notes
                if (parts.isEmpty()) {
                    meta.visibility = View.GONE
                } else {
                    meta.visibility = View.VISIBLE
                    meta.text = parts.joinToString(" · ")
                }
            }
        }
    }
}
