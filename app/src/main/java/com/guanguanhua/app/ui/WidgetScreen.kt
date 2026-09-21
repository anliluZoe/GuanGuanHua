package com.guanguanhua.app.ui

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    var pinHint by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) viewModel.uploadWidgetPhoto(uri)
    }

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
        PageHeader("桌面组件", "一张合照、一句说明，两部手机一起换", onBack = onBack)
        if (!session.joined) {
            SoftCard(modifier = Modifier.fillMaxWidth()) {
                Text("先加入家庭账本，才能设置桌面照片。", color = QTheme.colors.muted)
            }
            Spacer(Modifier.height(24.dp))
        } else {
        PhotoSlot(
            model = widget.imageUrl ?: widget.localImagePath,
            modifier = Modifier.fillMaxWidth().height(220.dp),
            emptyLabel = WidgetCopy.EMPTY_PHOTO,
            onClick = {
                photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            },
        )
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("共享说明", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "谁改了，两边的桌面都会更新。建议不超过 ${WidgetCopy.MAX_CAPTION} 字。",
                color = QTheme.colors.muted,
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            SoftField(
                value = caption,
                onValueChange = { caption = it.take(WidgetCopy.MAX_CAPTION) },
                label = "一句话",
                supportingText = "${caption.length}/${WidgetCopy.MAX_CAPTION}",
            )
            if (!widget.updatedBy.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
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
            PillButton(
                "保存说明",
                enabled = !isBusy && WidgetCopy.clampCaption(caption) != widget.caption,
                onClick = { viewModel.saveWidgetCaption(caption) },
            )
        }
        SoftCard(modifier = Modifier.fillMaxWidth()) {
            Text("加到桌面", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "长按桌面空白处 → 小组件 / 微件 → 找到「管管花」拖上去。照片铺满组件，下面是说明和「管管花」。",
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
}
