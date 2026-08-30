package cn.qingkui.app.ui

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.qingkui.app.data.repository.ApiFailureException
import cn.qingkui.app.data.repository.AppRepository
import cn.qingkui.app.data.repository.AppRepositoryProvider
import cn.qingkui.app.ui.model.AppDestination
import cn.qingkui.app.ui.model.AppUiState
import cn.qingkui.app.ui.model.AuthMode
import cn.qingkui.app.ui.model.ChatMessage
import cn.qingkui.app.ui.model.MessageAuthor
import cn.qingkui.app.ui.model.QaHelpLevel
import cn.qingkui.app.ui.model.QaMode
import cn.qingkui.app.ui.model.KnowledgeStatus
import cn.qingkui.app.ui.model.LearningFilter
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class AppViewModel(private val repository: AppRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.observeMistakeDrafts().collectLatest { drafts ->
                _uiState.update { it.copy(mistakeDrafts = drafts) }
            }
        }
        viewModelScope.launch {
            val hasSession = repository.hasSession()
            _uiState.update {
                it.copy(
                    authChecking = false,
                    authenticated = hasSession,
                    currentUserName = repository.nickname() ?: it.currentUserName,
                )
            }
            if (hasSession) refreshContent()
        }
    }

    fun setAuthMode(mode: AuthMode) = _uiState.update { it.copy(authMode = mode, errorMessage = null) }
    fun updateUsername(value: String) = _uiState.update { it.copy(username = value, errorMessage = null) }
    fun updatePassword(value: String) = _uiState.update { it.copy(password = value, errorMessage = null) }
    fun updateNickname(value: String) = _uiState.update { it.copy(nickname = value, errorMessage = null) }

    fun submitAuth() {
        val state = _uiState.value
        if (state.username.trim().length < 3 || state.password.length < 8) {
            _uiState.update { it.copy(errorMessage = "用户名至少 3 位，密码至少 8 位") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(authLoading = true, errorMessage = null) }
            try {
                val name = if (state.authMode == AuthMode.Login) {
                    repository.login(state.username, state.password)
                } else {
                    repository.register(state.username, state.password, state.nickname)
                }
                _uiState.update {
                    it.copy(
                        authenticated = true,
                        authScreenOpen = false,
                        authLoading = false,
                        password = "",
                        currentUserName = name,
                        pendingSendAfterAuth = false,
                    )
                }
                refreshContentNow()
                if (state.pendingSendAfterAuth) sendMessage()
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
                val graphTask = async { repository.graph() }
                val learningTask = async { repository.learningItems(_uiState.value.learningFilter) }
                val sessionsTask = async { repository.sessions() }
                val ledgerTask = async { repository.creditLedger() }
                val mistakesTask = async { repository.mistakes() }
                Sextuple(creditTask.await(), graphTask.await(), learningTask.await(), sessionsTask.await(), ledgerTask.await(), mistakesTask.await())
            }
            _uiState.update {
                it.copy(
                    credits = credits,
                    graphNodes = graph.nodes,
                    graphRelations = graph.relations,
                    selectedNodeId = graph.selectedNodeId,
                    currentSubject = graph.nodes.firstOrNull()?.evidence?.substringBefore(" · ") ?: it.currentSubject,
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
    }

    fun selectDestination(destination: AppDestination) {
        _uiState.update { it.copy(destination = destination, drawerOpen = false) }
    }

    fun showLearningRecords() = _uiState.update { it.copy(learningShowsMistakes = false) }

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
    ) {
        viewModelScope.launch {
            try {
                repository.saveMistakeDraft(imagePath, subject, questionText, studentWork, questionGoal)
                _uiState.update {
                    it.copy(
                        mistakeCaptureOpen = false,
                        destination = AppDestination.Learning,
                        learningShowsMistakes = true,
                        errorMessage = "图片已保存，将在网络可用时上传识别",
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
                val items = repository.mistakes()
                _uiState.update { it.copy(mistakes = items, mistakeLoading = false) }
            } catch (error: Exception) {
                handleApiError(error) { it.copy(mistakeLoading = false) }
            }
        }
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

    fun analyzeMistake(mistakeId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(mistakeLoading = true, errorMessage = null) }
            try {
                val outcome = repository.analyzeMistake(mistakeId)
                val items = repository.mistakes()
                _uiState.update {
                    it.copy(credits = outcome.balance, mistakes = items, mistakeLoading = false)
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
                val items = repository.mistakes()
                _uiState.update { it.copy(mistakes = items, mistakeLoading = false) }
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
                val items = repository.mistakes()
                _uiState.update { it.copy(mistakes = items, mistakeLoading = false) }
            } catch (error: Exception) {
                handleApiError(error) { it.copy(mistakeLoading = false) }
            }
        }
    }

    fun updateDraft(value: String) = _uiState.update { it.copy(draft = value, errorMessage = null) }
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
        val messageId = System.currentTimeMillis()
        val assistantMessageId = messageId + 1
        _uiState.update {
            it.copy(
                draft = "",
                sending = true,
                errorMessage = null,
                messages = it.messages + listOf(
                    ChatMessage(messageId, MessageAuthor.Student, question),
                    ChatMessage(assistantMessageId, MessageAuthor.Assistant, ""),
                ),
            )
        }
        viewModelScope.launch {
            try {
                val result = repository.sendQuestion(
                    state.conversationId,
                    state.selectedNodeId,
                    question,
                    state.qaMode,
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
            } catch (error: Exception) {
                handleApiError(error) {
                    it.copy(
                        sending = false,
                        draft = question,
                        messages = it.messages.filterNot { message ->
                            message.id == messageId || message.id == assistantMessageId
                        },
                    )
                }
            }
        }
    }

    fun selectNode(nodeId: String) {
        _uiState.update { it.copy(selectedNodeId = nodeId, conversationId = null) }
        viewModelScope.launch {
            runCatching { repository.graph(nodeId) }
                .onSuccess { graph -> _uiState.update { it.copy(graphNodes = graph.nodes, graphRelations = graph.relations, selectedNodeId = nodeId) } }
                .onFailure { handleApiError(it as? Exception ?: Exception(it)) { state -> state } }
            runCatching { repository.nodeDetail(nodeId) }
                .onSuccess { detail -> _uiState.update { it.copy(selectedNodeDetail = detail, graphNodes = it.graphNodes.map { node -> if (node.id == nodeId) detail else node }) } }
                .onFailure { handleApiError(it as? Exception ?: Exception(it)) { state -> state } }
            runCatching { repository.recordLearningEvent(nodeId, "viewed_node") }
        }
    }

    fun markNodeStatus(status: KnowledgeStatus) {
        val nodeId = _uiState.value.selectedNodeId ?: return
        viewModelScope.launch {
            try {
                repository.updateNodeState(nodeId, status, _uiState.value.noteDraft, null)
                _uiState.update { state -> state.copy(graphNodes = state.graphNodes.map { if (it.id == nodeId) it.copy(status = status) else it }, learningItems = state.learningItems.map { if (it.nodeId == nodeId) it.copy(status = status) else it }) }
            } catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun toggleFavorite() {
        val nodeId = _uiState.value.selectedNodeId ?: return
        val node = _uiState.value.graphNodes.firstOrNull { it.id == nodeId } ?: return
        viewModelScope.launch {
            try {
                repository.updateNodeState(nodeId, node.status, _uiState.value.noteDraft, !node.saved)
                _uiState.update { state -> state.copy(graphNodes = state.graphNodes.map { if (it.id == nodeId) it.copy(saved = !node.saved) else it }) }
            } catch (error: Exception) { handleApiError(error) { it } }
        }
    }

    fun updateNote(value: String) {
        val nodeId = _uiState.value.selectedNodeId
        _uiState.update { it.copy(noteDraft = value) }
        if (nodeId != null) {
            viewModelScope.launch {
                kotlinx.coroutines.delay(450)
                runCatching { repository.updateNodeState(nodeId, _uiState.value.graphNodes.firstOrNull { it.id == nodeId }?.status ?: KnowledgeStatus.Explored, value, null) }
            }
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

    fun changePassword(current: String, next: String) {
        viewModelScope.launch {
            try { repository.changePassword(current, next); repository.logout(); _uiState.value = AppUiState(authChecking = false, destination = AppDestination.Account, errorMessage = "密码已修改，请重新登录") }
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

    fun submitAnswerFeedback(messageId: Long, helpful: Boolean) {
        val message = _uiState.value.messages.firstOrNull { it.id == messageId } ?: return
        viewModelScope.launch {
            try {
                repository.submitAnswerFeedback(message.serverId, helpful)
                _uiState.update { it.copy(errorMessage = "感谢反馈，我们会持续改进回答") }
            } catch (error: Exception) {
                handleApiError(error) { it }
            }
        }
    }

    fun logout() {
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
        _uiState.update {
            update(it).copy(
                authenticated = if (unauthorized) false else it.authenticated,
                authScreenOpen = false,
                errorMessage = if (unauthorized) "登录已过期，请重新登录" else error.userMessage(),
            )
        }
    }

    private fun Exception.userMessage(): String = when ((this as? ApiFailureException)?.statusCode) {
        402 -> "额度不足，请先补充额度"
        502 -> "AI 服务暂时不可用，本次未扣除额度"
        503 -> "AI 服务尚未配置，请联系管理员"
        else -> message ?: "发生未知错误，请稍后重试"
    }

    companion object {
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T =
                AppViewModel(AppRepositoryProvider.get(context)) as T
        }
    }
}

private data class Sextuple<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F,
)
