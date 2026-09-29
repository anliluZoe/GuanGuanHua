package com.guanguanhua.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.guanguanhua.app.data.TripStopPhoto
import com.guanguanhua.app.trip.TripMath
import com.guanguanhua.app.ui.theme.QTheme

@Composable
fun TripBanner(name: String, onRecord: () -> Unit, onOpen: () -> Unit) {
    val q = QTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(q.mintSoft)
            .clickable(onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("旅行中 · $name", modifier = Modifier.weight(1f), color = q.ink, style = MaterialTheme.typography.titleSmall)
        Text(
            "记一站",
            color = q.mint,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .clickable(onClick = onRecord)
                .padding(start = 8.dp),
        )
    }
}

fun tripStopsLine(stopCount: Int, spentCents: Long): String =
    listOfNotNull("$stopCount 站", spentCents.takeIf { it > 0 }?.toYuan()).joinToString(" · ")

@Composable
fun TripAmountField(value: String, onValueChange: (String) -> Unit) {
    val invalid = value.isNotBlank() && value.yuanToCentsOrNull() == null
    SoftField(
        value = value,
        onValueChange = onValueChange,
        label = "这一站花了多少（可选）",
        prefix = "¥",
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        isError = invalid,
        supportingText = if (invalid) "最多两位小数" else "不进账本，只记在这一站上",
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TripKindChips(selected: String?, onSelect: (String) -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        TripMath.KINDS.forEach { kind ->
            ChoiceChip(kind, selected == kind, onClick = { onSelect(kind) })
        }
    }
}

@Composable
fun TripStars(rating: Int?, onChange: (Int?) -> Unit) {
    val q = QTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { star ->
            val on = rating != null && star <= rating
            Text(
                "★",
                color = if (on) q.gold else q.lineStrong,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier
                    .clickable { onChange(if (rating == star) null else star) }
                    .padding(end = 4.dp),
            )
        }
        if (rating == null) {
            Spacer(Modifier.width(6.dp))
            Text("未评分", color = q.muted, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun TripKindLabel(kind: String) {
    val q = QTheme.colors
    Text(
        kind,
        color = q.ink,
        style = MaterialTheme.typography.labelMedium,
        modifier = Modifier
            .border(1.dp, q.line, RoundedCornerShape(999.dp))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
fun TripPhotoStrip(
    photos: List<TripStopPhoto>,
    localUris: List<String> = emptyList(),
    canEdit: Boolean,
    onAdd: () -> Unit,
    onRemovePhoto: (TripStopPhoto) -> Unit = {},
    onRemoveLocal: (String) -> Unit = {},
) {
    val total = photos.size + localUris.size
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        photos.forEach { photo ->
            PhotoSlot(
                model = photo.url,
                modifier = Modifier.size(88.dp),
                showEmpty = false,
                onClear = if (canEdit) ({ onRemovePhoto(photo) }) else null,
            )
        }
        localUris.forEach { uri ->
            PhotoSlot(
                model = uri,
                modifier = Modifier.size(88.dp),
                showEmpty = false,
                onClear = { onRemoveLocal(uri) },
            )
        }
        if (canEdit && total < TripMath.MAX_PHOTOS) {
            PhotoSlot(
                model = null,
                modifier = Modifier.size(88.dp),
                emptyLabel = "加照片",
                onClick = onAdd,
            )
        }
    }
}
