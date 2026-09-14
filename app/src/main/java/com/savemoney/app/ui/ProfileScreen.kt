package com.savemoney.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savemoney.app.AppViewModel
import com.savemoney.app.UserRole
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(viewModel: AppViewModel) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var requesterName by rememberSaveable(profile.requesterName) { mutableStateOf(profile.requesterName) }
    var approverName by rememberSaveable(profile.approverName) { mutableStateOf(profile.approverName) }
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("我的") }) },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("当前身份", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = "申请人可以提交购买申请；审核人负责通过或拒绝，审核通过的申请会自动记入当月消费。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        UserRole.entries.forEachIndexed { index, role ->
                            SegmentedButton(
                                selected = profile.role == role,
                                onClick = { viewModel.updateProfile(role, requesterName, approverName) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = UserRole.entries.size),
                            ) {
                                Text(role.label)
                            }
                        }
                    }
                }
            }

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("成员名称", style = MaterialTheme.typography.titleMedium)
                    OutlinedTextField(
                        value = requesterName,
                        onValueChange = { requesterName = it },
                        label = { Text("申请人") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = approverName,
                        onValueChange = { approverName = it },
                        label = { Text("审核人") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Button(
                        onClick = {
                            viewModel.updateProfile(profile.role, requesterName, approverName)
                            scope.launch { snackbar.showSnackbar("已保存") }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("保存名称")
                    }
                }
            }

            Text(
                text = "数据仅保存在本机。两人共用一台设备时，请在此处切换身份。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
