package cn.qingkui.app.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MistakeDraftPolicyTest {
    @Test
    fun manualQuestionDoesNotRequireAnImageForRetry() {
        val error = mistakeDraftValidationError(
            imagePath = "",
            questionText = "求函数的定义域",
            imageExists = { false },
        )

        assertNull(error)
    }

    @Test
    fun missingImageIsRejectedWhenDraftReferencesOne() {
        val error = mistakeDraftValidationError(
            imagePath = "missing.jpg",
            questionText = "",
            imageExists = { false },
        )

        assertEquals("本地图片已不存在", error)
    }

    @Test
    fun emptyManualDraftIsRejected() {
        val error = mistakeDraftValidationError(
            imagePath = "",
            questionText = "   ",
            imageExists = { false },
        )

        assertEquals("手动题目内容为空", error)
    }
}
