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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savemoney.app.AppViewModel
import com.savemoney.app.UserRole
import com.savemoney.app.ui.theme.Cute
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val status by viewModel.statusMessage.collectAsStateWithLifecycle()
    var requesterName by rememberSaveable(profile.requesterName) { mutableStateOf(profile.requesterName) }
    var approverName by rememberSaveable(profile.approverName) { mutableStateOf(profile.approverName) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(status) {
        if (!status.isNullOrBlank()) {
            snackbar.showSnackbar(status!!)
            viewModel.consumeStatus()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        PageHeader("我们俩", "两部手机连同一个家庭码，申请和照片会同步")
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Mascot(MascotKind.Coin, size = 140.dp)
        }
        SnackbarHost(snackbar)
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("家庭码", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                session.householdCode.ifBlank { "还未加入" },
                style = MaterialTheme.typography.displaySmall,
                color = Cute.Peach,
            )
            Spacer(Modifier.height(6.dp))
            Text("把这串数字发给另一部手机，在启动页点「加入」。", color = Cute.Muted, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(4.dp))
            Text(session.serverUrl, color = Cute.Muted, style = MaterialTheme.typography.bodySmall)
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("今天我是…", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "申请人提交想买的东西；审核人点头或摇头。通过的申请会自动记入当月小账本。",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RoleCard("申请人", "✏️", profile.role == UserRole.REQUESTER) {
                    viewModel.updateProfile(UserRole.REQUESTER, requesterName, approverName)
                }
                RoleCard("审核人", "👀", profile.role == UserRole.APPROVER) {
                    viewModel.updateProfile(UserRole.APPROVER, requesterName, approverName)
                }
            }
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("怎么称呼", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            SoftField(value = requesterName, onValueChange = { requesterName = it }, label = "申请人")
            Spacer(Modifier.height(10.dp))
            SoftField(value = approverName, onValueChange = { approverName = it }, label = "审核人")
            Spacer(Modifier.height(16.dp))
            CharcoalPillButton("保存名称", onClick = {
                viewModel.updateProfile(profile.role, requesterName, approverName)
                scope.launch { snackbar.showSnackbar("记下啦") }
            })
        }
        CharcoalPillButton("退出这个家庭账本", filled = false, onClick = { viewModel.leaveHome() })
        Spacer(Modifier.height(96.dp))
    }
}
