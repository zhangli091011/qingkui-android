package cn.qingkui.app.data.local

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_SOURCE_PIXELS = 100_000_000L

object MistakeImageProcessor {
    fun process(
        sourcePath: String,
        userRotationDegrees: Int,
        cropInsetFraction: Float,
        maxEdge: Int = 2048,
        jpegQuality: Int = 85,
    ): String {
        val source = File(sourcePath)
        require(source.isFile) { "图片文件不存在" }

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(source.absolutePath, bounds)
        require(bounds.outWidth > 0 && bounds.outHeight > 0) { "无法解析图片" }
        require(bounds.outWidth.toLong() * bounds.outHeight.toLong() <= MAX_SOURCE_PIXELS) { "图片像素尺寸过大" }
        var sampleSize = 1
        while (max(bounds.outWidth, bounds.outHeight) / sampleSize > maxEdge * 2) {
            sampleSize *= 2
        }
        val decoded = BitmapFactory.decodeFile(
            source.absolutePath,
            BitmapFactory.Options().apply { inSampleSize = sampleSize },
        ) ?: error("无法解码图片")

        var current = decoded
        try {
            val inset = cropInsetFraction.coerceIn(0f, 0.2f)
            if (inset > 0f) {
                val insetX = (current.width * inset).roundToInt().coerceAtMost((current.width - 1) / 2)
                val insetY = (current.height * inset).roundToInt().coerceAtMost((current.height - 1) / 2)
                current = replace(current) {
                    Bitmap.createBitmap(
                        current,
                        insetX,
                        insetY,
                        current.width - insetX * 2,
                        current.height - insetY * 2,
                    )
                }
            }

            val rotation = (exifRotation(source) + userRotationDegrees).mod(360)
            if (rotation != 0) {
                current = replace(current) {
                    Bitmap.createBitmap(
                        current,
                        0,
                        0,
                        current.width,
                        current.height,
                        Matrix().apply { postRotate(rotation.toFloat()) },
                        true,
                    )
                }
            }

            val longest = max(current.width, current.height)
            if (longest > maxEdge) {
                val scale = maxEdge.toFloat() / longest
                current = replace(current) {
                    Bitmap.createScaledBitmap(
                        current,
                        (current.width * scale).roundToInt().coerceAtLeast(1),
                        (current.height * scale).roundToInt().coerceAtLeast(1),
                        true,
                    )
                }
            }

            val output = File(source.parentFile, "${UUID.randomUUID()}-processed.jpg")
            FileOutputStream(output).use { stream ->
                check(current.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(70, 95), stream)) {
                    "图片压缩失败"
                }
                stream.fd.sync()
            }
            check(output.length() > 0) { "图片输出为空" }
            return output.absolutePath
        } finally {
            if (!current.isRecycled) current.recycle()
        }
    }

    private inline fun replace(source: Bitmap, create: () -> Bitmap): Bitmap {
        val result = create()
        if (result !== source && !source.isRecycled) source.recycle()
        return result
    }

    private fun exifRotation(source: File): Int = runCatching {
        when (ExifInterface(source.absolutePath).getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL,
        )) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }.getOrDefault(0)
}
