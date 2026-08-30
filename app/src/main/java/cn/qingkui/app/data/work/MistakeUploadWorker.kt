package cn.qingkui.app.data.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import cn.qingkui.app.data.auth.TokenStore
import cn.qingkui.app.data.local.MistakeDatabase
import cn.qingkui.app.data.remote.NetworkModule
import cn.qingkui.app.data.remote.dto.MistakeCreateDto
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import retrofit2.HttpException
import java.io.File

class MistakeUploadWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val draftId = inputData.getString(KEY_DRAFT_ID) ?: return Result.failure()
        val dao = MistakeDatabase.get(applicationContext).drafts()
        val draft = dao.get(draftId) ?: return Result.success()
        val image = draft.imagePath.takeIf { it.isNotBlank() }?.let(::File)
        if (image != null && !image.isFile) {
            dao.updateStatus(draftId, "failed", "本地图片已不存在")
            return Result.failure()
        }
        if (image == null && draft.questionText.isBlank()) {
            dao.updateStatus(draftId, "failed", "手动题目内容为空")
            return Result.failure()
        }
        dao.updateStatus(draftId, "uploading", null)
        return try {
            val api = NetworkModule.create(TokenStore(applicationContext))
            val mistakeId = draft.remoteId ?: api.createMistake(
                MistakeCreateDto(
                    subject = draft.subject,
                    questionText = draft.questionText.ifBlank { null },
                    studentWork = draft.studentWork.ifBlank { null },
                    questionGoal = draft.questionGoal.ifBlank { "识别图片中的题目并分析错因" },
                ),
            ).id
            if (image == null) {
                dao.markUploaded(draftId, mistakeId, null)
                return Result.success()
            }
            val mime = when (image.extension.lowercase()) {
                "png" -> "image/png"
                "webp" -> "image/webp"
                else -> "image/jpeg"
            }
            val part = MultipartBody.Part.createFormData(
                "image",
                image.name,
                image.asRequestBody(mime.toMediaType()),
            )
            val task = api.uploadMistakeImage(mistakeId, part)
            dao.markUploaded(draftId, mistakeId, task.id)
            Result.success()
        } catch (error: Exception) {
            val retryable = error !is HttpException || error.code() >= 500 || error.code() == 408 || error.code() == 429
            dao.updateStatus(draftId, if (retryable) "waiting" else "failed", error.message?.take(240))
            if (retryable && runAttemptCount < 5) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val KEY_DRAFT_ID = "draft_id"
    }
}
