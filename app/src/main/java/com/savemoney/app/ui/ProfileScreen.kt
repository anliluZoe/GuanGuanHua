package com.savemoney.app.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savemoney.app.AppViewModel
import com.savemoney.app.notify.ReviewActivityWorker
import com.savemoney.app.ui.theme.QTheme

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
    LifecycleResumeEffect(Unit) {
        notificationsOn = ReviewActivityWorker.notificationsAllowed(context)
        onPauseOrDispose { }
    }
    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QTheme.colors.screenGlow),
    ) {
        RefreshBar(visible = isRefreshing && isReady)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
        Spacer(Modifier.height(12.dp))
        Mascot(MascotKind.Dog, size = 56.dp)
        PageHeader("我们", "两个人的小金库 · 才不是腻歪呢")
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
                NameDot(profile.partnerName ?: "？", QTheme.colors.mint, size = 44.dp)
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
        PillButton("退出这个家庭账本", filled = false, enabled = !isBusy, onClick = { viewModel.leaveHome() })
        Spacer(Modifier.height(96.dp))
        }
    }
}
