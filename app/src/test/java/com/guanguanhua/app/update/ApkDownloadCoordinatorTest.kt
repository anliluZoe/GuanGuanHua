package com.guanguanhua.app.update

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class ApkDownloadCoordinatorTest {

    @Test
    fun sameApkDoesNotStartASecondDownloadAndKeepsProgress() = runBlocking {
        val calls = AtomicInteger()
        val gate = CompletableDeferred<Unit>()
        var installed = 0
        val parent = SupervisorJob()
        val scope = CoroutineScope(parent + Dispatchers.Unconfined)
        val update = sampleUpdate()
        val coordinator = ApkDownloadCoordinator(
            scope = scope,
            download = { _: AvailableUpdate, onProgress: (Long, Long) -> Unit ->
                calls.incrementAndGet()
                onProgress(40, 100)
                gate.await()
            },
            install = { installed += 1 },
        )

        assertEquals(DownloadStart.Started, coordinator.request(update))
        assertEquals(DownloadStart.AlreadyRunning, coordinator.request(update))
        assertEquals(1, calls.get())
        val running = coordinator.phase.value as ApkDownloadPhase.Running
        assertEquals(update, running.update)
        assertEquals(0.4f, running.progress)
        assertEquals(running, coordinator.phase.value)

        gate.complete(Unit)
        assertTrue(coordinator.phase.value is ApkDownloadPhase.Ready)
        assertEquals(1, installed)
        assertEquals(DownloadStart.Reinstall, coordinator.request(update))
        assertEquals(1, calls.get())
        assertEquals(2, installed)
        parent.cancel()
    }

    @Test
    fun returningToTheScreenShowsInFlightProgressInsteadOfStartingOver() {
        val update = sampleUpdate()
        val shown = presentUpdate(
            check = UpdatePresentation.Idle,
            phase = ApkDownloadPhase.Running(update, progress = 0.4f),
        )
        assertTrue(shown is UpdatePresentation.Downloading)
        assertEquals(0.4f, (shown as UpdatePresentation.Downloading).progress)
        assertTrue(
            presentUpdate(
                check = UpdatePresentation.Available(update),
                phase = ApkDownloadPhase.Running(update, progress = 0.4f),
            ) is UpdatePresentation.Downloading,
        )
    }

    @Test
    fun failedDownloadCanBeRequestedAgain() = runBlocking {
        val calls = AtomicInteger()
        val parent = SupervisorJob()
        val scope = CoroutineScope(parent + Dispatchers.Unconfined)
        val update = sampleUpdate()
        val coordinator = ApkDownloadCoordinator(
            scope = scope,
            download = { _: AvailableUpdate, _: (Long, Long) -> Unit ->
                if (calls.incrementAndGet() == 1) error("网络断了")
            },
            install = {},
        )

        assertEquals(DownloadStart.Started, coordinator.request(update))
        val failed = coordinator.phase.value as ApkDownloadPhase.Failed
        assertEquals("网络断了", failed.message)
        val shown = presentUpdate(UpdatePresentation.Available(update), coordinator.phase.value)
        assertTrue(shown is UpdatePresentation.Error)
        assertEquals(update, (shown as UpdatePresentation.Error).retry)

        assertEquals(DownloadStart.Started, coordinator.request(update))
        assertEquals(2, calls.get())
        assertTrue(coordinator.phase.value is ApkDownloadPhase.Ready)
        parent.cancel()
    }

    @Test
    fun differentApkReplacesTheInFlightDownload() = runBlocking {
        val calls = mutableListOf<String>()
        val gate = CompletableDeferred<Unit>()
        val parent = SupervisorJob()
        val scope = CoroutineScope(parent + Dispatchers.Unconfined)
        val first = sampleUpdate(url = "http://host/a.apk", code = 2)
        val second = sampleUpdate(url = "http://host/b.apk", code = 3)
        val coordinator = ApkDownloadCoordinator(
            scope = scope,
            download = { update: AvailableUpdate, _: (Long, Long) -> Unit ->
                calls.add(update.apkUrl)
                if (update.apkUrl.endsWith("a.apk")) gate.await()
            },
            install = {},
        )

        coordinator.request(first)
        coordinator.request(second)
        assertEquals(listOf("http://host/a.apk", "http://host/b.apk"), calls)
        val ready = coordinator.phase.value as ApkDownloadPhase.Ready
        assertEquals(second.apkUrl, ready.update.apkUrl)
        parent.cancel()
    }

    private fun sampleUpdate(
        url: String = "http://8.153.195.112:8080/api/update/download/saveMoney.apk",
        code: Int = 9,
    ) = AvailableUpdate(
        versionName = "1.4.0",
        versionCode = code,
        apkUrl = url,
        apkName = "saveMoney.apk",
        notes = null,
    )
}
