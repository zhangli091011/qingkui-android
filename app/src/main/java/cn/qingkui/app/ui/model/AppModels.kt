package cn.qingkui.app.ui.model

import androidx.compose.runtime.Immutable

enum class AppDestination(val label: String) {
    Chat("问答"),
    Graph("图谱"),
    Learning("学习"),
    Account("账户"),
}

enum class MessageAuthor { Student, Assistant }

@Immutable
data class ChatMessage(
    val id: Long,
    val author: MessageAuthor,
    val text: String,
    val source: String? = null,
)

enum class KnowledgeStatus(val label: String) {
    Unexplored("未探索"),
    Explored("已探索"),
    Understood("已理解"),
    Verified("已验证"),
    Unstable("不稳定"),
    ErrorProne("易错"),
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
}

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
data class AppUiState(
    val destination: AppDestination = AppDestination.Chat,
    val draft: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val credits: Int = 1280,
    val selectedNodeId: String? = "quadratic_function",
    val drawerOpen: Boolean = false,
)
