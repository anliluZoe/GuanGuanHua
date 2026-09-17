package com.savemoney.app.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateTest {

    private val productionBase = "http://8.153.195.112:8080"

    @Test
    fun latestUrlUsesConfiguredServerBase() {
        assertEquals(
            "http://8.153.195.112:8080/api/update/latest",
            AppUpdates.latestUrl("http://8.153.195.112:8080/"),
        )
        assertEquals(
            "http://10.0.2.2:8080/api/update/latest",
            AppUpdates.latestUrl("http://10.0.2.2:8080"),
        )
    }

    @Test
    fun resolveUrlJoinsRelativePathsAndKeepsAbsolute() {
        assertEquals(
            "http://8.153.195.112:8080/api/update/download/saveMoney.apk",
            AppUpdates.resolveUrl(productionBase, "/api/update/download/saveMoney.apk"),
        )
        assertEquals(
            "http://8.153.195.112:8080/updates/app-debug.apk",
            AppUpdates.resolveUrl("$productionBase/", "updates/app-debug.apk"),
        )
        assertEquals(
            "https://cdn.example/saveMoney.apk",
            AppUpdates.resolveUrl(productionBase, "https://cdn.example/saveMoney.apk"),
        )
    }

    @Test
    fun gsonMapsLatestJsonAndResolvesRelativeApkUrl() {
        val json = """
            {
              "versionCode": 3,
              "versionName": "1.2.0",
              "apkUrl": "/api/update/download/saveMoney.apk",
              "notes": "修了点小毛病"
            }
        """.trimIndent()
        val latest = com.google.gson.Gson().fromJson(json, ServerLatest::class.java)
        val result = AppUpdates.resolveUpdate(latest, currentVersionCode = 2, serverBase = productionBase)
        assertTrue(result is UpdateCheckResult.Available)
        result as UpdateCheckResult.Available
        assertEquals("1.2.0", result.update.versionName)
        assertEquals(3, result.update.versionCode)
        assertEquals("saveMoney.apk", result.update.apkName)
        assertEquals(
            "http://8.153.195.112:8080/api/update/download/saveMoney.apk",
            result.update.apkUrl,
        )
        assertEquals("修了点小毛病", result.update.notes)
    }

    @Test
    fun resolveUpdateRequiresNewerVersionCode() {
        val latest = ServerLatest(
            versionCode = 5,
            versionName = "1.2.0",
            apkUrl = "/api/update/download/saveMoney.apk",
            notes = "修了点小毛病",
        )
        val available = AppUpdates.resolveUpdate(latest, currentVersionCode = 2, serverBase = productionBase)
        assertTrue(available is UpdateCheckResult.Available)
        available as UpdateCheckResult.Available
        assertEquals(5, available.update.versionCode)

        assertTrue(
            AppUpdates.resolveUpdate(latest, currentVersionCode = 5, serverBase = productionBase)
                is UpdateCheckResult.UpToDate,
        )
        assertTrue(
            AppUpdates.resolveUpdate(latest, currentVersionCode = 6, serverBase = productionBase)
                is UpdateCheckResult.UpToDate,
        )
    }

    @Test
    fun incompleteLatestPayloadFails() {
        assertTrue(
            AppUpdates.resolveUpdate(
                ServerLatest(versionCode = 0, versionName = "1.2.0", apkUrl = "/x.apk"),
                currentVersionCode = 1,
                serverBase = productionBase,
            ) is UpdateCheckResult.Failed,
        )
        assertTrue(
            AppUpdates.resolveUpdate(
                ServerLatest(versionCode = 3, versionName = "  ", apkUrl = "/x.apk"),
                currentVersionCode = 1,
                serverBase = productionBase,
            ) is UpdateCheckResult.Failed,
        )
        assertTrue(
            AppUpdates.resolveUpdate(
                ServerLatest(versionCode = 3, versionName = "1.2.0", apkUrl = ""),
                currentVersionCode = 1,
                serverBase = productionBase,
            ) is UpdateCheckResult.Failed,
        )
    }

    @Test
    fun apkNameFallsBackWhenUrlHasNoApk() {
        assertEquals("saveMoney.apk", AppUpdates.apkNameFromUrl("http://host/api/update/download"))
        assertEquals("household-debug.apk", AppUpdates.apkNameFromUrl("/api/update/download/household-debug.apk?x=1"))
    }
}
