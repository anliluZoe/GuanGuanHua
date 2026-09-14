package com.savemoney.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savemoney.app.AppViewModel
import java.time.YearMonth

@OptIn(ExperimentalMaterial3Api::class)
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

    Scaffold(
        topBar = { TopAppBar(title = { Text("消费记录") }) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    IconButton(onClick = { viewModel.shiftMonth(-1) }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上个月")
                    }
                    Text(
                        text = "${month.year}年${month.monthValue}月",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    IconButton(
                        onClick = { viewModel.shiftMonth(1) },
                        enabled = month < YearMonth.now(),
                    ) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下个月")
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("本月已消费", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text(
                            text = totalCents.toYuan(),
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (budgetCents == null) "尚未设置本月预算"
                                else "预算 ${budgetCents.toYuan()} · 剩余 ${(budgetCents - totalCents).toYuan()}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (budgetCents != null && totalCents > budgetCents) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.weight(1f),
                            )
                            IconButton(onClick = {
                                budgetText = budgetCents?.toYuan()?.removePrefix("¥") ?: ""
                                editingBudget = true
                            }) {
                                Icon(Icons.Default.Edit, contentDescription = "设置预算")
                            }
                        }
                        if (budgetCents != null && budgetCents > 0) {
                            LinearProgressIndicator(
                                progress = { (totalCents.toFloat() / budgetCents).coerceIn(0f, 1f) },
                                modifier = Modifier.fillMaxWidth().height(8.dp),
                                color = if (totalCents > budgetCents) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "共 ${expenses.size} 笔已通过的购买",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }

            if (byCategory.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("分类占比", style = MaterialTheme.typography.titleMedium)
                            byCategory.forEach { (category, cents) ->
                                Column {
                                    Row(modifier = Modifier.fillMaxWidth()) {
                                        Text(category, modifier = Modifier.weight(1f))
                                        Text(cents.toYuan(), fontWeight = FontWeight.Medium)
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    LinearProgressIndicator(
                                        progress = { if (totalCents == 0L) 0f else cents.toFloat() / totalCents },
                                        modifier = Modifier.fillMaxWidth().height(6.dp),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item {
                Text(
                    text = "明细",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            if (expenses.isEmpty()) {
                item {
                    Text(
                        text = "本月还没有消费记录。申请审核通过后会自动出现在这里。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                }
            } else {
                items(expenses, key = { it.id }) { record ->
                    Column {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(record.itemName, fontWeight = FontWeight.Medium)
                                Text(
                                    text = "${record.category} · ${record.requesterName} 申请 · ${record.reviewerName} 审核 · ${record.spentAt.toDateTimeText()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                text = record.amountCents.toYuan(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                        HorizontalDivider()
                    }
                }
            }
        }

        if (editingBudget) {
            val parsed = budgetText.yuanToCentsOrNull()
            AlertDialog(
                onDismissRequest = { editingBudget = false },
                title = { Text("设置 ${month.year}年${month.monthValue}月 预算") },
                text = {
                    OutlinedTextField(
                        value = budgetText,
                        onValueChange = { budgetText = it },
                        label = { Text("预算金额（元）") },
                        prefix = { Text("¥") },
                        singleLine = true,
                        isError = budgetText.isNotBlank() && parsed == null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                },
                confirmButton = {
                    TextButton(
                        enabled = parsed != null,
                        onClick = {
                            viewModel.setBudget(parsed!!)
                            editingBudget = false
                        },
                    ) { Text("保存") }
                },
                dismissButton = {
                    TextButton(onClick = { editingBudget = false }) { Text("取消") }
                },
            )
        }
    }
}
