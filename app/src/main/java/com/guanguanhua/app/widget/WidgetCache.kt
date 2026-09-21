package com.guanguanhua.app.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.edit
import androidx.glance.appwidget.updateAll
import com.guanguanhua.app.data.WidgetDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class WidgetState(
    val imageUrl: String? = null,
    val caption: String = "",
    val captionColor: String = WidgetCopy.DEFAULT_CAPTION_COLOR,
    val updatedBy: String? = null,
    val updatedAt: Long? = null,
    val localImagePath: String? = null,
)

object WidgetCache {
    private const val PREFS = "widget"
    private const val KEY_IMAGE_URL = "imageUrl"
    private const val KEY_CAPTION = "caption"
    private const val KEY_CAPTION_COLOR = "captionColor"
    private const val KEY_UPDATED_BY = "updatedBy"
    private const val KEY_UPDATED_AT = "updatedAt"

    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    fun coverFile(context: Context): File = File(context.filesDir, "widget/cover.jpg")

    fun read(context: Context): WidgetState {
        val prefs = prefs(context)
        val imageUrl = prefs.getString(KEY_IMAGE_URL, null)
        val file = coverFile(context)
        return WidgetState(
            imageUrl = imageUrl,
            caption = prefs.getString(KEY_CAPTION, "").orEmpty(),
            captionColor = WidgetCopy.normalizeCaptionColor(prefs.getString(KEY_CAPTION_COLOR, null)),
            updatedBy = prefs.getString(KEY_UPDATED_BY, null),
            updatedAt = if (prefs.contains(KEY_UPDATED_AT)) prefs.getLong(KEY_UPDATED_AT, 0L) else null,
            localImagePath = if (!imageUrl.isNullOrBlank() && file.exists()) file.absolutePath else null,
        )
    }

    fun clear(context: Context) {
        coverFile(context).delete()
        prefs(context).edit(commit = true) { clear() }
    }

    fun decodeCover(context: Context): Bitmap? {
        val file = coverFile(context)
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        return BitmapFactory.decodeFile(
            file.absolutePath,
            BitmapFactory.Options().apply {
                inSampleSize = WidgetCopy.bitmapSampleSize(bounds.outWidth, bounds.outHeight)
            },
        )
    }

    suspend fun applyRemote(context: Context, remote: WidgetDto): WidgetState = withContext(Dispatchers.IO) {
        val prefs = prefs(context)
        val file = coverFile(context)
        val previousUrl = prefs.getString(KEY_IMAGE_URL, null)
        val remoteUrl = remote.widgetImageUrl
        if (remoteUrl.isNullOrBlank()) {
            if (file.exists()) file.delete()
        } else if (remoteUrl != previousUrl || !file.exists()) {
            file.parentFile?.mkdirs()
            client.newCall(Request.Builder().url(remoteUrl).build()).execute().use { response ->
                if (!response.isSuccessful) error("照片下载失败")
                val bytes = response.body?.bytes() ?: error("照片是空的")
                val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
                val bitmap = BitmapFactory.decodeByteArray(
                    bytes,
                    0,
                    bytes.size,
                    BitmapFactory.Options().apply {
                        inSampleSize = WidgetCopy.bitmapSampleSize(bounds.outWidth, bounds.outHeight)
                    },
                ) ?: error("照片打不开")
                FileOutputStream(file).use { out -> bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out) }
                bitmap.recycle()
            }
        }
        prefs.edit(commit = true) {
            putString(KEY_IMAGE_URL, remoteUrl)
            putString(KEY_CAPTION, remote.widgetCaption.orEmpty())
            putString(KEY_CAPTION_COLOR, WidgetCopy.normalizeCaptionColor(remote.widgetCaptionColor))
            putString(KEY_UPDATED_BY, remote.widgetUpdatedBy)
            val updatedAt = remote.widgetUpdatedAt
            if (updatedAt != null) putLong(KEY_UPDATED_AT, updatedAt) else remove(KEY_UPDATED_AT)
        }
        read(context)
    }

    suspend fun publish(context: Context) {
        HouseholdWidget().updateAll(context)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
