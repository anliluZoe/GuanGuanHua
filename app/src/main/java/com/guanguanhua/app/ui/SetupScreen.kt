package com.guanguanhua.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanguanhua.app.AppViewModel
import com.guanguanhua.app.ui.theme.QTheme

@Composable
fun SetupScreen(viewModel: AppViewModel) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val status by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val appearance by viewModel.appearance.collectAsStateWithLifecycle()
    var serverUrl by rememberSaveable { mutableStateOf(session.serverUrl) }
    var name by rememberSaveable { mutableStateOf(profile.name.ifBlank { "小明" }) }
    var code by rememberSaveable { mutableStateOf("") }
    val colors = QTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.screenGlow)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (colors.isDark) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(28.dp))
                    .background(colors.skySoft)
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy((-10).dp), verticalAlignment = Alignment.CenterVertically) {
                    Mascot(MascotKind.Cat, size = 72.dp)
                    Mascot(MascotKind.Dog, size = 72.dp)
                }
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy((-8).dp), verticalAlignment = Alignment.CenterVertically) {
                Mascot(MascotKind.Cat, size = 92.dp)
                Mascot(MascotKind.Dog, size = 92.dp)
            }
        }
        PageHeader("管管花", "两个人的小金库 · 才不是随便花的")
        if (colors.isDark) {
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                SoftField(value = serverUrl, onValueChange = { serverUrl = it }, label = "服务器地址")
                Spacer(Modifier.height(6.dp))
                Text(
                    "电脑上运行后端后，填 http://电脑局域网IP:8080。模拟器用 http://10.0.2.2:8080。",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.muted,
                )
                Spacer(Modifier.height(12.dp))
                SoftField(value = name, onValueChange = { name = it }, label = "我的名字")
                Spacer(Modifier.height(16.dp))
                PillButton("创建家庭账本", enabled = !isBusy, onClick = {
                    viewModel.consumeStatus()
                    viewModel.createHome(serverUrl, name)
                })
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.line))
                Text("或", color = colors.muted, style = MaterialTheme.typography.labelMedium)
                Box(modifier = Modifier.weight(1f).height(1.dp).background(colors.line))
            }
        } else {
            SoftField(value = serverUrl, onValueChange = { serverUrl = it }, label = "服务器地址")
            Text(
                "电脑上运行后端后，填 http://电脑局域网IP:8080。模拟器用 http://10.0.2.2:8080。",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
            SoftField(value = name, onValueChange = { name = it }, label = "我的名字")
            Text(
                "谁想买都行，另一半说了算哼。先连上同一台服务器，再用家庭码把两部手机绑在一起。",
                style = MaterialTheme.typography.bodySmall,
                color = colors.muted,
            )
            PillButton("创建家庭账本", enabled = !isBusy, onClick = {
                viewModel.consumeStatus()
                viewModel.createHome(serverUrl, name)
            })
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("已经有家庭码？", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            SoftField(value = code, onValueChange = { code = it }, label = "6 位家庭码")
            Spacer(Modifier.height(12.dp))
            PillButton("加入", filled = false, enabled = !isBusy, onClick = {
                viewModel.consumeStatus()
                viewModel.joinHome(serverUrl, code, name)
            })
        }
        AppearancePicker(value = appearance, onChange = viewModel::setAppearance)
        if (!status.isNullOrBlank()) {
            Text(
                status!!,
                color = if (colors.isDark) colors.rose else colors.coral,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
