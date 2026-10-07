package com.endroid.class8homework

import android.content.Context
import android.widget.TextView
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin

/**
 * Shared Markwon with Markdown + LaTeX ($...$, $$...$$).
 */
object MarkdownKit {

    fun create(context: Context): Markwon {
        val textSizePx = 15f * context.resources.displayMetrics.scaledDensity
        return Markwon.builder(context)
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(
                JLatexMathPlugin.create(textSizePx) { builder ->
                    builder.inlinesEnabled(true)
                    builder.blocksEnabled(true)
                }
            )
            .build()
    }

    /** Normalize common LaTeX delimiters for JLatexMath. */
    fun prepare(md: String): String {
        var s = md.replace("\r\n", "\n")
        // \[ ... \] → $$ ... $$
        s = Regex("\\\\\\[([\\s\\S]+?)\\\\\\]").replace(s) { m ->
            "\n$$" + m.groupValues[1].trim() + "$$\n"
        }
        // \( ... \) → $ ... $
        s = Regex("\\\\\\(([\\s\\S]+?)\\\\\\)").replace(s) { m ->
            "$" + m.groupValues[1].trim() + "$"
        }
        // ```math / latex / tex fences
        s = Regex("```(?:math|latex|tex)\\s*\\n([\\s\\S]+?)```", RegexOption.IGNORE_CASE).replace(s) { m ->
            "\n$$" + m.groupValues[1].trim() + "$$\n"
        }
        return s
    }

    fun set(markwon: Markwon, tv: TextView, markdown: String) {
        markwon.setMarkdown(tv, prepare(markdown))
    }
}
