package cn.qingkui.app.ui.components

internal sealed interface MathTextSegment {
    data class Text(val value: String) : MathTextSegment
    data class Formula(val latex: String, val display: Boolean) : MathTextSegment
}

internal fun parseMathText(value: String): List<MathTextSegment> {
    if (value.isEmpty()) return listOf(MathTextSegment.Text(""))
    val result = mutableListOf<MathTextSegment>()
    var textStart = 0
    var cursor = 0
    while (cursor < value.length) {
        val delimiter = when {
            value.startsWith("\$\$", cursor) -> FormulaDelimiter("\$\$", "\$\$", true)
            value.startsWith("\\[", cursor) -> FormulaDelimiter("\\[", "\\]", true)
            value.startsWith("\\(", cursor) -> FormulaDelimiter("\\(", "\\)", false)
            value[cursor] == '$' && !value.isEscaped(cursor) -> FormulaDelimiter("\$", "\$", false)
            else -> null
        }
        if (delimiter == null) {
            cursor++
            continue
        }
        val formulaStart = cursor + delimiter.open.length
        val closeAt = value.findClosingDelimiter(delimiter.close, formulaStart)
        if (closeAt < 0) {
            cursor += delimiter.open.length
            continue
        }
        val latex = value.substring(formulaStart, closeAt).trim()
        if (latex.isEmpty()) {
            cursor = closeAt + delimiter.close.length
            continue
        }
        if (cursor > textStart) result += MathTextSegment.Text(value.substring(textStart, cursor))
        result += MathTextSegment.Formula(latex, delimiter.display)
        cursor = closeAt + delimiter.close.length
        textStart = cursor
    }
    if (textStart < value.length) result += MathTextSegment.Text(value.substring(textStart))
    return result.ifEmpty { listOf(MathTextSegment.Text(value)) }
}

private data class FormulaDelimiter(val open: String, val close: String, val display: Boolean)

private fun String.isEscaped(index: Int): Boolean {
    var backslashes = 0
    var cursor = index - 1
    while (cursor >= 0 && this[cursor] == '\\') {
        backslashes++
        cursor--
    }
    return backslashes % 2 == 1
}

private fun String.findClosingDelimiter(delimiter: String, start: Int): Int {
    var cursor = start
    while (cursor <= length - delimiter.length) {
        if (startsWith(delimiter, cursor) && (delimiter != "\$" || !isEscaped(cursor))) return cursor
        cursor++
    }
    return -1
}
