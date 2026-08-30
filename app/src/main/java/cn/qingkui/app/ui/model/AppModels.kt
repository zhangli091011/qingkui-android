package cn.qingkui.app.ui.model

import androidx.compose.runtime.Immutable

enum class AppDestination(val label: String) {
    Chat("问答"),
    Graph("图谱"),
    Learning("学习"),
    Account("账户"),
}

enum class MessageAuthor { Student, Assistant }

enum class QaHelpLevel(val label: String, val apiValue: String, val creditCost: Int) {
    Keyword("关键词", "keyword", 1),
    NextStep("下一步", "next_step", 1),
    Approach("解题思路", "approach", 1),
    Full("完整解析", "full", 2),
    Conclusion("只看结论", "conclusion", 1),
}

enum class QaMode(val label: String, val apiValue: String) {
    Knowledge("知识理解", "knowledge"),
    Problem("题目讲解", "problem"),
    Error("错题分析", "error"),
    Review("复习规划", "review"),
    Explore("知识探索", "explore"),
    Verify("结论核验", "verify"),
}

@Immutable
data class MessageCitation(
    val nodeName: String,
    val sourceTitle: String,
    val sourceLocation: String,
    val excerpt: String,
)

@Immutable
data class ChatMessage(
    val id: Long,
    val author: MessageAuthor,
    val text: String,
    val source: String? = null,
    val serverId: String? = null,
    val citations: List<MessageCitation> = emptyList(),
)

enum class KnowledgeStatus(val label: String) {
    Unexplored("未探索"),
    Explored("已探索"),
    Understood("已理解"),
    Verified("已验证"),
    Unstable("不稳定"),
    ErrorProne("易错"),
    ToExplore("待探索"),
}

enum class KnowledgeKind(val label: String) {
    Concept("概念"),
    Formula("公式"),
    Method("方法"),
    QuestionType("题型"),
}

enum class KnowledgeSource(val label: String) {
    Official("正式知识库"),
    Personal("个人知识卡"),
    AiCandidate("AI 建议"),
}

enum class RelationType(val label: String) {
    Prerequisite("前置"),
    Related("关联"),
    Confusable("易混"),
    QuestionType("题型"),
    Extension("延伸"),
}

enum class AuthMode { Login, Register }

@Immutable
data class KnowledgeNode(
    val id: String,
    val title: String,
    val subtitle: String,
    val status: KnowledgeStatus,
    val x: Float,
    val y: Float,
    val kind: KnowledgeKind = KnowledgeKind.Concept,
    val source: KnowledgeSource = KnowledgeSource.Official,
    val description: String = "",
    val evidence: String = "校本知识库 · 高一数学",
    val saved: Boolean = false,
)

@Immutable
data class KnowledgeRelation(
    val fromId: String,
    val toId: String,
    val type: RelationType,
)

@Immutable
data class LearningItem(
    val nodeId: String,
    val title: String,
    val status: KnowledgeStatus,
    val lastStudied: String,
    val action: String,
)

@Immutable
data class ConversationSummary(
    val id: String,
    val title: String,
    val mode: String,
    val subject: String?,
    val updatedAt: String = "",
)

@Immutable
data class CreditLedgerItem(
    val id: String,
    val amount: Int,
    val balanceAfter: Int,
    val entryType: String,
    val createdAt: String,
)

@Immutable
data class MistakeDraftItem(
    val id: String,
    val imagePath: String,
    val subject: String,
    val questionText: String,
    val status: String,
    val errorMessage: String?,
)

@Immutable
data class MistakeItem(
    val id: String,
    val subject: String,
    val questionText: String,
    val ocrTaskId: String?,
    val ocrStatus: String,
    val confidence: Double?,
    val requiresReview: Boolean,
    val errorCategory: String?,
    val studyStatus: String,
)

@Immutable
data class AppUiState(
    val destination: AppDestination = AppDestination.Chat,
    val draft: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val credits: Int = 0,
    val selectedNodeId: String? = null,
    val drawerOpen: Boolean = false,
    val authChecking: Boolean = true,
    val authenticated: Boolean = false,
    val authScreenOpen: Boolean = false,
    val authMode: AuthMode = AuthMode.Login,
    val username: String = "",
    val password: String = "",
    val nickname: String = "",
    val currentUserName: String = "青葵同学",
    val authLoading: Boolean = false,
    val contentLoading: Boolean = false,
    val sending: Boolean = false,
    val helpLevel: QaHelpLevel = QaHelpLevel.Approach,
    val qaMode: QaMode = QaMode.Knowledge,
    val pendingSendAfterAuth: Boolean = false,
    val errorMessage: String? = null,
    val conversationId: String? = null,
    val currentSubject: String = "数学",
    val graphNodes: List<KnowledgeNode> = emptyList(),
    val graphRelations: List<KnowledgeRelation> = emptyList(),
    val learningItems: List<LearningItem> = emptyList(),
    val sessions: List<ConversationSummary> = emptyList(),
    val ledger: List<CreditLedgerItem> = emptyList(),
    val selectedNodeDetail: KnowledgeNode? = null,
    val noteDraft: String = "",
    val mistakeDrafts: List<MistakeDraftItem> = emptyList(),
    val mistakes: List<MistakeItem> = emptyList(),
    val mistakeCaptureOpen: Boolean = false,
    val mistakeLoading: Boolean = false,
    val learningShowsMistakes: Boolean = false,
)
