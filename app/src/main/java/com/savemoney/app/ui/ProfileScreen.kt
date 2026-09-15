package com.savemoney.app.ui

import android.content.Intent
import android.provider.Settings
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
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savemoney.app.AppViewModel
import com.savemoney.app.ui.theme.Palette

@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    var name by rememberSaveable(profile.name) { mutableStateOf(profile.name) }
    val context = LocalContext.current
    var notificationsOn by remember { mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled()) }
    LifecycleResumeEffect(Unit) {
        notificationsOn = NotificationManagerCompat.from(context).areNotificationsEnabled()
        onPauseOrDispose { }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.ScreenGlow)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Mascot(MascotKind.Dog, size = 56.dp)
        PageHeader("我们", "两个人的小金库 · 才不是腻歪呢")
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("家庭码", style = MaterialTheme.typography.titleMedium, color = Palette.Muted)
            Spacer(Modifier.height(6.dp))
            Text(
                session.householdCode.ifBlank { "还未加入" },
                style = MaterialTheme.typography.displaySmall.copy(
                    fontFeatureSettings = "tnum",
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Medium,
                ),
                color = Palette.Sky,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "把码塞给另一部手机，启动页点「加入」——别搞丢了哦。",
                color = Palette.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(4.dp))
            Text(session.serverUrl, color = Palette.Muted, style = MaterialTheme.typography.bodySmall)
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("谁来把关", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "谁想买都行，但另一半说了算哼。过了关的钱才会乖乖进账本。",
                style = MaterialTheme.typography.bodySmall,
                color = Palette.Muted,
            )
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                NameDot(profile.partnerName ?: "？", Palette.Mint, size = 44.dp)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("另一半", color = Palette.Muted, style = MaterialTheme.typography.labelMedium)
                    Text(
                        profile.partnerName ?: "还没有人加入，把家庭码发给TA吧",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (profile.partnerName == null) Palette.Muted else Palette.Ink,
                    )
                }
            }
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("怎么称呼我", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            SoftField(value = name, onValueChange = { name = it }, label = "我的名字")
            Spacer(Modifier.height(16.dp))
            PillButton("保存名字", enabled = name.trim().isNotBlank() && name.trim() != profile.name, onClick = {
                viewModel.updateName(name)
            })
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("审核动态提醒", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("对方发起新申请、或审核了你的申请，都会通知你。", style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(
                "App 打开时每 30 秒自动刷新；放在后台约每 15 分钟检查一次。",
                color = Palette.Muted,
                style = MaterialTheme.typography.bodySmall,
            )
            if (!notificationsOn) {
                Spacer(Modifier.height(14.dp))
                Text("系统通知现在是关着的，打开后才能收到提醒。", color = Palette.Coral, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(10.dp))
                PillButton("去开启通知", filled = false, onClick = {
                    context.startActivity(
                        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    )
                })
            }
        }
        PillButton("退出这个家庭账本", filled = false, onClick = { viewModel.leaveHome() })
        Spacer(Modifier.height(96.dp))
    }
}
