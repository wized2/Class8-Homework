package com.endroid.class8homework

/**
 * Detect MCQ / Q&A blocks only when clearly present (options A/B/…).
 * Never strip normal numbered solution steps like "1. Start with…".
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
        """^\s*(?:\*\*)?(?:Q(?:uestion)?\s+)(\d+)[\.\:\)]\s*(.+?)(?:\*\*)?\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val qLineLoose = Regex(
        """^\s*(?:\*\*)?(\d+)[\.\:\)]\s*(.+?)(?:\*\*)?\s*$"""
    )
    private val answerLine = Regex(
        """^\s*(?:\*\*)?(?:Answer|Ans|Correct)\s*[:\-]\s*(.+?)\s*(?:\*\*)?\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val qaPair = Regex(
        """(?im)^\s*(?:\*\*)?(?:Q|Question)\s*[:\-]\s*(.+?)\s*(?:\*\*)?\s*$\n\s*(?:\*\*)?(?:A|Answer)\s*[:\-]\s*(.+?)\s*(?:\*\*)?\s*$"""
    )

    fun parse(raw: String): Parsed {
        val lines = raw.replace("\r\n", "\n").split('\n')
        val mcqs = mutableListOf<Mcq>()
        val lead = StringBuilder()
        var i = 0

        while (i < lines.size) {
            val line = lines[i]
            val qm = qLine.matchEntire(line) ?: qLineLoose.matchEntire(line)
            if (qm != null) {
                // Look ahead: only MCQ if we soon see at least two A/B/C options
                val look = mutableListOf<Pair<String, String>>()
                var ans: String? = null
                var j = i + 1
                var consumed = i + 1
                while (j < lines.size && j < i + 12) {
                    val l = lines[j]
                    val om = optLine.matchEntire(l)
                    val am = answerLine.matchEntire(l)
                    when {
                        om != null -> {
                            look += om.groupValues[1].uppercase() to om.groupValues[2].trim()
                            consumed = j + 1
                        }
                        am != null && look.isNotEmpty() -> {
                            ans = am.groupValues[1].trim()
                            consumed = j + 1
                        }
                        l.isBlank() -> { /* skip */ }
                        look.isNotEmpty() -> break
                        else -> break
                    }
                    j++
                }
                if (look.size >= 2) {
                    mcqs += Mcq(qm.groupValues[2].trim(), look, ans)
                    i = consumed
                    continue
                }
                // Not an MCQ — keep numbered step as normal markdown
                lead.append(line).append('\n')
                i++
                continue
            }
            lead.append(line).append('\n')
            i++
        }

        val text = lead.toString()
        val qas = mutableListOf<Qa>()
        if (mcqs.isEmpty()) {
            qaPair.findAll(raw).forEach { m ->
                qas += Qa(m.groupValues[1].trim(), m.groupValues[2].trim())
            }
        }

        // If we extracted MCQs, lead is the prose around them; else full text
        val leadOut = if (mcqs.isEmpty() && qas.isEmpty()) {
            raw.trim()
        } else {
            text.trim()
        }

        return Parsed(leadOut, mcqs, qas)
    }
}
