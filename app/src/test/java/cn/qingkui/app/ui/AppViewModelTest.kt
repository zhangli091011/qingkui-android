package cn.qingkui.app.ui

import cn.qingkui.app.data.repository.AppRepository
import cn.qingkui.app.data.repository.GraphData
import cn.qingkui.app.data.repository.MistakeAnalysisOutcome
import cn.qingkui.app.data.repository.QaAnswer
import cn.qingkui.app.ui.model.AppDestination
import cn.qingkui.app.ui.model.ChatMessage
import cn.qingkui.app.ui.model.CreditLedgerItem
import cn.qingkui.app.ui.model.DeviceSessionItem
import cn.qingkui.app.ui.model.FeedbackItem
import cn.qingkui.app.ui.model.LearningItem
import cn.qingkui.app.ui.model.MessageAuthor
import cn.qingkui.app.ui.model.MistakeDraftItem
import cn.qingkui.app.ui.model.MistakeItem
import cn.qingkui.app.ui.model.QaHelpLevel
import cn.qingkui.app.ui.model.QaMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
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

    @Test
    fun guestCannotOpenMistakeCameraWithoutLogin() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = AppViewModel(FakeRepository(authenticated = false))
            advanceUntilIdle()
            viewModel.openMistakeCapture()
            assertTrue(viewModel.uiState.value.authScreenOpen)
            assertEquals(false, viewModel.uiState.value.mistakeCaptureOpen)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun accountDeviceSessionCanBeRevoked() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository()
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()

            assertEquals(1, viewModel.uiState.value.deviceSessions.size)
            viewModel.revokeDeviceSession("device-session-1")
            advanceUntilIdle()

            assertEquals(listOf("device-session-1"), repository.revokedSessions)
            assertTrue(viewModel.uiState.value.deviceSessions.isEmpty())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun accountFeedbackIsSubmittedAndRefreshed() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository()
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()

            viewModel.submitFeedback("product_issue", "横屏按钮被遮挡")
            advanceUntilIdle()

            assertEquals(listOf("product_issue" to "横屏按钮被遮挡"), repository.feedbackSubmissions)
            assertEquals("pending", viewModel.uiState.value.feedbackItems.first().status)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun mistakeAnalysisUsesBackendBalanceAndRefreshesList() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository(answerBalance = 41)
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()

            viewModel.analyzeMistake("mistake-1")
            advanceUntilIdle()

            assertEquals(listOf("mistake-1"), repository.analyzedMistakes)
            assertEquals(41, viewModel.uiState.value.credits)
            assertEquals(false, viewModel.uiState.value.mistakeLoading)
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
    val revokedSessions = mutableListOf<String>()
    val feedbackSubmissions = mutableListOf<Pair<String, String>>()
    val analyzedMistakes = mutableListOf<String>()

    override suspend fun hasSession() = authenticated
    override suspend fun nickname(): String? = null
    override suspend fun login(username: String, password: String) = "测试同学"
    override suspend fun register(username: String, password: String, nickname: String) = nickname
    override suspend fun logout() = Unit
    override suspend fun credits() = answerBalance
    override suspend fun creditLedger(): List<CreditLedgerItem> = emptyList()
    override suspend fun deviceSessions(): List<DeviceSessionItem> = listOf(
        DeviceSessionItem("device-session-1", "测试平板", "2026-09-30 12:00", true),
    )
    override suspend fun revokeDeviceSession(sessionId: String) {
        revokedSessions += sessionId
    }
    override suspend fun feedback(): List<FeedbackItem> = listOf(
        FeedbackItem("feedback-1", "product_issue", "横屏按钮被遮挡", "pending", null, "2026-08-30 12:00"),
    )
    override suspend fun submitFeedback(category: String, content: String) {
        feedbackSubmissions += category to content
    }
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
    override fun observeMistakeDrafts(): Flow<List<MistakeDraftItem>> = flowOf(emptyList())
    override suspend fun mistakes(): List<MistakeItem> = emptyList()
    override suspend fun saveMistakeDraft(
        imagePath: String,
        subject: String,
        questionText: String,
        studentWork: String,
        questionGoal: String,
    ) = Unit
    override suspend fun confirmMistakeOcr(mistakeId: String, taskId: String, correctedText: String) = Unit
    override suspend fun analyzeMistake(mistakeId: String): MistakeAnalysisOutcome {
        analyzedMistakes += mistakeId
        return MistakeAnalysisOutcome(answerBalance)
    }
    override suspend fun generateMistakePractice(mistakeId: String) = Unit
    override suspend fun submitMistakePractice(mistakeId: String, practiceId: String, answer: String) = Unit
}
