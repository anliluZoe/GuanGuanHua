package com.savemoney.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.savemoney.app.data.RequestStatus
import com.savemoney.app.ui.theme.Cute

val CATEGORY_EMOJI = mapOf(
    "餐饮" to "🍜",
    "日用品" to "🧴",
    "服饰" to "👕",
    "数码" to "💻",
    "交通" to "🚌",
    "娱乐" to "🎮",
    "学习" to "📚",
    "医疗" to "💊",
    "其他" to "✨",
)

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(28.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color(0x14000000), RoundedCornerShape(28.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(20.dp),
        content = content,
    )
}

@Composable
fun PageHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .background(MaterialTheme.colorScheme.surface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Cute.Ink)
            }
            Spacer(Modifier.height(12.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Cute.Muted)
    }
}

@Composable
fun CharcoalPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(28.dp),
        colors = if (filled) ButtonDefaults.buttonColors(
            containerColor = Cute.Ink,
            contentColor = Cute.Gold,
            disabledContainerColor = Color(0xFFE6E8E2),
            disabledContentColor = Color(0xFFB0B4AA),
        ) else ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Color(0xFF6B1A16),
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color(0xFFB0B4AA),
        ),
        border = if (filled) null else BorderStroke(1.5.dp, Color(0xFFE6E8E0)),
        elevation = ButtonDefaults.buttonElevation(0.dp),
    ) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
fun SoftField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1,
    isError: Boolean = false,
    supportingText: String? = null,
    prefix: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    val shape = RoundedCornerShape(22.dp)
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = minLines,
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        prefix = prefix?.let { { Text(it) } },
        keyboardOptions = keyboardOptions,
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Cute.Peach,
            unfocusedBorderColor = Color(0xFFE6E8E0),
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

@Composable
fun ChoiceChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bg = if (selected) Cute.PeachSoft else MaterialTheme.colorScheme.surface
    val border = if (selected) Cute.Peach else Color(0xFFE6E8E0)
    val fg = if (selected) Color(0xFF5A2A12) else Cute.Ink
    Text(
        text = label,
        color = fg,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .border(1.5.dp, border, RoundedCornerShape(22.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

@Composable
fun StatusBadge(status: RequestStatus, modifier: Modifier = Modifier) {
    val (bg, fg) = when (status) {
        RequestStatus.PENDING -> Cute.SkySoft to Color(0xFF0C3A46)
        RequestStatus.APPROVED -> Cute.MintSoft to Color(0xFF1B5A32)
        RequestStatus.REJECTED -> Color(0xFFFFE2E0) to Color(0xFF6B1A16)
    }
    Text(
        text = status.label,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
fun CategoryBubble(category: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Cute.PeachSoft),
        contentAlignment = Alignment.Center,
    ) {
        Text(CATEGORY_EMOJI[category] ?: "✨", style = MaterialTheme.typography.titleLarge)
    }
}

@Composable
fun EmptyHint(kind: MascotKind, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Mascot(kind, size = 150.dp)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, color = Cute.Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun RowScope.RoleCard(title: String, emoji: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) Cute.PeachSoft else MaterialTheme.colorScheme.surface
    val border = if (selected) Cute.Peach else Color(0xFFE6E8E0)
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(24.dp))
            .border(2.dp, border, RoundedCornerShape(24.dp))
            .background(bg)
            .clickable(onClick = onClick)
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(emoji, style = MaterialTheme.typography.headlineSmall)
        Text(title, style = MaterialTheme.typography.titleMedium, color = if (selected) Color(0xFF5A2A12) else Cute.Ink)
        if (selected) Text("当前身份", style = MaterialTheme.typography.labelMedium, color = Cute.Peach)
    }
}
