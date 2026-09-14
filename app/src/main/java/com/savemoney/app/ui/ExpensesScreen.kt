package com.savemoney.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savemoney.app.AppViewModel
import com.savemoney.app.ui.theme.Cute
import java.time.YearMonth

@Composable
fun ExpensesScreen(viewModel: AppViewModel) {
    val month by viewModel.selectedMonth.collectAsStateWithLifecycle()
    val expenses by viewModel.monthExpenses.collectAsStateWithLifecycle()
    val budget by viewModel.monthBudget.collectAsStateWithLifecycle()
    var editingBudget by rememberSaveable { mutableStateOf(false) }
    var budgetText by rememberSaveable { mutableStateOf("") }

    val totalCents = expenses.sumOf { it.amountCents }
    val budgetCents = budget?.amountCents
    val byCategory = expenses.groupBy { it.category }
        .map { (category, list) -> category to list.sumOf { it.amountCents } }
        .sortedByDescending { it.second }
    val overBudget = budgetCents != null && totalCents > budgetCents

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        PageHeader("小账本", "看看这个月花到哪里去了")
        LazyColumn(
            contentPadding = PaddingValues(bottom = 24.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { viewModel.shiftMonth(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月", tint = Cute.Ink)
                    }
                    Text(
                        "${month.year}年${month.monthValue}月",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    IconButton(onClick = { viewModel.shiftMonth(1) }, enabled = month < YearMonth.now()) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下个月", tint = Cute.Ink)
                    }
                }
            }

            item {
                SoftCard(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Mascot(MascotKind.Piggy, size = 88.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("本月已消费", color = Cute.Muted, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                totalCents.toYuan(),
                                style = MaterialTheme.typography.displaySmall,
                                color = if (overBudget) MaterialTheme.colorScheme.error else Cute.Ink,
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = if (budgetCents == null) "还没设预算，点下面设一个小目标"
                        else "预算 ${budgetCents.toYuan()}  ·  剩余 ${(budgetCents - totalCents).toYuan()}",
                        color = if (overBudget) MaterialTheme.colorScheme.error else Cute.Muted,
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    if (budgetCents != null && budgetCents > 0) {
                        Spacer(Modifier.height(10.dp))
                        LinearProgressIndicator(
                            progress = { (totalCents.toFloat() / budgetCents).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(20.dp)),
                            color = if (overBudget) MaterialTheme.colorScheme.error else Cute.Peach,
                            trackColor = Cute.PeachSoft,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    ChoiceChip(
                        label = if (budgetCents == null) "设置预算" else "改预算",
                        selected = false,
                        onClick = {
                            budgetText = budgetCents?.toYuan()?.removePrefix("¥") ?: ""
                            editingBudget = true
                        },
                    )
                    Spacer(Modifier.height(6.dp))
                    Text("共 ${expenses.size} 笔已通过的购买", color = Cute.Muted, style = MaterialTheme.typography.bodySmall)
                }
            }

            if (byCategory.isNotEmpty()) {
                item {
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Text("花在哪儿", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(12.dp))
                        byCategory.forEach { (category, cents) ->
                            Column(modifier = Modifier.padding(bottom = 10.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                    Text("${CATEGORY_EMOJI[category]}  $category", modifier = Modifier.weight(1f))
                                    Text(cents.toYuan(), fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(6.dp))
                                LinearProgressIndicator(
                                    progress = { if (totalCents == 0L) 0f else cents.toFloat() / totalCents },
                                    modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(20.dp)),
                                    color = Cute.Sky,
                                    trackColor = Cute.SkySoft,
                                )
                            }
                        }
                    }
                }
            }

            item { Text("明细", style = MaterialTheme.typography.titleMedium) }

            if (expenses.isEmpty()) {
                item {
                    EmptyHint(MascotKind.Wallet, "本月还是空空的", "申请通过后，会自动出现在这里")
                }
            } else {
                items(expenses, key = { it.id }) { record ->
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CategoryBubble(record.category)
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.itemName, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "${record.requesterName} 申请 · ${record.reviewerName} 审核 · ${record.spentAt.toDateTimeText()}",
                                    color = Cute.Muted,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(record.amountCents.toYuan(), fontWeight = FontWeight.Bold, color = Cute.Peach)
                        }
                    }
                }
            }
        }
    }

    if (editingBudget) {
        val parsed = budgetText.yuanToCentsOrNull()
        AlertDialog(
            onDismissRequest = { editingBudget = false },
            title = { Text("设置 ${month.year}年${month.monthValue}月预算") },
            text = {
                SoftField(
                    value = budgetText,
                    onValueChange = { budgetText = it },
                    label = "预算金额（元）",
                    prefix = "¥",
                    isError = budgetText.isNotBlank() && parsed == null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                )
            },
            confirmButton = {
                TextButton(enabled = parsed != null, onClick = {
                    viewModel.setBudget(parsed!!)
                    editingBudget = false
                }) { Text("保存", color = Cute.Peach) }
            },
            dismissButton = {
                TextButton(onClick = { editingBudget = false }) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }
}
