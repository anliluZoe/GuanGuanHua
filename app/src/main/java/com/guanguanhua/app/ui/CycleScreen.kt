package com.guanguanhua.app.ui

import android.content.ClipData
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanguanhua.app.AppViewModel
import com.guanguanhua.app.cycle.CycleMath
import com.guanguanhua.app.data.CycleRecord
import com.guanguanhua.app.data.CycleSettings
import com.guanguanhua.app.ui.theme.QTheme
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset

private enum class CycleSheet { Record, Edit, Settings }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleScreen(viewModel: AppViewModel) {
    val cycle by viewModel.cycle.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val isReady by viewModel.isReady.collectAsStateWithLifecycle()
    var monthKey by rememberSaveable { mutableStateOf(YearMonth.now().toString()) }
    val month = YearMonth.parse(monthKey)
    var sheet by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedIso by rememberSaveable { mutableStateOf<String?>(null) }
    var editingId by rememberSaveable { mutableStateOf(0L) }
    val prediction = CycleMath.predict(cycle.cycles, cycle.settings)
    val periodDates = CycleMath.periodDates(cycle.cycles)
    val today = LocalDate.now()
    val context = LocalContext.current
    val activeSheet = sheet?.let { runCatching { CycleSheet.valueOf(it) }.getOrNull() }
    val editing = cycle.cycles.firstOrNull { it.id == editingId }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QTheme.colors.screenGlow)
            .padding(horizontal = 20.dp),
    ) {
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("周期", style = androidx.compose.material3.MaterialTheme.typography.headlineSmall, color = QTheme.colors.ink)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (session.joined) "轻轻记一下，下次大概心里有数" else "先去「我们」加入家庭，记录才会跟着你的身份走",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = QTheme.colors.secondary,
                )
            }
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(QTheme.colors.paper)
                    .border(1.dp, QTheme.colors.lineStrong, CircleShape)
                    .clickable { sheet = CycleSheet.Settings.name },
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Settings, contentDescription = "周期设置", tint = QTheme.colors.ink)
            }
        }
        PullRefreshBox(
            isRefreshing = isRefreshing && isReady,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.weight(1f).fillMaxWidth(),
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    PredictCard(prediction)
                }
                item {
                    CalendarCard(
                        month = month,
                        today = today,
                        periodDates = periodDates,
                        predict = prediction.nextStart,
                        ovulation = prediction.ovulation,
                        onPrev = { monthKey = month.minusMonths(1).toString() },
                        onNext = { monthKey = month.plusMonths(1).toString() },
                        onDay = { date ->
                            selectedIso = date.toString()
                            sheet = CycleSheet.Record.name
                        },
                    )
                }
                item {
                    RemindCard(
                        enabled = cycle.settings.remindEnabled,
                        joined = session.joined,
                        busy = isBusy,
                        onChange = { viewModel.setCycleRemind(it) },
                    )
                }
                item {
                    Column {
                        Text("历史记录", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                        Text(
                            "可改起止 · 暂不支持删除",
                            color = QTheme.colors.muted,
                            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        )
                    }
                }
                if (cycle.cycles.isEmpty()) {
                    item {
                        Text(
                            if (session.joined) "还没有记录，点日历某天开始记吧" else "加入家庭后，点日历就能记",
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(22.dp))
                                .border(1.dp, QTheme.colors.lineStrong, RoundedCornerShape(22.dp))
                                .padding(vertical = 28.dp, horizontal = 16.dp),
                            color = QTheme.colors.muted,
                            textAlign = TextAlign.Center,
                            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                        )
                    }
                } else {
                    items(cycle.cycles.sortedWith(compareByDescending<CycleRecord> { it.start }.thenByDescending { it.id }), key = { it.id }) { record ->
                        HistoryRow(record) {
                            editingId = record.id
                            sheet = CycleSheet.Edit.name
                        }
                    }
                }
                item {
                    PrivacyTip(
                        title = "隐私小提示",
                        body = "数据跟着当前登录成员走；另一半平时看不到。用家庭码切身份时仍可能被看到——记得多留点心就好。",
                    )
                }
            }
        }
    }

    if (activeSheet != null) {
        ModalBottomSheet(
            onDismissRequest = { if (!isBusy) sheet = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = QTheme.colors.paper,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            when (activeSheet) {
                CycleSheet.Record -> RecordSheet(
                    iso = selectedIso,
                    busy = isBusy,
                    joined = session.joined,
                    onStart = {
                        selectedIso?.let(viewModel::recordCycleStart)
                        sheet = null
                    },
                    onEnd = {
                        selectedIso?.let(viewModel::recordCycleEnd)
                        sheet = null
                    },
                    onClose = { sheet = null },
                )
                CycleSheet.Edit -> if (editing != null) {
                    EditSheet(
                        record = editing,
                        busy = isBusy,
                        onSave = { start, end ->
                            viewModel.saveCycle(editing.id, start, end)
                            sheet = null
                        },
                        onDelete = { viewModel.postStatus("删不掉是故意的——记录先留着") },
                        onClose = { sheet = null },
                    )
                }
                CycleSheet.Settings -> SettingsSheet(
                    settings = cycle.settings,
                    prediction = prediction,
                    busy = isBusy,
                    joined = session.joined,
                    onReference = viewModel::setCycleReference,
                    onPeriod = viewModel::setCyclePeriodDays,
                    onRemind = viewModel::setCycleRemind,
                    onExport = {
                        val csv = CycleMath.toCsv(cycle.cycles)
                        val name = "guanguanhua-cycle-${LocalDate.now()}.csv"
                        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
                        val file = File(dir, name)
                        file.writeText(csv, Charsets.UTF_8)
                        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/csv"
                            putExtra(Intent.EXTRA_STREAM, uri)
                            putExtra(Intent.EXTRA_SUBJECT, name)
                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            clipData = ClipData.newRawUri(name, uri)
                        }
                        val launched = runCatching {
                            context.startActivity(Intent.createChooser(send, "导出 CSV"))
                        }.isSuccess
                        viewModel.postStatus(if (launched) "导出好啦 · $name" else "没能打开分享")
                    },
                    onClose = { sheet = null },
                )
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PredictCard(prediction: com.guanguanhua.app.cycle.CyclePrediction) {
    val q = QTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(q.lavenderSoft)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "下次大概",
                color = q.lavender,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(q.paper.copy(alpha = 0.72f))
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            )
            Spacer(Modifier.width(10.dp))
            Text(
                CycleMath.predictDateText(prediction),
                color = q.ink,
                style = androidx.compose.material3.MaterialTheme.typography.titleLarge,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(CycleMath.predictMeta(prediction), color = q.secondary, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(6.dp))
        Text(CycleMath.predictOvuText(prediction), color = q.lavender, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CalendarCard(
    month: YearMonth,
    today: LocalDate,
    periodDates: Set<LocalDate>,
    predict: LocalDate?,
    ovulation: LocalDate?,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onDay: (LocalDate) -> Unit,
) {
    val q = QTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(q.paper)
            .border(1.dp, q.line, RoundedCornerShape(22.dp))
            .padding(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "上一月", tint = q.ink)
            }
            Text(
                "${month.year}年${month.monthValue}月",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center,
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            )
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "下一月", tint = q.ink)
            }
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            listOf("日", "一", "二", "三", "四", "五", "六").forEach { label ->
                Text(
                    label,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    color = q.muted2,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        val pad = month.atDay(1).dayOfWeek.value % 7
        val cells = List(pad) { null as LocalDate? } + (1..month.lengthOfMonth()).map { month.atDay(it) }
        val padded = cells + List((7 - cells.size % 7) % 7) { null }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val cell = maxWidth / 7
            padded.chunked(7).forEach { week ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    week.forEach { date ->
                        if (date == null) {
                            Spacer(Modifier.size(cell))
                        } else {
                            val isPeriod = date in periodDates
                            val isPredict = predict == date && !isPeriod
                            val isOvu = ovulation == date && !isPeriod && predict != date
                            val isToday = date == today
                            val bg = when {
                                isPeriod -> q.roseSoft
                                isOvu -> q.lavenderSoft
                                else -> Color.Transparent
                            }
                            val fg = when {
                                isPeriod -> q.rose
                                isPredict -> q.coral
                                isOvu -> q.lavender
                                else -> q.ink
                            }
                            val borderColor = when {
                                isToday && isPeriod -> q.rose
                                isToday -> q.sky
                                else -> Color.Transparent
                            }
                            Box(
                                modifier = Modifier
                                    .size(cell)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(bg)
                                    .border(if (isToday) 1.5.dp else 0.dp, borderColor, RoundedCornerShape(14.dp))
                                    .clickable { onDay(date) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(date.dayOfMonth.toString(), color = fg, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                if (isPredict || isOvu) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .padding(bottom = 5.dp)
                                            .size(5.dp)
                                            .clip(CircleShape)
                                            .background(if (isPredict) q.coral else q.lavender),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            LegendDot(q.rose, "已记录")
            LegendDot(q.coral, "下次大概")
            LegendDot(q.lavender, "排卵大概")
            LegendDot(q.sky, "今天", ring = true)
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "排卵＝下次经期首日 −14，只作参考；周期不稳时别当真。",
            color = q.muted,
            fontSize = 11.sp,
            lineHeight = 16.sp,
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String, ring: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (ring) Color.Transparent else color)
                .border(if (ring) 1.5.dp else 0.dp, if (ring) color else Color.Transparent, CircleShape),
        )
        Spacer(Modifier.width(4.dp))
        Text(label, color = QTheme.colors.muted, fontSize = 11.sp)
    }
}

@Composable
private fun RemindCard(enabled: Boolean, joined: Boolean, busy: Boolean, onChange: (Boolean) -> Unit) {
    val q = QTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(q.paper)
            .border(1.dp, q.line, RoundedCornerShape(22.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("经期提醒", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                Text("默认提前 2 天提醒", color = q.muted, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            }
            Switch(
                checked = enabled,
                onCheckedChange = onChange,
                enabled = joined && !busy,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = q.mint,
                    checkedThumbColor = Color.White,
                    uncheckedThumbColor = Color.White,
                    uncheckedBorderColor = q.lineStrong,
                ),
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            if (enabled) "开着的话，大概前两天轻轻戳你一下～" else "提醒关着啦，想开随时拨回来。",
            color = q.muted,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
private fun HistoryRow(record: CycleRecord, onClick: () -> Unit) {
    val q = QTheme.colors
    val start = CycleMath.parse(record.start)
    val end = CycleMath.parse(record.end)
    val duration = CycleMath.durationDays(record.start, record.end)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(q.paper)
            .border(1.dp, q.line, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                "${start?.let(CycleMath::formatCn) ?: record.start} → ${if (end == null) "未填结束" else CycleMath.formatCn(end)}",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
            )
            Text(
                if (duration == null) "结束日还空着 · 点这里改" else "共 $duration 天 · 点这里改",
                color = q.muted,
                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            )
        }
        Text(
            "编辑",
            color = q.sky,
            style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
            modifier = Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(q.skySoft)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun PrivacyTip(title: String, body: String) {
    val q = QTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(q.lavenderSoft)
            .padding(14.dp),
    ) {
        Text(title, color = q.lavender, style = androidx.compose.material3.MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(body, color = q.inkSoft, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun RecordSheet(
    iso: String?,
    busy: Boolean,
    joined: Boolean,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    onClose: () -> Unit,
) {
    val date = CycleMath.parse(iso)
    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("记录这一天", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Text(date?.let(CycleMath::formatCnFull) ?: "—", color = QTheme.colors.muted)
        PillButton("设为开始日", onClick = onStart, enabled = joined && !busy && date != null)
        PillButton("设为结束日", onClick = onEnd, enabled = joined && !busy && date != null, approve = true)
        PillButton("先不了", onClick = onClose, filled = false, enabled = !busy)
        Text(
            "结束日可以先空着，回头再补也行～",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = QTheme.colors.muted2,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditSheet(
    record: CycleRecord,
    busy: Boolean,
    onSave: (String, String?) -> Unit,
    onDelete: () -> Unit,
    onClose: () -> Unit,
) {
    var start by rememberSaveable(record.id) { mutableStateOf(record.start) }
    var end by rememberSaveable(record.id) { mutableStateOf(record.end.orEmpty()) }
    var picking by rememberSaveable { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("改一改这条", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Text("起止可以调；删除暂不开放", color = QTheme.colors.muted, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        DateField("开始日", CycleMath.parse(start)?.let(CycleMath::formatCnFull) ?: "选一天") { picking = "start" }
        DateField("结束日 可空", CycleMath.parse(end)?.let(CycleMath::formatCnFull) ?: "可以先空着") { picking = "end" }
        if (end.isNotBlank()) {
            TextButton(onClick = { end = "" }) { Text("清空结束日", color = QTheme.colors.sky) }
        }
        PillButton("保存", enabled = !busy && start.isNotBlank(), onClick = { onSave(start, end.ifBlank { null }) })
        PillButton("取消", filled = false, enabled = !busy, onClick = onClose)
        Text(
            "删除（暂不可用）",
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(28.dp))
                .background(QTheme.colors.disabledFill)
                .border(1.dp, QTheme.colors.lineStrong, RoundedCornerShape(28.dp))
                .clickable(onClick = onDelete)
                .padding(vertical = 12.dp),
            color = QTheme.colors.disabledInk,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "删不掉是故意的——记录先留着。",
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
            color = QTheme.colors.muted2,
            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
        )
    }
    val initial = CycleMath.parse(if (picking == "end") end.ifBlank { start } else start)
    if (picking != null && initial != null) {
        val state = rememberDatePickerState(initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
        DatePickerDialog(
            onDismissRequest = { picking = null },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        val picked = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                        if (picking == "end") end = picked else start = picked
                    }
                    picking = null
                }) { Text("好") }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun DateField(label: String, value: String, onClick: () -> Unit) {
    val q = QTheme.colors
    Column {
        Text(label, color = q.muted, style = androidx.compose.material3.MaterialTheme.typography.labelMedium)
        Spacer(Modifier.height(6.dp))
        Text(
            value,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .background(if (q.isDark) q.sandDeep else q.canvas)
                .border(1.dp, q.lineStrong, RoundedCornerShape(18.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 14.dp),
            color = q.ink,
        )
    }
}

@Composable
private fun SettingsSheet(
    settings: CycleSettings,
    prediction: com.guanguanhua.app.cycle.CyclePrediction,
    busy: Boolean,
    joined: Boolean,
    onReference: (Int?) -> Unit,
    onPeriod: (Int) -> Unit,
    onRemind: (Boolean) -> Unit,
    onExport: () -> Unit,
    onClose: () -> Unit,
) {
    val q = QTheme.colors
    val manual = settings.referenceCycleDays != null
    val shownReference = settings.referenceCycleDays ?: CycleMath.suggestedReference(prediction)
    Column(
        modifier = Modifier
            .heightIn(max = 640.dp)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("提醒与导出", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
        Text("周期天数、经期天数、提醒和导出", color = q.muted, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        SettingsBlock {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("提前 2 天提醒", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                    Text("默认开启，可随手关掉", color = q.muted, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
                }
                Switch(
                    checked = settings.remindEnabled,
                    onCheckedChange = onRemind,
                    enabled = joined && !busy,
                    colors = SwitchDefaults.colors(checkedTrackColor = q.mint, checkedThumbColor = Color.White, uncheckedThumbColor = Color.White),
                )
            }
        }
        SettingsBlock {
            Text("我的周期", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text(
                "周期＝这次首日到下次首日。有足够记录优先用你的平均（21–35 天）；不够时才用手填或默认 28。",
                color = q.muted,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ChoiceChip("智能平均", selected = !manual, onClick = { onReference(null) }, modifier = Modifier.weight(1f))
                ChoiceChip("参考天数", selected = manual, onClick = { if (!manual) onReference(shownReference) }, modifier = Modifier.weight(1f))
            }
            StepperRow(
                label = "周期天数",
                value = shownReference,
                enabled = joined && !busy,
                onMinus = { onReference((shownReference - 1).coerceAtLeast(CycleMath.REF_MIN)) },
                onPlus = { onReference((shownReference + 1).coerceAtMost(CycleMath.REF_MAX)) },
            )
            Spacer(Modifier.height(8.dp))
            Text(CycleMath.settingsHint(prediction), color = q.muted, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
        }
        SettingsBlock {
            Text("每次大概几天", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("记开始日时，会按这个天数先填上结束日，之后还能改。", color = q.muted, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            StepperRow(
                label = "经期天数",
                value = CycleMath.periodDays(settings),
                enabled = joined && !busy,
                onMinus = { onPeriod((CycleMath.periodDays(settings) - 1).coerceAtLeast(CycleMath.PERIOD_MIN)) },
                onPlus = { onPeriod((CycleMath.periodDays(settings) + 1).coerceAtMost(CycleMath.PERIOD_MAX)) },
            )
        }
        SettingsBlock {
            Text("导出 CSV", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(6.dp))
            Text("把历史周期打成表格，方便自己存一份。", color = q.muted, style = androidx.compose.material3.MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            PillButton("导出 CSV", enabled = joined && !busy, onClick = onExport)
        }
        PrivacyTip(title = "隐私", body = "同步到当前登录成员；另一半默认看不见。家庭码切身份时请留心。")
        PillButton("关掉", filled = false, onClick = onClose)
    }
}

@Composable
private fun SettingsBlock(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (QTheme.colors.isDark) QTheme.colors.sandDeep else QTheme.colors.canvas)
            .padding(14.dp),
        content = { content() },
    )
}

@Composable
private fun StepperRow(label: String, value: Int, enabled: Boolean, onMinus: () -> Unit, onPlus: () -> Unit) {
    val q = QTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
        StepperButton("−", enabled, onMinus)
        Text(
            value.toString(),
            modifier = Modifier.width(40.dp),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        StepperButton("+", enabled, onPlus)
        Text("天", color = q.muted, fontSize = 12.sp)
    }
}

@Composable
private fun StepperButton(text: String, enabled: Boolean, onClick: () -> Unit) {
    val q = QTheme.colors
    Text(
        text,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (q.isDark) q.paper else q.sandDeep)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(top = 2.dp),
        textAlign = TextAlign.Center,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        color = if (enabled) q.ink else q.disabledInk,
    )
}
