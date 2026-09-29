package com.guanguanhua.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt

data class JpegUpload(
    val bytes: ByteArray,
    val filename: String,
    val mimeType: String = ImageCompress.MIME_JPEG,
)

/** 上传前把相册图压成 JPEG：限边、白底填透明、固定 MIME。 */
object ImageCompress {
    const val PHOTO_MAX_EDGE = 1280
    const val AVATAR_MAX_EDGE = 1024
    const val JPEG_QUALITY = 85
    const val MIME_JPEG = "image/jpeg"

    fun scaledSize(width: Int, height: Int, maxEdge: Int): Pair<Int, Int> {
        if (width <= 0 || height <= 0 || maxEdge <= 0) return 1 to 1
        val longest = max(width, height)
        if (longest <= maxEdge) return width to height
        val scale = maxEdge.toDouble() / longest.toDouble()
        return (width * scale).roundToInt().coerceAtLeast(1) to
            (height * scale).roundToInt().coerceAtLeast(1)
    }

    fun decodeSampleSize(width: Int, height: Int, maxEdge: Int): Int {
        if (width <= 0 || height <= 0 || maxEdge <= 0) return 1
        var sample = 1
        val longest = max(width, height)
        while (longest / (sample * 2) >= maxEdge) sample *= 2
        return sample
    }

    fun compress(context: Context, uri: Uri, maxEdge: Int, filename: String): JpegUpload {
        // 相册选择器的 content URI 往往只能读一次，而且相机 JPEG 的 EXIF 经常超过
        // BitmapFactory.decodeStream 的 16KB 缓冲，直接解码会得到 0×0，于是报「读不到这张照片」。
        val temp = File.createTempFile("upload-", ".img", context.cacheDir)
        try {
            val copied = runCatching {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    temp.outputStream().use { output -> input.copyTo(output) }
                }
            }.getOrNull()
            if (copied == null || temp.length() <= 0L) error("读不到这张照片")
            val bitmap = decodeUploadBitmap(temp, maxEdge) ?: error("读不到这张照片")
            val opaque = flattenOnWhite(bitmap)
            if (opaque !== bitmap) bitmap.recycle()
            val out = ByteArrayOutputStream()
            val ok = opaque.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            opaque.recycle()
            if (!ok || out.size() == 0) error("照片压缩失败")
            return JpegUpload(bytes = out.toByteArray(), filename = filename)
        } finally {
            temp.delete()
        }
    }

    private fun decodeUploadBitmap(file: File, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        val srcW = bounds.outWidth
        val srcH = bounds.outHeight
        val mime = bounds.outMimeType.orEmpty().lowercase()
        if (srcW <= 0 || srcH <= 0 || mime.contains("heif") || mime.contains("heic")) {
            return decodeWithImageDecoder(file, maxEdge)
        }
        val orientation = readOrientation(file)
        val swapped = orientation == 90 || orientation == 270
        val (orientedW, orientedH) = if (swapped) srcH to srcW else srcW to srcH
        val (targetW, targetH) = scaledSize(orientedW, orientedH, maxEdge)
        val decoded = BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = decodeSampleSize(srcW, srcH, maxEdge)
                inPreferredConfig = Bitmap.Config.ARGB_8888
            },
        ) ?: return decodeWithImageDecoder(file, maxEdge)
        val oriented = applyOrientation(decoded, orientation)
        if (oriented !== decoded) decoded.recycle()
        if (oriented.width == targetW && oriented.height == targetH) return oriented
        return Bitmap.createScaledBitmap(oriented, targetW, targetH, true).also { scaled ->
            if (scaled !== oriented) oriented.recycle()
        }
    }

    private fun decodeWithImageDecoder(file: File, maxEdge: Int): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return runCatching {
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(file)) { decoder, info, _ ->
                val (width, height) = scaledSize(info.size.width, info.size.height, maxEdge)
                decoder.setTargetSize(width, height)
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }.getOrNull()
    }

    internal fun flattenOnWhite(src: Bitmap): Bitmap {
        if (!src.hasAlpha()) return src
        val flat = Bitmap.createBitmap(src.width, src.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(flat)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(src, 0f, 0f, null)
        return flat
    }

    private fun readOrientation(file: File): Int {
        val exif = runCatching { ExifInterface(file.absolutePath) }.getOrNull() ?: return 0
        return when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
    }

    private fun applyOrientation(src: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return src
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(src, 0, 0, src.width, src.height, matrix, true)
    }
}
