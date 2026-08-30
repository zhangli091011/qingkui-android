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

data class DeviceSessionDto(
    val id: String,
    @SerializedName("device_name") val deviceName: String?,
    @SerializedName("expires_at") val expiresAt: String,
    @SerializedName("revoked_at") val revokedAt: String?,
    @SerializedName("created_at") val createdAt: String,
    val active: Boolean,
)

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

data class CreditCampaignDto(
    val id: String,
    val name: String,
    val amount: Int,
    @SerializedName("school_id") val schoolId: String?,
    val status: String,
    @SerializedName("starts_at") val startsAt: String,
    @SerializedName("ends_at") val endsAt: String,
    @SerializedName("max_redemptions") val maxRedemptions: Int,
    @SerializedName("redemption_count") val redemptionCount: Int,
    @SerializedName("per_user_limit") val perUserLimit: Int,
)

data class CreditRedemptionDto(
    val id: String,
    @SerializedName("campaign_id") val campaignId: String,
    @SerializedName("campaign_name") val campaignName: String,
    val amount: Int,
    @SerializedName("created_at") val createdAt: String,
)

data class CreditRedeemRequest(val code: String)
data class CreditRedeemResponseDto(
    @SerializedName("campaign_id") val campaignId: String,
    @SerializedName("campaign_name") val campaignName: String,
    val amount: Int,
    val balance: Int,
)

data class SchoolDto(val id: String, val name: String, val code: String, val status: String)
data class SchoolMembershipDto(
    val id: String,
    @SerializedName("school_id") val schoolId: String,
    val role: String,
    val status: String,
    @SerializedName("joined_at") val joinedAt: String,
    val school: SchoolDto,
)
data class OrganizationMeDto(val memberships: List<SchoolMembershipDto>)
data class SchoolClassDto(
    val id: String,
    @SerializedName("school_id") val schoolId: String,
    val name: String,
    val grade: String?,
    @SerializedName("academic_year") val academicYear: String,
    val status: String,
)
data class OrganizationInviteRedeemDto(val code: String)
data class OrganizationJoinDto(
    val school: SchoolDto,
    @SerializedName("school_role") val schoolRole: String,
    val classroom: SchoolClassDto?,
    val joined: Boolean,
)
data class ClassStudentOverviewDto(
    @SerializedName("anonymous_id") val anonymousId: String,
    @SerializedName("joined_at") val joinedAt: String,
    @SerializedName("last_activity_at") val lastActivityAt: String?,
    val questions: Int,
    val mistakes: Int,
    @SerializedName("verified_nodes") val verifiedNodes: Int,
)
data class ClassOverviewDto(
    val classroom: SchoolClassDto,
    @SerializedName("student_count") val studentCount: Int,
    @SerializedName("active_7d_students") val active7dStudents: Int,
    val questions: Int,
    val mistakes: Int,
    @SerializedName("verified_nodes") val verifiedNodes: Int,
    val students: List<ClassStudentOverviewDto>,
)

data class ContributionCreateDto(
    @SerializedName("contribution_type") val contributionType: String,
    val title: String,
    val content: String,
    @SerializedName("source_reference") val sourceReference: String?,
)
data class ContributionDto(
    val id: String,
    @SerializedName("school_id") val schoolId: String?,
    @SerializedName("contribution_type") val contributionType: String,
    val title: String,
    val content: String,
    @SerializedName("source_reference") val sourceReference: String?,
    val status: String,
    @SerializedName("review_note") val reviewNote: String?,
    @SerializedName("reward_amount") val rewardAmount: Int,
    @SerializedName("reward_status") val rewardStatus: String,
    @SerializedName("created_at") val createdAt: String,
)

data class KnowledgeNodeDto(
    val id: String,
    val name: String,
    val subject: String,
    val grade: String,
    val chapter: String,
    val section: String = "本章知识点",
    val definition: String,
    val status: String,
)

data class KnowledgeCatalogItemDto(
    val subject: String,
    val grade: String,
    @SerializedName("textbook_version") val textbookVersion: String,
    @SerializedName("node_count") val nodeCount: Int,
)
data class KnowledgeTreeNodeDto(val id: String, val name: String, val status: String)
data class KnowledgeTreeSectionDto(val name: String, val nodes: List<KnowledgeTreeNodeDto>)
data class KnowledgeTreeChapterDto(val name: String, val sections: List<KnowledgeTreeSectionDto>)
data class KnowledgeTreeDto(
    val subject: String,
    val grade: String,
    @SerializedName("textbook_version") val textbookVersion: String,
    val chapters: List<KnowledgeTreeChapterDto>,
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

data class PasswordResetRequest(val email: String)
data class PasswordResetConfirm(val token: String, @SerializedName("new_password") val newPassword: String)

data class QaResultDto(
    @SerializedName("conversation_id") val conversationId: String,
    @SerializedName("assistant_message") val assistantMessage: MessageDto,
    val balance: Int,
    @SerializedName("credits_charged") val creditsCharged: Int,
    val subject: String? = null,
)

data class QaIntentRequest(val content: String, val mode: String)

data class QaIntentOptionDto(
    val id: String,
    val label: String,
    val instruction: String,
    val mode: String,
)

data class QaIntentResultDto(
    @SerializedName("needs_clarification") val needsClarification: Boolean,
    val subject: String? = null,
    val prompt: String? = null,
    val options: List<QaIntentOptionDto> = emptyList(),
)

data class FeedbackCreate(
    val category: String,
    val content: String,
    @SerializedName("message_id") val messageId: String?,
)

data class FeedbackDto(
    val id: String,
    val category: String = "other",
    val content: String = "",
    @SerializedName("node_id") val nodeId: String? = null,
    @SerializedName("message_id") val messageId: String? = null,
    val status: String = "pending",
    @SerializedName("review_note") val reviewNote: String? = null,
    @SerializedName("reviewed_at") val reviewedAt: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
)

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
    val verified: List<LearningSummaryItemDto> = emptyList(),
)

data class LearningEventCreate(
    @SerializedName("event_type") val eventType: String,
    @SerializedName("node_id") val nodeId: String?,
    @SerializedName("event_data") val eventData: Map<String, Any?> = emptyMap(),
)

data class LearningEventDto(val id: String)

data class LearningCheckChoiceDto(
    val id: String,
    val text: String,
)

data class LearningCheckDto(
    val id: String,
    @SerializedName("node_id") val nodeId: String,
    val prompt: String,
    val choices: List<LearningCheckChoiceDto> = emptyList(),
    val status: String,
    @SerializedName("expires_at") val expiresAt: String,
)

data class LearningCheckSubmitDto(@SerializedName("choice_id") val choiceId: String)

data class LearningCheckResultDto(
    @SerializedName("attempt_id") val attemptId: String,
    val passed: Boolean,
    val status: String,
    val state: LearningSummaryItemDto,
)

data class ApiErrorDto(val detail: String?)

data class MistakeCreateDto(
    val subject: String?,
    @SerializedName("question_text") val questionText: String?,
    @SerializedName("student_work") val studentWork: String?,
    @SerializedName("question_goal") val questionGoal: String?,
    @SerializedName("error_category") val errorCategory: String? = null,
)

data class OcrCorrectionDto(@SerializedName("corrected_text") val correctedText: String)

data class MistakeAssetDto(
    val id: String,
    @SerializedName("mime_type") val mimeType: String,
    val status: String,
)

data class OcrTaskDto(
    val id: String,
    @SerializedName("asset_id") val assetId: String,
    val status: String,
    @SerializedName("result_text") val resultText: String? = null,
    val confidence: Double? = null,
    @SerializedName("requires_review") val requiresReview: Boolean = true,
    @SerializedName("error_message") val errorMessage: String? = null,
)

data class MistakeAnalysisDataDto(
    val diagnosis: String,
    @SerializedName("error_category") val errorCategory: String,
    @SerializedName("error_note") val errorNote: String,
    @SerializedName("correction_steps") val correctionSteps: List<String> = emptyList(),
    @SerializedName("suggested_node_id") val suggestedNodeId: String? = null,
    @SerializedName("node_confidence") val nodeConfidence: Double = 0.0,
    @SerializedName("similar_question") val similarQuestion: String,
    @SerializedName("answer_reference") val answerReference: String,
    val uncertain: Boolean = false,
)

data class MistakeAnalysisResponseDto(
    @SerializedName("mistake_id") val mistakeId: String,
    val analysis: MistakeAnalysisDataDto,
    @SerializedName("credits_charged") val creditsCharged: Int,
    val balance: Int,
)

data class MistakePracticeDto(
    val id: String,
    @SerializedName("round_id") val roundId: String? = null,
    val position: Int? = null,
    @SerializedName("question_text") val questionText: String,
    val hint: String? = null,
    @SerializedName("answer_reference") val answerReference: String? = null,
    val status: String,
    @SerializedName("student_answer") val studentAnswer: String? = null,
    @SerializedName("is_correct") val isCorrect: Boolean? = null,
    @SerializedName("validation_details") val validationDetails: Map<String, Any?> = emptyMap(),
)

data class MistakePracticeRoundDto(
    val id: String,
    @SerializedName("round_number") val roundNumber: Int,
    @SerializedName("review_stage") val reviewStage: String,
    val status: String,
    @SerializedName("question_count") val questionCount: Int,
    @SerializedName("correct_count") val correctCount: Int,
    @SerializedName("authoritative_correct_count") val authoritativeCorrectCount: Int,
    val practices: List<MistakePracticeDto> = emptyList(),
)

data class PracticeSubmitDto(@SerializedName("student_answer") val studentAnswer: String)

data class MistakeDto(
    val id: String,
    val subject: String? = null,
    @SerializedName("question_text") val questionText: String? = null,
    @SerializedName("corrected_text") val correctedText: String? = null,
    @SerializedName("student_work") val studentWork: String? = null,
    @SerializedName("error_category") val errorCategory: String? = null,
    @SerializedName("error_note") val errorNote: String? = null,
    val analysis: Map<String, Any?> = emptyMap(),
    @SerializedName("analysis_status") val analysisStatus: String = "not_started",
    @SerializedName("knowledge_node_id") val knowledgeNodeId: String? = null,
    @SerializedName("review_status") val reviewStatus: String,
    @SerializedName("study_status") val studyStatus: String,
    @SerializedName("review_stage") val reviewStage: String = "correction",
    @SerializedName("next_review_at") val nextReviewAt: String? = null,
    @SerializedName("second_attempt_correct") val secondAttemptCorrect: Boolean? = null,
    @SerializedName("review_streak") val reviewStreak: Int = 0,
    @SerializedName("created_at") val createdAt: String,
    val assets: List<MistakeAssetDto> = emptyList(),
    @SerializedName("ocr_tasks") val ocrTasks: List<OcrTaskDto> = emptyList(),
    val practices: List<MistakePracticeDto> = emptyList(),
)

data class WeeklyMistakeLinkDto(
    @SerializedName("mistake_id") val mistakeId: String,
    @SerializedName("practice_round_id") val practiceRoundId: String? = null,
    val title: String,
    @SerializedName("review_stage") val reviewStage: String,
)

data class MistakeWeeklyReviewDto(
    @SerializedName("week_start") val weekStart: String,
    @SerializedName("week_end") val weekEnd: String,
    @SerializedName("new_mistakes") val newMistakes: Int,
    @SerializedName("error_categories") val errorCategories: Map<String, Int> = emptyMap(),
    @SerializedName("due_reviews") val dueReviews: List<WeeklyMistakeLinkDto> = emptyList(),
    @SerializedName("practice_completion_rate") val practiceCompletionRate: Double,
    @SerializedName("authoritative_accuracy") val authoritativeAccuracy: Double,
    @SerializedName("second_attempt_accuracy") val secondAttemptAccuracy: Double,
    @SerializedName("seven_day_followup_rate") val sevenDayFollowupRate: Double,
)
