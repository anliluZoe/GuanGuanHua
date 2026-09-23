package com.guanguanhua.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanguanhua.app.AppViewModel
import com.guanguanhua.app.GuanGuanHuaApp
import com.guanguanhua.app.data.ApiConfig
import com.guanguanhua.app.ui.theme.QTheme
import com.guanguanhua.app.update.AppUpdates
import com.guanguanhua.app.update.AvailableUpdate
import com.guanguanhua.app.update.UpdateCheckResult
import com.guanguanhua.app.update.UpdatePresentation
import com.guanguanhua.app.update.presentUpdate
import com.guanguanhua.app.widget.WidgetCopy
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: AppViewModel, onOpenWidget: () -> Unit = {}, onOpenEdit: () -> Unit = {}) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    val widget by viewModel.widget.collectAsStateWithLifecycle()
    val joinPicker by viewModel.joinPicker.collectAsStateWithLifecycle()
    var name by rememberSaveable(profile.name) { mutableStateOf(profile.name.ifBlank { "小明" }) }
    var householdCode by rememberSaveable { mutableStateOf("") }
    var serverUrl by rememberSaveable(session.serverUrl) { mutableStateOf(session.serverUrl) }
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
            subtitle = if (session.joined) {
                "${profile.name.ifBlank { "我" }} × ${profile.partnerName ?: "另一半"}"
            } else {
                "两个人的小金库 · 才不是腻歪呢"
            },
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
        if (!session.joined) {
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("家庭账本", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "不用填服务器。默认连家里那台，先起个名字就能建账本。每个家庭最多两个人。",
                    color = QTheme.colors.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                SoftField(value = name, onValueChange = { name = it }, label = "我的名字")
                Spacer(Modifier.height(16.dp))
                PillButton("创建家庭账本", enabled = !isBusy && name.trim().isNotBlank(), onClick = {
                    viewModel.consumeStatus()
                    viewModel.createHome(name)
                })
            }
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("已经有家庭码？", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "还没满两人就新建身份；已经有两个人了，会让你选其中一个进入，不会再加第三人。",
                    color = QTheme.colors.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(10.dp))
                SoftField(value = householdCode, onValueChange = { householdCode = it }, label = "6 位家庭码")
                Spacer(Modifier.height(12.dp))
                PillButton(
                    "加入",
                    filled = false,
                    enabled = !isBusy && name.trim().isNotBlank() && householdCode.trim().isNotBlank(),
                    onClick = {
                        viewModel.consumeStatus()
                        viewModel.joinHome(householdCode, name)
                    },
                )
            }
        } else {
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MemberAvatar(
                        name = profile.name,
                        presetId = profile.avatarPreset,
                        photoUrl = profile.avatarUrl,
                        size = 56.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        profile.name.ifBlank { "还没起名" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("我", color = QTheme.colors.muted, style = MaterialTheme.typography.labelMedium)
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    MemberAvatar(
                        name = profile.partnerName ?: "另一半",
                        presetId = profile.partnerAvatarPreset,
                        photoUrl = profile.partnerAvatarUrl,
                        fallbackPreset = AvatarIds.DOG,
                        size = 56.dp,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        profile.partnerName ?: "还没加入",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (profile.partnerName == null) QTheme.colors.muted else QTheme.colors.ink,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text("另一半", color = QTheme.colors.muted, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(16.dp))
            Text("家庭码", color = QTheme.colors.muted, style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(4.dp))
            CopyableHouseholdCode(session.householdCode) {
                viewModel.postStatus(HouseholdCodeCopy.SNACKBAR)
            }
            Spacer(Modifier.height(16.dp))
            PillButton("编辑资料", filled = false, onClick = onOpenEdit)
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            val hasPhoto = !widget.imageUrl.isNullOrBlank() || !widget.localImagePath.isNullOrBlank()
            Row(verticalAlignment = Alignment.CenterVertically) {
                PhotoSlot(
                    model = widget.imageUrl ?: widget.localImagePath,
                    modifier = Modifier
                        .width(96.dp)
                        .height(96.dp),
                    showEmpty = false,
                )
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("桌面组件", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        WidgetCopy.summaryLine(hasPhoto, widget.caption),
                        color = QTheme.colors.muted,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            PillButton("编辑桌面组件", filled = false, onClick = onOpenWidget)
        }
        }
        AppearancePicker(value = appearance, onChange = viewModel::setAppearance)
        UpdateCard()
        if (!session.joined) {
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("服务器地址", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    "调试用。一般不用改，默认已连家里的服务器。",
                    color = QTheme.colors.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(12.dp))
                SoftField(value = serverUrl, onValueChange = { serverUrl = it }, label = "API 地址")
                Spacer(Modifier.height(16.dp))
                PillButton(
                    "保存地址",
                    filled = false,
                    enabled = !isBusy && ApiConfig.resolvedServerUrl(serverUrl) != session.serverUrl,
                    onClick = { viewModel.setServerUrl(serverUrl) },
                )
            }
        }
        Spacer(Modifier.height(96.dp))
        }
    }

    val picker = joinPicker
    if (picker != null) {
        val colors = QTheme.colors
        ModalBottomSheet(
            onDismissRequest = { if (!isBusy) viewModel.dismissJoinPicker() },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = colors.paper,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("这个家庭已经有两个人了", style = MaterialTheme.typography.titleLarge)
                Text(
                    "选一个身份进入。不会新建第三人。",
                    color = colors.secondary,
                    style = MaterialTheme.typography.bodySmall,
                )
                picker.members.forEachIndexed { index, member ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(if (colors.isDark) colors.sandDeep else colors.chipWash)
                            .clickable(enabled = !isBusy) { viewModel.enterAsExistingMember(member.id) }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        MemberAvatar(
                            name = member.name,
                            presetId = member.avatarPreset,
                            photoUrl = member.avatarUrl,
                            fallbackPreset = if (index == 0) AvatarIds.CAT else AvatarIds.DOG,
                            size = 48.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(member.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "用这个身份进入",
                                color = colors.muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
                PillButton("先不了", filled = false, enabled = !isBusy, onClick = { viewModel.dismissJoinPicker() })
            }
        }
    }
}

@Composable
private fun UpdateCard() {
    val context = LocalContext.current
    val colors = QTheme.colors
    val scope = rememberCoroutineScope()
    val downloads = (context.applicationContext as GuanGuanHuaApp).updateDownloads
    val phase by downloads.phase.collectAsStateWithLifecycle()
    val installed = remember { AppUpdates.installedVersion(context) }
    var ui by remember { mutableStateOf<UpdatePresentation>(UpdatePresentation.Idle) }
    var confirmUpdate by remember { mutableStateOf<AvailableUpdate?>(null) }
    var needInstallPermission by remember { mutableStateOf<AvailableUpdate?>(null) }
    var awaitingInstallPermission by remember { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf<AvailableUpdate?>(null) }
    var resumeDownload by remember { mutableStateOf(false) }
    val shown = presentUpdate(ui, phase)
    val offer = when (shown) {
        is UpdatePresentation.Available -> shown.update
        is UpdatePresentation.Error -> shown.retry
        else -> null
    }
    val busy = shown is UpdatePresentation.Checking || shown is UpdatePresentation.Downloading

    fun startInstall(update: AvailableUpdate) {
        if (!AppUpdates.canInstallPackages(context)) {
            pendingDownload = update
            needInstallPermission = update
            return
        }
        downloads.request(update)
    }

    LaunchedEffect(Unit) {
        AppUpdates.cachedAvailable(context)?.let { cached ->
            if (cached.versionCode > installed.versionCode) {
                ui = UpdatePresentation.Available(cached)
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
        pendingDownload?.let { startInstall(it) }
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
            when (val state = shown) {
                UpdatePresentation.Idle -> "有新版本时会从当前服务器下载安装包。"
                UpdatePresentation.Checking -> "正在看看有没有新版本…"
                UpdatePresentation.UpToDate -> "已经是最新的啦。"
                is UpdatePresentation.Available -> "发现新版本 ${state.update.versionName}（内部号 ${state.update.versionCode}）"
                is UpdatePresentation.Downloading -> {
                    val percent = state.progress?.let { "${(it * 100).toInt()}%" }
                    if (percent == null) "正在下载…" else "正在下载 $percent"
                }
                is UpdatePresentation.Error -> state.message
            },
            style = MaterialTheme.typography.bodySmall,
            color = when (shown) {
                is UpdatePresentation.Error -> if (colors.isDark) colors.rose else colors.coral
                is UpdatePresentation.Available -> colors.sky
                else -> colors.muted
            },
        )
        val notes = (shown as? UpdatePresentation.Available)?.update?.notes
        if (!notes.isNullOrBlank()) {
            Spacer(Modifier.height(4.dp))
            Text(
                notes.lineSequence().take(4).joinToString("\n"),
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
        }
        if (shown is UpdatePresentation.Downloading) {
            Spacer(Modifier.height(12.dp))
            val progress = shown.progress
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
        if (offer != null) {
            PillButton(
                "下载并安装",
                enabled = !busy,
                onClick = { confirmUpdate = offer },
            )
            Spacer(Modifier.height(10.dp))
        }
        PillButton(
            if (shown is UpdatePresentation.Checking) "正在检查…" else "检查更新",
            filled = offer == null,
            enabled = !busy,
            onClick = {
                scope.launch {
                    ui = UpdatePresentation.Checking
                    ui = when (val result = AppUpdates.checkLatest(context)) {
                        is UpdateCheckResult.Available -> UpdatePresentation.Available(result.update)
                        UpdateCheckResult.UpToDate -> UpdatePresentation.UpToDate
                        is UpdateCheckResult.Failed -> UpdatePresentation.Error(result.message)
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
