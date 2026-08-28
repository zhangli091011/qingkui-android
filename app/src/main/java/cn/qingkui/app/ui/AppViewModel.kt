package cn.qingkui.app.ui

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import cn.qingkui.app.ui.model.AppDestination
import cn.qingkui.app.ui.model.AppUiState
import cn.qingkui.app.ui.model.ChatMessage
import cn.qingkui.app.ui.model.KnowledgeNode
import cn.qingkui.app.ui.model.KnowledgeKind
import cn.qingkui.app.ui.model.KnowledgeRelation
import cn.qingkui.app.ui.model.KnowledgeSource
import cn.qingkui.app.ui.model.KnowledgeStatus
import cn.qingkui.app.ui.model.LearningItem
import cn.qingkui.app.ui.model.MessageAuthor
import cn.qingkui.app.ui.model.RelationType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class AppViewModel(private val savedStateHandle: SavedStateHandle) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AppUiState(
            destination = savedStateHandle.get<String>(KEY_DESTINATION)
                ?.let { value -> AppDestination.entries.firstOrNull { it.name == value } }
                ?: AppDestination.Chat,
            draft = savedStateHandle.get<String>(KEY_DRAFT) ?: "",
        ),
    )
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    val graphNodes = listOf(
        KnowledgeNode(
            "linear_function", "一次函数", "前置知识", KnowledgeStatus.Verified, .12f, .60f,
            KnowledgeKind.Concept, KnowledgeSource.Official,
            "形如 y = kx + b 的函数，是理解函数图像与变化趋势的基础。",
            "必修第一册 · 第 3 章", true,
        ),
        KnowledgeNode(
            "function_concept", "函数概念", "前置知识", KnowledgeStatus.Understood, .27f, .27f,
            KnowledgeKind.Concept, KnowledgeSource.Official,
            "描述两个变量之间确定对应关系的数学模型。",
            "必修第一册 · 3.1 函数的概念", true,
        ),
        KnowledgeNode(
            "quadratic_function", "二次函数", "当前知识", KnowledgeStatus.Explored, .49f, .49f,
            KnowledgeKind.Concept, KnowledgeSource.Official,
            "一般地，形如 y = ax² + bx + c（a ≠ 0）的函数叫作二次函数。",
            "必修第一册 · 3.2 二次函数", true,
        ),
        KnowledgeNode(
            "discriminant", "判别式", "关联公式", KnowledgeStatus.Unstable, .70f, .25f,
            KnowledgeKind.Formula, KnowledgeSource.Official,
            "Δ = b² - 4ac，用来判断一元二次方程实数根的个数。",
            "必修第一册 · 2.3 一元二次方程", false,
        ),
        KnowledgeNode(
            "quadratic_inequality", "一元二次不等式", "迁移应用", KnowledgeStatus.Unexplored, .86f, .56f,
            KnowledgeKind.Method, KnowledgeSource.AiCandidate,
            "结合二次函数图像判断不等式解集，是函数与方程思想的综合应用。",
            "AI 关联建议 · 待教师审核", false,
        ),
        KnowledgeNode(
            "parabola", "抛物线", "易错知识", KnowledgeStatus.ErrorProne, .65f, .78f,
            KnowledgeKind.Concept, KnowledgeSource.Personal,
            "二次函数的图像，开口方向、顶点和对称轴由解析式共同决定。",
            "个人知识卡 · 最近更新于昨天", true,
        ),
        KnowledgeNode(
            "vertex_formula", "顶点式", "常用方法", KnowledgeStatus.Explored, .39f, .82f,
            KnowledgeKind.Formula, KnowledgeSource.Official,
            "y = a(x-h)² + k 可直接读出抛物线顶点 (h, k)。",
            "必修第一册 · 3.2 二次函数", false,
        ),
        KnowledgeNode(
            "parameter_problem", "参数范围题", "典型题型", KnowledgeStatus.Unexplored, .88f, .82f,
            KnowledgeKind.QuestionType, KnowledgeSource.Personal,
            "根据根、交点或最值条件建立参数不等式。",
            "个人知识卡 · 来源：错题整理", false,
        ),
    )

    val graphRelations = listOf(
        KnowledgeRelation("function_concept", "quadratic_function", RelationType.Prerequisite),
        KnowledgeRelation("linear_function", "quadratic_function", RelationType.Prerequisite),
        KnowledgeRelation("quadratic_function", "discriminant", RelationType.Related),
        KnowledgeRelation("quadratic_function", "parabola", RelationType.Confusable),
        KnowledgeRelation("quadratic_function", "vertex_formula", RelationType.Related),
        KnowledgeRelation("quadratic_function", "quadratic_inequality", RelationType.Related),
        KnowledgeRelation("discriminant", "parameter_problem", RelationType.QuestionType),
        KnowledgeRelation("parabola", "parameter_problem", RelationType.QuestionType),
    )

    val learningItems = listOf(
        LearningItem("quadratic_function", "二次函数", KnowledgeStatus.Explored, "今天 09:20", "继续探索"),
        LearningItem("discriminant", "判别式", KnowledgeStatus.Unstable, "昨天 20:42", "开始复习"),
        LearningItem("parabola", "抛物线的顶点与对称轴", KnowledgeStatus.ErrorProne, "8 月 26 日", "查看错因"),
        LearningItem("linear_function", "一次函数", KnowledgeStatus.Verified, "8 月 24 日", "回到图谱"),
    )

    fun selectDestination(destination: AppDestination) {
        savedStateHandle[KEY_DESTINATION] = destination.name
        _uiState.update { it.copy(destination = destination, drawerOpen = false) }
    }

    fun updateDraft(value: String) {
        savedStateHandle[KEY_DRAFT] = value
        _uiState.update { it.copy(draft = value) }
    }

    fun sendMessage() {
        val question = _uiState.value.draft.trim()
        if (question.isEmpty()) return
        val messageId = System.currentTimeMillis()
        val answer = "二次函数可以写成 y = ax² + bx + c（a ≠ 0）。它的图像是一条抛物线，a 的正负决定开口方向。你可以先观察 a、b、c 分别变化时，图像会发生什么。"
        _uiState.update {
            it.copy(
                draft = "",
                credits = (it.credits - 1).coerceAtLeast(0),
                messages = it.messages + listOf(
                    ChatMessage(messageId, MessageAuthor.Student, question),
                    ChatMessage(messageId + 1, MessageAuthor.Assistant, answer, "高一数学 · 二次函数"),
                ),
            )
        }
        savedStateHandle[KEY_DRAFT] = ""
    }

    fun selectNode(nodeId: String) {
        _uiState.update { it.copy(selectedNodeId = nodeId) }
    }

    fun askAboutNode(nodeId: String) {
        val node = graphNodes.firstOrNull { it.id == nodeId } ?: return
        updateDraft("请帮我理解${node.title}")
        selectDestination(AppDestination.Chat)
    }

    fun setDrawerOpen(open: Boolean) {
        _uiState.update { it.copy(drawerOpen = open) }
    }

    private companion object {
        const val KEY_DESTINATION = "destination"
        const val KEY_DRAFT = "chat_draft"
    }
}
