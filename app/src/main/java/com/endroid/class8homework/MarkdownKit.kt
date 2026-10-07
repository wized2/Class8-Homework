package com.endroid.class8homework

import android.content.Context
import android.widget.TextView
import io.noties.markwon.Markwon
import io.noties.markwon.SoftBreakAddsNewLinePlugin
import io.noties.markwon.ext.latex.JLatexMathPlugin
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin
import io.noties.markwon.ext.tasklist.TaskListPlugin
import io.noties.markwon.InlineParserPlugin
import ru.noties.jlatexmath.JLatexMathDrawable

/**
 * Shared Markwon with full Markdown + LaTeX ($...$, $$...$$).
 */
object MarkdownKit {

    fun create(context: Context, textSizePx: Float = 15f * context.resources.displayMetrics.scaledDensity): Markwon {
        return Markwon.builder(context)
            .usePlugin(SoftBreakAddsNewLinePlugin.create())
            .usePlugin(StrikethroughPlugin.create())
            .usePlugin(TablePlugin.create(context))
            .usePlugin(TaskListPlugin.create(context))
            .usePlugin(
                JLatexMathPlugin.create(textSizePx) { builder ->
                    builder.inlinesEnabled(true)
                    builder.blocksEnabled(true)
                    builder.theme().apply {
                        // readable on light & dark via TextView text color inheritance where possible
                    }
                }
            )
            .build()
    }

    /** Normalize common LaTeX delimiters to forms Markwon/JLatexMath expects. */
    fun prepare(md: String): String {
        var s = md.replace("\r\n", "\n")
        // \[ ... \] display
        s = Regex("\\\\\\[([\\s\\S]+?)\\\\\\]").replace(s) { m ->
            "\n$$" + m.groupValues[1].trim() + "$$\n"
        }
        // \( ... \) inline
        s = Regex("\\\\\\(([\\s\\S]+?)\\\\\\)").replace(s) { m ->
            "$" + m.groupValues[1].trim() + "$"
        }
        // ```math ... ``` or ```latex ... ```
        s = Regex("```(?:math|latex|tex)\\s*\\n([\\s\\S]+?)```", RegexOption.IGNORE_CASE).replace(s) { m ->
            "\n$$" + m.groupValues[1].trim() + "$$\n"
        }
        return s
    }

    fun set(markwon: Markwon, tv: TextView, markdown: String) {
        markwon.setMarkdown(tv, prepare(markdown))
    }
}
