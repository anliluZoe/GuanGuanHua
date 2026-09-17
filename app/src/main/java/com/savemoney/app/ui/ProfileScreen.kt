package com.savemoney.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savemoney.app.AppViewModel
import com.savemoney.app.notify.ReviewActivityWorker
import com.savemoney.app.ui.theme.QTheme
import com.savemoney.app.update.AppUpdates
import com.savemoney.app.update.AvailableUpdate
import com.savemoney.app.update.UpdateCheckResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    var name by rememberSaveable(profile.name) { mutableStateOf(profile.name) }
    val context = LocalContext.current
    var notificationsOn by remember { mutableStateOf(ReviewActivityWorker.notificationsAllowed(context)) }
    val notificationSettings = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsOn = ReviewActivityWorker.notificationsAllowed(context)
        if (granted) {
            ReviewActivityWorker.enqueueSoon(context)
        } else {
            val activity = context as? Activity
            if (activity != null && !activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                context.startActivity(notificationSettings)
            }
        }
    }
    var showPicker by remember { mutableStateOf(false) }
    val avatarPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            viewModel.updateAvatarPhoto(uri)
            showPicker = false
        }
    }
    LifecycleResumeEffect(Unit) {
        notificationsOn = ReviewActivityWorker.notificationsAllowed(context)
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) { viewModel.refresh() }

    PullRefreshBox(
        isRefreshing = isRefreshing && isReady,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier
            .fillMaxSize()
            .background(QTheme.colors.screenGlow),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        Spacer(Modifier.height(12.dp))
        PageHeader(
            title = "我们",
            subtitle = "两个人的小金库 · 才不是腻歪呢",
            leading = {
                StackedAvatars(
                    meName = profile.name,
                    mePreset = profile.avatarPreset,
                    mePhotoUrl = profile.avatarUrl,
                    partnerName = profile.partnerName,
                    partnerPreset = profile.partnerAvatarPreset,
                    partnerPhotoUrl = profile.partnerAvatarUrl,
                )
            },
        )
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("我的头像", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MemberAvatar(
                    name = profile.name,
                    presetId = profile.avatarPreset,
                    photoUrl = profile.avatarUrl,
                    size = 56.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(profile.name.ifBlank { "还没起名" }, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(2.dp))
                    Text(
                        "预设或相册都行，对方那边也会同步",
                        color = QTheme.colors.muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                ChoiceChip("更换", selected = false, onClick = { showPicker = true })
            }
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("家庭码", style = MaterialTheme.typography.titleMedium, color = QTheme.colors.muted)
            Spacer(Modifier.height(6.dp))
            Text(
                session.householdCode.ifBlank { "还未加入" },
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFeatureSettings = "tnum",
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = QTheme.colors.sky,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "把码塞给另一部手机，启动页点「加入」——别搞丢了哦。",
                color = QTheme.colors.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(session.serverUrl, color = QTheme.colors.muted, style = MaterialTheme.typography.bodySmall)
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("谁来把关", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "谁想买都行，但另一半说了算哼。过了关的钱才会乖乖进账本。",
                style = MaterialTheme.typography.bodySmall,
                color = QTheme.colors.muted,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MemberAvatar(
                    name = profile.partnerName ?: "另一半",
                    presetId = profile.partnerAvatarPreset,
                    photoUrl = profile.partnerAvatarUrl,
                    fallbackPreset = AvatarIds.DOG,
                    size = 44.dp,
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("另一半", color = QTheme.colors.muted, style = MaterialTheme.typography.labelMedium)
                    Text(
                        profile.partnerName ?: "还没有人加入，把家庭码发给TA吧",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (profile.partnerName == null) QTheme.colors.muted else QTheme.colors.ink,
                    )
                }
            }
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("怎么称呼我", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            SoftField(value = name, onValueChange = { name = it }, label = "我的名字")
            Spacer(Modifier.height(16.dp))
            PillButton(
                "保存名字",
                enabled = !isBusy && name.trim().isNotBlank() && name.trim() != profile.name,
                onClick = { viewModel.updateName(name) },
            )
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("审核动态提醒", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("对方发起新申请、或审核了你的申请，都会通知你。", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "App 打开时每 30 秒自动刷新；放到后台后大约 20 秒会检查一次，之后约每 15 分钟再查。不依赖 Google 推送。",
                color = QTheme.colors.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "部分手机厂商会限制后台任务，若迟迟收不到，请在系统设置里允许本应用自启动/后台运行，并关掉电池优化。",
                color = QTheme.colors.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            if (!notificationsOn) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "系统通知现在是关着的，打开后才能收到提醒。",
                    color = if (QTheme.colors.isDark) QTheme.colors.rose else QTheme.colors.coral,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                PillButton("去开启通知", filled = false, onClick = {
                    val needsRuntime = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                    if (needsRuntime) {
                        askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        context.startActivity(notificationSettings)
                    }
                })
            }
        }
        AppearancePicker(value = appearance, onChange = viewModel::setAppearance)
        UpdateCard()
        PillButton("退出这个家庭账本", filled = false, enabled = !isBusy, onClick = { viewModel.leaveHome() })
        Spacer(Modifier.height(96.dp))
        }
    }

    if (showPicker) {
        val colors = QTheme.colors
        val sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        var selectedPreset by remember(profile.avatarPreset, profile.avatarUrl) {
            mutableStateOf(if (profile.avatarUrl.isNullOrBlank()) profile.avatarPreset else null)
        }
        ModalBottomSheet(
            onDismissRequest = { showPicker = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.paper,
            shape = sheetShape,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("换个头像呗", style = MaterialTheme.typography.titleLarge)
                Text(
                    "先挑预设，或从相册上传一张",
                    color = colors.secondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                AvatarIds.ALL.chunked(3).forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        row.forEach { id ->
                            val selected = selectedPreset == id
                            val tileShape = RoundedCornerShape(18.dp)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .clip(tileShape)
                                    .background(if (colors.isDark) colors.sandDeep else colors.chipWash)
                                    .border(2.dp, if (selected) colors.sky else colors.line, tileShape)
                                    .clickable { selectedPreset = id },
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    painter = painterResource(avatarPresetRes(id)),
                                    contentDescription = null,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.fillMaxSize().padding(6.dp),
                                )
                            }
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .drawBehind {
                            drawRoundRect(
                                color = colors.lineStrong,
                                style = Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(16f, 12f), 0f),
                                ),
                                cornerRadius = CornerRadius(28.dp.toPx()),
                            )
                        }
                        .clickable {
                            avatarPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "从相册上传",
                        color = colors.coral,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                PillButton("先这样", filled = false, onClick = {
                    val preset = selectedPreset
                    if (preset != null && AvatarIds.known(preset) &&
                        (preset != profile.avatarPreset || !profile.avatarUrl.isNullOrBlank())
                    ) {
                        viewModel.updateAvatarPreset(preset)
                    }
                    showPicker = false
                })
            }
        }
    }
}

private sealed interface UpdateUi {
    data object Idle : UpdateUi
    data object Checking : UpdateUi
    data object UpToDate : UpdateUi
    data class Available(val update: AvailableUpdate) : UpdateUi
    data class Downloading(val progress: Float?) : UpdateUi
    data class Error(val message: String) : UpdateUi
}

@Composable
private fun UpdateCard() {
    val context = LocalContext.current
    val colors = QTheme.colors
    val scope = rememberCoroutineScope()
    val installed = remember { AppUpdates.installedVersion(context) }
    var ui by remember { mutableStateOf<UpdateUi>(UpdateUi.Idle) }
    var confirmUpdate by remember { mutableStateOf<AvailableUpdate?>(null) }
    var needInstallPermission by remember { mutableStateOf<AvailableUpdate?>(null) }
    var awaitingInstallPermission by remember { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf<AvailableUpdate?>(null) }
    var resumeDownload by remember { mutableStateOf(false) }
    val busy = ui is UpdateUi.Checking || ui is UpdateUi.Downloading

    fun downloadAndInstall(update: AvailableUpdate) {
        scope.launch {
            ui = UpdateUi.Downloading(null)
            runCatching {
                val file = AppUpdates.apkFile(context)
                AppUpdates.downloadApk(update.apkUrl, file) { downloaded, total ->
                    ui = UpdateUi.Downloading(if (total > 0) downloaded.toFloat() / total else null)
                }
                AppUpdates.installApk(context, file)
                ui = UpdateUi.Available(update)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                ui = UpdateUi.Error(error.message ?: "下载失败")
            }
        }
    }

    fun startInstall(update: AvailableUpdate) {
        if (!AppUpdates.canInstallPackages(context)) {
            pendingDownload = update
            needInstallPermission = update
            return
        }
        downloadAndInstall(update)
    }

    LaunchedEffect(Unit) {
        AppUpdates.cachedAvailable(context)?.let { cached ->
            if (cached.versionCode > installed.versionCode) {
                ui = UpdateUi.Available(cached)
            }
        }
    }
    LifecycleResumeEffect(awaitingInstallPermission) {
        if (awaitingInstallPermission && AppUpdates.canInstallPackages(context)) {
            awaitingInstallPermission = false
            resumeDownload = true
        }
        onPauseOrDispose { }
    }
    LaunchedEffect(resumeDownload) {
        if (!resumeDownload) return@LaunchedEffect
        resumeDownload = false
        pendingDownload?.let { downloadAndInstall(it) }
        pendingDownload = null
    }

    SoftCard(modifier = Modifier.fillMaxWidth()) {
        Text("检查更新", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "当前版本 ${installed.versionName}（内部号 ${installed.versionCode}）",
            style = MaterialTheme.typography.bodySmall,
            color = colors.secondary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            when (val state = ui) {
                UpdateUi.Idle -> "有新版本时会从当前服务器下载安装包。"
                UpdateUi.Checking -> "正在看看有没有新版本…"
                UpdateUi.UpToDate -> "已经是最新的啦。"
                is UpdateUi.Available -> "发现新版本 ${state.update.versionName}（内部号 ${state.update.versionCode}）"
                is UpdateUi.Downloading -> {
                    val percent = state.progress?.let { "${(it * 100).toInt()}%" }
                    if (percent == null) "正在下载…" else "正在下载 $percent"
                }
                is UpdateUi.Error -> state.message
            },
            style = MaterialTheme.typography.bodySmall,
            color = when (ui) {
                is UpdateUi.Error -> if (colors.isDark) colors.rose else colors.coral
                is UpdateUi.Available -> colors.sky
                else -> colors.muted
            },
        )
        val notes = (ui as? UpdateUi.Available)?.update?.notes
        if (!notes.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                notes.lineSequence().take(4).joinToString("\n"),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
        if (ui is UpdateUi.Downloading) {
            Spacer(Modifier.height(12.dp))
            val progress = (ui as UpdateUi.Downloading).progress
            if (progress == null) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = colors.primaryButton,
                    trackColor = if (colors.isDark) colors.skySoft else colors.coralSoft,
                )
            } else {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(4.dp),
                    color = colors.primaryButton,
                    trackColor = if (colors.isDark) colors.skySoft else colors.coralSoft,
                )
            }
        }
        Spacer(Modifier.height(14.dp))
        if (ui is UpdateUi.Available) {
            PillButton(
                "下载并安装",
                enabled = !busy,
                onClick = { confirmUpdate = (ui as UpdateUi.Available).update },
            )
            Spacer(Modifier.height(10.dp))
        }
        PillButton(
            if (ui is UpdateUi.Checking) "正在检查…" else "检查更新",
            filled = ui !is UpdateUi.Available,
            enabled = !busy,
            onClick = {
                scope.launch {
                    ui = UpdateUi.Checking
                    ui = when (val result = AppUpdates.checkLatest(context)) {
                        is UpdateCheckResult.Available -> UpdateUi.Available(result.update)
                        UpdateCheckResult.UpToDate -> UpdateUi.UpToDate
                        is UpdateCheckResult.Failed -> UpdateUi.Error(result.message)
                    }
                }
            },
        )
    }

    confirmUpdate?.let { update ->
        AlertDialog(
            onDismissRequest = { if (!busy) confirmUpdate = null },
            title = { Text("安装 ${update.versionName}") },
            text = { Text("会从家里的服务器下载 APK，再打开系统安装界面。内部号 ${update.versionCode}，要比现在的 ${installed.versionCode} 大才能覆盖安装。") },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        confirmUpdate = null
                        startInstall(update)
                    },
                ) { Text("下载并安装", color = colors.primaryButton) }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { confirmUpdate = null }) { Text("先不了") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }
    needInstallPermission?.let {
        AlertDialog(
            onDismissRequest = { needInstallPermission = null },
            title = { Text("允许安装未知应用") },
            text = { Text("要装新版本，得先允许「管管花」安装未知应用。系统设置里打开一下就好。") },
            confirmButton = {
                TextButton(onClick = {
                    needInstallPermission = null
                    awaitingInstallPermission = true
                    context.startActivity(AppUpdates.unknownSourcesIntent(context))
                }) { Text("去设置", color = colors.primaryButton) }
            },
            dismissButton = {
                TextButton(onClick = { needInstallPermission = null }) { Text("先不了") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }
}
