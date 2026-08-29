package cn.qingkui.app.data.repository

import android.content.Context
import cn.qingkui.app.data.auth.TokenStore
import cn.qingkui.app.data.remote.NetworkModule
import cn.qingkui.app.data.remote.QingkuiApi
import cn.qingkui.app.data.remote.dto.ApiErrorDto
import cn.qingkui.app.data.remote.dto.ConversationCreate
import cn.qingkui.app.data.remote.dto.ChangePasswordRequest
import cn.qingkui.app.data.remote.dto.FeedbackCreate
import cn.qingkui.app.data.remote.dto.KnowledgeNodeDto
import cn.qingkui.app.data.remote.dto.LearningEventCreate
import cn.qingkui.app.data.remote.dto.LearningEventRequest
import cn.qingkui.app.data.remote.dto.KnowledgeStateUpdate
import cn.qingkui.app.data.remote.dto.KnowledgeNodeDetailDto
import cn.qingkui.app.data.remote.dto.LoginRequest
import cn.qingkui.app.data.remote.dto.LogoutRequest
import cn.qingkui.app.data.remote.dto.MessageCreate
import cn.qingkui.app.data.remote.dto.NeighborNodeDto
import cn.qingkui.app.data.remote.dto.RegisterRequest
import cn.qingkui.app.ui.model.ChatMessage
import cn.qingkui.app.ui.model.ConversationSummary
import cn.qingkui.app.ui.model.CreditLedgerItem
import cn.qingkui.app.ui.model.KnowledgeKind
import cn.qingkui.app.ui.model.KnowledgeNode
import cn.qingkui.app.ui.model.KnowledgeRelation
import cn.qingkui.app.ui.model.KnowledgeSource
import cn.qingkui.app.ui.model.KnowledgeStatus
import cn.qingkui.app.ui.model.LearningItem
import cn.qingkui.app.ui.model.MessageAuthor
import cn.qingkui.app.ui.model.MessageCitation
import cn.qingkui.app.ui.model.QaHelpLevel
import cn.qingkui.app.ui.model.QaMode
import cn.qingkui.app.ui.model.RelationType
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
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

class ApiFailureException(val statusCode: Int?, message: String) : Exception(message)

interface AppRepository {
    suspend fun hasSession(): Boolean
    suspend fun nickname(): String?
    suspend fun login(username: String, password: String): String
    suspend fun register(username: String, password: String, nickname: String): String
    suspend fun logout()
    suspend fun credits(): Int
    suspend fun creditLedger(): List<CreditLedgerItem>
    suspend fun graph(centerId: String = "quadratic_function"): GraphData
    suspend fun nodeDetail(nodeId: String): KnowledgeNode
    suspend fun search(query: String): GraphData
    suspend fun sessions(): List<ConversationSummary>
    suspend fun restoreSession(sessionId: String): List<ChatMessage>
    suspend fun deleteSession(sessionId: String)
    suspend fun learningItems(): List<LearningItem>
    suspend fun updateNodeState(nodeId: String, status: KnowledgeStatus, note: String?, favorite: Boolean?): LearningItem?
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
    suspend fun submitAnswerFeedback(messageId: String?, helpful: Boolean)
}

class NetworkAppRepository(
    private val api: QingkuiApi,
    private val tokenStore: TokenStore,
) : AppRepository {
    private val gson = Gson()

    override suspend fun hasSession(): Boolean = tokenStore.refreshToken() != null
    override suspend fun nickname(): String? = tokenStore.nickname()

    override suspend fun login(username: String, password: String): String = apiCall {
        api.login(LoginRequest(username.trim(), password)).also { tokenStore.save(it) }.user.nickname
    }

    override suspend fun register(username: String, password: String, nickname: String): String = apiCall {
        api.register(RegisterRequest(username.trim(), password, nickname.trim().ifBlank { null }))
            .also { tokenStore.save(it) }
            .user.nickname
    }

    override suspend fun logout() {
        val refreshToken = tokenStore.refreshToken()
        try {
            if (refreshToken != null) api.logout(LogoutRequest(refreshToken))
        } catch (_: Exception) {
            // Local sign-out must remain available when the backend is offline.
        } finally {
            tokenStore.clear()
        }
    }

    override suspend fun credits(): Int = apiCall { api.credits().balance }

    override suspend fun creditLedger(): List<CreditLedgerItem> = apiCall {
        api.creditLedger().map { item ->
            CreditLedgerItem(item.id, item.amount, item.balanceAfter, item.entryType, item.createdAt)
        }
    }

    override suspend fun graph(centerId: String): GraphData = apiCall {
        val response = api.neighbors(centerId)
        val center = response.center.toUi(.5f, .5f)
        val neighbors = response.nodes.mapIndexed { index, node ->
            val angle = (2.0 * Math.PI * index / response.nodes.size.coerceAtLeast(1)) - Math.PI / 2
            node.toUi(
                x = (.5 + .34 * cos(angle)).toFloat(),
                y = (.5 + .34 * sin(angle)).toFloat(),
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

    override suspend fun sessions(): List<ConversationSummary> = apiCall {
        api.sessions().map { item ->
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

    override suspend fun learningItems(): List<LearningItem> = apiCall {
        api.learningSummary().recent.map { item ->
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
        val streamResponse = api.streamMessage(activeSessionId, body)
        val result = if (streamResponse.code() == 404) {
            streamResponse.errorBody()?.close()
            api.sendMessage(activeSessionId, body).also { fallback ->
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

    private fun parseStream(body: okhttp3.ResponseBody?, onDelta: (String) -> Unit): cn.qingkui.app.data.remote.dto.QaResultDto {
        if (body == null) throw ApiFailureException(null, "流式响应为空")
        var event = ""
        var completed: cn.qingkui.app.data.remote.dto.QaResultDto? = null
        body.use { responseBody ->
            val source = responseBody.source()
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
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

    override suspend fun submitAnswerFeedback(messageId: String?, helpful: Boolean) = apiCall {
        api.submitFeedback(
            FeedbackCreate(
                category = if (helpful) "other" else "answer_error",
                content = if (helpful) "该回答对本次学习有帮助" else "该回答没有解决我的问题",
                messageId = messageId,
            ),
        )
        Unit
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
    )
}

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
            NetworkAppRepository(NetworkModule.create(tokenStore), tokenStore).also { instance = it }
        }
    }
}
