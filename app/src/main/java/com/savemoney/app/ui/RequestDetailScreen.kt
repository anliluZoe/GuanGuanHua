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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savemoney.app.AppViewModel
import com.savemoney.app.data.RequestStatus
import com.savemoney.app.ui.theme.QTheme

@Composable
fun RequestDetailScreen(viewModel: AppViewModel, requestId: Long, onBack: () -> Unit) {
    val requestFlow = remember(requestId) { viewModel.observeRequest(requestId) }
    val request by requestFlow.collectAsStateWithLifecycle(initialValue = null)
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    var comment by rememberSaveable { mutableStateOf("") }
    var confirmWithdraw by rememberSaveable { mutableStateOf(false) }
    var priceText by rememberSaveable { mutableStateOf("") }
    var quantityText by rememberSaveable { mutableStateOf("") }
    var approveAttempted by rememberSaveable { mutableStateOf(false) }

    val current = request
    if (current == null) {
        Box(modifier = Modifier.fillMaxSize().background(QTheme.colors.screenGlow), contentAlignment = Alignment.Center) {
            LoadingHint("正在打开申请…")
        }
        return
    }

    LaunchedEffect(current.id) {
        if (priceText.isBlank()) {
            priceText = current.unitPriceCents.toYuan().removePrefix("¥")
            quantityText = current.quantity.toString()
        }
    }

    val approveQty = quantityText.trim().toIntOrNull()
    val approvePrice = priceText.yuanToCentsOrNull()
    val priceOk = approvePrice != null && approvePrice >= 1
    val qtyOk = approveQty != null && approveQty >= 1
    val approveAmountsOk = approvedAmountsOk(
        current.unitPriceCents,
        current.quantity,
        approvePrice,
        approveQty,
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QTheme.colors.screenGlow)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        PageHeader("申请详情", current.itemName, onBack = onBack)
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (QTheme.colors.isDark) {
                    CategoryChip(current.category)
                } else {
                    RequestThumb(current.category, current.imagePath)
                }
                Spacer(Modifier.weight(1f))
                StatusBadge(current.status, partial = current.partial)
            }
            if (!current.imagePath.isNullOrBlank()) {
                Spacer(Modifier.height(14.dp))
                PhotoSlot(
                    model = current.imagePath,
                    showEmpty = false,
                    modifier = Modifier.fillMaxWidth().height(220.dp),
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(current.itemName, style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(6.dp))
            MoneyText(current.totalCents, style = MaterialTheme.typography.displaySmall)
            Spacer(Modifier.height(16.dp))
            listOf(
                "分类" to current.category,
                "单价" to if (current.approvedUnitPriceCents != null && current.approvedUnitPriceCents != current.unitPriceCents) {
                    if (QTheme.colors.isDark) {
                        "${current.unitPriceCents.toYuan()} → ${current.approvedUnitPriceCents.toYuan()}"
                    } else {
                        "${current.approvedUnitPriceCents.toYuan()}（申请 ${current.unitPriceCents.toYuan()}）"
                    }
                } else {
                    current.unitPriceCents.toYuan()
                },
                "数量" to if (current.approvedQuantity != null && current.approvedQuantity != current.quantity) {
                    if (QTheme.colors.isDark) {
                        "${current.quantity} → ${current.approvedQuantity}"
                    } else {
                        "${current.approvedQuantity}（申请 ${current.quantity} 个）"
                    }
                } else {
                    "${current.quantity}"
                },
                "申请人" to if (current.mine) "我（${current.requesterName}）" else current.requesterName,
                "申请时间" to current.createdAt.toDateTimeText(),
            ).forEachIndexed { index, (label, value) ->
                if (QTheme.colors.isDark && index > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                            .height(1.dp)
                            .background(QTheme.colors.line),
                    )
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text(label, color = QTheme.colors.secondary, modifier = Modifier.weight(1f))
                    if (label == "分类") {
                        CategoryChip(current.category)
                    } else {
                        Text(value, fontWeight = FontWeight.Medium)
                    }
                }
            }
            if (current.reason.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text("购买理由", color = QTheme.colors.muted)
                Spacer(Modifier.height(4.dp))
                Text(current.reason)
            }
        }

        when {
            current.status != RequestStatus.PENDING -> {
                val accent = when {
                    !QTheme.colors.isDark -> null
                    current.status == RequestStatus.REJECTED -> QTheme.colors.rose
                    current.partial -> QTheme.colors.coral
                    else -> QTheme.colors.mint
                }
                SoftCard(modifier = Modifier.fillMaxWidth(), accent = accent) {
                    Text(
                        when {
                            current.status == RequestStatus.REJECTED -> "这次先不买啦"
                            current.partial -> "部分通过"
                            else -> "过关啦"
                        },
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "${current.reviewerName ?: "-"} 于 ${current.reviewedAt?.toDateTimeText() ?: "-"} ${if (current.partial) "部分通过" else current.status.label}",
                        color = QTheme.colors.muted,
                    )
                    if (current.partial) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "按 ${(current.approvedQuantity ?: current.quantity)} 个、单价 ${(current.approvedUnitPriceCents ?: current.unitPriceCents).toYuan()} 入账 ${current.totalCents.toYuan()}（申请 ${current.askedCents.toYuan()}）",
                        )
                    }
                    current.reviewComment?.let {
                        Spacer(Modifier.height(6.dp))
                        Text("意见：$it")
                    }
                    if (current.status == RequestStatus.APPROVED) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "已自动记入当月消费小账本",
                            color = QTheme.colors.coral,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            !current.mine -> {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text("帮 ${current.requesterName} 把把关", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "可以改数量或单价再通过，但总额不能超过申请 ${current.askedCents.toYuan()}。过了关的钱才会乖乖进账本。",
                        style = MaterialTheme.typography.bodySmall,
                        color = QTheme.colors.muted,
                    )
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        SoftField(
                            value = priceText,
                            onValueChange = { priceText = it },
                            label = "同意的单价（元）",
                            prefix = "¥",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = approveAttempted && !priceOk,
                            supportingText = if (approveAttempted && !priceOk) "须大于 0" else "可改单价",
                            modifier = Modifier.weight(1.4f),
                        )
                        SoftField(
                            value = quantityText,
                            onValueChange = { quantityText = it },
                            label = "同意买几个",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            isError = approveAttempted && !qtyOk,
                            supportingText = if (approveAttempted && !qtyOk) "须大于 0" else "可改数量",
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (approveAmountsOk) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (approveQty != current.quantity || approvePrice != current.unitPriceCents) {
                                "按这个通过：${(approvePrice!! * approveQty!!).toYuan()}（申请 ${current.askedCents.toYuan()}）"
                            } else {
                                "按申请全额通过：${current.askedCents.toYuan()}"
                            },
                            color = QTheme.colors.coral,
                            fontWeight = FontWeight.SemiBold,
                        )
                    } else if (priceOk && qtyOk) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "批准总额 ${(approvePrice!! * approveQty!!).toYuan()} 超过了申请 ${current.askedCents.toYuan()}",
                            color = QTheme.colors.rose,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
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
                        PillButton("拒绝", filled = false, enabled = !isBusy, onClick = {
                            viewModel.review(current.id, approve = false, comment = comment, onSuccess = onBack)
                        }, modifier = Modifier.weight(1f))
                        PillButton("通过", enabled = !isBusy, onClick = {
                            approveAttempted = true
                            if (approveAmountsOk) {
                                viewModel.review(
                                    current.id,
                                    approve = true,
                                    comment = comment,
                                    unitPriceCents = approvePrice,
                                    quantity = approveQty,
                                    onSuccess = onBack,
                                )
                            }
                        }, modifier = Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("通过后会自动记入当月消费。", color = QTheme.colors.muted, style = MaterialTheme.typography.bodySmall)
                }
            }

            else -> {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Text("正在等 ${profile.partnerName ?: "另一半"} 看一眼…", color = QTheme.colors.secondary)
                    Spacer(Modifier.height(12.dp))
                    PillButton("撤回申请", filled = false, enabled = !isBusy, onClick = { confirmWithdraw = true })
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
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        confirmWithdraw = false
                        viewModel.withdrawRequest(current.id, onSuccess = onBack)
                    },
                ) { Text("撤回", color = QTheme.colors.coral) }
            },
            dismissButton = {
                TextButton(onClick = { confirmWithdraw = false }) { Text("再想想") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }
}
