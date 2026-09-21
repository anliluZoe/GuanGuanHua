package com.guanguanhua.app.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanguanhua.app.AppViewModel
import com.guanguanhua.app.ui.theme.QTheme
import com.guanguanhua.app.widget.HouseholdWidgetReceiver
import com.guanguanhua.app.widget.WidgetCopy

@Composable
fun WidgetScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val widget by viewModel.widget.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    var caption by rememberSaveable(widget.caption) { mutableStateOf(widget.caption) }
    var captionColor by rememberSaveable(widget.captionColor) {
        mutableStateOf(WidgetCopy.normalizeCaptionColor(widget.captionColor))
    }
    var customHex by rememberSaveable { mutableStateOf(captionColor) }
    var pickingCustom by rememberSaveable { mutableStateOf(false) }
    var pinHint by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.uploadWidgetPhoto(uri)
    }
    val normalizedColor = WidgetCopy.normalizeCaptionColor(captionColor)
    val customSelected = WidgetCopy.CAPTION_COLOR_PRESETS.none { it.equals(normalizedColor, ignoreCase = true) }
    val canSave = WidgetCopy.clampCaption(caption) != widget.caption ||
        normalizedColor != WidgetCopy.normalizeCaptionColor(widget.captionColor)

    LaunchedEffect(session.joined) {
        if (session.joined) viewModel.refreshWidget()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QTheme.colors.screenGlow)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        PageHeader("桌面组件", "一张合照、一句说明叠在正中间，两部手机一起换", onBack = onBack)
        if (!session.joined) {
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("先加入家庭账本，才能设置桌面照片。", color = QTheme.colors.muted)
            }
            Spacer(Modifier.height(24.dp))
        } else {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(20.dp))
                .clickable {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
            contentAlignment = Alignment.Center,
        ) {
            PhotoSlot(
                model = widget.imageUrl ?: widget.localImagePath,
                modifier = Modifier.fillMaxSize(),
                showEmpty = false,
            )
            val previewCaption = WidgetCopy.clampCaption(caption)
            if (previewCaption.isNotBlank()) {
                Text(
                    text = previewCaption,
                    color = Color(WidgetCopy.captionColorArgb(captionColor)),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                )
            } else if (widget.imageUrl.isNullOrBlank() && widget.localImagePath.isNullOrBlank()) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(WidgetCopy.EMPTY_PHOTO, color = QTheme.colors.inkSoft, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(WidgetCopy.EMPTY_PHOTO_HINT, color = QTheme.colors.muted, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            SoftField(
                value = caption,
                onValueChange = { caption = it.take(WidgetCopy.MAX_CAPTION) },
                label = "写一句放在照片正中间（可选）",
                supportingText = "${caption.length}/${WidgetCopy.MAX_CAPTION}",
            )
            Spacer(Modifier.height(14.dp))
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WidgetCopy.CAPTION_COLOR_PRESETS.forEach { preset ->
                    val selected = preset.equals(normalizedColor, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(WidgetCopy.captionColorArgb(preset)))
                            .border(
                                width = if (selected) 3.dp else 1.dp,
                                color = if (selected) QTheme.colors.sky else QTheme.colors.lineStrong,
                                shape = CircleShape,
                            )
                            .clickable { captionColor = preset },
                    )
                }
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .then(
                            if (customSelected) {
                                Modifier.background(Color(WidgetCopy.captionColorArgb(captionColor)))
                            } else {
                                Modifier.background(
                                    Brush.sweepGradient(
                                        listOf(
                                            Color(0xFFFF6B6B),
                                            Color(0xFFFFD93D),
                                            Color(0xFF6BCB77),
                                            Color(0xFF4D96FF),
                                            Color(0xFFB983FF),
                                            Color(0xFFFF6B6B),
                                        ),
                                    ),
                                )
                            },
                        )
                        .border(
                            width = if (customSelected) 3.dp else 1.dp,
                            color = if (customSelected) QTheme.colors.sky else QTheme.colors.lineStrong,
                            shape = CircleShape,
                        )
                        .clickable {
                            customHex = captionColor
                            pickingCustom = true
                        },
                )
            }
            if (!widget.updatedBy.isNullOrBlank()) {
                Spacer(Modifier.height(10.dp))
                Text(
                    buildString {
                        append("上次是 ${widget.updatedBy} 改的")
                        widget.updatedAt?.let { append(" · ${it.toDateTimeText()}") }
                    },
                    color = QTheme.colors.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(
                    "保存文案",
                    modifier = Modifier.weight(1f),
                    filled = false,
                    enabled = !isBusy && canSave,
                    onClick = { viewModel.saveWidgetCaption(caption, captionColor) },
                )
                PillButton(
                    "上传照片",
                    modifier = Modifier.weight(1f),
                    enabled = !isBusy,
                    onClick = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                "真机需在系统桌面「添加小部件 → 管管花」。照片铺满，说明叠在正中间。",
                color = QTheme.colors.muted,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("加到桌面", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "长按桌面空白处 → 小组件 / 微件 → 找到「管管花」拖上去。照片铺满组件，说明叠在正中间，没有「管管花」底栏。",
                color = QTheme.colors.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "这部手机保存后会马上刷新；另一部大约每 15 分钟同步一次，也可以打开 App 再拉一次。",
                color = QTheme.colors.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            if (!pinHint.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(pinHint!!, color = QTheme.colors.sky, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(Modifier.height(16.dp))
            PillButton(
                "添加到桌面",
                filled = false,
                enabled = !isBusy && !isRefreshing,
                onClick = {
                    val manager = AppWidgetManager.getInstance(context)
                    val provider = ComponentName(context, HouseholdWidgetReceiver::class.java)
                    pinHint = if (!manager.isRequestPinAppWidgetSupported) {
                        "这个桌面不支持一键添加，请长按桌面空白处，在小组件里找「管管花」。"
                    } else if (manager.requestPinAppWidget(provider, null, null)) {
                        "系统会弹出添加确认，选一块空位就好。"
                    } else {
                        "没能调起添加面板，请长按桌面空白处手动加。"
                    }
                },
            )
        }
        Spacer(Modifier.height(24.dp))
        }
    }

    if (pickingCustom) {
        val parsedCustom = WidgetCopy.parseCaptionColorRgb(customHex)
        AlertDialog(
            onDismissRequest = { pickingCustom = false },
            title = { Text("自定义颜色") },
            text = {
                Column {
                    SoftField(
                        value = customHex,
                        onValueChange = { customHex = it.take(7) },
                        label = "颜色（#RRGGBB）",
                        isError = customHex.isNotBlank() && parsedCustom == null,
                        supportingText = if (parsedCustom == null) "用 #RGB 或 #RRGGBB" else null,
                    )
                    if (parsedCustom != null) {
                        Spacer(Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(Color(WidgetCopy.captionColorArgb(customHex)))
                                .border(1.dp, QTheme.colors.lineStrong, CircleShape),
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = parsedCustom != null,
                    onClick = {
                        captionColor = WidgetCopy.normalizeCaptionColor(customHex)
                        pickingCustom = false
                    },
                ) { Text("用这个颜色", color = QTheme.colors.sky) }
            },
            dismissButton = {
                TextButton(onClick = { pickingCustom = false }) { Text("取消") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }
}
