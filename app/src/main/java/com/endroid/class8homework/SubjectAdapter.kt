package com.endroid.class8homework

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import io.noties.markwon.Markwon

class SubjectAdapter(
    private val markwon: Markwon
) : RecyclerView.Adapter<SubjectAdapter.VH>() {

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
        return VH(v, markwon)
    }

    override fun getItemCount() = items.size
    override fun onBindViewHolder(holder: VH, position: Int) = holder.bind(items[position])

    class VH(itemView: View, private val markwon: Markwon) : RecyclerView.ViewHolder(itemView) {
        private val icon: ImageView = itemView.findViewById(R.id.icon)
        private val title: TextView = itemView.findViewById(R.id.title)
        private val source: TextView = itemView.findViewById(R.id.source)
        private val body: TextView = itemView.findViewById(R.id.body)
        private val meta: TextView = itemView.findViewById(R.id.meta)
        private val btnCopy: ImageButton = itemView.findViewById(R.id.btnCopy)

        fun bind(row: Row) {
            icon.setImageResource(row.info.iconRes)
            title.text = row.info.label
            val e = row.entry
            if (e == null) {
                source.visibility = View.GONE
                body.alpha = 0.55f
                body.text = itemView.context.getString(R.string.no_subject)
                meta.visibility = View.GONE
                btnCopy.visibility = View.GONE
            } else {
                source.visibility = View.VISIBLE
                source.text = e.source.replaceFirstChar { it.uppercase() }
                body.alpha = 1f
                body.setSingleLine(false)
                body.maxLines = Integer.MAX_VALUE
                MarkdownKit.set(markwon, body, e.description.ifBlank { "—" })

                val metaParts = mutableListOf<String>()
                if (e.page.isNotBlank()) metaParts += "Page ${e.page}"
                if (e.notes.isNotBlank()) metaParts += e.notes
                if (metaParts.isEmpty()) {
                    meta.visibility = View.GONE
                } else {
                    meta.visibility = View.VISIBLE
                    MarkdownKit.set(markwon, meta, metaParts.joinToString(" · "))
                }

                btnCopy.visibility = View.VISIBLE
                btnCopy.setOnClickListener {
                    val text = buildString {
                        append(row.info.label)
                        append('\n')
                        if (e.description.isNotBlank()) append(e.description.trim()).append('\n')
                        if (e.page.isNotBlank()) append("Page: ").append(e.page).append('\n')
                        if (e.notes.isNotBlank()) append(e.notes.trim())
                    }.trim()
                    val cm = itemView.context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(ClipData.newPlainText("homework", text))
                    Toast.makeText(itemView.context, R.string.copied, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
