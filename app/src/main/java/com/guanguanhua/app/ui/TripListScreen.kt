package com.guanguanhua.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanguanhua.app.AppViewModel
import com.guanguanhua.app.data.TripSummary
import com.guanguanhua.app.trip.TripMath
import com.guanguanhua.app.ui.theme.QTheme
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@Composable
fun TripListScreen(
    viewModel: AppViewModel,
    onBack: () -> Unit,
    onStart: () -> Unit,
    onRecord: () -> Unit,
    onOpenRoute: (Long) -> Unit,
    askEnd: Boolean = false,
    onAskEndConsumed: () -> Unit = {},
) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    var confirmEnd by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(askEnd) {
        if (askEnd && trips.active != null) confirmEnd = true
        if (askEnd) onAskEndConsumed()
    }
    val active = trips.active
    SubpageScaffold(title = "足迹", onBack = onBack) { inner ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp),
            contentPadding = PaddingValues(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!session.joined) {
                item { EmptyHint("还没连上账本", "先加入家庭，旅程会跟两个人一起走。") }
            } else if (active != null) {
                item {
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Text("旅行中 · ${active.name}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "${TripMath.formatRange(active.startedAt, null)} · ${tripStopsLine(active.stops.size, active.spentCents)}",
                            color = QTheme.colors.muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Spacer(Modifier.height(14.dp))
                        PillButton("记一站", onClick = onRecord)
                        Spacer(Modifier.height(10.dp))
                        PillButton("看这次路线", filled = false, onClick = { onOpenRoute(active.id) })
                        Spacer(Modifier.height(10.dp))
                        PillButton("结束旅程", filled = false, enabled = !isBusy, onClick = { confirmEnd = true })
                    }
                }
            } else {
                item {
                    SoftCard(modifier = Modifier.fillMaxWidth()) {
                        Text("还没有进行中的旅程", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(6.dp))
                        Text("开始之后会一直记，直到有人点结束。", color = QTheme.colors.muted, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(14.dp))
                        PillButton("开始旅程", onClick = onStart)
                    }
                }
            }
            if (trips.trips.any { !it.active }) {
                item { Text("以前的旅程", style = MaterialTheme.typography.titleMedium) }
                items(trips.trips.filter { !it.active }, key = { it.id }) { trip ->
                    PastTripCard(trip) { onOpenRoute(trip.id) }
                }
            }
        }
    }
    if (confirmEnd && active != null) {
        val todayEmpty = active.stops.none { it.visitedOn == LocalDate.now().toString() }
        AlertDialog(
            onDismissRequest = { if (!isBusy) confirmEnd = false },
            title = { Text("结束这段旅程？") },
            text = {
                Text(
                    buildString {
                        append("「${active.name}」会停在今天。两个人都不能再往这次路线上记。确定吗？")
                        if (todayEmpty) append(" 今天还没记站，结束后就不能补进这次了。")
                    },
                )
            },
            confirmButton = {
                TextButton(
                    enabled = !isBusy,
                    onClick = {
                        confirmEnd = false
                        viewModel.endTrip()
                    },
                ) { Text("结束旅程", color = QTheme.colors.coral) }
            },
            dismissButton = {
                TextButton(enabled = !isBusy, onClick = { confirmEnd = false }) { Text("还在继续") }
            },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }
}

@Composable
private fun PastTripCard(trip: TripSummary, onClick: () -> Unit) {
    SoftCard(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Text(trip.name, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "${TripMath.formatRange(trip.startedAt, trip.endedAt)} · ${tripStopsLine(trip.stopCount, trip.spentCents)}",
            color = QTheme.colors.muted,
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StartTripScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val askNotifications = rememberAskNotifications()
    var name by rememberSaveable { mutableStateOf("") }
    var planned by rememberSaveable { mutableStateOf("") }
    var picking by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(trips.active) {
        if (trips.active != null) onBack()
    }
    SubpageScaffold(title = "开始旅程", onBack = onBack) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SoftField(value = name, onValueChange = { name = it }, label = "旅程名字", supportingText = "比如 桂林阳朔")
            Text(
                if (planned.isBlank()) "预计结束（选填，到了会提醒你）" else "预计结束 ${planned}",
                color = QTheme.colors.sky,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { picking = true }
                    .padding(vertical = 8.dp),
            )
            if (planned.isNotBlank()) {
                TextButton(onClick = { planned = "" }) { Text("清掉预计日", color = QTheme.colors.sky) }
            }
            PillButton(
                "开始旅程",
                enabled = !isBusy && name.trim().isNotBlank(),
                onClick = {
                    askNotifications()
                    viewModel.startTrip(name, planned.ifBlank { null }, onSuccess = onBack)
                },
            )
            PillButton("先不了", filled = false, enabled = !isBusy, onClick = onBack)
        }
    }
    if (picking) {
        val state = rememberDatePickerState()
        DatePickerDialog(
            onDismissRequest = { picking = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        planned = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    picking = false
                }) { Text("好") }
            },
            dismissButton = { TextButton(onClick = { picking = false }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }
}
