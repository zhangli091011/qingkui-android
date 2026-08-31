package cn.qingkui.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class MathTextParserTest {
    @Test
    fun parsesInlineAndDisplayFormulaDelimiters() {
        val segments = parseMathText("导数 \\(f'(x)=1/x\\)，且 \$\$a>0\$\$。")

        assertEquals(
            listOf(
                MathTextSegment.Text("导数 "),
                MathTextSegment.Formula("f'(x)=1/x", false),
                MathTextSegment.Text("，且 "),
                MathTextSegment.Formula("a>0", true),
                MathTextSegment.Text("。"),
            ),
            segments,
        )
    }

    @Test
    fun keepsUnclosedOrEscapedDollarAsText() {
        val value = "价格 \\\$5，公式 \$x+1"
        assertEquals(listOf(MathTextSegment.Text(value)), parseMathText(value))
    }

    @Test
    fun parsesBracketDisplayFormula() {
        assertEquals(
            listOf(MathTextSegment.Formula("\\frac{a}{b}", true)),
            parseMathText("\\[\\frac{a}{b}\\]"),
        )
    }
}
