package com.guanguanhua.app.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.FullscreenExit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.guanguanhua.app.AppViewModel
import com.guanguanhua.app.UserProfile
import com.guanguanhua.app.data.TripStop
import com.guanguanhua.app.trip.MapFrame
import com.guanguanhua.app.trip.TripMath
import com.guanguanhua.app.ui.theme.QTheme
import java.time.LocalDate
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

private val stopKindFill = mapOf(
    "住宿" to Color(0xFF6BA3C4),
    "美食" to Color(0xFFF07A5C),
    "风景" to Color(0xFF4DB6A0),
    "博物馆" to Color(0xFFA78BC4),
    "杂物店" to Color(0xFFD4A84B),
)

@Composable
private fun TripAxisNode(
    leftLabel: String,
    lineAbove: Boolean,
    lineBelow: Boolean,
    onClick: (() -> Unit)? = null,
    node: @Composable BoxScope.() -> Unit,
    content: @Composable () -> Unit,
) {
    val q = QTheme.colors
    val axis = q.sky.copy(alpha = 0.5f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .fillMaxHeight()
                .padding(top = 10.dp, end = 6.dp),
            contentAlignment = Alignment.TopEnd,
        ) {
            if (leftLabel.isNotEmpty()) {
                Text(
                    leftLabel,
                    color = q.muted,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
        Box(
            modifier = Modifier
                .width(22.dp)
                .fillMaxHeight(),
            contentAlignment = Alignment.TopCenter,
        ) {
            if (lineAbove) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .width(2.dp)
                        .height(18.dp)
                        .background(axis),
                )
            }
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .size(20.dp),
                contentAlignment = Alignment.Center,
                content = node,
            )
            if (lineBelow) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(top = 28.dp)
                        .width(2.dp)
                        .fillMaxHeight()
                        .background(axis),
                )
            }
        }
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(top = 6.dp, bottom = 14.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .width(10.dp)
                    .height(2.dp)
                    .background(axis),
            )
            Box(modifier = Modifier.weight(1f)) {
                content()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripRouteScreen(viewModel: AppViewModel, tripId: Long, onBack: () -> Unit) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val opened by viewModel.openedTrip.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    LaunchedEffect(tripId) { viewModel.openTrip(tripId) }
    val trip = opened?.takeIf { it.id == tripId } ?: trips.active?.takeIf { it.id == tripId }
    var day by rememberSaveable { mutableStateOf("all") }
    var selectedId by rememberSaveable { mutableStateOf(0L) }
    var playStopId by rememberSaveable { mutableStateOf(0L) }
    var deletingId by rememberSaveable { mutableStateOf(0L) }
    var renaming by rememberSaveable { mutableStateOf(false) }
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "${TripMath.formatRange(trip.startedAt, trip.endedAt)} · ${tripStopsLine(trip.stops.size, trip.spentCents)}",
                    color = QTheme.colors.muted,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "改名字",
                    color = QTheme.colors.sky,
                    modifier = Modifier
                        .clickable(enabled = !isBusy) {
                            renaming = true
                        }
                        .padding(start = 12.dp, top = 4.dp, bottom = 4.dp),
                )
            }
            if (!trip.active) {
                Text(
                    "点一站可以改店名、评分、花费和照片。",
                    color = QTheme.colors.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            var mapExpanded by rememberSaveable { mutableStateOf(false) }
            TripMap(
                stops = shown,
                selectedId = selectedId,
                expanded = mapExpanded,
                onToggleExpand = { mapExpanded = !mapExpanded },
                modifier = if (mapExpanded) Modifier.fillMaxWidth().weight(1f) else Modifier.fillMaxWidth().height(260.dp),
                onSelect = {
                    selectedId = it
                    playStopId = 0L
                },
                onPlayStop = { id ->
                    playStopId = id
                    if (id != 0L) selectedId = 0L
                },
                profile = profile,
            )
            if (shown.isNotEmpty() && mapped == 0 && !mapExpanded) {
                Text(
                    "这些站还没有位置，地图上画不出线。记的时候点附近一家，或打开定位再手写。",
                    color = QTheme.colors.muted,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!mapExpanded) {
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
                contentPadding = PaddingValues(bottom = 24.dp),
            ) {
                shown.forEachIndexed { index, stop ->
                    val dayStart = index == 0 || stop.visitedOn != shown[index - 1].visitedOn
                    val lastStop = index == shown.lastIndex
                    if (dayStart) {
                        item(key = "day-${stop.visitedOn}") {
                            val q = QTheme.colors
                            val visited = runCatching { LocalDate.parse(stop.visitedOn) }.getOrNull()
                            val dayNo = visited?.let { TripMath.dayNumber(trip.startedAt, it) }
                            TripAxisNode(
                                leftLabel = visited?.let { "${it.monthValue}/${it.dayOfMonth}" }.orEmpty(),
                                lineAbove = index > 0,
                                lineBelow = true,
                                node = {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .border(2.dp, q.sky, CircleShape)
                                            .background(q.paper, CircleShape),
                                    )
                                },
                            ) {
                                Text(
                                    dayNo?.let { "第${it}天" } ?: "这一天",
                                    color = q.sky,
                                    fontWeight = FontWeight.SemiBold,
                                    style = MaterialTheme.typography.titleSmall,
                                )
                            }
                        }
                    }
                    item(key = stop.id) {
                        val q = QTheme.colors
                        val selected = stop.id == selectedId || stop.id == playStopId
                        val fill = stopKindFill[stop.kind] ?: q.sky
                        TripAxisNode(
                            leftLabel = "",
                            lineAbove = true,
                            lineBelow = !lastStop,
                            onClick = { selectedId = stop.id },
                            node = {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .border(if (selected) 2.dp else 0.dp, Color.White, CircleShape)
                                        .clip(CircleShape)
                                        .background(fill),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        "${index + 1}",
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 9.sp,
                                        textAlign = TextAlign.Center,
                                        style = TextStyle(
                                            lineHeightStyle = LineHeightStyle(
                                                alignment = LineHeightStyle.Alignment.Center,
                                                trim = LineHeightStyle.Trim.Both,
                                            ),
                                        ),
                                    )
                                }
                            },
                        ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (selected) q.skySoft else q.paper)
                                .border(1.dp, if (selected) q.sky.copy(alpha = 0.45f) else q.line, RoundedCornerShape(16.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(stop.name, style = MaterialTheme.typography.titleMedium)
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    TripKindLabel(stop.kind)
                                    Text(
                                        stop.rating?.let { "★$it" } ?: "未评分",
                                        color = q.muted,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                    stop.amountCents?.let { cents ->
                                        Text(cents.toYuan(), color = q.coral, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                                stop.note?.takeIf { it.isNotBlank() }?.let { note ->
                                    Text(
                                        note,
                                        color = q.muted,
                                        style = MaterialTheme.typography.bodySmall,
                                        maxLines = 2,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("上移", color = q.sky, modifier = Modifier.clickable { viewModel.moveTripStop(stop.id, -1) })
                                    Text("下移", color = q.sky, modifier = Modifier.clickable { viewModel.moveTripStop(stop.id, 1) })
                                    Text("删除", color = q.coral, modifier = Modifier.clickable { deletingId = stop.id })
                                }
                            }
                            if (stop.photos.isNotEmpty()) {
                                PhotoSlot(
                                    model = stop.photos.first().url,
                                    modifier = Modifier.padding(start = 10.dp).size(52.dp),
                                    showEmpty = false,
                                )
                            }
                        }
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
            canEdit = true,
            busy = isBusy,
            onSave = { name, kind, rating, amountCents, note ->
                viewModel.updateTripStop(routeTrip.id, editing.id, name, kind, rating, editing.visitedOn, amountCents, note)
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
    val naming = trip
    if (renaming && naming != null) {
        RenameTripDialog(
            name = naming.name,
            busy = isBusy,
            onDismiss = { renaming = false },
            onSave = { next ->
                viewModel.renameTrip(naming.id, next) { renaming = false }
            },
        )
    }
}

@Composable
private fun TripMap(
    stops: List<TripStop>,
    selectedId: Long,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier,
    onSelect: (Long) -> Unit,
    onPlayStop: (Long) -> Unit,
    profile: UserProfile,
) {
    val located = stops.mapNotNull { stop ->
        val lat = stop.lat
        val lng = stop.lng
        if (lat == null || lng == null) null else stop
    }
    val q = QTheme.colors
    BoxWithConstraints(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(q.mintSoft),
    ) {
        val density = LocalDensity.current
        val context = LocalContext.current
        val widthPx = with(density) { maxWidth.roundToPx() }
        val heightPx = with(density) { maxHeight.roundToPx() }
        val coords = located.map { it.lat as Double to it.lng as Double }
        val fitted = remember(coords, widthPx, heightPx) {
            if (coords.isEmpty()) null else TripMath.mapFrame(coords, widthPx, heightPx)
        }
        val viewState = remember { mutableStateOf<MapFrame?>(null) }
        var pinchScale by remember { mutableFloatStateOf(1f) }
        var pinchOrigin by remember { mutableStateOf(Offset.Zero) }
        LaunchedEffect(fitted) {
            viewState.value = fitted
            pinchScale = 1f
        }
        val frame = viewState.value
        val tiles = remember(frame) { frame?.let { TripMath.mapTiles(it) }.orEmpty() }
        val tileDp = with(density) { (frame?.tileSize ?: 256).toDp() }
        val hitRadius = with(density) { 14.dp.roundToPx() }
        val minMarkerDist = with(density) { 20.dp.toPx() }
        val textMeasurer = rememberTextMeasurer()
        val markerStyle = remember {
            TextStyle(
                color = Color.White,
                fontSize = 9.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 9.sp,
                textAlign = TextAlign.Center,
                lineHeightStyle = LineHeightStyle(
                    alignment = LineHeightStyle.Alignment.Center,
                    trim = LineHeightStyle.Trim.Both,
                ),
            )
        }
        val labels = remember(located.size, markerStyle, textMeasurer) {
            List(located.size) { index ->
                textMeasurer.measure("${index + 1}", markerStyle)
            }
        }
        val pixels = remember(located, frame, minMarkerDist) {
            val shown = frame ?: return@remember emptyList()
            TripMath.spreadOverlapping(
                located.map { TripMath.mapPixel(it.lat as Double, it.lng as Double, shown) },
                minMarkerDist,
            )
        }
        var playing by remember { mutableStateOf(false) }
        val playAnim = remember { Animatable(0f) }
        LaunchedEffect(located.map { it.id }) {
            playing = false
            playAnim.snapTo(0f)
            onPlayStop(0L)
        }
        LaunchedEffect(playing) {
            if (!playing) return@LaunchedEffect
            if (located.size < 2) {
                playing = false
                return@LaunchedEffect
            }
            playAnim.snapTo(0f)
            playAnim.animateTo(1f, tween(TripMath.playMs(located.size), easing = LinearEasing))
            playing = false
        }
        val play = playAnim.value
        val showingPlay = play > 0f
        if (frame == null) return@BoxWithConstraints
        val pathPts = remember(located, frame) {
            located.map { TripMath.mapPixel(it.lat as Double, it.lng as Double, frame) }
        }
        val head = if (showingPlay) TripMath.routePlayhead(pathPts, play) else null
        LaunchedEffect(head?.reached, playing) {
            if (playing && head != null) {
                located.getOrNull(head.reached)?.let { onPlayStop(it.id) }
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = pinchScale
                    scaleY = pinchScale
                    if (size.width > 0f && size.height > 0f) {
                        transformOrigin = TransformOrigin(
                            pinchOrigin.x / size.width,
                            pinchOrigin.y / size.height,
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTransformGestures { centroid, pan, zoom, _ ->
                        val current = viewState.value ?: return@detectTransformGestures
                        if (zoom != 1f) {
                            pinchOrigin = centroid
                            pinchScale = (pinchScale * zoom).coerceIn(0.5f, 2f)
                            when {
                                pinchScale >= 1.25f && current.zoom < TripMath.MAP_MAX_ZOOM -> {
                                    viewState.value = TripMath.zoomFrame(
                                        current, current.zoom + 1, centroid.x, centroid.y,
                                    )
                                    pinchScale = 1f
                                }
                                pinchScale <= 0.8f && current.zoom > TripMath.MAP_MIN_ZOOM -> {
                                    viewState.value = TripMath.zoomFrame(
                                        current, current.zoom - 1, centroid.x, centroid.y,
                                    )
                                    pinchScale = 1f
                                }
                            }
                        }
                        if (pan.x != 0f || pan.y != 0f) {
                            viewState.value = TripMath.panFrame(viewState.value ?: current, pan.x, pan.y)
                        }
                    }
                }
                .pointerInput(pixels, located) {
                    detectTapGestures(
                        onTap = { tap ->
                            val hit = located.indices.minByOrNull { index ->
                                hypot(
                                    (pixels[index].first - tap.x).toDouble(),
                                    (pixels[index].second - tap.y).toDouble(),
                                )
                            } ?: return@detectTapGestures
                            val dist = hypot(
                                (pixels[hit].first - tap.x).toDouble(),
                                (pixels[hit].second - tap.y).toDouble(),
                            )
                            if (dist <= hitRadius * 1.4) onSelect(located[hit].id)
                        },
                        onDoubleTap = { tap ->
                            val current = viewState.value ?: return@detectTapGestures
                            pinchScale = 1f
                            viewState.value = TripMath.zoomFrame(
                                current, current.zoom + 1, tap.x, tap.y,
                            )
                        },
                    )
                },
        ) {
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
                val progresses = TripMath.routeProgress(located.map { it.visitedOn })
                val dim = if (showingPlay) 0.35f else 1f
                val early = lerp(q.sky, Color.White, 0.42f).copy(alpha = 0.42f * dim)
                val late = lerp(q.sky, Color(0xFF0A2740), 0.62f).copy(alpha = 0.95f * dim)
                pathPts.zipWithNext().forEachIndexed { index, (start, end) ->
                    val fromPx = Offset(start.first, start.second)
                    val toPx = Offset(end.first, end.second)
                    drawLine(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                lerp(early, late, progresses[index]),
                                lerp(early, late, progresses[index + 1]),
                            ),
                            start = fromPx,
                            end = toPx,
                        ),
                        start = fromPx,
                        end = toPx,
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 8f)),
                    )
                }
                if (head != null) {
                    val ink = q.sky.copy(alpha = 0.92f)
                    val walked = pathPts.take(head.reached + 1) + (head.x to head.y)
                    walked.zipWithNext().forEach { (from, to) ->
                        drawLine(
                            color = ink,
                            start = Offset(from.first, from.second),
                            end = Offset(to.first, to.second),
                            strokeWidth = 4.5f,
                            cap = StrokeCap.Round,
                        )
                    }
                }
                located.forEachIndexed { index, stop ->
                    val pixel = pixels[index]
                    val center = Offset(pixel.first, pixel.second)
                    val selected = stop.id == selectedId || index == head?.reached
                    val radius = if (selected) 9.dp.toPx() else 7.dp.toPx()
                    drawCircle(stopKindFill[stop.kind] ?: q.sky, radius, center)
                    if (selected) {
                        drawCircle(Color.White, radius, center, style = Stroke(width = 1.5.dp.toPx()))
                    }
                    val label = labels[index]
                    drawText(
                        textLayoutResult = label,
                        topLeft = Offset(
                            center.x - label.size.width / 2f,
                            center.y - label.size.height / 2f,
                        ),
                    )
                }
            }
            if (head != null) {
                val avatarPx = with(density) { 30.dp.toPx() }
                val sep = with(density) { 11.dp.toPx() }
                val rad = Math.toRadians(head.angleDeg.toDouble())
                val nx = (-sin(rad)).toFloat()
                val ny = cos(rad).toFloat()
                val together = !profile.partnerName.isNullOrBlank() ||
                    !profile.partnerAvatarUrl.isNullOrBlank() ||
                    !profile.partnerAvatarPreset.isNullOrBlank()
                val meX = if (together) head.x + nx * sep else head.x
                val meY = if (together) head.y + ny * sep else head.y
                if (together) {
                    MemberAvatar(
                        name = profile.partnerName.orEmpty(),
                        presetId = profile.partnerAvatarPreset,
                        photoUrl = profile.partnerAvatarUrl,
                        fallbackPreset = AvatarIds.DOG,
                        size = 30.dp,
                        modifier = Modifier
                            .zIndex(9f)
                            .offset {
                                IntOffset(
                                    (head.x - nx * sep - avatarPx / 2f).roundToInt(),
                                    (head.y - ny * sep - avatarPx / 2f).roundToInt(),
                                )
                            },
                    )
                }
                MemberAvatar(
                    name = profile.name.ifBlank { "我" },
                    presetId = profile.avatarPreset,
                    photoUrl = profile.avatarUrl,
                    fallbackPreset = AvatarIds.CAT,
                    size = 30.dp,
                    modifier = Modifier
                        .zIndex(10f)
                        .offset {
                            IntOffset(
                                (meX - avatarPx / 2f).roundToInt(),
                                (meY - avatarPx / 2f).roundToInt(),
                            )
                        },
                )
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(10.dp)
                .width(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(q.paper)
                .border(1.dp, q.line, RoundedCornerShape(10.dp)),
        ) {
            listOf("+" to 1, "−" to -1).forEachIndexed { index, (label, delta) ->
                val enabled = if (delta > 0) frame.zoom < TripMath.MAP_MAX_ZOOM else frame.zoom > TripMath.MAP_MIN_ZOOM
                if (index > 0) {
                    Box(Modifier.width(36.dp).height(1.dp).background(q.line))
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable(enabled = enabled) {
                            pinchScale = 1f
                            viewState.value = TripMath.zoomFrame(
                                frame,
                                frame.zoom + delta,
                                frame.widthPx / 2f,
                                frame.heightPx / 2f,
                            )
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        color = if (enabled) q.ink else q.muted,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }
            }
            Box(Modifier.width(36.dp).height(1.dp).background(q.line))
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clickable(onClick = onToggleExpand),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = if (expanded) Icons.Outlined.FullscreenExit else Icons.Outlined.Fullscreen,
                    contentDescription = if (expanded) "收起地图" else "铺满屏幕",
                    tint = q.ink,
                    modifier = Modifier.size(20.dp),
                )
            }
            if (located.size >= 2) {
                Box(Modifier.width(36.dp).height(1.dp).background(q.line))
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { playing = !playing },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = if (playing) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (playing) "停止播放" else "播放足迹",
                        tint = q.ink,
                        modifier = Modifier.size(22.dp),
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
    onSave: (String, String, Int?, Long?, String?) -> Unit,
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
    var noteText by rememberSaveable(stop.id) { mutableStateOf(stop.note.orEmpty()) }
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
                TripNoteField(noteText) { noteText = it }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    TripKindLabel(stop.kind)
                    Text(stop.rating?.let { "★$it" } ?: "未评分", color = QTheme.colors.muted)
                    stop.amountCents?.let { Text(it.toYuan(), color = QTheme.colors.coral) }
                }
                stop.note?.takeIf { it.isNotBlank() }?.let { note ->
                    Text(note, color = QTheme.colors.ink, style = MaterialTheme.typography.bodyMedium)
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
                        onSave(
                            name,
                            kind,
                            TripMath.ratingOrNull(rating.takeIf { it > 0 }),
                            amountText.yuanToCentsOrNull(),
                            noteText,
                        )
                    },
                )
            }
            PillButton(if (canEdit) "先不了" else "好", filled = false, onClick = onClose)
        }
    }
}
