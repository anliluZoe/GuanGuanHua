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
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var requesterName by rememberSaveable(profile.requesterName) { mutableStateOf(profile.requesterName) }
    var approverName by rememberSaveable(profile.approverName) { mutableStateOf(profile.approverName) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        PageHeader("我们俩", "切换身份，互相给对方的购物把关")
        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            Mascot(MascotKind.Coin, size = 140.dp)
        }
        SnackbarHost(snackbar)
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
        Text(
            "数据只存在这台手机里。两个人共用时，来这里切换身份就好。",
            style = MaterialTheme.typography.bodySmall,
        )
        Spacer(Modifier.height(24.dp))
    }
}
