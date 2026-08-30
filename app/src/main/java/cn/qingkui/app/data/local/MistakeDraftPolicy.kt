package cn.qingkui.app.data.local

import java.io.File

internal fun mistakeDraftValidationError(
    imagePath: String,
    questionText: String,
    imageExists: (String) -> Boolean = { File(it).isFile },
): String? = when {
    imagePath.isNotBlank() && !imageExists(imagePath) -> "本地图片已不存在"
    imagePath.isBlank() && questionText.isBlank() -> "手动题目内容为空"
    else -> null
}
