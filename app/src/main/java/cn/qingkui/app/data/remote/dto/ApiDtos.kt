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
    @SerializedName("privacy_consent") val privacyConsent: Boolean = false,
    @SerializedName("privacy_notice_version") val privacyNoticeVersion: String,
    @SerializedName("device_name") val deviceName: String = "Qingkui Android",
)

data class RefreshRequest(@SerializedName("refresh_token") val refreshToken: String)
data class LogoutRequest(@SerializedName("refresh_token") val refreshToken: String)

data class PrivacyConsentRequest(
    val accepted: Boolean = true,
    @SerializedName("notice_version") val noticeVersion: String,
)

data class PrivacyConsentResponse(
    val required: Boolean,
    @SerializedName("required_version") val requiredVersion: String,
    @SerializedName("accepted_version") val acceptedVersion: String?,
    @SerializedName("accepted_at") val acceptedAt: String?,
)

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

data class HealthDto(
    val status: String,
    @SerializedName("ai_enabled") val aiEnabled: Boolean,
    @SerializedName("ai_ready") val aiReady: Boolean,
)

data class WorkspaceCapabilitiesDto(
    @SerializedName("student_workspace") val studentWorkspace: Boolean = true,
    @SerializedName("teacher_workspace") val teacherWorkspace: Boolean = false,
    @SerializedName("content_workspace") val contentWorkspace: Boolean = false,
    @SerializedName("operations_workspace") val operationsWorkspace: Boolean = false,
    @SerializedName("raw_student_content") val rawStudentContent: Boolean = false,
)

data class WorkspaceUserDto(
    val id: String,
    val username: String,
    val nickname: String,
    val role: String,
)

data class WorkspaceMeDto(
    val schema: String = "",
    val user: WorkspaceUserDto,
    val roles: List<String> = emptyList(),
    val scope: String = "self_only",
    @SerializedName("school_ids") val schoolIds: List<String> = emptyList(),
    val capabilities: WorkspaceCapabilitiesDto = WorkspaceCapabilitiesDto(),
    @SerializedName("data_scope_user_count") val dataScopeUserCount: Int? = null,
)

data class WorkspaceOcrQueueDto(
    val queued: Int = 0,
    val processing: Int = 0,
    val failed: Int = 0,
    @SerializedName("needs_review") val needsReview: Int = 0,
)

data class WorkspaceMetricsDto(
    @SerializedName("active_students") val activeStudents: Int = 0,
    @SerializedName("seven_day_return_rate") val sevenDayReturnRate: Double = 0.0,
    @SerializedName("mistake_upload_success_rate") val mistakeUploadSuccessRate: Double = 0.0,
    @SerializedName("ocr_correction_rate") val ocrCorrectionRate: Double = 0.0,
    @SerializedName("ocr_queue") val ocrQueue: WorkspaceOcrQueueDto = WorkspaceOcrQueueDto(),
    @SerializedName("mistake_analysis_completion_rate") val mistakeAnalysisCompletionRate: Double = 0.0,
    @SerializedName("same_practice_completion_rate") val samePracticeCompletionRate: Double = 0.0,
    @SerializedName("second_attempt_accuracy") val secondAttemptAccuracy: Double = 0.0,
    @SerializedName("ai_helpful_rate") val aiHelpfulRate: Double = 0.0,
    @SerializedName("ai_calls") val aiCalls: Int = 0,
    @SerializedName("ai_failed_calls") val aiFailedCalls: Int = 0,
    @SerializedName("ai_failure_rate") val aiFailureRate: Double = 0.0,
    @SerializedName("average_user_cost_tokens") val averageUserCostTokens: Double = 0.0,
    @SerializedName("mistakes_created") val mistakesCreated: Int = 0,
    @SerializedName("error_categories") val errorCategories: Map<String, Int> = emptyMap(),
    @SerializedName("due_reviews") val dueReviews: Int = 0,
    @SerializedName("pending_content") val pendingContent: Int? = null,
    @SerializedName("pending_formulas") val pendingFormulas: Int? = null,
    @SerializedName("approved_nodes") val approvedNodes: Int? = null,
    val documents: Int? = null,
    @SerializedName("knowledge_edges") val knowledgeEdges: Int? = null,
    @SerializedName("pending_feedback") val pendingFeedback: Int? = null,
    @SerializedName("audit_events") val auditEvents: Int? = null,
    @SerializedName("release_gate_passed") val releaseGatePassed: Boolean? = null,
    @SerializedName("security_events") val securityEvents: Int = 0,
)

data class WorkspaceRangeDto(
    @SerializedName("start_at") val startAt: String = "",
    @SerializedName("end_at") val endAt: String = "",
    val days: Int = 0,
)

data class WorkspaceDashboardDto(
    val schema: String = "",
    @SerializedName("generated_at") val generatedAt: String = "",
    val range: WorkspaceRangeDto = WorkspaceRangeDto(),
    val filters: Map<String, String?> = emptyMap(),
    val viewer: Map<String, Any?> = emptyMap(),
    val metrics: WorkspaceMetricsDto = WorkspaceMetricsDto(),
    val alerts: WorkspaceAlertsDto = WorkspaceAlertsDto(),
)

data class WorkspaceAlertDto(
    val severity: String = "warning",
    val code: String = "",
    val message: String = "",
    val value: Double? = null,
    val threshold: Double? = null,
)

data class WorkspaceAlertsDto(
    val status: String = "ok",
    val alerts: List<WorkspaceAlertDto> = emptyList(),
    @SerializedName("release_blockers") val releaseBlockers: List<WorkspaceAlertDto> = emptyList(),
    val restricted: Boolean = false,
)

data class WorkspaceTaskDto(
    @SerializedName("task_type") val taskType: String = "",
    val id: String = "",
    val status: String = "",
    @SerializedName("requires_review") val requiresReview: Boolean = false,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("updated_at") val updatedAt: String? = null,
    @SerializedName("error_code") val errorCode: String? = null,
    val scope: String? = null,
)

data class WorkspaceTasksDto(
    val schema: String = "",
    val scope: String = "",
    val items: List<WorkspaceTaskDto> = emptyList(),
)

data class WorkspaceActivityDto(
    val id: String = "",
    val action: String = "",
    @SerializedName("target_type") val targetType: String? = null,
    @SerializedName("target_id") val targetId: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
)

data class WorkspaceActivityResponseDto(
    val schema: String = "",
    val scope: String = "",
    val items: List<WorkspaceActivityDto> = emptyList(),
)

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
data class ClassAggregateCountDto(
    val label: String,
    val count: Int,
)
data class ClassOverviewDto(
    val classroom: SchoolClassDto,
    @SerializedName("student_count") val studentCount: Int,
    @SerializedName("active_7d_students") val active7dStudents: Int,
    val questions: Int,
    val mistakes: Int,
    @SerializedName("verified_nodes") val verifiedNodes: Int,
    @SerializedName("top_error_categories") val topErrorCategories: List<ClassAggregateCountDto>,
    @SerializedName("weak_knowledge_points") val weakKnowledgePoints: List<ClassAggregateCountDto>,
    @SerializedName("practice_completion_rate") val practiceCompletionRate: Double,
    @SerializedName("second_attempt_accuracy") val secondAttemptAccuracy: Double,
    @SerializedName("due_review_count") val dueReviewCount: Int,
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
    @SerializedName("is_favorite") val isFavorite: Boolean = false,
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
    @SerializedName("is_favorite") val isFavorite: Boolean = false,
    @SerializedName("edge_type") val edgeType: String,
    @SerializedName("edge_explanation") val edgeExplanation: String,
    @SerializedName("edge_outgoing") val edgeOutgoing: Boolean = true,
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
    val note: String? = null,
    @SerializedName("is_favorite") val isFavorite: Boolean = false,
)

data class AdminUserDto(
    val id: String,
    val username: String,
    val email: String? = null,
    val nickname: String = "",
    val role: String = "student",
    @SerializedName("tenant_id") val tenantId: String? = null,
    @SerializedName("is_active") val isActive: Boolean = true,
    val balance: Int? = null,
    @SerializedName("created_at") val createdAt: String = "",
    @SerializedName("deleted_at") val deletedAt: String? = null,
)

data class AdminSessionDto(
    val id: String,
    @SerializedName("user_id") val userId: String,
    val username: String = "",
    @SerializedName("device_name") val deviceName: String? = null,
    @SerializedName("expires_at") val expiresAt: String = "",
    @SerializedName("revoked_at") val revokedAt: String? = null,
    @SerializedName("created_at") val createdAt: String = "",
    val active: Boolean = true,
)

data class CorpusGenerateRequest(val subject: String, val category: String = "综合", val topic: String? = null, val grade: String = "高中", val count: Int = 3)
data class CorpusItemDto(val title: String, val content: String, val keywords: List<String> = emptyList(), val subject: String = "", val category: String = "", val grade: String = "", @SerializedName("source_date") val sourceDate: String = "", @SerializedName("source_url") val sourceUrl: String = "")
data class CorpusGenerateResponseDto(val items: List<CorpusItemDto> = emptyList())

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
    @SerializedName("node_id") val nodeId: String? = null,
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
    @SerializedName("review_reasons") val reviewReasons: List<String> = emptyList(),
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
    @SerializedName("knowledge_node_id") val knowledgeNodeId: String? = null,
    val title: String,
    @SerializedName("review_stage") val reviewStage: String,
    @SerializedName("next_review_at") val nextReviewAt: String? = null,
)

data class WeakKnowledgePointDto(
    @SerializedName("knowledge_node_id") val knowledgeNodeId: String,
    val name: String,
    @SerializedName("mistake_count") val mistakeCount: Int,
)

data class MistakeWeeklyReviewDto(
    @SerializedName("week_start") val weekStart: String,
    @SerializedName("week_end") val weekEnd: String,
    @SerializedName("new_mistakes") val newMistakes: Int,
    @SerializedName("error_categories") val errorCategories: Map<String, Int> = emptyMap(),
    @SerializedName("weak_knowledge_points") val weakKnowledgePoints: List<WeakKnowledgePointDto> = emptyList(),
    @SerializedName("due_reviews") val dueReviews: List<WeeklyMistakeLinkDto> = emptyList(),
    @SerializedName("upload_success_rate") val uploadSuccessRate: Double,
    @SerializedName("ocr_correction_rate") val ocrCorrectionRate: Double,
    @SerializedName("practice_completion_rate") val practiceCompletionRate: Double,
    @SerializedName("authoritative_accuracy") val authoritativeAccuracy: Double,
    @SerializedName("second_attempt_accuracy") val secondAttemptAccuracy: Double,
    @SerializedName("seven_day_followup_rate") val sevenDayFollowupRate: Double,
)
