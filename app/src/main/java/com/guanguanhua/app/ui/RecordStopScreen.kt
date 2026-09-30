package com.guanguanhua.app.ui

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.guanguanhua.app.AppViewModel
import com.guanguanhua.app.data.NearbyPlace
import com.guanguanhua.app.trip.NearbyPlaces
import com.guanguanhua.app.trip.TripMath
import com.guanguanhua.app.ui.theme.QTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordStopScreen(viewModel: AppViewModel, onBack: () -> Unit, onOpenRoute: (Long) -> Unit) {
    val trips by viewModel.trips.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()
    val active = trips.active
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var name by rememberSaveable { mutableStateOf("") }
    var kind by rememberSaveable { mutableStateOf("") }
    var rating by rememberSaveable { mutableStateOf(0) }
    var amountText by rememberSaveable { mutableStateOf("") }
    var noteText by rememberSaveable { mutableStateOf("") }
    var photoUris by rememberSaveable { mutableStateOf(listOf<String>()) }
    var visitedOn by rememberSaveable { mutableStateOf(LocalDate.now().toString()) }
    var lat by rememberSaveable { mutableStateOf<String?>(null) }
    var lng by rememberSaveable { mutableStateOf<String?>(null) }
    var nearby by remember { mutableStateOf<List<NearbyPlace>>(emptyList()) }
    var nearbyHint by rememberSaveable { mutableStateOf("正在找附近…") }
    var remoteHits by remember { mutableStateOf<List<NearbyPlace>>(emptyList()) }
    var searchHint by remember { mutableStateOf<String?>(null) }
    var pickingDate by rememberSaveable { mutableStateOf(false) }
    var picked by rememberSaveable { mutableStateOf(false) }
    fun findNearby() {
        nearbyHint = "正在找附近…"
        scope.launch { loadNearby(context) { places, hint -> nearby = places; nearbyHint = hint } }
    }
    val askLocation = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        findNearby()
    }
    val photoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickMultipleVisualMedia(TripMath.MAX_PHOTOS),
    ) { uris ->
        photoUris = (photoUris + uris.map { it.toString() }).distinct().take(TripMath.MAX_PHOTOS)
    }

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) {
            findNearby()
        } else {
            nearbyHint = "打不开定位，也可以先搜店名"
            askLocation.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
            )
        }
    }

    LaunchedEffect(name, picked) {
        val q = name.trim()
        if (picked || q.length < 2) {
            remoteHits = emptyList()
            searchHint = null
            return@LaunchedEffect
        }
        delay(400)
        searchHint = "正在搜「$q」…"
        val origin = NearbyPlaces.lastLocation(context)
        val result = runCatching { NearbyPlaces.searchByName(q, origin?.latitude, origin?.longitude) }
        remoteHits = result.getOrDefault(emptyList())
        searchHint = when {
            result.isFailure -> "这会儿搜不到，稍后再试，或用这个名字"
            remoteHits.isEmpty() -> "没搜到「$q」，换个词，或用这个名字"
            else -> "搜到这些，点一家"
        }
    }

    SubpageScaffold(title = "记一站", onBack = onBack) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (active == null) {
                Text("这段旅程已经结束了", color = QTheme.colors.secondary)
                PillButton("看这次路线", filled = false, onClick = {
                    trips.trips.firstOrNull()?.let { onOpenRoute(it.id) } ?: onBack()
                })
                return@Column
            }
            val day = runCatching { TripMath.dayNumber(active.startedAt, LocalDate.parse(visitedOn)) }.getOrDefault(1)
            Text("${active.name} · 第 $day 天", color = QTheme.colors.muted, style = MaterialTheme.typography.bodyMedium)
            Text("今天 ${active.stops.count { it.visitedOn == LocalDate.now().toString() }} 站", color = QTheme.colors.secondary)
            if (!picked) {
                val query = name.trim()
                val shownPlaces = if (query.length < 2) nearby else {
                    val local = nearby.filter { it.name.contains(query, ignoreCase = true) }
                    (remoteHits + local).distinctBy { it.name to it.lat.toBits() }.sortedBy { it.meters }
                }
                SoftField(value = name, onValueChange = { name = it }, label = "搜店名或地名")
                Text(
                    searchHint ?: nearbyHint,
                    color = QTheme.colors.muted,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.clickable {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED ||
                            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                            PackageManager.PERMISSION_GRANTED
                        if (granted) findNearby() else askLocation.launch(
                            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                        )
                    },
                )
                shownPlaces.forEach { place ->
                    SoftCard(modifier = Modifier.fillMaxWidth(), onClick = {
                        name = place.name
                        kind = place.kind.orEmpty()
                        lat = place.lat.toString()
                        lng = place.lng.toString()
                        picked = true
                    }) {
                        Text(place.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            listOfNotNull(
                                place.kind,
                                place.meters.takeIf { it > 0 }?.let { "${it} 米" },
                            ).joinToString(" · "),
                            color = QTheme.colors.muted,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                if (name.isNotBlank()) {
                    PillButton("用这个名字", filled = false, onClick = {
                        if (lat == null || lng == null) {
                            NearbyPlaces.lastLocation(context)?.let { location ->
                                lat = location.latitude.toString()
                                lng = location.longitude.toString()
                            }
                        }
                        picked = true
                    })
                }
            } else {
                SoftField(value = name, onValueChange = { name = it }, label = "名字")
                Text("类型", style = MaterialTheme.typography.labelLarge)
                TripKindChips(kind.ifBlank { null }) { kind = it }
                Text("评分", style = MaterialTheme.typography.labelLarge)
                TripStars(rating.takeIf { it > 0 }) { rating = it ?: 0 }
                TripAmountField(amountText) { amountText = it }
                TripNoteField(noteText) { noteText = it }
                TripPhotoStrip(
                    photos = emptyList(),
                    localUris = photoUris,
                    canEdit = true,
                    onAdd = {
                        photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    onRemoveLocal = { uri -> photoUris = photoUris.filterNot { it == uri } },
                )
                Text(
                    if (visitedOn == LocalDate.now().toString()) "不是今天" else "记在 $visitedOn",
                    color = QTheme.colors.sky,
                    modifier = Modifier.clickable { pickingDate = true },
                )
                PillButton(
                    "记下",
                    enabled = !isBusy && name.trim().isNotBlank() && TripMath.knownKind(kind) &&
                        (amountText.isBlank() || amountText.yuanToCentsOrNull() != null),
                    onClick = {
                        if (lat == null || lng == null) {
                            NearbyPlaces.lastLocation(context)?.let { location ->
                                lat = location.latitude.toString()
                                lng = location.longitude.toString()
                            }
                        }
                        viewModel.addTripStop(
                            name = name,
                            kind = kind,
                            rating = TripMath.ratingOrNull(rating.takeIf { it > 0 }),
                            lat = lat?.toDoubleOrNull(),
                            lng = lng?.toDoubleOrNull(),
                            visitedOn = visitedOn,
                            amountCents = amountText.yuanToCentsOrNull(),
                            note = noteText,
                            photoUris = photoUris.map { Uri.parse(it) },
                            onSuccess = {
                                name = ""
                                kind = ""
                                rating = 0
                                amountText = ""
                                noteText = ""
                                photoUris = emptyList()
                                lat = null
                                lng = null
                                picked = false
                            },
                        )
                    },
                )
                PillButton("换一家", filled = false, enabled = !isBusy, onClick = {
                    name = ""
                    kind = ""
                    lat = null
                    lng = null
                    rating = 0
                    amountText = ""
                    noteText = ""
                    photoUris = emptyList()
                    picked = false
                })
            }
            if (active.stops.isNotEmpty()) {
                Text("今天记下的", style = MaterialTheme.typography.titleMedium)
                active.stops.filter { it.visitedOn == LocalDate.now().toString() }.asReversed().forEach { stop ->
                    Text(
                        listOfNotNull(
                            stop.amountCents?.toYuan(),
                            stop.rating?.let { "★$it" },
                            stop.kind,
                            stop.name,
                            stop.note?.takeIf { it.isNotBlank() }?.let { "有笔记" },
                            stop.photos.takeIf { it.isNotEmpty() }?.let { "图${it.size}" },
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    if (pickingDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = runCatching { LocalDate.parse(visitedOn).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli() }.getOrNull(),
        )
        DatePickerDialog(
            onDismissRequest = { pickingDate = false },
            confirmButton = {
                TextButton(onClick = {
                    val millis = state.selectedDateMillis
                    if (millis != null) {
                        visitedOn = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    pickingDate = false
                }) { Text("好") }
            },
            dismissButton = { TextButton(onClick = { pickingDate = false }) { Text("取消") } },
        ) { DatePicker(state = state) }
    }
}

private suspend fun loadNearby(
    context: android.content.Context,
    onResult: (List<NearbyPlace>, String) -> Unit,
) {
    val location = NearbyPlaces.locate(context)
    if (location == null) {
        onResult(emptyList(), "打不开定位，点这里再试，也可以先写店名")
        return
    }
    val places = runCatching { NearbyPlaces.search(location.latitude, location.longitude) }
    val found = places.getOrDefault(emptyList())
    onResult(
        found,
        when {
            places.isFailure -> "附近这会儿连不上，点这里再找一次"
            found.isEmpty() -> "附近没找到，自己写，或点这里再找一次"
            else -> "点下面一家，或自己写店名"
        },
    )
}
