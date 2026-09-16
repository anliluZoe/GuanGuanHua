package com.savemoney.app.update

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import androidx.core.content.edit
import androidx.core.content.pm.PackageInfoCompat
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
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
 * GitHub Releases 应用内更新。
 *
 * 发版约定（给打包的人）：
 * - tag / 发布标题写成 `v{versionName}+{versionCode}`，例如 `v1.1.0+2`
 * - 挂上 APK，优先命名 `saveMoney.apk`，其次 `app-debug.apk`
 * - [gradle.properties] 里的 `appVersionCode` 必须比手机上已装的更大，才能覆盖安装
 */
data class ReleaseVersion(
    val versionName: String,
    val versionCode: Int,
)

data class GithubAsset(
    val name: String? = null,
    @SerializedName("browser_download_url") val browserDownloadUrl: String? = null,
)

data class GithubRelease(
    @SerializedName("tag_name") val tagName: String? = null,
    val name: String? = null,
    val body: String? = null,
    val assets: List<GithubAsset>? = null,
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
    const val GITHUB_OWNER = "anliluZoe"
    const val GITHUB_REPO = "saveMoney"
    const val LATEST_RELEASE_URL =
        "https://api.github.com/repos/$GITHUB_OWNER/$GITHUB_REPO/releases/latest"

    private const val PREFS = "app_update"
    private const val KEY_LAST_CHECK_MS = "last_check_ms"
    private const val KEY_CACHE_CODE = "cache_code"
    private const val KEY_CACHE_NAME = "cache_name"
    private const val KEY_CACHE_URL = "cache_url"
    private const val KEY_CACHE_APK = "cache_apk"
    private const val KEY_CACHE_NOTES = "cache_notes"
    private const val DAY_MS = 24 * 60 * 60 * 1000L
    private const val USER_AGENT = "saveMoney-android"
    private val preferredApkNames = listOf("savemoney.apk", "app-debug.apk", "app-release.apk")
    private val versionInText = Regex("""v?(\d+[0-9A-Za-z.\-]*)\+(\d+)""")

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

    fun parseReleaseVersion(tagName: String, releaseName: String? = null): ReleaseVersion? {
        listOf(tagName, releaseName.orEmpty()).forEach { raw ->
            val text = raw.trim()
            if (text.isEmpty()) return@forEach
            val match = versionInText.find(text) ?: return@forEach
            val name = match.groupValues[1]
            val code = match.groupValues[2].toIntOrNull() ?: return@forEach
            if (name.isNotBlank() && code > 0) return ReleaseVersion(name, code)
        }
        return null
    }

    fun pickApkAsset(assets: List<Pair<String, String>>): Pair<String, String>? {
        val apks = assets.filter { it.first.endsWith(".apk", ignoreCase = true) }
        if (apks.isEmpty()) return null
        preferredApkNames.forEach { preferred ->
            apks.firstOrNull { it.first.equals(preferred, ignoreCase = true) }?.let { return it }
        }
        return apks.first()
    }

    fun resolveUpdate(release: GithubRelease, currentVersionCode: Int): UpdateCheckResult {
        val version = parseReleaseVersion(release.tagName.orEmpty(), release.name)
            ?: return UpdateCheckResult.Failed("发布 tag 要写成 v1.1.0+2 这种格式")
        val apk = pickApkAsset(
            release.assets.orEmpty().mapNotNull { asset ->
                val name = asset.name?.trim().orEmpty()
                val url = asset.browserDownloadUrl?.trim().orEmpty()
                if (name.isEmpty() || url.isEmpty()) null else name to url
            },
        ) ?: return UpdateCheckResult.Failed("这个版本还没挂上 APK")
        if (version.versionCode <= currentVersionCode) return UpdateCheckResult.UpToDate
        return UpdateCheckResult.Available(
            AvailableUpdate(
                versionName = version.versionName,
                versionCode = version.versionCode,
                apkUrl = apk.second,
                apkName = apk.first,
                notes = release.body?.trim()?.takeIf { it.isNotEmpty() },
            ),
        )
    }

    fun installedVersion(context: Context): ReleaseVersion {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        return ReleaseVersion(
            versionName = info.versionName ?: "?",
            versionCode = PackageInfoCompat.getLongVersionCode(info).toInt(),
        )
    }

    suspend fun checkLatest(context: Context): UpdateCheckResult {
        val result = try {
            resolveUpdate(fetchLatestRelease(), installedVersion(context).versionCode)
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

    private suspend fun fetchLatestRelease(): GithubRelease = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/vnd.github+json")
            .build()
        http.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            when (response.code) {
                200 -> gson.fromJson(body, GithubRelease::class.java)
                    ?: throw IOException("GitHub 返回空数据")
                404 -> throw IOException("还没有正式发布")
                403, 429 -> throw IOException("GitHub 有点忙，过一会再试")
                else -> throw IOException("检查更新失败（${response.code}）")
            }
        }
    }
}
