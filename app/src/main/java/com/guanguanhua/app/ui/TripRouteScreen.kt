package com.guanguanhua.app.ui

import android.annotation.SuppressLint
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.gson.Gson
import com.guanguanhua.app.AppViewModel
import com.guanguanhua.app.data.TripStop
import com.guanguanhua.app.trip.TripMath
import com.guanguanhua.app.ui.theme.QTheme
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripRouteScreen(viewModel: AppViewModel, tripId: Long, onBack: () -> Unit) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val opened by viewModel.openedTrip.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    LaunchedEffect(tripId) { viewModel.openTrip(tripId) }
    val trip = opened?.takeIf { it.id == tripId } ?: trips.active?.takeIf { it.id == tripId }
    var day by rememberSaveable { mutableStateOf("all") }
    var editingId by rememberSaveable { mutableStateOf(0L) }
    var deletingId by rememberSaveable { mutableStateOf(0L) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    val shown = trip?.stops.orEmpty().filter { day == "all" || it.visitedOn == day }

    LaunchedEffect(shown, webView) {
        val view = webView ?: return@LaunchedEffect
        val json = Gson().toJson(shown.map { mapOf("name" to it.name, "kind" to it.kind, "lat" to it.lat, "lng" to it.lng) })
        view.evaluateJavascript("window.renderStops && window.renderStops($json)", null)
    }

    SubpageScaffold(title = trip?.name ?: "这次路线", onBack = onBack) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (trip == null) {
                LoadingHint("正在打开这次路线…")
                return@Column
            }
            Text(
                "${TripMath.formatRange(trip.startedAt, trip.endedAt)} · ${trip.stops.size} 站",
                color = QTheme.colors.muted,
            )
            TripMap(onReady = { webView = it })
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ChoiceChip("全部", day == "all", onClick = { day = "all" })
                trip.stops.map { it.visitedOn }.distinct().forEach { iso ->
                    val n = runCatching { TripMath.dayNumber(trip.startedAt, LocalDate.parse(iso)) }.getOrDefault(1)
                    ChoiceChip("第${n}天", day == iso, onClick = { day = iso })
                }
            }
            shown.forEachIndexed { index, stop ->
                SoftCard(modifier = Modifier.fillMaxWidth(), onClick = { if (trip.active) editingId = stop.id }) {
                    Text("${index + 1}. ${stop.name}", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TripKindLabel(stop.kind)
                        Text(
                            stop.rating?.let { "★$it" } ?: "未评分",
                            color = QTheme.colors.muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    if (trip.active) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text("上移", color = QTheme.colors.sky, modifier = Modifier.clickable { viewModel.moveTripStop(stop.id, -1) })
                            Text("下移", color = QTheme.colors.sky, modifier = Modifier.clickable { viewModel.moveTripStop(stop.id, 1) })
                            Text("删除", color = QTheme.colors.coral, modifier = Modifier.clickable { deletingId = stop.id })
                        }
                    }
                }
            }
        }
    }

    val editing = trip?.stops?.firstOrNull { it.id == editingId }
    val routeTrip = trip
    if (editing != null && routeTrip != null) {
        StopEditSheet(
            stop = editing,
            busy = isBusy,
            onSave = { name, kind, rating ->
                viewModel.updateTripStop(routeTrip.id, editing.id, name, kind, rating, editing.visitedOn)
                editingId = 0L
            },
            onClose = { editingId = 0L },
        )
    }
    val deleting = routeTrip?.stops?.firstOrNull { it.id == deletingId }
    if (deleting != null && routeTrip != null) {
        AlertDialog(
            onDismissRequest = { deletingId = 0L },
            title = { Text("删掉「${deleting.name}」？") },
            text = { Text("这条从路线上拿掉。") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteTripStop(routeTrip.id, deleting.id)
                    deletingId = 0L
                }) { Text("删除", color = QTheme.colors.coral) }
            },
            dismissButton = { TextButton(onClick = { deletingId = 0L }) { Text("先不了") } },
            containerColor = MaterialTheme.colorScheme.surface,
            shape = MaterialTheme.shapes.large,
        )
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun TripMap(onReady: (WebView) -> Unit) {
    AndroidView(
        factory = { context ->
            WebView(context).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.let(onReady)
                    }
                }
                loadUrl("file:///android_asset/trip_map.html")
            }
        },
        modifier = Modifier.fillMaxWidth().height(240.dp),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopEditSheet(
    stop: TripStop,
    busy: Boolean,
    onSave: (String, String, Int?) -> Unit,
    onClose: () -> Unit,
) {
    var name by rememberSaveable(stop.id) { mutableStateOf(stop.name) }
    var kind by rememberSaveable(stop.id) { mutableStateOf(stop.kind) }
    var rating by rememberSaveable(stop.id) { mutableStateOf(stop.rating ?: 0) }
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = QTheme.colors.paper,
    ) {
        Column(modifier = Modifier.padding(horizontal = 20.dp).padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("改这一站", style = MaterialTheme.typography.titleLarge)
            SoftField(value = name, onValueChange = { name = it }, label = "名字")
            TripKindChips(kind) { kind = it }
            TripStars(rating.takeIf { it > 0 }) { rating = it ?: 0 }
            PillButton("保存", enabled = !busy && name.trim().isNotBlank() && TripMath.knownKind(kind), onClick = {
                onSave(name, kind, TripMath.ratingOrNull(rating.takeIf { it > 0 }))
            })
            PillButton("先不了", filled = false, onClick = onClose)
        }
    }
}
