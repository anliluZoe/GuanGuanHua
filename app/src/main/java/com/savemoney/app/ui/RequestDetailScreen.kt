package com.savemoney.app.ui

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.savemoney.app.ui.theme.Cute

@Composable
fun RequestDetailScreen(viewModel: AppViewModel, requestId: Long, onBack: () -> Unit) {
    val requestFlow = remember(requestId) { viewModel.observeRequest(requestId) }
    val request by requestFlow.collectAsStateWithLifecycle(initialValue = null)
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    var comment by rememberSaveable { mutableStateOf("") }
    var confirmWithdraw by rememberSaveable { mutableStateOf(false) }

    val current = request
    if (current == null) {
        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Cute.Peach)
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader("申请详情", current.itemName, onBack = onBack)
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CategoryBubble(current.category)
                Spacer(Modifier.weight(1f))
                StatusBadge(current.status)
            }
            Spacer(Modifier.height(16.dp))
            Text(current.itemName, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            Text(
                current.totalCents.toYuan(),
                style = MaterialTheme.typography.displaySmall,
                color = Cute.Peach,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(16.dp))
            listOf(
                "分类" to "${CATEGORY_EMOJI[current.category]} ${current.category}",
                "单价" to current.unitPriceCents.toYuan(),
                "数量" to "${current.quantity}",
                "申请人" to current.requesterName,
                "申请时间" to current.createdAt.toDateTimeText(),
            ).forEach { (label, value) ->
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text(label, color = Cute.Muted, modifier = Modifier.weight(1f))
                    Text(value, fontWeight = FontWeight.Medium)
                }
            }
            if (current.reason.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("购买理由", color = Cute.Muted)
                Spacer(Modifier.height(4.dp))
                Text(current.reason)
            }
        }

        when {
            current.status != RequestStatus.PENDING -> {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text(if (current.status == RequestStatus.APPROVED) "🎉 已通过" else "这次先不买啦", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${current.reviewerName ?: "-"} 于 ${current.reviewedAt?.toDateTimeText() ?: "-"} ${current.status.label}",
                        color = Cute.Muted,
                    )
                    current.reviewComment?.let {
                        Spacer(Modifier.height(6.dp))
                        Text("意见：$it")
                    }
                    if (current.status == RequestStatus.APPROVED) {
                        Spacer(Modifier.height(6.dp))
                        Text("已自动记入当月消费小账本", color = Cute.Peach, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            profile.role == UserRole.APPROVER -> {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text("帮TA把把关", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(10.dp))
                    SoftField(
                        value = comment,
                        onValueChange = { comment = it },
                        label = "审核意见（可选）",
                        singleLine = false,
                        minLines = 2,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CharcoalPillButton("拒绝", filled = false, onClick = {
                            viewModel.review(current.id, approve = false, comment = comment)
                            onBack()
                        }, modifier = Modifier.weight(1f))
                        CharcoalPillButton("通过", onClick = {
                            viewModel.review(current.id, approve = true, comment = comment)
                            onBack()
                        }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("通过后会自动记入当月消费。", color = Cute.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }

            else -> {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text("正在等 ${profile.approverName} 看一眼…", color = Cute.Muted)
                    Spacer(Modifier.height(12.dp))
                    CharcoalPillButton("撤回申请", filled = false, onClick = { confirmWithdraw = true })
                }
            }
        }
        Spacer(Modifier.height(24.dp))
    }

    if (confirmWithdraw) {
        AlertDialog(
            onDismissRequest = { confirmWithdraw = false },
            title = { Text("撤回申请") },
            text = { Text("撤回后这条申请会消失哦，确定吗？") },
            confirmButton = {
                TextButton(onClick = {
                    confirmWithdraw = false
                    viewModel.withdrawRequest(current.id)
                    onBack()
                }) { Text("撤回", color = Cute.Peach) }
            },
            dismissButton = {
                TextButton(onClick = { confirmWithdraw = false }) { Text("再想想") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }
}
