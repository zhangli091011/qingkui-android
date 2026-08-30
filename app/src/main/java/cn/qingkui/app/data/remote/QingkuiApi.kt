package cn.qingkui.app.data.remote

import cn.qingkui.app.data.remote.dto.AuthResponse
import cn.qingkui.app.data.remote.dto.ConversationCreate
import cn.qingkui.app.data.remote.dto.ConversationDto
import cn.qingkui.app.data.remote.dto.ChangePasswordRequest
import cn.qingkui.app.data.remote.dto.CreditLedgerDto
import cn.qingkui.app.data.remote.dto.CreditDto
import cn.qingkui.app.data.remote.dto.DeviceSessionDto
import cn.qingkui.app.data.remote.dto.FeedbackCreate
import cn.qingkui.app.data.remote.dto.FeedbackDto
import cn.qingkui.app.data.remote.dto.KnowledgeNodeDto
import cn.qingkui.app.data.remote.dto.KnowledgeNodeDetailDto
import cn.qingkui.app.data.remote.dto.KnowledgeStateUpdate
import cn.qingkui.app.data.remote.dto.LearningEventCreate
import cn.qingkui.app.data.remote.dto.LearningEventRequest
import cn.qingkui.app.data.remote.dto.LearningSummaryItemDto
import cn.qingkui.app.data.remote.dto.LearningEventDto
import cn.qingkui.app.data.remote.dto.LearningCheckDto
import cn.qingkui.app.data.remote.dto.LearningCheckResultDto
import cn.qingkui.app.data.remote.dto.LearningCheckSubmitDto
import cn.qingkui.app.data.remote.dto.LearningSummaryDto
import cn.qingkui.app.data.remote.dto.LoginRequest
import cn.qingkui.app.data.remote.dto.LogoutRequest
import cn.qingkui.app.data.remote.dto.MessageCreate
import cn.qingkui.app.data.remote.dto.NeighborResponse
import cn.qingkui.app.data.remote.dto.QaResultDto
import cn.qingkui.app.data.remote.dto.RefreshRequest
import cn.qingkui.app.data.remote.dto.RegisterRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response
import retrofit2.http.Streaming
import okhttp3.ResponseBody
import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.Part
import cn.qingkui.app.data.remote.dto.MistakeCreateDto
import cn.qingkui.app.data.remote.dto.MistakeDto
import cn.qingkui.app.data.remote.dto.MistakeAnalysisResponseDto
import cn.qingkui.app.data.remote.dto.MistakePracticeDto
import cn.qingkui.app.data.remote.dto.OcrCorrectionDto
import cn.qingkui.app.data.remote.dto.OcrTaskDto
import cn.qingkui.app.data.remote.dto.PracticeSubmitDto

interface QingkuiApi {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): AuthResponse

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): AuthResponse

    @POST("auth/refresh")
    suspend fun refresh(@Body body: RefreshRequest): AuthResponse

    @POST("auth/logout")
    suspend fun logout(@Body body: LogoutRequest)

    @POST("auth/change-password")
    suspend fun changePassword(@Body body: ChangePasswordRequest)

    @GET("auth/sessions")
    suspend fun deviceSessions(): List<DeviceSessionDto>

    @DELETE("auth/sessions/{id}")
    suspend fun revokeDeviceSession(@Path("id") sessionId: String): retrofit2.Response<Unit>

    @DELETE("auth/me")
    suspend fun deleteAccount(): retrofit2.Response<Unit>

    @GET("credits")
    suspend fun credits(): CreditDto

    @GET("credits/ledger")
    suspend fun creditLedger(@Query("limit") limit: Int = 100): List<CreditLedgerDto>

    @GET("knowledge/search")
    suspend fun search(@Query("q") query: String): List<KnowledgeNodeDto>

    @GET("knowledge/nodes/{id}/neighbors")
    suspend fun neighbors(@Path("id") nodeId: String): NeighborResponse

    @GET("knowledge/nodes/{id}")
    suspend fun nodeDetail(@Path("id") nodeId: String): KnowledgeNodeDetailDto

    @PATCH("learning/nodes/{id}/state")
    suspend fun updateNodeState(@Path("id") nodeId: String, @Body body: KnowledgeStateUpdate): LearningSummaryItemDto

    @POST("learning/nodes/{id}/checks")
    suspend fun createLearningCheck(@Path("id") nodeId: String): LearningCheckDto

    @POST("learning/checks/{id}/submit")
    suspend fun submitLearningCheck(
        @Path("id") attemptId: String,
        @Body body: LearningCheckSubmitDto,
    ): LearningCheckResultDto

    @POST("qa/sessions")
    suspend fun createSession(@Body body: ConversationCreate): ConversationDto

    @GET("qa/sessions")
    suspend fun sessions(
        @Query("q") query: String? = null,
        @Query("limit") limit: Int = 50,
    ): List<ConversationDto>

    @GET("qa/sessions/{id}")
    suspend fun session(@Path("id") sessionId: String): ConversationDto

    @DELETE("qa/sessions/{id}")
    suspend fun deleteSession(@Path("id") sessionId: String): retrofit2.Response<Unit>

    @POST("qa/sessions/{id}/messages")
    suspend fun sendMessage(
        @Path("id") sessionId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: MessageCreate,
    ): QaResultDto

    @Streaming
    @POST("qa/sessions/{id}/messages/stream")
    suspend fun streamMessage(
        @Path("id") sessionId: String,
        @Header("Idempotency-Key") idempotencyKey: String,
        @Body body: MessageCreate,
    ): Response<ResponseBody>

    @POST("learning/events")
    suspend fun createLearningEvent(@Body body: LearningEventCreate): LearningEventDto

    @POST("learning/events")
    suspend fun recordLearningEvent(@Body body: LearningEventRequest): LearningEventDto

    @GET("learning/summary")
    suspend fun learningSummary(): LearningSummaryDto

    @POST("feedback")
    suspend fun submitFeedback(@Body body: FeedbackCreate): FeedbackDto

    @GET("feedback")
    suspend fun feedback(@Query("limit") limit: Int = 50): List<FeedbackDto>

    @POST("mistakes")
    suspend fun createMistake(@Body body: MistakeCreateDto): MistakeDto

    @GET("mistakes")
    suspend fun mistakes(@Query("limit") limit: Int = 100): List<MistakeDto>

    @DELETE("mistakes/{id}")
    suspend fun deleteMistake(@Path("id") mistakeId: String): retrofit2.Response<Unit>

    @Multipart
    @POST("mistakes/{id}/images")
    suspend fun uploadMistakeImage(
        @Path("id") mistakeId: String,
        @Part image: MultipartBody.Part,
    ): OcrTaskDto

    @POST("mistakes/{mistakeId}/ocr/{taskId}/confirm")
    suspend fun confirmMistakeOcr(
        @Path("mistakeId") mistakeId: String,
        @Path("taskId") taskId: String,
        @Body body: OcrCorrectionDto,
    ): MistakeDto

    @POST("mistakes/{mistakeId}/ocr/{taskId}/cancel")
    suspend fun cancelMistakeOcr(
        @Path("mistakeId") mistakeId: String,
        @Path("taskId") taskId: String,
    ): OcrTaskDto

    @POST("mistakes/{mistakeId}/ocr/{taskId}/retry")
    suspend fun retryMistakeOcr(
        @Path("mistakeId") mistakeId: String,
        @Path("taskId") taskId: String,
    ): OcrTaskDto

    @POST("mistakes/{id}/analyze")
    suspend fun analyzeMistake(@Path("id") mistakeId: String): MistakeAnalysisResponseDto

    @POST("mistakes/{id}/practices/generate")
    suspend fun generateMistakePractice(@Path("id") mistakeId: String): MistakePracticeDto

    @POST("mistakes/{mistakeId}/practices/{practiceId}/submit")
    suspend fun submitMistakePractice(
        @Path("mistakeId") mistakeId: String,
        @Path("practiceId") practiceId: String,
        @Body body: PracticeSubmitDto,
    ): MistakePracticeDto
}
