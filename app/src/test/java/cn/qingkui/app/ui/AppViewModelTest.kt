package cn.qingkui.app.ui

import cn.qingkui.app.data.repository.AppRepository
import cn.qingkui.app.data.repository.GraphData
import cn.qingkui.app.data.repository.QaAnswer
import cn.qingkui.app.ui.model.AppDestination
import cn.qingkui.app.ui.model.ChatMessage
import cn.qingkui.app.ui.model.CreditLedgerItem
import cn.qingkui.app.ui.model.LearningItem
import cn.qingkui.app.ui.model.MessageAuthor
import cn.qingkui.app.ui.model.QaHelpLevel
import cn.qingkui.app.ui.model.QaMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelTest {
    @Test
    fun destinationAndDraftUpdateImmediately() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = AppViewModel(FakeRepository())
            advanceUntilIdle()
            viewModel.updateDraft("判别式是什么？")
            viewModel.selectDestination(AppDestination.Graph)

            assertEquals("判别式是什么？", viewModel.uiState.value.draft)
            assertEquals(AppDestination.Graph, viewModel.uiState.value.destination)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun sendingQuestionUsesAtomicBalanceReturnedByBackend() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = AppViewModel(FakeRepository(answerBalance = 37))
            advanceUntilIdle()
            viewModel.updateDraft("二次函数是什么？")

            viewModel.sendMessage()
            advanceUntilIdle()

            val state = viewModel.uiState.value
            assertEquals("", state.draft)
            assertEquals(37, state.credits)
            assertEquals(2, state.messages.size)
            assertEquals(MessageAuthor.Student, state.messages.first().author)
            assertEquals(MessageAuthor.Assistant, state.messages.last().author)
            assertTrue(state.messages.last().source?.contains("二次函数") == true)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun guestSendOpensAuthAndPreservesQuestion() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository(authenticated = false)
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()
            viewModel.updateDraft("什么是二次函数？")

            viewModel.sendMessage()

            assertTrue(viewModel.uiState.value.authScreenOpen)
            assertEquals("什么是二次函数？", viewModel.uiState.value.draft)
            assertEquals(0, repository.questionsSent)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun blankQuestionDoesNotCallBackend() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository()
            val viewModel = AppViewModel(repository)
            viewModel.updateDraft("   ")
            viewModel.sendMessage()
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.messages.isEmpty())
            assertEquals(0, repository.questionsSent)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private class FakeRepository(
    private val answerBalance: Int = 1280,
    private val authenticated: Boolean = true,
) : AppRepository {
    var questionsSent = 0

    override suspend fun hasSession() = authenticated
    override suspend fun nickname(): String? = null
    override suspend fun login(username: String, password: String) = "测试同学"
    override suspend fun register(username: String, password: String, nickname: String) = nickname
    override suspend fun logout() = Unit
    override suspend fun credits() = answerBalance
    override suspend fun creditLedger(): List<CreditLedgerItem> = emptyList()
    override suspend fun graph(centerId: String) = GraphData(emptyList(), emptyList(), null)
    override suspend fun nodeDetail(nodeId: String) = throw UnsupportedOperationException()
    override suspend fun search(query: String) = GraphData(emptyList(), emptyList(), null)
    override suspend fun sessions() = emptyList<cn.qingkui.app.ui.model.ConversationSummary>()
    override suspend fun restoreSession(sessionId: String) = emptyList<ChatMessage>()
    override suspend fun deleteSession(sessionId: String) = Unit
    override suspend fun learningItems(): List<LearningItem> = emptyList()
    override suspend fun updateNodeState(nodeId: String, status: cn.qingkui.app.ui.model.KnowledgeStatus, note: String?, favorite: Boolean?) = null
    override suspend fun recordLearningEvent(nodeId: String, eventType: String, data: Map<String, Any?>) = Unit
    override suspend fun changePassword(current: String, next: String) = Unit
    override suspend fun deleteAccount() = Unit

    override suspend fun sendQuestion(
        sessionId: String?,
        nodeId: String?,
        question: String,
        mode: QaMode,
        helpLevel: QaHelpLevel,
        onDelta: (String) -> Unit,
    ): QaAnswer {
        questionsSent++
        onDelta("二次函数")
        onDelta("回答")
        return QaAnswer(
            conversationId = "session-1",
            message = ChatMessage(2L, MessageAuthor.Assistant, "二次函数回答", "二次函数 · 演示来源"),
            balance = answerBalance,
            creditsCharged = helpLevel.creditCost,
        )
    }

    override suspend fun submitAnswerFeedback(messageId: String?, helpful: Boolean) = Unit
}
