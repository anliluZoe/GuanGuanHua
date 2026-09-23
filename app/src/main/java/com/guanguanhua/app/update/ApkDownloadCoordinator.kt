package com.guanguanhua.app.update

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.coroutines.cancellation.CancellationException

/**
 * 安装包下载挂在进程级作用域上，不跟「我们」页的组合函数一起销毁。
 * 同一版本的 APK 同时只会有一次下载；回到页面时读 [phase] 即可看到当前进度。
 */
class ApkDownloadCoordinator(
    private val scope: CoroutineScope,
    private val download: suspend (update: AvailableUpdate, onProgress: (downloaded: Long, total: Long) -> Unit) -> Unit,
    private val install: (AvailableUpdate) -> Unit,
) {
    private val lock = Any()
    private val _phase = MutableStateFlow<ApkDownloadPhase>(ApkDownloadPhase.Idle)
    val phase: StateFlow<ApkDownloadPhase> = _phase.asStateFlow()

    @Volatile
    private var activeGeneration = 0L
    private var job: Job? = null

    fun request(update: AvailableUpdate): DownloadStart {
        val started = synchronized(lock) {
            val phase = _phase.value
            val running = phase as? ApkDownloadPhase.Running
            if (running != null && running.update.samePackage(update) && job?.isCompleted == false) {
                return DownloadStart.AlreadyRunning
            }
            val ready = phase as? ApkDownloadPhase.Ready
            if (ready != null && ready.update.samePackage(update)) {
                return@synchronized null
            }
            val gen = activeGeneration + 1
            activeGeneration = gen
            job?.cancel()
            _phase.value = ApkDownloadPhase.Running(update, progress = null)
            val launched = scope.launch(start = CoroutineStart.LAZY) {
                try {
                    download(update) { downloaded, total ->
                        if (activeGeneration != gen) return@download
                        _phase.value = ApkDownloadPhase.Running(
                            update,
                            progress = if (total > 0L) downloaded.toFloat() / total.toFloat() else null,
                        )
                    }
                    if (activeGeneration != gen) return@launch
                    _phase.value = ApkDownloadPhase.Ready(update)
                    install(update)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    if (activeGeneration == gen) {
                        _phase.value = ApkDownloadPhase.Failed(update, e.message ?: "下载失败")
                    }
                }
            }
            job = launched
            launched
        }
        if (started == null) {
            runCatching { install(update) }.onFailure { error ->
                _phase.value = ApkDownloadPhase.Failed(update, error.message ?: "无法打开安装")
            }
            return DownloadStart.Reinstall
        }
        started.start()
        return DownloadStart.Started
    }
}

sealed class ApkDownloadPhase {
    data object Idle : ApkDownloadPhase()
    data class Running(val update: AvailableUpdate, val progress: Float?) : ApkDownloadPhase()
    data class Ready(val update: AvailableUpdate) : ApkDownloadPhase()
    data class Failed(val update: AvailableUpdate, val message: String) : ApkDownloadPhase()
}

sealed class DownloadStart {
    data object Started : DownloadStart()
    data object AlreadyRunning : DownloadStart()
    data object Reinstall : DownloadStart()
}

/** 「我们」页本地的检查结果。下载进度不放在这里，避免离开页面后被丢掉。 */
sealed class UpdatePresentation {
    data object Idle : UpdatePresentation()
    data object Checking : UpdatePresentation()
    data object UpToDate : UpdatePresentation()
    data class Available(val update: AvailableUpdate) : UpdatePresentation()
    data class Downloading(val progress: Float?) : UpdatePresentation()
    data class Error(val message: String, val retry: AvailableUpdate? = null) : UpdatePresentation()
}

/**
 * 把检查结果和进程级下载阶段合成界面状态。
 * 页面重新进入时检查状态会回到 [UpdatePresentation.Idle]，进行中的下载仍显示进度，不会当成一次新的下载。
 */
fun presentUpdate(check: UpdatePresentation, phase: ApkDownloadPhase): UpdatePresentation {
    if (phase is ApkDownloadPhase.Running) {
        return UpdatePresentation.Downloading(phase.progress)
    }
    if (check is UpdatePresentation.Checking) return check
    return when (phase) {
        is ApkDownloadPhase.Failed -> when (check) {
            is UpdatePresentation.UpToDate -> check
            else -> UpdatePresentation.Error(phase.message, retry = phase.update)
        }
        is ApkDownloadPhase.Ready -> when (check) {
            is UpdatePresentation.UpToDate -> check
            else -> UpdatePresentation.Available(phase.update)
        }
        else -> check
    }
}

private fun AvailableUpdate.samePackage(other: AvailableUpdate): Boolean =
    apkUrl == other.apkUrl && versionCode == other.versionCode
