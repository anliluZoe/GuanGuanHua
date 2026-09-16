package com.savemoney.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.savemoney.app.data.RequestStatus
import com.savemoney.app.ui.theme.Appearance
import com.savemoney.app.ui.theme.QTheme
import java.io.File

data class CategoryLook(
    val icon: ImageVector,
    val accent: Color,
    val wash: Color,
)

@Composable
fun categoryLook(name: String): CategoryLook {
    val colors = QTheme.colors
    return when (name) {
        "餐饮" -> CategoryLook(Icons.Outlined.Restaurant, colors.coral, colors.coralSoft)
        "日用品" -> CategoryLook(Icons.Outlined.Home, colors.sky, colors.skySoft)
        "服饰" -> CategoryLook(Icons.Outlined.Checkroom, colors.lavender, colors.lavenderSoft)
        "数码" -> CategoryLook(Icons.Outlined.Devices, colors.gold, colors.goldSoft)
        "交通" -> CategoryLook(Icons.Outlined.DirectionsBus, colors.mint, colors.mintSoft)
        "娱乐" -> CategoryLook(Icons.Outlined.SportsEsports, colors.peach, colors.peachSoft)
        "学习" -> CategoryLook(Icons.AutoMirrored.Outlined.MenuBook, colors.denim, colors.denimSoft)
        "医疗" -> CategoryLook(Icons.Outlined.FavoriteBorder, colors.rose, colors.roseSoft)
        else -> CategoryLook(Icons.Outlined.MoreHoriz, colors.muted, colors.chipWash)
    }
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
            .border(1.dp, QTheme.colors.line, shape)
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
                    .border(1.dp, QTheme.colors.line, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回", tint = QTheme.colors.ink)
            }
            Spacer(Modifier.height(14.dp))
        }
        Text(title, style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(4.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = QTheme.colors.muted)
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
    val colors = QTheme.colors
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(54.dp),
        shape = RoundedCornerShape(28.dp),
        colors = if (filled) ButtonDefaults.buttonColors(
            containerColor = colors.coral,
            contentColor = Color.White,
            disabledContainerColor = colors.disabledFill,
            disabledContentColor = colors.disabledInk,
        ) else ButtonDefaults.buttonColors(
            containerColor = Color.Transparent,
            contentColor = colors.ink,
            disabledContainerColor = Color.Transparent,
            disabledContentColor = colors.disabledInk,
        ),
        border = if (filled) null else BorderStroke(1.5.dp, colors.line),
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
    val colors = QTheme.colors
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
            focusedBorderColor = colors.sky,
            unfocusedBorderColor = colors.line,
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
            focusedTextColor = colors.ink,
            unfocusedTextColor = colors.ink,
            focusedLabelColor = colors.sky,
            unfocusedLabelColor = colors.muted,
            cursorColor = colors.sky,
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
    val colors = QTheme.colors
    val shape = RoundedCornerShape(22.dp)
    val bg = if (selected) colors.sky else MaterialTheme.colorScheme.surface
    val border = if (selected) colors.sky else colors.line
    val fg = if (selected) Color.White else colors.ink
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
    val colors = QTheme.colors
    val (bg, fg, label) = when {
        status == RequestStatus.PENDING -> Triple(colors.cream, colors.pendingInk, status.label)
        status == RequestStatus.REJECTED -> Triple(colors.rejectedWash, colors.rejectedInk, status.label)
        partial -> Triple(colors.coralSoft, colors.partialInk, "部分通过")
        else -> Triple(colors.mintSoft, colors.approvedInk, status.label)
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
        Text(category, style = MaterialTheme.typography.labelMedium, color = QTheme.colors.ink)
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
    val colors = QTheme.colors
    val imageModel: Any? = when {
        model.isNullOrBlank() -> null
        model.startsWith("/") -> File(model)
        else -> model
    }
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(colors.skySoft)
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
                    color = colors.ink,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(8.dp)
                        .clip(CircleShape)
                        .background(colors.overlay)
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
                Text(emptyLabel, color = colors.ink, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
fun CoralProgress(modifier: Modifier = Modifier) {
    val colors = QTheme.colors
    CircularProgressIndicator(
        modifier = modifier.size(36.dp),
        color = colors.coral,
        strokeWidth = 3.dp,
        trackColor = colors.coralSoft,
    )
}

@Composable
fun RefreshBar(visible: Boolean, modifier: Modifier = Modifier) {
    if (visible) {
        val colors = QTheme.colors
        LinearProgressIndicator(
            modifier = modifier.fillMaxWidth().height(3.dp),
            color = colors.coral,
            trackColor = colors.coralSoft,
        )
    }
}

@Composable
fun LoadingHint(title: String = "稍等一下哦…") {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        CoralProgress()
        Text(title, color = QTheme.colors.muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun LoadingScrim(visible: Boolean, hint: String = "稍等一下哦…") {
    if (!visible) return
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background.copy(alpha = 0.72f))
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent()
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        SoftCard(modifier = Modifier.padding(horizontal = 28.dp)) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Mascot(MascotKind.Cat, size = 72.dp)
                CoralProgress()
                Text(hint, color = QTheme.colors.muted, style = MaterialTheme.typography.bodyMedium)
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
        Text(subtitle, color = QTheme.colors.muted, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
fun MoneyText(
    cents: Long,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.titleLarge,
    color: Color? = null,
) {
    Text(
        text = cents.toYuan(),
        modifier = modifier,
        style = style.copy(fontFeatureSettings = "tnum", fontWeight = FontWeight.Bold),
        color = color ?: QTheme.colors.coral,
    )
}

@Composable
fun NameDot(name: String, fill: Color, modifier: Modifier = Modifier, size: Dp = 36.dp) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(fill)
            .border(2.dp, QTheme.colors.canvas, CircleShape),
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

@Composable
fun AppearancePicker(
    value: Appearance,
    onChange: (Appearance) -> Unit,
    modifier: Modifier = Modifier,
) {
    SoftCard(modifier = modifier.fillMaxWidth()) {
        Text("外观", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            "浅色是现在的清爽 Q 版；深色用深夜画布，珊瑚只留给金额和主按钮。",
            style = MaterialTheme.typography.bodySmall,
            color = QTheme.colors.muted,
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ChoiceChip("跟随系统", value == Appearance.System, onClick = { onChange(Appearance.System) })
            ChoiceChip("浅色", value == Appearance.Light, onClick = { onChange(Appearance.Light) })
            ChoiceChip("深色", value == Appearance.Dark, onClick = { onChange(Appearance.Dark) })
        }
    }
}
