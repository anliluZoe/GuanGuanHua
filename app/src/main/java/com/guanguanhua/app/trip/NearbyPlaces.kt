package com.guanguanhua.app.trip

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.guanguanhua.app.data.NearbyPlace
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

object NearbyPlaces {
    private const val FRESH_LOCATION_MS = 2 * 60 * 1000L
    private const val USABLE_LOCATION_MS = 30 * 60 * 1000L
    private const val LOCATE_TIMEOUT_MS = 10_000L
    private const val CACHE_MS = 5 * 60 * 1000L
    private const val USER_AGENT = "GuanGuanHua/1.0 (trip nearby)"
    private val overpassUrls = listOf(
        "https://overpass.kumi.systems/api/interpreter",
        "https://overpass-api.de/api/interpreter",
        "https://overpass.private.coffee/api/interpreter",
        "https://overpass.nchc.org.tw/api/interpreter",
    )
    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(16, TimeUnit.SECONDS)
        .callTimeout(12, TimeUnit.SECONDS)
        .build()

    @Volatile
    private var remembered: Location? = null

    @Volatile
    private var cachedSearch: Triple<String, Long, List<NearbyPlace>>? = null

    @SuppressLint("MissingPermission")
    fun lastLocation(context: Context): Location? {
        val manager = context.getSystemService(LocationManager::class.java)
        val gps = runCatching { manager?.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
        val net = runCatching { manager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
        val fused = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            runCatching { manager?.getLastKnownLocation(LocationManager.FUSED_PROVIDER) }.getOrNull()
        } else {
            null
        }
        return listOfNotNull(gps, net, fused, remembered).maxByOrNull { it.time }
    }

    @SuppressLint("MissingPermission")
    suspend fun locate(context: Context): Location? {
        val now = System.currentTimeMillis()
        val last = lastLocation(context)
        if (last != null && now - last.time <= FRESH_LOCATION_MS) {
            remembered = last
            return last
        }
        val manager = context.getSystemService(LocationManager::class.java)
        val provider = manager?.let { enabledProvider(it) }
        val current = if (manager != null && provider != null) {
            withTimeoutOrNull(LOCATE_TIMEOUT_MS) {
                suspendCancellableCoroutine<Location?> { cont ->
                    val signal = CancellationSignal()
                    cont.invokeOnCancellation { signal.cancel() }
                    LocationManagerCompat.getCurrentLocation(
                        manager,
                        provider,
                        signal,
                        ContextCompat.getMainExecutor(context),
                    ) { location ->
                        if (cont.isActive) cont.resume(location)
                    }
                }
            }
        } else {
            null
        }
        if (current != null) remembered = current
        if (current != null) return current
        val usable = last?.takeIf { now - it.time <= USABLE_LOCATION_MS }
        return usable ?: remembered
    }

    suspend fun search(lat: Double, lng: Double, radiusMeters: Int = 1500): List<NearbyPlace> =
        withContext(Dispatchers.IO) {
            val cacheKey = "${(lat * 250).roundToInt()}_${(lng * 250).roundToInt()}_$radiusMeters"
            val cached = cachedSearch
            if (cached != null && cached.first == cacheKey && System.currentTimeMillis() - cached.second < CACHE_MS) {
                return@withContext cached.third
            }
            val query = """
                [out:json][timeout:15];
                (
                  nwr(around:$radiusMeters,$lat,$lng)[amenity][name];
                  nwr(around:$radiusMeters,$lat,$lng)[amenity]["name:zh"];
                  nwr(around:$radiusMeters,$lat,$lng)[tourism][name];
                  nwr(around:$radiusMeters,$lat,$lng)[tourism]["name:zh"];
                  nwr(around:$radiusMeters,$lat,$lng)[shop][name];
                  nwr(around:$radiusMeters,$lat,$lng)[shop]["name:zh"];
                );
                out center 48;
            """.trimIndent()
            var lastError: IOException? = null
            for (url in overpassUrls) {
                val request = Request.Builder()
                    .url(url)
                    .header("User-Agent", USER_AGENT)
                    .post(FormBody.Builder().add("data", query).build())
                    .build()
                val response = runCatching { client.newCall(request).execute() }.getOrElse { error ->
                    lastError = IOException(error.message ?: "附近连不上", error)
                    null
                } ?: continue
                val places = response.use { http ->
                    when {
                        http.isSuccessful -> parseOverpass(http.body?.string().orEmpty(), lat, lng)
                        http.code == 429 || http.code >= 500 -> {
                            lastError = IOException("overpass ${http.code}")
                            null
                        }
                        else -> return@withContext emptyList()
                    }
                }
                if (places != null) {
                    cachedSearch = Triple(cacheKey, System.currentTimeMillis(), places)
                    return@withContext places
                }
            }
            throw lastError ?: IOException("附近连不上")
        }

    fun metersBetween(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Int {
        val earth = 6371000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLng = Math.toRadians(lng2 - lng1)
        val a = sin(dLat / 2) * sin(dLat / 2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2) * sin(dLng / 2)
        return (2 * earth * atan2(sqrt(a), sqrt(1 - a))).roundToInt()
    }

    fun parseOverpass(raw: String, originLat: Double, originLng: Double): List<NearbyPlace> {
        val root = runCatching { JsonParser.parseString(raw).asJsonObject }.getOrNull() ?: return emptyList()
        val elements = root.getAsJsonArray("elements") ?: return emptyList()
        return elements.mapNotNull { element ->
            val obj = element.asJsonObject
            val tags = obj.getAsJsonObject("tags") ?: return@mapNotNull null
            val name = tagText(tags, "name:zh-Hans") ?: tagText(tags, "name:zh") ?: tagText(tags, "name")
                ?: return@mapNotNull null
            val center = obj.get("center")?.takeIf { it.isJsonObject }?.asJsonObject
            val lat = coord(obj, "lat") ?: coord(center, "lat") ?: return@mapNotNull null
            val lng = coord(obj, "lon") ?: coord(center, "lon") ?: return@mapNotNull null
            NearbyPlace(
                name = name,
                kind = TripMath.kindFromTags(
                    tagText(tags, "amenity"),
                    tagText(tags, "tourism"),
                    tagText(tags, "shop"),
                ),
                lat = lat,
                lng = lng,
                meters = metersBetween(originLat, originLng, lat, lng),
            )
        }.distinctBy { it.name to it.lat.toBits() }.sortedBy { it.meters }.take(24)
    }

    private fun tagText(tags: JsonObject, key: String): String? =
        runCatching { tags.get(key)?.asString?.trim()?.takeIf { it.isNotEmpty() } }.getOrNull()

    private fun coord(obj: JsonObject?, key: String): Double? =
        runCatching { obj?.get(key)?.asDouble }.getOrNull()

    private fun enabledProvider(manager: LocationManager): String? {
        val candidates = buildList {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(LocationManager.FUSED_PROVIDER)
            add(LocationManager.NETWORK_PROVIDER)
            add(LocationManager.GPS_PROVIDER)
        }
        return candidates.firstOrNull { name ->
            runCatching { manager.isProviderEnabled(name) }.getOrDefault(false)
        }
    }
}
