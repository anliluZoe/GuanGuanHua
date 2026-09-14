package com.savemoney.app.data

import android.content.Context
import android.net.Uri
import java.io.File
import java.util.UUID

/** 把相册/拍照得到的图片复制到应用私有目录，路径写入申请记录。 */
class PhotoStore(private val context: Context) {

    fun save(from: Uri): String? {
        val dir = File(context.filesDir, "photos").apply { mkdirs() }
        val dest = File(dir, "${UUID.randomUUID()}.jpg")
        context.contentResolver.openInputStream(from)?.use { input ->
            dest.outputStream().use { input.copyTo(it) }
        } ?: return null
        return dest.absolutePath
    }

    fun delete(path: String?) {
        if (path.isNullOrBlank()) return
        File(path).takeIf { it.exists() }?.delete()
    }
}
