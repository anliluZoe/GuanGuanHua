package com.guanguanhua.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanguanhua.app.AppViewModel
import com.guanguanhua.app.data.ApiConfig
import com.guanguanhua.app.notify.ReviewActivityWorker
import com.guanguanhua.app.ui.theme.QTheme
import com.guanguanhua.app.widget.WidgetCopy

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileEditScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    var name by rememberSaveable(profile.name) { mutableStateOf(profile.name.ifBlank { "小明" }) }
    var serverUrl by rememberSaveable(session.serverUrl) { mutableStateOf(session.serverUrl) }
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
    var confirmLeave by remember { mutableStateOf(false) }
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QTheme.colors.screenGlow)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader("编辑资料", "改头像和名字，保存后回到「我们」", onBack = onBack)
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
            Text("怎么称呼我", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            SoftField(value = name, onValueChange = { name = it }, label = "我的名字")
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("家庭码", style = MaterialTheme.typography.titleMedium, color = QTheme.colors.muted)
            Spacer(Modifier.height(6.dp))
            CopyableHouseholdCode(session.householdCode) {
                viewModel.postStatus(HouseholdCodeCopy.SNACKBAR)
            }
            Spacer(Modifier.height(8.dp))
            Text(
                "家庭码不能改。把码发给另一半，打开「我们」页加入。已经有两个人时，对方会选一个身份进入。",
                color = QTheme.colors.muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        PillButton(
            "保存",
            enabled = !isBusy && name.trim().isNotBlank(),
            onClick = {
                if (name.trim() == profile.name) {
                    onBack()
                } else {
                    viewModel.updateName(name, onSuccess = onBack)
                }
            },
        )
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
        if (session.joined) {
            PillButton("退出这个家庭账本", filled = false, enabled = !isBusy, onClick = {
                confirmLeave = true
            })
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmLeave) {
        val colors = QTheme.colors
        AlertDialog(
            onDismissRequest = { if (!isBusy) confirmLeave = false },
            title = { Text(LeaveHouseholdPrompt.TITLE) },
            text = { Text(LeaveHouseholdPrompt.BODY) },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        confirmLeave = false
                        if (LeaveHouseholdPrompt.afterChoice(confirmed = true) == LeaveHouseholdChoice.Leave) {
                            viewModel.leaveHome(onSuccess = onBack)
                        }
                    },
                ) {
                    Text(
                        LeaveHouseholdPrompt.CONFIRM,
                        color = if (colors.isDark) colors.rose else colors.coral,
                    )
                }
            },
            dismissButton = {
                TextButton(enabled = !isBusy, onClick = { confirmLeave = false }) {
                    Text(LeaveHouseholdPrompt.CANCEL)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
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
                Text(
                    WidgetCopy.COMPRESS_HINT,
                    color = colors.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
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
