package cn.qingkui.app.data.repository

import cn.qingkui.app.BuildConfig

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import cn.qingkui.app.data.auth.TokenStore
import cn.qingkui.app.data.local.MistakeDatabase
import cn.qingkui.app.data.local.MistakeDraftEntity
import cn.qingkui.app.data.local.mistakeDraftValidationError
import cn.qingkui.app.data.remote.NetworkModule
import cn.qingkui.app.data.remote.QingkuiApi
import cn.qingkui.app.data.remote.dto.ApiErrorDto
import cn.qingkui.app.data.remote.dto.ConversationCreate
import cn.qingkui.app.data.remote.dto.ChangePasswordRequest
import cn.qingkui.app.data.remote.dto.FeedbackCreate
import cn.qingkui.app.data.remote.dto.KnowledgeNodeDto
import cn.qingkui.app.data.remote.dto.LearningEventCreate
import cn.qingkui.app.data.remote.dto.LearningEventRequest
import cn.qingkui.app.data.remote.dto.LearningCheckSubmitDto
import cn.qingkui.app.data.remote.dto.KnowledgeStateUpdate
import cn.qingkui.app.data.remote.dto.KnowledgeNodeDetailDto
import cn.qingkui.app.data.remote.dto.LoginRequest
import cn.qingkui.app.data.remote.dto.LogoutRequest
import cn.qingkui.app.data.remote.dto.MessageCreate
import cn.qingkui.app.data.remote.dto.NeighborNodeDto
import cn.qingkui.app.data.remote.dto.OcrCorrectionDto
import cn.qingkui.app.data.remote.dto.PracticeSubmitDto
import cn.qingkui.app.data.remote.dto.RegisterRequest
import cn.qingkui.app.data.remote.dto.PasswordResetRequest
import cn.qingkui.app.data.remote.dto.PasswordResetConfirm
import cn.qingkui.app.data.remote.dto.QaIntentRequest
import cn.qingkui.app.data.work.MistakeUploadWorker
import cn.qingkui.app.ui.model.ChatMessage
import cn.qingkui.app.ui.model.AnswerFeedbackAction
import cn.qingkui.app.ui.model.ConversationSummary
import cn.qingkui.app.ui.model.CreditLedgerItem
import cn.qingkui.app.ui.model.DeviceSessionItem
import cn.qingkui.app.ui.model.FeedbackItem
import cn.qingkui.app.ui.model.ClassOverviewItem
import cn.qingkui.app.ui.model.ClassAggregateCountItem
import cn.qingkui.app.ui.model.ClassStudentOverviewItem
import cn.qingkui.app.ui.model.ContributionItem
import cn.qingkui.app.ui.model.CreditCampaignItem
import cn.qingkui.app.ui.model.CreditRedemptionItem
import cn.qingkui.app.ui.model.KnowledgeKind
import cn.qingkui.app.ui.model.KnowledgeNode
import cn.qingkui.app.ui.model.KnowledgeRelation
import cn.qingkui.app.ui.model.KnowledgeSource
import cn.qingkui.app.ui.model.KnowledgeCatalogScope
import cn.qingkui.app.ui.model.KnowledgeTreeChapter
import cn.qingkui.app.ui.model.KnowledgeTreeSection
import cn.qingkui.app.ui.model.KnowledgeTreeNode
import cn.qingkui.app.ui.model.KnowledgeStatus
import cn.qingkui.app.ui.model.LearningItem
import cn.qingkui.app.ui.model.LearningFilter
import cn.qingkui.app.ui.model.UnderstandingCheck
import cn.qingkui.app.ui.model.UnderstandingCheckChoice
import cn.qingkui.app.ui.model.MessageAuthor
import cn.qingkui.app.ui.model.MessageCitation
import cn.qingkui.app.ui.model.MistakeDraftItem
import cn.qingkui.app.ui.model.MistakeItem
import cn.qingkui.app.ui.model.MistakePracticeItem
import cn.qingkui.app.ui.model.MistakeWeeklyReview
import cn.qingkui.app.ui.model.QaHelpLevel
import cn.qingkui.app.ui.model.QaMode
import cn.qingkui.app.ui.model.QaClarification
import cn.qingkui.app.ui.model.QaClarificationOption
import cn.qingkui.app.ui.model.RelationType
import cn.qingkui.app.ui.model.SchoolClassItem
import cn.qingkui.app.ui.model.SchoolMembershipItem
import cn.qingkui.app.ui.model.WorkspaceAlert
import cn.qingkui.app.ui.model.WorkspaceCapabilities
import cn.qingkui.app.ui.model.WorkspaceDashboard
import cn.qingkui.app.ui.model.WorkspaceMetricSnapshot
import cn.qingkui.app.ui.model.WorkspaceTask
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import retrofit2.HttpException
import java.io.IOException
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.math.cos
import kotlin.math.sin

data class GraphData(
    val nodes: List<KnowledgeNode>,
    val relations: List<KnowledgeRelation>,
    val selectedNodeId: String?,
)

data class QaAnswer(
    val conversationId: String,
    val message: ChatMessage,
    val balance: Int,
    val creditsCharged: Int,
    val subject: String? = null,
)

data class MistakeAnalysisOutcome(val balance: Int)
data class UnderstandingCheckOutcome(val passed: Boolean, val status: KnowledgeStatus)
data class CreditRedeemOutcome(val campaignName: String, val amount: Int, val balance: Int)
data class ServiceAvailability(val aiAvailable: Boolean, val message: String? = null)

class ApiFailureException(val statusCode: Int?, message: String) : Exception(message)

interface AppRepository {
    suspend fun serviceAvailability(): ServiceAvailability = ServiceAvailability(aiAvailable = true)
    suspend fun workspaceMe(): Pair<WorkspaceCapabilities, List<String>> = WorkspaceCapabilities() to emptyList()
    suspend fun workspaceDashboard(): WorkspaceDashboard = WorkspaceDashboard()
    suspend fun workspaceTasks(): List<WorkspaceTask> = emptyList()
    suspend fun workspaceAlerts(): List<WorkspaceAlert> = emptyList()
    suspend fun hasSession(): Boolean
    suspend fun nickname(): String?
    suspend fun login(username: String, password: String): String
    suspend fun register(
        username: String,
        password: String,
        nickname: String,
        email: String,
        privacyConsent: Boolean,
    ): String
    suspend fun privacyConsentRequired(): Boolean
    suspend fun acceptPrivacyConsent()
    suspend fun requestPasswordReset(email: String)
    suspend fun confirmPasswordReset(token: String, newPassword: String)
    suspend fun logout()
    suspend fun credits(): Int
    suspend fun creditLedger(): List<CreditLedgerItem>
    suspend fun deviceSessions(): List<DeviceSessionItem>
    suspend fun revokeDeviceSession(sessionId: String)
    suspend fun feedback(): List<FeedbackItem>
    suspend fun submitFeedback(category: String, content: String)
    suspend fun schoolMemberships(): List<SchoolMembershipItem>
    suspend fun schoolClasses(schoolId: String): List<SchoolClassItem>
    suspend fun redeemOrganizationInvite(code: String)
    suspend fun leaveSchool(schoolId: String)
    suspend fun classOverview(classId: String): ClassOverviewItem
    suspend fun creditCampaigns(): List<CreditCampaignItem>
    suspend fun creditRedemptions(): List<CreditRedemptionItem>
    suspend fun redeemCreditCode(code: String): CreditRedeemOutcome
    suspend fun contributions(): List<ContributionItem>
    suspend fun submitContribution(type: String, title: String, content: String, sourceReference: String?)
    suspend fun deleteContribution(contributionId: String)
    suspend fun graph(centerId: String = "quadratic_function"): GraphData
    suspend fun nodeDetail(nodeId: String): KnowledgeNode
    suspend fun search(query: String): GraphData
    suspend fun knowledgeCatalog(): List<KnowledgeCatalogScope>
    suspend fun knowledgeTree(scope: KnowledgeCatalogScope): List<KnowledgeTreeChapter>
    suspend fun sessions(query: String? = null): List<ConversationSummary>
    suspend fun restoreSession(sessionId: String): List<ChatMessage>
    suspend fun deleteSession(sessionId: String)
    suspend fun clarifyQaIntent(question: String, mode: QaMode): QaClarification?
    suspend fun learningItems(filter: LearningFilter = LearningFilter.Recent): List<LearningItem>
    suspend fun updateNodeState(nodeId: String, status: KnowledgeStatus, note: String?, favorite: Boolean?): LearningItem?
    suspend fun startUnderstandingCheck(nodeId: String): UnderstandingCheck
    suspend fun submitUnderstandingCheck(attemptId: String, choiceId: String): UnderstandingCheckOutcome
    suspend fun recordLearningEvent(nodeId: String, eventType: String, data: Map<String, Any?> = emptyMap())
    suspend fun changePassword(current: String, next: String)
    suspend fun deleteAccount()
    suspend fun sendQuestion(
        sessionId: String?,
        nodeId: String?,
        question: String,
        mode: QaMode,
        helpLevel: QaHelpLevel,
        onDelta: (String) -> Unit,
    ): QaAnswer
    suspend fun submitAnswerFeedback(messageId: String?, action: AnswerFeedbackAction, detail: String? = null)
    fun observeMistakeDrafts(): Flow<List<MistakeDraftItem>>
    suspend fun mistakes(): List<MistakeItem>
    suspend fun mistakeWeeklyReview(): MistakeWeeklyReview
    suspend fun saveMistakeDraft(
        imagePath: String,
        subject: String,
        questionText: String,
        studentWork: String,
        questionGoal: String,
        errorCategory: String,
    )
    suspend fun retryMistakeDraft(draftId: String)
    suspend fun deleteMistakeDraft(draftId: String)
    suspend fun cancelMistakeOcr(mistakeId: String, taskId: String)
    suspend fun retryMistakeOcr(mistakeId: String, taskId: String)
    suspend fun deleteMistake(mistakeId: String)
    suspend fun confirmMistakeOcr(mistakeId: String, taskId: String, correctedText: String)
    suspend fun analyzeMistake(mistakeId: String): MistakeAnalysisOutcome
    suspend fun generateMistakePractice(mistakeId: String)
    suspend fun submitMistakePractice(mistakeId: String, practiceId: String, answer: String)
}

class NetworkAppRepository(
    private val api: QingkuiApi,
    private val tokenStore: TokenStore,
    private val context: Context,
) : AppRepository {
    private val gson = Gson()
    private val draftDao = MistakeDatabase.get(context).drafts()

    override suspend fun serviceAvailability(): ServiceAvailability = apiCall {
        val health = api.health()
        val available = health.aiEnabled && health.aiReady
        ServiceAvailability(
            aiAvailable = available,
            message = if (available) null else "AI 服务维护中，浏览和已有学习记录仍可使用",
        )
    }

    override suspend fun workspaceMe(): Pair<WorkspaceCapabilities, List<String>> = apiCall {
        val value = api.workspaceMe()
        WorkspaceCapabilities(
            studentWorkspace = value.capabilities.studentWorkspace,
            teacherWorkspace = value.capabilities.teacherWorkspace,
            contentWorkspace = value.capabilities.contentWorkspace,
            operationsWorkspace = value.capabilities.operationsWorkspace,
            rawStudentContent = value.capabilities.rawStudentContent,
        ) to value.roles
    }

    override suspend fun workspaceDashboard(): WorkspaceDashboard = apiCall {
        val value = api.workspaceDashboard()
        val m = value.metrics
        WorkspaceDashboard(
            generatedAt = value.generatedAt.displayDateTime(),
            rangeLabel = "近 ${value.range.days.coerceAtLeast(1)} 天",
            metrics = WorkspaceMetricSnapshot(
                activeStudents = m.activeStudents,
                sevenDayReturnRate = m.sevenDayReturnRate,
                mistakeUploadSuccessRate = m.mistakeUploadSuccessRate,
                ocrCorrectionRate = m.ocrCorrectionRate,
                queuedOcr = m.ocrQueue.queued,
                processingOcr = m.ocrQueue.processing,
                failedOcr = m.ocrQueue.failed,
                ocrNeedsReview = m.ocrQueue.needsReview,
                mistakeAnalysisCompletionRate = m.mistakeAnalysisCompletionRate,
                samePracticeCompletionRate = m.samePracticeCompletionRate,
                secondAttemptAccuracy = m.secondAttemptAccuracy,
                aiHelpfulRate = m.aiHelpfulRate,
                aiFailureRate = m.aiFailureRate,
                averageUserCostTokens = m.averageUserCostTokens,
                mistakesCreated = m.mistakesCreated,
                pendingContent = m.pendingContent,
                pendingFormulas = m.pendingFormulas,
                approvedNodes = m.approvedNodes,
                documents = m.documents,
                knowledgeEdges = m.knowledgeEdges,
                pendingFeedback = m.pendingFeedback,
                auditEvents = m.auditEvents,
                releaseGatePassed = m.releaseGatePassed,
                securityEvents = m.securityEvents,
            ),
            alerts = value.alerts.alerts.map { WorkspaceAlert(it.severity, it.code, it.message, it.value) },
            releaseBlockers = value.alerts.releaseBlockers.map { WorkspaceAlert(it.severity, it.code, it.message, it.value) },
        )
    }

    override suspend fun workspaceTasks(): List<WorkspaceTask> = apiCall {
        api.workspaceTasks().items.map {
            WorkspaceTask(it.taskType, it.id, it.status, it.requiresReview, it.createdAt.displayDateTime(), it.errorCode)
        }
    }

    override suspend fun workspaceAlerts(): List<WorkspaceAlert> = apiCall {
        val value = api.workspaceAlerts()
        (value.alerts + value.releaseBlockers).map { WorkspaceAlert(it.severity, it.code, it.message, it.value) }
    }

    override suspend fun hasSession(): Boolean = tokenStore.refreshToken() != null
    override suspend fun nickname(): String? = tokenStore.nickname()

    override suspend fun login(username: String, password: String): String = apiCall {
        api.login(LoginRequest(username.trim(), password)).also { tokenStore.save(it) }.user.nickname
    }

    override suspend fun register(
        username: String,
        password: String,
        nickname: String,
        email: String,
        privacyConsent: Boolean,
    ): String = apiCall {
        api.register(
            RegisterRequest(
                username.trim(),
                password,
                nickname.trim().ifBlank { null },
                email.trim().ifBlank { null },
                privacyConsent = privacyConsent,
                privacyNoticeVersion = BuildConfig.PRIVACY_NOTICE_VERSION,
            )
        )
            .also { tokenStore.save(it) }
            .user.nickname
    }

    override suspend fun privacyConsentRequired(): Boolean = apiCall { api.privacyConsent().required }

    override suspend fun acceptPrivacyConsent() = apiCall {
        api.acceptPrivacyConsent(
            cn.qingkui.app.data.remote.dto.PrivacyConsentRequest(
                noticeVersion = BuildConfig.PRIVACY_NOTICE_VERSION,
            )
        )
        Unit
    }

    override suspend fun requestPasswordReset(email: String) = apiCall {
        api.requestPasswordReset(PasswordResetRequest(email.trim()))
    }

    override suspend fun confirmPasswordReset(token: String, newPassword: String) = apiCall {
        api.confirmPasswordReset(PasswordResetConfirm(token.trim(), newPassword))
    }

    override suspend fun logout() {
        val refreshToken = tokenStore.refreshToken()
        try {
            if (refreshToken != null) api.logout(LogoutRequest(refreshToken))
        } catch (_: Exception) {
            // Local sign-out must remain available when the backend is offline.
        } finally {
            clearLocalMistakes()
            tokenStore.clear()
        }
    }

    override suspend fun credits(): Int = apiCall { api.credits().balance }

    override suspend fun creditLedger(): List<CreditLedgerItem> = apiCall {
        api.creditLedger().map { item ->
            CreditLedgerItem(item.id, item.amount, item.balanceAfter, item.entryType, item.createdAt)
        }
    }

    override suspend fun deviceSessions(): List<DeviceSessionItem> = apiCall {
        api.deviceSessions().map { item ->
            DeviceSessionItem(
                id = item.id,
                deviceName = item.deviceName?.ifBlank { null } ?: "未命名设备",
                expiresAt = item.expiresAt.replace('T', ' ').take(16),
                active = item.active,
            )
        }
    }

    override suspend fun revokeDeviceSession(sessionId: String) = apiCall {
        val response = api.revokeDeviceSession(sessionId)
        if (!response.isSuccessful) throw ApiFailureException(response.code(), "撤销设备会话失败")
    }

    override suspend fun feedback(): List<FeedbackItem> = apiCall {
        api.feedback().map { item ->
            FeedbackItem(
                id = item.id,
                category = item.category,
                content = item.content,
                status = item.status,
                reviewNote = item.reviewNote,
                createdAt = item.createdAt.replace('T', ' ').take(16),
            )
        }
    }

    override suspend fun submitFeedback(category: String, content: String) = apiCall {
        api.submitFeedback(FeedbackCreate(category, content.trim(), null))
        Unit
    }

    override suspend fun schoolMemberships(): List<SchoolMembershipItem> = apiCall {
        api.organizations().memberships.map { item ->
            SchoolMembershipItem(item.id, item.schoolId, item.school.name, item.school.code, item.role, item.joinedAt.displayDateTime())
        }
    }

    override suspend fun schoolClasses(schoolId: String): List<SchoolClassItem> = apiCall {
        api.schoolClasses(schoolId).map { item ->
            SchoolClassItem(item.id, item.schoolId, item.name, item.grade.orEmpty(), item.academicYear)
        }
    }

    override suspend fun redeemOrganizationInvite(code: String) = apiCall {
        api.redeemOrganizationInvite(cn.qingkui.app.data.remote.dto.OrganizationInviteRedeemDto(code.trim()))
        Unit
    }

    override suspend fun leaveSchool(schoolId: String) = apiCall {
        val response = api.leaveSchool(schoolId)
        if (!response.isSuccessful) throw responseFailure(response.code(), response.errorBody()?.charStream())
    }

    override suspend fun classOverview(classId: String): ClassOverviewItem = apiCall {
        val value = api.classOverview(classId)
        ClassOverviewItem(
            classroom = value.classroom.toSchoolClassItem(),
            studentCount = value.studentCount,
            active7dStudents = value.active7dStudents,
            questions = value.questions,
            mistakes = value.mistakes,
            verifiedNodes = value.verifiedNodes,
            topErrorCategories = value.topErrorCategories.map { ClassAggregateCountItem(it.label, it.count) },
            weakKnowledgePoints = value.weakKnowledgePoints.map { ClassAggregateCountItem(it.label, it.count) },
            practiceCompletionRate = value.practiceCompletionRate,
            secondAttemptAccuracy = value.secondAttemptAccuracy,
            dueReviewCount = value.dueReviewCount,
            students = value.students.map { student ->
                ClassStudentOverviewItem(student.anonymousId, student.lastActivityAt?.displayDateTime(), student.questions, student.mistakes, student.verifiedNodes)
            },
        )
    }

    override suspend fun creditCampaigns(): List<CreditCampaignItem> = apiCall {
        api.creditCampaigns().map { item ->
            CreditCampaignItem(item.id, item.name, item.amount, item.schoolId, item.endsAt.displayDateTime(), (item.maxRedemptions - item.redemptionCount).coerceAtLeast(0))
        }
    }

    override suspend fun creditRedemptions(): List<CreditRedemptionItem> = apiCall {
        api.creditRedemptions().map { item ->
            CreditRedemptionItem(item.id, item.campaignId, item.campaignName, item.amount, item.createdAt.displayDateTime())
        }
    }

    override suspend fun redeemCreditCode(code: String): CreditRedeemOutcome = apiCall {
        val result = api.redeemCreditCode(cn.qingkui.app.data.remote.dto.CreditRedeemRequest(code.trim()))
        CreditRedeemOutcome(result.campaignName, result.amount, result.balance)
    }

    override suspend fun contributions(): List<ContributionItem> = apiCall {
        api.contributions().map { it.toContributionItem() }
    }

    override suspend fun submitContribution(type: String, title: String, content: String, sourceReference: String?) = apiCall {
        api.createContribution(
            cn.qingkui.app.data.remote.dto.ContributionCreateDto(type, title.trim(), content.trim(), sourceReference?.trim()?.ifBlank { null }),
        )
        Unit
    }

    override suspend fun deleteContribution(contributionId: String) = apiCall {
        val response = api.deleteContribution(contributionId)
        if (!response.isSuccessful) throw responseFailure(response.code(), response.errorBody()?.charStream())
    }

    override suspend fun graph(centerId: String): GraphData = apiCall {
        val response = api.neighbors(centerId)
        val center = response.center.toUi(.5f, .5f)
        val neighbors = response.nodes.mapIndexed { index, node ->
            val (ring, positionInRing) = graphRingPosition(index)
            val capacity = graphRingCapacity(ring)
            val angle = (2.0 * Math.PI * positionInRing / capacity) - Math.PI / 2
            val radius = (.15 + ring * .105).coerceAtMost(.45)
            node.toUi(
                x = (.5 + radius * cos(angle)).toFloat(),
                y = (.5 + radius * sin(angle)).toFloat(),
            )
        }
        GraphData(
            nodes = listOf(center) + neighbors,
            relations = response.nodes.map { node ->
                KnowledgeRelation(center.id, node.id, node.edgeType.toRelationType())
            },
            selectedNodeId = center.id,
        )
    }

    override suspend fun nodeDetail(nodeId: String): KnowledgeNode = apiCall {
        api.nodeDetail(nodeId).toUiNode(.5f, .5f)
    }

    override suspend fun startUnderstandingCheck(nodeId: String): UnderstandingCheck = apiCall {
        api.createLearningCheck(nodeId).let { check ->
            UnderstandingCheck(
                id = check.id,
                nodeId = check.nodeId,
                prompt = check.prompt,
                choices = check.choices.map { UnderstandingCheckChoice(it.id, it.text) },
            )
        }
    }

    override suspend fun submitUnderstandingCheck(
        attemptId: String,
        choiceId: String,
    ): UnderstandingCheckOutcome = apiCall {
        api.submitLearningCheck(attemptId, LearningCheckSubmitDto(choiceId)).let { result ->
            UnderstandingCheckOutcome(result.passed, result.state.status.toKnowledgeStatus())
        }
    }

    override suspend fun search(query: String): GraphData = apiCall {
        val nodes = api.search(query).mapIndexed { index, node ->
            val columns = 4
            node.toUi(
                x = ((index % columns) + 1f) / (columns + 1f),
                y = ((index / columns) + 1f) / ((index + columns) / columns + 1f),
            )
        }
        GraphData(nodes, emptyList(), nodes.firstOrNull()?.id)
    }

    override suspend fun knowledgeCatalog(): List<KnowledgeCatalogScope> = apiCall {
        api.knowledgeCatalog().map { KnowledgeCatalogScope(it.subject, it.grade, it.textbookVersion, it.nodeCount) }
    }

    override suspend fun knowledgeTree(scope: KnowledgeCatalogScope): List<KnowledgeTreeChapter> = apiCall {
        api.knowledgeTree(scope.subject, scope.grade, scope.textbookVersion).chapters.map { chapter ->
            KnowledgeTreeChapter(
                chapter.name,
                chapter.sections.map { section ->
                    KnowledgeTreeSection(
                        section.name,
                        section.nodes.map { node -> KnowledgeTreeNode(node.id, node.name, node.status.toKnowledgeStatus()) },
                    )
                },
            )
        }
    }

    override suspend fun sessions(query: String?): List<ConversationSummary> = apiCall {
        api.sessions(query = query?.trim()?.takeIf { it.isNotEmpty() }).map { item ->
            ConversationSummary(item.id, item.title, item.mode, item.subject, item.updatedAt)
        }
    }

    override suspend fun restoreSession(sessionId: String): List<ChatMessage> = apiCall {
        api.session(sessionId).messages.mapIndexed { index, message ->
            ChatMessage(
                id = message.id.hashCode().toLong() + index,
                author = if (message.role == "student") MessageAuthor.Student else MessageAuthor.Assistant,
                text = message.content,
                serverId = message.id,
                citations = message.citations.map { citation ->
                    MessageCitation(citation.nodeName, citation.sourceTitle, citation.sourceLocation, citation.excerpt)
                },
            )
        }
    }

    override suspend fun deleteSession(sessionId: String) = apiCall {
        val response = api.deleteSession(sessionId)
        if (!response.isSuccessful) throw ApiFailureException(response.code(), "删除会话失败")
    }

    override suspend fun learningItems(filter: LearningFilter): List<LearningItem> = apiCall {
        val summary = api.learningSummary()
        val source = when (filter) {
            LearningFilter.Recent -> summary.recent
            LearningFilter.Review -> summary.review
            LearningFilter.ErrorProne -> summary.errorProne
            LearningFilter.Verified -> summary.verified
        }
        source.map { item ->
            val status = item.status.toKnowledgeStatus()
            LearningItem(
                nodeId = item.id,
                title = item.name,
                status = status,
                lastStudied = item.updatedAt.replace('T', ' ').take(16),
                action = when (status) {
                    KnowledgeStatus.Verified -> "回到图谱"
                    KnowledgeStatus.ErrorProne -> "查看错因"
                    KnowledgeStatus.Unstable -> "开始复习"
                    else -> "继续探索"
                },
            )
        }
    }

    override suspend fun updateNodeState(
        nodeId: String,
        status: KnowledgeStatus,
        note: String?,
        favorite: Boolean?,
    ): LearningItem? = apiCall {
        val item = api.updateNodeState(
            nodeId,
            KnowledgeStateUpdate(status.apiValue(), note, favorite),
        )
        LearningItem(item.id, item.name, item.status.toKnowledgeStatus(), item.updatedAt, "继续探索")
    }

    override suspend fun recordLearningEvent(nodeId: String, eventType: String, data: Map<String, Any?>) = apiCall {
        api.recordLearningEvent(LearningEventRequest(eventType, nodeId, data))
        Unit
    }

    override suspend fun changePassword(current: String, next: String) = apiCall {
        api.changePassword(ChangePasswordRequest(current, next))
        Unit
    }

    override suspend fun deleteAccount() = apiCall {
        val response = api.deleteAccount()
        if (!response.isSuccessful) throw ApiFailureException(response.code(), "注销账户失败")
        clearLocalMistakes()
        tokenStore.clear()
    }

    override suspend fun sendQuestion(
        sessionId: String?,
        nodeId: String?,
        question: String,
        mode: QaMode,
        helpLevel: QaHelpLevel,
        onDelta: (String) -> Unit,
    ): QaAnswer = apiCall {
        val activeSessionId = sessionId ?: api.createSession(ConversationCreate(mode = mode.apiValue, knowledgeNodeId = nodeId)).id
        val body = MessageCreate(question, helpLevel.apiValue)
        val idempotencyKey = "android-${UUID.randomUUID()}"
        val streamResponse = api.streamMessage(activeSessionId, idempotencyKey, body)
        val result = if (streamResponse.code() == 404) {
            streamResponse.errorBody()?.close()
            api.sendMessage(activeSessionId, idempotencyKey, body).also { fallback ->
                fallback.assistantMessage.content.chunked(3).forEach { chunk ->
                    onDelta(chunk)
                    delay(18)
                }
            }
        } else {
            if (!streamResponse.isSuccessful) {
                throw responseFailure(streamResponse.code(), streamResponse.errorBody()?.charStream())
            }
            withContext(Dispatchers.IO) { parseStream(streamResponse.body(), onDelta) }
        }
        result.toQaAnswer()
    }

    override suspend fun clarifyQaIntent(question: String, mode: QaMode): QaClarification? = apiCall {
        val result = api.clarifyQaIntent(QaIntentRequest(question, mode.apiValue))
        if (!result.needsClarification) return@apiCall null
        QaClarification(
            originalQuestion = question,
            prompt = result.prompt ?: "你希望我怎样帮助你？",
            options = result.options.map { option ->
                QaClarificationOption(
                    id = option.id,
                    label = option.label,
                    instruction = option.instruction,
                    mode = QaMode.entries.firstOrNull { it.apiValue == option.mode } ?: mode,
                )
            },
        )
    }

    private suspend fun parseStream(body: okhttp3.ResponseBody?, onDelta: (String) -> Unit): cn.qingkui.app.data.remote.dto.QaResultDto {
        if (body == null) throw ApiFailureException(null, "流式响应为空")
        var event = ""
        var completed: cn.qingkui.app.data.remote.dto.QaResultDto? = null
        body.use { responseBody ->
            val source = responseBody.source()
            while (!source.exhausted()) {
                currentCoroutineContext().ensureActive()
                val line = source.readUtf8Line() ?: break
                currentCoroutineContext().ensureActive()
                when {
                    line.startsWith("event: ") -> event = line.removePrefix("event: ")
                    line.startsWith("data: ") -> {
                        val data = line.removePrefix("data: ")
                        when (event) {
                            "delta" -> {
                                val chunk = JsonParser.parseString(data).asJsonObject["content"]?.asString.orEmpty()
                                if (chunk.isNotEmpty()) onDelta(chunk)
                            }
                            "done" -> completed = gson.fromJson(data, cn.qingkui.app.data.remote.dto.QaResultDto::class.java)
                            "error" -> {
                                val payload = JsonParser.parseString(data).asJsonObject
                                throw ApiFailureException(
                                    payload["status"]?.asInt,
                                    payload["detail"]?.asString ?: "流式回答失败",
                                )
                            }
                        }
                    }
                }
            }
        }
        return completed ?: throw ApiFailureException(null, "流式回答意外中断，请重试")
    }

    private fun responseFailure(statusCode: Int, body: java.io.Reader?): ApiFailureException {
        val detail = runCatching { gson.fromJson(body, ApiErrorDto::class.java)?.detail }.getOrNull()
        return ApiFailureException(statusCode, detail ?: "服务请求失败 ($statusCode)")
    }

    private fun cn.qingkui.app.data.remote.dto.QaResultDto.toQaAnswer(): QaAnswer {
        val source = assistantMessage.citations
            .joinToString("；") { "${it.nodeName} · ${it.sourceTitle} ${it.sourceLocation}" }
            .ifBlank { null }
        return QaAnswer(
            conversationId = conversationId,
            message = ChatMessage(
                id = assistantMessage.id.hashCode().toLong(),
                author = MessageAuthor.Assistant,
                text = assistantMessage.content,
                source = source,
                serverId = assistantMessage.id,
                citations = assistantMessage.citations.map {
                    MessageCitation(it.nodeName, it.sourceTitle, it.sourceLocation, it.excerpt)
                },
            ),
            balance = balance,
            creditsCharged = creditsCharged,
            subject = subject,
        )
    }

    override suspend fun submitAnswerFeedback(messageId: String?, action: AnswerFeedbackAction, detail: String?) = apiCall {
        val (category, defaultContent) = when (action) {
            AnswerFeedbackAction.Helpful -> "other" to "该回答对本次学习有帮助"
            AnswerFeedbackAction.Unhelpful -> "answer_error" to "该回答没有解决我的问题"
            AnswerFeedbackAction.ContentError -> "answer_error" to "该回答可能存在内容错误，请人工审核"
            AnswerFeedbackAction.Review -> "review_request" to "将回答关联知识点加入待复习"
        }
        api.submitFeedback(
            FeedbackCreate(
                category = category,
                content = detail?.trim()?.takeIf { it.isNotEmpty() }?.let { "$defaultContent：$it" } ?: defaultContent,
                messageId = messageId,
            ),
        )
        Unit
    }

    override fun observeMistakeDrafts(): Flow<List<MistakeDraftItem>> = draftDao.observeAll().map { drafts ->
        drafts.map { draft ->
            MistakeDraftItem(
                id = draft.id,
                imagePath = draft.imagePath,
                subject = draft.subject,
                questionText = draft.questionText,
                errorCategory = draft.errorCategory,
                status = draft.status,
                errorMessage = draft.errorMessage,
            )
        }
    }

    override suspend fun mistakes(): List<MistakeItem> = apiCall {
        val response = api.mistakes()
        val remoteIds = response.mapTo(mutableSetOf()) { it.id }
        draftDao.all().filter { it.status == "uploaded" && it.remoteId in remoteIds }.forEach { draft ->
            if (draft.imagePath.isNotBlank()) File(draft.imagePath).delete()
            draftDao.delete(draft.id)
        }
        response.map { mistake ->
            val task = mistake.ocrTasks.lastOrNull()
            MistakeItem(
                id = mistake.id,
                subject = mistake.subject ?: "待识别",
                questionText = mistake.correctedText ?: task?.resultText ?: mistake.questionText ?: "图片题目识别中",
                ocrTaskId = task?.id,
                ocrStatus = task?.status ?: "manual",
                ocrErrorMessage = task?.errorMessage,
                confidence = task?.confidence,
                requiresReview = task?.requiresReview == true,
                reviewReasons = task?.reviewReasons.orEmpty(),
                errorCategory = mistake.errorCategory,
                errorNote = mistake.errorNote,
                analysisStatus = mistake.analysisStatus,
                analysisDiagnosis = mistake.analysis["diagnosis"] as? String,
                correctionSteps = (mistake.analysis["correction_steps"] as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty(),
                knowledgeNodeId = mistake.knowledgeNodeId,
                practices = mistake.practices.map { practice ->
                    MistakePracticeItem(
                        id = practice.id,
                        roundId = practice.roundId,
                        position = practice.position,
                        questionText = practice.questionText,
                        hint = practice.hint,
                        answerReference = practice.answerReference,
                        status = practice.status,
                        studentAnswer = practice.studentAnswer,
                        isCorrect = practice.isCorrect,
                        validationMethod = practice.validationDetails["method"] as? String,
                    )
                },
                studyStatus = mistake.studyStatus,
                reviewStage = mistake.reviewStage,
                nextReviewAt = mistake.nextReviewAt,
                secondAttemptCorrect = mistake.secondAttemptCorrect,
                reviewStreak = mistake.reviewStreak,
            )
        }
    }

    override suspend fun mistakeWeeklyReview(): MistakeWeeklyReview = apiCall {
        val report = api.mistakeWeeklyReview()
        MistakeWeeklyReview(
            weekStart = report.weekStart,
            weekEnd = report.weekEnd,
            newMistakes = report.newMistakes,
            dueReviewCount = report.dueReviews.size,
            topErrorCategory = report.errorCategories.maxByOrNull { it.value }?.key,
            weakKnowledgePoints = report.weakKnowledgePoints.map {
                cn.qingkui.app.ui.model.WeakKnowledgePointItem(
                    knowledgeNodeId = it.knowledgeNodeId,
                    name = it.name,
                    mistakeCount = it.mistakeCount,
                )
            },
            uploadSuccessRate = report.uploadSuccessRate,
            ocrCorrectionRate = report.ocrCorrectionRate,
            practiceCompletionRate = report.practiceCompletionRate,
            authoritativeAccuracy = report.authoritativeAccuracy,
            secondAttemptAccuracy = report.secondAttemptAccuracy,
            sevenDayFollowupRate = report.sevenDayFollowupRate,
            dueReviews = report.dueReviews.map {
                cn.qingkui.app.ui.model.WeeklyMistakeLinkItem(
                    mistakeId = it.mistakeId,
                    practiceRoundId = it.practiceRoundId,
                    knowledgeNodeId = it.knowledgeNodeId,
                    title = it.title,
                    reviewStage = it.reviewStage,
                    nextReviewAt = it.nextReviewAt,
                )
            },
        )
    }

    override suspend fun saveMistakeDraft(
        imagePath: String,
        subject: String,
        questionText: String,
        studentWork: String,
        questionGoal: String,
        errorCategory: String,
    ) {
        val id = UUID.randomUUID().toString()
        draftDao.upsert(
            MistakeDraftEntity(
                id = id,
                imagePath = imagePath,
                subject = subject,
                questionText = questionText,
                studentWork = studentWork,
                questionGoal = questionGoal,
                errorCategory = errorCategory,
                status = "waiting",
            ),
        )
        enqueueMistakeDraft(id, ExistingWorkPolicy.KEEP)
    }

    override suspend fun retryMistakeDraft(draftId: String) {
        val draft = draftDao.get(draftId) ?: throw ApiFailureException(404, "本地草稿不存在")
        mistakeDraftValidationError(draft.imagePath, draft.questionText)?.let { message ->
            throw ApiFailureException(null, message)
        }
        draftDao.updateStatus(draftId, "waiting", null)
        enqueueMistakeDraft(draftId, ExistingWorkPolicy.REPLACE)
    }

    override suspend fun deleteMistakeDraft(draftId: String) {
        WorkManager.getInstance(context).cancelUniqueWork("mistake-upload-$draftId")
        draftDao.get(draftId)?.imagePath?.takeIf { it.isNotBlank() }?.let { File(it).delete() }
        draftDao.delete(draftId)
    }

    override suspend fun cancelMistakeOcr(mistakeId: String, taskId: String) = apiCall {
        api.cancelMistakeOcr(mistakeId, taskId)
        Unit
    }

    override suspend fun retryMistakeOcr(mistakeId: String, taskId: String) = apiCall {
        api.retryMistakeOcr(mistakeId, taskId)
        Unit
    }

    override suspend fun deleteMistake(mistakeId: String) = apiCall {
        val response = api.deleteMistake(mistakeId)
        if (!response.isSuccessful) throw responseFailure(response.code(), response.errorBody()?.charStream())
    }

    private fun enqueueMistakeDraft(id: String, policy: ExistingWorkPolicy) {
        val request = OneTimeWorkRequestBuilder<MistakeUploadWorker>()
            .addTag(MISTAKE_UPLOAD_TAG)
            .setInputData(Data.Builder().putString(MistakeUploadWorker.KEY_DRAFT_ID, id).build())
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "mistake-upload-$id",
            policy,
            request,
        )
    }

    override suspend fun confirmMistakeOcr(
        mistakeId: String,
        taskId: String,
        correctedText: String,
    ) = apiCall {
        api.confirmMistakeOcr(mistakeId, taskId, OcrCorrectionDto(correctedText))
        Unit
    }

    override suspend fun analyzeMistake(mistakeId: String): MistakeAnalysisOutcome = apiCall {
        MistakeAnalysisOutcome(api.analyzeMistake(mistakeId).balance)
    }

    override suspend fun generateMistakePractice(mistakeId: String) = apiCall {
        api.generateMistakePractice(mistakeId)
        Unit
    }

    override suspend fun submitMistakePractice(mistakeId: String, practiceId: String, answer: String) = apiCall {
        api.submitMistakePractice(mistakeId, practiceId, PracticeSubmitDto(answer.trim()))
        Unit
    }

    private suspend fun clearLocalMistakes() {
        WorkManager.getInstance(context).cancelAllWorkByTag(MISTAKE_UPLOAD_TAG)
        draftDao.all().forEach { draft ->
            draft.imagePath.takeIf { it.isNotBlank() }?.let { File(it).delete() }
        }
        draftDao.deleteAll()
    }

    private companion object {
        const val MISTAKE_UPLOAD_TAG = "mistake-upload"
    }

    private suspend fun <T> apiCall(block: suspend () -> T): T = try {
        block()
    } catch (error: HttpException) {
        val detail = runCatching {
            gson.fromJson(error.response()?.errorBody()?.charStream(), ApiErrorDto::class.java)?.detail
        }.getOrNull()
        throw ApiFailureException(error.code(), detail ?: "服务请求失败 (${error.code()})")
    } catch (error: IOException) {
        throw ApiFailureException(null, "无法连接后端，请确认服务已启动且 API 地址可访问")
    }

    private fun KnowledgeNodeDto.toUi(x: Float, y: Float) = KnowledgeNode(
        id = id,
        title = name,
        subtitle = chapter,
        status = status.toKnowledgeStatus(),
        x = x,
        y = y,
        kind = KnowledgeKind.Concept,
        source = KnowledgeSource.Official,
        description = definition,
        evidence = "$subject · $grade · $chapter",
        saved = isFavorite,
    )

    private fun KnowledgeNodeDetailDto.toUiNode(x: Float, y: Float) = KnowledgeNode(
        id = id,
        title = name,
        subtitle = chapter,
        status = status.toKnowledgeStatus(),
        x = x,
        y = y,
        kind = KnowledgeKind.Concept,
        source = KnowledgeSource.Official,
        description = if (explanation.isBlank()) definition else explanation,
        evidence = "$subject · $grade · $chapter",
        saved = isFavorite,
        note = note.orEmpty(),
    )

    private fun NeighborNodeDto.toUi(x: Float, y: Float) = KnowledgeNode(
        id = id,
        title = name,
        subtitle = chapter,
        status = status.toKnowledgeStatus(),
        x = x,
        y = y,
        kind = KnowledgeKind.Concept,
        source = KnowledgeSource.Official,
        description = definition,
        evidence = "$subject · $grade · $chapter",
        saved = isFavorite,
    )

    private fun cn.qingkui.app.data.remote.dto.SchoolClassDto.toSchoolClassItem() = SchoolClassItem(
        id = id,
        schoolId = schoolId,
        name = name,
        grade = grade.orEmpty(),
        academicYear = academicYear,
    )

    private fun cn.qingkui.app.data.remote.dto.ContributionDto.toContributionItem() = ContributionItem(
        id = id,
        type = contributionType,
        title = title,
        status = status,
        reviewNote = reviewNote,
        rewardAmount = rewardAmount,
        rewardStatus = rewardStatus,
        createdAt = createdAt.displayDateTime(),
    )
}

private fun String.displayDateTime(): String = replace('T', ' ').take(16)

private fun KnowledgeStatus.apiValue(): String = when (this) {
    KnowledgeStatus.Unexplored -> "unexplored"
    KnowledgeStatus.Explored -> "explored"
    KnowledgeStatus.Understood -> "understood"
    KnowledgeStatus.Verified -> "verified"
    KnowledgeStatus.Unstable -> "unstable"
    KnowledgeStatus.ErrorProne -> "error_prone"
    KnowledgeStatus.ToExplore -> "to_explore"
}

private fun String.toKnowledgeStatus(): KnowledgeStatus = when (this) {
    "explored" -> KnowledgeStatus.Explored
    "understood" -> KnowledgeStatus.Understood
    "verified" -> KnowledgeStatus.Verified
    "unstable" -> KnowledgeStatus.Unstable
    "error_prone" -> KnowledgeStatus.ErrorProne
    "to_explore" -> KnowledgeStatus.ToExplore
    else -> KnowledgeStatus.Unexplored
}

private fun String.toRelationType(): RelationType = when (this) {
    "prerequisite" -> RelationType.Prerequisite
    "confused_with" -> RelationType.Confusable
    "question_type" -> RelationType.QuestionType
    "extension" -> RelationType.Extension
    else -> RelationType.Related
}

object AppRepositoryProvider {
    @Volatile private var instance: AppRepository? = null

    fun get(context: Context): AppRepository = instance ?: synchronized(this) {
        instance ?: TokenStore(context.applicationContext).let { tokenStore ->
            NetworkAppRepository(NetworkModule.create(tokenStore), tokenStore, context.applicationContext).also { instance = it }
        }
    }
}

private fun graphRingCapacity(ring: Int): Int = 8 + ring * 6

private fun graphRingPosition(index: Int): Pair<Int, Int> {
    var ring = 0
    var position = index
    while (position >= graphRingCapacity(ring)) {
        position -= graphRingCapacity(ring)
        ring++
    }
    return ring to position
}
