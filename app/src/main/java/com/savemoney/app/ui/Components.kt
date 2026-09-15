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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.outlined.Checkroom
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SportsEsports
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.savemoney.app.data.RequestStatus
import com.savemoney.app.ui.theme.Palette
import java.io.File

data class CategoryLook(
    val icon: ImageVector,
    val accent: Color,
    val wash: Color,
)

fun categoryLook(name: String): CategoryLook = when (name) {
    "餐饮" -> CategoryLook(Icons.Outlined.Restaurant, Palette.Coral, Palette.CoralSoft)
    "日用品" -> CategoryLook(Icons.Outlined.Home, Palette.Sky, Palette.SkySoft)
    "服饰" -> CategoryLook(Icons.Outlined.Checkroom, Palette.Lavender, Palette.LavenderSoft)
    "数码" -> CategoryLook(Icons.Outlined.Devices, Color(0xFFD4A84B), Color(0xFFF8EFC8))
    "交通" -> CategoryLook(Icons.Outlined.DirectionsBus, Palette.Mint, Palette.MintSoft)
    "娱乐" -> CategoryLook(Icons.Outlined.SportsEsports, Color(0xFFE89A5C), Color(0xFFFBE6D4))
    "学习" -> CategoryLook(Icons.AutoMirrored.Outlined.MenuBook, Color(0xFF7B9FD4), Color(0xFFDCE6F6))
    "医疗" -> CategoryLook(Icons.Outlined.FavoriteBorder, Color(0xFFD98BA8), Color(0xFFF8DCE6))
    else -> CategoryLook(Icons.Outlined.MoreHoriz, Palette.Muted, Color(0xFFEEF1F5))
}

@Composable
fun SoftCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Palette.Line, shape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(18.dp),
        content = content,
    )
}

@Composable
fun PageHeader(
    title: String,
    subtitle: String,
    onBack: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp)) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onBack)
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, Palette.Line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = Palette.Ink)
            }
            Spacer(Modifier.height(14.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = Palette.Muted)
    }
}

@Composable
fun PillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    filled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(28.dp),
        colors = if (filled) ButtonDefaults.buttonColors(
            containerColor = Palette.Coral,
            contentColor = Color.White,
            disabledContainerColor = Color(0xFFE6EAF0),
            disabledContentColor = Color(0xFFB0B7C2),
        ) else ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = Palette.Ink,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = Color(0xFFB0B7C2),
        ),
        border = if (filled) null else BorderStroke(1.5.dp, Palette.Line),
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
    val shape = RoundedCornerShape(18.dp)
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
            focusedBorderColor = Palette.Sky,
            unfocusedBorderColor = Palette.Line,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            cursorColor = Palette.Sky,
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
    val shape = RoundedCornerShape(22.dp)
    val bg = if (selected) Palette.Sky else MaterialTheme.colorScheme.surface
    val border = if (selected) Palette.Sky else Palette.Line
    val fg = if (selected) Color.White else Palette.Ink
    Text(
        text = label,
        color = fg,
        style = MaterialTheme.typography.labelLarge,
        modifier = modifier
            .clip(shape)
            .border(1.dp, border, shape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 9.dp),
    )
}

@Composable
fun StatusBadge(status: RequestStatus, modifier: Modifier = Modifier, partial: Boolean = false) {
    val (bg, fg, label) = when {
        status == RequestStatus.PENDING -> Triple(Palette.Cream, Color(0xFF8A6A20), status.label)
        status == RequestStatus.REJECTED -> Triple(Color(0xFFFBE3E3), Color(0xFF8A2E2E), status.label)
        partial -> Triple(Palette.CoralSoft, Color(0xFF8A3A24), "部分通过")
        else -> Triple(Palette.MintSoft, Color(0xFF1B5A48), status.label)
    }
    Text(
        text = label,
        style = MaterialTheme.typography.labelMedium,
        color = fg,
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 5.dp),
    )
}

@Composable
fun CategoryChip(category: String, modifier: Modifier = Modifier) {
    val look = categoryLook(category)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(look.wash)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(look.accent),
        )
        Text(category, style = MaterialTheme.typography.labelMedium, color = Palette.Ink)
    }
}

@Composable
fun CategoryBubble(category: String, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    val look = categoryLook(category)
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(16.dp))
            .background(look.wash),
        contentAlignment = Alignment.Center,
    ) {
        Icon(look.icon, contentDescription = category, tint = look.accent, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
fun RequestThumb(category: String, imagePath: String?, modifier: Modifier = Modifier) {
    if (imagePath.isNullOrBlank()) {
        CategoryBubble(category, modifier)
    } else {
        PhotoSlot(model = imagePath, modifier = modifier.size(56.dp), showEmpty = false)
    }
}

@Composable
fun PhotoSlot(
    model: String?,
    modifier: Modifier = Modifier,
    showEmpty: Boolean = true,
    emptyLabel: String = "加点照片，给审核的人看看",
    onClick: (() -> Unit)? = null,
    onClear: (() -> Unit)? = null,
) {
    val imageModel: Any? = when {
        model.isNullOrBlank() -> null
        model.startsWith("/") -> File(model)
        else -> model
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Palette.SkySoft)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        if (imageModel != null) {
            AsyncImage(
                model = imageModel,
                contentDescription = "物品照片",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            if (onClear != null) {
                Text(
                    "✕",
                    color = Palette.Ink,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.92f))
                        .clickable(onClick = onClear)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        } else if (showEmpty) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text("📷", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(6.dp))
                Text(emptyLabel, color = Palette.Ink, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun EmptyHint(kind: MascotKind, title: String, subtitle: String) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Mascot(kind, size = 128.dp)
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(subtitle, color = Palette.Muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun MoneyText(
    cents: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color = Palette.Coral,
) {
    Text(
        text = cents.toYuan(),
        modifier = modifier,
        style = style.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold),
        color = color,
    )
}

@Composable
fun NameDot(name: String, fill: Color, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, Color.White, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.take(1),
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
            fontSize = (size.value * 0.38f).sp,
        )
    }
}
