package com.guanguanhua.app.trip

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.ln
import kotlin.math.sin

object TripMath {
    val KINDS = listOf("住宿", "美食", "风景", "博物馆", "杂物店")
    const val MAX_PHOTOS = 6
    const val MAP_MIN_ZOOM = 4
    const val MAP_MAX_ZOOM = 18

    fun knownKind(kind: String): Boolean = kind in KINDS

    fun ratingOrNull(rating: Int?): Int? = rating?.takeIf { it in 1..5 }

    fun startedDate(startedAt: Long, zone: ZoneId = ZoneId.systemDefault()): LocalDate =
        Instant.ofEpochMilli(startedAt).atZone(zone).toLocalDate()

    fun dayNumber(startedAt: Long, visitedOn: LocalDate, zone: ZoneId = ZoneId.systemDefault()): Int =
        ChronoUnit.DAYS.between(startedDate(startedAt, zone), visitedOn).toInt() + 1

    fun formatRange(startedAt: Long, endedAt: Long?, zone: ZoneId = ZoneId.systemDefault()): String {
        val start = startedDate(startedAt, zone)
        val end = endedAt?.let { Instant.ofEpochMilli(it).atZone(zone).toLocalDate() }
        return if (end == null) "${start.monthValue}月${start.dayOfMonth}日至今" else
            "${start.monthValue}月${start.dayOfMonth}日–${end.monthValue}月${end.dayOfMonth}日"
    }

    fun formatDay(startedAt: Long, visitedOn: LocalDate, zone: ZoneId = ZoneId.systemDefault()): String {
        val n = dayNumber(startedAt, visitedOn, zone)
        return "第${n}天 · ${visitedOn.monthValue}月${visitedOn.dayOfMonth}日"
    }

    fun kindFromTags(amenity: String?, tourism: String?, shop: String?): String? {
        val a = amenity.orEmpty()
        val t = tourism.orEmpty()
        val s = shop.orEmpty()
        return when {
            t in setOf("hotel", "guest_house", "hostel", "motel", "apartment") || a == "hotel" -> "住宿"
            a in setOf("restaurant", "cafe", "fast_food", "bar", "food_court", "ice_cream") -> "美食"
            t in setOf("museum", "gallery") || a == "arts_centre" -> "博物馆"
            s in setOf("convenience", "supermarket", "general", "gift") -> "杂物店"
            t.isNotBlank() || a == "attraction" -> "风景"
            else -> null
        }
    }

    /**
     * 预计结束日到了提醒一次；若还没结束，满 2 天再提醒一次。
     * 返回这次提醒对应的日期（写入 lastReminded）；不该提醒则 null。
     */
    fun nextEndReminder(plannedEnd: LocalDate?, ended: Boolean, today: LocalDate, lastReminded: LocalDate?): LocalDate? {
        if (ended || plannedEnd == null || today.isBefore(plannedEnd)) return null
        if (lastReminded == null) return plannedEnd
        if (lastReminded == plannedEnd && !today.isBefore(plannedEnd.plusDays(2))) return today
        return null
    }

    fun mapFrame(points: List<Pair<Double, Double>>, widthPx: Int, heightPx: Int, tileSize: Int = 256): MapFrame {
        val width = widthPx.coerceAtLeast(1)
        val height = heightPx.coerceAtLeast(1)
        check(points.isNotEmpty()) { "没有带位置的站点时不画地图" }
        val located = points
        var minLat = located.minOf { it.first }
        var maxLat = located.maxOf { it.first }
        var minLng = located.minOf { it.second }
        var maxLng = located.maxOf { it.second }
        if (maxLat - minLat < 0.08) {
            val mid = (minLat + maxLat) / 2
            minLat = mid - 0.04
            maxLat = mid + 0.04
        }
        if (maxLng - minLng < 0.08) {
            val mid = (minLng + maxLng) / 2
            minLng = mid - 0.04
            maxLng = mid + 0.04
        }
        val latPad = (maxLat - minLat) * 0.22
        val lngPad = (maxLng - minLng) * 0.22
        minLat -= latPad
        maxLat += latPad
        minLng -= lngPad
        maxLng += lngPad
        var zoom = MAP_MAX_ZOOM
        while (zoom > MAP_MIN_ZOOM) {
            val spanX = (lonToTileX(maxLng, zoom) - lonToTileX(minLng, zoom)) * tileSize
            val spanY = (latToTileY(minLat, zoom) - latToTileY(maxLat, zoom)) * tileSize
            if (spanX <= width && spanY <= height) break
            zoom--
        }
        val west = lonToTileX(minLng, zoom)
        val east = lonToTileX(maxLng, zoom)
        val north = latToTileY(maxLat, zoom)
        val south = latToTileY(minLat, zoom)
        val left = west - (width / tileSize.toDouble() - (east - west)) / 2
        val top = north - (height / tileSize.toDouble() - (south - north)) / 2
        return MapFrame(zoom, left, top, width, height, tileSize)
    }

    fun zoomFrame(frame: MapFrame, newZoom: Int, focusX: Float, focusY: Float): MapFrame {
        val zoom = newZoom.coerceIn(MAP_MIN_ZOOM, MAP_MAX_ZOOM)
        val scale = (1 shl zoom).toDouble() / (1 shl frame.zoom)
        val worldX = frame.left + focusX / frame.tileSize
        val worldY = frame.top + focusY / frame.tileSize
        return clampFrame(
            frame.copy(
                zoom = zoom,
                left = worldX * scale - focusX / frame.tileSize,
                top = worldY * scale - focusY / frame.tileSize,
            ),
        )
    }

    fun panFrame(frame: MapFrame, dxPx: Float, dyPx: Float): MapFrame =
        clampFrame(
            frame.copy(
                left = frame.left - dxPx / frame.tileSize,
                top = frame.top - dyPx / frame.tileSize,
            ),
        )

    fun mapTiles(frame: MapFrame): List<MapTile> {
        val maxIndex = (1 shl frame.zoom) - 1
        val x0 = floor(frame.left).toInt()
        val y0 = floor(frame.top).toInt()
        val x1 = floor(frame.left + frame.widthPx / frame.tileSize.toDouble()).toInt()
        val y1 = floor(frame.top + frame.heightPx / frame.tileSize.toDouble()).toInt()
        val tiles = mutableListOf<MapTile>()
        for (x in x0..x1) {
            for (y in y0..y1) {
                val tx = x.coerceIn(0, maxIndex)
                val ty = y.coerceIn(0, maxIndex)
                val host = 1 + ((tx + ty) and 3)
                tiles += MapTile(
                    x = tx,
                    y = ty,
                    z = frame.zoom,
                    url = "https://wprd0$host.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scl=1&style=7&x=$tx&y=$ty&z=${frame.zoom}",
                    offsetX = ((x - frame.left) * frame.tileSize).toFloat(),
                    offsetY = ((y - frame.top) * frame.tileSize).toFloat(),
                )
            }
        }
        return tiles
    }

    fun mapPixel(lat: Double, lng: Double, frame: MapFrame): Pair<Float, Float> {
        val x = (lonToTileX(lng, frame.zoom) - frame.left) * frame.tileSize
        val y = (latToTileY(lat, frame.zoom) - frame.top) * frame.tileSize
        return x.toFloat() to y.toFloat()
    }

    fun spreadOverlapping(pixels: List<Pair<Float, Float>>, minDist: Float): List<Pair<Float, Float>> {
        if (pixels.size < 2 || minDist <= 0f) return pixels
        val out = pixels.map { it.first to it.second }.toMutableList()
        repeat(12) {
            for (i in out.indices) {
                for (j in i + 1 until out.size) {
                    val dx = out[j].first - out[i].first
                    val dy = out[j].second - out[i].second
                    val dist = hypot(dx.toDouble(), dy.toDouble()).toFloat()
                    if (dist >= minDist) continue
                    if (dist < 0.5f) {
                        val angle = i * 2.399f + j
                        out[j] = out[j].first + cos(angle) * minDist to out[j].second + sin(angle) * minDist
                        continue
                    }
                    val push = (minDist - dist) / 2f
                    val ux = dx / dist
                    val uy = dy / dist
                    out[i] = out[i].first - ux * push to out[i].second - uy * push
                    out[j] = out[j].first + ux * push to out[j].second + uy * push
                }
            }
        }
        return out
    }

    fun routeSteps(
        x0: Float,
        y0: Float,
        x1: Float,
        y1: Float,
        stepPx: Float,
        sidePx: Float,
    ): List<RouteStep> {
        val dx = x1 - x0
        val dy = y1 - y0
        val len = hypot(dx.toDouble(), dy.toDouble()).toFloat()
        if (len < stepPx * 1.6f || stepPx <= 0f) return emptyList()
        val ux = dx / len
        val uy = dy / len
        // 屏幕坐标：0° 朝右、90° 朝下。脚印椭圆长轴沿 +X，再按这个角旋转。
        val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
        val steps = mutableListOf<RouteStep>()
        var walked = stepPx
        var left = true
        while (walked < len - stepPx * 0.45f) {
            val side = if (left) -1f else 1f
            steps += RouteStep(
                x = x0 + ux * walked + -uy * sidePx * side,
                y = y0 + uy * walked + ux * sidePx * side,
                angleDeg = angle,
                left = left,
            )
            left = !left
            walked += stepPx
        }
        return steps
    }

    private fun clampFrame(frame: MapFrame): MapFrame {
        val world = (1 shl frame.zoom).toDouble()
        val widthTiles = frame.widthPx / frame.tileSize.toDouble()
        val heightTiles = frame.heightPx / frame.tileSize.toDouble()
        return frame.copy(
            left = frame.left.coerceIn(0.0, (world - widthTiles).coerceAtLeast(0.0)),
            top = frame.top.coerceIn(0.0, (world - heightTiles).coerceAtLeast(0.0)),
        )
    }

    private fun lonToTileX(lng: Double, zoom: Int): Double =
        (lng + 180.0) / 360.0 * (1 shl zoom)

    private fun latToTileY(lat: Double, zoom: Int): Double {
        val clamped = lat.coerceIn(-85.05112878, 85.05112878)
        val sinLat = sin(Math.toRadians(clamped))
        return (1.0 - ln((1.0 + sinLat) / (1.0 - sinLat)) / (2.0 * PI)) / 2.0 * (1 shl zoom)
    }
}

data class MapFrame(
    val zoom: Int,
    val left: Double,
    val top: Double,
    val widthPx: Int,
    val heightPx: Int,
    val tileSize: Int = 256,
)

data class MapTile(
    val x: Int,
    val y: Int,
    val z: Int,
    val url: String,
    val offsetX: Float,
    val offsetY: Float,
)

data class RouteStep(
    val x: Float,
    val y: Float,
    val angleDeg: Float,
    val left: Boolean,
)
