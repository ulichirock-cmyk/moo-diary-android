package com.moodiary.app.ui.map

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.ln
import kotlin.math.pow
import kotlin.math.log2
import kotlin.math.sin
import kotlin.math.sinh

/** A point on the globe, in WGS-84 — the same datum the raster tiles are drawn in. */
data class GeoPoint(val lat: Double, val lng: Double)

/** Raster tiles are 256×256 at every zoom level. */
internal const val TILE_PX = 256

internal const val MIN_ZOOM = 3f
internal const val MAX_ZOOM = 19f

/** Web Mercator stops here; beyond it the projection runs off to infinity. */
private const val MAX_LAT = 85.05112878

/**
 * Web Mercator in normalised world coordinates: x and y both run 0..1 across the whole
 * map. Keeping the camera in these units instead of tile units means zooming does not
 * have to rewrite the position — tile coordinates at zoom z are simply these times 2^z.
 */
internal data class WorldPoint(val x: Double, val y: Double)

internal fun GeoPoint.toWorld(): WorldPoint {
    val s = sin(lat.coerceIn(-MAX_LAT, MAX_LAT) * PI / 180.0)
    return WorldPoint(
        x = (lng + 180.0) / 360.0,
        y = 0.5 - ln((1 + s) / (1 - s)) / (4 * PI),
    )
}

internal fun WorldPoint.toGeo(): GeoPoint = GeoPoint(
    lat = 180.0 / PI * atan(sinh(PI - 2.0 * PI * y)),
    lng = x * 360.0 - 180.0,
)

/**
 * What the map is looking at. Hoisted out of [TileMap] so the screen can move the
 * camera itself — the 回到当前位置 button does exactly that.
 */
@Stable
class MapCameraState(center: GeoPoint, zoom: Float) {

    internal var world by mutableStateOf(center.toWorld())
        private set

    var zoom by mutableFloatStateOf(zoom.coerceIn(MIN_ZOOM, MAX_ZOOM))
        private set

    /** Where the pin sits: the map is dragged under a fixed centre crosshair. */
    val center: GeoPoint get() = world.toGeo()

    fun moveTo(point: GeoPoint, zoom: Float = this.zoom) {
        world = point.toWorld()
        this.zoom = zoom.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    /** Screen pixels per normalised world unit at the current zoom. */
    internal fun pixelsPerWorld(baseTilePx: Float): Double =
        baseTilePx.toDouble() * 2.0.pow(zoom.toDouble())

    internal fun pan(dx: Float, dy: Float, baseTilePx: Float) {
        val scale = pixelsPerWorld(baseTilePx)
        world = WorldPoint(
            x = world.x - dx / scale,
            // Latitude is clamped rather than wrapped: you cannot drag past the poles.
            y = (world.y - dy / scale).coerceIn(0.0, 1.0),
        )
    }

    /**
     * Scales by [factor] while keeping whatever is under [focus] under [focus]. Without
     * that anchoring a pinch drifts away from the fingers doing it.
     */
    internal fun zoomBy(factor: Float, focus: Offsets, baseTilePx: Float) {
        val target = (zoom + log2(factor)).coerceIn(MIN_ZOOM, MAX_ZOOM)
        if (target == zoom) return
        val before = pixelsPerWorld(baseTilePx)
        val anchorX = world.x + (focus.x - focus.centerX) / before
        val anchorY = world.y + (focus.y - focus.centerY) / before
        zoom = target
        val after = pixelsPerWorld(baseTilePx)
        world = WorldPoint(
            x = anchorX - (focus.x - focus.centerX) / after,
            y = (anchorY - (focus.y - focus.centerY) / after).coerceIn(0.0, 1.0),
        )
    }

    /** The gesture focus plus the viewport centre it is measured against. */
    internal data class Offsets(val x: Float, val y: Float, val centerX: Float, val centerY: Float)
}
