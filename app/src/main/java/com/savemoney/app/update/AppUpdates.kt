package com.savemoney.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.content.pm.PackageInfoCompat
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.coroutineContext

/**
 * 用家里的后端做应用内更新：`GET {serverUrl}/api/update/latest`。
 *
 * 发版时把 APK 放到服务器数据目录 `updates/`，并写 `latest.json`
 *（versionCode / versionName / filename / notes）。
 * [gradle.properties] 里的 `appVersionCode` 必须比手机上已装的更大，才能覆盖安装。
 */
data class InstalledVersion(
    val versionName: String,
    val versionCode: Int,
)

data class ServerLatest(
    val versionCode: Int = 0,
    val versionName: String? = null,
    val apkUrl: String? = null,
    val notes: String? = null,
)

data class AvailableUpdate(
    val versionName: String,
    val versionCode: Int,
    val apkUrl: String,
    val apkName: String,
    val notes: String?,
)

sealed class UpdateCheckResult {
    data class Available(val update: AvailableUpdate) : UpdateCheckResult()
    data object UpToDate : UpdateCheckResult()
    data class Failed(val message: String) : UpdateCheckResult()
}

object AppUpdates {
    private const val PREFS = "app_update"
    private const val SESSION_PREFS = "session"
    private const val DEFAULT_SERVER = "http://10.0.2.2:8080"
    private const val KEY_LAST_CHECK_MS = "last_check_ms"
    private const val KEY_CACHE_CODE = "cache_code"
    private const val KEY_CACHE_NAME = "cache_name"
    private const val KEY_CACHE_URL = "cache_url"
    private const val KEY_CACHE_APK = "cache_apk"
    private const val KEY_CACHE_NOTES = "cache_notes"
    private const val DAY_MS = 24 * 60 * 60 * 1000L
    private const val USER_AGENT = "watchMoney-android"

    private val gson = Gson()
    private val http by lazy {
        OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(5, TimeUnit.MINUTES)
            .writeTimeout(5, TimeUnit.MINUTES)
            .followRedirects(true)
            .followSslRedirects(true)
            .build()
    }

    fun configuredServerBase(context: Context): String =
        context.getSharedPreferences(SESSION_PREFS, Context.MODE_PRIVATE)
            .getString("serverUrl", DEFAULT_SERVER)!!
            .trim()
            .trimEnd('/')

    fun latestUrl(serverBase: String): String =
        "${serverBase.trim().trimEnd('/')}/api/update/latest"

    fun resolveUrl(serverBase: String, apkUrl: String): String {
        val url = apkUrl.trim()
        if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
            return url
        }
        val root = serverBase.trim().trimEnd('/')
        return if (url.startsWith("/")) "$root$url" else "$root/$url"
    }

    fun apkNameFromUrl(url: String): String {
        val path = url.substringBefore('?').trimEnd('/')
        val name = path.substringAfterLast('/')
        return if (name.endsWith(".apk", ignoreCase = true)) name else "saveMoney.apk"
    }

    fun resolveUpdate(latest: ServerLatest, currentVersionCode: Int, serverBase: String): UpdateCheckResult {
        val versionName = latest.versionName?.trim().orEmpty()
        val apkUrl = latest.apkUrl?.trim().orEmpty()
        if (latest.versionCode <= 0 || versionName.isEmpty() || apkUrl.isEmpty()) {
            return UpdateCheckResult.Failed("更新信息不完整")
        }
        if (latest.versionCode <= currentVersionCode) return UpdateCheckResult.UpToDate
        val resolved = resolveUrl(serverBase, apkUrl)
        return UpdateCheckResult.Available(
            AvailableUpdate(
                versionName = versionName,
                versionCode = latest.versionCode,
                apkUrl = resolved,
                apkName = apkNameFromUrl(resolved),
                notes = latest.notes?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
    }

    fun installedVersion(context: Context): InstalledVersion {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return InstalledVersion(
            versionName = info.versionName ?: "?",
            versionCode = PackageInfoCompat.getLongVersionCode(info).toInt(),
        )
    }

    suspend fun checkLatest(context: Context): UpdateCheckResult {
        val serverBase = configuredServerBase(context)
        val result = try {
            resolveUpdate(fetchLatest(serverBase), installedVersion(context).versionCode, serverBase)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            UpdateCheckResult.Failed(e.message ?: "检查更新失败")
        }
        when (result) {
            is UpdateCheckResult.Available -> saveCachedAvailable(context, result.update)
            UpdateCheckResult.UpToDate -> clearCachedAvailable(context)
            is UpdateCheckResult.Failed -> Unit
        }
        return result
    }

    suspend fun maybeCheckDaily(context: Context): UpdateCheckResult? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (now - prefs.getLong(KEY_LAST_CHECK_MS, 0L) < DAY_MS) return null
        val result = checkLatest(context)
        if (result !is UpdateCheckResult.Failed) {
            prefs.edit { putLong(KEY_LAST_CHECK_MS, now) }
        }
        return result
    }

    fun cachedAvailable(context: Context): AvailableUpdate? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val code = prefs.getInt(KEY_CACHE_CODE, 0)
        val name = prefs.getString(KEY_CACHE_NAME, null)
        val url = prefs.getString(KEY_CACHE_URL, null)
        val apk = prefs.getString(KEY_CACHE_APK, null)
        if (code <= 0 || name.isNullOrBlank() || url.isNullOrBlank() || apk.isNullOrBlank()) return null
        return AvailableUpdate(
            versionName = name,
            versionCode = code,
            apkUrl = url,
            apkName = apk,
            notes = prefs.getString(KEY_CACHE_NOTES, null),
        )
    }

    private fun saveCachedAvailable(context: Context, update: AvailableUpdate) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            putInt(KEY_CACHE_CODE, update.versionCode)
            putString(KEY_CACHE_NAME, update.versionName)
            putString(KEY_CACHE_URL, update.apkUrl)
            putString(KEY_CACHE_APK, update.apkName)
            putString(KEY_CACHE_NOTES, update.notes)
        }
    }

    private fun clearCachedAvailable(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit {
            remove(KEY_CACHE_CODE)
            remove(KEY_CACHE_NAME)
            remove(KEY_CACHE_URL)
            remove(KEY_CACHE_APK)
            remove(KEY_CACHE_NOTES)
        }
    }

    fun apkFile(context: Context): File = File(context.cacheDir, "updates/saveMoney-update.apk")

    fun canInstallPackages(context: Context): Boolean =
        context.packageManager.canRequestPackageInstalls()

    fun unknownSourcesIntent(context: Context): Intent =
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    suspend fun downloadApk(
        url: String,
        destFile: File,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/octet-stream")
            .build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("下载失败（${response.code}）")
            val body = response.body ?: throw IOException("下载空文件")
            val total = body.contentLength()
            destFile.parentFile?.mkdirs()
            val part = File(destFile.parentFile, "${destFile.name}.part")
            var downloaded = 0L
            var lastEmit = 0L
            body.byteStream().use { input ->
                part.outputStream().use { output ->
                    val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        coroutineContext.ensureActive()
                        val read = input.read(buf)
                        if (read == -1) break
                        output.write(buf, 0, read)
                        downloaded += read
                        val now = System.currentTimeMillis()
                        if (now - lastEmit >= 120L) {
                            lastEmit = now
                            withContext(Dispatchers.Main.immediate) { onProgress(downloaded, total) }
                        }
                    }
                }
            }
            if (destFile.exists() && !destFile.delete()) throw IOException("没法覆盖旧安装包")
            if (!part.renameTo(destFile)) throw IOException("保存安装包失败")
            withContext(Dispatchers.Main.immediate) { onProgress(downloaded, total) }
        }
    }

    fun installApk(context: Context, file: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private data class ErrorBody(val detail: String? = null)

    private suspend fun fetchLatest(serverBase: String): ServerLatest = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(latestUrl(serverBase))
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            val detail = runCatching {
                gson.fromJson(body, ErrorBody::class.java)?.detail?.trim()?.takeIf { it.isNotEmpty() }
            }.getOrNull()
            when (response.code) {
                200 -> gson.fromJson(body, ServerLatest::class.java)
                    ?: throw IOException("服务器返回空数据")
                404 -> throw IOException(detail ?: "还没有发布新版本")
                else -> throw IOException(detail ?: "检查更新失败（${response.code}）")
            }
        }
    }
}
