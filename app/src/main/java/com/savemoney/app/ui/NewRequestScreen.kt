package com.savemoney.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.savemoney.app.AppViewModel
import com.savemoney.app.ui.theme.Palette

@Composable
fun NewRequestScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var itemName by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf(CATEGORIES.first()) }
    var priceText by rememberSaveable { mutableStateOf("") }
    var quantityText by rememberSaveable { mutableStateOf("1") }
    var reason by rememberSaveable { mutableStateOf("") }
    var photoUri by rememberSaveable { mutableStateOf<String?>(null) }
    var submitted by rememberSaveable { mutableStateOf(false) }
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        photoUri = uri?.toString()
    }

    val priceCents = priceText.yuanToCentsOrNull()
    val quantity = quantityText.trim().toIntOrNull()?.takeIf { it in 1..9999 }
    val nameValid = itemName.isNotBlank()
    val formValid = nameValid && priceCents != null && quantity != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Palette.ScreenGlow)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageHeader("又想买什么？", "填清楚哦，可别指望糊弄过去~", onBack = onBack)
        SoftField(
            value = itemName,
            onValueChange = { itemName = it },
            label = "物品名称",
            isError = submitted && !nameValid,
            supportingText = if (submitted && !nameValid) "给它起个名字吧" else null,
        )
        Text("分类", style = MaterialTheme.typography.labelLarge)
        CATEGORIES.chunked(3).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                row.forEach { option ->
                    val selected = category == option
                    val look = categoryLook(option)
                    val shape = RoundedCornerShape(20.dp)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(shape)
                            .border(1.5.dp, if (selected) Palette.Sky else Palette.Line, shape)
                            .background(if (selected) Palette.SkySoft else MaterialTheme.colorScheme.surface)
                            .clickable { category = option }
                            .padding(vertical = 14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(look.wash),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(look.icon, contentDescription = option, tint = look.accent, modifier = Modifier.size(20.dp))
                        }
                        Text(option, style = MaterialTheme.typography.labelMedium, color = Palette.Ink)
                    }
                }
                repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
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
                    MoneyText((priceCents * quantity), style = MaterialTheme.typography.headlineSmall)
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
        PhotoSlot(
            model = photoUri,
            modifier = Modifier.fillMaxWidth().height(180.dp),
            onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
            onClear = photoUri?.let { { photoUri = null } },
        )
        Spacer(Modifier.height(4.dp))
        PillButton(
            text = "提交申请",
            enabled = !isBusy,
            onClick = {
                submitted = true
                if (formValid) {
                    viewModel.submitRequest(itemName, category, priceCents!!, quantity!!, reason, photoUri, onSuccess = onBack)
                }
            },
        )
        Spacer(Modifier.height(24.dp))
    }
}
