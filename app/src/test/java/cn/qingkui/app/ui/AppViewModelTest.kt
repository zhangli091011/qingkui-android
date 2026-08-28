package cn.qingkui.app.ui

import androidx.lifecycle.SavedStateHandle
import cn.qingkui.app.ui.model.AppDestination
import cn.qingkui.app.ui.model.MessageAuthor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppViewModelTest {
    @Test
    fun draftAndDestinationAreRestoredFromSavedState() {
        val savedState = SavedStateHandle()
        val first = AppViewModel(savedState)

        first.updateDraft("判别式是什么？")
        first.selectDestination(AppDestination.Graph)

        val restored = AppViewModel(savedState).uiState.value
        assertEquals("判别式是什么？", restored.draft)
        assertEquals(AppDestination.Graph, restored.destination)
    }

    @Test
    fun sendingQuestionCreatesConversationAndChargesOneCredit() {
        val viewModel = AppViewModel(SavedStateHandle())
        viewModel.updateDraft("二次函数是什么？")

        viewModel.sendMessage()

        val state = viewModel.uiState.value
        assertEquals("", state.draft)
        assertEquals(1279, state.credits)
        assertEquals(2, state.messages.size)
        assertEquals(MessageAuthor.Student, state.messages.first().author)
        assertEquals(MessageAuthor.Assistant, state.messages.last().author)
        assertTrue(state.messages.last().source?.contains("二次函数") == true)
    }

    @Test
    fun blankQuestionDoesNotCreateMessageOrChargeCredit() {
        val viewModel = AppViewModel(SavedStateHandle())
        viewModel.updateDraft("   ")

        viewModel.sendMessage()

        val state = viewModel.uiState.value
        assertTrue(state.messages.isEmpty())
        assertEquals(1280, state.credits)
    }
}
