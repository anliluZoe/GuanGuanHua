package com.guanguanhua.app.trip

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import com.google.gson.JsonParser
import com.guanguanhua.app.data.NearbyPlace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

object NearbyPlaces {
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .build()

    @SuppressLint("MissingPermission")
    fun lastLocation(context: Context): Location? {
        val manager = context.getSystemService(LocationManager::class.java) ?: return null
        val gps = runCatching { manager.getLastKnownLocation(LocationManager.GPS_PROVIDER) }.getOrNull()
        val net = runCatching { manager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER) }.getOrNull()
        return listOfNotNull(gps, net).maxByOrNull { it.time }
    }

    suspend fun search(lat: Double, lng: Double, radiusMeters: Int = 700): List<NearbyPlace> =
        withContext(Dispatchers.IO) {
            val query = """
                [out:json][timeout:12];
                (
                  node(around:$radiusMeters,$lat,$lng)[name][amenity];
                  node(around:$radiusMeters,$lat,$lng)[name][tourism];
                  node(around:$radiusMeters,$lat,$lng)[name][shop];
                );
                out body 36;
            """.trimIndent()
            val request = Request.Builder()
                .url("https://overpass-api.de/api/interpreter")
                .header("User-Agent", "GuanGuanHua/1.0 (trip nearby)")
                .post(FormBody.Builder().add("data", query).build())
                .build()
            val body = client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext emptyList()
                response.body?.string().orEmpty()
            }
            parseOverpass(body, lat, lng)
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
            val name = tags.get("name")?.asString?.trim().orEmpty()
            if (name.isBlank()) return@mapNotNull null
            val lat = obj.get("lat")?.asDouble ?: return@mapNotNull null
            val lng = obj.get("lon")?.asDouble ?: return@mapNotNull null
            NearbyPlace(
                name = name,
                kind = TripMath.kindFromTags(
                    tags.get("amenity")?.asString,
                    tags.get("tourism")?.asString,
                    tags.get("shop")?.asString,
                ),
                lat = lat,
                lng = lng,
                meters = metersBetween(originLat, originLng, lat, lng),
            )
        }.distinctBy { it.name to it.lat.toBits() }.sortedBy { it.meters }.take(24)
    }
}
