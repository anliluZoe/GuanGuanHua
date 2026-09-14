package com.savemoney.app.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savemoney.app.AppViewModel
import com.savemoney.app.UserRole
import com.savemoney.app.data.RequestStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RequestDetailScreen(viewModel: AppViewModel, requestId: Long, onBack: () -> Unit) {
    val requestFlow = remember(requestId) { viewModel.observeRequest(requestId) }
    val request by requestFlow.collectAsStateWithLifecycle(initialValue = null)
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var comment by rememberSaveable { mutableStateOf("") }
    var confirmWithdraw by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("申请详情") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        val current = request
        if (current == null) {
            Box(modifier = Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = current.itemName,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f),
                        )
                        StatusBadge(current.status)
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = current.totalCents.toYuan(),
                        style = MaterialTheme.typography.displaySmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(12.dp))
                    listOf(
                        "分类" to current.category,
                        "单价" to current.unitPriceCents.toYuan(),
                        "数量" to "${current.quantity}",
                        "申请人" to current.requesterName,
                        "申请时间" to current.createdAt.toDateTimeText(),
                    ).forEach { (label, value) ->
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text(
                                text = label,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                            )
                            Text(text = value, fontWeight = FontWeight.Medium)
                        }
                    }
                    if (current.reason.isNotBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("购买理由", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(4.dp))
                        Text(current.reason)
                    }
                }
            }

            when {
                current.status != RequestStatus.PENDING -> {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("审核结果", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${current.reviewerName ?: "-"} 于 ${current.reviewedAt?.toDateTimeText() ?: "-"} ${current.status.label}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            current.reviewComment?.let { Text("意见：$it") }
                            if (current.status == RequestStatus.APPROVED) {
                                Text(
                                    text = "已自动计入审核当月的消费记录",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }
                }

                profile.role == UserRole.APPROVER -> {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("审核", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = comment,
                            onValueChange = { comment = it },
                            label = { Text("审核意见（可选）") },
                            minLines = 2,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            OutlinedButton(
                                onClick = {
                                    viewModel.review(current.id, approve = false, comment = comment)
                                    onBack()
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.Close, contentDescription = null)
                                Text(" 拒绝")
                            }
                            Button(
                                onClick = {
                                    viewModel.review(current.id, approve = true, comment = comment)
                                    onBack()
                                },
                                modifier = Modifier.weight(1f),
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Text(" 通过")
                            }
                        }
                        Text(
                            text = "通过后将自动记入当月消费。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "等待 ${profile.approverName} 审核中…",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        OutlinedButton(
                            onClick = { confirmWithdraw = true },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("撤回申请")
                        }
                    }
                }
            }
        }

        if (confirmWithdraw) {
            AlertDialog(
                onDismissRequest = { confirmWithdraw = false },
                title = { Text("撤回申请") },
                text = { Text("撤回后该申请将被删除，确定要撤回吗？") },
                confirmButton = {
                    TextButton(onClick = {
                        confirmWithdraw = false
                        viewModel.withdrawRequest(current.id)
                        onBack()
                    }) { Text("撤回") }
                },
                dismissButton = {
                    TextButton(onClick = { confirmWithdraw = false }) { Text("取消") }
                },
            )
        }
    }
}
