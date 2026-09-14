package com.savemoney.app.ui

import androidx.compose.foundation.background
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.savemoney.app.AppViewModel
import com.savemoney.app.ui.theme.Cute

@OptIn(ExperimentalLayoutApi::class)
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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageHeader("想买点什么？", "填好物品和小金额，交给另一半把关", onBack = onBack)
        SoftField(
            value = itemName,
            onValueChange = { itemName = it },
            label = "物品名称",
            isError = submitted && !nameValid,
            supportingText = if (submitted && !nameValid) "给它起个名字吧" else null,
        )
        Text("分类", style = MaterialTheme.typography.labelLarge)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            CATEGORIES.forEach { option ->
                ChoiceChip(
                    label = "${CATEGORY_EMOJI[option]} $option",
                    selected = category == option,
                    onClick = { category = option },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SoftField(
                value = priceText,
                onValueChange = { priceText = it },
                label = "单价（元）",
                prefix = "¥",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                isError = submitted && priceCents == null,
                supportingText = if (submitted && priceCents == null) "最多两位小数" else null,
                modifier = Modifier.weight(1.4f),
            )
            SoftField(
                value = quantityText,
                onValueChange = { quantityText = it },
                label = "数量",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = submitted && quantity == null,
                supportingText = if (submitted && quantity == null) "1~9999" else null,
                modifier = Modifier.weight(1f),
            )
        }
        if (priceCents != null && quantity != null) {
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("合计", style = MaterialTheme.typography.titleMedium)
                    Text(
                        (priceCents * quantity).toYuan(),
                        style = MaterialTheme.typography.headlineSmall,
                        color = Cute.Peach,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
        SoftField(
            value = reason,
            onValueChange = { reason = it },
            label = "购买理由（可选）",
            singleLine = false,
            minLines = 3,
        )
        Spacer(Modifier.height(4.dp))
        CharcoalPillButton(
            text = "提交申请",
            enabled = true,
            onClick = {
                submitted = true
                if (formValid) {
                    viewModel.submitRequest(itemName, category, priceCents!!, quantity!!, reason)
                    onBack()
                }
            },
        )
        Spacer(Modifier.height(24.dp))
    }
}
