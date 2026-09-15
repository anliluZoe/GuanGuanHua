package com.savemoney.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savemoney.app.AppViewModel
import com.savemoney.app.ui.theme.Palette

@Composable
fun SetupScreen(viewModel: AppViewModel) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val status by viewModel.statusMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    var serverUrl by rememberSaveable { mutableStateOf(session.serverUrl) }
    var name by rememberSaveable { mutableStateOf(profile.name.ifBlank { "小明" }) }
    var code by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.ScreenGlow)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-8).dp), verticalAlignment = Alignment.CenterVertically) {
            Mascot(MascotKind.Cat, size = 92.dp)
            Mascot(MascotKind.Dog, size = 92.dp)
        }
        PageHeader("省钱助手", "两个人的小金库 · 才不是随便花的")
        SoftField(value = serverUrl, onValueChange = { serverUrl = it }, label = "服务器地址")
        Text(
            "电脑上运行后端后，填 http://电脑局域网IP:8080。模拟器用 http://10.0.2.2:8080。",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.Muted,
        )
        SoftField(value = name, onValueChange = { name = it }, label = "我的名字")
        Text(
            "谁想买都行，另一半说了算哼。先连上同一台服务器，再用家庭码把两部手机绑在一起。",
            style = MaterialTheme.typography.bodySmall,
            color = Palette.Muted,
        )
        PillButton("创建家庭账本", enabled = !isBusy, onClick = {
            viewModel.consumeStatus()
            viewModel.createHome(serverUrl, name)
        })
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
        if (!status.isNullOrBlank()) {
            Text(status!!, color = Palette.Coral, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
