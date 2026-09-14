package com.savemoney.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.savemoney.app.AppViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NewRequestScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var itemName by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(CATEGORIES.first()) }
    var priceText by rememberSaveable { mutableStateOf("") }
    var quantityText by rememberSaveable { mutableStateOf("1") }
    var reason by rememberSaveable { mutableStateOf("") }
    var submitted by rememberSaveable { mutableStateOf(false) }

    val priceCents = priceText.yuanToCentsOrNull()
    val quantity = quantityText.trim().toIntOrNull()?.takeIf { it in 1..9999 }
    val nameValid = itemName.isNotBlank()
    val formValid = nameValid && priceCents != null && quantity != null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("新建购买申请") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            OutlinedTextField(
                value = itemName,
                onValueChange = { itemName = it },
                label = { Text("物品名称") },
                singleLine = true,
                isError = submitted && !nameValid,
                supportingText = if (submitted && !nameValid) ({ Text("请输入物品名称") }) else null,
                modifier = Modifier.fillMaxWidth(),
            )

            Column {
                Text("分类", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    CATEGORIES.forEach { option ->
                        FilterChip(
                            selected = category == option,
                            onClick = { category = option },
                            label = { Text(option) },
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("单价（元）") },
                    prefix = { Text("¥") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = submitted && priceCents == null,
                    supportingText = if (submitted && priceCents == null) ({ Text("请输入正确金额，最多两位小数") }) else null,
                    modifier = Modifier.weight(1.4f),
                )
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("数量") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = submitted && quantity == null,
                    supportingText = if (submitted && quantity == null) ({ Text("1~9999") }) else null,
                    modifier = Modifier.weight(1f),
                )
            }

            if (priceCents != null && quantity != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("合计", style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = (priceCents * quantity).toYuan(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            OutlinedTextField(
                value = reason,
                onValueChange = { reason = it },
                label = { Text("购买理由（可选）") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = {
                    submitted = true
                    if (formValid) {
                        viewModel.submitRequest(itemName, category, priceCents!!, quantity!!, reason)
                        onBack()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("提交申请")
            }
        }
    }
}
