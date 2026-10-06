package com.endroid.class8homework

/**
 * Lightweight math-friendly Markdown for Class 8 answers.
 * Converts $...$ / $$...$$ LaTeX-ish snippets into readable Unicode
 * so students see a₉, ×, ÷ instead of raw $a_9$.
 */
object MathMarkdown {
    private val SUB = mapOf(
        '0' to '₀', '1' to '₁', '2' to '₂', '3' to '₃', '4' to '₄',
        '5' to '₅', '6' to '₆', '7' to '₇', '8' to '₈', '9' to '₉',
        '+' to '₊', '-' to '₋', '=' to '₌', '(' to '₍', ')' to '₎',
        'a' to 'ₐ', 'e' to 'ₑ', 'h' to 'ₕ', 'i' to 'ᵢ', 'j' to 'ⱼ',
        'k' to 'ₖ', 'l' to 'ₗ', 'm' to 'ₘ', 'n' to 'ₙ', 'o' to 'ₒ',
        'p' to 'ₚ', 'r' to 'ᵣ', 's' to 'ₛ', 't' to 'ₜ', 'u' to 'ᵤ',
        'v' to 'ᵥ', 'x' to 'ₓ'
    )
    private val SUP = mapOf(
        '0' to '⁰', '1' to '¹', '2' to '²', '3' to '³', '4' to '⁴',
        '5' to '⁵', '6' to '⁶', '7' to '⁷', '8' to '⁸', '9' to '⁹',
        '+' to '⁺', '-' to '⁻', '=' to '⁼', '(' to '⁽', ')' to '⁾',
        'n' to 'ⁿ', 'i' to 'ⁱ'
    )

    fun enhance(md: String): String {
        var s = md
        // display math blocks → plain with spacing
        s = Regex("\\$\\$([\\s\\S]+?)\\$\\$").replace(s) { m ->
            "\n" + convertExpr(m.groupValues[1].trim()) + "\n"
        }
        // inline $...$
        s = Regex("(?<!\\\\)\\$([^$\\n]+?)\\$").replace(s) { m ->
            convertExpr(m.groupValues[1].trim())
        }
        // \( ... \) and \[ ... \]
        s = Regex("\\\\\\(([\\s\\S]+?)\\\\\\)").replace(s) { convertExpr(it.groupValues[1].trim()) }
        s = Regex("\\\\\\[([\\s\\S]+?)\\\\\\]").replace(s) { "\n" + convertExpr(it.groupValues[1].trim()) + "\n" }
        return s
    }

    private fun convertExpr(raw: String): String {
        var e = raw
        e = e.replace("\\times", "×")
            .replace("\\div", "÷")
            .replace("\\pm", "±")
            .replace("\\cdot", "·")
            .replace("\\leq", "≤")
            .replace("\\geq", "≥")
            .replace("\\neq", "≠")
            .replace("\\approx", "≈")
            .replace("\\infty", "∞")
            .replace("\\pi", "π")
            .replace("\\theta", "θ")
            .replace("\\alpha", "α")
            .replace("\\beta", "β")
            .replace("\\sqrt", "√")
            .replace("\\frac", "frac")
        // \frac{a}{b} → (a)/(b)
        e = Regex("frac\\{([^{}]+)\\}\\{([^{}]+)\\}").replace(e) { m ->
            "(${m.groupValues[1]})/(${m.groupValues[2]})"
        }
        // a_{9} or a_9
        e = Regex("([A-Za-z])_\\{([^{}]+)\\}").replace(e) { m ->
            m.groupValues[1] + toScript(m.groupValues[2], SUB)
        }
        e = Regex("([A-Za-z])_([0-9]+|[a-z])").replace(e) { m ->
            m.groupValues[1] + toScript(m.groupValues[2], SUB)
        }
        // a^{2} or a^2
        e = Regex("([A-Za-z0-9])\\^\\{([^{}]+)\\}").replace(e) { m ->
            m.groupValues[1] + toScript(m.groupValues[2], SUP)
        }
        e = Regex("([A-Za-z0-9])\\^([0-9]+)").replace(e) { m ->
            m.groupValues[1] + toScript(m.groupValues[2], SUP)
        }
        // strip remaining backslashes on simple commands
        e = e.replace(Regex("\\\\([a-zA-Z]+)"), "$1")
        e = e.replace("{", "").replace("}", "")
        return e
    }

    private fun toScript(s: String, map: Map<Char, Char>): String =
        buildString {
            for (c in s) append(map[c] ?: c)
        }
}
