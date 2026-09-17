package com.savemoney.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {

    @Test
    fun latestReleaseUsesWatchMoneyRepo() {
        assertEquals("watchMoney", AppUpdates.GITHUB_REPO)
        assertEquals(
            "https://api.github.com/repos/anliluZoe/watchMoney/releases/latest",
            AppUpdates.LATEST_RELEASE_URL,
        )
    }

    @Test
    fun parsesTagWithVPrefix() {
        val version = AppUpdates.parseReleaseVersion("v1.1.0+2")
        assertEquals("1.1.0", version?.versionName)
        assertEquals(2, version?.versionCode)
    }

    @Test
    fun parsesTagWithoutVPrefix() {
        val version = AppUpdates.parseReleaseVersion("1.2.3+10")
        assertEquals("1.2.3", version?.versionName)
        assertEquals(10, version?.versionCode)
    }

    @Test
    fun parsesPrereleaseNameAndFindsVersionInReleaseTitle() {
        assertEquals(
            ReleaseVersion("1.1.0-beta.1", 3),
            AppUpdates.parseReleaseVersion("v1.1.0-beta.1+3"),
        )
        assertEquals(
            ReleaseVersion("1.1.0", 2),
            AppUpdates.parseReleaseVersion("untagged", "省钱助手 v1.1.0+2"),
        )
    }

    @Test
    fun rejectsMissingVersionCode() {
        assertNull(AppUpdates.parseReleaseVersion("v1.1.0"))
        assertNull(AppUpdates.parseReleaseVersion(""))
        assertNull(AppUpdates.parseReleaseVersion("latest"))
    }

    @Test
    fun prefersSaveMoneyApkThenDebug() {
        val mixed = listOf(
            "notes.txt" to "https://example/notes",
            "app-debug.apk" to "https://example/debug",
            "saveMoney.apk" to "https://example/named",
        )
        assertEquals("saveMoney.apk" to "https://example/named", AppUpdates.pickApkAsset(mixed))
        assertEquals(
            "app-debug.apk" to "https://example/debug",
            AppUpdates.pickApkAsset(mixed.filterNot { it.first.equals("saveMoney.apk", ignoreCase = true) }),
        )
    }

    @Test
    fun fallsBackToAnyApk() {
        val assets = listOf("readme.md" to "https://example/md", "household-debug.apk" to "https://example/apk")
        assertEquals("household-debug.apk" to "https://example/apk", AppUpdates.pickApkAsset(assets))
        assertNull(AppUpdates.pickApkAsset(listOf("notes.txt" to "https://example/notes")))
    }

    @Test
    fun resolveUpdateRequiresNewerVersionCodeAndApk() {
        val release = GithubRelease(
            tagName = "v1.2.0+5",
            name = "省钱助手 1.2.0",
            body = "修了点小毛病",
            assets = listOf(GithubAsset("saveMoney.apk", "https://example/saveMoney.apk")),
        )
        val available = AppUpdates.resolveUpdate(release, currentVersionCode = 2)
        assertTrue(available is UpdateCheckResult.Available)
        available as UpdateCheckResult.Available
        assertEquals("1.2.0", available.update.versionName)
        assertEquals(5, available.update.versionCode)
        assertEquals("https://example/saveMoney.apk", available.update.apkUrl)

        assertTrue(AppUpdates.resolveUpdate(release, currentVersionCode = 5) is UpdateCheckResult.UpToDate)
        assertTrue(AppUpdates.resolveUpdate(release, currentVersionCode = 6) is UpdateCheckResult.UpToDate)
        assertTrue(
            AppUpdates.resolveUpdate(release.copy(tagName = "v1.2.0"), currentVersionCode = 2)
                is UpdateCheckResult.Failed,
        )
        assertTrue(
            AppUpdates.resolveUpdate(release.copy(assets = emptyList()), currentVersionCode = 2)
                is UpdateCheckResult.Failed,
        )
    }

    @Test
    fun gsonMapsGithubLatestReleaseJson() {
        val json = """
            {
              "tag_name": "v1.1.0+2",
              "name": "省钱助手 1.1.0",
              "body": "覆盖安装用",
              "assets": [
                {"name": "app-debug.apk", "browser_download_url": "https://example/app-debug.apk"},
                {"name": "saveMoney.apk", "browser_download_url": "https://example/saveMoney.apk"}
              ]
            }
        """.trimIndent()
        val release = com.google.gson.Gson().fromJson(json, GithubRelease::class.java)
        val result = AppUpdates.resolveUpdate(release, currentVersionCode = 1)
        assertTrue(result is UpdateCheckResult.Available)
        result as UpdateCheckResult.Available
        assertEquals("1.1.0", result.update.versionName)
        assertEquals(2, result.update.versionCode)
        assertEquals("saveMoney.apk", result.update.apkName)
        assertEquals("https://example/saveMoney.apk", result.update.apkUrl)
    }
}
