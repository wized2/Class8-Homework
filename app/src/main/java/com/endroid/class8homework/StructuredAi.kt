package com.endroid.class8homework

/**
 * Detect MCQ / Q&A blocks in AI Markdown so the chat UI can render them as cards.
 */
object StructuredAi {

    data class Mcq(
        val question: String,
        val options: List<Pair<String, String>>,
        val answer: String?
    )

    data class Qa(
        val question: String,
        val answer: String
    )

    data class Parsed(
        val leadMarkdown: String,
        val mcqs: List<Mcq>,
        val qas: List<Qa>
    )

    private val optLine = Regex("""^\s*([A-Da-d])[\.\)\-:\]]\s+(.+)\s*$""")
    private val qLine = Regex(
        """^\s*(?:\*\*)?(?:Q(?:uestion)?\s*)?(\d+)[\.\:\)]\s*(.+?)(?:\*\*)?\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val answerLine = Regex(
        """^\s*(?:\*\*)?(?:Answer|Ans|Correct)\s*[:\-]\s*([A-Da-d0-9].*?)\s*(?:\*\*)?\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val qaPair = Regex(
        """(?im)^\s*(?:\*\*)?(?:Q|Question)\s*[:\-]?\s*(.+?)\s*(?:\*\*)?\s*$\s*(?:\*\*)?(?:A|Answer)\s*[:\-]?\s*(.+?)\s*(?:\*\*)?\s*$"""
    )

    fun parse(raw: String): Parsed {
        val lines = raw.replace("\r\n", "\n").split('\n')
        val mcqs = mutableListOf<Mcq>()
        val lead = StringBuilder()
        var i = 0
        var inMcqRegion = false

        while (i < lines.size) {
            val line = lines[i]
            val qm = qLine.matchEntire(line)
            if (qm != null) {
                inMcqRegion = true
                val qText = qm.groupValues[2].trim()
                val opts = mutableListOf<Pair<String, String>>()
                var ans: String? = null
                i++
                while (i < lines.size) {
                    val l = lines[i]
                    val om = optLine.matchEntire(l)
                    val am = answerLine.matchEntire(l)
                    when {
                        om != null -> opts += om.groupValues[1].uppercase() to om.groupValues[2].trim()
                        am != null -> ans = am.groupValues[1].trim()
                        l.isBlank() && opts.isNotEmpty() -> break
                        qLine.matchEntire(l) != null -> break
                        opts.isEmpty() && l.isNotBlank() && !l.startsWith("**") -> {
                            // continuation of question
                        }
                        else -> if (opts.isNotEmpty()) break else { /* keep */ }
                    }
                    if (om != null || am != null) i++ else {
                        if (opts.isNotEmpty()) break
                        i++
                    }
                }
                if (opts.size >= 2) {
                    mcqs += Mcq(qText, opts, ans)
                } else {
                    lead.append(line).append('\n')
                }
                continue
            }
            if (!inMcqRegion) lead.append(line).append('\n')
            else if (line.isNotBlank() && answerLine.matchEntire(line) == null && optLine.matchEntire(line) == null) {
                // trailing non-mcq after region — treat as lead only if no mcqs yet
                if (mcqs.isEmpty()) lead.append(line).append('\n')
            }
            i++
        }

        val qas = mutableListOf<Qa>()
        if (mcqs.isEmpty()) {
            qaPair.findAll(raw).forEach { m ->
                qas += Qa(m.groupValues[1].trim(), m.groupValues[2].trim())
            }
        }

        return Parsed(lead.toString().trim(), mcqs, qas)
    }
}
