package cn.qingkui.app.ui

import androidx.lifecycle.SavedStateHandle
import cn.qingkui.app.data.repository.AppRepository
import cn.qingkui.app.data.repository.GraphData
import cn.qingkui.app.data.repository.MistakeAnalysisOutcome
import cn.qingkui.app.data.repository.QaAnswer
import cn.qingkui.app.data.repository.ServiceAvailability
import cn.qingkui.app.data.repository.UnderstandingCheckOutcome
import cn.qingkui.app.ui.model.AppDestination
import cn.qingkui.app.ui.model.AnswerFeedbackAction
import cn.qingkui.app.ui.model.ChatMessage
import cn.qingkui.app.ui.model.CreditLedgerItem
import cn.qingkui.app.ui.model.DeviceSessionItem
import cn.qingkui.app.ui.model.FeedbackItem
import cn.qingkui.app.ui.model.ClassOverviewItem
import cn.qingkui.app.ui.model.ContributionItem
import cn.qingkui.app.ui.model.CreditCampaignItem
import cn.qingkui.app.ui.model.CreditRedemptionItem
import cn.qingkui.app.ui.model.LearningItem
import cn.qingkui.app.ui.model.LearningFilter
import cn.qingkui.app.ui.model.KnowledgeStatus
import cn.qingkui.app.ui.model.KnowledgeCatalogScope
import cn.qingkui.app.ui.model.KnowledgeTreeChapter
import cn.qingkui.app.ui.model.KnowledgeTreeNode
import cn.qingkui.app.ui.model.KnowledgeTreeSection
import cn.qingkui.app.ui.model.SchoolClassItem
import cn.qingkui.app.ui.model.SchoolMembershipItem
import cn.qingkui.app.ui.model.MessageAuthor
import cn.qingkui.app.ui.model.MistakeDraftItem
import cn.qingkui.app.ui.model.MistakeItem
import cn.qingkui.app.ui.model.QaHelpLevel
import cn.qingkui.app.ui.model.QaMode
import cn.qingkui.app.ui.model.QaClarification
import cn.qingkui.app.ui.model.QaClarificationOption
import cn.qingkui.app.ui.model.UnderstandingCheck
import cn.qingkui.app.ui.model.UnderstandingCheckChoice
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
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
    fun restoresAndPersistsNonSensitiveUiStateAcrossProcessRecreation() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val savedState = SavedStateHandle(
                mapOf(
                    "ui.destination" to AppDestination.Graph.name,
                    "ui.chat_draft" to "尚未发送的问题",
                    "ui.selected_node" to "history-node",
                    "ui.help_level" to QaHelpLevel.Full.name,
                    "ui.qa_mode" to QaMode.Verify.name,
                    "ui.subject" to "历史",
                    "ui.scope_grade" to "高一",
                    "ui.scope_version" to "人教版",
                    "ui.learning_filter" to LearningFilter.Review.name,
                    "ui.shows_mistakes" to true,
                    "ui.session_search" to "辛亥革命",
                    "ui.conversation_id" to "conversation-restore",
                ),
            )
            val viewModel = AppViewModel(FakeRepository(authenticated = false), savedStateHandle = savedState)

            assertEquals(AppDestination.Graph, viewModel.uiState.value.destination)
            assertEquals("尚未发送的问题", viewModel.uiState.value.draft)
            assertEquals(QaHelpLevel.Full, viewModel.uiState.value.helpLevel)
            assertEquals(QaMode.Verify, viewModel.uiState.value.qaMode)
            assertEquals("历史", viewModel.uiState.value.selectedKnowledgeScope?.subject)
            assertEquals("人教版", viewModel.uiState.value.selectedKnowledgeScope?.textbookVersion)
            assertEquals("conversation-restore", viewModel.uiState.value.conversationId)

            viewModel.updateDraft("新的草稿")
            viewModel.selectDestination(AppDestination.Learning)
            viewModel.selectHelpLevel(QaHelpLevel.Keyword)
            advanceUntilIdle()

            assertEquals("新的草稿", savedState.get<String>("ui.chat_draft"))
            assertEquals(AppDestination.Learning.name, savedState.get<String>("ui.destination"))
            assertEquals(QaHelpLevel.Keyword.name, savedState.get<String>("ui.help_level"))
            assertEquals(null, savedState.get<String>("ui.password"))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun authenticatedProcessRecreationReloadsConversationFromBackend() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository(authenticated = true)
            val savedState = SavedStateHandle(mapOf("ui.conversation_id" to "restored-session"))

            AppViewModel(repository, savedStateHandle = savedState)
            advanceUntilIdle()

            assertEquals(listOf("restored-session"), repository.restoredSessions)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun selectingNodeRestoresPrivateNoteAndFavoriteFromDetail() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = AppViewModel(FakeRepository())
            advanceUntilIdle()

            viewModel.selectNode("quadratic_function")
            advanceUntilIdle()

            assertEquals("复习定义域", viewModel.uiState.value.noteDraft)
            assertTrue(viewModel.uiState.value.selectedNodeDetail?.saved == true)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun favoriteAndLatestPrivateNoteArePersistedAndReflectedInDetail() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository()
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()
            viewModel.selectNode("quadratic_function")
            advanceUntilIdle()

            viewModel.toggleFavorite()
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.selectedNodeDetail?.saved)
            assertEquals(false, repository.nodeStateUpdates.last().favorite)

            repository.nodeStateUpdates.clear()
            viewModel.updateNote("第一版")
            viewModel.updateNote("最终笔记")
            advanceUntilIdle()

            assertEquals("最终笔记", viewModel.uiState.value.selectedNodeDetail?.note)
            assertEquals(listOf("最终笔记"), repository.nodeStateUpdates.map { it.note })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun existingSessionWaitsForUpdatedPrivacyConsent() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository(privacyRequired = true)
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.privacyConsentRequired)
            viewModel.acceptPrivacyConsent()
            advanceUntilIdle()

            assertTrue(repository.privacyAccepted)
            assertEquals(false, viewModel.uiState.value.privacyConsentRequired)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun mistakeCanBeCarriedIntoPrivateErrorReviewChat() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val viewModel = AppViewModel(FakeRepository())
            advanceUntilIdle()
            val mistake = mistakeWithStatus("succeeded").copy(knowledgeNodeId = "quadratic_function")

            viewModel.askAboutMistake(mistake)

            val state = viewModel.uiState.value
            assertEquals(AppDestination.Chat, state.destination)
            assertEquals(QaMode.Error, state.qaMode)
            assertEquals("quadratic_function", state.selectedNodeId)
            assertTrue(state.draft.contains(mistake.questionText))
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun communityActionsUseRepositoryAndRefreshBalance() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository()
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()

            viewModel.redeemOrganizationInvite("QK-invite-123")
            advanceUntilIdle()
            viewModel.redeemCreditCode("QKC-credit-123")
            advanceUntilIdle()
            viewModel.submitContribution("explanation", "补充知识", "这是一段足够长的知识补充内容，用于测试投稿提交链路。", null)
            advanceUntilIdle()

            assertEquals(listOf("QK-invite-123"), repository.redeemedInvites)
            assertEquals(listOf("QKC-credit-123"), repository.redeemedCodes)
            assertEquals(1290, viewModel.uiState.value.credits)
            assertEquals(listOf("补充知识"), repository.submittedContributionTitles)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun selectingKnowledgeScopeLoadsItsChapterTree() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository()
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()
            val history = KnowledgeCatalogScope("历史", "高一", "人教版", 1)

            viewModel.selectKnowledgeScope(history)
            advanceUntilIdle()

            assertEquals(history, viewModel.uiState.value.selectedKnowledgeScope)
            assertEquals("历史", viewModel.uiState.value.currentSubject)
            assertEquals("第一章", viewModel.uiState.value.knowledgeChapters.single().name)
            assertEquals(history, repository.requestedKnowledgeScopes.last())
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun serverValidatedUnderstandingCheckUpdatesNodeStatus() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository()
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()
            viewModel.selectNode("quadratic_function")
            advanceUntilIdle()

            viewModel.startUnderstandingCheck()
            advanceUntilIdle()
            val check = viewModel.uiState.value.understandingCheck
            assertEquals("check-1", check?.id)

            viewModel.submitUnderstandingCheck("choice-correct")
            advanceUntilIdle()

            assertEquals(null, viewModel.uiState.value.understandingCheck)
            assertTrue(viewModel.uiState.value.errorMessage?.contains("已验证") == true)
            assertEquals(listOf("check-1" to "choice-correct"), repository.submittedChecks)
        } finally {
            Dispatchers.resetMain()
        }
    }

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
    fun detailedAnswerErrorIsAttachedToTheServerMessage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository()
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()
            viewModel.updateDraft("二次函数是什么？")
            viewModel.sendMessage()
            advanceUntilIdle()

            val assistantId = viewModel.uiState.value.messages.last().id
            viewModel.submitContentErrorFeedback(assistantId, "公式缺少定义域条件")
            advanceUntilIdle()

            assertEquals(listOf("message-2"), repository.answerFeedbackMessageIds)
            assertEquals(listOf(AnswerFeedbackAction.ContentError), repository.answerFeedbackActions)
            assertEquals(listOf("公式缺少定义域条件"), repository.answerFeedbackDetails)
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
    fun unavailableAiDisablesQuestionAndMistakeAnalysisBeforeNetworkCall() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository(aiAvailable = false)
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()
            viewModel.updateDraft("什么是二次函数？")

            viewModel.sendMessage()
            viewModel.analyzeMistake("mistake-1")
            advanceUntilIdle()

            assertEquals(false, viewModel.uiState.value.aiAvailable)
            assertEquals(0, repository.questionsSent)
            assertTrue(repository.analyzedMistakes.isEmpty())
            assertTrue(viewModel.uiState.value.errorMessage?.contains("维护") == true)
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
    fun vagueQuestionRequiresClarificationBeforeSending() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository(clarifyVague = true)
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()
            viewModel.updateDraft("这个怎么做")
            viewModel.sendMessage()
            advanceUntilIdle()
            assertEquals(0, repository.questionsSent)
            val option = viewModel.uiState.value.qaClarification!!.options.first()
            viewModel.selectQaClarification(option)
            advanceUntilIdle()
            assertEquals(1, repository.questionsSent)
            assertEquals("这个怎么做", viewModel.uiState.value.messages.first().text)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun stoppingStreamCancelsRequestAndMarksPartialAnswerOnlyOnce() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val gate = CompletableDeferred<Unit>()
            val repository = FakeRepository(streamGates = mutableListOf(gate))
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()
            viewModel.updateDraft("解释二次函数")

            viewModel.sendMessage()
            runCurrent()
            assertEquals("二次函数", viewModel.uiState.value.messages.last().text)

            viewModel.stopGenerating()
            runCurrent()
            viewModel.stopGenerating()

            assertEquals(1, repository.cancelledQuestions)
            assertEquals(false, viewModel.uiState.value.sending)
            assertEquals("二次函数\n\n已停止生成", viewModel.uiState.value.messages.last().text)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun stoppingDuringClarificationDoesNotModifyPreviousAnswer() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val clarificationGate = CompletableDeferred<Unit>()
            val repository = FakeRepository(clarifyVague = true, clarificationGate = clarificationGate)
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()
            viewModel.updateDraft("二次函数是什么？")
            viewModel.sendMessage()
            advanceUntilIdle()
            val previousAnswer = viewModel.uiState.value.messages.last()

            viewModel.updateDraft("这个怎么做")
            viewModel.sendMessage()
            runCurrent()
            viewModel.stopGenerating()
            runCurrent()

            assertEquals(2, viewModel.uiState.value.messages.size)
            assertEquals(previousAnswer, viewModel.uiState.value.messages.last())
            assertEquals(null, viewModel.uiState.value.qaClarification)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun sendingAgainImmediatelyAfterStopKeepsNewRequestActive() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val firstGate = CompletableDeferred<Unit>()
            val secondGate = CompletableDeferred<Unit>()
            val repository = FakeRepository(streamGates = mutableListOf(firstGate, secondGate))
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()
            viewModel.updateDraft("第一问")
            viewModel.sendMessage()
            runCurrent()

            viewModel.stopGenerating()
            viewModel.updateDraft("第二问")
            viewModel.sendMessage()
            runCurrent()

            assertEquals(2, repository.questionsSent)
            assertEquals(true, viewModel.uiState.value.sending)
            assertEquals("二次函数", viewModel.uiState.value.messages.last().text)
            assertEquals(4, viewModel.uiState.value.messages.map { it.id }.distinct().size)

            secondGate.complete(Unit)
            advanceUntilIdle()
            assertEquals(false, viewModel.uiState.value.sending)
            assertEquals("二次函数回答", viewModel.uiState.value.messages.last().text)
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
    fun manualMistakeDraftIsSavedWithoutAnImage() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository()
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()

            viewModel.saveMistakeDraft("", "数学", "求函数的定义域", "", "分析错因", "method")
            advanceUntilIdle()

            assertEquals(listOf(""), repository.savedDraftImagePaths)
            assertEquals("手动题目已保存，将在网络可用时同步", viewModel.uiState.value.errorMessage)
            assertTrue(viewModel.uiState.value.learningShowsMistakes)
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

    @Test
    fun learningFilterLoadsTheMatchingServerCollection() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository()
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()

            viewModel.selectLearningFilter(LearningFilter.Verified)
            advanceUntilIdle()

            assertEquals(LearningFilter.Verified, viewModel.uiState.value.learningFilter)
            assertEquals(listOf("verified-node"), viewModel.uiState.value.learningItems.map { it.nodeId })
            assertEquals(LearningFilter.Verified, repository.lastLearningFilter)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun sessionSearchUsesServerQueryAndKeepsResultsRestorable() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository()
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()

            viewModel.updateSessionSearch("  判别式  ")
            viewModel.searchSessions()
            advanceUntilIdle()

            assertEquals("判别式", repository.lastSessionQuery)
            assertEquals(listOf("matched-session"), viewModel.uiState.value.sessions.map { it.id })
            assertEquals("  判别式  ", viewModel.uiState.value.sessionSearchQuery)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun mistakeRecoveryActionsCallRepositoryAndDeleteFromState() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository()
            val viewModel = AppViewModel(repository)
            advanceUntilIdle()

            viewModel.retryMistakeDraft("draft-1")
            viewModel.cancelMistakeOcr("mistake-1", "task-1")
            viewModel.retryMistakeOcr("mistake-1", "task-1")
            viewModel.deleteMistake("mistake-1")
            advanceUntilIdle()

            assertEquals(listOf("draft-1"), repository.retriedDrafts)
            assertEquals(listOf("mistake-1" to "task-1"), repository.cancelledOcrTasks)
            assertEquals(listOf("mistake-1" to "task-1"), repository.retriedOcrTasks)
            assertEquals(listOf("mistake-1"), repository.deletedMistakes)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun mistakeBookPollsQueuedOcrUntilItFinishes() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        try {
            val repository = FakeRepository()
            val viewModel = AppViewModel(
                repository,
                mistakePollIntervalMillis = 100,
                mistakePollMaxAttempts = 5,
            )
            advanceUntilIdle()
            repository.mistakeResponses += listOf(mistakeWithStatus("queued"))
            repository.mistakeResponses += listOf(mistakeWithStatus("succeeded"))

            viewModel.selectDestination(AppDestination.Learning)
            viewModel.showMistakeBook()
            advanceUntilIdle()

            assertEquals("succeeded", viewModel.uiState.value.mistakes.single().ocrStatus)
            assertEquals(3, repository.mistakeCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }
}

private fun mistakeWithStatus(status: String) = MistakeItem(
    id = "polling-mistake",
    subject = "数学",
    questionText = "测试 OCR 状态刷新",
    ocrTaskId = "polling-task",
    ocrStatus = status,
    confidence = null,
    requiresReview = false,
    reviewReasons = emptyList(),
    errorCategory = null,
    errorNote = null,
    analysisStatus = "not_started",
    analysisDiagnosis = null,
    correctionSteps = emptyList(),
    knowledgeNodeId = null,
    practices = emptyList(),
    studyStatus = "new",
    reviewStage = "correction",
    nextReviewAt = null,
    secondAttemptCorrect = null,
    reviewStreak = 0,
)

private class FakeRepository(
    private val answerBalance: Int = 1280,
    private val authenticated: Boolean = true,
    private val clarifyVague: Boolean = false,
    private var privacyRequired: Boolean = false,
    private val streamGates: MutableList<CompletableDeferred<Unit>> = mutableListOf(),
    private val clarificationGate: CompletableDeferred<Unit>? = null,
    private val aiAvailable: Boolean = true,
) : AppRepository {
    var privacyAccepted = false
    var questionsSent = 0
    var cancelledQuestions = 0
    val revokedSessions = mutableListOf<String>()
    val feedbackSubmissions = mutableListOf<Pair<String, String>>()
    val analyzedMistakes = mutableListOf<String>()
    var lastLearningFilter = LearningFilter.Recent
    var lastSessionQuery: String? = null
    val retriedDrafts = mutableListOf<String>()
    val cancelledOcrTasks = mutableListOf<Pair<String, String>>()
    val retriedOcrTasks = mutableListOf<Pair<String, String>>()
    val deletedMistakes = mutableListOf<String>()
    val savedDraftImagePaths = mutableListOf<String>()
    val mistakeResponses = mutableListOf<List<MistakeItem>>()
    var mistakeCalls = 0
    val submittedChecks = mutableListOf<Pair<String, String>>()
    val redeemedInvites = mutableListOf<String>()
    val redeemedCodes = mutableListOf<String>()
    val submittedContributionTitles = mutableListOf<String>()
    val requestedKnowledgeScopes = mutableListOf<KnowledgeCatalogScope>()
    val restoredSessions = mutableListOf<String>()
    val answerFeedbackMessageIds = mutableListOf<String?>()
    val answerFeedbackActions = mutableListOf<AnswerFeedbackAction>()
    val answerFeedbackDetails = mutableListOf<String?>()
    val nodeStateUpdates = mutableListOf<NodeStateCall>()

    override suspend fun serviceAvailability() = ServiceAvailability(
        aiAvailable = aiAvailable,
        message = if (aiAvailable) null else "AI 服务维护中，浏览和已有学习记录仍可使用",
    )

    override suspend fun hasSession() = authenticated
    override suspend fun nickname(): String? = null
    override suspend fun login(username: String, password: String) = "测试同学"
    override suspend fun register(username: String, password: String, nickname: String, email: String) = nickname
    override suspend fun privacyConsentRequired() = privacyRequired
    override suspend fun acceptPrivacyConsent() {
        privacyAccepted = true
        privacyRequired = false
    }
    override suspend fun requestPasswordReset(email: String) = Unit
    override suspend fun confirmPasswordReset(token: String, newPassword: String) = Unit
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
    override suspend fun schoolMemberships(): List<SchoolMembershipItem> = emptyList()
    override suspend fun schoolClasses(schoolId: String): List<SchoolClassItem> = emptyList()
    override suspend fun redeemOrganizationInvite(code: String) { redeemedInvites += code }
    override suspend fun leaveSchool(schoolId: String) = Unit
    override suspend fun classOverview(classId: String): ClassOverviewItem = throw UnsupportedOperationException()
    override suspend fun creditCampaigns(): List<CreditCampaignItem> = emptyList()
    override suspend fun creditRedemptions(): List<CreditRedemptionItem> = emptyList()
    override suspend fun redeemCreditCode(code: String): cn.qingkui.app.data.repository.CreditRedeemOutcome {
        redeemedCodes += code
        return cn.qingkui.app.data.repository.CreditRedeemOutcome("测试活动", 10, answerBalance + 10)
    }
    override suspend fun contributions(): List<ContributionItem> = emptyList()
    override suspend fun submitContribution(type: String, title: String, content: String, sourceReference: String?) { submittedContributionTitles += title }
    override suspend fun deleteContribution(contributionId: String) = Unit
    override suspend fun graph(centerId: String) = GraphData(emptyList(), emptyList(), null)
    override suspend fun nodeDetail(nodeId: String) = cn.qingkui.app.ui.model.KnowledgeNode(
        id = nodeId,
        title = "二次函数",
        subtitle = "函数",
        status = KnowledgeStatus.Explored,
        x = .5f,
        y = .5f,
        saved = true,
        note = "复习定义域",
    )
    override suspend fun search(query: String) = GraphData(emptyList(), emptyList(), null)
    override suspend fun knowledgeCatalog() = listOf(
        KnowledgeCatalogScope("数学", "高一", "人教A版", 6),
        KnowledgeCatalogScope("历史", "高一", "人教版", 1),
    )
    override suspend fun knowledgeTree(scope: KnowledgeCatalogScope): List<KnowledgeTreeChapter> {
        requestedKnowledgeScopes += scope
        return if (scope.subject == "历史") {
            listOf(
                KnowledgeTreeChapter(
                    "第一章",
                    listOf(KnowledgeTreeSection("第一节", listOf(KnowledgeTreeNode("history-1", "中华文明起源", KnowledgeStatus.Unexplored)))),
                ),
            )
        } else emptyList()
    }
    override suspend fun sessions(query: String?): List<cn.qingkui.app.ui.model.ConversationSummary> {
        lastSessionQuery = query
        return if (query == "判别式") {
            listOf(cn.qingkui.app.ui.model.ConversationSummary("matched-session", "判别式", "knowledge", "数学"))
        } else {
            emptyList()
        }
    }
    override suspend fun restoreSession(sessionId: String): List<ChatMessage> {
        restoredSessions += sessionId
        return emptyList()
    }
    override suspend fun deleteSession(sessionId: String) = Unit
    override suspend fun clarifyQaIntent(question: String, mode: QaMode): QaClarification? =
        if (clarifyVague && question == "这个怎么做") {
            clarificationGate?.await()
            QaClarification(
                question,
                "你希望我怎样帮助你？",
                listOf(QaClarificationOption("solve", "分析题目", "分析题目条件并给出解题思路", QaMode.Problem)),
            )
        } else null
    override suspend fun learningItems(filter: LearningFilter): List<LearningItem> {
        lastLearningFilter = filter
        return if (filter == LearningFilter.Verified) {
            listOf(LearningItem("verified-node", "已验证知识点", KnowledgeStatus.Verified, "刚刚", "回到图谱"))
        } else {
            emptyList()
        }
    }
    override suspend fun updateNodeState(nodeId: String, status: cn.qingkui.app.ui.model.KnowledgeStatus, note: String?, favorite: Boolean?): LearningItem? {
        nodeStateUpdates += NodeStateCall(nodeId, status, note, favorite)
        return null
    }
    override suspend fun startUnderstandingCheck(nodeId: String) = UnderstandingCheck(
        id = "check-1",
        nodeId = nodeId,
        prompt = "选择定义",
        choices = listOf(UnderstandingCheckChoice("choice-correct", "正确定义")),
    )
    override suspend fun submitUnderstandingCheck(attemptId: String, choiceId: String): UnderstandingCheckOutcome {
        submittedChecks += attemptId to choiceId
        return UnderstandingCheckOutcome(true, KnowledgeStatus.Verified)
    }
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
        val gate = streamGates.removeFirstOrNull()
        if (gate != null) {
            try {
                gate.await()
            } catch (cancelled: CancellationException) {
                cancelledQuestions++
                throw cancelled
            }
        }
        onDelta("回答")
        return QaAnswer(
            conversationId = "session-1",
            message = ChatMessage(
                id = 2L,
                author = MessageAuthor.Assistant,
                text = "二次函数回答",
                source = "二次函数 · 演示来源",
                serverId = "message-2",
            ),
            balance = answerBalance,
            creditsCharged = helpLevel.creditCost,
        )
    }

    override suspend fun submitAnswerFeedback(messageId: String?, action: AnswerFeedbackAction, detail: String?) {
        answerFeedbackMessageIds += messageId
        answerFeedbackActions += action
        answerFeedbackDetails += detail
    }
    override fun observeMistakeDrafts(): Flow<List<MistakeDraftItem>> = flowOf(emptyList())
    override suspend fun mistakes(): List<MistakeItem> {
        mistakeCalls += 1
        return if (mistakeResponses.isEmpty()) emptyList() else mistakeResponses.removeAt(0)
    }
    override suspend fun mistakeWeeklyReview() = cn.qingkui.app.ui.model.MistakeWeeklyReview(
        weekStart = "2026-08-24",
        weekEnd = "2026-08-31",
        newMistakes = 0,
        dueReviewCount = 0,
        topErrorCategory = null,
        uploadSuccessRate = 0.0,
        ocrCorrectionRate = 0.0,
        practiceCompletionRate = 0.0,
        authoritativeAccuracy = 0.0,
        secondAttemptAccuracy = 0.0,
        sevenDayFollowupRate = 0.0,
    )
    override suspend fun saveMistakeDraft(
        imagePath: String,
        subject: String,
        questionText: String,
        studentWork: String,
        questionGoal: String,
        errorCategory: String,
    ) {
        savedDraftImagePaths += imagePath
    }
    override suspend fun retryMistakeDraft(draftId: String) { retriedDrafts += draftId }
    override suspend fun deleteMistakeDraft(draftId: String) = Unit
    override suspend fun cancelMistakeOcr(mistakeId: String, taskId: String) { cancelledOcrTasks += mistakeId to taskId }
    override suspend fun retryMistakeOcr(mistakeId: String, taskId: String) { retriedOcrTasks += mistakeId to taskId }
    override suspend fun deleteMistake(mistakeId: String) { deletedMistakes += mistakeId }
    override suspend fun confirmMistakeOcr(mistakeId: String, taskId: String, correctedText: String) = Unit
    override suspend fun analyzeMistake(mistakeId: String): MistakeAnalysisOutcome {
        analyzedMistakes += mistakeId
        return MistakeAnalysisOutcome(answerBalance)
    }
    override suspend fun generateMistakePractice(mistakeId: String) = Unit
    override suspend fun submitMistakePractice(mistakeId: String, practiceId: String, answer: String) = Unit
}

private data class NodeStateCall(
    val nodeId: String,
    val status: KnowledgeStatus,
    val note: String?,
    val favorite: Boolean?,
)
