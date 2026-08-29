package cn.qingkui.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class LoginRequest(
    val username: String,
    val password: String,
    @SerializedName("device_name") val deviceName: String = "Qingkui Android",
)

data class RegisterRequest(
    val username: String,
    val password: String,
    val nickname: String?,
    val email: String? = null,
    @SerializedName("device_name") val deviceName: String = "Qingkui Android",
)

data class RefreshRequest(@SerializedName("refresh_token") val refreshToken: String)
data class LogoutRequest(@SerializedName("refresh_token") val refreshToken: String)

data class UserDto(
    val id: String,
    val username: String,
    val nickname: String,
)

data class AuthResponse(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    val user: UserDto,
)

data class CreditDto(val balance: Int)

data class KnowledgeNodeDto(
    val id: String,
    val name: String,
    val subject: String,
    val grade: String,
    val chapter: String,
    val definition: String,
    val status: String,
)

data class NeighborNodeDto(
    val id: String,
    val name: String,
    val subject: String,
    val grade: String,
    val chapter: String,
    val definition: String,
    val status: String,
    @SerializedName("edge_type") val edgeType: String,
    @SerializedName("edge_explanation") val edgeExplanation: String,
)

data class NeighborResponse(
    val center: KnowledgeNodeDto,
    val nodes: List<NeighborNodeDto>,
)

data class ConversationCreate(
    val mode: String = "knowledge",
    @SerializedName("knowledge_node_id") val knowledgeNodeId: String?,
)

data class ConversationDto(
    val id: String,
    val title: String = "新对话",
    val mode: String = "knowledge",
    @SerializedName("knowledge_node_id") val knowledgeNodeId: String? = null,
    val subject: String? = null,
    @SerializedName("updated_at") val updatedAt: String = "",
    val messages: List<MessageDto> = emptyList(),
)

data class MessageCreate(
    val content: String,
    @SerializedName("help_level") val helpLevel: String = "approach",
)

data class CitationDto(
    @SerializedName("node_id") val nodeId: String,
    @SerializedName("node_name") val nodeName: String,
    @SerializedName("source_title") val sourceTitle: String,
    @SerializedName("source_location") val sourceLocation: String,
    val excerpt: String,
)

data class MessageDto(
    val id: String,
    val role: String,
    val content: String,
    val citations: List<CitationDto> = emptyList(),
)

data class KnowledgeNodeDetailDto(
    val id: String,
    val name: String,
    val subject: String,
    val grade: String,
    val chapter: String,
    val definition: String,
    val status: String,
    @SerializedName("textbook_version") val textbookVersion: String = "",
    val explanation: String = "",
    @SerializedName("common_errors") val commonErrors: List<String> = emptyList(),
    @SerializedName("question_types") val questionTypes: List<String> = emptyList(),
    @SerializedName("source_excerpt") val sourceExcerpt: String = "",
)

data class KnowledgeStateUpdate(
    val status: String,
    val note: String? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean? = null,
)

data class LearningEventRequest(
    @SerializedName("event_type") val eventType: String,
    @SerializedName("node_id") val nodeId: String?,
    @SerializedName("event_data") val eventData: Map<String, Any?> = emptyMap(),
)

data class CreditLedgerDto(
    val id: String,
    val amount: Int,
    @SerializedName("balance_after") val balanceAfter: Int,
    @SerializedName("entry_type") val entryType: String,
    val feature: String,
    @SerializedName("reference_id") val referenceId: String?,
    @SerializedName("created_at") val createdAt: String,
)

data class ChangePasswordRequest(
    @SerializedName("current_password") val currentPassword: String,
    @SerializedName("new_password") val newPassword: String,
)

data class QaResultDto(
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("assistant_message") val assistantMessage: MessageDto,
    val balance: Int,
    @SerializedName("credits_charged") val creditsCharged: Int,
    val subject: String? = null,
)

data class FeedbackCreate(
    val category: String,
    val content: String,
    @SerializedName("message_id") val messageId: String?,
)

data class FeedbackDto(val id: String)

data class LearningSummaryItemDto(
    val id: String,
    val name: String,
    val status: String,
    @SerializedName("updated_at") val updatedAt: String,
)

data class LearningSummaryDto(
    val recent: List<LearningSummaryItemDto> = emptyList(),
    val review: List<LearningSummaryItemDto> = emptyList(),
    @SerializedName("error_prone") val errorProne: List<LearningSummaryItemDto> = emptyList(),
)

data class LearningEventCreate(
    @SerializedName("event_type") val eventType: String,
    @SerializedName("node_id") val nodeId: String?,
    @SerializedName("event_data") val eventData: Map<String, Any?> = emptyMap(),
)

data class LearningEventDto(val id: String)
data class ApiErrorDto(val detail: String?)
