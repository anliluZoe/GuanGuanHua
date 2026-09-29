package com.guanguanhua.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.guanguanhua.app.AppViewModel
import com.guanguanhua.app.data.TripStop
import com.guanguanhua.app.trip.TripMath
import com.guanguanhua.app.ui.theme.QTheme
import java.time.LocalDate
import kotlin.math.roundToInt

private val stopKindFill = mapOf(
    "住宿" to Color(0xFF6BA3C4),
    "美食" to Color(0xFFF07A5C),
    "风景" to Color(0xFF4DB6A0),
    "博物馆" to Color(0xFFA78BC4),
    "杂物店" to Color(0xFFD4A84B),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripRouteScreen(viewModel: AppViewModel, tripId: Long, onBack: () -> Unit) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val opened by viewModel.openedTrip.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    LaunchedEffect(tripId) { viewModel.openTrip(tripId) }
    val trip = opened?.takeIf { it.id == tripId } ?: trips.active?.takeIf { it.id == tripId }
    var day by rememberSaveable { mutableStateOf("all") }
    var selectedId by rememberSaveable { mutableStateOf(0L) }
    var deletingId by rememberSaveable { mutableStateOf(0L) }
    val shown = trip?.stops.orEmpty().filter { day == "all" || it.visitedOn == day }
    val mapped = shown.count { it.lat != null && it.lng != null }

    SubpageScaffold(title = trip?.name ?: "这次路线", onBack = onBack) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (trip == null) {
                LoadingHint("正在打开这次路线…")
                return@Column
            }
            Text(
                "${TripMath.formatRange(trip.startedAt, trip.endedAt)} · ${tripStopsLine(trip.stops.size, trip.spentCents)}",
                color = QTheme.colors.muted,
            )
            TripMap(shown, selectedId) { selectedId = it }
            if (shown.isNotEmpty() && mapped == 0) {
                Text(
                    "这些站还没有位置，地图上画不出线。记的时候点附近一家，或打开定位再手写。",
                    color = QTheme.colors.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(shown, key = { _, stop -> stop.id }) { index, stop ->
                    SoftCard(modifier = Modifier.fillMaxWidth(), onClick = { selectedId = stop.id }) {
                        Text("${index + 1}. ${stop.name}", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TripKindLabel(stop.kind)
                            Text(
                                stop.rating?.let { "★$it" } ?: "未评分",
                                color = QTheme.colors.muted,
                                style = MaterialTheme.typography.bodySmall,
                            )
                            stop.amountCents?.let { cents ->
                                Text(cents.toYuan(), color = QTheme.colors.coral, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        if (stop.photos.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            PhotoSlot(
                                model = stop.photos.first().url,
                                modifier = Modifier.fillMaxWidth().height(120.dp),
                                showEmpty = false,
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
    }

    val editing = trip?.stops?.firstOrNull { it.id == selectedId }
    val routeTrip = trip
    if (editing != null && routeTrip != null) {
        StopSheet(
            stop = editing,
            canEdit = routeTrip.active,
            busy = isBusy,
            onSave = { name, kind, rating, amountCents ->
                viewModel.updateTripStop(routeTrip.id, editing.id, name, kind, rating, editing.visitedOn, amountCents)
                selectedId = 0L
            },
            onAddPhotos = { uris ->
                viewModel.uploadTripStopPhotos(
                    routeTrip.id,
                    editing.id,
                    uris.take(TripMath.MAX_PHOTOS - editing.photos.size),
                )
            },
            onRemovePhoto = { photoId -> viewModel.deleteTripStopPhoto(routeTrip.id, editing.id, photoId) },
            onClose = { selectedId = 0L },
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

@Composable
private fun TripMap(stops: List<TripStop>, selectedId: Long, onSelect: (Long) -> Unit) {
    val located = stops.mapNotNull { stop ->
        val lat = stop.lat
        val lng = stop.lng
        if (lat == null || lng == null) null else stop
    }
    val q = QTheme.colors
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(q.mintSoft),
    ) {
        val density = LocalDensity.current
        val context = LocalContext.current
        val widthPx = with(density) { maxWidth.roundToPx() }
        val heightPx = with(density) { maxHeight.roundToPx() }
        val coords = located.map { it.lat as Double to it.lng as Double }
        val frame = remember(coords, widthPx, heightPx) {
            if (coords.isEmpty()) null else TripMath.mapFrame(coords, widthPx, heightPx)
        }
        val tiles = remember(frame) { frame?.let { TripMath.mapTiles(it) }.orEmpty() }
        val tileDp = with(density) { (frame?.tileSize ?: 256).toDp() }
        val hitRadius = with(density) { 14.dp.roundToPx() }
        val minMarkerDist = with(density) { 20.dp.toPx() }
        val pixels = remember(located, frame, minMarkerDist) {
            val shown = frame ?: return@remember emptyList()
            TripMath.spreadOverlapping(
                located.map { TripMath.mapPixel(it.lat as Double, it.lng as Double, shown) },
                minMarkerDist,
            )
        }
        if (frame == null) return@BoxWithConstraints
        tiles.forEach { tile ->
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(tile.url)
                    .addHeader("User-Agent", "GuanGuanHua/1.0")
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset { IntOffset(tile.offsetX.roundToInt(), tile.offsetY.roundToInt()) }
                    .size(tileDp),
            )
        }
        Canvas(Modifier.fillMaxSize()) {
            located.zipWithNext().forEach { (from, to) ->
                val start = TripMath.mapPixel(from.lat as Double, from.lng as Double, frame)
                val end = TripMath.mapPixel(to.lat as Double, to.lng as Double, frame)
                drawLine(
                    color = q.sky,
                    start = Offset(start.first, start.second),
                    end = Offset(end.first, end.second),
                    strokeWidth = 4f,
                    cap = StrokeCap.Round,
                )
            }
        }
        located.forEachIndexed { index, stop ->
            val pixel = pixels[index]
            val selected = stop.id == selectedId
            Box(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(pixel.first.roundToInt() - hitRadius, pixel.second.roundToInt() - hitRadius)
                    }
                    .size(28.dp)
                    .clickable { onSelect(stop.id) },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (selected) 18.dp else 14.dp)
                        .border(1.dp, if (selected) Color.White else Color.Transparent, CircleShape)
                        .clip(CircleShape)
                        .background(stopKindFill[stop.kind] ?: q.sky),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        "${index + 1}",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StopSheet(
    stop: TripStop,
    canEdit: Boolean,
    busy: Boolean,
    onSave: (String, String, Int?, Long?) -> Unit,
    onAddPhotos: (List<Uri>) -> Unit,
    onRemovePhoto: (Long) -> Unit,
    onClose: () -> Unit,
) {
    var name by rememberSaveable(stop.id) { mutableStateOf(stop.name) }
    var kind by rememberSaveable(stop.id) { mutableStateOf(stop.kind) }
    var rating by rememberSaveable(stop.id) { mutableStateOf(stop.rating ?: 0) }
    var amountText by rememberSaveable(stop.id) {
        mutableStateOf(stop.amountCents?.toYuan()?.removePrefix("¥") ?: "")
    }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(TripMath.MAX_PHOTOS),
    ) { uris ->
        if (uris.isNotEmpty()) onAddPhotos(uris)
    }
    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = QTheme.colors.paper,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(if (canEdit) "这一站" else stop.name, style = MaterialTheme.typography.titleLarge)
            if (canEdit) {
                SoftField(value = name, onValueChange = { name = it }, label = "名字")
                TripKindChips(kind) { kind = it }
                TripStars(rating.takeIf { it > 0 }) { rating = it ?: 0 }
                TripAmountField(amountText) { amountText = it }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TripKindLabel(stop.kind)
                    Text(stop.rating?.let { "★$it" } ?: "未评分", color = QTheme.colors.muted)
                    stop.amountCents?.let { Text(it.toYuan(), color = QTheme.colors.coral) }
                }
            }
            if (stop.photos.isNotEmpty() || canEdit) {
                TripPhotoStrip(
                    photos = stop.photos,
                    canEdit = canEdit && !busy,
                    onAdd = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemovePhoto = { photo -> onRemovePhoto(photo.id) },
                )
            }
            if (canEdit) {
                PillButton(
                    "保存",
                    enabled = !busy && name.trim().isNotBlank() && TripMath.knownKind(kind) &&
                        (amountText.isBlank() || amountText.yuanToCentsOrNull() != null),
                    onClick = {
                        onSave(name, kind, TripMath.ratingOrNull(rating.takeIf { it > 0 }), amountText.yuanToCentsOrNull())
                    },
                )
            }
            PillButton(if (canEdit) "先不了" else "好", filled = false, onClick = onClose)
        }
    }
}
