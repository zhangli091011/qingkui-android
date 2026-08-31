package cn.qingkui.app.ui

import android.content.Context
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import cn.qingkui.app.data.repository.ApiFailureException
import cn.qingkui.app.data.repository.AppRepository
import cn.qingkui.app.data.repository.AppRepositoryProvider
import cn.qingkui.app.ui.model.AppDestination
import cn.qingkui.app.ui.model.AnswerFeedbackAction
import cn.qingkui.app.ui.model.AppUiState
import cn.qingkui.app.ui.model.AuthMode
import cn.qingkui.app.ui.model.ChatMessage
import cn.qingkui.app.ui.model.MessageAuthor
import cn.qingkui.app.ui.model.QaHelpLevel
import cn.qingkui.app.ui.model.QaMode
import cn.qingkui.app.ui.model.QaClarificationOption
import cn.qingkui.app.ui.model.KnowledgeStatus
import cn.qingkui.app.ui.model.KnowledgeCatalogScope
import cn.qingkui.app.ui.model.LearningFilter
import cn.qingkui.app.ui.model.MistakeItem
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch

class AppViewModel(
    private val repository: AppRepository,
    private val mistakePollIntervalMillis: Long = 3_000,
    private val mistakePollMaxAttempts: Int = 40,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {
    private val _uiState = MutableStateFlow(restoredUiState(savedStateHandle))
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()
    private var mistakePollingJob: Job? = null
    private var qaJob: Job? = null
    private var qaAssistantMessageId: Long? = null
    private val noteSaveJobs = mutableMapOf<String, Job>()
    private var lastLocalMessageId: Long = 0

    init {
        viewModelScope.launch {
            _uiState.collectLatest { state -> persistUiState(savedStateHandle, state) }
        }
        viewModelScope.launch {
            repository.observeMistakeDrafts().collectLatest { drafts ->
                _uiState.update { it.copy(mistakeDrafts = drafts) }
                val state = _uiState.value
                if (
                    drafts.any { it.status == "uploaded" } &&
                    state.authenticated &&
                    state.destination == AppDestination.Learning &&
                    state.learningShowsMistakes
                ) {
                    refreshMistakes()
                }
            }
        }
        viewModelScope.launch {
            val hasSession = repository.hasSession()
            val consentRequired = if (hasSession) {
                runCatching { repository.privacyConsentRequired() }.getOrDefault(false)
            } else false
            _uiState.update {
                it.copy(
                    authChecking = false,
                    authenticated = hasSession,
                    privacyConsentRequired = consentRequired,
                    currentUserName = repository.nickname() ?: it.currentUserName,
                )
            }
            if (hasSession && !consentRequired) {
                refreshContent()
                restorePersistedConversation()
            }
        }
    }

    private fun restorePersistedConversation() {
        val conversationId = _uiState.value.conversationId ?: return
        viewModelScope.launch {
            runCatching { repository.restoreSession(conversationId) }
                .onSuccess { messages ->
                    _uiState.update { state ->
                        if (state.conversationId == conversationId) state.copy(messages = messages) else state
                    }
                }
                .onFailure {
                    _uiState.update { state ->
                        if (state.conversationId == conversationId) state.copy(conversationId = null, messages = emptyList()) else state
                    }
                }
        }
    }

    fun setAuthMode(mode: AuthMode) = _uiState.update { it.copy(authMode = mode, errorMessage = null) }
    fun updateUsername(value: String) = _uiState.update { it.copy(username = value, errorMessage = null) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun updateNickname(value: String) = _uiState.update { it.copy(nickname = value, errorMessage = null) }
    fun updateEmail(value: String) = _uiState.update { it.copy(email = value, errorMessage = null) }

    fun submitAuth(privacyAccepted: Boolean = false) {
        val state = _uiState.value
        if (state.username.trim().length < 3 || state.password.length < 8) {
            _uiState.update { it.copy(errorMessage = "用户名至少 3 位，密码至少 8 位") }
            return
        }
        if (state.authMode == AuthMode.Register && !privacyAccepted) {
            _uiState.update { it.copy(errorMessage = "请先阅读并同意隐私说明") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, errorMessage = null) }
            try {
                val name = if (state.authMode == AuthMode.Login) {
                    repository.login(state.username, state.password)
                } else {
                    repository.register(state.username, state.password, state.nickname, state.email)
                }
                val consentRequired = repository.privacyConsentRequired()
                _uiState.update {
                    it.copy(
                        authenticated = true,
                        privacyConsentRequired = consentRequired,
                        authScreenOpen = false,
                        authLoading = false,
                        password = "",
                        email = "",
                        currentUserName = name,
                        pendingSendAfterAuth = if (consentRequired) state.pendingSendAfterAuth else false,
                    )
                }
                if (!consentRequired) {
                    refreshContentNow()
                    if (state.pendingSendAfterAuth) sendMessage()
                }
            } catch (error: Exception) {
                _uiState.update { it.copy(authLoading = false, errorMessage = error.userMessage()) }
            }
        }
    }

    fun acceptPrivacyConsent() {
        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, errorMessage = null) }
            try {
                repository.acceptPrivacyConsent()
                val shouldSend = _uiState.value.pendingSendAfterAuth
                _uiState.update { it.copy(privacyConsentRequired = false, authLoading = false) }
                refreshContentNow()
                if (shouldSend) sendMessage()
            } catch (error: Exception) {
                _uiState.update { it.copy(authLoading = false, errorMessage = error.userMessage()) }
            }
        }
    }

    fun refreshContent() {
        viewModelScope.launch {
            refreshContentNow()
        }
    }

    private suspend fun refreshContentNow() {
        _uiState.update { it.copy(contentLoading = true, errorMessage = null) }
        try {
            val (credits, graph, learning, sessions, ledger, mistakes) = coroutineScope {
                val creditTask = async { repository.credits() }
                val graphTask = async {
                    _uiState.value.selectedNodeId?.let { selectedNodeId ->
                        runCatching { repository.graph(selectedNodeId) }.getOrNull()
                    } ?: repository.graph()
                }
                val learningTask = async { repository.learningItems(_uiState.value.learningFilter) }
                val sessionsTask = async {
                    repository.sessions(_uiState.value.sessionSearchQuery.trim().takeIf { it.isNotEmpty() })
                }
                val ledgerTask = async { repository.creditLedger() }
                val mistakesTask = async { repository.mistakes() }
                Sextuple(creditTask.await(), graphTask.await(), learningTask.await(), sessionsTask.await(), ledgerTask.await(), mistakesTask.await())
            }
            val (catalog, selectedDetail) = coroutineScope {
                val catalogTask = async { repository.knowledgeCatalog() }
                val detailTask = async {
                    graph.selectedNodeId?.let { nodeId -> runCatching { repository.nodeDetail(nodeId) }.getOrNull() }
                }
                catalogTask.await() to detailTask.await()
            }
            val restoredScope = _uiState.value.selectedKnowledgeScope
            val scope = catalog.firstOrNull {
                restoredScope != null &&
                    it.subject == restoredScope.subject &&
                    it.grade == restoredScope.grade &&
                    it.textbookVersion == restoredScope.textbookVersion
            } ?: catalog.firstOrNull { it.subject == _uiState.value.currentSubject } ?: catalog.firstOrNull()
            val chapters = if (scope != null) repository.knowledgeTree(scope) else emptyList()
            _uiState.update {
                it.copy(
                    credits = credits,
                    graphNodes = graph.nodes.map { node ->
                        if (selectedDetail != null && node.id == selectedDetail.id) selectedDetail else node
                    },
                    graphRelations = graph.relations,
                    knowledgeCatalog = catalog,
                    selectedKnowledgeScope = scope,
                    knowledgeChapters = chapters,
                    selectedNodeId = graph.selectedNodeId,
                    selectedNodeDetail = selectedDetail,
                    noteDraft = selectedDetail?.note.orEmpty(),
                    currentSubject = scope?.subject
                        ?: graph.nodes.firstOrNull()?.evidence?.substringBefore(" · ")
                        ?: it.currentSubject,
                    learningItems = learning,
                    sessions = sessions,
                    ledger = ledger,
                    mistakes = mistakes,
                    contentLoading = false,
                )
            }
            refreshAccountData()
        } catch (error: Exception) {
            handleApiError(error) { it.copy(contentLoading = false) }
        }
    }

    fun refreshAccount() {
        if (!_uiState.value.authenticated) return
        viewModelScope.launch { refreshAccountData() }
    }

    private suspend fun refreshAccountData() {
        val (deviceSessions, feedbackItems) = coroutineScope {
            val devicesTask = async {
                try {
                    repository.deviceSessions()
                } catch (_: Exception) {
                    emptyList()
                }
            }
            val feedbackTask = async {
                try {
                    repository.feedback()
                } catch (_: Exception) {
                    emptyList()
                }
            }
            devicesTask.await() to feedbackTask.await()
        }
        _uiState.update { it.copy(deviceSessions = deviceSessions, feedbackItems = feedbackItems) }
        refreshCommunityData()
    }

    private suspend fun refreshCommunityData() {
        _uiState.update { it.copy(communityLoading = true) }
        val organizations = runCatching { repository.schoolMemberships() }
        val memberships = organizations.getOrDefault(emptyList())
        val schoolId = memberships.firstOrNull()?.schoolId
        val classes = if (schoolId != null) runCatching { repository.schoolClasses(schoolId) } else Result.success(emptyList())
        val campaigns = runCatching { repository.creditCampaigns() }
        val redemptions = if (campaigns.isSuccess) runCatching { repository.creditRedemptions() } else Result.success(emptyList())
        val contributions = runCatching { repository.contributions() }
        _uiState.update { state ->
            state.copy(
                organizationsAvailable = featureAvailability(organizations) ?: state.organizationsAvailable,
                schoolMemberships = memberships,
                schoolClasses = classes.getOrDefault(emptyList()),
                creditCampaignsAvailable = featureAvailability(campaigns) ?: state.creditCampaignsAvailable,
                creditCampaigns = campaigns.getOrDefault(emptyList()),
                creditRedemptions = redemptions.getOrDefault(emptyList()),
                contributionsAvailable = featureAvailability(contributions) ?: state.contributionsAvailable,
                contributions = contributions.getOrDefault(emptyList()),
                classOverview = null,
                communityLoading = false,
            )
        }
    }

    fun refreshCommunity() {
        if (!_uiState.value.authenticated) return
        viewModelScope.launch { refreshCommunityData() }
    }

    fun redeemOrganizationInvite(code: String) {
        if (code.trim().length < 8) {
            _uiState.update { it.copy(errorMessage = "请输入有效的邀请码") }
            return
        }
        viewModelScope.launch {
            try {
                repository.redeemOrganizationInvite(code)
                refreshCommunityData()
                _uiState.update { it.copy(errorMessage = "已加入学校或班级") }
            } catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun leaveSchool(schoolId: String) {
        viewModelScope.launch {
            try {
                repository.leaveSchool(schoolId)
                refreshCommunityData()
                _uiState.update { it.copy(errorMessage = "已退出学校组织") }
            } catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun loadClassOverview(classId: String) {
        viewModelScope.launch {
            try {
                val overview = repository.classOverview(classId)
                _uiState.update { it.copy(classOverview = overview) }
            } catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun redeemCreditCode(code: String) {
        if (code.trim().length < 8) {
            _uiState.update { it.copy(errorMessage = "请输入有效的兑换码") }
            return
        }
        viewModelScope.launch {
            try {
                val result = repository.redeemCreditCode(code)
                _uiState.update { it.copy(credits = result.balance, errorMessage = "已兑换 ${result.amount} 额度") }
                refreshCommunityData()
            } catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun submitContribution(type: String, title: String, content: String, sourceReference: String?) {
        if (title.trim().length < 2 || content.trim().length < 20) {
            _uiState.update { it.copy(errorMessage = "标题至少 2 个字，内容至少 20 个字") }
            return
        }
        viewModelScope.launch {
            try {
                repository.submitContribution(type, title, content, sourceReference)
                refreshCommunityData()
                _uiState.update { it.copy(errorMessage = "投稿已提交，等待审核") }
            } catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun deleteContribution(contributionId: String) {
        viewModelScope.launch {
            try {
                repository.deleteContribution(contributionId)
                refreshCommunityData()
            } catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun selectDestination(destination: AppDestination) {
        _uiState.update { it.copy(destination = destination, drawerOpen = false) }
        reconcileMistakePolling(_uiState.value.mistakes)
    }

    fun showLearningRecords() {
        _uiState.update { it.copy(learningShowsMistakes = false) }
        stopMistakePolling()
    }

    fun selectLearningFilter(filter: LearningFilter) {
        if (_uiState.value.learningFilter == filter) return
        _uiState.update { it.copy(learningFilter = filter, contentLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val items = repository.learningItems(filter)
                _uiState.update { state ->
                    if (state.learningFilter == filter) {
                        state.copy(learningItems = items, contentLoading = false)
                    } else {
                        state
                    }
                }
            } catch (error: Exception) {
                handleApiError(error) { it.copy(contentLoading = false) }
            }
        }
    }
    fun showMistakeBook() {
        _uiState.update { it.copy(learningShowsMistakes = true) }
        refreshMistakes()
    }

    fun openMistakeCapture() {
        if (!_uiState.value.authenticated) {
            openAuth()
            return
        }
        _uiState.update { it.copy(mistakeCaptureOpen = true, errorMessage = null) }
    }

    fun closeMistakeCapture() = _uiState.update { it.copy(mistakeCaptureOpen = false) }

    fun saveMistakeDraft(
        imagePath: String,
        subject: String,
        questionText: String,
        studentWork: String,
        questionGoal: String,
        errorCategory: String,
    ) {
        viewModelScope.launch {
            try {
                repository.saveMistakeDraft(imagePath, subject, questionText, studentWork, questionGoal, errorCategory)
                _uiState.update {
                    it.copy(
                        mistakeCaptureOpen = false,
                        destination = AppDestination.Learning,
                        learningShowsMistakes = true,
                        errorMessage = if (imagePath.isBlank()) {
                            "手动题目已保存，将在网络可用时同步"
                        } else {
                            "图片已保存，将在网络可用时上传识别"
                        },
                    )
                }
            } catch (error: Exception) {
                handleApiError(error) { it.copy(mistakeCaptureOpen = true) }
            }
        }
    }

    fun refreshMistakes() {
        if (!_uiState.value.authenticated) return
        viewModelScope.launch {
            _uiState.update { it.copy(mistakeLoading = true) }
            try {
                val (items, weeklyReview) = loadMistakeSnapshot()
                _uiState.update {
                    it.copy(mistakes = items, mistakeWeeklyReview = weeklyReview, mistakeLoading = false)
                }
                reconcileMistakePolling(items)
            } catch (error: Exception) {
                handleApiError(error) { it.copy(mistakeLoading = false) }
            }
        }
    }

    private suspend fun loadMistakeSnapshot(): Pair<List<cn.qingkui.app.ui.model.MistakeItem>, cn.qingkui.app.ui.model.MistakeWeeklyReview> =
        coroutineScope {
            val itemsRequest = async { repository.mistakes() }
            val reviewRequest = async { repository.mistakeWeeklyReview() }
            itemsRequest.await() to reviewRequest.await()
        }

    private fun reconcileMistakePolling(items: List<cn.qingkui.app.ui.model.MistakeItem>) {
        val state = _uiState.value
        val visible = state.authenticated &&
            state.destination == AppDestination.Learning &&
            state.learningShowsMistakes
        if (!visible || items.none { it.ocrStatus in OCR_PENDING_STATUSES }) {
            stopMistakePolling()
            return
        }
        if (mistakePollingJob?.isActive == true) return
        mistakePollingJob = viewModelScope.launch {
            try {
                repeat(mistakePollMaxAttempts) {
                    delay(mistakePollIntervalMillis)
                    val current = _uiState.value
                    if (
                        !current.authenticated ||
                        current.destination != AppDestination.Learning ||
                        !current.learningShowsMistakes
                    ) return@launch
                    val refreshed = try {
                        repository.mistakes()
                    } catch (error: Exception) {
                        if (error is ApiFailureException && error.statusCode == 401) {
                            handleApiError(error) { it.copy(mistakeLoading = false) }
                            return@launch
                        }
                        return@repeat
                    }
                    _uiState.update { it.copy(mistakes = refreshed, mistakeLoading = false) }
                    if (refreshed.none { it.ocrStatus in OCR_PENDING_STATUSES }) return@launch
                }
            } finally {
                if (mistakePollingJob === currentCoroutineContext()[Job]) {
                    mistakePollingJob = null
                }
            }
        }
    }

    private fun stopMistakePolling() {
        mistakePollingJob?.cancel()
        mistakePollingJob = null
    }

    fun confirmMistakeOcr(mistakeId: String, taskId: String, correctedText: String) {
        if (correctedText.isBlank()) return
        viewModelScope.launch {
            try {
                repository.confirmMistakeOcr(mistakeId, taskId, correctedText.trim())
                refreshMistakes()
            } catch (error: Exception) {
                handleApiError(error) { it }
            }
        }
    }

    fun retryMistakeDraft(draftId: String) {
        viewModelScope.launch {
            try {
                repository.retryMistakeDraft(draftId)
            } catch (error: Exception) {
                handleApiError(error) { it }
            }
        }
    }

    fun deleteMistakeDraft(draftId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMistakeDraft(draftId)
            } catch (error: Exception) {
                handleApiError(error) { it }
            }
        }
    }

    fun cancelMistakeOcr(mistakeId: String, taskId: String) {
        viewModelScope.launch {
            try {
                repository.cancelMistakeOcr(mistakeId, taskId)
                refreshMistakes()
            } catch (error: Exception) {
                handleApiError(error) { it }
            }
        }
    }

    fun retryMistakeOcr(mistakeId: String, taskId: String) {
        viewModelScope.launch {
            try {
                repository.retryMistakeOcr(mistakeId, taskId)
                refreshMistakes()
            } catch (error: Exception) {
                handleApiError(error) { it }
            }
        }
    }

    fun deleteMistake(mistakeId: String) {
        viewModelScope.launch {
            try {
                repository.deleteMistake(mistakeId)
                val (items, weeklyReview) = loadMistakeSnapshot()
                _uiState.update { state -> state.copy(mistakes = items, mistakeWeeklyReview = weeklyReview) }
            } catch (error: Exception) {
                handleApiError(error) { it }
            }
        }
    }

    fun analyzeMistake(mistakeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(mistakeLoading = true, errorMessage = null) }
            try {
                val outcome = repository.analyzeMistake(mistakeId)
                val (items, weeklyReview) = loadMistakeSnapshot()
                _uiState.update {
                    it.copy(credits = outcome.balance, mistakes = items, mistakeWeeklyReview = weeklyReview, mistakeLoading = false)
                }
            } catch (error: Exception) {
                handleApiError(error) { it.copy(mistakeLoading = false) }
            }
        }
    }

    fun generateMistakePractice(mistakeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(mistakeLoading = true, errorMessage = null) }
            try {
                repository.generateMistakePractice(mistakeId)
                val (items, weeklyReview) = loadMistakeSnapshot()
                _uiState.update { it.copy(mistakes = items, mistakeWeeklyReview = weeklyReview, mistakeLoading = false) }
            } catch (error: Exception) {
                handleApiError(error) { it.copy(mistakeLoading = false) }
            }
        }
    }

    fun submitMistakePractice(mistakeId: String, practiceId: String, answer: String) {
        if (answer.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(mistakeLoading = true, errorMessage = null) }
            try {
                repository.submitMistakePractice(mistakeId, practiceId, answer)
                val (items, weeklyReview) = loadMistakeSnapshot()
                _uiState.update { it.copy(mistakes = items, mistakeWeeklyReview = weeklyReview, mistakeLoading = false) }
            } catch (error: Exception) {
                handleApiError(error) { it.copy(mistakeLoading = false) }
            }
        }
    }

    fun updateDraft(value: String) = _uiState.update { it.copy(draft = value, errorMessage = null) }

    fun askAboutMistake(item: MistakeItem) {
        _uiState.update {
            it.copy(
                destination = AppDestination.Chat,
                qaMode = QaMode.Error,
                conversationId = null,
                messages = emptyList(),
                draft = "请帮我复盘这道错题，先指出关键错因，再逐步提示我重新完成：\n${item.questionText}",
                selectedNodeId = item.knowledgeNodeId,
                errorMessage = null,
            )
        }
    }
    fun selectHelpLevel(value: QaHelpLevel) = _uiState.update { it.copy(helpLevel = value, errorMessage = null) }
    fun selectQaMode(value: QaMode) = _uiState.update { it.copy(qaMode = value, errorMessage = null, conversationId = null, messages = emptyList()) }

    fun sendMessage() {
        val state = _uiState.value
        val question = state.draft.trim()
        if (question.isEmpty() || state.sending) return
        if (!state.authenticated) {
            _uiState.update {
                it.copy(authScreenOpen = true, pendingSendAfterAuth = true, errorMessage = null)
            }
            return
        }
        if (state.credits < state.helpLevel.creditCost) {
            _uiState.update { it.copy(errorMessage = "额度不足，当前回答需要 ${state.helpLevel.creditCost} 额度") }
            return
        }
        launchQuestion(question, question, state.qaMode, checkIntent = true)
    }

    fun selectQaClarification(option: QaClarificationOption) {
        val clarification = _uiState.value.qaClarification ?: return
        val refined = "${clarification.originalQuestion}\n\n回答目标：${option.instruction}。"
        launchQuestion(refined, clarification.originalQuestion, option.mode, checkIntent = false)
    }

    fun dismissQaClarification() = _uiState.update { it.copy(qaClarification = null) }

    fun stopGenerating() {
        val stoppedAssistantId = qaAssistantMessageId
        qaJob?.cancel()
        qaJob = null
        qaAssistantMessageId = null
        _uiState.update { state ->
            state.copy(
                sending = false,
                messages = state.messages.map { message ->
                    if (message.id == stoppedAssistantId) {
                        message.copy(text = if (message.text.isBlank()) "已停止生成" else "${message.text}\n\n已停止生成")
                    } else message
                },
            )
        }
    }

    private fun launchQuestion(
        question: String,
        displayQuestion: String,
        mode: QaMode,
        checkIntent: Boolean,
    ) {
        val state = _uiState.value
        _uiState.update { it.copy(sending = true, qaClarification = null, errorMessage = null) }
        qaJob = viewModelScope.launch {
            var messageId: Long? = null
            var assistantMessageId: Long? = null
            try {
                if (checkIntent) {
                    val clarification = repository.clarifyQaIntent(question, mode)
                    if (clarification != null) {
                        _uiState.update { it.copy(sending = false, qaClarification = clarification) }
                        return@launch
                    }
                }
                messageId = maxOf(System.currentTimeMillis(), lastLocalMessageId + 1)
                assistantMessageId = messageId + 1
                lastLocalMessageId = assistantMessageId
                qaAssistantMessageId = assistantMessageId
                _uiState.update {
                    it.copy(
                        draft = "",
                        qaMode = mode,
                        messages = it.messages + listOf(
                            ChatMessage(messageId, MessageAuthor.Student, displayQuestion),
                            ChatMessage(assistantMessageId, MessageAuthor.Assistant, ""),
                        ),
                    )
                }
                val result = repository.sendQuestion(
                    state.conversationId,
                    state.selectedNodeId,
                    question,
                    mode,
                    state.helpLevel,
                ) { chunk ->
                    _uiState.update { current ->
                        current.copy(
                            messages = current.messages.map { message ->
                                if (message.id == assistantMessageId) message.copy(text = message.text + chunk) else message
                            },
                        )
                    }
                }
                _uiState.update {
                    it.copy(
                        messages = it.messages.map { message ->
                            if (message.id == assistantMessageId) result.message else message
                        },
                        conversationId = result.conversationId,
                        currentSubject = result.subject ?: it.currentSubject,
                        credits = result.balance,
                        sending = false,
                    )
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                handleApiError(error) {
                    it.copy(
                        sending = false,
                        draft = displayQuestion,
                        messages = it.messages.filterNot { message ->
                            message.id == messageId || message.id == assistantMessageId
                        },
                    )
                }
            } finally {
                val currentJob = currentCoroutineContext()[Job]
                if (qaJob === currentJob) qaJob = null
                if (qaAssistantMessageId == assistantMessageId) qaAssistantMessageId = null
            }
        }
    }

    fun selectNode(nodeId: String) {
        _uiState.update {
            it.copy(
                selectedNodeId = nodeId,
                selectedNodeDetail = null,
                noteDraft = "",
                conversationId = null,
                understandingCheck = null,
            )
        }
        viewModelScope.launch {
            runCatching { repository.graph(nodeId) }
                .onSuccess { graph -> _uiState.update { it.copy(graphNodes = graph.nodes, graphRelations = graph.relations, selectedNodeId = nodeId) } }
                .onFailure { handleApiError(it as? Exception ?: Exception(it)) { state -> state } }
            runCatching { repository.nodeDetail(nodeId) }
                .onSuccess { detail ->
                    _uiState.update {
                        it.copy(
                            selectedNodeDetail = detail,
                            noteDraft = detail.note,
                            graphNodes = it.graphNodes.map { node -> if (node.id == nodeId) detail else node },
                        )
                    }
                }
                .onFailure { handleApiError(it as? Exception ?: Exception(it)) { state -> state } }
            runCatching { repository.recordLearningEvent(nodeId, "viewed_node") }
        }
    }

    fun markNodeStatus(status: KnowledgeStatus) {
        val nodeId = _uiState.value.selectedNodeId ?: return
        viewModelScope.launch {
            try {
                repository.updateNodeState(nodeId, status, _uiState.value.noteDraft, null)
                _uiState.update { state ->
                    state.copy(
                        graphNodes = state.graphNodes.map { if (it.id == nodeId) it.copy(status = status) else it },
                        selectedNodeDetail = state.selectedNodeDetail?.let { if (it.id == nodeId) it.copy(status = status) else it },
                        learningItems = state.learningItems.map { if (it.nodeId == nodeId) it.copy(status = status) else it },
                    )
                }
            } catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun startUnderstandingCheck() {
        if (!_uiState.value.authenticated) {
            openAuth()
            return
        }
        val nodeId = _uiState.value.selectedNodeId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(contentLoading = true, errorMessage = null) }
            try {
                val check = repository.startUnderstandingCheck(nodeId)
                _uiState.update { it.copy(understandingCheck = check, contentLoading = false) }
            } catch (error: Exception) {
                handleApiError(error) { it.copy(contentLoading = false) }
            }
        }
    }

    fun submitUnderstandingCheck(choiceId: String) {
        val check = _uiState.value.understandingCheck ?: return
        if (_uiState.value.understandingCheckSubmitting) return
        viewModelScope.launch {
            _uiState.update { it.copy(understandingCheckSubmitting = true, errorMessage = null) }
            try {
                val outcome = repository.submitUnderstandingCheck(check.id, choiceId)
                _uiState.update { state ->
                    state.copy(
                        graphNodes = state.graphNodes.map { node ->
                            if (node.id == check.nodeId) node.copy(status = outcome.status) else node
                        },
                        selectedNodeDetail = state.selectedNodeDetail?.let { node ->
                            if (node.id == check.nodeId) node.copy(status = outcome.status) else node
                        },
                        learningItems = state.learningItems.map { item ->
                            if (item.nodeId == check.nodeId) item.copy(status = outcome.status) else item
                        },
                        understandingCheck = null,
                        understandingCheckSubmitting = false,
                        errorMessage = if (outcome.passed) "理解检查通过，已标记为已验证" else "本次未通过，已加入待复习",
                    )
                }
            } catch (error: Exception) {
                handleApiError(error) { it.copy(understandingCheckSubmitting = false) }
            }
        }
    }

    fun dismissUnderstandingCheck() {
        if (!_uiState.value.understandingCheckSubmitting) {
            _uiState.update { it.copy(understandingCheck = null) }
        }
    }

    fun toggleFavorite() {
        val nodeId = _uiState.value.selectedNodeId ?: return
        val node = _uiState.value.selectedNodeDetail
            ?.takeIf { it.id == nodeId }
            ?: _uiState.value.graphNodes.firstOrNull { it.id == nodeId }
            ?: return
        viewModelScope.launch {
            try {
                repository.updateNodeState(nodeId, node.status, _uiState.value.noteDraft, !node.saved)
                _uiState.update { state ->
                    state.copy(
                        graphNodes = state.graphNodes.map { if (it.id == nodeId) it.copy(saved = !node.saved) else it },
                        selectedNodeDetail = state.selectedNodeDetail?.let {
                            if (it.id == nodeId) it.copy(saved = !node.saved) else it
                        },
                    )
                }
            } catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun updateNote(value: String) {
        val nodeId = _uiState.value.selectedNodeId
        _uiState.update {
            it.copy(
                noteDraft = value,
                selectedNodeDetail = it.selectedNodeDetail?.let { node ->
                    if (node.id == nodeId) node.copy(note = value) else node
                },
                graphNodes = it.graphNodes.map { node -> if (node.id == nodeId) node.copy(note = value) else node },
            )
        }
        if (nodeId != null) {
            val previous = noteSaveJobs[nodeId]
            val job = viewModelScope.launch {
                previous?.cancelAndJoin()
                delay(450)
                val status = _uiState.value.graphNodes.firstOrNull { it.id == nodeId }?.status
                    ?: KnowledgeStatus.Explored
                try {
                    repository.updateNodeState(nodeId, status, value, null)
                } catch (cancelled: CancellationException) {
                    throw cancelled
                } catch (error: Exception) {
                    if (_uiState.value.selectedNodeId == nodeId) {
                        handleApiError(error) { it }
                    }
                } finally {
                    if (noteSaveJobs[nodeId] === currentCoroutineContext()[Job]) {
                        noteSaveJobs.remove(nodeId)
                    }
                }
            }
            noteSaveJobs[nodeId] = job
        }
    }

    fun searchGraph(query: String) {
        if (query.isBlank()) { refreshContent(); return }
        viewModelScope.launch {
            runCatching { repository.search(query.trim()) }
                .onSuccess { graph -> _uiState.update { it.copy(graphNodes = graph.nodes, graphRelations = graph.relations, selectedNodeId = graph.selectedNodeId) } }
                .onFailure { handleApiError(it as? Exception ?: Exception(it)) { state -> state } }
        }
    }

    fun selectKnowledgeScope(scope: KnowledgeCatalogScope) {
        _uiState.update { it.copy(selectedKnowledgeScope = scope, currentSubject = scope.subject, contentLoading = true) }
        viewModelScope.launch {
            try {
                val chapters = repository.knowledgeTree(scope)
                _uiState.update { state ->
                    if (state.selectedKnowledgeScope == scope) state.copy(knowledgeChapters = chapters, contentLoading = false) else state
                }
            } catch (error: Exception) { handleApiError(error) { it.copy(contentLoading = false) } }
        }
    }

    fun restoreSession(sessionId: String) {
        viewModelScope.launch {
            try {
                val restored = repository.restoreSession(sessionId)
                _uiState.update { it.copy(conversationId = sessionId, messages = restored, destination = AppDestination.Chat, drawerOpen = false) }
            } catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                repository.deleteSession(sessionId)
                _uiState.update { state -> state.copy(sessions = state.sessions.filterNot { it.id == sessionId }, conversationId = if (state.conversationId == sessionId) null else state.conversationId, messages = if (state.conversationId == sessionId) emptyList() else state.messages) }
            } catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun updateSessionSearch(value: String) {
        _uiState.update { it.copy(sessionSearchQuery = value.take(120), errorMessage = null) }
    }

    fun searchSessions(query: String = _uiState.value.sessionSearchQuery) {
        if (!_uiState.value.authenticated) return
        val normalized = query.trim().takeIf { it.isNotEmpty() }
        _uiState.update { it.copy(sessionSearchQuery = query.take(120), contentLoading = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val sessions = repository.sessions(normalized)
                _uiState.update { it.copy(sessions = sessions, contentLoading = false) }
            } catch (error: Exception) {
                handleApiError(error) { it.copy(contentLoading = false) }
            }
        }
    }

    fun changePassword(current: String, next: String) {
        viewModelScope.launch {
            try { repository.changePassword(current, next); repository.logout(); _uiState.value = AppUiState(authChecking = false, destination = AppDestination.Account, errorMessage = "密码已修改，请重新登录") }
            catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun requestPasswordReset(email: String) {
        if (!email.contains('@')) { _uiState.update { it.copy(errorMessage = "请输入有效邮箱") }; return }
        viewModelScope.launch {
            try { repository.requestPasswordReset(email); _uiState.update { it.copy(errorMessage = "如果邮箱已绑定，重置邮件将很快发送") } }
            catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun confirmPasswordReset(token: String, newPassword: String) {
        if (token.length < 32 || newPassword.length < 8) { _uiState.update { it.copy(errorMessage = "令牌或新密码格式不正确") }; return }
        viewModelScope.launch {
            try { repository.confirmPasswordReset(token, newPassword); _uiState.update { it.copy(errorMessage = "密码已重置，请登录") } }
            catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            try { repository.deleteAccount(); _uiState.value = AppUiState(authChecking = false, destination = AppDestination.Account, errorMessage = "账户已注销") }
            catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun revokeDeviceSession(sessionId: String) {
        viewModelScope.launch {
            try {
                repository.revokeDeviceSession(sessionId)
                _uiState.update { state ->
                    state.copy(deviceSessions = state.deviceSessions.filterNot { it.id == sessionId })
                }
            } catch (error: Exception) {
                handleApiError(error) { it }
            }
        }
    }

    fun submitFeedback(category: String, content: String) {
        if (content.trim().length < 2) {
            _uiState.update { it.copy(errorMessage = "请填写反馈内容") }
            return
        }
        viewModelScope.launch {
            try {
                repository.submitFeedback(category, content)
                refreshAccountData()
                _uiState.update { it.copy(errorMessage = "反馈已提交") }
            } catch (error: Exception) {
                handleApiError(error) { it }
            }
        }
    }

    fun askAboutNode(nodeId: String) {
        val node = _uiState.value.graphNodes.firstOrNull { it.id == nodeId } ?: return
        updateDraft("请帮我理解${node.title}")
        selectDestination(AppDestination.Chat)
    }

    fun setDrawerOpen(open: Boolean) = _uiState.update { it.copy(drawerOpen = open) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun openAuth() = _uiState.update {
        it.copy(authScreenOpen = true, drawerOpen = false, pendingSendAfterAuth = false, errorMessage = null)
    }
    fun closeAuth() = _uiState.update {
        it.copy(authScreenOpen = false, pendingSendAfterAuth = false, errorMessage = null)
    }

    fun retryAnswer(messageId: Long) {
        val messages = _uiState.value.messages
        val assistantIndex = messages.indexOfFirst { it.id == messageId }
        if (assistantIndex <= 0) return
        val question = messages.take(assistantIndex).lastOrNull { it.author == MessageAuthor.Student }?.text ?: return
        _uiState.update { it.copy(draft = question, errorMessage = null) }
    }

    fun submitAnswerFeedback(messageId: Long, action: AnswerFeedbackAction) {
        submitAnswerFeedback(messageId, action, null)
    }

    fun submitContentErrorFeedback(messageId: Long, detail: String) {
        submitAnswerFeedback(messageId, AnswerFeedbackAction.ContentError, detail)
    }

    private fun submitAnswerFeedback(messageId: Long, action: AnswerFeedbackAction, detail: String?) {
        val message = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        viewModelScope.launch {
            try {
                repository.submitAnswerFeedback(message.serverId, action, detail)
                val notice = when (action) {
                    AnswerFeedbackAction.Helpful, AnswerFeedbackAction.Unhelpful -> "感谢反馈，我们会持续改进回答"
                    AnswerFeedbackAction.ContentError -> "已提交内容审核"
                    AnswerFeedbackAction.Review -> "已将关联知识点加入待复习"
                }
                if (action == AnswerFeedbackAction.Review) {
                    val items = repository.learningItems(LearningFilter.Review)
                    _uiState.update { it.copy(learningItems = items, errorMessage = notice) }
                } else {
                    _uiState.update { it.copy(errorMessage = notice) }
                }
            } catch (error: Exception) {
                handleApiError(error) { it }
            }
        }
    }

    fun logout() {
        stopMistakePolling()
        viewModelScope.launch {
            repository.logout()
            _uiState.value = AppUiState(
                authChecking = false,
                destination = AppDestination.Account,
            )
        }
    }

    private fun handleApiError(error: Exception, update: (AppUiState) -> AppUiState) {
        val unauthorized = error is ApiFailureException && error.statusCode == 401
        val privacyRequired = error is ApiFailureException && error.statusCode == 428
        _uiState.update {
            update(it).copy(
                authenticated = if (unauthorized) false else it.authenticated,
                privacyConsentRequired = privacyRequired || it.privacyConsentRequired,
                authScreenOpen = false,
                errorMessage = when {
                    unauthorized -> "登录已过期，请重新登录"
                    privacyRequired -> null
                    else -> error.userMessage()
                },
            )
        }
    }

    private fun Exception.userMessage(): String = when ((this as? ApiFailureException)?.statusCode) {
        402 -> "额度不足，请先补充额度"
        428 -> "请先阅读并同意当前版本的隐私说明"
        502 -> "AI 服务暂时不可用，本次未扣除额度"
        503 -> message ?: "AI 服务暂时不可用，请稍后再试"
        else -> message ?: "发生未知错误，请稍后重试"
    }

    companion object {
        private val OCR_PENDING_STATUSES = setOf("queued", "recognizing")

        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AppViewModel(AppRepositoryProvider.get(context)) as T

            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T =
                AppViewModel(
                    repository = AppRepositoryProvider.get(context),
                    savedStateHandle = extras.createSavedStateHandle(),
                ) as T
        }
    }
}

private const val STATE_DESTINATION = "ui.destination"
private const val STATE_DRAFT = "ui.chat_draft"
private const val STATE_SELECTED_NODE = "ui.selected_node"
private const val STATE_HELP_LEVEL = "ui.help_level"
private const val STATE_QA_MODE = "ui.qa_mode"
private const val STATE_SUBJECT = "ui.subject"
private const val STATE_SCOPE_GRADE = "ui.scope_grade"
private const val STATE_SCOPE_VERSION = "ui.scope_version"
private const val STATE_LEARNING_FILTER = "ui.learning_filter"
private const val STATE_SHOWS_MISTAKES = "ui.shows_mistakes"
private const val STATE_SESSION_SEARCH = "ui.session_search"
private const val STATE_CONVERSATION_ID = "ui.conversation_id"

private fun restoredUiState(handle: SavedStateHandle): AppUiState {
    val subject = handle.get<String>(STATE_SUBJECT)?.takeIf { it.isNotBlank() } ?: "数学"
    val grade = handle.get<String>(STATE_SCOPE_GRADE)
    val version = handle.get<String>(STATE_SCOPE_VERSION)
    val scope = if (!grade.isNullOrBlank() && !version.isNullOrBlank()) {
        KnowledgeCatalogScope(subject, grade, version, 0)
    } else {
        null
    }
    return AppUiState(
        destination = enumValueOrDefault(handle[STATE_DESTINATION], AppDestination.Chat),
        draft = handle.get<String>(STATE_DRAFT).orEmpty(),
        selectedNodeId = handle[STATE_SELECTED_NODE],
        helpLevel = enumValueOrDefault(handle[STATE_HELP_LEVEL], QaHelpLevel.Approach),
        qaMode = enumValueOrDefault(handle[STATE_QA_MODE], QaMode.Knowledge),
        currentSubject = subject,
        selectedKnowledgeScope = scope,
        learningFilter = enumValueOrDefault(handle[STATE_LEARNING_FILTER], LearningFilter.Recent),
        learningShowsMistakes = handle.get<Boolean>(STATE_SHOWS_MISTAKES) ?: false,
        sessionSearchQuery = handle.get<String>(STATE_SESSION_SEARCH).orEmpty().take(120),
        conversationId = handle[STATE_CONVERSATION_ID],
    )
}

private inline fun <reified T : Enum<T>> enumValueOrDefault(value: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == value } ?: fallback

private fun persistUiState(handle: SavedStateHandle, state: AppUiState) {
    handle[STATE_DESTINATION] = state.destination.name
    handle[STATE_DRAFT] = state.draft
    handle[STATE_SELECTED_NODE] = state.selectedNodeId
    handle[STATE_HELP_LEVEL] = state.helpLevel.name
    handle[STATE_QA_MODE] = state.qaMode.name
    handle[STATE_SUBJECT] = state.currentSubject
    handle[STATE_SCOPE_GRADE] = state.selectedKnowledgeScope?.grade
    handle[STATE_SCOPE_VERSION] = state.selectedKnowledgeScope?.textbookVersion
    handle[STATE_LEARNING_FILTER] = state.learningFilter.name
    handle[STATE_SHOWS_MISTAKES] = state.learningShowsMistakes
    handle[STATE_SESSION_SEARCH] = state.sessionSearchQuery
    handle[STATE_CONVERSATION_ID] = state.conversationId
}

private fun featureAvailability(result: Result<*>): Boolean? = when {
    result.isSuccess -> true
    (result.exceptionOrNull() as? ApiFailureException)?.statusCode == 404 -> false
    else -> null
}

private data class Sextuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
)
